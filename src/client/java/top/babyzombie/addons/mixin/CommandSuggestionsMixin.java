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

    @Inject(method = "updateCommandInfo", at = @At("RETURN"))
    private void injectPlayCompletions(CallbackInfo ci) {
        String text = input.getValue().trim();
        if (text.length() < 6 || !text.toLowerCase().startsWith("/play") || !PlayCmdModule.isPlayCmdEnabled()
                || !HypixelLocationTracker.getInstance().isOnHypixel()) return;

        Suggestions base = new Suggestions(StringRange.between(6, text.length()), List.of());
        Suggestions enriched = PlayCmdModule.enrichPlaySuggestions(text, base);
        pendingSuggestions = CompletableFuture.completedFuture(enriched);
        showSuggestions(false);
    }

    @Shadow
    public abstract void showSuggestions(boolean immediateNarration);
}
