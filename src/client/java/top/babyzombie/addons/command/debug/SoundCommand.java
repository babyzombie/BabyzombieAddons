package top.babyzombie.addons.command.debug;

import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import top.babyzombie.addons.event.PlaySoundEvents;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommands.argument;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommands.literal;

public final class SoundCommand {
    private static boolean monitor;
    private static final java.util.Set<String> blacklist = new java.util.LinkedHashSet<>();

    private SoundCommand() {}

    public static void init() {
        PlaySoundEvents.BEFORE_PLAY.register(sound -> {
            if (!monitor) return false;
            var snd = sound.getSound();
            String id;
            if (snd != null) {
                id = snd.getLocation().toString();
            } else {
                id = "?";
            }
            if (blacklist.contains(id)) return false;

            float vol = 0, pit = 0;
            double sx = 0, sy = 0, sz = 0;
            try { vol = sound.getVolume(); } catch (Exception ignored) {}
            try { pit = sound.getPitch(); } catch (Exception ignored) {}
            try { sx = sound.getX(); } catch (Exception ignored) {}
            try { sy = sound.getY(); } catch (Exception ignored) {}
            try { sz = sound.getZ(); } catch (Exception ignored) {}

            String msg = String.format(
                    "§7resource: §f%s §7volume: §f%.2f §7, pitch: §f%.2f §7x: §f%.2f §7, y: §f%.2f §7, z: §f%.2f",
                    id, vol, pit, sx, sy, sz);

            var comp = Component.literal(msg)
                    .withStyle(style -> style
                            .withClickEvent(new ClickEvent.RunCommand("/bza debug getsound " + id))
                            .withHoverEvent(new net.minecraft.network.chat.HoverEvent.ShowText(
                                    Component.translatable("babyzombieaddons.sound.hover", id))));
            var player = Minecraft.getInstance().player;
            if (player != null) player.sendSystemMessage(comp);
            return false;
        });
    }

    public static void register(
            com.mojang.brigadier.builder.ArgumentBuilder<FabricClientCommandSource, ?> parent) {
        parent.then(literal("getsound")
                .executes(ctx -> toggle(ctx.getSource()))
                .then(literal("clear").executes(ctx -> clear(ctx.getSource())))
                .then(argument("filter", StringArgumentType.greedyString())
                        .executes(ctx -> set(ctx.getSource(),
                                StringArgumentType.getString(ctx, "filter")))));
    }

    private static int toggle(FabricClientCommandSource src) {
        monitor = !monitor;
        blacklist.clear();
        src.sendFeedback(Component.literal(
                monitor ? Component.translatable("babyzombieaddons.sound.monitor_on").getString()
                        : Component.translatable("babyzombieaddons.sound.monitor_off").getString()));
        return 1;
    }

    private static int clear(FabricClientCommandSource src) {
        blacklist.clear();
        src.sendFeedback(Component.translatable("babyzombieaddons.sound.clear_ignore"));
        return 1;
    }

    private static int set(FabricClientCommandSource src, String filter) {
        if (blacklist.contains(filter)) {
            blacklist.remove(filter);
            src.sendFeedback(
                    Component.translatable("babyzombieaddons.sound.removed_from_ignore", filter));
        } else {
            blacklist.add(filter);
            src.sendFeedback(
                    Component.translatable("babyzombieaddons.sound.added_to_ignore", filter));
        }
        return 1;
    }
}
