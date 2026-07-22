package top.babyzombie.addons.mixin.item;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import top.babyzombie.addons.config.ModConfigManager;
import top.babyzombie.addons.module.garden.GreenhouseDetector;
import top.babyzombie.addons.module.garden.Plot;
import top.babyzombie.addons.module.garden.PlotUtils;
import top.babyzombie.addons.util.tracker.HypixelLocationTracker;

import java.util.Set;

@Mixin(BlockItem.class)
public class BlockItemMixin {

    /** 水晶矿洞中可放置的石质方块。石砖等砖变种不可放置。 */
    @Unique
    private static final Set<Block> CRYSTAL_HOLLOWS_PLACEABLE = Set.of(
        Blocks.STONE,
        Blocks.COBBLESTONE,
        Blocks.ANDESITE,
        Blocks.POLISHED_ANDESITE,
        Blocks.DIORITE,
        Blocks.POLISHED_DIORITE,
        Blocks.GRANITE,
        Blocks.POLISHED_GRANITE,
        Blocks.TORCH
    );

    @Inject(method = "place", at = @At("HEAD"), cancellable = true)
    private void cancelClientPlacement(BlockPlaceContext placeContext, CallbackInfoReturnable<InteractionResult> cir) {
        // 仅客户端 + 开关开启 + 在 SkyBlock
        if (!placeContext.getLevel().isClientSide()) return;
        if (!ModConfigManager.get().skyblock.disableBlockPlacePrediction) return;
        if (!HypixelLocationTracker.getInstance().isInSkyblock()) return;

        // 排除可放置区域
        if (canPlaceBlocksHere(placeContext)) return;

        // 取消客户端预测放置，数据包仍会由 startPrediction() 发出
        cir.setReturnValue(InteractionResult.SUCCESS);
    }

    @Unique
    private static boolean canPlaceBlocksHere(BlockPlaceContext context) {
        HypixelLocationTracker tracker = HypixelLocationTracker.getInstance();
        ItemStack held = context.getItemInHand();

        // 私人岛屿 — 全部可放置
        if (tracker.isIn("Private Island")) return true;

        // 水晶矿洞 — 只有特定石质方块可放置（石砖等砖变种不可）
        if (tracker.isIn("Crystal Hollows")) {
            return held.getItem() instanceof BlockItem blockItem
                && CRYSTAL_HOLLOWS_PLACEABLE.contains(blockItem.getBlock());
        }

        // 花园 — 普通地皮可放置，温室和谷仓不可
        if (tracker.isIn("Garden")) {
            // 谷仓中心不可建造区域
            if (isInBarnNoBuildZone(context)) return false;

            Plot plot = PlotUtils.getCurrentPlot();
            if (plot == null) return true;          // 无法判断，默认放行
            if (GreenhouseDetector.isCurrentPlotGreenhouse()) {
                return isInGreenhouseBuildArea(plot, context);  // 温室 10×10 建造区
            }
            return true;                             // 普通地皮 — 可放置
        }

        // 其他 SkyBlock 区域 — 不可放置
        return false;
    }

    /** 温室内 10×10 可建造区域（相对地皮偏移 43-52，高度 73-74）。 */
    @Unique
    private static boolean isInGreenhouseBuildArea(Plot plot, BlockPlaceContext context) {
        BlockPos pos = context.getClickedPos();
        return pos.getX() >= plot.minX() + 43 && pos.getX() <= plot.minX() + 52
            && pos.getZ() >= plot.minZ() + 43 && pos.getZ() <= plot.minZ() + 52
            && pos.getY() >= 73 && pos.getY() <= 74;
    }

    /** 谷仓中心不可建造区域：X -33~35, Z -47~-5。 */
    @Unique
    private static boolean isInBarnNoBuildZone(BlockPlaceContext context) {
        BlockPos pos = context.getClickedPos();
        return pos.getX() >= -33 && pos.getX() <= 35
            && pos.getZ() >= -47 && pos.getZ() <= -5;
    }
}
