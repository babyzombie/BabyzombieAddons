package top.babyzombie.addons.mixin.screen;

import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.FriendsButton;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import top.babyzombie.addons.config.GeneralConfig;
import top.babyzombie.addons.config.ModConfigManager;

import java.util.ArrayList;
import java.util.List;

/**
 * 主菜单图标按钮控制（好友/语言/无障碍）。
 * 原版 26.2 主菜单右上角有一排图标按钮，与暂停菜单同款；
 * 这里按配置把选中的按钮从渲染列表移除（removeWidget）。
 */
@Mixin(TitleScreen.class)
public abstract class TitleScreenMixin extends Screen {

    protected TitleScreenMixin(Component title) {
        super(title);
    }

    @Inject(method = "init", at = @At("RETURN"))
    private void hideIconButtons(CallbackInfo ci) {
        var cfg = ModConfigManager.get().general.titleScreen;
        if (!cfg.hideFriends && !cfg.hideLanguage && !cfg.hideAccessibility) return;

        List<net.minecraft.client.gui.components.events.GuiEventListener> toRemove = new ArrayList<>();
        for (var child : this.children()) {
            if (shouldHide(cfg, child)) toRemove.add(child);
        }
        for (var child : toRemove) {
            this.removeWidget(child);
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
}
