package top.babyzombie.addons.util.pet;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLevelEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemLore;
import org.jetbrains.annotations.Nullable;
import top.babyzombie.addons.config.ModConfigManager;
import top.babyzombie.addons.event.ContainerClickEvents;
import top.babyzombie.addons.util.*;
import top.babyzombie.addons.util.pet.state.PlayerPetState;
import top.babyzombie.addons.util.tracker.HypixelLocationTracker;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import de.hysky.skyblocker.skyblock.item.SkyblockInventoryScreen;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Manages per-profile pet data parsed from Hypixel SkyBlock pet items.
 * Persists to config/babyzombieaddons/data/&lt;uuid&gt;_&lt;profileId&gt;/pets.json.
 */
public final class PetManager {

    private static final PetManager INSTANCE = new PetManager();

    private static final Pattern PROFILE_ID_PATTERN =
        Pattern.compile("Profile ID: ([a-f0-9-]+)");
    private static final Pattern AUTO_PET_PATTERN = Pattern.compile(
        "Autopet equipped your \\[Lvl ([0-9]{1,3})]" +
        "( \\[[0-9]{1,4}✦\\])? ([a-zA-Z0-9_\\- ✦]+)! VIEW RULE"
    );
    private static final Pattern HELD_ITEM_PATTERN =
        Pattern.compile("Held Item: (.+)", Pattern.CASE_INSENSITIVE);
    // Loadout switch pending flag — set by preset click, consumed by whenScreenOpened.
    // Timestamp guards against stale flags persisting across separate page visits.
    private boolean loadoutSwitchPending;
    private long loadoutSwitchPendingTime;

    public boolean isLoadoutSwitchPending() { return loadoutSwitchPending; }
    public void setLoadoutSwitchPending(boolean v) {
        this.loadoutSwitchPending = v;
        if (v) this.loadoutSwitchPendingTime = System.currentTimeMillis();
    }

    /** True only if the pending flag was set recently (within 3 seconds). */
    private boolean isRecentLoadoutSwitch() {
        return loadoutSwitchPending
            && System.currentTimeMillis() - loadoutSwitchPendingTime < 3_000;
    }

    private String currentProfileKey;
    private final List<PetData> pets = new ArrayList<>();
    @Nullable
    private String currentPetUuid;
    private final Set<String> sharedPetUuids = new LinkedHashSet<>();
    private final PlayerPetState petState = new PlayerPetState();

    private PetManager() {}

    public static PetManager getInstance() { return INSTANCE; }

    // ===== Init =====

    public void init() {
        // Load constants early
        PetConstants.getInstance().ensureLoaded();
        loadProfile(profileKey());
        registerProfileSwitch();
        registerAutoPetRule();
        registerPetItemInteract();
        registerGuiClick();
        registerPetsMenuScan();
        registerLoadoutSwitch();
        registerDisconnectSave();
        // Initialize XP tracker and mayor fetcher
        PetExperienceTracker.getInstance().init(this, petState);
        MayorFetcher.getInstance().init(petState);
    }

    // ===== Profile Management =====

    private static String profileKey() {
        var tracker = HypixelLocationTracker.getInstance();
        String uuid = tracker.getUuid();
        String profileId = tracker.getProfileId();
        if (uuid == null || profileId == null) return null;
        return uuid + "_" + profileId;
    }

    private void loadProfile(String key) {
        if (key == null) return;
        if (key.equals(currentProfileKey)) return;
        saveCurrentProfile();
        currentProfileKey = key;
        ProfilePetData loaded = DataPersistence.load(key, "pets.json", ProfilePetData.class);
        pets.clear();
        currentPetUuid = null;
        sharedPetUuids.clear();
        if (loaded != null) {
            if (loaded.pets != null) pets.addAll(loaded.pets);
            currentPetUuid = loaded.currentPetUuid;
            if (loaded.sharedPetUuids != null) sharedPetUuids.addAll(loaded.sharedPetUuids);
        }
        // Load pet state
        PlayerPetState loadedState = DataPersistence.load(key, "pet_state.json", PlayerPetState.class);
        if (loadedState != null) {
            // Migrate from old tamingLevel field
            if (loadedState.tamingLevel > 0 && !loadedState.skillLevels.containsKey(SkillType.TAMING)) {
                loadedState.skillLevels.put(SkillType.TAMING, loadedState.tamingLevel);
            }
            petState.skillLevels = loadedState.skillLevels;
            petState.beastmasterMult = loadedState.beastmasterMult;
            petState.battleExperienceLevel = loadedState.battleExperienceLevel;
            petState.whyNotMoreLevel = loadedState.whyNotMoreLevel;
            petState.dianaMayor = loadedState.dianaMayor;
            petState.dianaPetXpBuff = loadedState.dianaPetXpBuff;
            petState.dianaSharingIsCaring = loadedState.dianaSharingIsCaring;
            petState.mayorLastCheckTime = loadedState.mayorLastCheckTime;
        }
        PetExperienceTracker.getInstance().reset();
        MayorFetcher.getInstance().fetchIfStale();
    }

