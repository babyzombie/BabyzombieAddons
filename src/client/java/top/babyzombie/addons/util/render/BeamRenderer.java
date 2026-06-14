package top.babyzombie.addons.util.render;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuSampler;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Axis;
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

    private static final String MOD_ID = "babyzombieaddons";
    private static final Identifier BEAM_TEXTURE_ID =
            Identifier.fromNamespaceAndPath("minecraft", "textures/entity/beacon/beacon_beam.png");
    private static final int FULLBRIGHT = 0xF000F0;

    private static final RenderPipeline BEAM_PIPELINE = RenderPipelines.BEACON_BEAM_TRANSLUCENT;

    // ── Buffer management ─────────────────────────────────────────
    private static final ByteBufferBuilder ALLOCATOR = new ByteBufferBuilder(1536);
    private static MappableRingBuffer beamVertexBuffer;
    private static BufferBuilder beamBuf;

    private static final Vector4f COLOR_MODULATOR = new Vector4f(1f, 1f, 1f, 1f);
    private static final Vector3f MODEL_OFFSET = new Vector3f();
    private static final Matrix4f TEXTURE_MATRIX = new Matrix4f();

    private BeamRenderer() {}

    /** Draw a beacon-style beam from (x,y,z) upward for {@code height} blocks. */
    public static void drawBeam(WorldRenderContext context,
                                 double x, double y, double z,
                                 double height, float halfWidth,
                                 int argb) {
        var pipeline = BEAM_PIPELINE;
        if (beamBuf == null) {
            beamBuf = new BufferBuilder(ALLOCATOR, pipeline.getVertexFormatMode(), pipeline.getVertexFormat());
        }

        // Animation: scroll UV based on game time
        var client = Minecraft.getInstance();
        long gameTime = Objects.requireNonNull(client.level).getGameTime();
        float partialTick = client.getDeltaTracker().getGameTimeDeltaPartialTick(true);
        float animTime = Math.floorMod(gameTime, 40) + partialTick;

        // Pack color for vertex: ARGB → BGRA (vanilla vertex format)
        int r = (argb >> 16) & 0xFF;
        int g = (argb >> 8) & 0xFF;
        int b = argb & 0xFF;
        int packedColor = (argb & 0xFF000000) | (r << 16) | (g << 8) | b;

        var matrices = context.matrices();
        Vec3 camera = context.worldState().cameraRenderState.pos;
        matrices.pushPose();
        matrices.translate(-camera.x + x, -camera.y + y, -camera.z + z);

        // Rotation like vanilla
        matrices.pushPose();
        matrices.mulPose(Axis.YP.rotationDegrees(animTime * 2.25f - 45.0f));
        renderBeamSection(animTime, height, halfWidth, packedColor, matrices.last());
        matrices.popPose();

        matrices.popPose();

        uploadAndDraw(pipeline, beamBuf);
        beamBuf = null;
    }

    private static void renderBeamSection(float animTime, double height, float hw,
                                           int packedColor, PoseStack.Pose pose) {
        float scroll = Mth.frac(-animTime * 0.2f - Mth.floor(-animTime * 0.1f));
        float v0 = -1.0f + scroll;
        float v1 = (float)height * (0.5f / hw) + v0;
        float hw1 = -hw, hw2 = hw;

        float h = (float)height;

        // Four side quads around local origin
        // Side -Z  (z=-hw, x: -hw → +hw)
        addQuad(pose, hw1, hw1, hw2, hw1, 0f, h, packedColor, v0, v1);
        // Side +X  (x=+hw, z: -hw → +hw)
        addQuad(pose, hw2, hw1, hw2, hw2, 0f, h, packedColor, v0, v1);
        // Side +Z  (z=+hw, x: +hw → -hw)
        addQuad(pose, hw2, hw2, hw1, hw2, 0f, h, packedColor, v0, v1);
        // Side -X  (x=-hw, z: +hw → -hw)
        addQuad(pose, hw1, hw2, hw1, hw1, 0f, h, packedColor, v0, v1);
    }

    /** Winding reversed relative to vanilla so normals point outward from beam center */
    private static void addQuad(PoseStack.Pose pose,
                                 float x1, float z1,  // corner A
                                 float x2, float z2,  // corner B
                                 float yBottom, float yTop,
                                 int packedColor, float vBottom, float vTop) {
        beamBuf.addVertex(pose, x2, yTop,    z2).setColor(packedColor).setUv(1f, vTop)   .setLight(FULLBRIGHT).setNormal(pose, 0f, 1f, 0f);
        beamBuf.addVertex(pose, x2, yBottom, z2).setColor(packedColor).setUv(1f, vBottom).setLight(FULLBRIGHT).setNormal(pose, 0f, 1f, 0f);
        beamBuf.addVertex(pose, x1, yBottom, z1).setColor(packedColor).setUv(0f, vBottom).setLight(FULLBRIGHT).setNormal(pose, 0f, 1f, 0f);
        beamBuf.addVertex(pose, x1, yTop,    z1).setColor(packedColor).setUv(0f, vTop)   .setLight(FULLBRIGHT).setNormal(pose, 0f, 1f, 0f);
    }

    // ═══════════════════════════════════════════════════════════════════
    // Upload + Draw
    // ═══════════════════════════════════════════════════════════════════

    private static void uploadAndDraw(RenderPipeline pipeline, BufferBuilder buf) {
        MeshData builtBuffer = buf.buildOrThrow();
        MeshData.DrawState drawParams = builtBuffer.drawState();
        VertexFormat format = drawParams.format();

        int vertexBufferSize = drawParams.vertexCount() * format.getVertexSize();
        if (beamVertexBuffer == null || beamVertexBuffer.size() < vertexBufferSize) {
            if (beamVertexBuffer != null) beamVertexBuffer.close();
            beamVertexBuffer = new MappableRingBuffer(
                () -> MOD_ID + " beam render",
                GpuBuffer.USAGE_VERTEX | GpuBuffer.USAGE_MAP_WRITE, vertexBufferSize);
        }

        var commandEncoder = RenderSystem.getDevice().createCommandEncoder();
        try (var mappedView = commandEncoder.mapBuffer(
                beamVertexBuffer.currentBuffer().slice(0, builtBuffer.vertexBuffer().remaining()), false, true)) {
            MemoryUtil.memCopy(builtBuffer.vertexBuffer(), mappedView.data());
        }

        GpuBuffer vertices = beamVertexBuffer.currentBuffer();

        RenderSystem.AutoStorageIndexBuffer shapeIndexBuffer =
            RenderSystem.getSequentialBuffer(pipeline.getVertexFormatMode());
        GpuBuffer indices = shapeIndexBuffer.getBuffer(drawParams.indexCount());
        var indexType = shapeIndexBuffer.type();

        GpuBufferSlice dynamicTransforms = RenderSystem.getDynamicUniforms()
            .writeTransform(RenderSystem.getModelViewMatrix(), COLOR_MODULATOR, MODEL_OFFSET, TEXTURE_MATRIX);

        var texManager = Minecraft.getInstance().getTextureManager();
        AbstractTexture beamTex = texManager.getTexture(BEAM_TEXTURE_ID);
        GpuTextureView texView = beamTex.getTextureView();
        GpuSampler texSampler = RenderSystem.getSamplerCache().getRepeat(FilterMode.NEAREST);

        var client = Minecraft.getInstance();
        if (client.getMainRenderTarget().getColorTextureView() != null) {
            try (RenderPass renderPass = RenderSystem.getDevice()
                    .createCommandEncoder()
                    .createRenderPass(() -> MOD_ID + " beam pass",
                        client.getMainRenderTarget().getColorTextureView(), OptionalInt.empty(),
                        client.getMainRenderTarget().getDepthTextureView(), OptionalDouble.empty())) {
                renderPass.setPipeline(pipeline);
                RenderSystem.bindDefaultUniforms(renderPass);
                renderPass.setUniform("DynamicTransforms", dynamicTransforms);
                renderPass.bindTexture("Sampler0", texView, texSampler);
                renderPass.setVertexBuffer(0, vertices);
                renderPass.setIndexBuffer(indices, indexType);
                renderPass.drawIndexed(0, 0, drawParams.indexCount(), 1);
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
