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

        // 伤害/触手血量都在 subtitle（"☠ 240M/240M❤" / "৫ 838.8k/30M❤"），
        // 独立检查：主 title 为 null 时也必须能隐藏
        Component sub = acc.getSubtitle();
        if (sub != null) {
            String subText = ChatUtils.stripColor(sub.getString());

            // Hide Kuudra damage title
            if (ModConfigManager.get().kuudra.phase4.hideKuudraDamageTitle
                    && DAMAGE_TITLE.matcher(subText).matches()) {
                ci.cancel();
                return;
            }

            // Hide tentacle HP title (Phase 1 — lava tentacles)
            if (ModConfigManager.get().kuudra.phase1.hideTentacleTitle
                    && subText.startsWith("৫")) {
                ci.cancel();
                return;
            }
        }

        // 主 title：Supply/fuel progress HUD（补给进度条在主 title）
        Component title = acc.getTitle();
        if (title == null) return;
        String text = title.getString();
        if (text.isEmpty()) return;
        if (KuudraSupplyProgressHUD.onTitle(text)) {
            ci.cancel();
        }
    }
}
