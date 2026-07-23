package top.babyzombie.addons.module.kuudra;

import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.HitResult;
import top.babyzombie.addons.config.ModConfigManager;
import top.babyzombie.addons.util.ItemUtils;
import top.babyzombie.addons.util.tracker.HypixelLocationTracker;

/**
 * 在 Kuudra 里拿着有 etherwarp 的传送剑（AOTE/AOTV）蹲下右键时，
 * 如果目标方块上方是岩浆则取消这次传送。
 */
public final class KuudraEtherwarpLavaPrevent {
    private KuudraEtherwarpLavaPrevent() {}

    private static final String AOTE = "ASPECT_OF_THE_END";
    private static final String AOTV = "ASPECT_OF_THE_VOID";

    public static void init() {
        UseItemCallback.EVENT.register((player, world, hand) -> {
            if (!ModConfigManager.get().kuudra.etherwarpLavaPrevent) return InteractionResult.PASS;
            if (!HypixelLocationTracker.getInstance().isInKuudra()) return InteractionResult.PASS;
            if (!player.isShiftKeyDown()) return InteractionResult.PASS;

            var held = player.getItemInHand(hand);
            String sbId = ItemUtils.getSkyblockId(held);
            if (sbId == null || (!sbId.equals(AOTE) && !sbId.equals(AOTV))) return InteractionResult.PASS;

            // 检查是否有 etherwarp 技能
            var customData = held.get(DataComponents.CUSTOM_DATA);
            if (customData == null) return InteractionResult.PASS;
            if (customData.copyTag().getInt("ethermerge").orElse(0) != 1) return InteractionResult.PASS;

            // 方块射线，无视途中的实体
            var eyePos = player.getEyePosition();
            var lookVec = player.getViewVector(0.0F);
            var farPoint = eyePos.add(lookVec.scale(62.0));
            var hit = world.clip(new ClipContext(eyePos, farPoint,
                    ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));
            if (hit.getType() == HitResult.Type.MISS) return InteractionResult.PASS;
            BlockPos abovePos = hit.getBlockPos().above();
            if (world.getBlockState(abovePos).is(Blocks.LAVA)) {
                return InteractionResult.FAIL;
            }

            return InteractionResult.PASS;
        });
    }
}
