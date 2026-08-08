package top.babyzombie.addons.mixin.screen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.FriendsButton;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.gui.screens.options.SoundOptionsScreen;
import net.minecraft.client.gui.screens.options.controls.KeyBindsScreen;
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import top.babyzombie.addons.config.GeneralConfig;
import top.babyzombie.addons.config.ModConfigManager;
import top.babyzombie.addons.util.HypixelServer;
import top.babyzombie.addons.util.ModConfigOpener;
import top.babyzombie.addons.util.ScreenButtons;
import top.babyzombie.addons.util.SodiumCompat;

import java.util.ArrayList;
import java.util.List;

/**
 * 主菜单图标按钮控制（好友/语言/无障碍）+ 自定义快捷按钮行。
 * 原版 26.2 主菜单有一排图标按钮，与暂停菜单同款；
 * 这里按配置把选中的按钮从渲染列表移除（removeWidget），
 * 再按需在原版图标行位置添加自定义快捷按钮。
 */
@Mixin(TitleScreen.class)
public abstract class TitleScreenMixin extends Screen {

    // 原版图标按钮行的 y 坐标（单机/多人/Realms 三行之下），与 init() 内的 topPos 计算一致
    @Unique
    private static final int ICON_ROW_Y = 120;

    protected TitleScreenMixin(Component title) {
        super(title);
    }

    @Inject(method = "init", at = @At("RETURN"))
    private void hideIconButtons(CallbackInfo ci) {
        var cfg = ModConfigManager.get().general.titleScreen;
        if (cfg.hideFriends || cfg.hideLanguage || cfg.hideAccessibility) {
            List<net.minecraft.client.gui.components.events.GuiEventListener> toRemove = new ArrayList<>();
            for (var child : this.children()) {
                if (shouldHide(cfg, child)) toRemove.add(child);
            }
            for (var child : toRemove) {
                this.removeWidget(child);
            }
        }
        if (cfg.enableQuickButtons && !cfg.quickButtonOrder.isEmpty()) {
            addQuickButtons(cfg);
        }
    }

    @Unique
    private static boolean shouldHide(GeneralConfig.TitleScreen cfg, Object child) {
        if (child instanceof FriendsButton) return cfg.hideFriends;
        if (child instanceof AbstractWidget widget) {
            String text = widget.getMessage().getString();
            if (text.equals(Component.translatable("options.language").getString())) return cfg.hideLanguage;
            if (text.equals(Component.translatable("options.accessibility").getString())) return cfg.hideAccessibility;
        }
        return false;
    }

    /** 是否原版图标按钮（好友/语言/无障碍） */
    @Unique
    private static boolean isVanillaIcon(Object child) {
        if (child instanceof FriendsButton) return true;
        if (child instanceof AbstractWidget widget) {
            String text = widget.getMessage().getString();
            return text.equals(Component.translatable("options.language").getString())
                    || text.equals(Component.translatable("options.accessibility").getString());
        }
        return false;
    }

    @Unique
    private void addQuickButtons(GeneralConfig.TitleScreen cfg) {
        // 未安装对应 mod 的第三方设置按钮不渲染
        var quickButtons = cfg.quickButtonOrder.stream()
                .filter(GeneralConfig.QuickButtonType::isAvailable)
                .toList();
        if (quickButtons.isEmpty()) return;

        // 可见的原版图标按钮（保持原顺序），与快捷按钮合并成一行统一居中重排
        List<AbstractWidget> vanillaIcons = new ArrayList<>();
        for (var child : this.children()) {
            if (isVanillaIcon(child)) vanillaIcons.add((AbstractWidget) child);
        }

        int total = vanillaIcons.size() + quickButtons.size();
        int totalWidth = total * 20 + (total - 1) * 4;
        int startX = this.width / 2 - totalWidth / 2;
        int y = this.height / 4 + ICON_ROW_Y;
        int x = startX;

        for (var widget : vanillaIcons) {
            widget.setPosition(x, y);
            x += 24;
        }

        for (var type : quickButtons) {
            var button = switch (type) {
                case SINGLEPLAYER -> ScreenButtons.icon("selectWorld.title",
                        "pause_menu/singleplayer", () -> new SelectWorldScreen(this));
                case SERVER_LIST -> ScreenButtons.icon("menu.multiplayer",
                        "pause_menu/server_list", () -> new JoinMultiplayerScreen(this));
                case VIDEO_SETTINGS -> ScreenButtons.icon("options.videoTitle",
                        "pause_menu/video_settings",
                        // 装 Sodium(及 RSO)时走 Sodium 入口，否则原版界面
                        () -> SodiumCompat.createVideoSettingsScreen(this));
                case KEY_BINDS -> ScreenButtons.icon("controls.keybinds.title",
                        "pause_menu/key_binds",
                        () -> new KeyBindsScreen(this, Minecraft.getInstance().options));
                case SOUND_OPTIONS -> ScreenButtons.icon("options.sounds.title",
                        "pause_menu/sound_options",
                        () -> new SoundOptionsScreen(this, Minecraft.getInstance().options));
                case BZA_CONFIG -> ScreenButtons.icon(
                        "config.babyzombieaddons.quickbutton.BZA_CONFIG", "pause_menu/settings",
                        () -> ModConfigManager.createGUI(this, ""));
                case HYPIXEL -> ScreenButtons.icon(
                        "config.babyzombieaddons.quickbutton.HYPIXEL", "pause_menu/hypixel",
                        () -> HypixelServer.join(this));
                case SKYBLOCKER, FIRMAMENT, SKYHANNI, AARON -> ScreenButtons.icon(
                        "config.babyzombieaddons.quickbutton." + type.name(),
                        "pause_menu/" + type.name().toLowerCase(),
                        () -> ModConfigOpener.createScreen(type.modId(), this));
            };
            button.setPosition(x, y);
            this.addRenderableWidget(button);
            x += 24;
        }
    }
}
