package top.babyzombie.addons.command;

import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import top.babyzombie.addons.config.ModConfigManager;
import top.babyzombie.addons.module.playcmd.PlayCmdModule;
import top.babyzombie.addons.util.ChatUtils;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommands.literal;

public final class BabyzombieAddonsCommand {
    private BabyzombieAddonsCommand() {}

    public static void init() {
        SoundCommand.init();

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            var bza = literal("bza").executes(BabyzombieAddonsCommand::settings)
                    .then(literal("s").executes(BabyzombieAddonsCommand::settings))
                    .then(literal("settings").executes(BabyzombieAddonsCommand::settings))
                    .then(literal("play").executes(BabyzombieAddonsCommand::play))
                    .then(literal("help").executes(BabyzombieAddonsCommand::help))
                    .then(literal("l").executes(ctx -> { ChatUtils.sendCommand("limbo"); return 1; }))
                    .then(literal("trevorautocall").executes(ctx -> {
                        top.babyzombie.addons.module.garden.TrevorAutoAccept.disableAutoCall();
                        ctx.getSource().sendFeedback(
                                Component.translatable("babyzombieaddons.trevor.auto_call_disabled"));
                        return 1;
                    }))
                    ;

            var debug = literal("debug");
            SoundCommand.register(debug);
            InfoCommand.register(debug);

            SendCoordsCommand.register(bza);
            RotationCommand.register(bza);
            WaypointCommand.register(bza);
            bza.then(debug);

            dispatcher.register(bza);
            dispatcher.register(literal("babyzombieaddons").executes(BabyzombieAddonsCommand::settings)
                    .then(literal("settings").executes(BabyzombieAddonsCommand::settings)));
        });
    }

    static int settings(CommandContext<FabricClientCommandSource> ctx) {
        Minecraft.getInstance().execute(() ->
                Minecraft.getInstance().setScreenAndShow(ModConfigManager.createGUI(null)));
        return 1;
    }

    private static int help(CommandContext<FabricClientCommandSource> ctx) {
        ctx.getSource().sendFeedback(Component.translatable("babyzombieaddons.help"));
        return 1;
    }

    private static int play(CommandContext<FabricClientCommandSource> ctx) {
        PlayCmdModule.openGUI();
        return 1;
    }
}
