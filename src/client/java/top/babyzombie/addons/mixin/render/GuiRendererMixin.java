package top.babyzombie.addons.mixin.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.render.GuiItemAtlas;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.TrackingItemStackRenderState;
import net.minecraft.client.renderer.state.gui.BlitRenderState;
import net.minecraft.client.renderer.state.gui.GuiRenderState;
import net.minecraft.world.item.ItemDisplayContext;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import top.babyzombie.addons.util.render.ChatItemIconBridge;
import top.babyzombie.addons.util.render.ChatItemIconRenderable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * C2：把聊天字形的图标请求接入 {@code GuiRenderer} 的官方物品渲染管线。
 *
 * <p>在 {@code prepare()} 的 prepareText 之后、sortElements 之前排空
 * {@link ChatItemIconBridge} 的请求：用 ItemModelResolver 解析完整 ItemStack
 * （头颅/自定义模型/染色/光泽全支持），用 GuiItemAtlas 烘出图标槽位纹理，
 * 以 {@link BlitRenderState} 提交到聊天所在图层（原样复用 26.x 官方物品图标的渲染机制）。
 *
 * <p>隔一帧生效：字形 render() 在 addElementsToMeshes 阶段登记请求，
 * 下一帧 prepare 排空并提交 blit（肉眼不可见）。
 */
@Mixin(net.minecraft.client.gui.render.GuiRenderer.class)
public abstract class GuiRendererMixin {

    @Shadow
    private @Nullable GuiItemAtlas itemAtlas;

    @Shadow
    @Final
    private GuiRenderState renderState;

    @Shadow
    private GuiItemAtlas prepareItemAtlas(Set<Object> itemsInFrame, int slotTextureSize) {
        throw new AssertionError();
    }

    @Shadow
    private int getGuiScaleInvalidatingItemAtlasIfChanged() {
        throw new AssertionError();
    }

    @Inject(method = "prepare",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/render/GuiRenderer;prepareText()V",
                    shift = At.Shift.AFTER))
    private void babyzombie$afterPrepareText(CallbackInfo ci) {
        List<ChatItemIconBridge.ChatIconRequest> requests = ChatItemIconBridge.drain();
        if (requests.isEmpty()) return;
        this.babyzombie$submitChatItemIcons(requests);
    }

    @Unique
    private void babyzombie$submitChatItemIcons(List<ChatItemIconBridge.ChatIconRequest> requests) {
        try {
            Minecraft mc = Minecraft.getInstance();
            ItemModelResolver resolver = mc.getItemModelResolver();
            ClientLevel level = mc.level;
            LocalPlayer player = mc.player;

            // 1) 解析每个完整 ItemStack 为官方渲染状态
            List<TrackingItemStackRenderState> states = new ArrayList<>(requests.size());
            Set<Object> identities = new HashSet<>();
            for (ChatItemIconBridge.ChatIconRequest request : requests) {
                TrackingItemStackRenderState state = new TrackingItemStackRenderState();
                resolver.updateForTopItem(state, request.stack(), ItemDisplayContext.GUI, level, player, 0);
                states.add(state);
                identities.add(state.getModelIdentity());
            }
            if (states.isEmpty()) return;

            // 2) 确保 item-atlas 存在（提取阶段若已创建则复用，不关闭它）
            //    烘烤槽位固定 16px（与 gui.item 一致，保证清晰）；显示尺寸由 blit 决定
            if (this.itemAtlas == null) {
                int guiScale = this.getGuiScaleInvalidatingItemAtlasIfChanged();
                this.itemAtlas = this.prepareItemAtlas(identities, SLOT_BAKE_SIZE * guiScale);
            }

            // 3) 烘出每个物品的槽位并以 blit 提交到当前图层（与聊天文字同层）
            for (int i = 0; i < requests.size(); i++) {
                ChatItemIconBridge.ChatIconRequest request = requests.get(i);
                GuiItemAtlas.SlotView slot = this.itemAtlas.getOrUpdate(states.get(i));
                if (slot == null) continue;

                int size = ChatItemIconRenderable.ICON_SIZE; // 8px
                int x0 = (int) request.x();
                int y0 = (int) request.y(); // 与文本行顶对齐

                this.renderState.addBlitToCurrentLayer(new BlitRenderState(
                        RenderPipelines.GUI_TEXTURED_PREMULTIPLIED_ALPHA,
                        TextureSetup.singleTexture(slot.textureView(),
                                RenderSystem.getSamplerCache().getRepeat(FilterMode.NEAREST)),
                        request.pose(),
                        x0, y0, x0 + size, y0 + size,
                        slot.u0(), slot.u1(), slot.v0(), slot.v1(),
                        -1, request.scissor(), null));
            }
        } catch (Throwable t) {
            // 渲染兜底：任何异常都不应崩掉 GUI 绘制
        }
    }

    /** 图集烘烤槽位尺寸（固定 16px，gui.item 同款）。 */
    @Unique
    private static final int SLOT_BAKE_SIZE = 16;
}