package top.babyzombie.addons.util.render;

import java.util.Optional;
import net.minecraft.client.renderer.rendertype.RenderType;

public interface GlowRenderTypeHolder {
    default Optional<RenderType> babyzombie$getGlowRenderType() { throw new UnsupportedOperationException("Implemented via Mixin"); }
}
