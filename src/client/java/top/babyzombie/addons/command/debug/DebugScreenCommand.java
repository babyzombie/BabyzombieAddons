package top.babyzombie.addons.command.debug;

import com.mojang.brigadier.builder.ArgumentBuilder;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommands.literal;

public final class DebugScreenCommand {
    private DebugScreenCommand() {}

    public static void register(ArgumentBuilder<FabricClientCommandSource, ?> parent) {
        parent.then(literal("screen").executes(ctx -> screen(ctx.getSource())));
    }

    private static int screen(FabricClientCommandSource src) {
        var mc = Minecraft.getInstance();
        var win = mc.getWindow();

        var framebufferW = win.getWidth();
        var framebufferH = win.getHeight();
        var guiW = win.getGuiScaledWidth();
        var guiH = win.getGuiScaledHeight();
        var guiScale = win.getGuiScale();
        var scaleFactor = win.calculateScale(0, mc.isEnforceUnicode());
        var fullscreen = win.isFullscreen();

        var sb = new StringBuilder();
        sb.append("§6§l=== Screen Info ===\n");
        sb.append("§7Fullscreen: §f").append(fullscreen).append('\n');
        sb.append("§7Framebuffer: §f").append(framebufferW).append(" × ").append(framebufferH).append('\n');
        sb.append("§7GUI Scaled:  §f").append(guiW).append(" × ").append(guiH).append('\n');
        sb.append("§7GUI Scale:   §f").append(guiScale).append('\n');
        sb.append("§7Scale Factor:§f ").append(scaleFactor);

        src.sendFeedback(Component.literal(sb.toString()));
        return 1;
    }
}
