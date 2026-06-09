package top.babyzombie.addons.module.garden;

import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import top.babyzombie.addons.config.ModConfigManager;
import top.babyzombie.addons.util.ChatUtils;
import top.babyzombie.addons.util.tracker.HypixelLocationTracker;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Reads yaw/pitch from garden signs and sets player rotation.
 * Uses regex to match values from sign text lines.
 */
public final class SignAutoRotate {

    private static final Pattern YAW_PATTERN = Pattern.compile("yaw[:\\s]*([\\d.\\-]+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern PITCH_PATTERN = Pattern.compile("pitch[:\\s]*([\\d.\\-]+)", Pattern.CASE_INSENSITIVE);

    private SignAutoRotate() {}

    public static void init() {
        if (!ModConfigManager.get().garden.signAutoRotate) return;

        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            if (!world.isClientSide() || !HypixelLocationTracker.getInstance().isInSkyblock())
                return InteractionResult.PASS;

            if (!(world.getBlockEntity(hitResult.getBlockPos()) instanceof SignBlockEntity sign))
                return InteractionResult.PASS;

            var frontText = sign.getFrontText();
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 4; i++)
                sb.append(ChatUtils.stripColor(frontText.getMessage(i, false).getString())).append(' ');
            String text = sb.toString();

            Matcher ym = YAW_PATTERN.matcher(text);
            Matcher pm = PITCH_PATTERN.matcher(text);

            if (ym.find() && pm.find()) {
                ItemStack held = player.getItemInHand(hand);
                if (held.getItem() instanceof HoeItem || held.getItem() instanceof AxeItem) {
                    try {
                        float yaw = Float.parseFloat(ym.group(1));
                        float pitch = Float.parseFloat(pm.group(1));
                        var p = Minecraft.getInstance().player;
                        if (p != null) {
                            p.setYRot(yaw);
                            p.setXRot(pitch);
                        }
                    } catch (NumberFormatException ignored) {}
                    return InteractionResult.FAIL;
                }
            }

            return InteractionResult.PASS;
        });
    }
}
