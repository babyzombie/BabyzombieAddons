package top.babyzombie.addons.util.render;

public final class GlowRenderer {
    private GlowRenderer() {}
    private static boolean depthTestActive;
    public static boolean isDepthTestActive() { return depthTestActive; }
    public static void markDepthTestActive() { depthTestActive = true; }
    public static void endDepthTestedOutline() { depthTestActive = false; }
}
