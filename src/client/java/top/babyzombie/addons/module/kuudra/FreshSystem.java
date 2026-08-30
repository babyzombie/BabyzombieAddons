/*
 * 部分内容改编自 IQ Addons (https://github.com/iqaddons/IQ, Apache License 2.0),
 * 已由 BabyzombieAddons 修改;详见 THIRD_PARTY_NOTICES.txt。
 */
package top.babyzombie.addons.module.kuudra;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLevelEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.Identifier;
import net.minecraft.world.scores.DisplaySlot;
import top.babyzombie.addons.config.ModConfigManager;
import top.babyzombie.addons.config.hud.HudManager;
import top.babyzombie.addons.util.ChatUtils;
import top.babyzombie.addons.util.ServerTick;
import top.babyzombie.addons.util.render.GlowController;
import top.babyzombie.addons.util.render.RenderPhaseRegister;
import top.babyzombie.addons.util.render.WorldTextRenderer;
import top.babyzombie.addons.util.tracker.HypixelLocationTracker;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Fresh 系统 — 消息、计时、高亮（穿墙）、头顶倒计时、建造进度 HUD。
 */
public final class FreshSystem {
    private FreshSystem() {}

    private static final long FRESH_DURATION_MS = 10_000;
    private static final int FRESH_COLOR = 0xFF55FFFF; // ARGB
    private static final long BUILD_START_COUNTDOWN_MS = 6200;
    private static final Pattern SUPPLY_PLACE_PATTERN = Pattern.compile(".+? recovered.*?\\((\\d)/6\\)");

    // Scoreboard: "Protect Elle (69%)"
    private static final Pattern BUILD_PROGRESS_PATTERN = Pattern.compile("Protect Elle\\s*\\((\\d+)%\\)");

    private record FreshEntry(long startMs, String playerName) {}
    private static final Map<Integer, FreshEntry> freshPlayers = new ConcurrentHashMap<>();
    private static final List<FreshEntry> freshHistory = new ArrayList<>();
    private static long buildStartMs;

    private static int buildProgress = 0;
    private static boolean inBuildPhase;
    private static long buildCountdownEndMs = -1; // 补给 6/6 放完后的建造开始倒计时
    private static long lastCountdownTickMs = -1;
    private static final long LAG_COMPENSATION_THRESHOLD_MS = 120L;
    private static final long MAX_COMPENSATION_PER_TICK_MS = 1500L;

