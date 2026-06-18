package top.babyzombie.addons.util.pet;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLevelEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemLore;
import org.jetbrains.annotations.Nullable;
import top.babyzombie.addons.event.ContainerClickEvents;
import top.babyzombie.addons.util.ChatUtils;
import top.babyzombie.addons.util.DataPersistence;
import top.babyzombie.addons.util.ItemUtils;
import top.babyzombie.addons.util.ScreenHelper;
import top.babyzombie.addons.util.tracker.HypixelLocationTracker;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
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

    private String currentProfileKey;
    private final List<PetData> pets = new ArrayList<>();
    private PetData currentPet;

    private PetManager() {}

    public static PetManager getInstance() { return INSTANCE; }

    // ===== Init =====

    public void init() {
        loadProfile(profileKey());
        registerProfileSwitch();
        registerAutoPetRule();
        registerPetItemInteract();
        registerGuiClick();
        registerPetsMenuScan();
        registerDisconnectSave();
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
        currentPet = null;
        if (loaded != null) {
            if (loaded.pets != null) pets.addAll(loaded.pets);
            currentPet = loaded.currentPet;
        }
    }

    public void saveCurrentProfile() {
        if (currentProfileKey == null) return;
        DataPersistence.save(currentProfileKey, "pets.json",
            new ProfilePetData(currentPet, new ArrayList<>(pets)));
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
        if (currentPet != null && uuid.equals(currentPet.uuid())) {
            currentPet = null;
        }
        saveCurrentProfile();
    }

    public void setCurrentPet(@Nullable PetData pet) {
        this.currentPet = pet;
        saveCurrentProfile();
    }

    @Nullable
    public PetData getCurrentPet() { return currentPet; }

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
                    || !ChatUtils.stripColor(petHeld).equals(ChatUtils.stripColor(heldItemName)))
                    continue;
            }
            return p;
        }
        return null;
    }

    // ===== NBT Helpers =====

    /** Extract petInfo JSON string from an ItemStack. */
    @Nullable
    public static String getPetInfoFromStack(ItemStack stack) {
        var customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData == null) return null;
        var tag = customData.copyTag();
        var extra = tag.getCompound("ExtraAttributes").orElse(null);
        if (extra == null) return null;
        return extra.getString("petInfo").orElse(null);
    }

    /** Extract item UUID from ExtraAttributes. */
    @Nullable
    public static String getItemUuid(ItemStack stack) {
        var customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData == null) return null;
        var tag = customData.copyTag();
        var extra = tag.getCompound("ExtraAttributes").orElse(null);
        if (extra == null) return null;
        return extra.getString("uuid").orElse(null);
    }

    /** Get the held item's display name from its SkyBlock ID. */
    @Nullable
    private static String getHeldItemDisplayName(@Nullable String heldItem) {
        if (heldItem == null) return null;
        return heldItem.replace("PET_ITEM_", "").replace("_", " ").toLowerCase(Locale.ROOT);
    }

    // ===== Event Handlers =====

    /** Profile switch via chat message "Profile ID: ..." */
    private void registerProfileSwitch() {
        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            if (overlay) return;
            if (!HypixelLocationTracker.getInstance().isInSkyblock()) return;
            Matcher m = PROFILE_ID_PATTERN.matcher(message.getString());
            if (m.find()) {
                String uuid = HypixelLocationTracker.getInstance().getUuid();
                if (uuid == null) return;
                loadProfile(uuid + "_" + m.group(1));
            }
        });
    }

    /** Detect Autopet rule equip from action bar messages. */
    private void registerAutoPetRule() {
        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            if (!overlay) return;
            if (!HypixelLocationTracker.getInstance().isInSkyblock()) return;
            String text = ChatUtils.stripColor(message.getString());
            Matcher m = AUTO_PET_PATTERN.matcher(text);
            if (!m.find()) return;

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
        });
    }

    /** Right-click with a PET item adds it to the list. */
    private void registerPetItemInteract() {
        UseItemCallback.EVENT.register((player, world, hand) -> {
            if (!HypixelLocationTracker.getInstance().isInSkyblock())
                return InteractionResult.PASS;
            ItemStack stack = player.getItemInHand(hand);
            if (!"PET".equals(ItemUtils.getSkyblockId(stack)))
                return InteractionResult.PASS;
            String petInfo = getPetInfoFromStack(stack);
            if (petInfo != null) {
                addPet(PetData.fromPetInfo(petInfo));
            }
            return InteractionResult.PASS;
        });
    }

    /** GUI mouse click: right-click removes pet, left-click selects it. */
    private void registerGuiClick() {
        ContainerClickEvents.BEFORE_MOUSE_CLICK.register((screen, slot, button) -> {
            if (!HypixelLocationTracker.getInstance().isInSkyblock()) return false;
            if (slot == null || !slot.hasItem()) return false;
            ItemStack stack = slot.getItem();
            if (!"PET".equals(ItemUtils.getSkyblockId(stack))) return false;

            String uuid = getItemUuid(stack);
            if (uuid == null) return false;

            if (button == 1) {
                removePet(uuid);
            } else if (button == 0) {
                for (PetData p : pets) {
                    if (uuid.equals(p.uuid())) {
                        setCurrentPet(p);
                        break;
                    }
                }
            }
            return false;
        });
    }

    /** Scan the Pets menu when it opens to capture all pet items. */
    private void registerPetsMenuScan() {
        ScreenEvents.AFTER_INIT.register((client, screen, sw, sh) -> {
            if (!HypixelLocationTracker.getInstance().isInSkyblock()) return;
            if (!(screen instanceof AbstractContainerScreen<?> cs)) return;
            String title = ChatUtils.stripColor(cs.getTitle().getString());
            if (!title.startsWith("Pets")) return;

            AtomicBoolean done = new AtomicBoolean(false);
            ClientTickEvents.END_CLIENT_TICK.register(tickClient -> {
                if (done.getAndSet(true)) return;
                if (ScreenHelper.getCurrent() != cs) return;
                scanPetsContainer(cs);
            });
        });
    }

    /** Save on disconnect or world unload. */
    private void registerDisconnectSave() {
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> saveAll());
        ClientLevelEvents.AFTER_CLIENT_LEVEL_CHANGE.register((client, world) -> {
            if (world == null) saveAll();
        });
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
        for (Slot slot : screen.getMenu().slots) {
            ItemStack stack = slot.getItem();
            if (stack.isEmpty()) continue;
            if (!"PET".equals(ItemUtils.getSkyblockId(stack))) continue;

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
            addPet(PetData.fromPetInfo(petInfo));

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

    // ===== Serialization =====

    private record ProfilePetData(
        @Nullable PetData currentPet,
        @Nullable List<PetData> pets
    ) {}
}
