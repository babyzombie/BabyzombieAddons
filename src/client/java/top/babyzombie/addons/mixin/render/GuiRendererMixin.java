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
import top.babyzombie.addons.util.render.ChatItemIconSubmitter;

import java.util.Set;

/**
 * C2：把聊天物品图标的烘烤 + 提交接入 {@code GuiRenderer} 的官方物品渲染管线。
 *
 * <p>{@code prepare()} 期间把当前 GuiRenderer 挂到 {@link ChatItemIconBridge}；
 * 聊天文本打包字形（见 GuiRenderStateMixin → addGlyphToCurrentLayer）发现物品图标字形时，
 * 调用 {@link #submitChatIconBlit} 在<b>聊天所在节点</b>提交 blit——
 * 图标与聊天文字同层，不会被抬到暂停页模糊等覆盖层之上，也无需隔帧。
 */
@Mixin(net.minecraft.client.gui.render.GuiRenderer.class)
public abstract class GuiRendererMixin implements ChatItemIconSubmitter {

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

    @Inject(method = "prepare", at = @At("HEAD"))
    private void babyzombie$prepareHead(CallbackInfo ci) {
        ChatItemIconBridge.setCurrentRenderer(this);
    }

    @Inject(method = "prepare", at = @At("RETURN"))
    private void babyzombie$prepareReturn(CallbackInfo ci) {
        ChatItemIconBridge.setCurrentRenderer(null);
    }

    /**
     * 烘烤一个聊天物品图标并提交 blit 到当前节点。
     * 调用时机：GuiRenderStateMixin 在 addGlyphToCurrentLayer 发现物品图标字形时，
     * 此刻 {@code renderState.current} == 该聊天文本所在节点，blit 与其同层。
     */
    @Override
    @Unique
    public void submitChatIconBlit(ChatItemIconRenderable icon) {
        try {
            Minecraft mc = Minecraft.getInstance();
            ItemModelResolver resolver = mc.getItemModelResolver();
            ClientLevel level = mc.level;
            LocalPlayer player = mc.player;

            // 解析完整 ItemStack 为官方渲染状态
            TrackingItemStackRenderState state = new TrackingItemStackRenderState();
            resolver.updateForTopItem(state, icon.stack(), ItemDisplayContext.GUI, level, player, 0);
            Set<Object> identities = Set.of(state.getModelIdentity());

            // 确保 item-atlas 存在（提取阶段若已创建则复用，不关闭它）；烘烤槽位固定 16px
            if (this.itemAtlas == null) {
                int guiScale = this.getGuiScaleInvalidatingItemAtlasIfChanged();
                this.itemAtlas = this.prepareItemAtlas(identities, SLOT_BAKE_SIZE * guiScale);
            }
            if (this.itemAtlas == null) return;

            GuiItemAtlas.SlotView slot = this.itemAtlas.getOrUpdate(state);
            if (slot == null) return;

            int size = ChatItemIconRenderable.ICON_SIZE; // 8px
            int x0 = (int) icon.x();
            int y0 = (int) icon.y();

            this.renderState.addBlitToCurrentLayer(new BlitRenderState(
                    RenderPipelines.GUI_TEXTURED_PREMULTIPLIED_ALPHA,
                    TextureSetup.singleTexture(slot.textureView(),
                            RenderSystem.getSamplerCache().getRepeat(FilterMode.NEAREST)),
                    icon.pose(),
                    x0, y0, x0 + size, y0 + size,
                    slot.u0(), slot.u1(), slot.v0(), slot.v1(),
                    -1, icon.scissor(), null));
        } catch (Throwable t) {
            // 渲染兜底：任何异常都不应崩掉 GUI 绘制
        }
    }

    /** 图集烘烤槽位尺寸（固定 16px，gui.item 同款）。 */
    @Unique
    private static final int SLOT_BAKE_SIZE = 16;
}