    public static void init() {
        ClientLevelEvents.AFTER_CLIENT_LEVEL_CHANGE.register((client, world) -> {
            clearAll();
            freshHistory.clear();
            buildStartMs = 0;
            buildProgress = 0;
            inBuildPhase = false;
            buildCountdownEndMs = -1;
        });

        // ── Chat detection ──
        ClientReceiveMessageEvents.ALLOW_GAME.register((message, overlay) -> {
            if (overlay || !HypixelLocationTracker.getInstance().isInKuudra()) return true;
            String text = ChatUtils.stripColor(message.getString());

            // P2 start（不清倒计时：让它自然走完，避免提前变进度条）
            if (KuudraChatLines.isSuppliesCollected(text)) {
                inBuildPhase = true;
                buildProgress = 0;
                buildStartMs = ServerTick.getTime();
                return true;
            }
            // P2 end（清除建造开始倒计时，避免结束后仍显示 0.00s）
            if (KuudraChatLines.isBallistaReady(text)) {
                inBuildPhase = false;
                buildCountdownEndMs = -1;
                clearAll();
                return true;
            }

            // 补给 6/6 放完 → 建造阶段开始倒计时
            Matcher sm = SUPPLY_PLACE_PATTERN.matcher(text);
            if (sm.find() && sm.group(1).equals("6")) {
                buildCountdownEndMs = ServerTick.getTime() + BUILD_START_COUNTDOWN_MS;
            }

            // Self fresh：完整消息 "Your Fresh Tools Perk bonus doubles your building speed for the next 10 seconds!"，
            // 消息无前缀，startsWith 防止玩家在聊天栏复制这句造成误判
            if (text.startsWith("Your Fresh Tools Perk bonus doubles your building speed")) {
                onSelfFresh();
                return true;
            }

            // Party fresh: detect both our own and IQ's "FRESH!" messages
            // IQ pattern: Party > [rank] PlayerName: [IQ] FRESH!
            // Our pattern: Party > PlayerName: FRESH!
            // 用去色后的纯文本匹配（更可靠），拿到名字后从 tab 取带颜色的名字；
            // [^:]+? 兼容带 emoji 的玩家名
            Matcher fm = Pattern.compile(
                    "Party > (?:\\[[^]]+] )?([^:]+?): (?:\\[IQ] )?FRESH\\b",
                    Pattern.CASE_INSENSITIVE).matcher(text);
            if (fm.find() && inBuildPhase) {
                // 聊天名与 Tab 名形态不同（聊天含 rank、Tab 含 [等级]+图标），
                // 统一归一化后再进查找，否则 findColoredName / findPlayerByName 都匹配不上
                String plainName = normalizePlayerName(fm.group(1));
                String coloredName = findColoredName(plainName);
                trackFresh(0, coloredName != null ? coloredName : plainName);
            }
            return true;
        });

        // ── Tick: cleanup expired, update build progress ──
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            long now = ServerTick.getTime();
            Iterator<Map.Entry<Integer, FreshEntry>> it = freshPlayers.entrySet().iterator();
            while (it.hasNext()) {
                var entry = it.next();
                if (now - entry.getValue().startMs >= FRESH_DURATION_MS) {
                    var p = findPlayerById(entry.getKey());
                    if (p != null) GlowController.setGlow(p, false);
                    it.remove();
                }
            }

            // Read build progress from scoreboard every 20 ticks
            if (inBuildPhase) {
                buildProgress = readBuildProgress(client);
            }

            // 倒计时兜底：6/6 放置完成但消息未匹配到时，用补给计数轮询触发；
            // 只在 P1 阶段（inSuppliesPhase）触发，避免 P2 结束后用残留计数重新计时
            if (!inBuildPhase && buildCountdownEndMs < 0
                    && KuudraSupplyProgressHUD.isInSuppliesPhase()
                    && KuudraSupplyProgressHUD.getSupplyCount() >= 6) {
                buildCountdownEndMs = ServerTick.getTime() + BUILD_START_COUNTDOWN_MS;
            }

            // 卡顿补偿：掉帧导致实际经过时间超过 50ms/tick 时，把倒计时终点往后推
            if (buildCountdownEndMs > 0 && lastCountdownTickMs > 0) {
                buildCountdownEndMs = compensateLag(buildCountdownEndMs, lastCountdownTickMs, now);
            }
            lastCountdownTickMs = now;
        });

        // ── World render: fresh countdown above heads ──
        RenderPhaseRegister.register(ctx -> {
            long now = ServerTick.getTime();
            for (var entry : freshPlayers.entrySet()) {
                var player = findPlayerById(entry.getKey());
                if (player == null || player.isRemoved()) continue;
                long remaining = FRESH_DURATION_MS - (now - entry.getValue().startMs);
                if (remaining <= 0) continue;

                double sec = remaining / 1000.0;
                int color; // ARGB
                if (sec > 6) color = 0xFF55FF55;
                else if (sec > 3) color = 0xFFFFFF55;
                else color = 0xFFFF5555;

                double y = player.getY() + 2.5;
                WorldTextRenderer.renderString(ctx, String.format("%.1fs", sec),
                        player.getX(), y, player.getZ(), color, 0.05f, true);
            }
        });

        // ── HUD: build progress + build start countdown ──
        HudElementRegistry.attachElementAfter(VanillaHudElements.OVERLAY_MESSAGE,
                Identifier.fromNamespaceAndPath("babyzombieaddons", "kuudra_build_progress"),
                (context, tickCounter) -> {
                    var cfg = ModConfigManager.get().kuudra.phase2;
                    if (!cfg.buildProgressHud) return;

                    var font = Minecraft.getInstance().font;
                    int x = HudManager.x("BuildProgress"), y = HudManager.y("BuildProgress");
                    float s = HudManager.scale("BuildProgress");

                    // 补给 6/6 放完后显示建造开始倒计时（阶段动画期间）；只显示秒数；
                    // 倒计时未走完（remaining>0）或 P2 未开始（remaining=0 时仍显示 0.00s）都显示倒计时
                    if (cfg.buildStartCountdown && buildCountdownEndMs > 0) {
                        long remainingMs = Math.max(0, buildCountdownEndMs - ServerTick.getTime());
                        if (remainingMs > 0 || !inBuildPhase) {
                            double ratio = Math.clamp((double) remainingMs / BUILD_START_COUNTDOWN_MS, 0.0, 1.0);
                            String color = ratio > 0.75 ? "§a" : ratio > 0.50 ? "§e" : ratio > 0.25 ? "§6" : "§c";
                            String text = String.format("%s%.2fs", color, remainingMs / 1000.0);
                            HudManager.drawScaled(context, font, text, x, y, s);
                            return;
                        }
                    }

                    if (!inBuildPhase) return;

                    String text = buildBar(buildProgress) + " §e" + buildProgress + "%";
                    HudManager.drawScaled(context, font, text, x, y, s);
                });

        // Fresh history HUD: shows who fresh when during build phase
        HudElementRegistry.attachElementAfter(VanillaHudElements.OVERLAY_MESSAGE,
                Identifier.fromNamespaceAndPath("babyzombieaddons", "kuudra_fresh_history"),
                (context, tickCounter) -> {
                    if (!ModConfigManager.get().kuudra.phase2.freshHistory) return;
                    if (freshHistory.isEmpty()) return;

                    var font = Minecraft.getInstance().font;
                    int x = HudManager.x("FreshHistory"), y = HudManager.y("FreshHistory");
                    float s = HudManager.scale("FreshHistory");

                    StringBuilder sb = new StringBuilder("§b§lFresh Records");
                    for (FreshEntry e : freshHistory) {
                        double sec = (e.startMs - buildStartMs) / 1000.0;
                        sb.append('\n').append(String.format("%s §8@ §e%.1fs", e.playerName, sec));
                    }
                    HudManager.drawScaled(context, font, sb.toString(), x, y, s);
                });
    }

    private static void onSelfFresh() {
        var player = Minecraft.getInstance().player;
        if (player == null) return;
        var cfg = ModConfigManager.get().kuudra.phase2;
        if (!cfg.freshMessage && !cfg.freshHighlight) return;

        // 自己的名字也带 tab 里的颜色（去掉 rank 文本但保留其格式；去掉 emoji）
        String name = ChatUtils.removeEmoji(ChatUtils.toLegacyString(player.getDisplayName()))
                .replaceAll("^((?:§.)*)\\[[^]]*]\\s*", "$1").trim();
        FreshEntry entry = new FreshEntry(ServerTick.getTime(), name);
        freshPlayers.put(player.getId(), entry);
        freshHistory.add(entry);

        if (cfg.freshHighlight) {
            GlowController.setGlow(player, true, FRESH_COLOR, false);
        }

        if (cfg.freshMessage) {
            ChatUtils.sendCommand("pc FRESH! (" + buildProgress + "%)");
        }
    }

    private static void trackFresh(int entityId, String playerName) {
        var player = findPlayerById(entityId);
        if (player == null && entityId == 0) {
            player = findPlayerByName(playerName);
        }
        if (player == null) return;

        // 自己按 fresh 由 onSelfFresh 记录；这里只记队友（Hypixel 会把我们自己的 pc 消息回显，
        // 不跳过会重复记录自己）
        if (player == Minecraft.getInstance().player) return;

        FreshEntry entry = new FreshEntry(ServerTick.getTime(), playerName);
        freshPlayers.put(player.getId(), entry);
        freshHistory.add(entry);
        GlowController.setGlow(player, true, FRESH_COLOR, false);
    }

    /** 卡顿补偿：单次 tick 实际间隔超过预期（50ms）+120ms 阈值时，把倒计时终点顺延。 */
    private static long compensateLag(long endMs, long prevTickMs, long nowMs) {
        if (endMs <= 0 || prevTickMs <= 0 || nowMs <= prevTickMs) return endMs;
        long lagMs = (nowMs - prevTickMs) - 50L;
        if (lagMs <= LAG_COMPENSATION_THRESHOLD_MS) return endMs;
        return endMs + Math.min(lagMs, MAX_COMPENSATION_PER_TICK_MS);
    }

    /** 建造进度条：已完成部分用黄色 |，未完成部分用灰色 |。 */
    private static String buildBar(int pct) {
        int barLen = 14;
        int filled = (pct * barLen) / 100;
        StringBuilder bar = new StringBuilder("§8[");
        if (filled > 0) bar.append("§e");
        for (int i = 0; i < barLen; i++) {
            if (i == filled) bar.append("§7");
            bar.append('|');
        }
        bar.append("§8]");
        return bar.toString();
    }

    private static int readBuildProgress(Minecraft client) {
        if (client.player == null) return 0;
        var obj = client.player.level().getScoreboard().getDisplayObjective(DisplaySlot.BY_ID.apply(1));
        if (obj == null) return 0;
        for (var holder : client.player.level().getScoreboard().getTrackedPlayers()) {
            if (!client.player.level().getScoreboard().listPlayerScores(holder).containsKey(obj)) continue;
            var team = client.player.level().getScoreboard().getPlayersTeam(holder.getScoreboardName());
            if (team == null) continue;
            String line = ChatUtils.stripColor(ChatUtils.removeEmoji(
                    team.getPlayerPrefix().getString() + team.getPlayerSuffix().getString()));
            Matcher m = BUILD_PROGRESS_PATTERN.matcher(line);
            if (m.find()) {
                try { return Integer.parseInt(m.group(1)); } catch (NumberFormatException ignored) {}
            }
        }
        return buildProgress; // keep previous value
    }

    private static AbstractClientPlayer findPlayerById(int id) {
        var world = Minecraft.getInstance().level;
        if (world == null) return null;
        for (var p : world.players()) {
            if (p.getId() == id) return p;
        }
        return null;
    }

    /** 归一化玩家名：去色码 → 删所有 [..] 组（等级/rank/带方括号的图标）→ 只保留名字字符。
     *  MC 玩家名只含 [A-Za-z0-9_]，名字里不可能有空格、方括号或装饰字符，因此这一步对
     *  聊天侧（「[MVP++] 名字」）和 Tab 侧（「[4483] 名字 Ӄ/✌」等任意图标形态）都安全，
     *  且是严格相等比较（不会像子串匹配那样误配「Secriforts2」）。 */
    private static String normalizePlayerName(String s) {
        return ChatUtils.stripColor(s)
                .replaceAll("\\[[^]]*\\]", "")
                .replaceAll("[^A-Za-z0-9_]", "");
    }

    private static AbstractClientPlayer findPlayerByName(String name) {
        var world = Minecraft.getInstance().level;
        if (world == null) return null;
        String plain = normalizePlayerName(name);
        for (var p : world.players()) {
            if (normalizePlayerName(p.getName().getString()).equalsIgnoreCase(plain)) return p;
            // Tab 显示名兜底：与 getName() 一致时也能命中（显示名可能含 [等级] 与图标）
            var display = p.getDisplayName();
            if (normalizePlayerName(ChatUtils.toLegacyString(display)).equalsIgnoreCase(plain)) return p;
        }
        return null;
    }

    /** 按纯名字从 tab 找玩家，返回带颜色的名字（去 rank/等级、去 emoji），找不到返回 null。 */
    private static String findColoredName(String plainName) {
        var world = Minecraft.getInstance().level;
        if (world == null) return null;
        String target = normalizePlayerName(plainName);
        for (var p : world.players()) {
            var display = p.getDisplayName();
            boolean nameHit = normalizePlayerName(p.getName().getString()).equalsIgnoreCase(target);
            boolean displayHit = normalizePlayerName(ChatUtils.toLegacyString(display)).equalsIgnoreCase(target);
            if (!nameHit && !displayHit) continue;
            String raw = ChatUtils.toLegacyString(display);
            String colored = ChatUtils.removeEmoji(raw)
                    .replaceAll("^((?:§.)*)\\[[^]]*]\\s*", "$1") // 去 [等级]/[rank] 前缀
                    .replaceAll("\\s+.*$", "")                    // 名字不含空格：截掉图标后缀（含其色码与 Ӄ 等字形）
                    .trim();
            return colored.isEmpty() ? null : colored;
        }
        return null;
    }

    private static void clearAll() {
        for (int id : freshPlayers.keySet()) {
            GlowController.setGlow(Objects.requireNonNull(findPlayerById(id)), false);
        }
        freshPlayers.clear();
    }
}
