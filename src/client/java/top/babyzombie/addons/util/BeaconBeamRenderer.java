package top.babyzombie.addons.util;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BeaconRenderer;
import net.minecraft.util.ARGB;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Renders beacon beams using vanilla's BeaconRenderer.submitBeaconBeam().
 * Hooks into LevelRenderer.submitBlockEntities via BeaconBeamMixin.
 * Default height is 327 (vanilla beacon range).
 */
public final class BeaconBeamRenderer {

    public static final float DEFAULT_HEIGHT = 327f;
    private static final List<Beam> beams = new ArrayList<>();

    private BeaconBeamRenderer() {}

    public static void renderWorldBeams(SubmitNodeCollector collector, double camX, double camY, double camZ) {
        long now = ServerTick.getTime();
        PoseStack ps = new PoseStack();
        ps.translate(-camX, -camY, -camZ);
        Iterator<Beam> it = beams.iterator();
        while (it.hasNext()) {
            Beam b = it.next();
            if (now > b.expires) { it.remove(); continue; }
            int color = ARGB.color(b.color.getAlpha(), b.color.getRed(), b.color.getGreen(), b.color.getBlue());
            ps.pushPose();
            ps.translate(b.x, b.y, b.z);
            BeaconRenderer.submitBeaconBeam(ps, collector,
                    BeaconRenderer.BEAM_LOCATION, 1.0f, 1.0f, 0, (int)b.height, color, 0.2f, 0.25f);
            ps.popPose();
        }
    }

    public static void addBeam(double x, double y, double z, Color color, float height, int dur) {
        beams.add(new Beam(x, y, z, color, height, ServerTick.getTime() + dur));
    }

    public static void setBeam(String id, double x, double y, double z, Color color, float height) {
        beams.removeIf(b -> id.equals(b.id));
        beams.add(new Beam(id, x, y, z, color, height, Long.MAX_VALUE));
    }
    public static void removeBeam(String id) {
        beams.removeIf(b -> id.equals(b.id));
    }
    public static void removeNear(double x, double y, double z) {
        beams.removeIf(b -> Math.abs(b.x - x) < 0.5 && Math.abs(b.y - y) < 0.5 && Math.abs(b.z - z) < 0.5);
    }
    public static void clearAll() { beams.clear(); }

    record Beam(String id, double x, double y, double z, Color color, float height, long expires) {
        Beam(double x, double y, double z, Color color, float height, long expires) {
            this("", x, y, z, color, height, expires);
        }
    }
}
