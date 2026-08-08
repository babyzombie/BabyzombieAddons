package top.babyzombie.addons.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;
import top.babyzombie.addons.command.debug.*;
import top.babyzombie.addons.config.ModConfigManager;
import top.babyzombie.addons.config.hud.HudManager;
import top.babyzombie.addons.config.hud.HudTag;
import top.babyzombie.addons.module.chat.PartyModule;
import top.babyzombie.addons.module.chat.playcmd.PlayCmdModule;
import top.babyzombie.addons.util.ChatUtils;

import java.util.Arrays;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommands.literal;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommands.argument;

public final class BabyzombieAddonsCommand {
    private BabyzombieAddonsCommand() {}

    // /bza hud <tag> 的标签补全,前缀匹配且大小写不敏感
    private static final SuggestionProvider<FabricClientCommandSource> SUGGEST_HUD_TAG = (context, builder) -> {
        String remaining = builder.getRemaining().toLowerCase(Locale.ROOT);
        for (HudTag tag : HudTag.values()) {
            if (tag.name().toLowerCase(Locale.ROOT).startsWith(remaining)) {
                builder.suggest(tag.name());
            }
        }
        return CompletableFuture.completedFuture(builder.build());
    };

    public static void init() {
        SoundCommand.init();
        FindCommand.init();

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            var bza = literal("bza").executes(BabyzombieAddonsCommand::settings)
                    .then(literal("s")
                            .executes(BabyzombieAddonsCommand::settings)
                            .then(argument("search", StringArgumentType.greedyString())
                                    .executes(BabyzombieAddonsCommand::settingsWithSearch)))
                    .then(literal("settings")
                            .executes(BabyzombieAddonsCommand::settings)
                            .then(argument("search", StringArgumentType.greedyString())
                                    .executes(BabyzombieAddonsCommand::settingsWithSearch)))
                    .then(literal("hud")
                            .executes(BabyzombieAddonsCommand::hud)
                            .then(argument("tag", StringArgumentType.word())
                                    .suggests(SUGGEST_HUD_TAG)
                                    .executes(BabyzombieAddonsCommand::hudWithTag)))
                    .then(literal("play").executes(BabyzombieAddonsCommand::play))
                    .then(literal("help").executes(BabyzombieAddonsCommand::help))
                    .then(literal("autois").executes(BabyzombieAddonsCommand::toggleAutois))
                    .then(literal("l").executes(ctx -> { ChatUtils.sendCommand("limbo"); return 1; }))
                    .then(literal("trevorautocall").executes(ctx -> {
                        top.babyzombie.addons.module.garden.TrevorAutoAccept.disableAutoCall();
                        ctx.getSource().sendFeedback(
                                Component.translatable("babyzombieaddons.trevor.auto_call_disabled"));
                        return 1;
                    }))
                    .then(literal("acceptpartyinvite")
                            .executes(ctx -> acceptPartyInvite(ctx, null))
                            .then(argument("player", StringArgumentType.word())
                                    .executes(ctx -> acceptPartyInvite(ctx,
                                            StringArgumentType.getString(ctx, "player")))))
                    .then(literal("ap")
                            .executes(ctx -> acceptPartyInvite(ctx, null))
                            .then(argument("player", StringArgumentType.word())
                                    .executes(ctx -> acceptPartyInvite(ctx,
                                            StringArgumentType.getString(ctx, "player")))))
                    .then(literal("acceptparty")
                            .executes(ctx -> acceptPartyInvite(ctx, null))
                            .then(argument("player", StringArgumentType.word())
                                    .executes(ctx -> acceptPartyInvite(ctx,
                                            StringArgumentType.getString(ctx, "player")))))
                    ;

            var debug = literal("debug");
            SoundCommand.register(debug);
            InfoCommand.register(debug);
            DebugPlaySoundCommand.register(debug);
            DebugStopSoundCommand.register(debug);
            DebugEntityCommand.register(debug);
            DebugPetCommand.register(debug);
            DebugPartyCommand.register(debug);
            DebugRankCommand.register(debug);
            DebugBossbarCommand.register(debug);
            DebugTabListCommand.register(debug);
            DebugScreenCommand.register(debug);
            DebugHitResultCommand.register(debug);
            DebugGetTitleCommand.register(debug);
            DebugUnhideCommand.register(debug);

            FindCommand.register(bza);
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
                Minecraft.getInstance().gui.setScreen(ModConfigManager.createGUI(null)));
        return 1;
    }

    static int settingsWithSearch(CommandContext<FabricClientCommandSource> ctx) {
        String search = StringArgumentType.getString(ctx, "search");
        Minecraft.getInstance().execute(() ->
                Minecraft.getInstance().gui.setScreen(ModConfigManager.createGUI(null, search)));
        return 1;
    }

    static int hud(CommandContext<FabricClientCommandSource> ctx) {
        Minecraft.getInstance().execute(() ->
                HudManager.openEditScreen(null));
        return 1;
    }

    static int hudWithTag(CommandContext<FabricClientCommandSource> ctx) {
        String raw = StringArgumentType.getString(ctx, "tag");
        try {
            HudTag tag = HudTag.valueOf(raw.toUpperCase(Locale.ROOT));
            Minecraft.getInstance().execute(() ->
                    HudManager.openEditScreen(null, tag));
        } catch (IllegalArgumentException e) {
            String available = Arrays.stream(HudTag.values())
                    .map(HudTag::name)
                    .reduce((a, b) -> a + ", " + b)
                    .orElse("");
            ctx.getSource().sendFeedback(
                    Component.translatable("babyzombieaddons.command.hud.invalidTag", raw, available));
        }
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

    private static int toggleAutois(CommandContext<FabricClientCommandSource> ctx) {
        boolean enabled = !ModConfigManager.get().skyblock.autois.enabled;
        ModConfigManager.get().skyblock.autois.enabled = enabled;
        ctx.getSource().sendFeedback(
                Component.translatable(enabled
                        ? "babyzombieaddons.command.autois.enabled"
                        : "babyzombieaddons.command.autois.disabled"));
        return 1;
    }

    private static int acceptPartyInvite(CommandContext<FabricClientCommandSource> ctx, @Nullable String player) {
        PartyModule.scheduleAutoAccept(player);
        if (player != null) {
            ctx.getSource().sendFeedback(
                    Component.translatable("babyzombieaddons.acceptparty.scheduled_player", player));
        } else {
            ctx.getSource().sendFeedback(
                    Component.translatable("babyzombieaddons.acceptparty.scheduled_any"));
        }
        return 1;
    }
}
