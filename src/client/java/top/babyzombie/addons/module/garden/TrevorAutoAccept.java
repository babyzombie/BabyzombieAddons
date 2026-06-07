package top.babyzombie.addons.module.garden;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import top.babyzombie.addons.config.ModConfigManager;
import top.babyzombie.addons.util.ChatUtils;
import top.babyzombie.addons.util.HypixelLocationTracker;
import top.babyzombie.addons.util.ServerTick;

/**
 * Auto-accepts Trevor the Trapper's hunting task by clicking [YES],
 * and auto-calls Trevor after task completion with a 31-second cooldown.
 */
public final class TrevorAutoAccept {
    private TrevorAutoAccept() {}

    private static long taskStartTime;
    private static boolean waitingToCall;
    private static boolean autoCallDisabled;

    public static void disableAutoCall() { autoCallDisabled = true; }

    public static void init() {
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            autoCallDisabled = false;
        });

        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            if (overlay) return;
            if (!HypixelLocationTracker.getInstance().isInSkyblock()) return;
            var cfg = ModConfigManager.get().garden;
            String text = ChatUtils.stripColor(message.getString());

            // Auto-accept
            if (cfg.trevorAutoAccept && text.startsWith("Accept the trapper's task to hunt the animal?")) {
                String cmd = findClickCommand(message, "YES");
                if (cmd != null) ChatUtils.sendCommand(cmd);
            }

            // Auto-call
            if (cfg.trevorAutoCall && !autoCallDisabled) {
                if (text.startsWith("You can find your ") && text.contains(" animal near the ")) {
                    taskStartTime = ServerTick.getTime();
                } else if (text.equals("Return to the Trapper soon to get a new animal to hunt!")) {
                    long elapsed = ServerTick.getTime() - taskStartTime;
                    if (taskStartTime == 0 || elapsed >= 31000) {
                        doCall();
                    } else {
                        waitingToCall = true;
                    }
                }
            }
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (!waitingToCall || autoCallDisabled) {
                waitingToCall = false;
                return;
            }
            if (ServerTick.getTime() - taskStartTime >= 31000) {
                doCall();
            }
        });
    }

    private static void doCall() {
        ChatUtils.sendCommand("call trevor_the_trapper");
        waitingToCall = false;
        var player = Minecraft.getInstance().player;
        if (player != null) {
            var btn = Component.translatable("babyzombieaddons.trevor.auto_call_disable_btn")
                    .withStyle(style -> style
                            .withClickEvent(new net.minecraft.network.chat.ClickEvent.RunCommand("/bza trevorautocall"))
                            .withHoverEvent(new net.minecraft.network.chat.HoverEvent.ShowText(
                                    Component.translatable("babyzombieaddons.trevor.auto_call_disable_hover"))));
            player.displayClientMessage(
                    Component.translatable("babyzombieaddons.trevor.auto_call").append(" ").append(btn), false);
        }
    }

    private static String findClickCommand(Component component, String target) {
        if (component.getString().contains(target)) {
            ClickEvent ce = component.getStyle().getClickEvent();
            if (ce instanceof ClickEvent.RunCommand runCmd) return runCmd.command();
        }
        for (Component child : component.getSiblings()) {
            String cmd = findClickCommand(child, target);
            if (cmd != null) return cmd;
        }
        return null;
    }
}
