package top.babyzombie.addons.module.dungeon;

import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import top.babyzombie.addons.config.ModConfig;
import top.babyzombie.addons.config.ModConfigManager;
import top.babyzombie.addons.event.EntityRenderEvents;
import top.babyzombie.addons.module.party.PartyModule;
import top.babyzombie.addons.util.ChatUtils;
import top.babyzombie.addons.util.PartyTracker;
import top.babyzombie.addons.util.DataPersistence;
import top.babyzombie.addons.util.HypixelLocationTracker;
import top.babyzombie.addons.util.Scheduler;

/**
 * Dungeon: F4 crowd hiding, auto requeue, auto chest close, daily counter.
 * End detection matches original ChatTriggers JS exact message patterns.
 */
public final class DungeonModule {

    private static int dailyRuns, dailyTimestamp;
    private static boolean instanceStarted;

    private DungeonModule() {}

    public static void init() {
        AutoRequeue.init();

        // F4 crowd hiding — uses entity render event or direct removal
        EntityRenderEvents.BEFORE_RENDER.register(entity -> {
            var mode = ModConfigManager.get().dungeon.f4CrowdHiding;
            if (mode == ModConfig.CrowdHideMode.OFF) return false;
            if (!HypixelLocationTracker.getInstance().isInDungeon()) return false;
            String floor = HypixelLocationTracker.getInstance().getFloor();
            if (floor == null || !floor.contains("4")) return false;

            var player = Minecraft.getInstance().player;
            if (player == null) return false;
            double px = player.getX(), py = player.getY(), pz = player.getZ();
            if (px < -40 || px > 50 || py < 0 || py > 255 || pz < -40 || pz > 50) return false;

            if (entity instanceof net.minecraft.world.entity.player.Player otherPlayer) {
                String name = otherPlayer.getName().getString();
                if (name.contains(" ")) return false;
                if (name.contains("Decoy") || name.contains("Spirit Bear")) return false;
                return mode == ModConfig.CrowdHideMode.HIDE;
            }
            boolean isCrowd = entity.getClass().getSimpleName().equals("Zombie")
                    || entity.getClass().getSimpleName().equals("Skeleton");
            return isCrowd && mode == ModConfig.CrowdHideMode.HIDE;
        });

        // F4 crowd removal (REMOVE mode) — done on tick
        net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (ModConfigManager.get().dungeon.f4CrowdHiding != ModConfig.CrowdHideMode.REMOVE) return;
            if (!HypixelLocationTracker.getInstance().isInDungeon()) return;
            String floor = HypixelLocationTracker.getInstance().getFloor();
            if (floor == null || !floor.contains("4")) return;
            if (client.player == null || client.level == null) return;
            double px = client.player.getX(), py = client.player.getY(), pz = client.player.getZ();
            if (px < -40 || px > 50 || py < 0 || py > 255 || pz < -40 || pz > 50) return;

            for (var entity : client.level.entitiesForRendering()) {
                if (entity instanceof net.minecraft.world.entity.player.Player otherPlayer) {
                    String name = otherPlayer.getName().getString();
                    if (name.contains(" ") || name.contains("Decoy") || name.contains("Spirit Bear")) continue;
                    entity.discard();
                } else if (entity.getClass().getSimpleName().equals("Zombie")
                        || entity.getClass().getSimpleName().equals("Skeleton")) {
                    entity.discard();
                }
            }
        });

        // Instance start
        ClientReceiveMessageEvents.GAME.register((m, o) -> {
            if (o) return;
            String t = ChatUtils.stripColor(m.getString());
            if (t.equals("Starting in 1 second.")) {
                instanceStarted = true;
                AutoRequeue.onInstanceStart();
            }
        });

        // Instance end detection — exact message matching from original JS
        ClientReceiveMessageEvents.GAME.register((m, o) -> {
            if (o || !instanceStarted) return;
            String t = ChatUtils.stripColor(m.getString());
            // Exact message matching (space counts are fixed by Hypixel format)
            boolean win = t.equals("          "
                    + "          "
                    + "         > EXTRA STATS <")  // 29 spaces — dungeon win
                    || t.equals("                               KUUDRA DOWN!");  // 31 spaces — kuudra win
            boolean fail = t.equals("                                   DEFEAT")  // 35 spaces — kuudra fail
                    || (t.startsWith("     ") && t.contains("   ☠ Defeated ") && t.contains(" in "));

            if (win || fail) {
                instanceStarted = false;

                if (win && ModConfigManager.get().dungeon.dailyCounter) {
                    loadDaily();
                    dailyRuns++;
                    saveDaily();
                    if (dailyRuns <= 5)
                        ChatUtils.sendCommand("pc Daily runs: " + dailyRuns + "/5");
                }

                AutoRequeue.schedule(win);
            }
        });

        // Cancel keywords from party chat (only by party leader)
        ClientReceiveMessageEvents.GAME.register((m, o) -> {
            if (o) return;
            if (ModConfigManager.get().dungeon.autoRequeue == ModConfig.RequeueMode.OFF) return;
            var pm = PartyModule.PARTY_CHAT.matcher(m.getString());
            if (!pm.find()) return;
            String sender = pm.group(1);
            if (!PartyTracker.getInstance().hasLeaderName(sender) && PartyTracker.getInstance().getLeaderName() != null)
                return;
            String t = ChatUtils.stripColor(pm.group(2)).trim().toLowerCase();
            for (String kw : ModConfigManager.get().dungeon.requeueCancelKeywords.toLowerCase().split("\\|")) {
                if (!kw.isEmpty() && t.equals(kw)) {
                    AutoRequeue.cancel();
                    return;
                }
            }
        });

        // Auto chest close
        ScreenEvents.AFTER_INIT.register((client, screen, sw, sh) -> {
            if (!ModConfigManager.get().dungeon.autoChestClose) return;
            if (!HypixelLocationTracker.getInstance().isInDungeon()) return;
            if (screen instanceof AbstractContainerScreen<?> cs) {
                String title = cs.getTitle().getString();
                if (title.equals("Chest") || title.equals("箱子")) {
                    Scheduler.schedule(4, () -> {
                        if (client.screen == screen && HypixelLocationTracker.getInstance().isInDungeon())
                            client.execute(() -> { if (client.player != null) client.player.closeContainer(); });
                    });
                }
            }
        });
    }

    private static String dailySubDir() {
        var t = HypixelLocationTracker.getInstance();
        return (t.getUuid() != null ? t.getUuid() : "unknown")
                + "_" + (t.getProfileId() != null ? t.getProfileId() : "unknown");
    }

    private static void loadDaily() {
        var data = DataPersistence.load(dailySubDir(), "dungeon_daily.json", DailyData.class);
        int today = todayKey();
        if (data != null && data.timestamp == today) {
            dailyRuns = data.runs;
            dailyTimestamp = data.timestamp;
        } else {
            dailyRuns = 0;
            dailyTimestamp = today;
        }
    }

    private static void saveDaily() {
        DataPersistence.save(dailySubDir(), "dungeon_daily.json",
                new DailyData(dailyRuns, dailyTimestamp));
    }

    private static int todayKey() {
        var now = java.time.ZonedDateTime.now(java.time.ZoneOffset.UTC);
        return now.getYear() * 10000 + now.getMonthValue() * 100 + now.getDayOfMonth();
    }

    private record DailyData(int runs, int timestamp) {}
}
