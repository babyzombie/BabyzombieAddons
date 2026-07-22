package top.babyzombie.addons.command.debug;

import com.mojang.brigadier.builder.ArgumentBuilder;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.network.chat.Component;
import top.babyzombie.addons.mixin.render.GuiAccessor;
import top.babyzombie.addons.util.ChatUtils;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommands.literal;

public final class DebugGetTitleCommand {
    private DebugGetTitleCommand() {}

    public static void register(ArgumentBuilder<FabricClientCommandSource, ?> parent) {
        parent.then(literal("gettitle").executes(ctx -> {
            dumpTitle(ctx.getSource());
            return 1;
        }));
    }

    private static void dumpTitle(FabricClientCommandSource src) {
        var gui = src.getClient().gui;
        var ga = (GuiAccessor) gui;

        var title = ga.getTitle();
        var subtitle = ga.getSubtitle();
        var overlay = ga.getOverlayMessageString();
        int titleTime = ga.getTitleTime();
        int fadeIn = ga.getTitleFadeInTime();
        int stay = ga.getTitleStayTime();
        int fadeOut = ga.getTitleFadeOutTime();
        int overlayTime = ga.getOverlayMessageTime();

        var sb = new StringBuilder();
        sb.append("§6§l=== Title State ===\n");

        // Title
        sb.append("§7Title: ");
        if (title != null) {
            sb.append("§f").append(ChatUtils.toLegacyString(title));
            sb.append("\n§7  (remaining ").append(titleTime).append("t / in ").append(fadeIn)
                    .append(" stay ").append(stay).append(" out ").append(fadeOut).append(")");
        } else {
            sb.append("§8(none)");
        }
        sb.append('\n');

        // Subtitle
        sb.append("§7Subtitle: ");
        if (subtitle != null) {
            sb.append("§f").append(ChatUtils.toLegacyString(subtitle));
        } else {
            sb.append("§8(none)");
        }
        sb.append('\n');

        // ActionBar / OverlayMessage
        sb.append("§7ActionBar: ");
        if (overlay != null) {
            sb.append("§f").append(ChatUtils.toLegacyString(overlay));
            sb.append("\n§7  (remaining ").append(overlayTime).append("t)");
        } else {
            sb.append("§8(none)");
        }

        src.sendFeedback(Component.literal(sb.toString()));
    }
}
