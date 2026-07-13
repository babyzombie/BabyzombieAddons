package top.babyzombie.addons.module.mining;

import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import top.babyzombie.addons.config.ModConfigManager;
import top.babyzombie.addons.util.ChatUtils;
import top.babyzombie.addons.util.ServerTick;
import top.babyzombie.addons.util.tracker.HypixelLocationTracker;

public final class NucleusAutoWarp {
    private static long warpReady;

    private NucleusAutoWarp() {}

    public static void init() {
        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            if (overlay) return;
            if (!ModConfigManager.get().mining.crystalHollows.nucleusAutoWarp) return;
            if(!HypixelLocationTracker.getInstance().isIn("Crystal Hollows")) return;
            if (!ChatUtils.stripColor(message.getString()).equals("You have already obtained this Crystal!")) return;

            if (warpReady == 0 || ServerTick.getTime() - warpReady > 10_000) {
                ChatUtils.showMessage(net.minecraft.network.chat.Component.translatable("babyzombieaddons.ch.nucleus").getString());
                warpReady = ServerTick.getTime();
            } else if (ServerTick.getTime() - warpReady > 500) {
                ChatUtils.sendCommand("warp nucleus");
                warpReady = ServerTick.getTime() + 10_000;
            }
        });
    }
}
