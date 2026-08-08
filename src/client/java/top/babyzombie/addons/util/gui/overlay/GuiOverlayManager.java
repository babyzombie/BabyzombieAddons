package top.babyzombie.addons.util.gui.overlay;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public final class GuiOverlayManager {
    private static final List<IGuiOverlay> overlays = new CopyOnWriteArrayList<>();
    private GuiOverlayManager() {}

    public static void init() {}

    public static void register(IGuiOverlay overlay) {
        overlays.add(overlay);
    }

    public static void onRender(Screen screen, GuiGraphicsExtractor g, int mx, int my, float delta) {
        if (screen == null) return;
        for (IGuiOverlay o : overlays) {
            try {
                if (o.shouldRender(screen)) o.render(g, mx, my, delta);
            } catch (Exception ignored) {}
        }
    }

    public static boolean onMouseClicked(Screen screen, double mx, double my, int button) {
        if (screen == null) return false;
        for (IGuiOverlay o : overlays) {
            try {
                if (o.shouldRender(screen) && o.mouseClicked(mx, my, button)) return true;
            } catch (Exception ignored) {}
        }
        return false;
    }

    public static boolean onMouseReleased(Screen screen, double mx, double my, int button) {
        if (screen == null) return false;
        for (IGuiOverlay o : overlays) {
            try {
                if (o.shouldRender(screen) && o.mouseReleased(mx, my, button)) return true;
            } catch (Exception ignored) {}
        }
        return false;
    }

    public static boolean onMouseDragged(Screen screen, double mx, double my, int button, double dx, double dy) {
        if (screen == null) return false;
        for (IGuiOverlay o : overlays) {
            try {
                if (o.shouldRender(screen) && o.mouseDragged(mx, my, button, dx, dy)) return true;
            } catch (Exception ignored) {}
        }
        return false;
    }

    public static void notifyInventoryUpdate() {
        for (IGuiOverlay o : overlays) {
            try { o.onInventoryUpdated(); } catch (Exception ignored) {}
        }
    }
}
