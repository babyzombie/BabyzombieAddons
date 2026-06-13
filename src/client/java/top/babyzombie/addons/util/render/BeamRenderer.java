package top.babyzombie.addons.util.render;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.StagedVertexBuffer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import com.mojang.math.Axis;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalDouble;

public final class BeamRenderer {
    private static final Identifier BEAM_TEX = Identifier.fromNamespaceAndPath("minecraft", "textures/entity/beacon/beacon_beam.png");
    private static final int LIGHT = 0xF000F0;
    private static final StagedVertexBuffer VB = new StagedVertexBuffer(
        () -> "bza_beam", net.minecraft.client.renderer.rendertype.RenderType.SMALL_BUFFER_SIZE);
    private static final List<StagedVertexBuffer.Draw> beamDraws = new java.util.ArrayList<>();

    static {
        net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents.END_MAIN.register(ctx -> {
            if (beamDraws.isEmpty()) return;
            VB.upload();
            var t = Minecraft.getInstance().gameRenderer.mainRenderTarget();
            if (t.getColorTextureView()==null) { beamDraws.clear(); VB.endDraw(); VB.endFrame(); return; }
            var tex = Minecraft.getInstance().getTextureManager().getTexture(BEAM_TEX);
            var e = RenderSystem.getDevice().createCommandEncoder();
            try(var r = e.createRenderPass(()->"bza_beam",t.getColorTextureView(),Optional.empty(),t.getDepthTextureView(),OptionalDouble.empty())){
                r.setPipeline(RenderPipelines.BEACON_BEAM_TRANSLUCENT); RenderSystem.bindDefaultUniforms(r);
                r.bindTexture("Sampler0",tex.getTextureView(),RenderSystem.getSamplerCache().getRepeat(com.mojang.blaze3d.textures.FilterMode.NEAREST));
                r.setUniform("DynamicTransforms",RenderSystem.getDynamicUniforms().writeTransform(RenderSystem.getModelViewMatrixCopy(),new Vector4f(1,1,1,1)));
                for (var d : beamDraws) {
                    var info = VB.getExecuteInfo(d); if (info==null) continue;
                    r.setVertexBuffer(0,info.vertexBuffer().slice()); r.setIndexBuffer(info.indexBuffer(),info.indexType());
                    r.drawIndexed(info.indexCount(),1,info.firstIndex(),info.baseVertex(),0);
                }
            }
            e.submit(); VB.endDraw(); VB.endFrame();
            beamDraws.clear();
        });
    }

    public static void drawBeam(WorldRenderContext ctx, double x,double y,double z, double h, float w, int c){
        drawBeam(ctx.worldState().cameraRenderState,x,y,z,h,w,c);
    }
    public static void drawBeam(CameraRenderState cam, double x,double y,double z, double h, float w, int argb){
        var mc = Minecraft.getInstance(); long t = Objects.requireNonNull(mc.level).getGameTime();
        float a = Math.floorMod(t,40)+mc.getDeltaTracker().getGameTimeDeltaPartialTick(true);
        Matrix4f m = BzaRenderer.cameraMatrix(cam); m.translate((float)x,(float)y,(float)z);
        m.rotate(Axis.YP.rotationDegrees(a*2.25f-45f));
        var d = VB.appendDraw(RenderPipelines.BEACON_BEAM_TRANSLUCENT.getVertexFormatBinding(0),PrimitiveTopology.QUADS);
        beamDraws.add(d);
        float r=((argb>>16)&0xFF)/255f, g=((argb>>8)&0xFF)/255f, b=(argb&0xFF)/255f, alpha=((argb>>24)&0xFF)/255f;
        beam(VB.getVertexBuilder(d),m,a,h,w,r,g,b,alpha);
    }

    private static void beam(VertexConsumer b,Matrix4f m,float a,double h,float w,float r,float g,float bl,float al){
        float s=Mth.frac(-a*0.2f-Mth.floor(-a*0.1f)),v0=s,v1=(float)h+v0;
        float hw=w,H=(float)h;
        q(b,m,-hw,-hw,hw,-hw,0,H,r,g,bl,al,1,v0,1,v1); q(b,m,hw,-hw,hw,hw,0,H,r,g,bl,al,1,v0,1,v1);
        q(b,m,hw,hw,-hw,hw,0,H,r,g,bl,al,1,v0,1,v1); q(b,m,-hw,hw,-hw,-hw,0,H,r,g,bl,al,1,v0,1,v1);
    }

    private static void q(VertexConsumer b,Matrix4f m,float x1,float z1,float x2,float z2,float yB,float yT,float r,float g,float bl,float al,float u0,float v0,float u1,float v1){
        b.addVertex(m,x2,yT,z2).setColor(r,g,bl,al).setUv(u1,v1).setLight(LIGHT);
        b.addVertex(m,x2,yB,z2).setColor(r,g,bl,al).setUv(u1,v0).setLight(LIGHT);
        b.addVertex(m,x1,yB,z1).setColor(r,g,bl,al).setUv(u0,v0).setLight(LIGHT);
        b.addVertex(m,x1,yT,z1).setColor(r,g,bl,al).setUv(u0,v1).setLight(LIGHT);
    }
}
