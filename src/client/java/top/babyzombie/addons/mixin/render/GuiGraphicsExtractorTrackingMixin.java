package top.babyzombie.addons.mixin.render;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import top.babyzombie.addons.util.HudSourceTracker;

/**
 * Intercepts drawing methods on {@link GuiGraphicsExtractor} during HUD rendering.
 */
@Mixin(GuiGraphicsExtractor.class)
public class GuiGraphicsExtractorTrackingMixin {

    // ── text: funnel overload (ALL text calls converge here) ──

    @Inject(
            method = "text(Lnet/minecraft/client/gui/Font;Lnet/minecraft/util/FormattedCharSequence;IIIZ)V",
            at = @At("HEAD")
    )
    private void onFormattedText(Font font, FormattedCharSequence str, int x, int y, int color,
                                  boolean dropShadow, CallbackInfo ci) {
        if (!HudSourceTracker.isTracking()) return;
        int w = font.width(str);
        HudSourceTracker.record(x, y, x + w, y + font.lineHeight);
    }

    // ── fill ──

    @Inject(method = "fill(IIIII)V", at = @At("HEAD"))
    private void onFill(int x0, int y0, int x1, int y1, int col, CallbackInfo ci) {
        if (!HudSourceTracker.isTracking()) return;
        HudSourceTracker.record(
                Math.min(x0, x1), Math.min(y0, y1),
                Math.max(x0, x1), Math.max(y0, y1)
        );
    }

    @Inject(method = "fill(Lcom/mojang/blaze3d/pipeline/RenderPipeline;IIIII)V", at = @At("HEAD"))
    private void onFillPipeline(RenderPipeline pipeline, int x0, int y0, int x1, int y1, int col, CallbackInfo ci) {
        if (!HudSourceTracker.isTracking()) return;
        HudSourceTracker.record(
                Math.min(x0, x1), Math.min(y0, y1),
                Math.max(x0, x1), Math.max(y0, y1)
        );
    }

    // ── blitSprite ──

    @Inject(
            method = "blitSprite(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIII)V",
            at = @At("HEAD")
    )
    private void onBlitSprite(RenderPipeline renderPipeline, Identifier location,
                               int x, int y, int width, int height, CallbackInfo ci) {
        if (!HudSourceTracker.isTracking()) return;
        HudSourceTracker.record(x, y, x + width, y + height);
    }

    @Inject(
            method = "blitSprite(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIIII)V",
            at = @At("HEAD")
    )
    private void onBlitSpriteColored(RenderPipeline renderPipeline, Identifier location,
                                      int x, int y, int width, int height, int color, CallbackInfo ci) {
        if (!HudSourceTracker.isTracking()) return;
        HudSourceTracker.record(x, y, x + width, y + height);
    }

    // ── item ──

    @Inject(method = "item(Lnet/minecraft/world/item/ItemStack;II)V", at = @At("HEAD"))
    private void onItem(ItemStack itemStack, int x, int y, CallbackInfo ci) {
        if (!HudSourceTracker.isTracking()) return;
        HudSourceTracker.record(x, y, x + 16, y + 16);
    }

    @Inject(method = "item(Lnet/minecraft/world/item/ItemStack;III)V", at = @At("HEAD"))
    private void onItemSeeded(ItemStack itemStack, int x, int y, int seed, CallbackInfo ci) {
        if (!HudSourceTracker.isTracking()) return;
        HudSourceTracker.record(x, y, x + 16, y + 16);
    }

    @Inject(
            method = "item(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/item/ItemStack;III)V",
            at = @At("HEAD")
    )
    private void onItemEntity(LivingEntity owner, ItemStack stack, int x, int y, int seed, CallbackInfo ci) {
        if (!HudSourceTracker.isTracking()) return;
        HudSourceTracker.record(x, y, x + 16, y + 16);
    }

    @Inject(method = "fakeItem(Lnet/minecraft/world/item/ItemStack;III)V", at = @At("HEAD"))
    private void onFakeItem(ItemStack stack, int x, int y, int seed, CallbackInfo ci) {
        if (!HudSourceTracker.isTracking()) return;
        HudSourceTracker.record(x, y, x + 16, y + 16);
    }
}
