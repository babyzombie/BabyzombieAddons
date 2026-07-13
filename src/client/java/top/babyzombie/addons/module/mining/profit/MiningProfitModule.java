package top.babyzombie.addons.module.mining.profit;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLevelEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import top.babyzombie.addons.config.ModConfigManager;
import top.babyzombie.addons.util.ChatUtils;
import top.babyzombie.addons.util.tracker.HypixelLocationTracker;

import java.util.LinkedHashSet;
import java.util.Set;

public final class MiningProfitModule {
    private static final SackProfitTracker SACK_TRACKER = new SackProfitTracker();
    private static final GemProfitTracker GEM_TRACKER = new GemProfitTracker();
    private static final ProfitPriceService PRICE_SERVICE = new ProfitPriceService();

    private static volatile ProfitSnapshot snapshot = ProfitSnapshot.EMPTY;
    private static boolean enabledLastTick;

    private MiningProfitModule() {}

    public static void init() {
        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            if (overlay || !isEnabled() || !isInSkyblock()) return;

            long now = System.currentTimeMillis();
            syncConfig();

            String plain = normalizeForParsing(message.getString());
            String hoverText = extractHoverText(message);

            if (ProfitTextParser.parseSackWindowSeconds(plain) != null) {
                PRICE_SERVICE.updatePricesAsync();
                SACK_TRACKER.handleSackMessage(plain, hoverText, PRICE_SERVICE, now);
            }

            if (ProfitTextParser.parsePristineMessage(plain) != null) {
                PRICE_SERVICE.updatePricesAsync();
                GEM_TRACKER.handlePristineMessage(plain, PRICE_SERVICE, now);
            }
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (!isEnabled()) {
                if (enabledLastTick) {
                    reset();
                }
                enabledLastTick = false;
                snapshot = ProfitSnapshot.EMPTY;
                return;
            }

            enabledLastTick = true;
            syncConfig();

            long now = System.currentTimeMillis();
            SACK_TRACKER.tick(now);
            GEM_TRACKER.tick(now);
            snapshot = new ProfitSnapshot(
                    SACK_TRACKER.getActiveStats(now),
                    GEM_TRACKER.getStats(now, PRICE_SERVICE),
                    PRICE_SERVICE.isUsingNpcPrices()
            );
        });

        ClientLevelEvents.AFTER_CLIENT_LEVEL_CHANGE.register((client, world) -> {
            reset();
            if (world != null && isEnabled()) {
                syncConfig();
                PRICE_SERVICE.updatePricesAsync();
            }
        });

        MiningProfitHud.init();
    }

    public static ProfitSnapshot getSnapshot() {
        return snapshot;
    }

    private static void syncConfig() {
        var config = ModConfigManager.get().mining;
        PRICE_SERVICE.setUseNpcPrices(config.profitTrackerUseNpcPrices);
        GEM_TRACKER.setIncludeRough(config.profitTrackerIncludeRoughGems);
        GEM_TRACKER.setPristineChance(config.profitTrackerPristineChance);
        GEM_TRACKER.setGemTier(config.profitTrackerGemTier);
    }

    private static boolean isEnabled() {
        return ModConfigManager.get().mining.profitTracker;
    }

    private static boolean isInSkyblock() {
        return HypixelLocationTracker.getInstance().isInSkyblock();
    }

    private static void reset() {
        SACK_TRACKER.reset();
        GEM_TRACKER.reset();
        snapshot = ProfitSnapshot.EMPTY;
    }

    private static String normalizeForParsing(String text) {
        return ChatUtils.removeEmoji(ChatUtils.stripColor(text))
                .replace('\u00A0', ' ')
                .replaceAll("\\s+", " ")
                .trim();
    }

    private static String extractHoverText(Component message) {
        Set<String> seen = new LinkedHashSet<>();
        StringBuilder sb = new StringBuilder();
        appendHoverText(message, sb, seen);
        return normalizeHoverText(sb.toString());
    }

    private static void appendHoverText(Component component, StringBuilder sb, Set<String> seen) {
        HoverEvent hover = component.getStyle().getHoverEvent();
        if (hover instanceof HoverEvent.ShowText showText) {
            String text = showText.value().getString();
            if (seen.add(text)) {
                if (!sb.isEmpty()) {
                    sb.append('\n');
                }
                sb.append(text);
            }
        }

        for (Component sibling : component.getSiblings()) {
            appendHoverText(sibling, sb, seen);
        }
    }

    private static String normalizeHoverText(String text) {
        if (text == null || text.isBlank()) return "";

        StringBuilder normalized = new StringBuilder();
        for (String line : text.split("\n")) {
            String clean = ChatUtils.removeEmoji(ChatUtils.stripColor(line))
                    .replace('\u00A0', ' ')
                    .replaceAll("\\s+", " ")
                    .trim();
            if (clean.isEmpty()) continue;
            if (!normalized.isEmpty()) {
                normalized.append('\n');
            }
            normalized.append(clean);
        }
        return normalized.toString();
    }
}
