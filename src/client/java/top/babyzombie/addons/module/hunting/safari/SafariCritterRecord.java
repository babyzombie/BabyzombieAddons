package top.babyzombie.addons.module.hunting.safari;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLevelEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import top.babyzombie.addons.config.HuntingConfig;
import top.babyzombie.addons.config.HuntingConfig.SafariCritterRecord.CaughtDisplayMode;
import top.babyzombie.addons.config.HuntingConfig.SafariCritterRecord.SafariCritter;
import top.babyzombie.addons.config.ModConfigManager;
import top.babyzombie.addons.config.hud.HudManager;
import top.babyzombie.addons.util.ChatUtils;
import top.babyzombie.addons.util.tracker.HypixelLocationTracker;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Safari 生物捕获记录（原 Wumpa 记录的全面版，覆盖全分区）。
 *
 * <p>机制：本轮每种生物第一次捕获才会给经验，之后不再给。因此记录本轮已捕获
 * 的生物，HUD 按分区（区域名 + 生物）列出所选生物；已捕获按配置的显示方式标注
 * （颜色区分 / 打钩 / 划掉 / 不显示）；所选生物全部捕获后可选弹 Title + 播放声音
 * （同一份列表本轮只提示一次，列表变化后再次全抓可重新提示）。</p>
 *
 * <p>通过 ALLOW_GAME 消息正则匹配 CAPTURE!（含 Hideyho 的 You found 变体）/ LOOT SHARE! 消息，
 * 切换世界时重置。</p>
 */
public final class SafariCritterRecord {

    /** HUD 名称（位置/缩放在 HUD 编辑页保存，沿用原 Wumpa 记录的默认位置） */
    private static final String HUD_NAME = "SafariCritterRecord";

    // ── 正则：个人捕获 CAPTURE! You caught a {name} and gained a {name} Shard! ──
    // 新机制消息有变体（SPARKLING / 首抓经验等），名字段宽匹配，由 matchCritter 收敛
    private static final Pattern CAPTURE_PATTERN =
            Pattern.compile("CAPTURE! You caught (?:a |an |\\d+x )?(.+?)(?:,? and |!|$)");

    // ── 正则：Hideyho 等 "找到" 式捕获 CAPTURE! You found {name}, and ... ──
    private static final Pattern FOUND_PATTERN =
            Pattern.compile("CAPTURE! You found (.+?)(?:,|,? and |!|$)");

    // ── 正则：队友捕获 LOOT SHARE! You received a {shard} from {player} catching a {name}! ──
    private static final Pattern LOOT_SHARE_PATTERN =
            Pattern.compile("LOOT SHARE! You received .+? from .+? catching (?:a |an |\\d+x )?(.+?)(?:,? and |!|$)");

    /** 本轮已捕获（首次捕获）的生物 */
    private static final Set<SafariCritter> captured = EnumSet.noneOf(SafariCritter.class);

    /** 已完成提示对应的列表签名（列表变化后再次全抓会重新提示） */
    private static String notifiedSignature = "";

    private SafariCritterRecord() {}

    public static void init() {
        // ── 切换世界时重置（每轮 Safari 重新统计）──
        ClientLevelEvents.AFTER_CLIENT_LEVEL_CHANGE.register((_, _) -> reset());

        // ── 消息监听：ALLOW_GAME 确保不被取消的消息也能检测 ──
        ClientReceiveMessageEvents.ALLOW_GAME.register((message, overlay) -> {
            if (overlay) return true;
            var cfg = ModConfigManager.get().hunting.safari.critterRecord;
            if (!cfg.enabled) return true;
            if (!HypixelLocationTracker.getInstance().isInSafari()) return true;

            String raw = ChatUtils.stripColor(message.getString());

            // 先匹配个人捕获，再匹配 "找到"式捕获（Hideyho 等），最后队友捕获（LOOT SHARE）
            Matcher m = CAPTURE_PATTERN.matcher(raw);
            if (!m.find()) {
                m = FOUND_PATTERN.matcher(raw);
                if (!m.find()) {
                    m = LOOT_SHARE_PATTERN.matcher(raw);
                    if (!m.find()) return true;
                }
            }
            SafariCritter critter = matchCritter(m.group(1));
            if (critter == null) return true;
            // 只在”本轮第一次抓到该生物“且它还在当前列表里时检查完成
            if (captured.add(critter) && cfg.displayedCritters.contains(critter)) {
                checkComplete(cfg);
            }
            return true;
        });

        // ── HUD 渲染：所选生物按分区归组显示 ──
        HudElementRegistry.attachElementAfter(VanillaHudElements.OVERLAY_MESSAGE,
                Identifier.fromNamespaceAndPath("babyzombieaddons", "safari_critter_record"),
                (context, tickCounter) -> renderHud(context));
    }

