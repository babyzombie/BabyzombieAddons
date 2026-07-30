package top.babyzombie.addons.mixin.render;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Hud;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import top.babyzombie.addons.config.ModConfigManager;
import top.babyzombie.addons.module.kuudra.KuudraSupplyProgressHUD;
import top.babyzombie.addons.util.ChatUtils;

import java.util.regex.Pattern;

/**
 * Intercepts title rendering for supply/fuel progress HUD and Kuudra damage title hiding.
 */
@Mixin(Hud.class)
public class GuiTitleMixin {

    @Unique
    private static final Pattern DAMAGE_TITLE = Pattern.compile("^[^\\d]*[\\d.,]+[KMBT]?/[\\d.,]+[KMBT]?.?$");

    @Inject(method = "extractTitle", at = @At("HEAD"), cancellable = true)
    private void onExtractTitle(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        HudAccessor acc = (HudAccessor) this;
        Component title = acc.getTitle();
        if (title == null) return;
        String text = title.getString();
        if (text.isEmpty()) return;

        // Supply/fuel progress HUD
        if (KuudraSupplyProgressHUD.onTitle(text)) {
            ci.cancel();
            return;
        }

        // Hide Kuudra damage title (e.g. "☠ 240M/240M❤")
        if (ModConfigManager.get().kuudra.phase4.hideKuudraDamageTitle) {
            if (DAMAGE_TITLE.matcher(ChatUtils.stripColor(text)).matches()) {
                ci.cancel();
            }
        }
    }
}
