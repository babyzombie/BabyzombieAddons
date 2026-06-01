package top.babyzombie.addons.module.kuudra;

import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Items;
import top.babyzombie.addons.config.ModConfigManager;
import top.babyzombie.addons.util.ChatUtils;
import top.babyzombie.addons.util.HypixelLocationTracker;

public final class EnderPearlRefill {
    private EnderPearlRefill() {}

    private static long lastRefill;

    public static void init() {
        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            if (!ModConfigManager.get().kuudra.enderPearlRefill) return;
            if (overlay) return;
            String text = ChatUtils.stripColor(message.getString());
            if (HypixelLocationTracker.getInstance().isInKuudra()
                    && (text.contains("Okay adventurers, I will go and fish up Kuudra")
                    || text.contains("Starting in 1 second."))) {
                var player = Minecraft.getInstance().player;
                if (player == null) return;
                int total = countEnderPearls(player);
                if (total < 16) refill(16 - total);
            }
        });

        UseItemCallback.EVENT.register((player, world, hand) -> {
            if (!ModConfigManager.get().kuudra.enderPearlRefill) return InteractionResult.PASS;
            if (!HypixelLocationTracker.getInstance().isInSkyblock()) return InteractionResult.PASS;
            var held = player.getItemInHand(hand);
            if (held.getItem() != Items.ENDER_PEARL) return InteractionResult.PASS;
            if (System.currentTimeMillis() - lastRefill < 2000) return InteractionResult.PASS;

            int total = countEnderPearls(player);
            if (total >= 16) return InteractionResult.PASS;

            refill(16 - total);
            return InteractionResult.PASS;
        });
    }

    private static void refill(int amount) {
        lastRefill = System.currentTimeMillis();
        ChatUtils.sendCommand("gfs ender_pearl " + amount);
    }

    private static int countEnderPearls(net.minecraft.world.entity.player.Player player) {
        int count = 0;
        for (var stack : player.getInventory().getNonEquipmentItems()) {
            if (stack.getItem() == Items.ENDER_PEARL
                    && stack.getDisplayName().getString().contains("Ender Pearl"))
                count += stack.getCount();
        }
        return count;
    }
}
