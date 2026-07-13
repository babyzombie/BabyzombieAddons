package top.babyzombie.addons.mixin.screen;

import io.github.notenoughupdates.moulconfig.common.text.StructuredText;
import io.github.notenoughupdates.moulconfig.processor.MoulConfigProcessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import top.babyzombie.addons.config.MoulConfigTextHelper;

/**
 * Redirects {@code StructuredText.of()} in MoulConfig to use
 * {@code translatable()} when the string looks like a translation key
 * (i.e., contains a dot), otherwise keeps it as literal text.
 */
@Mixin(value = MoulConfigProcessor.class, remap = false)
public class MoulConfigTranslationMixin {

    @Redirect(
            method = "createProcessedOption",
            at = @At(value = "INVOKE", target = "Lio/github/notenoughupdates/moulconfig/common/text/StructuredText;of(Ljava/lang/String;)Lio/github/notenoughupdates/moulconfig/common/text/StructuredText$Mutable;"),
            require = 0)
    private static StructuredText.Mutable translateOptionText(String text) {
        return MoulConfigTextHelper.translatableIfKey(text);
    }

    @Redirect(
            method = "beginCategory",
            at = @At(value = "INVOKE", target = "Lio/github/notenoughupdates/moulconfig/common/text/StructuredText;of(Ljava/lang/String;)Lio/github/notenoughupdates/moulconfig/common/text/StructuredText$Mutable;"),
            require = 0)
    private static StructuredText.Mutable translateCategoryText(String text) {
        return MoulConfigTextHelper.translatableIfKey(text);
    }
}
