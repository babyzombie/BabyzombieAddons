/*
 * 部分内容改编自 IQ Addons (https://github.com/iqaddons/IQ, Apache License 2.0),
 * 已由 BabyzombieAddons 修改;详见 THIRD_PARTY_NOTICES.txt。
 */
package top.babyzombie.addons.module.kuudra;

import com.google.gson.reflect.TypeToken;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
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
    private static final String PAID_CHEST_TEXTURE_URL = "https://textures.minecraft.net/texture/d90a7dec36b1421c95b699a39fdb500b3e74648fee5821e3fd05668ee01ce10c";
    private static ItemStack chestIcon;
    private static boolean pendingChestOpen;
    private static long pendingSinceMs;

    // Keyed by "uuid_profileId"
    private static Map<String, Integer> allCounts = new HashMap<>();
    private static String currentKey;
    private static int count;

    public static int getCount() { return count; }

    /** Reset current profile counter to 0 (called from config button). */
    public static void resetCounter() {
        count = 0;
        if (currentKey != null) allCounts.put(currentKey, 0);
        save();
    }

    /** 一场 Kuudra 结束（无论输赢）+1，并处理里程碑提醒（打完时发队伍消息）。 */
    public static void onRunEnd() {
        if (!HypixelLocationTracker.getInstance().isInKuudra()) return;
        if (!ModConfigManager.get().kuudra.chestCounterCfg.enabled) return;
        if (count >= MAX_CHESTS) return;
        count++;
        save();

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
        ClientReceiveMessageEvents.ALLOW_GAME.register((msg, overlay) -> {
            if (overlay) return true;
            updateKey();
            return true;
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
        ClientReceiveMessageEvents.ALLOW_GAME.register((msg, overlay) -> {
            if (overlay || !pendingChestOpen) return true;
            String text = ChatUtils.stripColor(msg.getString());
            // Expire pending if too old
            if (System.currentTimeMillis() - pendingSinceMs > PENDING_TIMEOUT_MS) {
                pendingChestOpen = false;
                return true;
            }
            if (text.contains("PAID CHEST REWARDS") || text.contains("FREE CHEST REWARDS")) {
                pendingChestOpen = false;
                if (count > 0) {
                    count--;
                    save();
                }
            }
            return true;
        });

        // HUD（世界内）
        HudElementRegistry.attachElementAfter(VanillaHudElements.OVERLAY_MESSAGE,
                Identifier.fromNamespaceAndPath("babyzombieaddons", "kuudra_chest_counter"),
                (context, tickCounter) -> {
                    if (!ModConfigManager.get().kuudra.chestCounterCfg.enabled) return;
                    if (count <= 0) return;
                    if (!shouldDisplay()) return;

                    int x = HudManager.x("ChestCounter"), y = HudManager.y("ChestCounter");
                    float s = HudManager.scale("ChestCounter");
                    String color = count >= 60 ? "§c" : count >= 50 ? "§e" : "§a";
                    String text = color + count + "§7/" + MAX_CHESTS;

                    // 图标和文字共用同一缩放矩阵，文字紧贴 16x16 图标
                    var ps = context.pose();
                    ps.pushMatrix();
                    ps.translate((float) x, (float) y);
                    ps.scale(s, s);
                    context.item(getChestIcon(), 0, 0);
                    context.text(Minecraft.getInstance().font, text, 16, 4, 0xFFFFFFFF, true);
                    ps.popMatrix();
                });
    }

    /** 显示模式：只在 Kuudra / 包含 Crimson Isle 和地牢大厅 / 所有地方。 */
    private static boolean shouldDisplay() {
        var t = HypixelLocationTracker.getInstance();
        return switch (ModConfigManager.get().kuudra.chestCounterCfg.displayMode) {
            case KUUDRA_ONLY -> t.isInKuudra();
            case INCLUDE_CRIMSON_DUNGEON ->
                    t.isInKuudra() || t.isInCrimson() || "Dungeon Hub".equals(t.getLocation());
            case EVERYWHERE -> true;
        };
    }

    /** 当前点击触发的指令（按所在位置）。 */
    private static String clickCommand() {
        var t = HypixelLocationTracker.getInstance();
        String loc = t.getLocation();
        if ("Dragontail".equals(loc)) return "/bz Enchanted Red Sand";
        if ("Scarleton".equals(loc)) return "/bz Enchanted Mycelium";
        if (!t.isInCrimson()) return "/warp crimson";
        return "/warp kuudra";
    }

    /** 在容器/背包页面渲染计数 HUD，并处理 hover 提示（独立开关 chestCounterInteract）。 */
    public static void renderOnScreen(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        var cfg = ModConfigManager.get().kuudra.chestCounterCfg;
        if (!cfg.enabled || !cfg.interact) return;
        if (count <= 0) return;
        if (!(Minecraft.getInstance().screen instanceof AbstractContainerScreen)) return;
        if (!shouldDisplay()) return;

        int x = HudManager.x("ChestCounter"), y = HudManager.y("ChestCounter");
        float s = HudManager.scale("ChestCounter");
        String color = count >= 60 ? "§c" : count >= 50 ? "§e" : "§a";
        String text = color + count + "§7/" + MAX_CHESTS;
        var font = Minecraft.getInstance().font;

        var ps = graphics.pose();
        ps.pushMatrix();
        ps.translate((float) x, (float) y);
        ps.scale(s, s);
        graphics.item(getChestIcon(), 0, 0);
        graphics.text(font, text, 16, 4, 0xFFFFFFFF, true);
        ps.popMatrix();

        // Hover：显示 T5 钥匙材料需求 + 点击指令
        // HudManager 的 x/y 是 GUI 坐标（不缩放），scale 只缩放内容
        int hx = x, hy = y;
        int hw = Math.round((16 + font.width(text)) * s), hh = Math.round(16 * s);
        if (mouseX >= hx && mouseX <= hx + hw && mouseY >= hy && mouseY <= hy + hh) {
            drawTooltip(graphics, mouseX, mouseY, count);
        }
    }

    /** 容器/背包页面点击 HUD 区域 → 触发指令。返回 true 表示已处理（取消原点击）。 */
    public static boolean onScreenClick(MouseButtonEvent event) {
        if (event.button() != 0 && event.button() != 1) return false; // 仅左键/右键
        var cfg = ModConfigManager.get().kuudra.chestCounterCfg;
        if (!cfg.enabled || !cfg.interact) return false;
        if (count <= 0) return false;
        if (!(Minecraft.getInstance().screen instanceof AbstractContainerScreen)) return false;
        if (!shouldDisplay()) return false;

        int x = HudManager.x("ChestCounter"), y = HudManager.y("ChestCounter");
        float s = HudManager.scale("ChestCounter");
        String color = count >= 60 ? "§c" : count >= 50 ? "§e" : "§a";
        String text = color + count + "§7/" + MAX_CHESTS;
        var font = Minecraft.getInstance().font;

        // HudManager 的 x/y 是 GUI 坐标（不缩放），scale 只缩放内容
        int hx = x, hy = y;
        int hw = Math.round((16 + font.width(text)) * s), hh = Math.round(16 * s);
        int mx = (int) event.x(), my = (int) event.y();
        if (mx >= hx && mx <= hx + hw && my >= hy && my <= hy + hh) {
            if (event.button() == 1) {
                ChatUtils.sendCommand("/warp dungeon_hub"); // 右键 → 地牢大厅
            } else {
                ChatUtils.sendCommand(clickCommand());
            }
            return true;
        }
        return false;
    }

    /** 材料需求提示（按 T5 钥匙价格），用原版 tooltip 样式渲染。 */
    private static void drawTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY, int count) {
        String cmd = clickCommand();
        double coinsM = 2.4 * count;
        String coins = coinsM >= 1000 ? String.format("%.0fm", coinsM) : String.format("%.1fm", coinsM);
        java.util.List<net.minecraft.network.chat.Component> lines = java.util.List.of(
                net.minecraft.network.chat.Component.literal(String.format("§6%s coins", coins)),
                net.minecraft.network.chat.Component.literal(String.format("§e%d §7x §aEnchanted Mycelium/Red Sand", 80 * count)),
                net.minecraft.network.chat.Component.literal(String.format("§e%d §7x §6Nether Stars", 2 * count)),
                net.minecraft.network.chat.Component.literal(String.format("§aClick: §f%s §8(RMB: /warp dungeon_hub)", cmd))
        );
        // 原版 tooltip 渲染（带边框样式）
        graphics.setTooltipForNextFrame(Minecraft.getInstance().font, lines,
                java.util.Optional.empty(), mouseX, mouseY, null);
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
            save(); // profile 切换也是数据更新点，旧计数落盘
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
