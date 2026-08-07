package top.babyzombie.addons.mixin.screen;

import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.SpriteIconButton;
import net.minecraft.client.gui.layouts.GridLayout;
import net.minecraft.client.gui.layouts.LayoutElement;
import net.minecraft.client.gui.layouts.LayoutSettings;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.gui.screens.options.SoundOptionsScreen;
import net.minecraft.client.gui.screens.options.controls.KeyBindsScreen;
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import top.babyzombie.addons.config.ModConfigManager;
import top.babyzombie.addons.util.SodiumCompat;

import java.util.function.Supplier;

@Mixin(PauseScreen.class)
public abstract class PauseScreenMixin extends Screen {

    protected PauseScreenMixin(Component title) {
        super(title);
    }

    // ═══════════════════════════════════════════════
    // Part 1: 自定义快捷按钮
    // 注入点在原版图标行加入网格之后 (arrangeElements 之前)
    // 图标少 → 合并进原版行；图标多/原版行关了 → 另起一行
    // ═══════════════════════════════════════════════

    @Unique
    private static final int MERGE_THRESHOLD = 7;

    @Inject(
            method = "createPauseMenu",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/layouts/GridLayout$RowHelper;addChild"
                            + "(Lnet/minecraft/client/gui/layouts/LayoutElement;I"
                            + "Lnet/minecraft/client/gui/layouts/LayoutSettings;)"
                            + "Lnet/minecraft/client/gui/layouts/LayoutElement;",
                    ordinal = 1,
                    shift = At.Shift.AFTER),
            require = 0
    )
    private void addQuickButtons(CallbackInfo ci,
                                 @Local(name = "iconButtonRow") LinearLayout iconButtonRow,
                                 @Local(name = "helper") GridLayout.RowHelper helper) {
        var general = ModConfigManager.get().general.pauseScreen;
        if (!general.enableQuickButtons || general.quickButtonOrder.isEmpty()) return;

        int customCount = general.quickButtonOrder.size();

        int vanillaVisible = 0;
        var hideCfg = general.hideButtons;
        if (!hideCfg.hideReportBugs) vanillaVisible++;
        if (!hideCfg.hideFeedback) vanillaVisible++;
        if (!hideCfg.hideFriends) vanillaVisible++;
        if (!hideCfg.hidePlayerReporting) vanillaVisible++;

        boolean merge = !general.hideIconButtonRow
                && (vanillaVisible + customCount) <= MERGE_THRESHOLD;

        LinearLayout targetRow = merge ? iconButtonRow : LinearLayout.horizontal().spacing(4);

        for (var type : general.quickButtonOrder) {
            switch (type) {
                case SINGLEPLAYER -> targetRow.addChild(icon("selectWorld.title",
                        "pause_menu/singleplayer", () -> new SelectWorldScreen(this)));
                case SERVER_LIST -> targetRow.addChild(icon("menu.multiplayer",
                        "pause_menu/server_list", () -> new JoinMultiplayerScreen(this)));
                case VIDEO_SETTINGS -> targetRow.addChild(icon("options.videoTitle",
                        "pause_menu/video_settings",
                        // 装 Sodium(及 RSO)时走 Sodium 入口，否则原版界面
                        () -> SodiumCompat.createVideoSettingsScreen(this)));
                case KEY_BINDS -> targetRow.addChild(icon("controls.keybinds.title",
                        "pause_menu/key_binds",
                        () -> new KeyBindsScreen(this, Minecraft.getInstance().options)));
                case SOUND_OPTIONS -> targetRow.addChild(icon("options.sounds.title",
                        "pause_menu/sound_options",
                        () -> new SoundOptionsScreen(this, Minecraft.getInstance().options)));
            }
        }

        if (!merge) {
            helper.addChild(targetRow, 2,
                    helper.newCellSettings().alignHorizontallyCenter());
        }
    }

    @Unique
    private static SpriteIconButton icon(String titleKey, String spritePath,
                                         Supplier<Screen> screen) {
        return SpriteIconButton.builder(
                        Component.translatable(titleKey),
                        var1 -> Minecraft.getInstance().gui.setScreen(screen.get()),
                        true
                ).width(20)
                .sprite(Identifier.fromNamespaceAndPath(
                        "babyzombieaddons", spritePath), 15, 15)
                .withTootip()
                .build();
    }

    // ═══════════════════════════════════════════════
    // Part 2: 文字按钮 — RowHelper.addChild 1-arg
    // 涵盖: 进度、统计、选项(单人)、多人选项
    // 不数 ordinal，靠按钮文本识别
    // ═══════════════════════════════════════════════

    @Redirect(method = "createPauseMenu",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/layouts/GridLayout$RowHelper;addChild"
                            + "(Lnet/minecraft/client/gui/layouts/LayoutElement;)"
                            + "Lnet/minecraft/client/gui/layouts/LayoutElement;"),
            require = 0)
    private <T extends LayoutElement> T filterTextButtons(GridLayout.RowHelper helper, T child) {
        if (child instanceof AbstractWidget widget) {
            String text = widget.getMessage().getString();
            var hideCfg = ModConfigManager.get().general.pauseScreen.hideButtons;

            if (hideCfg.hideAdvancements
                    && text.equals(Component.translatable("gui.advancements").getString())) return null;
            if (hideCfg.hideStats
                    && text.equals(Component.translatable("gui.stats").getString())) return null;
            if (hideCfg.hideOptions
                    && text.equals(Component.translatable("menu.options").getString())) return null;
            if (hideCfg.hideMultiplayerOptions
                    && text.equals(Component.translatable("menu.multiplayerOptions.button").getString())) return null;
        }
        return helper.addChild(child);
    }

    // ═══════════════════════════════════════════════
    // Part 3: 图标行逐按钮 — LinearLayout.addChild
    // (四个 ordinal 固定可靠)
    // ═══════════════════════════════════════════════

    @Redirect(method = "createPauseMenu",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/layouts/LinearLayout;addChild"
                            + "(Lnet/minecraft/client/gui/layouts/LayoutElement;)"
                            + "Lnet/minecraft/client/gui/layouts/LayoutElement;",
                    ordinal = 0),
            require = 0)
    private <T extends LayoutElement> T hideReportBugs(LinearLayout layout, T child) {
        if (ModConfigManager.get().general.pauseScreen.hideButtons.hideReportBugs) return null;
        return layout.addChild(child);
    }

    @Redirect(method = "createPauseMenu",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/layouts/LinearLayout;addChild"
                            + "(Lnet/minecraft/client/gui/layouts/LayoutElement;)"
                            + "Lnet/minecraft/client/gui/layouts/LayoutElement;",
                    ordinal = 1),
            require = 0)
    private <T extends LayoutElement> T hideFeedback(LinearLayout layout, T child) {
        if (ModConfigManager.get().general.pauseScreen.hideButtons.hideFeedback) return null;
        return layout.addChild(child);
    }

    @Redirect(method = "createPauseMenu",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/layouts/LinearLayout;addChild"
                            + "(Lnet/minecraft/client/gui/layouts/LayoutElement;)"
                            + "Lnet/minecraft/client/gui/layouts/LayoutElement;",
                    ordinal = 2),
            require = 0)
    private <T extends LayoutElement> T hideFriends(LinearLayout layout, T child) {
        if (ModConfigManager.get().general.pauseScreen.hideButtons.hideFriends) return null;
        return layout.addChild(child);
    }

    @Redirect(method = "createPauseMenu",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/layouts/LinearLayout;addChild"
                            + "(Lnet/minecraft/client/gui/layouts/LayoutElement;)"
                            + "Lnet/minecraft/client/gui/layouts/LayoutElement;",
                    ordinal = 3),
            require = 0)
    private <T extends LayoutElement> T hidePlayerReporting(LinearLayout layout, T child) {
        if (ModConfigManager.get().general.pauseScreen.hideButtons.hidePlayerReporting) return null;
        return layout.addChild(child);
    }

    // ═══════════════════════════════════════════════
    // Part 4: 回到游戏 / 整行图标 (3-arg, ordinal 固定)
    // ═══════════════════════════════════════════════

    @Redirect(method = "createPauseMenu",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/layouts/GridLayout$RowHelper;addChild"
                            + "(Lnet/minecraft/client/gui/layouts/LayoutElement;I"
                            + "Lnet/minecraft/client/gui/layouts/LayoutSettings;)"
                            + "Lnet/minecraft/client/gui/layouts/LayoutElement;",
                    ordinal = 0),
            require = 0)
    private <T extends LayoutElement> T hideReturnToGame(
            GridLayout.RowHelper helper, T widget, int columnWidth, LayoutSettings layoutSettings) {
        if (ModConfigManager.get().general.pauseScreen.hideReturnToGame) return null;
        return helper.addChild(widget, columnWidth, layoutSettings);
    }

    @Redirect(method = "createPauseMenu",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/layouts/GridLayout$RowHelper;addChild"
                            + "(Lnet/minecraft/client/gui/layouts/LayoutElement;I"
                            + "Lnet/minecraft/client/gui/layouts/LayoutSettings;)"
                            + "Lnet/minecraft/client/gui/layouts/LayoutElement;",
                    ordinal = 1),
            require = 0)
    private <T extends LayoutElement> T hideIconButtonRow(
            GridLayout.RowHelper helper, T widget, int columnWidth, LayoutSettings layoutSettings) {
        if (ModConfigManager.get().general.pauseScreen.hideIconButtonRow) return null;
        return helper.addChild(widget, columnWidth, layoutSettings);
    }

    // ═══════════════════════════════════════════════
    // Part 5: 选项(多人) / 断开连接 — RowHelper 2-arg
    // 不数 ordinal，靠按钮文本识别
    // ═══════════════════════════════════════════════

    @Redirect(method = "createPauseMenu",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/layouts/GridLayout$RowHelper;addChild"
                            + "(Lnet/minecraft/client/gui/layouts/LayoutElement;I)"
                            + "Lnet/minecraft/client/gui/layouts/LayoutElement;"),
            require = 0)
    private <T extends LayoutElement> T filterFullWidthButtons(
            GridLayout.RowHelper helper, T child, int columnWidth) {
        if (child instanceof AbstractWidget widget) {
            String text = widget.getMessage().getString();
            var hideCfg = ModConfigManager.get().general.pauseScreen.hideButtons;

            if (hideCfg.hideOptions
                    && text.equals(Component.translatable("menu.options").getString())) return null;
            if (hideCfg.hideDisconnect
                    && (text.equals(Component.translatable("menu.disconnect").getString())
                    || text.equals(Component.translatable("menu.returnToMenu").getString()))) return null;
        }
        return helper.addChild(child, columnWidth);
    }

    // ═══════════════════════════════════════════════
    // Part 6: 服务器自定义按钮 (Dialog 数据包)
    // ═══════════════════════════════════════════════

    @Inject(
            method = "addCustomDialogButtons",
            at = @At("HEAD"),
            cancellable = true,
            require = 0
    )
    private void hideCustomAdditions(CallbackInfo ci) {
        if (ModConfigManager.get().general.pauseScreen.hideButtons.hideCustomAdditions) {
            ci.cancel();
        }
    }
}
