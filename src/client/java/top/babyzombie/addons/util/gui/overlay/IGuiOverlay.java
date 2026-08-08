package top.babyzombie.addons.util.gui.overlay;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;

public interface IGuiOverlay {
    boolean shouldRender(Screen currentScreen);
    default void onInventoryUpdated() {}
    void render(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta);
    boolean mouseClicked(double mouseX, double mouseY, int button);
    default boolean mouseReleased(double mouseX, double mouseY, int button) { return false; }
    default boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) { return false; }
}
