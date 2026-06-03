package top.babyzombie.addons.util;

import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;

import java.awt.Color;

public final class BeaconBeamRenderer {

    public static final float DEFAULT_HEIGHT = 300f;

    private BeaconBeamRenderer() {}

    public static void render(double x, double y, double z, Color color, float h) {
        var client = Minecraft.getInstance();
        if (client.level == null || client.player == null) return;
        var cam = client.gameRenderer.getMainCamera();
        float cx = (float) cam.position().x, cy = (float) cam.position().y, cz = (float) cam.position().z;
        float r = color.getRed() / 255f, g = color.getGreen() / 255f, b = color.getBlue() / 255f, a = color.getAlpha() / 255f;
        float bx = (float) x - cx, by = (float) y - cy, bz = (float) z - cz;

        double time = System.nanoTime() / 5e7; // ~20 ticks per second, no precision loss
        // Outer beam: static, semi-transparent
        double s = 0.22;
        double o1=0.5-s, o2=0.5+s;

        var tess = Tesselator.getInstance();
        float oa = a * 0.15f, ob = 0.04f;
        var buf = tess.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        v(buf,bx+o1,by+h,bz+o1,r,g,b,oa); v(buf,bx+o1,by,bz+o1,r,g,b,ob);
        v(buf,bx+o2,by,bz+o1,r,g,b,ob); v(buf,bx+o2,by+h,bz+o1,r,g,b,oa);
        v(buf,bx+o2,by+h,bz+o2,r,g,b,oa); v(buf,bx+o2,by,bz+o2,r,g,b,ob);
        v(buf,bx+o1,by,bz+o2,r,g,b,ob); v(buf,bx+o1,by+h,bz+o2,r,g,b,oa);
        v(buf,bx+o2,by+h,bz+o1,r,g,b,oa); v(buf,bx+o2,by,bz+o1,r,g,b,ob);
        v(buf,bx+o2,by,bz+o2,r,g,b,ob); v(buf,bx+o2,by+h,bz+o2,r,g,b,oa);
        v(buf,bx+o1,by+h,bz+o2,r,g,b,oa); v(buf,bx+o1,by,bz+o2,r,g,b,ob);
        v(buf,bx+o1,by,bz+o1,r,g,b,ob); v(buf,bx+o1,by+h,bz+o1,r,g,b,oa);
        var mesh = buf.build();
        if (mesh != null) WorldRenderUtils.BOX.draw(mesh);

        // Inner ring: rotates, half speed
        float ia = a, ib = a;
        double t2 = time * 0.04;
        float ri = 0.18f;
        double c1=0.5+Mth.cos(t2)*ri, s1=0.5+Mth.sin(t2)*ri;
        double c2=0.5+Mth.cos(t2+Math.PI/2)*ri, s2=0.5+Mth.sin(t2+Math.PI/2)*ri;
        double c3=0.5+Mth.cos(t2+Math.PI)*ri, s3=0.5+Mth.sin(t2+Math.PI)*ri;
        double c4=0.5+Mth.cos(t2+Math.PI*1.5)*ri, s4=0.5+Mth.sin(t2+Math.PI*1.5)*ri;
        var buf2 = tess.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        v(buf2,bx+c1,by+h,bz+s1,r,g,b,ia); v(buf2,bx+c1,by,bz+s1,r,g,b,ib);
        v(buf2,bx+c2,by,bz+s2,r,g,b,ib); v(buf2,bx+c2,by+h,bz+s2,r,g,b,ia);
        v(buf2,bx+c3,by+h,bz+s3,r,g,b,ia); v(buf2,bx+c3,by,bz+s3,r,g,b,ib);
        v(buf2,bx+c4,by,bz+s4,r,g,b,ib); v(buf2,bx+c4,by+h,bz+s4,r,g,b,ia);
        v(buf2,bx+c2,by+h,bz+s2,r,g,b,ia); v(buf2,bx+c2,by,bz+s2,r,g,b,ib);
        v(buf2,bx+c3,by,bz+s3,r,g,b,ib); v(buf2,bx+c3,by+h,bz+s3,r,g,b,ia);
        v(buf2,bx+c4,by+h,bz+s4,r,g,b,ia); v(buf2,bx+c4,by,bz+s4,r,g,b,ib);
        v(buf2,bx+c1,by,bz+s1,r,g,b,ib); v(buf2,bx+c1,by+h,bz+s1,r,g,b,ia);
        var mesh2 = buf2.build();
        if (mesh2 != null) WorldRenderUtils.BOX.draw(mesh2);
    }

    private static void v(BufferBuilder b, double x, double y, double z, float r, float g, float bl, float a) {
        b.addVertex((float)x, (float)y, (float)z).setColor(r, g, bl, a);
    }
}
