package top.babyzombie.addons.module.garden;

import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import top.babyzombie.addons.config.ModConfigManager;
import top.babyzombie.addons.util.ChatUtils;
import top.babyzombie.addons.util.HypixelLocationTracker;

/**
 * Auto-accepts Trevor the Trapper's hunting task by clicking [YES].
 */
public final class TrevorAutoAccept {
    private TrevorAutoAccept() {}

    public static void init() {
        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            if (overlay) return;
            if (!ModConfigManager.get().garden.trevorAutoAccept) return;
            if (!HypixelLocationTracker.getInstance().isInSkyblock()) return;

            String text = ChatUtils.stripColor(message.getString());
            if (!text.contains("Accept the trapper's task to hunt the animal?")) return;

            String cmd = findClickCommand(message, "YES");
            if (cmd != null) ChatUtils.sendCommand(cmd);
        });
    }

    private static String findClickCommand(Component component, String target) {
        if (component.getString().contains(target)) {
            ClickEvent ce = component.getStyle().getClickEvent();
            if (ce instanceof ClickEvent.RunCommand(String command)) return command;
        }
        for (Component child : component.getSiblings()) {
            String cmd = findClickCommand(child, target);
            if (cmd != null) return cmd;
        }
        return null;
    }
}
