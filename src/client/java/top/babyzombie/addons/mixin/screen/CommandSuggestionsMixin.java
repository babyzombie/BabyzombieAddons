package top.babyzombie.addons.mixin.screen;

import com.mojang.brigadier.context.StringRange;
import com.mojang.brigadier.suggestion.Suggestion;
import com.mojang.brigadier.suggestion.Suggestions;
import net.minecraft.client.gui.components.CommandSuggestions;
import net.minecraft.client.gui.components.EditBox;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import top.babyzombie.addons.module.playcmd.PlayAutocomplete;
import top.babyzombie.addons.module.playcmd.PlayCmdModule;
import top.babyzombie.addons.util.tracker.HypixelLocationTracker;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Mixin(CommandSuggestions.class)
public abstract class CommandSuggestionsMixin {

    @Shadow
    @Final
    private EditBox input;

    @Shadow
    private @Nullable CompletableFuture<Suggestions> pendingSuggestions;

    @Shadow
    private boolean keepSuggestions;

    /**
     * Only handle chat prefixes !play and /pc !play.
     * /play command autocomplete is handled by ClientboundCommandsPacketMixin.
     */
    @Inject(method = "updateCommandInfo", at = @At("RETURN"))
    private void injectPlayCompletions(CallbackInfo ci) {
        // When keepSuggestions is true, the system is in the middle of Tab cycling
        // and preserving the original SuggestionsList. Don't interfere.
        if (keepSuggestions) return;

        String text = input.getValue();
        if (!PlayCmdModule.isPlayCmdEnabled()
                || !HypixelLocationTracker.getInstance().isOnHypixel()) return;

        int rangeStart = getChatPlayArgsStart(text);
        if (rangeStart < 0) return;

        String prefix = rangeStart < text.length()
                ? text.substring(rangeStart).toLowerCase()
                : "";
        StringRange range = StringRange.between(rangeStart, text.length());
        boolean needLeadingSpace = rangeStart == text.length();

        List<Suggestion> list = new ArrayList<>();
        for (String game : PlayAutocomplete.getMatchingGames(prefix)) {
            list.add(new Suggestion(range, needLeadingSpace ? " " + game : game));
        }

        pendingSuggestions = CompletableFuture.completedFuture(new Suggestions(range, list));
        showSuggestions(false);
    }

    /** Only match !play and /pc !play prefixes. Skip /play (handled by ClientboundCommandsPacketMixin). */
    private static int getChatPlayArgsStart(String text) {
        if (text.length() < 5) return -1;
        String lower = text.toLowerCase();
        int playEnd;
        if (lower.startsWith("!play")) {
            // Skip /play — starts with '/' so it's handled by ClientboundCommandsPacketMixin
            if (lower.charAt(0) == '/') return -1;
            playEnd = 5;
        } else if (lower.startsWith("/pc !play")) {
            playEnd = 9;
        } else {
            return -1;
        }
        if (text.length() > playEnd && text.charAt(playEnd) == ' ') {
            return playEnd + 1;
        }
        return text.length();
    }

    @Shadow
    public abstract void showSuggestions(boolean immediateNarration);
}
