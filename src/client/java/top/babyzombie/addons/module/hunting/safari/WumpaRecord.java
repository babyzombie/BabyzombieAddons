package top.babyzombie.addons.module.hunting.safari;

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
import top.babyzombie.addons.util.tracker.HypixelLocationTracker;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Wumpa 记录：追踪雪地区域所有生物的捕捉进度。
 * 抓过全部 8 种雪地生物至少各一只后，Wumpa 才会生成。
 *
 * <p>通过 ALLOW_GAME 消息正则匹配 CAPTURE! / LOOT SHARE! 消息。</p>
 */
public final class WumpaRecord {

    /** 雪地区域生物列表（按显示顺序） */
    private static final String[] SNOW_CREATURES = {
        "Strongarm", "Tepid", "Mantis Shrimp", "Nozzlenose",
        "Polaris", "Shuddersquid", "Billygoat", "Troodon"
    };

    // ── 正则：个人捕获 CAPTURE! You caught a {name} and gained a {name} Shard! ──
    private static final Pattern CAPTURE_PATTERN =
        Pattern.compile("CAPTURE! You caught a (.+?) and gained [an0-9x]{1,2} .+? Shard!");

    // ── 正则：队友捕获 LOOT SHARE! You received a {shard} from {player} catching a {name}! ──
    private static final Pattern LOOT_SHARE_PATTERN =
        Pattern.compile("LOOT SHARE! You received a .+? from .+? catching [an0-9x]{1,2} (.+?)!");

    /** 已捕捉的雪地生物名称集合 */
    private static final Set<String> captured = new LinkedHashSet<>();

    /** Wumpa 已被唤醒（进入 boss 战），不再显示 HUD */
    private static boolean wumpaAwoken;

    private WumpaRecord() {}

    public static void init() {
        // ── 切换世界时重置 ──
        ClientLevelEvents.AFTER_CLIENT_LEVEL_CHANGE.register((_, _) -> reset());

        // ── 消息监听：ALLOW_GAME 确保不被取消的消息也能检测 ──
        ClientReceiveMessageEvents.ALLOW_GAME.register((message, overlay) -> {
            if (overlay) return true;
            if (!ModConfigManager.get().hunting.safari.wumpaRecord) return true;
            if (!HypixelLocationTracker.getInstance().isInSafari()) return true;
            if (!isInSnowArea()) return true;

            String raw = ChatUtils.stripColor(message.getString());

            // Wumpa 已唤醒 → 进入 boss 战，不再显示
            if (raw.equals("The Wumpa has awoken.")) {
                wumpaAwoken = true;
                return true;
            }

            // 先尝试匹配个人捕获
            Matcher m = CAPTURE_PATTERN.matcher(raw);
            if (m.find()) {
                recordCapture(m.group(1));
                return true;
            }

            // 再尝试匹配队友捕获（LOOT SHARE）
            m = LOOT_SHARE_PATTERN.matcher(raw);
            if (m.find()) {
                recordCapture(m.group(1));
                return true;
            }

            return true;
        });

        // ── HUD 渲染 ──
        HudElementRegistry.attachElementAfter(VanillaHudElements.OVERLAY_MESSAGE,
                Identifier.fromNamespaceAndPath("babyzombieaddons", "wumpa_record"),
                (context, tickCounter) -> {
            if (!ModConfigManager.get().hunting.safari.wumpaRecord) return;
            if (wumpaAwoken) return;
            if (!HypixelLocationTracker.getInstance().isInSafari()) return;
            if (!isInSnowArea()) return;

            var font = Minecraft.getInstance().font;
            int x = HudManager.x("WumpaRecord"), y = HudManager.y("WumpaRecord");
            float s = HudManager.scale("WumpaRecord");

            int total = SNOW_CREATURES.length;
            int caught = 0;
            StringBuilder sb = new StringBuilder(
                    Component.translatable("hud.babyzombieaddons.wumpaRecord.title").getString());
            for (String name : SNOW_CREATURES) {
                boolean has = captured.contains(name);
                sb.append('\n');
                if (has) {
                    sb.append("§a✔ ");
                    caught++;
                } else {
                    sb.append("§7✘ ");
                }
                sb.append("§f").append(name);
            }
            if (caught >= total) {
                sb.append('\n').append(
                        Component.translatable("hud.babyzombieaddons.wumpaRecord.canSpawn").getString());
            }

            HudManager.drawScaled(context, font, sb.toString(), x, y, s);
        });
    }

    private static void reset() {
        captured.clear();
        wumpaAwoken = false;
    }

    private static void recordCapture(String name) {
        if (name == null || name.isBlank()) return;
        for (String creature : SNOW_CREATURES) {
            if (creature.equalsIgnoreCase(name)) {
                captured.add(creature);
                return;
            }
        }
    }

    // ── 雪地区域判断 ──
    // 雪地：x <= -52 且 z <= -2
    // 排除 Safari 中心安全区：x >= -72 且 z >= -24
    private static boolean isInSnowArea() {
        var player = Minecraft.getInstance().player;
        if (player == null) return false;
        var pos = player.blockPosition();
        int x = pos.getX();
        int z = pos.getZ();

        // 必须在雪地范围
        if (!(x <= -52 && z <= -2)) return false;

        // 排除中心安全区
        if (x >= -72 && z >= -24) return false;

        return true;
    }
}
