package top.babyzombie.addons.command.debug;

import com.mojang.brigadier.builder.ArgumentBuilder;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.network.chat.Component;
import top.babyzombie.addons.util.tracker.HypixelPlayerInfoTracker;
import top.babyzombie.addons.util.tracker.HypixelPlayerInfoTracker.PlayerInfo;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommands.literal;

public final class DebugRankCommand {
    private DebugRankCommand() {}

    private static final String PFX = "babyzombieaddons.debug.rank.";

    public static void register(ArgumentBuilder<FabricClientCommandSource, ?> parent) {
        parent.then(literal("rank").executes(ctx -> {
            dumpRankInfo(ctx.getSource());
            return 1;
        }));
    }

    private static void dumpRankInfo(FabricClientCommandSource src) {
        PlayerInfo info = HypixelPlayerInfoTracker.getInstance().getLastInfo();

        src.sendFeedback(Component.translatable(PFX + "header"));

        if (info == null) {
            src.sendFeedback(Component.translatable(PFX + "no_data"));
            return;
        }

        src.sendFeedback(Component.translatable(PFX + "player_rank", info.playerRank().name()));
        src.sendFeedback(Component.translatable(PFX + "package_rank", info.packageRank().name()));
        src.sendFeedback(Component.translatable(PFX + "monthly_package_rank", info.monthlyPackageRank().name()));
        src.sendFeedback(Component.translatable(PFX + "prefix",
                info.prefix() != null ? info.prefix() : Component.translatable(PFX + "none").getString()));
    }
}
