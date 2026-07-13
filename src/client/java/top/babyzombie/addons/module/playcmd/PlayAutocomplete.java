package top.babyzombie.addons.module.playcmd;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.tree.LiteralCommandNode;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import org.jspecify.annotations.Nullable;
import top.babyzombie.addons.config.ModConfigManager;
import top.babyzombie.addons.util.tracker.HypixelLocationTracker;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommands.argument;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommands.literal;

public final class PlayAutocomplete {
    private PlayAutocomplete() {}

    @Nullable
    public static LiteralCommandNode<FabricClientCommandSource> commandNode;

    private static final SuggestionProvider<FabricClientCommandSource> SUGGEST_GAME = (context, builder) -> {
        String remaining = builder.getRemaining().toLowerCase();
        List<String> matches = getMatchingGames(remaining);
        for (String game : matches) {
            builder.suggest(game);
        }
        return CompletableFuture.completedFuture(builder.build());
    };

    /** Build the custom /play command node. Call from PlayCmdModule.init(). */
    public static void init() {
        commandNode = literal("play")
                .requires(source -> ModConfigManager.get().general.playCmd
                        && HypixelLocationTracker.getInstance().isOnHypixel())
                .executes(ctx -> 1) // no-op, GUI is handled by SendCommandEvents
                .then(argument("game", StringArgumentType.greedyString())
                        .suggests(SUGGEST_GAME)
                        .executes(ctx -> 1))
                .build();
    }

    /** Filter games by prefix (case-insensitive). Public so CommandSuggestionsMixin can reuse. */
    public static List<String> getMatchingGames(String prefix) {
        List<String> result = new ArrayList<>();
        for (Object[][] cat : PlayCmdModule.GAMES) {
            for (int i = 1; i < cat.length; i++) {
                String cmd = (String) cat[i][1];
                if (!cmd.startsWith("/play ")) continue;
                String sub = cmd.substring(6);
                if (prefix.isEmpty() || sub.toLowerCase().startsWith(prefix)) {
                    if (!result.contains(sub)) result.add(sub);
                }
            }
        }
        return result;
    }
}