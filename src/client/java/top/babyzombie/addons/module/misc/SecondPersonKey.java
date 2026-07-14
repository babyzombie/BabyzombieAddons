package top.babyzombie.addons.module.misc;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.CameraType;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import top.babyzombie.addons.config.ModConfigManager;
import top.babyzombie.addons.util.KeyBindingUtil;

/**
 * Hold key to temporarily switch to second-person (front-facing) camera.
 * Releasing restores the previous perspective.
 */
public final class SecondPersonKey {
    public static KeyMapping KEY;

    private static boolean wasDown;
    private static CameraType previous = CameraType.FIRST_PERSON;

    private SecondPersonKey() {}

    public static void init() {
        if (KEY == null) {
            KEY = KeyBindingUtil.register(
                "key.babyzombieaddons.second_person",
                ModConfigManager.get().general.secondPerson);
        }
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            var player = Minecraft.getInstance().player;
            if (player == null) return;
            boolean down = KEY.isDown();
            if (down && !wasDown) {
                // Pressed: save current and switch to second person
                previous = client.options.getCameraType();
                if (previous == CameraType.THIRD_PERSON_FRONT) {
                    client.options.setCameraType(CameraType.FIRST_PERSON);
                } else {
                    client.options.setCameraType(CameraType.THIRD_PERSON_FRONT);
                }
            } else if (!down && wasDown) {
                // Released: restore previous
                if (client.options.getCameraType() == CameraType.THIRD_PERSON_FRONT) {
                    client.options.setCameraType(previous);
                }
            }
            wasDown = down;
        });
    }
}
