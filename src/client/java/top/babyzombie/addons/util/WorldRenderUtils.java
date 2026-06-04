package top.babyzombie.addons.util;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;

public final class WorldRenderUtils {

    private static final RenderType BOX, BOX_XRAY;
    static {
        var s = RenderPipeline.builder(RenderPipelines.DEBUG_FILLED_SNIPPET)
            .withVertexFormat(DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.QUADS)
            .withLocation("bza_box").withCull(false)
            .withBlend(BlendFunction.TRANSLUCENT)
            .withDepthTestFunction(DepthTestFunction.LEQUAL_DEPTH_TEST).buildSnippet();
        BOX = RenderType.create("bza_box", RenderSetup.builder(RenderPipeline.builder(s).withLocation("bza_box").build()).bufferSize(1536).createRenderSetup());
        var sx = RenderPipeline.builder(RenderPipelines.DEBUG_FILLED_SNIPPET)
            .withVertexFormat(DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.QUADS)
            .withLocation("bza_box_xray").withCull(false)
            .withBlend(BlendFunction.TRANSLUCENT)
            .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST).buildSnippet();
        BOX_XRAY = RenderType.create("bza_box_xray", RenderSetup.builder(RenderPipeline.builder(sx).withLocation("bza_box_xray").build()).bufferSize(1536).createRenderSetup());
    }

    private static final float THICK_SCALE = 0.005f;

    private WorldRenderUtils() {}

    public static void drawBox(double x1,double y1,double z1,double x2,double y2,double z2,float r,float g,float b,float a) { drawBox(x1,y1,z1,x2,y2,z2,r,g,b,a,false,3); }
    public static void drawBox(double x1,double y1,double z1,double x2,double y2,double z2,float r,float g,float b,float a,float lw) { drawBox(x1,y1,z1,x2,y2,z2,r,g,b,a,false,lw); }
    public static void drawBoxXray(double x1,double y1,double z1,double x2,double y2,double z2,float r,float g,float b,float a) { drawBox(x1,y1,z1,x2,y2,z2,r,g,b,a,true,3); }
    public static void drawBoxXray(double x1,double y1,double z1,double x2,double y2,double z2,float r,float g,float b,float a,float lw) { drawBox(x1,y1,z1,x2,y2,z2,r,g,b,a,true,lw); }
    public static void drawBoxAtEntity(double ex,double ey,double ez,double w,double h,double d,float r,float g,float b,float a) { drawBox(ex-w/2,ey,ez-d/2,ex+w/2,ey+h,ez+d/2,r,g,b,a,false,3); }
    public static void drawBoxAtEntityXray(double ex,double ey,double ez,double w,double h,double d,float r,float g,float b,float a) { drawBox(ex-w/2,ey,ez-d/2,ex+w/2,ey+h,ez+d/2,r,g,b,a,true,3); }
    public static void drawLine(double x1,double y1,double z1,double x2,double y2,double z2,float r,float g,float b,float a) { drawLine(x1,y1,z1,x2,y2,z2,r,g,b,a,false,3); }
    public static void drawLineXray(double x1,double y1,double z1,double x2,double y2,double z2,float r,float g,float b,float a) { drawLine(x1,y1,z1,x2,y2,z2,r,g,b,a,true,3); }

    private static void drawBox(double x1,double y1,double z1,double x2,double y2,double z2,float r,float g,float b,float a,boolean xr,float lw) {
        var eye = Minecraft.getInstance().player.getEyePosition();
        float cx=(float)eye.x, cy=(float)eye.y, cz=(float)eye.z;
        float fx1=(float)x1-cx, fy1=(float)y1-cy, fz1=(float)z1-cz, fx2=(float)x2-cx, fy2=(float)y2-cy, fz2=(float)z2-cz;
        int cr=(int)(r*255),cg=(int)(g*255),cb=(int)(b*255),ca=(int)(a*255);
        float t=lw*THICK_SCALE;
        var buf = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        e(buf,fx1,fy1,fz1,fx2,fy1,fz1,t,cr,cg,cb,ca); e(buf,fx2,fy1,fz1,fx2,fy1,fz2,t,cr,cg,cb,ca);
        e(buf,fx2,fy1,fz2,fx1,fy1,fz2,t,cr,cg,cb,ca); e(buf,fx1,fy1,fz2,fx1,fy1,fz1,t,cr,cg,cb,ca);
        e(buf,fx1,fy2,fz1,fx2,fy2,fz1,t,cr,cg,cb,ca); e(buf,fx2,fy2,fz1,fx2,fy2,fz2,t,cr,cg,cb,ca);
        e(buf,fx2,fy2,fz2,fx1,fy2,fz2,t,cr,cg,cb,ca); e(buf,fx1,fy2,fz2,fx1,fy2,fz1,t,cr,cg,cb,ca);
        e(buf,fx1,fy1,fz1,fx1,fy2,fz1,t,cr,cg,cb,ca); e(buf,fx2,fy1,fz1,fx2,fy2,fz1,t,cr,cg,cb,ca);
        e(buf,fx2,fy1,fz2,fx2,fy2,fz2,t,cr,cg,cb,ca); e(buf,fx1,fy1,fz2,fx1,fy2,fz2,t,cr,cg,cb,ca);
        var m=buf.build(); if(m!=null) (xr?BOX_XRAY:BOX).draw(m);
    }

    private static void drawLine(double x1,double y1,double z1,double x2,double y2,double z2,float r,float g,float b,float a,boolean xr,float lw) {
        var eye = Minecraft.getInstance().player.getEyePosition();
        float cx=(float)eye.x, cy=(float)eye.y, cz=(float)eye.z;
        int cr=(int)(r*255),cg=(int)(g*255),cb=(int)(b*255),ca=(int)(a*255); float t=lw*THICK_SCALE;
        var buf = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        e(buf,(float)x1-cx,(float)y1-cy,(float)z1-cz,(float)x2-cx,(float)y2-cy,(float)z2-cz,t,cr,cg,cb,ca);
        var m=buf.build(); if(m!=null) (xr?BOX_XRAY:BOX).draw(m);
    }

    private static void e(VertexConsumer b,float x1,float y1,float z1,float x2,float y2,float z2,float t,int r,int g,int bl,int a){
        float dx=x2-x1,dy=y2-y1,dz=z2-z1;
        float len=(float)java.lang.Math.sqrt(dx*dx+dy*dy+dz*dz); if(len<0.0001f)return;
        float px,py,pz;
        if(java.lang.Math.abs(dy)<0.99f*len){px=-dz;py=0;pz=dx;}else{px=1;py=0;pz=0;}
        float pl=(float)java.lang.Math.sqrt(px*px+py*py+pz*pz);px=px/pl*t;py=py/pl*t;pz=pz/pl*t;
        b.addVertex(x1+px,y1+py,z1+pz).setColor(r,g,bl,a);b.addVertex(x1-px,y1-py,z1-pz).setColor(r,g,bl,a);
        b.addVertex(x2-px,y2-py,z2-pz).setColor(r,g,bl,a);b.addVertex(x2+px,y2+py,z2+pz).setColor(r,g,bl,a);
    }
}
