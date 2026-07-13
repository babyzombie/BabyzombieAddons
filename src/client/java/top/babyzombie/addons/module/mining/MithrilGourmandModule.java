package top.babyzombie.addons.module.mining;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLevelEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import top.babyzombie.addons.config.ModConfigManager;
import top.babyzombie.addons.util.ChatUtils;
import top.babyzombie.addons.util.ClientBossbarManager;

import java.util.Locale;
import java.util.UUID;

public final class MithrilGourmandModule {
    private static final String TARGET_KEYWORD = "MITHRIL GOURMAND";
    private static UUID lastTriggeredBossbarId;

    private MithrilGourmandModule() {}

    public static void init() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> onClientTick());
        ClientLevelEvents.AFTER_CLIENT_LEVEL_CHANGE.register((client, world) -> lastTriggeredBossbarId = null);
    }

    /**
     * Watch the client-side bossbar list and fire the configured command once when the
     * Mithril Gourmand countdown reaches the chosen threshold.
     */
    private static void onClientTick() {
        var client = Minecraft.getInstance();
        if (client.player == null || client.getConnection() == null) {
            lastTriggeredBossbarId = null;
            return;
        }

        var config = ModConfigManager.get().mining;
        if (!config.mithrilGourmand.autoExpresso) {
            lastTriggeredBossbarId = null;
            return;
        }

        UUID candidateId = null;
        int smallestRemaining = Integer.MAX_VALUE;

        for (var snapshot : ClientBossbarManager.collectBossbars()) {
            if (snapshot.localOnly()) continue;

            String plainText = ChatUtils.stripColor(snapshot.name().getString()).toUpperCase(Locale.ROOT);
            if (!plainText.contains(TARGET_KEYWORD)) continue;
            if (snapshot.remainingSeconds() == null) continue;

            if (snapshot.remainingSeconds() < smallestRemaining) {
                smallestRemaining = snapshot.remainingSeconds();
                candidateId = snapshot.id();
            }
        }

        if (candidateId == null) {
            lastTriggeredBossbarId = null;
            return;
        }

        if (smallestRemaining <= config.mithrilGourmand.triggerSeconds
                && !candidateId.equals(lastTriggeredBossbarId)) {
            ChatUtils.sendCommand("tptodonexpresso");
            lastTriggeredBossbarId = candidateId;
        }
    }
}
