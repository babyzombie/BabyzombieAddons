package top.babyzombie.addons.module.slayer;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import top.babyzombie.addons.config.ModConfigManager;
import top.babyzombie.addons.util.ItemUtils;
import top.babyzombie.addons.util.HypixelLocationTracker;
import top.babyzombie.addons.util.ServerTick;

/**
 * Sound-based Reaper Armor ability detection with boots item verification.
 * Duration: 6s, Cooldown: 25s.
 */
public final class ReaperArmorTimer {
    static long soundTime;
    static long activeTime;
    static long cooldownEnd;

    private ReaperArmorTimer() {}

    public static void init() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            var cfg = ModConfigManager.get().slayer;
            if (!cfg.reaperArmorTimer) return;
            if (!HypixelLocationTracker.getInstance().isInSkyblock()) return;

            long now = ServerTick.getTime();
            if (soundTime > 0 && now - soundTime > 6000) {
                soundTime = 0;
            }
        });
    }

    /**
     * Called when a sound is played. Checks if it's the reaper sound and verifies boots.
     */
    public static void onSound(String name, float pitch) {
        var cfg = ModConfigManager.get().slayer;
        if (!cfg.reaperArmorTimer) return;
        if (!HypixelLocationTracker.getInstance().isInSkyblock()) return;

        if (!name.equals("zombie.remedy")) return;
        if (pitch != 1.0f) return;

        soundTime = ServerTick.getTime();
        checkBoots();
    }

    private static void checkBoots() {
        var player = Minecraft.getInstance().player;
        if (player == null) return;
        ItemStack boots = player.getItemBySlot(EquipmentSlot.FEET);
        if (boots.isEmpty()) return;

        String id = ItemUtils.getSkyblockId(boots);
        if (id == null || !id.equals("REAPER_BOOTS")) return;

        // Check if boots are dyed red (0xFF0000 = 16711680)
        var customData = boots.get(DataComponents.CUSTOM_DATA);
        if (customData == null) return;
        var tag = customData.copyTag();
        var display = tag.getCompound("display").orElse(null);
        if (display == null) return;
        int color = display.getInt("color").orElse(0);
        if (color != 16711680) return;

        long now = ServerTick.getTime();
        activeTime = now + 6000;
        cooldownEnd = now + 25000;
        soundTime = 0;
    }

}
