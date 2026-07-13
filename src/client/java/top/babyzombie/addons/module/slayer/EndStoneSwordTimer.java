package top.babyzombie.addons.module.slayer;

import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import top.babyzombie.addons.config.ModConfig;
import top.babyzombie.addons.config.ModConfigManager;
import top.babyzombie.addons.util.ChatUtils;
import top.babyzombie.addons.util.ItemUtils;
import top.babyzombie.addons.util.ServerTick;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Tracks End Stone Sword / Extreme Focus damage resistance and prevents double-use.
 */
public final class EndStoneSwordTimer {
    static long time;
    static int resistance;
    static int damage;
    private static final Pattern RESIST_PATTERN =
        Pattern.compile("You now have ([0-9]+)% Damage Resistance for 5s and \\+([0-9]+)% damage on your next hit within 5s!");

    private EndStoneSwordTimer() {}

    public static void init() {
        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            if (overlay) return;
            var cfg = ModConfigManager.get().slayer;
            if (cfg.itemSkillTimers.endStoneSwordTimer == ModConfig.EndStoneSwordMode.OFF) return;

            String text = ChatUtils.stripColor(message.getString());
            Matcher m = RESIST_PATTERN.matcher(text);
            if (m.find()) {
                resistance = Integer.parseInt(m.group(1));
                damage = Integer.parseInt(m.group(2));
                time = ServerTick.getTime() + 5000;
            }
        });

        UseItemCallback.EVENT.register((player, world, hand) ->
            shouldPreventUse(player, hand) ? InteractionResult.FAIL : InteractionResult.PASS);
    }

    /**
     * Returns true if the timer should prevent re-using the sword.
     */
    public static boolean shouldPreventUse(Player player, InteractionHand hand) {
        var cfg = ModConfigManager.get().slayer;
        if (cfg.itemSkillTimers.endStoneSwordTimer != ModConfig.EndStoneSwordMode.PREVENT_REUSE
                && cfg.itemSkillTimers.endStoneSwordTimer != ModConfig.EndStoneSwordMode.BOTH) {
            return false;
        }
        ItemStack held = player.getItemInHand(hand);
        if (held.isEmpty()) return false;
        String id = ItemUtils.getSkyblockId(held);
        if (!"END_STONE_SWORD".equals(id)) return false;
        return isActive();
    }

    static boolean isActive() {
        return time > ServerTick.getTime();
    }
}
