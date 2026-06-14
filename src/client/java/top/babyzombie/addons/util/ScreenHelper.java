package top.babyzombie.addons.util;

import net.minecraft.client.gui.screens.Screen;

public final class ScreenHelper {
    private ScreenHelper() {}
    private static Screen current;

    public static Screen getCurrent() { return current; }
    public static void setCurrent(Screen screen) { current = screen; }
}
