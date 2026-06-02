package top.babyzombie.addons.module.mining;

import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import top.babyzombie.addons.config.ModConfigManager;
import top.babyzombie.addons.util.ChatUtils;

public final class NucleusAutoWarp {
    private static long warpReady;

    private NucleusAutoWarp() {}

    public static void init() {
        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            if (overlay) return;
            if (!ModConfigManager.get().mining.nucleusAutoWarp) return;
            if (!ChatUtils.stripColor(message.getString()).equals("You have already obtained this Crystal!")) return;

            if (warpReady == 0 || System.currentTimeMillis() - warpReady > 10_000) {
                ChatUtils.showMessage("§6§l[BZA] §b准备/warp nucleus,再点一下水晶进行传送");
                warpReady = System.currentTimeMillis();
            } else if (System.currentTimeMillis() - warpReady > 500) {
                ChatUtils.sendCommand("warp nucleus");
                warpReady = System.currentTimeMillis() + 10_000;
            }
        });
    }
}
