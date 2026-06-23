package top.babyzombie.addons.module.dungeon;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLevelEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import top.babyzombie.addons.config.ModConfig.AutoPotionsMode;
import top.babyzombie.addons.config.ModConfigManager;
import top.babyzombie.addons.util.ChatUtils;
import top.babyzombie.addons.util.tracker.HypixelLocationTracker;

/** 进入地牢后轮询计分板层数，达到最低启用楼层时自动 /pb 打开药水包。 */
public final class DungeonAutoPB {

    private static int pollTicks;
    private static boolean polling;

    private DungeonAutoPB() {}

    static void init() {
        ClientLevelEvents.AFTER_CLIENT_LEVEL_CHANGE.register((client, level) -> {
            if (ModConfigManager.get().dungeon.autoOpenPotions == AutoPotionsMode.OFF) return;
            pollTicks = 0;
            polling = true;
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (!polling) return;

            var mode = ModConfigManager.get().dungeon.autoOpenPotions;
            if (mode == AutoPotionsMode.OFF) {
                polling = false;
                return;
            }

            var tracker = HypixelLocationTracker.getInstance();
            if (!tracker.isInDungeon()) {
                polling = false;
                return;
            }

            String location = tracker.getLocation();
            String floor = null;
            if (location != null) {
                int i = location.lastIndexOf('(');
                int j = location.lastIndexOf(')');
                if (i >= 0 && j > i) floor = location.substring(i + 1, j);
            }

            if (floor != null) {
                polling = false;
                if (floor.length() >= 2 && floor.charAt(0) == 'M') {
                    try {
                        int num = Integer.parseInt(floor.substring(1));
                        int threshold = Integer.parseInt(mode.name().substring(1));
                        if (num >= threshold) {
                            ChatUtils.sendCommand("pb");
                        }
                    } catch (NumberFormatException ignored) {}
                }
                return;
            }

            if (++pollTicks > 300) {
                polling = false;
            }
        });
    }
}
