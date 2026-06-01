package top.babyzombie.addons.util;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;

import java.util.Collection;

public final class WorldTextRenderer {
    private WorldTextRenderer() {}

    public static void render(PoseStack ps, Collection<? extends TextEntry> entries) {
        if (entries.isEmpty()) return;

        var player = Minecraft.getInstance().player;
        if (player == null) return;
        var font = Minecraft.getInstance().font;
        Vec3 cam = player.getEyePosition();
        var camRot = Minecraft.getInstance().gameRenderer.getMainCamera().rotation();
        MultiBufferSource.BufferSource buf = Minecraft.getInstance().renderBuffers().bufferSource();

        for (TextEntry e : entries) {
            ps.pushPose();
            ps.translate(e.x - cam.x(), e.y - cam.y(), e.z - cam.z());
            ps.mulPose(camRot);
            ps.scale(-0.025f, -0.025f, 0.025f);

            var m = ps.last().pose();
            font.drawInBatch(Component.literal(e.text), 0, 0, e.color, false, m, buf,
                    net.minecraft.client.gui.Font.DisplayMode.SEE_THROUGH, 0, 0xF000F0);
            ps.popPose();
        }
        buf.endBatch();
    }

    public record TextEntry(String text, double x, double y, double z, int color) {}
}
