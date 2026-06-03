package top.babyzombie.addons.util;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;

import java.util.Collection;

public final class WorldTextRenderer {

    private static final RenderType TEXT;
    static {
        var snippet = RenderPipeline.builder(RenderPipelines.DEBUG_FILLED_SNIPPET)
            .withVertexFormat(DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP, VertexFormat.Mode.QUADS)
            .withLocation("bza_text")
            .withCull(false)
            .withBlend(BlendFunction.TRANSLUCENT)
            .withDepthTestFunction(DepthTestFunction.LEQUAL_DEPTH_TEST)
            .buildSnippet();
        var pipeline = RenderPipeline.builder(snippet).withLocation("bza_text").build();
        TEXT = RenderType.create("bza_text",
            RenderSetup.builder(pipeline).bufferSize(1536).createRenderSetup());
    }

    private WorldTextRenderer() {}

    public static void render(PoseStack ps, Collection<? extends TextEntry> entries) {
        if (entries.isEmpty()) return;
        var player = Minecraft.getInstance().player;
        if (player == null) return;
        var font = Minecraft.getInstance().font;
        Vec3 cam = player.getEyePosition();
        var camRot = Minecraft.getInstance().gameRenderer.getMainCamera().rotation();

        var tess = Tesselator.getInstance();

        for (TextEntry e : entries) {
            ps.pushPose();
            ps.translate(e.x - cam.x(), e.y - cam.y(), e.z - cam.z());
            ps.mulPose(camRot);
            ps.scale(-0.025f, -0.025f, 0.025f);

            var m = ps.last().pose();
            var builder = tess.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP);
            MultiBufferSource mbs = rt -> builder;
            font.drawInBatch(Component.literal(e.text), 0, 0, e.color, false, m, mbs,
                    Font.DisplayMode.SEE_THROUGH, 0, 0xF000F0);
            var mesh = builder.build();
            if (mesh != null) TEXT.draw(mesh);
            ps.popPose();
        }
    }

    public record TextEntry(String text, double x, double y, double z, int color) {}
}
