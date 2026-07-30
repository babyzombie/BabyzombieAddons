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
    private static final int FRESH_COLOR = 0x55FFFF;

    // Scoreboard: "Protect Elle (69%)"
    private static final Pattern BUILD_PROGRESS_PATTERN = Pattern.compile("Protect Elle\\s*\\((\\d+)%\\)");

    private record FreshEntry(long startMs, String playerName) {}
    private static final Map<Integer, FreshEntry> freshPlayers = new ConcurrentHashMap<>();
    private static final List<FreshEntry> freshHistory = new ArrayList<>();
    private static long buildStartMs;

    private static int buildProgress = 0;
    private static boolean inBuildPhase;

    public static void init() {
        ClientLevelEvents.AFTER_CLIENT_LEVEL_CHANGE.register((client, world) -> {
            clearAll();
            buildProgress = 0;
            inBuildPhase = false;
        });

        // ── Chat detection ──
        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            if (overlay || !HypixelLocationTracker.getInstance().isInKuudra()) return;
            String text = ChatUtils.stripColor(message.getString());

            // P2 start
            if (text.contains("OMG! Great work collecting my supplies")) {
                inBuildPhase = true;
                buildProgress = 0;
                freshHistory.clear();
                buildStartMs = System.currentTimeMillis();
                return;
            }
            // P2 end
            if (text.contains("Phew! The Ballista is finally ready")) {
                inBuildPhase = false;
                return;
            }

            // Self fresh
            if (text.contains("Your Fresh Tools Perk bonus doubles your building speed")) {
                onSelfFresh();
                return;
            }

            // Party fresh: detect both our own and IQ's "FRESH!" messages
            // IQ pattern: Party > [rank] PlayerName: [IQ] FRESH!
            // Our pattern: Party > PlayerName: FRESH!
            Matcher fm = Pattern.compile(
                    "Party > (?:\\[[^]]+] )?(\\w+): (?:\\[IQ] )?FRESH\\b",
                    Pattern.CASE_INSENSITIVE).matcher(text);
            if (fm.find() && inBuildPhase) {
                String name = fm.group(1);
                trackFresh(0, name);
            }
        });

        // ── Tick: cleanup expired, update build progress ──
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            long now = System.currentTimeMillis();
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
        });

        // ── World render: fresh countdown above heads ──
        RenderPhaseRegister.register(ctx -> {
            long now = System.currentTimeMillis();
            for (var entry : freshPlayers.entrySet()) {
                var player = findPlayerById(entry.getKey());
                if (player == null || player.isRemoved()) continue;
                long remaining = FRESH_DURATION_MS - (now - entry.getValue().startMs);
                if (remaining <= 0) continue;

                double sec = remaining / 1000.0;
                int color;
                if (sec > 6) color = 0x55FF55;
                else if (sec > 3) color = 0xFFFF55;
                else color = 0xFF5555;

                double y = player.getY() + 2.5;
                WorldTextRenderer.renderString(ctx, String.format("%.1fs", sec),
                        player.getX(), y, player.getZ(), color, 0.05f, true);
            }
        });

        // ── HUD: build progress ──
        HudElementRegistry.attachElementAfter(VanillaHudElements.OVERLAY_MESSAGE,
                Identifier.fromNamespaceAndPath("babyzombieaddons", "kuudra_build_progress"),
                (context, tickCounter) -> {
                    if (!ModConfigManager.get().kuudra.phase2.buildProgressHud) return;
                    if (!inBuildPhase || buildProgress <= 0) return;

                    var font = Minecraft.getInstance().font;
                    int x = HudManager.x("BuildProgress"), y = HudManager.y("BuildProgress");
                    float s = HudManager.scale("BuildProgress");

                    int barLen = 14;
                    int filled = (buildProgress * barLen) / 100;
                    StringBuilder bar = new StringBuilder("§8[");
                    if (filled > 0) bar.append("§e");
                    for (int i = 0; i < barLen; i++) {
                        if (i == filled) bar.append("§8");
                        bar.append(i < filled ? '|' : ' ');
                    }
                    bar.append("§8]");
                    String text = bar + " §e" + buildProgress + "%";
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

        FreshEntry entry = new FreshEntry(System.currentTimeMillis(), player.getName().getString());
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

        FreshEntry entry = new FreshEntry(System.currentTimeMillis(), playerName);
        freshPlayers.put(player.getId(), entry);
        freshHistory.add(entry);
        GlowController.setGlow(player, true, FRESH_COLOR, false);
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

    private static AbstractClientPlayer findPlayerByName(String name) {
        var world = Minecraft.getInstance().level;
        if (world == null) return null;
        for (var p : world.players()) {
            if (p.getName().getString().equalsIgnoreCase(name)) return p;
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