    public void saveCurrentProfile() {
        if (currentProfileKey == null) return;
        DataPersistence.save(currentProfileKey, "pets.json",
            new ProfilePetData(currentPetUuid, new ArrayList<>(pets), new LinkedHashSet<>(sharedPetUuids)));
        DataPersistence.save(currentProfileKey, "pet_state.json", petState);
    }

    public void saveAll() { saveCurrentProfile(); }

    // ===== CRUD =====

    public void addPet(PetData pet) {
        if (pet.uuid() != null) pets.removeIf(p -> pet.uuid().equals(p.uuid()));
        pets.add(pet);
        saveCurrentProfile();
    }

    public void removePet(@Nullable String uuid) {
        if (uuid == null) return;
        pets.removeIf(p -> uuid.equals(p.uuid()));
        if (uuid.equals(currentPetUuid)) {
            currentPetUuid = null;
        }
        saveCurrentProfile();
    }

    public void setCurrentPet(@Nullable PetData pet) {
        this.currentPetUuid = pet != null ? pet.uuid() : null;
        saveCurrentProfile();
    }

    @Nullable
    public PetData getCurrentPet() {
        if (currentPetUuid == null) return null;
        for (PetData p : pets) {
            if (currentPetUuid.equals(p.uuid())) return p;
        }
        return null;
    }

    public List<PetData> getPets() { return Collections.unmodifiableList(pets); }

    /**
     * Find a pet matching an Autopet rule by name, level, and optional held item.
     */
    @Nullable
    public PetData findPet(String normalizedName, int level, @Nullable String heldItemName) {
        for (PetData p : pets) {
            String typeName = p.type().replace("_", " ").toLowerCase(Locale.ROOT);
            String searchName = normalizedName.replace(" ✦", "").toLowerCase(Locale.ROOT);
            if (!typeName.equals(searchName)) continue;
            if (p.getLevel() != level) continue;
            if (heldItemName != null) {
                String petHeld = getHeldItemDisplayName(p.heldItem());
                if (petHeld == null
                    || !ChatUtils.stripColor(petHeld).equalsIgnoreCase(ChatUtils.stripColor(heldItemName)))
                    continue;
            }
            return p;
        }
        return null;
    }

    // ===== Shared Pets =====

    public void setSharedPetUuids(Set<String> uuids) {
        sharedPetUuids.clear();
        if (uuids != null) sharedPetUuids.addAll(uuids);
        saveCurrentProfile();
    }

    public Set<String> getSharedPetUuids() {
        return Collections.unmodifiableSet(sharedPetUuids);
    }

    /** Get all pets currently in exp share slots, filtered from the pets list. */
    public List<PetData> getSharedPets() {
        List<PetData> result = new ArrayList<>();
        for (PetData p : pets) {
            if (p.uuid() != null && sharedPetUuids.contains(p.uuid())) {
                result.add(p);
            }
        }
        return result;
    }

    // ===== Experience =====

    /**
     * Add experience to a pet by UUID. Replaces the old record with updated exp.
     */
    public void addPetExp(@Nullable String uuid, double expToAdd) {
        if (uuid == null || expToAdd <= 0) return;
        for (int i = 0; i < pets.size(); i++) {
            PetData p = pets.get(i);
            if (uuid.equals(p.uuid())) {
                pets.set(i, p.withExp(p.exp() + expToAdd));
                return;
            }
        }
    }

    // ===== Pet State =====

    public PlayerPetState getPetState() {
        return petState;
    }

    // ===== NBT Helpers =====

