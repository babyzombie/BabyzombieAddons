package top.babyzombie.addons.command.debug;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.network.chat.Component;
import top.babyzombie.addons.util.ChatUtils;
import top.babyzombie.addons.util.ClientBossbarManager;

import java.util.List;
import java.util.Locale;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommands.argument;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommands.literal;

public final class DebugBossbarCommand {
    private static final int MAX_CONTENT_LENGTH = 256;
    private static final String PFX = "babyzombieaddons.debug.bossbar.";

    private DebugBossbarCommand() {}

    public static void register(ArgumentBuilder<FabricClientCommandSource, ?> parent) {
        parent.then(literal("getbossbar")
                .executes(ctx -> dumpBossbars(ctx.getSource())));

        parent.then(literal("testbossbar")
                .then(argument("local", BoolArgumentType.bool())
                        .then(argument("duration", IntegerArgumentType.integer(1))
                                .then(argument("content", StringArgumentType.greedyString())
                                        .executes(ctx -> createTestBossbar(
                                                ctx.getSource(),
                                                BoolArgumentType.getBool(ctx, "local"),
                                                IntegerArgumentType.getInteger(ctx, "duration"),
                                                StringArgumentType.getString(ctx, "content")))))));
    }

    private static int dumpBossbars(FabricClientCommandSource source) {
        List<ClientBossbarManager.BossbarSnapshot> snapshots = ClientBossbarManager.collectBossbars();
        if (snapshots.isEmpty()) {
            source.sendFeedback(Component.translatable(PFX + "none"));
            return 1;
        }

        source.sendFeedback(Component.translatable(PFX + "header", snapshots.size()));
        for (int i = 0; i < snapshots.size(); i++) {
            source.sendFeedback(buildBossbarMessage(i + 1, snapshots.get(i)));
        }
        return 1;
    }

    private static int createTestBossbar(
            FabricClientCommandSource source,
            boolean localOnly,
            int durationSeconds,
            String rawContent
    ) {
        if (rawContent.length() > MAX_CONTENT_LENGTH) {
            source.sendFeedback(Component.translatable(PFX + "too_long", MAX_CONTENT_LENGTH));
            return 0;
        }

        String sanitized = ClientBossbarManager.sanitizeTestBossbarText(rawContent);
        if (sanitized.isBlank()) {
            source.sendFeedback(Component.translatable(PFX + "empty"));
            return 0;
        }

        ClientBossbarManager.showTestBossbar(localOnly, durationSeconds, sanitized);
        source.sendFeedback(Component.literal(String.format(Locale.ROOT,
                "§a已创建测试 Bossbar: local=%s 持续=%d秒 文本=%s",
                localOnly, durationSeconds, sanitized)));
        return 1;
    }

    private static Component buildBossbarMessage(int index, ClientBossbarManager.BossbarSnapshot snapshot) {
        StringBuilder text = new StringBuilder();
        text.append(T("entry", index)).append("\n");
        text.append(T("id", snapshot.id())).append("\n");
        text.append(T("name", ChatUtils.toLegacyString(snapshot.name()))).append("\n");
        text.append(T("health",
                formatDecimal(snapshot.currentHealth()),
                formatDecimal(snapshot.maxHealth()))).append("\n");
        text.append(T("progress", formatDecimal(snapshot.progress() * 100.0f))).append("\n");
        text.append(T("remaining", snapshot.remainingText())).append("\n");
        text.append(T("render", snapshot.renderX(), snapshot.renderY())).append("\n");
        text.append(T("style",
                snapshot.color().name(),
                snapshot.overlay().name(),
                snapshot.localOnly() ? "true" : "false",
                snapshot.darkenScreen() ? "true" : "false",
                snapshot.playBossMusic() ? "true" : "false",
                snapshot.createWorldFog() ? "true" : "false"));
        return Component.literal(text.toString());
    }

    private static String formatDecimal(float value) {
        return String.format(Locale.ROOT, "%.1f", value);
    }

    private static String T(String key, Object... args) {
        return Component.translatable(PFX + key, args).getString();
    }
}
