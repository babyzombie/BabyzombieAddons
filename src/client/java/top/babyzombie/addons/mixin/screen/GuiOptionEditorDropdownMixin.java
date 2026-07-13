package top.babyzombie.addons.mixin.screen;

import io.github.notenoughupdates.moulconfig.common.text.StructuredText;
import io.github.notenoughupdates.moulconfig.gui.editors.GuiOptionEditorDropdown;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import top.babyzombie.addons.config.MoulConfigTextHelper;

/**
 * Redirects {@code StructuredText.of(enum.toString())} in the dropdown editor
 * to use {@code translatable()} so enum display names get translated.
 */
@Mixin(value = GuiOptionEditorDropdown.class, remap = false)
public class GuiOptionEditorDropdownMixin {

    @Redirect(
            method = "<init>",
            at = @At(value = "INVOKE", target = "Lio/github/notenoughupdates/moulconfig/common/text/StructuredText;of(Ljava/lang/String;)Lio/github/notenoughupdates/moulconfig/common/text/StructuredText$Mutable;"),
            require = 0)
    private StructuredText.Mutable translateEnumText(String text) {
        return MoulConfigTextHelper.translatableIfKey(text);
    }
}
