package top.babyzombie.addons.command.debug;

import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import top.babyzombie.addons.event.ParticleRenderEvents;
import top.babyzombie.addons.util.ChatUtils;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommands.argument;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommands.literal;

/**
 * /bza debug getparticle — 开关式粒子监听。
 * 开启后，把玩家周围 N 格内创建的所有粒子打印到聊天栏（详情放在 hover 里）。
 * 用法：
 *   /bza debug getparticle         切换开/关（开启时默认 5 格）
 *   /bza debug getparticle <范围>   用指定范围开启（单位：格，关闭后重开会重置）
 */
public final class DebugGetParticleCommand {
    private static boolean monitor;
    private static double range = 5.0;

    private DebugGetParticleCommand() {}

    public static void init() {
        ParticleRenderEvents.BEFORE_CREATE.register((options, x, y, z, xa, ya, za) -> {
            if (!monitor) return false;
            var player = Minecraft.getInstance().player;
            if (player == null) return false;

            double dx = x - player.getX();
            double dy = y - player.getY();
            double dz = z - player.getZ();
            if (dx * dx + dy * dy + dz * dz > range * range) return false;

            var key = BuiltInRegistries.PARTICLE_TYPE.getKey(options.getType());
            String id = key == null ? "?" : key.toString();
            String className = options.getClass().getSimpleName();

            var hover = Component.translatable(
                    "babyzombieaddons.debug.particle.hover",
                    className, xa, ya, za);
            var line = Component.translatable(
                    "babyzombieaddons.debug.particle.line", id, x, y, z)
                    .withStyle(style -> style.withHoverEvent(new HoverEvent.ShowText(hover)));
            ChatUtils.showMessage(line);
            return false;
        });
    }

    public static void register(ArgumentBuilder<FabricClientCommandSource, ?> parent) {
        parent.then(literal("getparticle")
                .executes(ctx -> toggle(ctx.getSource()))
                .then(argument("range", DoubleArgumentType.doubleArg(0.5, 128.0))
                        .executes(ctx -> setRange(ctx.getSource(),
                                DoubleArgumentType.getDouble(ctx, "range")))));
    }

    private static int toggle(FabricClientCommandSource src) {
        monitor = !monitor;
        if (monitor) range = 5.0;
        src.sendFeedback(monitor
                ? Component.translatable("babyzombieaddons.debug.particle.monitor_on", range)
                : Component.translatable("babyzombieaddons.debug.particle.monitor_off"));
        return 1;
    }

    private static int setRange(FabricClientCommandSource src, double r) {
        range = r;
        monitor = true;
        src.sendFeedback(Component.translatable("babyzombieaddons.debug.particle.monitor_on", range));
        return 1;
    }
}
