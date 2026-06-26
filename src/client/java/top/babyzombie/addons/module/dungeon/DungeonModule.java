package top.babyzombie.addons.module.dungeon;

import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.client.Minecraft;
import top.babyzombie.addons.config.ModConfig;
import top.babyzombie.addons.config.ModConfigManager;
import top.babyzombie.addons.module.party.PartyModule;
import top.babyzombie.addons.util.ChatUtils;
import top.babyzombie.addons.util.tracker.HypixelLocationTracker;
import top.babyzombie.addons.util.tracker.PartyTracker;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommands.literal;
import java.util.regex.Pattern;

/**
 * Dungeon orchestration: instance start/end detection, auto requeue cancellation keywords,
 * death message copy/send, and initialization of submodules.
 */
public final class DungeonModule {

    private static boolean instanceStarted;
    private static final Pattern DEATH_MSG = Pattern.compile(
            "☠ (.+) and became a ghost\\.");

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
            boolean win = (t.startsWith("     ") && t.contains("   ☠ Defeated ") && t.contains(" in "))
                    || t.equals("                               KUUDRA DOWN!");
            boolean fail = t.equals("                                   DEFEAT")
                    || t.equals("                             > EXTRA STATS <");

            if (win || fail) {
                instanceStarted = false;
                if (win && HypixelLocationTracker.getInstance().isInDungeon()) {
                    DailyRunsCounter.incrementAndShow();
                }
                AutoRequeue.schedule(win);
            }
        });

        // Death message copy/send
        ClientReceiveMessageEvents.GAME.register((m, o) -> {
            if (o) return;
            var mode = ModConfigManager.get().dungeon.deathMessageAction;
            if (mode == ModConfig.DeathMessageAction.OFF) return;
            if (!HypixelLocationTracker.getInstance().isInDungeon()) return;
            String text = ChatUtils.stripColor(m.getString());
            var dm = DEATH_MSG.matcher(text);
            if (!dm.find()) return;
            String msg = dm.group(0);
            if (mode == ModConfig.DeathMessageAction.COPY || mode == ModConfig.DeathMessageAction.COPY_AND_SEND) {
                Minecraft.getInstance().keyboardHandler.setClipboard(msg);
            }
            if (mode == ModConfig.DeathMessageAction.COPY_AND_SEND || mode == ModConfig.DeathMessageAction.SEND) {
                ChatUtils.sendCommand("pc " + msg);
            }
        });

        // /dt command — cancel auto requeue + notify party
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(literal("dt").executes(DungeonModule::dtCancel));
        });

        // Cancel keywords from party chat
        ClientReceiveMessageEvents.GAME.register((m, o) -> {
            if (o) return;
            if (!AutoRequeue.canRequeue || AutoRequeue.cancelAutoJoin) return;
            if (!HypixelLocationTracker.getInstance().isInKuudra() && !HypixelLocationTracker.getInstance().isInDungeon()) return;
            if (ModConfigManager.get().dungeon.dungeonRequeue == ModConfig.RequeueMode.OFF && HypixelLocationTracker.getInstance().isInDungeon()) return;
            if (ModConfigManager.get().dungeon.kuudraRequeue == ModConfig.RequeueMode.OFF && HypixelLocationTracker.getInstance().isInKuudra()) return;
            if (!PartyTracker.getInstance().isSelfLeader()) return;
            var pm = PartyModule.PARTY_CHAT.matcher(ChatUtils.stripColor(m.getString()));
            if (!pm.find()) return;
            String t = ChatUtils.stripColor(pm.group(2)).trim().toLowerCase();
            if (t.startsWith("!")) t = t.replace("!", "");
            for (String kw : ModConfigManager.get().dungeon.requeueCancelKeywords.toLowerCase().split("\\|")) {
                if (!kw.isEmpty() && t.equals(kw)) {
                    AutoRequeue.cancel();
                    return;
                }
            }
        });

        DungeonAutoPB.init();
    }

    private static int dtCancel(CommandContext<FabricClientCommandSource> ctx) {
        if (!AutoRequeue.canRequeue || AutoRequeue.cancelAutoJoin) return 1;
        AutoRequeue.cancelAutoJoin = true;
        AutoRequeue.waitingForRevive = false;
        ctx.getSource().sendFeedback(
                net.minecraft.network.chat.Component.translatable("babyzombieaddons.dt.cancelled"));
        return 1;
    }
}
