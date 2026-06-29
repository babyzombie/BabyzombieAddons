package top.babyzombie.addons.mixin.screen;

import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.gui.screens.social.SocialInteractionsScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import top.babyzombie.addons.config.ModConfigManager;

import java.util.function.Supplier;

@Mixin(PauseScreen.class)
public class PauseScreenMixin {

    @Unique
    private static final String OPEN_BTN_TARGET =
            "Lnet/minecraft/client/gui/screens/PauseScreen;openScreenButton"
                    + "(Lnet/minecraft/network/chat/Component;"
                    + "Ljava/util/function/Supplier;)"
                    + "Lnet/minecraft/client/gui/components/Button;";

    @ModifyArg(
            method = "createPauseMenu",
            at = @At(value = "INVOKE", target = OPEN_BTN_TARGET),
            index = 0,
            require = 0
    )
    private Component replaceButtonText(Component original) {
        if (!ModConfigManager.get().general.replaceReportWithServerList) {
            return original;
        }
        if (original.getString().equals(
                Component.translatable("menu.playerReporting").getString())) {
            return Component.translatable("menu.multiplayer");
        }
        return original;
    }

    @ModifyArg(
            method = "createPauseMenu",
            at = @At(value = "INVOKE", target = OPEN_BTN_TARGET),
            index = 1,
            require = 0
    )
    private Supplier<Screen> replaceButtonAction(Supplier<Screen> original) {
        if (!ModConfigManager.get().general.replaceReportWithServerList) {
            return original;
        }
        Screen test = original.get();
        if (test instanceof SocialInteractionsScreen) {
            return () -> new JoinMultiplayerScreen((Screen) (Object) this);
        }
        return original;
    }
}
