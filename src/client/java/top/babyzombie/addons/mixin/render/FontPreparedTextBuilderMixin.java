package top.babyzombie.addons.mixin.render;

import net.minecraft.client.gui.font.TextRenderable;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.network.chat.Style;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix3x2f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import top.babyzombie.addons.util.render.ChatItemIconBridge;
import top.babyzombie.addons.util.render.ChatItemIconRenderable;
import top.babyzombie.addons.util.render.ChatItemIconSupport;

/**
 * 聊天物品图标渲染入口（C2 版）。
 *
 * <p>挂在 {@code Font$PreparedTextBuilder} 上——文本渲染管线里逐字符处理的入口，
 * 每个字符都带着 (style, codepoint) 和画笔坐标进入 {@code accept(int, Style, int)}。
 *
 * <p>遇到标记码点 {@link ChatItemIconSupport#MARKER} 且 style 携带 ShowItem 时，
 * 不画字形，插入一个 {@link ChatItemIconRenderable}：占一个 16px 图标槽位并前进画笔，
 * 渲染交给 {@code GuiRenderer} 的官方物品管线（见 GuiRendererMixin）。
 * 悬停 tooltip 走 style 里的 ShowItem，原版处理。
 */
@Mixin(targets = "net.minecraft.client.gui.Font$PreparedTextBuilder")
public abstract class FontPreparedTextBuilderMixin {

    @Shadow
    private float x;

    @Shadow
    private float y;

    @Shadow
    private void addGlyph(TextRenderable.Styled instance) {
    }

    @Inject(method = "accept(ILnet/minecraft/network/chat/Style;I)Z",
            at = @At("HEAD"), cancellable = true)
    private void babyzombie$onAcceptItemMarker(int position, Style style, int codepoint,
                                               CallbackInfoReturnable<Boolean> cir) {
        if (codepoint != ChatItemIconSupport.MARKER) return;

        // 只有 style 带 SHOW_ITEM 且能解析出物品时才替换；否则走原字形（缺字形→空）
        ItemStack stack = ChatItemIconSupport.stackFromStyle(style);
        if (stack == null) return;

        Matrix3x2f pose = ChatItemIconBridge.currentPose();
        if (pose == null) return; // 无文本上下文（不应发生），跳过渲染

        ScreenRectangle scissor = ChatItemIconBridge.currentScissor();
        this.addGlyph(new ChatItemIconRenderable(this.x, this.y, stack, pose, scissor, style));
        this.x += ChatItemIconRenderable.ICON_SIZE; // 画笔前进一个图标宽度
        cir.setReturnValue(true); // 吞掉原字形处理（返回 true = 继续迭代）
    }
}