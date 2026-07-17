package top.babyzombie.addons.module.slayer.itemtimer;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import top.babyzombie.addons.config.ModConfigManager;
import top.babyzombie.addons.util.ChatUtils;
import top.babyzombie.addons.util.ServerTick;

/**
 * Pigman Sword cooldown detection via sound, gated by playerInteract + held item check.
 * Holy Ice detection follows the same pattern for Rift.
 */
public final class PigmanSwordTimer {
    public static long time;
    private static boolean pigmanListening;
    private static long pigmanListenStart;
    private static boolean holyIceListening;
    private static long holyIceListenStart;

    private PigmanSwordTimer() {}

    public static void init() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            var player = Minecraft.getInstance().player;
            if (player == null) return;
            var held = player.getMainHandItem();
            if (held.isEmpty()) return;
            String heldName = ChatUtils.stripColor(held.getDisplayName().getString());

            // Pigman Sword — enable sound listener on right-click while holding
            if (ModConfigManager.get().slayer.itemSkillTimers.pigmanSwordTimer
                    && client.options.keyUse.isDown()
                    && heldName.contains("Pigman Sword")
                    && time == 0) { // not in cooldown
                pigmanListening = true;
                pigmanListenStart = ServerTick.getTime();
            }
            if (pigmanListening && ServerTick.getTime() - pigmanListenStart > 500) {
                pigmanListening = false;
            }

            // Holy Ice — enable sound listener on right-click while holding
            if (ModConfigManager.get().slayer.itemSkillTimers.holyIceTimer
                    && client.options.keyUse.isDown()
                    && heldName.contains("Holy Ice")) {
                holyIceListening = true;
                holyIceListenStart = ServerTick.getTime();
            }
            if (holyIceListening && ServerTick.getTime() - holyIceListenStart > 500) {
                holyIceListening = false;
            }
        });
    }

    /**
     * Called from PlaySoundEvents when a sound plays.
     */
    public static void onSound(String name) {
        if (name.equals("zpigangry")) {
            if (!ModConfigManager.get().slayer.itemSkillTimers.pigmanSwordTimer) return;
            if (!pigmanListening) return;
            time = ServerTick.getTime();
            pigmanListening = false;
        }
        if (name.equals("drink")) {
            if (!ModConfigManager.get().slayer.itemSkillTimers.holyIceTimer) return;
            if (!holyIceListening) return;
            HolyIceTimer.activated = true;
            HolyIceTimer.time = ServerTick.getTime();
            holyIceListening = false;
        }
    }
}
