package top.babyzombie.addons.mixin.render;

import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import top.babyzombie.addons.util.render.GlowRenderTypeHolder;
import top.babyzombie.addons.util.render.GlowRenderTypes;

import java.util.Optional;

@Mixin(RenderType.class)
public class RenderTypeMixin implements GlowRenderTypeHolder {
    @Unique private @Nullable Optional<RenderType> outlineDepth;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void init(CallbackInfo ci, @Local(name = "state") RenderSetup state) {
        var a = (RenderSetupAccessor) (Object) state;
        this.outlineDepth = a.getOutlineProperty() == RenderSetup.OutlineProperty.AFFECTS_OUTLINE
            ? a.getTextureBindings().values().stream().findFirst().map(v -> {
                try { return GlowRenderTypes.depthOutline((Identifier) v.getClass().getMethod("location").invoke(v), a.getPipeline().isCull()); }
                catch (Exception ignored) { return null; }
            }) : Optional.empty();
    }

    @Override public Optional<RenderType> babyzombie$getGlowRenderType() { return outlineDepth != null ? outlineDepth : Optional.empty(); }
}
