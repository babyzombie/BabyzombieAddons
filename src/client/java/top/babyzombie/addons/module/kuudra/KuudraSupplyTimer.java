package top.babyzombie.addons.module.kuudra;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLevelEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import top.babyzombie.addons.config.ModConfigManager;
import top.babyzombie.addons.config.hud.HudManager;
import top.babyzombie.addons.util.ChatUtils;
import top.babyzombie.addons.util.ServerTick;
import top.babyzombie.addons.util.tracker.HypixelLocationTracker;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 补给拾取计时器 — 追踪谁在什么时候放置了补给箱。
 * 支持聊天消息替换和 HUD 两种显示模式。
 */
public final class KuudraSupplyTimer {
    private KuudraSupplyTimer() {}

    // Matches: "DarkJota recovered a supply! (1/6)"
    // toLegacyString 会在颜色切换处插入格式码（如 §r），pattern 需允许 recovered 前的格式码
    private static final Pattern PLACE_PATTERN = Pattern.compile("(.+?)(?:§.)*?recovered.*?\\((\\d)/6\\)");

    private record Entry(String playerName, int supplyNumber, long placedAtMs) {}

    private static final List<Entry> entries = new ArrayList<>();
    private static long suppliesStartMs;

    public static long getStartMs() { return suppliesStartMs; }

    public static void reset() {
        entries.clear();
        suppliesStartMs = 0;
    }

    public static void init() {
        ClientLevelEvents.AFTER_CLIENT_LEVEL_CHANGE.register((client, world) -> reset());

        // Chat message: modify to include timing, or record and let through
        ClientReceiveMessageEvents.MODIFY_GAME.register((message, overlay) -> {
            var cfg = ModConfigManager.get().kuudra.phase1;
            if (!cfg.supplyPlaceTimerChat && !cfg.supplyPlaceTimerHud) return message;
            if (overlay || !HypixelLocationTracker.getInstance().isInKuudra()) return message;

            // getString() 不含颜色代码，用 toLegacyString 保留玩家名原色
            Matcher m = PLACE_PATTERN.matcher(ChatUtils.toLegacyString(message));
            if (!m.find()) return message;

            // 去掉 rank 文本（[MVP] 等）但保留其格式码——名字颜色常继承自 rank
            String playerName = m.group(1).replaceAll("^((?:§.)*)\\[[^]]*]\\s*", "$1").trim();
            int supplyNum = Integer.parseInt(m.group(2));
            long now = ServerTick.getTime(); // 服务器 tick 时间，不受本地时钟/延迟影响

            // Record entry
            if (suppliesStartMs == 0) suppliesStartMs = now;
            entries.add(new Entry(playerName, supplyNum, now));

            if (cfg.supplyPlaceTimerChat) {
                double elapsed = (now - suppliesStartMs) / 1000.0;
                return Component.literal(
                        String.format("%s §arecovered a supply! §a(%d/6) §e%.2fs",
                                playerName, supplyNum, elapsed));
            }
            return message;
        });

        // Reset on Kuudra start
        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            if (!ModConfigManager.get().kuudra.phase1.supplyPlaceTimerHud) return;
            if (overlay) return;
            String text = message.getString();
            if (text.contains("Okay adventurers, I will go and fish up Kuudra")) {
                entries.clear();
                suppliesStartMs = ServerTick.getTime();
            }
        });

        // HUD
        HudElementRegistry.attachElementAfter(VanillaHudElements.OVERLAY_MESSAGE,
                Identifier.fromNamespaceAndPath("babyzombieaddons", "kuudra_supply_times"),
                (context, tickCounter) -> {
                    if (!ModConfigManager.get().kuudra.phase1.supplyPlaceTimerHud) return;
                    if (entries.isEmpty()) return;

                    var font = Minecraft.getInstance().font;
                    int x = HudManager.x("SupplyTimes"), y = HudManager.y("SupplyTimes");
                    float s = HudManager.scale("SupplyTimes");

                    StringBuilder sb = new StringBuilder();
                    sb.append("§b§lSupply Times §8[§a").append(entries.size()).append("§8/§a6§8]");

                    for (Entry e : entries) {
                        double sec = (e.placedAtMs - suppliesStartMs) / 1000.0;
                        String timeColor = timeColor(sec);
                        sb.append('\n').append(String.format("%s§8(%d/6) %s%.2fs",
                                e.playerName, e.supplyNumber, timeColor, sec));
                    }

                    HudManager.drawScaled(context, font, sb.toString(), x, y, s);
                });
    }

    private static String timeColor(double seconds) {
        if (seconds <= 19.0) return "§f§l";
        if (seconds <= 20.0) return "§5§l";
        if (seconds <= 22.6) return "§9§l";
        if (seconds <= 25.0) return "§a§l";
        if (seconds <= 28.0) return "§2§l";
        if (seconds <= 32.0) return "§e§l";
        return "§c§l";
    }
}