    /** Extract petInfo JSON string from an ItemStack. */
    @Nullable
    public static String getPetInfoFromStack(ItemStack stack) {
        var customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData == null) return null;
        var tag = customData.copyTag();
        String petInfo = tag.getString("petInfo").orElse(null);
        if (petInfo != null) return petInfo;
        var extra = tag.getCompound("ExtraAttributes").orElse(null);
        if (extra == null) return null;
        return extra.getString("petInfo").orElse(null);
    }

    /** Extract item UUID from NBT. */
    @Nullable
    public static String getItemUuid(ItemStack stack) {
        var customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData == null) return null;
        var tag = customData.copyTag();
        String uuid = tag.getString("uuid").orElse(null);
        if (uuid != null) return uuid;
        var extra = tag.getCompound("ExtraAttributes").orElse(null);
        if (extra == null) return null;
        return extra.getString("uuid").orElse(null);
    }

    /** Extract the {@code skin} field from a petInfo JSON string. */
    @Nullable
    public static String getSkinFromPetInfo(String petInfoJson) {
        try {
            JsonObject obj = JsonParser.parseString(petInfoJson).getAsJsonObject();
            if (obj.has("skin") && !obj.get("skin").isJsonNull()) {
                return obj.get("skin").getAsString();
            }
        } catch (Exception ignored) {}
        return null;
    }

    /**
     * Resolve the pet skin variant and return a copy of the pet with
     * {@link PetData#resolvedSkin()} set. Called once at scan time.
     */
    private static PetData resolveAndApplySkin(PetData pet, String petInfoJson, ItemStack stack) {
        String skin = getSkinFromPetInfo(petInfoJson);
        if (skin == null || skin.isEmpty()) return pet;
        String skullTexture = ItemUtils.getSkullTexture(stack);
        String resolved = top.babyzombie.addons.module.misc.pet.PetSkinTexture.getInstance()
            .resolveSkinVariant(skin, skullTexture, petInfoJson);
        return pet.withResolvedSkin(resolved);
    }

    // ===== Item Repo =====

    private static final Map<String, String> displayNameCache = new HashMap<>();

    /** Get the items directory from the resolved item repo, or null. */
    @Nullable
    private static Path getItemsDir() {
        PetConstants constants = PetConstants.getInstance();
        constants.ensureLoaded();
        return constants.getItemsDir();
    }

    /** Get the held item's display name from the item repo, or fallback. */
    @Nullable
    private static String getHeldItemDisplayName(@Nullable String heldItem) {
        if (heldItem == null) return null;
        String cached = displayNameCache.get(heldItem);
        if (cached != null) return cached;

        String fallback = heldItem.replace("PET_ITEM_", "").replace("_", " ").toLowerCase(Locale.ROOT);

        Path itemsDir = getItemsDir();
        if (itemsDir != null) {
            Path file = itemsDir.resolve(heldItem + ".json");
            if (Files.exists(file)) {
                try {
                    String raw = Files.readString(file);
                    JsonObject obj = JsonParser.parseString(raw).getAsJsonObject();
                    String display = obj.get("displayname").getAsString();
                    String name = ChatUtils.stripColor(display);
                    displayNameCache.put(heldItem, name);
                    return name;
                } catch (IOException ignored) {}
            }
        }
        displayNameCache.put(heldItem, fallback);
        return fallback;
    }

    // ===== Event Handlers =====

    /** Profile switch via chat message "Profile ID: ..." */
    private void registerProfileSwitch() {
        ClientReceiveMessageEvents.ALLOW_GAME.register((message, overlay) -> {
            if (overlay) return true;
            if (!HypixelLocationTracker.getInstance().isInSkyblock()) return true;
            Matcher m = PROFILE_ID_PATTERN.matcher(message.getString());
            if (m.find()) {
                String uuid = HypixelLocationTracker.getInstance().getUuid();
                if (uuid == null) return true;
                loadProfile(uuid + "_" + m.group(1));
            }
            return true;
        });
    }

    /** Detect Autopet rule equip from action bar messages. */
    private void registerAutoPetRule() {
        ClientReceiveMessageEvents.ALLOW_GAME.register((message, overlay) -> {
            if (overlay) return true;
            if (!HypixelLocationTracker.getInstance().isInSkyblock()) return true;
            String text = ChatUtils.stripColor(message.getString());
            Matcher m = AUTO_PET_PATTERN.matcher(text);
            if (!m.find()) return true;

            int level = Integer.parseInt(m.group(1));
            String petNameRaw = m.group(3);

            // Extract held item name from the hover text of the component
            String heldItemName = findHoverTextContaining(message, "Autopet Rule");
            String heldItem = null;
            if (heldItemName != null) {
                Matcher hm = HELD_ITEM_PATTERN.matcher(ChatUtils.stripColor(heldItemName));
                if (hm.find()) heldItem = hm.group(1);
            }

            PetData matched = findPet(petNameRaw, level, heldItem);
            if (matched != null) {
                setCurrentPet(matched);
            }
            return true;
        });
    }

    /** Right-click with a PET item adds it to the list. */
    private void registerPetItemInteract() {
        UseItemCallback.EVENT.register((player, world, hand) -> {
            if (!HypixelLocationTracker.getInstance().isInSkyblock())
                return InteractionResult.PASS;
            ItemStack stack = player.getItemInHand(hand);
            String petInfo = getPetInfoFromStack(stack);
            if (petInfo == null) return InteractionResult.PASS;
            PetData pet = PetData.fromPetInfo(petInfo);
            pet = resolveAndApplySkin(pet, petInfo, stack);
            addPet(pet);
            return InteractionResult.PASS;
        });
    }

    /** GUI click: right-click removes pet, left-click selects/despawns (works in all screens). */
    private void registerGuiClick() {
        ContainerClickEvents.BEFORE_CONTAINER_INPUT.register((player, containerId, slotId, buttonNum, input) -> {
            if (input != ContainerInput.PICKUP) return false;
            if (!HypixelLocationTracker.getInstance().isInSkyblock()) return false;

            // Only track clicks in the Pets menu
            var screen = Minecraft.getInstance().screen;
            if (screen == null || !ChatUtils.stripColor(screen.getTitle().getString()).matches("(\\(\\d+/\\d+\\) )?Pets")) {
                return false;
            }
            var slot = player.containerMenu.getSlot(slotId);
            if (slot == null || !slot.hasItem()) return false;
            ItemStack stack = slot.getItem();
            if (getPetInfoFromStack(stack) == null) return false;

            String uuid = getItemUuid(stack);
            if (uuid == null) return false;

            // Check if this pet is currently summoned (has "Click to despawn!" lore)
            boolean isDespawnable = false;
            ItemLore lore = stack.get(DataComponents.LORE);
            if (lore != null) {
                for (Component line : lore.lines()) {
                    if ("Click to despawn!".equals(ChatUtils.stripColor(line.getString()))) {
                        isDespawnable = true;
                        break;
                    }
                }
            }

            if (buttonNum == 1) {
                // Right-click: convert to item → remove from tracking
                removePet(uuid);
                if (uuid.equals(currentPetUuid)) setCurrentPet(null);
            } else if (buttonNum == 0) {
                // Left-click: summon/despawn
                for (PetData p : pets) {
                    if (uuid.equals(p.uuid())) {
                        setCurrentPet(isDespawnable ? null : p);
                        break;
                    }
                }
            }
            return false;
        });
    }

    /** Scan the Pets menu, Exp Sharing, Skills, Accessory Bag, and Attribute Menu. */
    private void registerPetsMenuScan() {
        // Pets main page
        ScreenLoadWaiter.whenScreenOpened(
            title -> ChatUtils.stripColor(title).matches("(\\(\\d+/\\d+\\) )?Pets"),
            44, 0, this::scanPetsContainer);

        // Exp Sharing sub-page
        ScreenLoadWaiter.whenScreenOpened(
            title -> "Exp Sharing".equals(ChatUtils.stripColor(title)),
            44, 0, this::scanExpSharingPage);

        // Your Skills — scan Taming level
        ScreenLoadWaiter.whenScreenOpened(
            title -> "Your Skills".equals(ChatUtils.stripColor(title)),
            30, 0, this::scanSkillsPage);

        // Accessory Bag — scan Beastmaster Crest
        ScreenLoadWaiter.whenScreenOpened(
            title -> ChatUtils.stripColor(title).startsWith("Accessory Bag"),
            44, 0, this::scanAccessoryBag);

        // Attribute Menu — scan Battle Experience
        ScreenLoadWaiter.whenScreenOpened(
            title -> "Attribute Menu".equals(ChatUtils.stripColor(title)),
            44, 0, this::scanAttributeMenu);
    }

    /** Save on disconnect or world unload. */
    private void registerDisconnectSave() {
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> saveAll());
        ClientLevelEvents.AFTER_CLIENT_LEVEL_CHANGE.register((client, world) -> {
            if (world == null) saveAll();
        });
    }

    /**
     * Detect loadout switch via left-click on a preset slot in the Loadouts
     * menu (rows 2-5, cols 6-8 = 12 slots). Then wait for the refreshed page
     * to load (slot 53 ≠ air), scan the pet, and optionally auto-close.
     */
    private void registerLoadoutSwitch() {
        // Always scan the pet when the Loadouts menu opens (harmless, idempotent)
        ScreenLoadWaiter.whenScreenOpened(
            title -> ChatUtils.stripColor(title).matches("\\(\\d+/\\d+\\) Loadouts"),
            43, 0, // 等所有内容(43)加载即可，不用等整个页面(53)
            cs -> {
                scanLoadoutPet(cs);
                if (isRecentLoadoutSwitch()) {
                    loadoutSwitchPending = false;
                    if (ModConfigManager.get().skyblock.loadout.autoClose) {
                        var client = Minecraft.getInstance();
                        client.execute(() -> {
                            if (client.player != null) client.player.closeContainer();
                        });
                    }
                }
            });

        // Left-click on a preset slot in the Loadouts menu flags that the
        // next page refresh is a loadout switch and should trigger auto-close.
        // Uses BEFORE_CONTAINER_INPUT to also catch programmatic clicks (e.g. IQ mod).
        ContainerClickEvents.BEFORE_CONTAINER_INPUT.register((player, containerId, slotId, buttonNum, input) -> {
            if (input != ContainerInput.PICKUP) return false;
            if (buttonNum != 0) return false;
            if (slotId < 0) return false;
            if (!HypixelLocationTracker.getInstance().isInSkyblock()) return false;

            var client = Minecraft.getInstance();
            if (!(client.screen instanceof AbstractContainerScreen<?> screen)) return false;
            String title = ChatUtils.stripColor(screen.getTitle().getString());
            if (!title.matches("\\(\\d+/\\d+\\) Loadouts")) return false;

            // Preset slots: rows 2-5, cols 6-8 → slots 14-16, 23-25, 32-34, 41-43
            int row = slotId / 9;
            int col = slotId % 9;
            if (row < 1 || row > 4 || col < 5 || col > 7) return false;

            loadoutSwitchPending = true;
            return false;
        });
    }

    /**
     * Scan the Loadouts menu for the equipped pet.
     * The pet is in row 3, column 4 (0-indexed: row 2, col 3 → slot 21)
     * of the 6-row Loadouts chest menu.
     */
    public void scanLoadoutPet(AbstractContainerScreen<?> screen) {
        scanLoadoutPetAndEquipment(screen, true, true);
    }

    /**
     * Scan the Loadouts menu for pet + equipment, and sync equipment to
     * Skyblocker's SkyblockInventoryScreen.equipment[] so the inventory
     * equipment display stays up-to-date after a loadout switch.
     *
     * @param scanPet   whether to scan the pet slot
     * @param syncEquip whether to sync equipment to Skyblocker
     */
    public void scanLoadoutPetAndEquipment(AbstractContainerScreen<?> screen,
                                            boolean scanPet, boolean syncEquip) {
        var slots = screen.getMenu().slots;

        // --- Pet (slot 21) ---
        if (scanPet) {
            int petSlot = 21;
            if (petSlot < slots.size()) {
                ItemStack stack = slots.get(petSlot).getItem();
                if (!stack.isEmpty()) {
                    String petInfo = getPetInfoFromStack(stack);
                    if (petInfo != null) {
                        PetData pet = PetData.fromPetInfo(petInfo);
                        pet = resolveAndApplySkin(pet, petInfo, stack);
                        if (pet.uuid() != null) {
                            addPet(pet);
                            setCurrentPet(pet);
                        }
                    }
                }
            }
        }

        // --- Equipment (slots 10/19/28/37: necklace/cloak/belt/glove) ---
        if (syncEquip) {
            syncEquipmentToSkyblocker(slots);
        }
    }

    /** Write equipment from Loadouts container column 1 to Skyblocker's static array. */
    private static void syncEquipmentToSkyblocker(java.util.List<net.minecraft.world.inventory.Slot> slots) {
        if (!FabricLoader.getInstance().isModLoaded("skyblocker")) return;

        var equipment = SkyblockInventoryScreen.equipment;
        int[] equipSlots = {10, 19, 28, 37}; // necklace, cloak, belt, glove
        for (int i = 0; i < 4 && i < equipment.length; i++) {
            if (equipSlots[i] < slots.size()) {
                ItemStack stack = slots.get(equipSlots[i]).getItem();
                equipment[i] = (stack.isEmpty() || isPlaceholder(stack))
                    ? ItemStack.EMPTY : stack.copy();
            }
        }
    }

    /** Detect Hypixel placeholder items that appear before real equipment loads. */
    private static boolean isPlaceholder(ItemStack stack) {
        String name = ChatUtils.stripColor(stack.getHoverName().getString()).toLowerCase(java.util.Locale.ROOT);
        return name.startsWith("empty") || name.startsWith("slot ");
    }

    // ===== Internal Helpers =====

    /** Walk the component tree to find hover text containing the given keyword. */
    @Nullable
    private static String findHoverTextContaining(Component component, String keyword) {
        var hover = component.getStyle().getHoverEvent();
        if (hover != null) {
            String text = getShowText(hover);
            if (text != null && text.contains(keyword)) return text;
        }
        for (Component sibling : component.getSiblings()) {
            String result = findHoverTextContaining(sibling, keyword);
            if (result != null) return result;
        }
        return null;
    }

    @Nullable
    private static String getShowText(HoverEvent hover) {
        if (hover instanceof HoverEvent.ShowText showText) {
            Component text = showText.value();
            return text != null ? text.getString() : null;
        }
        return null;
    }

    /** Scan the Pets container for summoned pet items and add them to the list. */
    private void scanPetsContainer(AbstractContainerScreen<?> screen) {
        var slots = screen.getMenu().slots;
        for (int i = 10; i < 44; i++) {
            ItemStack stack = slots.get(i).getItem();
            if (stack.isEmpty()) continue;
            if (getPetInfoFromStack(stack) == null) continue;

            ItemLore lore = stack.get(DataComponents.LORE);
            if (lore == null) continue;

            boolean isConvertible = false;
            boolean isDespawnable = false;
            for (Component line : lore.lines()) {
                String text = ChatUtils.stripColor(line.getString());
                if ("Right-click to convert to an item!".equals(text)) isConvertible = true;
                if ("Click to despawn!".equals(text)) isDespawnable = true;
            }
            if (!isConvertible) continue;

            String petInfo = getPetInfoFromStack(stack);
            if (petInfo == null) continue;

            String uuid = getItemUuid(stack);
            if (uuid != null) removePet(uuid);
            PetData pet = PetData.fromPetInfo(petInfo);
            pet = resolveAndApplySkin(pet, petInfo, stack);
            addPet(pet);

            if (isDespawnable && uuid != null) {
                for (PetData p : pets) {
                    if (uuid.equals(p.uuid())) {
                        setCurrentPet(p);
                        break;
                    }
                }
            }
        }
    }

    /** Scan the Exp Sharing page for pets in share slots (row 4, cols 4/5/6 → slots 30/31/32). */
    private void scanExpSharingPage(AbstractContainerScreen<?> screen) {
        var slots = screen.getMenu().slots;
        Set<String> foundUuids = new LinkedHashSet<>();
        for (int slot : new int[]{30, 31, 32}) {
            ItemStack stack = slots.get(slot).getItem();
            if (stack.isEmpty()) continue;
            String petInfo = getPetInfoFromStack(stack);
            if (petInfo == null) continue;
            PetData pet = PetData.fromPetInfo(petInfo);
            if (pet.uuid() != null) {
                // Ensure pet is in the main list
                pets.removeIf(p -> pet.uuid().equals(p.uuid()));
                pets.add(pet);
                foundUuids.add(pet.uuid());
            }
        }
        setSharedPetUuids(foundUuids);
    }

    /** Scan the Your Skills page for pet-relevant skill levels. */
    private void scanSkillsPage(AbstractContainerScreen<?> screen) {
        var slots = screen.getMenu().slots;
        // Row 3 cols 2-8 (slots 19-25): Farming→Alchemy
        // Row 4 cols 2-4 (slots 28-30): Taming, Dungeoneering, Carpentry
        // Row 4 cols 6-7 (slots 32-33): Runecrafting, Social
        int[] skillSlots = {19,20,21,22,23,24,25, 28,29,30, 32,33};

        for (int slot : skillSlots) {
            ItemStack stack = slots.get(slot).getItem();
            if (stack.isEmpty()) continue;
            String name = ChatUtils.stripColor(stack.getHoverName().getString());
            String[] parts = name.split("\\s+");
            if (parts.length < 2) continue;
            SkillType skill = SkillType.fromDisplayName(parts[0]);
            if (skill == null) continue;
            int level = RomanNumeral.parse(parts[parts.length - 1]);
            if (level <= 0 || level > 60) continue;
            // Only keep skills that give pet XP
            if (skill == SkillType.FARMING || skill == SkillType.MINING
                || skill == SkillType.COMBAT || skill == SkillType.FORAGING
                || skill == SkillType.FISHING || skill == SkillType.ENCHANTING
                || skill == SkillType.ALCHEMY || skill == SkillType.TAMING
                || skill == SkillType.HUNTING) {
                petState.skillLevels.put(skill, level);
            }
        }
        saveCurrentProfile();
    }

    /** Scan the Accessory Bag for Beastmaster Crest and parse its pet XP boost. */
    private void scanAccessoryBag(AbstractContainerScreen<?> screen) {
        var slots = screen.getMenu().slots;
        // Skip player inventory (bottom 36 slots for 6-row chest)
        for (int i = 0; i < slots.size() - 36; i++) {
            ItemStack stack = slots.get(i).getItem();
            if (stack.isEmpty()) continue;
            String name = ChatUtils.stripColor(stack.getHoverName().getString());
            if (!name.contains("Beastmaster Crest")) continue;

            ItemLore lore = stack.get(DataComponents.LORE);
            if (lore == null) continue;
            for (Component line : lore.lines()) {
                String text = ChatUtils.stripColor(line.getString());
                // "Pet Exp Boost: +4%" or "Pet Exp Boost: +0.5%"
                if (text.startsWith("Pet Exp Boost: +") && text.endsWith("%")) {
                    try {
                        String pct = text.substring("Pet Exp Boost: +".length(), text.length() - 1);
                        double boost = Double.parseDouble(pct);
                        petState.beastmasterMult = 1.0 + boost / 100.0;
                        saveCurrentProfile();
                    } catch (NumberFormatException ignored) {}
                }
            }
            break; // Only one Beastmaster Crest matters
        }
    }

    /** Scan the Attribute Menu for Battle Experience and Why Not More. */
    private void scanAttributeMenu(AbstractContainerScreen<?> screen) {
        var slots = screen.getMenu().slots;
        boolean foundBattle = false, foundWhyNot = false;
        for (int i = 0; i < slots.size() - 36; i++) {
            ItemStack stack = slots.get(i).getItem();
            if (stack.isEmpty()) continue;
            String name = ChatUtils.stripColor(stack.getHoverName().getString());

            if (!foundBattle && name.startsWith("Battle Experience ")) {
                String roman = name.substring("Battle Experience ".length()).trim();
                int level = RomanNumeral.parse(roman);
                if (level >= 0 && level <= 10) {
                    petState.battleExperienceLevel = level;
                    foundBattle = true;
                }
            }
            if (!foundWhyNot && name.startsWith("Why Not More ")) {
                String roman = name.substring("Why Not More ".length()).trim();
                int level = RomanNumeral.parse(roman);
                if (level >= 0 && level <= 10) {
                    petState.whyNotMoreLevel = level;
                    foundWhyNot = true;
                }
            }
            if (foundBattle && foundWhyNot) break;
        }
        if (foundBattle || foundWhyNot) saveCurrentProfile();
    }

    // ===== Serialization =====

    private record ProfilePetData(
        @Nullable String currentPetUuid,
        @Nullable List<PetData> pets,
        @Nullable Set<String> sharedPetUuids
    ) {}
}
