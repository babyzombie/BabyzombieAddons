package top.babyzombie.addons.module.dungeon;

import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import top.babyzombie.addons.config.ModConfig;
import top.babyzombie.addons.config.ModConfigManager;
import top.babyzombie.addons.module.party.PartyModule;
import top.babyzombie.addons.util.ChatUtils;
import top.babyzombie.addons.util.HypixelLocationTracker;

/**
 * Dungeon orchestration: instance start/end detection, auto requeue cancellation keywords,
 * and initialization of submodules.
 */
public final class DungeonModule {

    private static boolean instanceStarted;

    private DungeonModule() {}

    public static void init() {
        AutoRequeue.init();
        F4CrowdHiding.init();
        AutoChestClose.init();
        StormThunderMute.init();

        // Instance start
        ClientReceiveMessageEvents.GAME.register((m, o) -> {
            if (o) return;
            String t = ChatUtils.stripColor(m.getString());
            if (t.equals("Starting in 1 second.")) {
                instanceStarted = true;
                AutoRequeue.onInstanceStart();
            }
        });

        // Instance end detection
        ClientReceiveMessageEvents.GAME.register((m, o) -> {
            if (o || !instanceStarted) return;
            String t = ChatUtils.stripColor(m.getString());
            boolean win = t.equals("          "
                    + "          "
                    + "         > EXTRA STATS <")
                    || t.equals("                               KUUDRA DOWN!");
            boolean fail = t.equals("                                   DEFEAT")
                    || (t.startsWith("     ") && t.contains("   ☠ Defeated ") && t.contains(" in "));

            if (win || fail) {
                instanceStarted = false;
                if (win && HypixelLocationTracker.getInstance().isInDungeon()) {
                    DailyRunsCounter.incrementAndShow();
                }
                AutoRequeue.schedule(win);
            }
        });

        // Cancel keywords from party chat
        ClientReceiveMessageEvents.GAME.register((m, o) -> {
            if (o) return;
            if (ModConfigManager.get().dungeon.autoRequeue == ModConfig.RequeueMode.OFF) return;
            if (!AutoRequeue.canRequeue) return;
            var pm = PartyModule.PARTY_CHAT.matcher(m.getString());
            if (!pm.find()) return;
            String t = ChatUtils.stripColor(pm.group(2)).trim().toLowerCase();
            for (String kw : ModConfigManager.get().dungeon.requeueCancelKeywords.toLowerCase().split("\\|")) {
                if (!kw.isEmpty() && t.equals(kw)) {
                    AutoRequeue.cancel();
                    return;
                }
            }
        });
    }
}
