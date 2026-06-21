package top.babyzombie.addons.module.kuudra;

import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Items;
import top.babyzombie.addons.config.ModConfigManager;
import top.babyzombie.addons.util.ChatUtils;
import top.babyzombie.addons.util.tracker.HypixelLocationTracker;
import top.babyzombie.addons.util.ServerTick;

public final class EnderPearlRefill {
    private EnderPearlRefill() {}

    private static long lastRefill;
    private static boolean initialized;

    public static void init() {
        if (initialized) return;
        initialized = true;

        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            if (overlay) return;
            var loc = HypixelLocationTracker.getInstance();
            var cfg = ModConfigManager.get();
            boolean dungeonOn = cfg.dungeon.enderPearlRefill && loc.isInDungeon();
            boolean kuudraOn = cfg.kuudra.enderPearlRefill && loc.isInKuudra();
            if (!dungeonOn && !kuudraOn) return;

            String text = ChatUtils.stripColor(message.getString());
            if (!text.equals("[NPC] Elle: Okay adventurers, I will go and fish up Kuudra!")
                && !text.equals("Starting in 1 second.")) return;
            var player = Minecraft.getInstance().player;
            if (player == null) return;
            int total = countEnderPearls(player);
            if (total < 16) refill(16 - total);
        });

        UseItemCallback.EVENT.register((player, world, hand) -> {
            var cfg = ModConfigManager.get();
            var loc = HypixelLocationTracker.getInstance();
            if (!((cfg.dungeon.enderPearlRefill || cfg.kuudra.enderPearlRefill) && loc.isInSkyblock())) return InteractionResult.PASS;

            var held = player.getItemInHand(hand);
            if (held.getItem() != Items.ENDER_PEARL) return InteractionResult.PASS;
            if (ServerTick.getTime() - lastRefill < 2000) return InteractionResult.PASS;

            int total = countEnderPearls(player);
            if (total > 4) return InteractionResult.PASS;

            refill(16 - total);
            return InteractionResult.PASS;
        });
    }

    private static void refill(int amount) {
        lastRefill = ServerTick.getTime();
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
