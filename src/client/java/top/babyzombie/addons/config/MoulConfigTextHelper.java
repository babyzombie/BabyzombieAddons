package top.babyzombie.addons.config;

import io.github.notenoughupdates.moulconfig.common.text.StructuredText;

/**
 * Shared helper for Mixins that need to convert MoulConfig strings to
 * translatable text when they look like translation keys.
 */
public final class MoulConfigTextHelper {

    private MoulConfigTextHelper() {}

    /** Only translate keys that belong to our mod. */
    private static final String KEY_PREFIX = "config.babyzombieaddons.";

    /**
     * Returns {@code StructuredText.translatable(text)} if the string
     * starts with our mod's translation key prefix, otherwise literal text.
     */
    public static StructuredText.Mutable translatableIfKey(String text) {
        if (text.isEmpty()) return StructuredText.empty();
        if (text.startsWith(KEY_PREFIX)) return StructuredText.translatable(text);
        return StructuredText.of(text);
    }
}
