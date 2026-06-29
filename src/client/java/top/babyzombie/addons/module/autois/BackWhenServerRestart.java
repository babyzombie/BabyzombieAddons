package top.babyzombie.addons.module.autois;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLevelEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.ClickEvent;
import top.babyzombie.addons.config.ModConfigManager;
import top.babyzombie.addons.util.ChatUtils;
import top.babyzombie.addons.util.Scheduler;
import top.babyzombie.addons.util.tracker.HypixelLocationTracker;

import java.util.regex.Pattern;

public class BackWhenServerRestart {
    private static final Pattern EVACUATE_MSG = Pattern.compile(
        "You have (\\d+) seconds to warp out! CLICK to warp now!"
    );
    private static boolean restart = false;
    private static boolean afk = false;

    private BackWhenServerRestart() {}

    static void init() {
        ClientReceiveMessageEvents.GAME.register((component, o) -> {
            if (ModConfigManager.get().general.autois) return;
            if (!ModConfigManager.get().general.backOnServerRestart) return;
            if (Minecraft.getInstance().player == null) return;
            if (!HypixelLocationTracker.getInstance().isInSkyblock()) return;
            if (!HypixelLocationTracker.getInstance().isIn("Private Island")) return;
            if (component.getStyle().getClickEvent() instanceof ClickEvent.RunCommand command) {
                if (command.command().replace("/", "").equals("evacuate")) {
                    if (EVACUATE_MSG.matcher(component.getString()).matches()) {
                        restart = true;
                        var playerLocation = Minecraft.getInstance().player.getPosition(1);
                        Scheduler.schedule(100, () -> {
                            var player = Minecraft.getInstance().player;
                            if ( player != null && player.getPosition(1).distanceTo(playerLocation) < 1)
                                afk = true;
                        });
                    }
                }
            }
        });

        ClientLevelEvents.AFTER_CLIENT_LEVEL_CHANGE.register(((client, level) -> {
            if (restart && afk && ModConfigManager.get().general.backOnServerRestart) {
                Scheduler.schedule(200, () -> {
                    if (HypixelLocationTracker.getInstance().isInSkyblock()
                        && HypixelLocationTracker.getInstance().isIn("Hub")
                        ) ChatUtils.sendCommand("is");
                });
            }
            restart = false;
            afk = false;
        }));
    }
}
