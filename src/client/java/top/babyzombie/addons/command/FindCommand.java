package top.babyzombie.addons.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.network.chat.Component;
import top.babyzombie.addons.event.HypixelLocationEvents;
import top.babyzombie.addons.util.ChatUtils;
import top.babyzombie.addons.util.Scheduler;
import top.babyzombie.addons.util.tracker.HypixelLocationTracker;
import top.babyzombie.addons.util.tracker.ServerVisitTracker;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommands.argument;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommands.literal;

/**
 * /bza find &lt;target&gt;  — warp to a location after returning to private island.
 * /bza findnew &lt;target&gt; — same, but loops until a never-before-visited server is found.
 * <p>
 * Teleport cooldown: after cross-world warp, the location packet arrives first,
 * then the Profile message.  The cooldown (for both /is and /warp) starts from
 * the Profile message — wait 1 second after Profile before sending any teleport.
 */
public final class FindCommand {

    private static final Pattern PROFILE_ID_PATTERN = Pattern.compile("Profile ID: ([a-f0-9-]+)");
    private static final String TK = "babyzombieaddons.find.";

    private enum State {
        IDLE,
        WAITING_PROFILE,   // waiting for Profile after /is
        WAITING_LOCATION,  // waiting for location update after /warp
        WAITING_RETRY      // visited server — waiting for Profile, then will /is
    }

    private static State state = State.IDLE;
    private static String target;
    private static boolean findNew;
    private static String preWarpServer;
    private static Set<String> visitedBeforeWarp;
    private static Runnable timeoutTask;

    private FindCommand() {}

    /** Register persistent event listeners. Called once during mod init. */
    public static void init() {
        // Chat listener
        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            if (overlay) return;
            if (ChatUtils.extractPattern(message, PROFILE_ID_PATTERN, 1) == null) return;

            switch (state) {
                case WAITING_PROFILE -> {
                    cancelTimeout();
                    preWarpServer = HypixelLocationTracker.getInstance().getServerName();
                    visitedBeforeWarp = new HashSet<>(ServerVisitTracker.getInstance().getVisitedServers());
                    // Cooldown: 1s after Profile before any teleport
                    Scheduler.schedule(20, () -> {
                        ChatUtils.sendCommand("warp " + target);
                        state = State.WAITING_LOCATION;
                        armTimeout();
                    });
                }
                case WAITING_RETRY -> {
                    cancelTimeout();
                    // Cooldown: 1s after Profile, then /is
                    Scheduler.schedule(20, () -> {
                        ChatUtils.sendCommand("is");
                        state = State.WAITING_PROFILE;
                        armTimeout();
                    });
                }
            }
        });

        // Location listener: check server after warp
        HypixelLocationEvents.LOCATION_UPDATE.register(data -> {
            if (state != State.WAITING_LOCATION) return;
            String newServer = data.getServerName();
            if (newServer == null || Objects.equals(newServer, preWarpServer)) return;

            cancelTimeout();

            if (findNew && visitedBeforeWarp != null && visitedBeforeWarp.contains(newServer)) {
                ChatUtils.showTranslatable(TK + "retrying", newServer);
                preWarpServer = null;
                visitedBeforeWarp = null;
                // Server visited — wait for the next Profile message
                // (arrives after location update for cross-world warps)
                state = State.WAITING_RETRY;
                armTimeout();
            } else {
                if (findNew) {
                    ChatUtils.showTranslatable(TK + "new_server_found", newServer);
                } else {
                    ChatUtils.showTranslatable(TK + "warped", target, newServer);
                }
                reset();
            }
        });

        // Reset on disconnect
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            if (state != State.IDLE) {
                ChatUtils.showTranslatable(TK + "cancelled_disconnect");
                reset();
            }
        });
    }

    /** Register brigadier nodes under the parent literal. */
    public static void register(ArgumentBuilder<FabricClientCommandSource, ?> parent) {
        parent.then(literal("find")
                .then(argument("target", StringArgumentType.greedyString())
                        .executes(ctx -> {
                            target = StringArgumentType.getString(ctx, "target");
                            findNew = false;
                            start(ctx.getSource());
                            return 1;
                        })));
        parent.then(literal("findnew")
                .then(argument("target", StringArgumentType.greedyString())
                        .executes(ctx -> {
                            target = StringArgumentType.getString(ctx, "target");
                            findNew = true;
                            start(ctx.getSource());
                            return 1;
                        })));
    }

    // ---- internal ----

    private static void start(FabricClientCommandSource source) {
        if (state != State.IDLE) {
            source.sendFeedback(Component.translatable(TK + "already_running"));
            return;
        }
        if (findNew) {
            source.sendFeedback(Component.translatable(TK + "starting_new", target));
        } else {
            source.sendFeedback(Component.translatable(TK + "starting", target));
        }
        ChatUtils.sendCommand("is");
        state = State.WAITING_PROFILE;
        armTimeout();
    }

    private static void armTimeout() {
        timeoutTask = () -> {
            if (state != State.IDLE) {
                ChatUtils.showTranslatable(TK + "timed_out");
                reset();
            }
        };
        Scheduler.schedule(200, timeoutTask); // 10 seconds
    }

    private static void cancelTimeout() {
        if (timeoutTask != null) {
            Scheduler.cancel(timeoutTask);
            timeoutTask = null;
        }
    }

    private static void reset() {
        cancelTimeout();
        state = State.IDLE;
        target = null;
        findNew = false;
        preWarpServer = null;
        visitedBeforeWarp = null;
    }
}
