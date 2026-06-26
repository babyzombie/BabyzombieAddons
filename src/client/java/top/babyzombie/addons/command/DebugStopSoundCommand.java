package top.babyzombie.addons.command;

import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.arguments.IdentifierArgument;
import net.minecraft.commands.synchronization.SuggestionProviders;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import top.babyzombie.addons.mixin.sound.SoundEngineAccessor;
import top.babyzombie.addons.mixin.sound.SoundManagerAccessor;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommands.argument;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommands.literal;

final class DebugStopSoundCommand {
    private DebugStopSoundCommand() {}

    static void register(
            com.mojang.brigadier.builder.ArgumentBuilder<FabricClientCommandSource, ?> parent) {
        parent.then(literal("stopsound")
                .executes(ctx -> stop(ctx.getSource(), null))
                .then(argument("id", IdentifierArgument.id())
                        .suggests(SuggestionProviders.cast(SuggestionProviders.AVAILABLE_SOUNDS))
                        .executes(ctx -> stop(ctx.getSource(),
                                ctx.getArgument("id", Identifier.class)))));
    }

    private static int stop(FabricClientCommandSource src, Identifier sound) {
        var engine = ((SoundManagerAccessor) Minecraft.getInstance().getSoundManager())
                .getSoundEngine();
        ((SoundEngineAccessor) engine).invokeStop(sound, null);

        if (sound == null) {
            src.sendFeedback(Component.translatable("babyzombieaddons.debug.stopsound.all"));
        } else {
            src.sendFeedback(Component.translatable(
                    "babyzombieaddons.debug.stopsound.one", sound.toString()));
        }
        return 1;
    }
}
