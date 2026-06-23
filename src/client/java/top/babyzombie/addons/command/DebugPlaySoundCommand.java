package top.babyzombie.addons.command;

import com.mojang.brigadier.arguments.DoubleArgumentType;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.commands.arguments.IdentifierArgument;
import net.minecraft.commands.synchronization.SuggestionProviders;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundSource;
import top.babyzombie.addons.util.PlaySoundHelper;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommands.argument;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommands.literal;

final class DebugPlaySoundCommand {
    private DebugPlaySoundCommand() {}

    static void register(
            com.mojang.brigadier.builder.ArgumentBuilder<FabricClientCommandSource, ?> parent) {
        parent.then(literal("playsound")
                .executes(ctx -> {
                    ctx.getSource().sendFeedback(
                            Component.translatable("babyzombieaddons.debug.playsound.usage"));
                    return 1;
                })
                .then(argument("id", IdentifierArgument.id())
                        .suggests(SuggestionProviders.cast(SuggestionProviders.AVAILABLE_SOUNDS))
                        .executes(ctx -> play(ctx.getSource(),
                                ctx.getArgument("id", Identifier.class), 1.0f, 1.0f, 0, 0))
                        .then(argument("volume", DoubleArgumentType.doubleArg(0, 10))
                                .then(argument("pitch", DoubleArgumentType.doubleArg(0.5, 2))
                                        .executes(ctx -> play(ctx.getSource(),
                                                ctx.getArgument("id", Identifier.class),
                                                (float) DoubleArgumentType.getDouble(ctx, "volume"),
                                                (float) DoubleArgumentType.getDouble(ctx, "pitch"),
                                                0, 0))
                                        .then(argument("seek", DoubleArgumentType.doubleArg(0))
                                                .then(argument("duration", DoubleArgumentType.doubleArg(0.1, 60))
                                                        .executes(ctx -> play(ctx.getSource(),
                                                                ctx.getArgument("id", Identifier.class),
                                                                (float) DoubleArgumentType.getDouble(ctx, "volume"),
                                                                (float) DoubleArgumentType.getDouble(ctx, "pitch"),
                                                                (float) DoubleArgumentType.getDouble(ctx, "seek"),
                                                                (float) DoubleArgumentType.getDouble(ctx, "duration")))))))));
    }

    private static int play(FabricClientCommandSource src, Identifier location,
                            float volume, float pitch, float seek, float duration) {
        volume = clamp(volume, 0, 10);
        pitch = clamp(pitch, 0.5f, 2.0f);

        // relative + NONE attenuation → 声音跟随玩家，不会走远就没了
        var instance = new SimpleSoundInstance(
                location, SoundSource.MASTER,
                volume, pitch,
                SoundInstance.createUnseededRandom(),
                false, 0,
                SoundInstance.Attenuation.NONE,
                0, 0, 0, true
        );

        if (seek > 0 || duration > 0) {
            PlaySoundHelper.playSeeked(instance, seek, duration);
        } else {
            Minecraft.getInstance().getSoundManager().play(instance);
        }

        if (seek > 0 || duration > 0) {
            src.sendFeedback(Component.translatable(
                    "babyzombieaddons.debug.playsound.played_seeked",
                    location.toString(),
                    String.format("%.1f", volume),
                    String.format("%.1f", pitch),
                    String.format("%.1f", seek),
                    String.format("%.1f", duration)));
        } else {
            src.sendFeedback(Component.translatable(
                    "babyzombieaddons.debug.playsound.played",
                    location.toString(),
                    String.format("%.1f", volume),
                    String.format("%.1f", pitch)));
        }
        return 1;
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
}
