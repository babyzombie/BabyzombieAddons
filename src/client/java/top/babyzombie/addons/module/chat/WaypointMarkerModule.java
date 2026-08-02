package top.babyzombie.addons.module.chat;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.KeyMapping;
import top.babyzombie.addons.command.SendCoordsCommand;
import top.babyzombie.addons.config.ModConfig;
import top.babyzombie.addons.config.ModConfigManager;
import top.babyzombie.addons.util.KeyBindingUtil;

/**
 * 标点快捷键：按一下发送一次准星指向的方块坐标到指定频道。
 */
public final class WaypointMarkerModule {

    public static KeyMapping key;

    private WaypointMarkerModule() {}

    public static void init() {
        key = KeyBindingUtil.register("key.babyzombieaddons.marker", -1);
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null) return;
            while (key.consumeClick()) {
                var cfg = ModConfigManager.get().general.waypointMarker;
                SendCoordsCommand.sendLookingAt(channelPrefix(cfg.markerChannel), cfg.markerSuffix);
            }
        });
    }

    private static String channelPrefix(ModConfig.MarkerChannel channel) {
        return switch (channel) {
            case DEFAULT -> null;
            case AC, PC, GC -> channel.name().toLowerCase();
        };
    }
}
