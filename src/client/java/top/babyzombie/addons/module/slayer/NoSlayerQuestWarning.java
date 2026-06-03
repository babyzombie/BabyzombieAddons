package top.babyzombie.addons.module.slayer;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.core.component.DataComponents;
import top.babyzombie.addons.config.ModConfigManager;
import top.babyzombie.addons.util.ChatUtils;
import top.babyzombie.addons.util.HypixelLocationTracker;

/**
 * Reminds the user if they forgot to take a slayer quest after failing one.
 * Monitors champion_combat_xp on held weapon via entity death events.
 */
public final class NoSlayerQuestWarning {
    private static boolean slayerQuestCheck;
    private static int weaponXP;
    private static long checkStartTime;
    private static final long CHECK_DURATION = 5 * 60 * 1000; // 5 minutes

    private NoSlayerQuestWarning() {}

    public static void init() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            var cfg = ModConfigManager.get().slayer;
            if (!cfg.noslayerquest) return;
            if (!HypixelLocationTracker.getInstance().isInSkyblock()) return;

            if (slayerQuestCheck && System.currentTimeMillis() - checkStartTime > CHECK_DURATION) {
                slayerQuestCheck = false;
                weaponXP = 0;
            }
        });
    }

    static void onSlayerFail() {
        var cfg = ModConfigManager.get().slayer;
        if (!cfg.noslayerquest) return;
        if (!HypixelLocationTracker.getInstance().isInSkyblock()) return;

        slayerQuestCheck = true;
        weaponXP = 0;
        checkStartTime = System.currentTimeMillis();
    }

    static void onSlayerStart() {
        slayerQuestCheck = false;
        weaponXP = 0;
    }

    static void onEntityDeath() {
        var cfg = ModConfigManager.get().slayer;
        if (!cfg.noslayerquest) return;
        if (!slayerQuestCheck) return;
        if (!HypixelLocationTracker.getInstance().isInSkyblock()) return;

        var player = Minecraft.getInstance().player;
        if (player == null) return;
        var held = player.getMainHandItem();
        if (held.isEmpty()) return;

        var customData = held.get(DataComponents.CUSTOM_DATA);
        if (customData == null) return;
        var tag = customData.copyTag();
        if (!tag.contains("champion_combat_xp")) return;

        int xp = tag.getInt("champion_combat_xp").orElse(0);
        if (xp > weaponXP) {
            ChatUtils.showTranslatableTitle("slayer.noslayerquest.warning", 0, 35, 5);
        }
        weaponXP = xp;
    }
}