    private static void renderHud(GuiGraphicsExtractor context) {
        var cfg = ModConfigManager.get().hunting.safari.critterRecord;
        if (!cfg.enabled) return;
        if (!HypixelLocationTracker.getInstance().isInSafari()) return;

        // 按所选列表首次出现顺序分区归组（HIDE 模式先滤掉已捕获的生物）
        Map<SafariZoneUtil.SafariZone, List<SafariCritter>> byZone = new LinkedHashMap<>();
        for (SafariCritter critter : cfg.displayedCritters) {
            if (cfg.caughtMode == CaughtDisplayMode.HIDE && captured.contains(critter)) continue;
            byZone.computeIfAbsent(critter.zone(), z -> new ArrayList<>()).add(critter);
        }
        if (byZone.isEmpty()) return;

        StringBuilder sb = new StringBuilder(Component.translatable(
                "hud.babyzombieaddons.safariCritterRecord.title").getString());
        for (Map.Entry<SafariZoneUtil.SafariZone, List<SafariCritter>> entry : byZone.entrySet()) {
            sb.append('\n').append(SafariZoneUtil.colorCode(entry.getKey())).append("§l")
                    .append(Component.translatable(
                            "hud.babyzombieaddons.safariCritterRecord.zone." + entry.getKey().name()).getString());
            for (SafariCritter critter : entry.getValue()) {
                sb.append('\n').append(formatCritter(critter, cfg.caughtMode));
            }
        }

        var font = Minecraft.getInstance().font;
        HudManager.drawScaled(context, font, sb.toString(),
                HudManager.x(HUD_NAME), HudManager.y(HUD_NAME), HudManager.scale(HUD_NAME));
    }

    /** 已捕获标注：颜色区分 / 打钩 / 划掉（不显示已在归组前过滤） */
    private static String formatCritter(SafariCritter critter, CaughtDisplayMode mode) {
        String name = Component.translatable(
                "config.babyzombieaddons.option.safariCritterList." + critter.name()).getString();
        boolean caught = captured.contains(critter);
        return switch (mode) {
            case COLOR -> " " + (caught ? "§a" : "§7") + name;
            case CHECK -> caught ? " §a✔ §f" + name : " §7✘ §f" + name;
            case STRIKE -> caught ? " §7§m" + name : " §f" + name;
            case HIDE -> " §f" + name;
        };
    }

    /** 所选列表全部捕获：弹 Title / 播放声音（同一列表只提示一次） */
    private static void checkComplete(HuntingConfig.SafariCritterRecord cfg) {
        StringBuilder sig = new StringBuilder();
        for (SafariCritter critter : cfg.displayedCritters) {
            if (!captured.contains(critter)) return;
            sig.append(critter.name()).append(',');
        }
        if (sig.isEmpty()) return;
        String signature = sig.toString();
        if (signature.equals(notifiedSignature)) return;
        notifiedSignature = signature;

        if (cfg.completeTitle) {
            ChatUtils.showTranslatableTitle(
                    "hud.babyzombieaddons.safariCritterRecord.complete.title",
                    "hud.babyzombieaddons.safariCritterRecord.complete.subtitle",
                    0, 50, 10);
        }
        if (cfg.completeSound) {
            var player = Minecraft.getInstance().player;
            if (player != null) {
                var pos = player.blockPosition();
                player.level().playLocalSound(
                        pos.getX(), pos.getY(), pos.getZ(),
                        SoundEvents.UI_TOAST_CHALLENGE_COMPLETE,
                        SoundSource.PLAYERS, 1.0f, 1.0f, false);
            }
        }
    }

    /** 聊天消息里提取的生物名 → 枚举：先精确匹配，再词边界子串匹配（容错 SPARKLING/首抓/XP 等附加措辞） */
    private static SafariCritter matchCritter(String rawName) {
        if (rawName == null) return null;
        String candidate = rawName.trim().replaceAll("[.!?,;:]+$", "");
        if (candidate.isEmpty()) return null;
        for (SafariCritter critter : SafariCritter.values()) {
            if (critter.chatName().equalsIgnoreCase(candidate)) return critter;
        }
        String haystack = " " + candidate.toLowerCase(Locale.ROOT) + " ";
        for (SafariCritter critter : SafariCritter.values()) {
            if (haystack.contains(" " + critter.chatName().toLowerCase(Locale.ROOT) + " ")) return critter;
        }
        return null;
    }

    private static void reset() {
        captured.clear();
        notifiedSignature = "";
    }
}