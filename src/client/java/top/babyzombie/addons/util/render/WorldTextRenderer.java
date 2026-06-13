package top.babyzombie.addons.util.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.font.TextRenderable;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.StagedVertexBuffer;
import net.minecraft.network.chat.Component;
import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.vertex.*;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import org.joml.Matrix4f;
import org.joml.Vector4f;

public final class WorldTextRenderer {
    private static final StagedVertexBuffer VB = new StagedVertexBuffer(
        () -> "bza_text", net.minecraft.client.renderer.rendertype.RenderType.SMALL_BUFFER_SIZE);
    private static final Matrix4f mat = new Matrix4f();

    public static void renderString(WorldRenderContext ctx, String text, double x,double y,double z, int color, float scale, boolean throughWalls){
        renderString(ctx.worldState().cameraRenderState, text, x, y, z, color, scale, throughWalls, 0);
    }
    public static void renderString(WorldRenderContext ctx, String text, double x,double y,double z, int color, float scale, boolean throughWalls, float yOff){
        renderString(ctx.worldState().cameraRenderState, text, x, y, z, color, scale, throughWalls, yOff);
    }
    public static void renderString(CameraRenderState cam, String text, double x,double y,double z, int color, float scale, boolean throughWalls){
        renderString(cam, text, x, y, z, color, scale, throughWalls, 0);
    }
    public static void renderString(CameraRenderState cam, String t, double x,double y,double z, int color, float s, boolean tw, float yOff){
        var f = Minecraft.getInstance().font; if(f==null)return;
        mat.identity().translate((float)(x-cam.pos.x),(float)(y-cam.pos.y),(float)(z-cam.pos.z))
            .rotate(cam.orientation).scale(s,-s,s);
        var prep = f.prepareText(Component.literal(t).getVisualOrderText(),0,0,color,false,false,0);
        var sampler = RenderSystem.getSamplerCache().getClampToEdge(FilterMode.NEAREST);

        final RenderPipeline[] lastP = {null};
        final TextureSetup[] lastT = {null};
        StagedVertexBuffer.Draw[] d = new StagedVertexBuffer.Draw[1];

        prep.visit(new Font.GlyphVisitor() {
            public void acceptGlyph(TextRenderable.Styled g) { draw(g); }
            public void acceptEffect(TextRenderable g) { draw(g); }
            void draw(TextRenderable g) {
                RenderPipeline pipe = tw ? RenderPipelines.TEXT_SEE_THROUGH
                    : (g.guiPipeline() == RenderPipelines.GUI_TEXT_GRAYSCALE ? RenderPipelines.TEXT_GRAYSCALE : RenderPipelines.TEXT);
                TextureSetup ts = TextureSetup.singleTextureWithLightmap(g.textureView(), sampler);
                // Simple: just use one pipeline. Use the first seen glyph's pipeline.
                if (lastP[0] == null) { lastP[0] = pipe; lastT[0] = ts;
                    d[0] = VB.appendDraw(pipe.getVertexFormatBinding(0), PrimitiveTopology.QUADS);
                }
                g.render(mat, VB.getVertexBuilder(d[0]), 0xF000F0, false);
            }
        });
        if (d[0] == null) return;

        VB.upload();
        var info = VB.getExecuteInfo(d[0]); if(info==null)return;
        var main = Minecraft.getInstance().gameRenderer.mainRenderTarget();
        if(main.getColorTextureView()==null)return;
        var enc = RenderSystem.getDevice().createCommandEncoder();
        try(var rp = enc.createRenderPass(()->"bza_txt",main.getColorTextureView(),
                java.util.Optional.empty(),main.getDepthTextureView(),java.util.OptionalDouble.empty())){
            rp.setPipeline(lastP[0]); RenderSystem.bindDefaultUniforms(rp);
            if(lastT[0].texure0()!=null) rp.bindTexture("Sampler0", lastT[0].texure0(), lastT[0].sampler0());
            rp.setUniform("DynamicTransforms",RenderSystem.getDynamicUniforms().writeTransform(RenderSystem.getModelViewMatrixCopy(),new Vector4f(1,1,1,1)));
            rp.setVertexBuffer(0,info.vertexBuffer().slice()); rp.setIndexBuffer(info.indexBuffer(),info.indexType());
            rp.drawIndexed(info.indexCount(),1,info.firstIndex(),info.baseVertex(),0);
        }
        enc.submit(); VB.endDraw(); VB.endFrame();
    }
}
