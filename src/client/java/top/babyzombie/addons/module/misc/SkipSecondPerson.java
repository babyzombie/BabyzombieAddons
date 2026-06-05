package top.babyzombie.addons.module.misc;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import top.babyzombie.addons.config.ModConfigManager;

/**
 * Skips second-person (third-person front) view on quick F5 presses.
 * Long-pressing F5 past the delay switches to second-person on release.
 * 0 = off, 20 ticks = 1 second.
 */
public final class SkipSecondPerson {
    private static int heldTicks;

    private SkipSecondPerson() {}

    public static void init() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            int delay = ModConfigManager.get().general.skipSecondPerson;
            if (delay <= 0) return;

            var options = Minecraft.getInstance().options;
            if (options.keyTogglePerspective.isDown()) {
                heldTicks++;
                if (heldTicks < delay && options.getCameraType() == CameraType.THIRD_PERSON_FRONT) {
                    options.setCameraType(CameraType.FIRST_PERSON);
                }
            } else if (heldTicks > 0) {
                if (heldTicks >= delay) {
                    options.setCameraType(CameraType.THIRD_PERSON_FRONT);
                }
                heldTicks = 0;
            }
        });
    }
}
