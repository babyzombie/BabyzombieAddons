package top.babyzombie.addons.mixin;

import com.mojang.brigadier.context.StringRange;
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
import top.babyzombie.addons.module.playcmd.PlayCmdModule;
import top.babyzombie.addons.util.tracker.HypixelLocationTracker;

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

    @Inject(method = "updateCommandInfo", at = @At("RETURN"))
    private void injectPlayCompletions(CallbackInfo ci) {
        // When keepSuggestions is true, the system is in the middle of Tab cycling
        // and preserving the original SuggestionsList. Don't interfere.
        if (keepSuggestions) return;

        String text = input.getValue();
        if (!PlayCmdModule.isPlayCmdEnabled()
                || !HypixelLocationTracker.getInstance().isOnHypixel()) return;

        int rangeStart = PlayCmdModule.getGameArgsStart(text);
        if (rangeStart < 0) return;

        Suggestions base = new Suggestions(StringRange.between(rangeStart, text.length()), List.of());
        Suggestions enriched = PlayCmdModule.enrichPlaySuggestions(text, base);
        pendingSuggestions = CompletableFuture.completedFuture(enriched);
        showSuggestions(false);
    }

    @Shadow
    public abstract void showSuggestions(boolean immediateNarration);
}
