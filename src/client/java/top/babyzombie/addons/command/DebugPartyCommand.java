package top.babyzombie.addons.command;

import com.mojang.brigadier.builder.ArgumentBuilder;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.network.chat.Component;
import top.babyzombie.addons.util.ServerTick;
import top.babyzombie.addons.util.tracker.PartyTracker;
import top.babyzombie.addons.util.tracker.PartyTracker.PartyInfo;

import java.text.SimpleDateFormat;
import java.util.Date;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommands.literal;

/**
 * /bza debug party — shows last party info fetch time and member data.
 */
final class DebugPartyCommand {
    private DebugPartyCommand() {}

    private static final String PFX = "babyzombieaddons.debug.party.";

    static void register(ArgumentBuilder<FabricClientCommandSource, ?> parent) {
        parent.then(literal("party").executes(ctx -> {
            dumpPartyInfo(ctx.getSource());
            return 1;
        }));
    }

    private static void dumpPartyInfo(FabricClientCommandSource src) {
        PartyTracker tracker = PartyTracker.getInstance();
        long lastRequestTime = tracker.getLastRequestTime();
        PartyInfo lastInfo = tracker.getLastInfo();
        long now = ServerTick.getTime();

        src.sendFeedback(Component.translatable(PFX + "header"));

        // ---- Last Fetch Time ----
        if (lastRequestTime > 0) {
            String timeStr = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(lastRequestTime));
            long elapsed = now - lastRequestTime;
            String elapsedStr = formatDuration(elapsed);
            src.sendFeedback(Component.translatable(PFX + "last_fetch",
                    timeStr, elapsedStr));
        } else {
            src.sendFeedback(Component.translatable(PFX + "never_fetched"));
        }

        // ---- Leader ----
        String leaderName = tracker.getLeaderName();
        if (leaderName != null) {
            src.sendFeedback(Component.translatable(PFX + "leader",
                    leaderName,
                    t(tracker.isSelfLeader() ? PFX + "yes" : PFX + "no")));
        } else {
            src.sendFeedback(Component.translatable(PFX + "leader_unknown"));
        }

        // ---- Members ----
        if (lastInfo != null && !lastInfo.members().isEmpty()) {
            var members = lastInfo.members();
            src.sendFeedback(Component.translatable(PFX + "member_count", members.size()));
            int i = 1;
            for (var uuid : members) {
                src.sendFeedback(Component.literal(
                        "  §7" + i + ". §f" + uuid.toString()));
                i++;
            }
            // Also show raw data
            src.sendFeedback(Component.translatable(PFX + "raw_data",
                    lastInfo.toString()));
        } else {
            src.sendFeedback(Component.translatable(PFX + "no_members"));
        }
    }

    private static String formatDuration(long ms) {
        if (ms < 1000) return ms + "ms";
        long s = ms / 1000;
        if (s < 60) return s + "s";
        long m = s / 60;
        s %= 60;
        if (m < 60) return m + "m" + s + "s";
        long h = m / 60;
        m %= 60;
        return h + "h" + m + "m" + s + "s";
    }

    private static String t(String key) {
        return Component.translatable(key).getString();
    }
}
