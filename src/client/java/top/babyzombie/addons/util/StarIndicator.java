package top.babyzombie.addons.util;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

public final class StarIndicator {

    private static final Identifier PROTECT = Identifier.fromNamespaceAndPath("babyzombieaddons", "item_protection");
    private static final Identifier SHARE   = Identifier.fromNamespaceAndPath("babyzombieaddons", "share_arrow");

    private StarIndicator() {}

    /** 左侧画指示贴图：聊天打开→分享箭头，聊天关闭→保护星星 */
    public static void draw(GuiGraphicsExtractor g, int mouseX, int mouseY, boolean chatActive) {
        Identifier sprite = chatActive ? SHARE : PROTECT;
        g.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, mouseX - 6, mouseY + 6, 8, 8);
    }
}
