package top.babyzombie.addons.module.slayer.itemtimer;

import net.minecraft.client.Minecraft;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import top.babyzombie.addons.config.ModConfigManager;
import top.babyzombie.addons.util.tracker.HypixelLocationTracker;
import top.babyzombie.addons.util.ItemUtils;
import top.babyzombie.addons.util.Scheduler;
import top.babyzombie.addons.util.ServerTick;

/**
 * Sound-based Reaper Armor ability detection with boots item verification.
 * Duration: 6s, Cooldown: 25s.
 */
public final class ReaperArmorTimer {
    public static long soundTime;
    public static long activeTime;
    public static long cooldownEnd;

    private ReaperArmorTimer() {}

    public static void init() {
        Scheduler.scheduleRepeating(120, () -> {
            if (soundTime > 0 && ServerTick.getTime() - soundTime > 6000) {
                soundTime = 0;
            }
        });
    }

    public static void onSound(String name, float pitch) {
        var cfg = ModConfigManager.get().slayer;
        if (!cfg.itemSkillTimers.reaperArmorTimer) return;
        if (!HypixelLocationTracker.getInstance().isInSkyblock()) return;

        if (!name.equals("zombie/remedy")) return;
        if (pitch != 1.0f) return;

        soundTime = ServerTick.getTime();
        Scheduler.schedule(2, ReaperArmorTimer::checkBoots);
    }

    private static void checkBoots() {
        var player = Minecraft.getInstance().player;
        if (player == null) return;
        ItemStack boots = player.getItemBySlot(EquipmentSlot.FEET);
        if (boots.isEmpty()) return;

        String id = ItemUtils.getSkyblockId(boots);
        if (id == null || !id.equals("REAPER_BOOTS")) return;

        var dyed = boots.get(DataComponents.DYED_COLOR);
        if (dyed == null) return;
        if (dyed.rgb() != 16711680) return;

        long now = ServerTick.getTime();
        activeTime = now + 6000;
        cooldownEnd = now + 25000;
        soundTime = 0;
    }
}
