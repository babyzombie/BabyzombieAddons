package top.babyzombie.addons.module.kuudra;

import com.google.gson.reflect.TypeToken;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import top.babyzombie.addons.config.ModConfigManager;
import top.babyzombie.addons.config.hud.HudManager;
import top.babyzombie.addons.event.ContainerClickEvents;
import com.google.common.collect.LinkedHashMultimap;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.properties.PropertyMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ResolvableProfile;
import top.babyzombie.addons.util.ChatUtils;
import top.babyzombie.addons.util.DataPersistence;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.UUID;
import top.babyzombie.addons.util.tracker.HypixelLocationTracker;

import java.util.HashMap;
import java.util.Map;

/**
 * 箱子计数器 — 打完 Kuudra +1，开箱子 -1，支持持久化和手动归零。
 * 通过 Croesus/Vesuvius 菜单中 Paid Chest 头颅检测开箱。
 */
public final class ChestCounter {
    private ChestCounter() {}

    private static final int MAX_CHESTS = 60;
    private static final long PENDING_TIMEOUT_MS = 30_000;
    private static final String PAID_CHEST_TEXTURE_URL = "http://textures.minecraft.net/texture/d90a7dec36b1421c95b699a39fdb500b3e74648fee5821e3fd05668ee01ce10c";
    private static ItemStack chestIcon;
    private static boolean pendingChestOpen;
    private static long pendingSinceMs;

    // Keyed by "uuid_profileId"
    private static Map<String, Integer> allCounts = new HashMap<>();
    private static String currentKey;
    private static int count;
    private static boolean dirty;

    public static int getCount() { return count; }

    /** Reset current profile counter to 0 (called from config button). */
    public static void resetCounter() {
        count = 0;
        if (currentKey != null) allCounts.put(currentKey, 0);
        dirty = true;
        save();
    }

    private static ItemStack getChestIcon() {
        if (chestIcon == null) chestIcon = createChestIcon();
        return chestIcon;
    }

    private static ItemStack createChestIcon() {
        var stack = new ItemStack(Items.PLAYER_HEAD);
        var uuid = UUID.fromString("cde08b6e-4bc9-4b36-b24d-75e23e0bb4ed");
        var textureData = "{\"textures\":{\"SKIN\":{\"url\":\"" + PAID_CHEST_TEXTURE_URL + "\"}}}";
        var encoded = Base64.getEncoder().encodeToString(textureData.getBytes(StandardCharsets.UTF_8));
        var multimap = LinkedHashMultimap.<String, Property>create();
        multimap.put("textures", new Property("textures", encoded, null));
        var gp = new GameProfile(uuid, "", new PropertyMap(multimap));
        stack.set(DataComponents.PROFILE, ResolvableProfile.createResolved(gp));
        return stack;
    }

    public static void init() {
        load();

        // Persist on game close
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> save());

        // Update key when profile changes
        ClientReceiveMessageEvents.GAME.register((msg, overlay) -> {
            if (overlay) return;
            updateKey();
        });

        // Run end → +1
        ClientReceiveMessageEvents.GAME.register((msg, overlay) -> {
            if (overlay || !HypixelLocationTracker.getInstance().isInKuudra()) return;
            var cfg = ModConfigManager.get().kuudra;
            if (!cfg.chestCounterCfg.enabled) return;

            String text = ChatUtils.stripColor(msg.getString());
            if (text.equals("                               KUUDRA DOWN!") || text.contains("Good job everyone")) {
                if (count < MAX_CHESTS) {
                    count++;
                    dirty = true;

                    var cc = ModConfigManager.get().kuudra.chestCounterCfg;
                    int remaining = MAX_CHESTS - count;

                    if (count % 10 == 0 && count > 0 && count < MAX_CHESTS) {
                        playSound();
                        if (cc.partyAnnounce)
                            ChatUtils.sendCommand("pc Chests: " + count + "/" + MAX_CHESTS + " (" + remaining + " left)");
                    }
                    if (count == 59) {
                        playSound();
                        if (cc.partyAnnounce)
                            ChatUtils.sendCommand("pc Run 59/60, dont forget to !dt next run.");
                    }
                    if (count >= MAX_CHESTS) {
                        ChatUtils.showTranslatableTitle("kuudra.chest.full",0,80,20);
                        playSound();
                        if (cc.partyAnnounce)
                            ChatUtils.sendCommand("pc 60/60 chests, opening soon!");
                    }
                }
            }
        });

