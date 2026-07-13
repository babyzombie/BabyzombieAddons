package top.babyzombie.addons.config;

import io.github.notenoughupdates.moulconfig.common.text.StructuredText;

/**
 * Shared helper for Mixins that need to convert MoulConfig strings to
 * translatable text when they look like translation keys.
 */
public final class MoulConfigTextHelper {

    private MoulConfigTextHelper() {}

    /**
     * Returns {@code StructuredText.translatable(text)} if the string
     * contains a dot (translation key pattern), otherwise literal text.
     */
    public static StructuredText.Mutable translatableIfKey(String text) {
        if (text.isEmpty()) return StructuredText.empty();
        if (text.contains(".")) return StructuredText.translatable(text);
        return StructuredText.of(text);
    }
}
