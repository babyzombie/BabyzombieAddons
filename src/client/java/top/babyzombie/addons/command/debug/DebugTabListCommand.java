package top.babyzombie.addons.command.debug;

import com.mojang.brigadier.builder.ArgumentBuilder;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.network.chat.Component;
import top.babyzombie.addons.mixin.render.PlayerTabOverlayAccessor;
import top.babyzombie.addons.util.ChatUtils;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommands.literal;

public final class DebugTabListCommand {
    private DebugTabListCommand() {}
    private static final String PFX = "babyzombieaddons.debug.tablist.";

    public static void register(ArgumentBuilder<FabricClientCommandSource, ?> parent) {
        parent.then(literal("gettablist").executes(ctx -> {
            dumpTabList(ctx.getSource());
            return 1;
        }));
    }

    private static void dumpTabList(FabricClientCommandSource src) {
        var client = src.getClient();
        var conn = client.getConnection();
        if (conn == null) {
            src.sendFeedback(Component.translatable(PFX + "no_connection"));
            return;
        }

        var tabList = client.gui.getTabList();
        var ta = (PlayerTabOverlayAccessor) tabList;
        StringBuilder sb = new StringBuilder();

        // Header
        var header = ta.getHeader();
        String headerStr = header != null ? ChatUtils.toLegacyString(header).trim() : "";
        if (!headerStr.isEmpty()) {
            sb.append("Header:\n").append(headerStr).append("\n");
        }

        // Players
        var players = ta.invokeGetPlayerInfos();
        if (!players.isEmpty()) {
            var idx = new int[]{0};
            sb.append("Players:\n");
            players.forEach(playerInfo -> {
                sb.append(idx[0]++).append(". ");
                var displayName = playerInfo.getTabListDisplayName();
                if (displayName != null) {
                    sb.append(ChatUtils.toLegacyString(displayName));
                }
                sb.append("\n");
            });
        }

        // Footer
        var footer = ta.getFooter();
        String footerStr = footer != null ? ChatUtils.toLegacyString(footer).trim() : "";
        if (!footerStr.isEmpty()) {
            sb.append("Footer:\n").append(footerStr).append("\n");
        }

        String result = sb.toString().stripTrailing();
        if (result.isEmpty()) {
            src.sendFeedback(Component.translatable(PFX + "empty"));
            return;
        }

        ChatUtils.copyToClipboard(result);
        src.sendFeedback(Component.translatable(PFX + "copied"));
    }
}
