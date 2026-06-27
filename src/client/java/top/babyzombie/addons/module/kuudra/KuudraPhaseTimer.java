package top.babyzombie.addons.module.kuudra;

import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import top.babyzombie.addons.config.ModConfigManager;
import top.babyzombie.addons.util.ChatUtils;
import top.babyzombie.addons.util.tracker.HypixelLocationTracker;
import top.babyzombie.addons.util.ServerTick;

public final class KuudraPhaseTimer {
    private KuudraPhaseTimer() {}

    private static long startTime;
    private static long p1, p2, p3, p4;
    private static boolean summaryPrinted;

    public static void init() {
        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            if (!ModConfigManager.get().kuudra.phaseTimer) return;
            if (overlay || !HypixelLocationTracker.getInstance().isInKuudra()) return;
            String text = ChatUtils.stripColor(message.getString());

            // KUUDRA DOWN! — 自动重开检测同款消息，打得快 Elle 来不及说话时也能结算
            if (text.equals("                               KUUDRA DOWN!") && startTime > 0 && !summaryPrinted) {
                if (p4 == 0) {
                    p4 = ServerTick.getTime() - startTime - p1 - p2 - p3;
                }
                printSummary(isT5());
                summaryPrinted = true;
                return;
            }

            if (!text.startsWith("[NPC] Elle:")) return;

            if (text.contains("Okay adventurers, I will go and fish up Kuudra")) {
                startTime = ServerTick.getTime();
                p1 = p2 = p3 = p4 = 0;
                summaryPrinted = false;
            } else if (text.contains("OMG! Great work collecting my supplies")) {
                p1 = ServerTick.getTime() - startTime;
                show("kuudra.phase.p1", p1);
            } else if (text.contains("Phew! The Ballista is finally ready")) {
                p2 = ServerTick.getTime() - startTime - p1;
                show("kuudra.phase.p2", p2);
            } else if (text.contains("POW! SURELY THAT'S IT")) {
                p3 = ServerTick.getTime() - startTime - p1 - p2;
                if (isT5()) show("kuudra.phase.p3", p3);
            } else if (text.contains("Good job everyone")) {
                p4 = ServerTick.getTime() - startTime - p1 - p2 - p3;
                if (!summaryPrinted) {
                    printSummary(isT5());
                    summaryPrinted = true;
                }
            }
        });
    }

    private static boolean isT5() {
        var loc = HypixelLocationTracker.getInstance().getLocation();
        return loc != null && loc.contains("T5");
    }

    private static void show(String phaseKey, long time) {
        String phase = ChatUtils.translate(phaseKey);
        ChatUtils.showMessage(ChatUtils.translate("kuudra.phase.time", phase, formatTime(time)));
    }

    private static void printSummary(boolean isT5) {
        show("kuudra.phase.p1", p1);
        show("kuudra.phase.p2", p2);
        if (isT5) {
            show("kuudra.phase.p3", p3);
            show("kuudra.phase.p4", p4);
        } else {
            show("kuudra.phase.p3_short", p3 + p4);
        }
    }

    private static String formatTime(long ms) {
        long s = ms / 1000, m = s / 60;
        s %= 60;
        return String.format("%d:%02d", m, s);
    }
}
