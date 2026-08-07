package top.babyzombie.addons.util;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.SpriteIconButton;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.util.function.Supplier;

/**
 * 菜单屏幕图标按钮工具：暂停菜单 / 主菜单的自定义快捷按钮共用。
 * sprite 纹理位于 textures/gui/sprites/ 下（spritePath 为相对路径）。
 */
public final class ScreenButtons {

    private ScreenButtons() {}

    public static SpriteIconButton icon(String titleKey, String spritePath,
                                        Supplier<Screen> screen) {
        return icon(titleKey, spritePath, () -> Minecraft.getInstance().gui.setScreen(screen.get()));
    }

    /**
     * 回调形式按钮（打开屏幕之外的用途，如直接连接服务器）。
     */
    public static SpriteIconButton icon(String titleKey, String spritePath,
                                        Runnable onPress) {
        return SpriteIconButton.builder(
                        Component.translatable(titleKey),
                        _ -> onPress.run(),
                        true
                ).width(20)
                .sprite(Identifier.fromNamespaceAndPath(
                        "babyzombieaddons", spritePath), 15, 15)
                .withTootip()
                .build();
    }
}
