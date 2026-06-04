package top.babyzombie.addons.util;

import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.font.TextRenderable;
import net.minecraft.network.chat.Component;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

import java.util.ArrayList;
import java.util.List;

public final class WorldTextRenderer {

    private static final List<PendingText> pending = new ArrayList<>();

    static {
        WorldRenderEvents.BEFORE_TRANSLUCENT.register(ctx -> {
            if (pending.isEmpty()) return;
            var player = Minecraft.getInstance().player;
            if (player == null) return;
            var font = Minecraft.getInstance().font;
            var cam = ctx.worldState().cameraRenderState;

            for (var p : pending) {
                Matrix4f posMatrix = new Matrix4f()
                    .translate((float)(p.x - cam.pos.x()), (float)(p.y - cam.pos.y()), (float)(p.z - cam.pos.z()))
                    .rotate(cam.orientation)
                    .scale(-p.scale, -p.scale, p.scale);

                var text = font.prepareText(Component.literal(p.text).getVisualOrderText(), -font.width(p.text)/2f, 0, p.color, false, false, 0);
                text.visit(new Font.GlyphVisitor() {
                    public void acceptGlyph(TextRenderable.Styled g) {
                        var rt = g.renderType(Font.DisplayMode.SEE_THROUGH);
                        var buf = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP);
                        g.render(posMatrix, buf, 0xF000F0, false);
                        var mesh = buf.build();
                        if (mesh != null) rt.draw(mesh);
                    }
                });
            }
            pending.clear();
        });
    }

    private WorldTextRenderer() {}

    public static void renderString(PoseStack ps, String text, double x, double y, double z, int color, float scale) {
        pending.add(new PendingText(text, x, y, z, color, scale));
    }

    private record PendingText(String text, double x, double y, double z, int color, float scale) {}
}