        // Chest open → -1: two-step detection
        // Step 1: click buy button (slot 31) in a chest reward GUI
        ContainerClickEvents.BEFORE_MOUSE_CLICK.register((screen, slot, event) -> {
            var cfg = ModConfigManager.get().kuudra;
            if (!cfg.chestCounterCfg.enabled) return false;
            if (slot == null || slot.index != 31) return false;
            if (!(screen instanceof net.minecraft.client.gui.screens.inventory.AbstractContainerScreen<?> cs)) return false;

            String title = ChatUtils.stripColor(cs.getTitle().getString());
            if (!isChestRewardGUI(title)) return false;

            if (!slot.hasItem()) return true; // empty slot, allow (packet may not have arrived yet)
            if (isAlreadyOpened(slot.getItem())) return false;

            // Is this a "Click to open!" buy action?
            if (isBuyAction(slot.getItem())) {
                pendingChestOpen = true;
                pendingSinceMs = System.currentTimeMillis();
            }
            return false;
        });

        // Step 2: chat message confirms the chest opened
        ClientReceiveMessageEvents.GAME.register((msg, overlay) -> {
            if (overlay || !pendingChestOpen) return;
            String text = ChatUtils.stripColor(msg.getString());
            // Expire pending if too old
            if (System.currentTimeMillis() - pendingSinceMs > PENDING_TIMEOUT_MS) {
                pendingChestOpen = false;
                return;
            }
            if (text.contains("PAID CHEST REWARDS") || text.contains("FREE CHEST REWARDS")) {
                pendingChestOpen = false;
                if (count > 0) {
                    count--;
                    dirty = true;
                    ChatUtils.sendCommand("pc Chests: " + count + "/" + MAX_CHESTS);
                }
            }
        });

        // HUD
        HudElementRegistry.attachElementAfter(VanillaHudElements.OVERLAY_MESSAGE,
                Identifier.fromNamespaceAndPath("babyzombieaddons", "kuudra_chest_counter"),
                (context, tickCounter) -> {
                    if (!ModConfigManager.get().kuudra.chestCounterCfg.enabled) return;
                    if (count <= 0) return;
                    if (dirty) { save(); dirty = false; }

                    int x = HudManager.x("ChestCounter"), y = HudManager.y("ChestCounter");
                    float s = HudManager.scale("ChestCounter");
                    String color = count >= 60 ? "§c" : count >= 50 ? "§e" : "§a";

                    // Draw chest icon
                    context.item(getChestIcon(), Math.round(x / s), Math.round(y / s));

                    // Draw count text (offset right of icon)
                    HudManager.drawScaled(context, Minecraft.getInstance().font,
                            color + "  " + count + "§7/" + MAX_CHESTS, x + 16, y, s);
                });
    }

    private static void updateKey() {
        var t = HypixelLocationTracker.getInstance();
        String uuid = t.getUuid();
        String profile = t.getProfileId();
        if (uuid == null || profile == null) return;
        String key = uuid + "_" + profile;
        if (!key.equals(currentKey)) {
            // Save old key
            if (currentKey != null) allCounts.put(currentKey, count);
            currentKey = key;
            count = allCounts.getOrDefault(key, 0);
        }
    }

    private static void load() {
        var type = new TypeToken<Map<String, Integer>>(){}.getType();
        Map<String, Integer> loaded = DataPersistence.load("chest_counter.json", type);
        allCounts = loaded != null ? loaded : new HashMap<>();
    }

    private static boolean isChestRewardGUI(String title) {
        return title.contains("Chest") || title.contains("Rewards");
    }
    private static boolean isAlreadyOpened(ItemStack stack) {
        for (var line : stack.getTooltipLines(net.minecraft.world.item.Item.TooltipContext.EMPTY, null, net.minecraft.world.item.TooltipFlag.NORMAL)) {
            String s = ChatUtils.stripColor(line.getString());
            if (s.contains("Already opened") || s.contains("Chest already opened")) return true;
        }
        return false;
    }
    private static boolean isBuyAction(ItemStack stack) {
        for (var line : stack.getTooltipLines(net.minecraft.world.item.Item.TooltipContext.EMPTY, null, net.minecraft.world.item.TooltipFlag.NORMAL)) {
            if (ChatUtils.stripColor(line.getString()).contains("Click to open")) return true;
        }
        return false;
    }

    private static void playSound() {
        var cc = ModConfigManager.get().kuudra.chestCounterCfg;
        if (!cc.sound) return;
        var player = Minecraft.getInstance().player;
        if (player != null) {
            player.playSound(net.minecraft.sounds.SoundEvents.PLAYER_LEVELUP, 1f, 1f);
        }
    }

    private static void save() {
        if (currentKey != null) allCounts.put(currentKey, count);
        DataPersistence.save("chest_counter.json", allCounts);
    }
}
