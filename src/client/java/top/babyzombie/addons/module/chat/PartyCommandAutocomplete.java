package top.babyzombie.addons.module.chat;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.tree.LiteralCommandNode;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import org.jspecify.annotations.Nullable;
import top.babyzombie.addons.config.ModConfig;
import top.babyzombie.addons.config.ModConfigManager;
import top.babyzombie.addons.config.PartyConfig;
import top.babyzombie.addons.util.tracker.HypixelLocationTracker;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommands.argument;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommands.literal;

public final class PartyCommandAutocomplete {
    private PartyCommandAutocomplete() {}

    @Nullable
    public static LiteralCommandNode<FabricClientCommandSource> commandNode;

    private static final SuggestionProvider<FabricClientCommandSource> SUGGEST_PARTY_CMD = (context, builder) -> {
        String remaining = builder.getRemaining().toLowerCase();
        for (String cmd : getEnabledPartyCommands()) {
            if (cmd.toLowerCase().startsWith(remaining)) {
                builder.suggest(cmd);
            }
        }
        return CompletableFuture.completedFuture(builder.build());
    };

    /** Build the custom /pc command node. Call from PartyModule.init(). */
    public static void init() {
        commandNode = literal("pc")
                .requires(source -> ModConfigManager.get().party.partyCommandSuggestion
                        && HypixelLocationTracker.getInstance().isOnHypixel())
                .then(argument("message", StringArgumentType.greedyString())
                        .suggests(SUGGEST_PARTY_CMD))
                .build();
    }

    /** Build the list of enabled party commands from current config. */
    private static List<String> getEnabledPartyCommands() {
        List<String> commands = new ArrayList<>();
        PartyConfig cfg = ModConfigManager.get().party;
        if (cfg.partyAllinvite) {
            commands.add("!all");
            commands.add("!allinv");
            commands.add("!allinvite");
        }
        if (cfg.partyInvite) {
            commands.add("!p ");
        }
        if (cfg.partyWarp) {
            commands.add("!w");
            commands.add("!warp");
        }
        if (cfg.partyJoinInstance) {
            commands.add("!join ");
            for (int i = 0; i <= 7; i++) commands.add("!f" + i);
            for (int i = 1; i <= 7; i++) commands.add("!m" + i);
            for (int i = 1; i <= 5; i++) commands.add("!t" + i);
        }
        if (cfg.partySendCoords) {
            commands.add("!sc");
            commands.add("!coords");
            commands.add("!sendcoords");
        }
        if (cfg.partyPlay) {
            commands.add("!play ");
        }
        if (cfg.partyStream) {
            commands.add("!stream ");
        }
        if (cfg.partyTransfer) {
            commands.add("!pt");
            commands.add("!ptme");
        }
        // !dt — dungeon/kuudra auto-requeue
        if (ModConfigManager.get().dungeon.requeue.dungeonRequeue != ModConfig.RequeueMode.OFF
                || ModConfigManager.get().kuudra.requeue.kuudraRequeue != ModConfig.RequeueMode.OFF) {
            commands.add("!dt");
        }
        return commands;
    }
}
