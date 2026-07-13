package top.babyzombie.addons.module.mining.profit;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class SackProfitTracker {
    private static final long HIDE_DELAY_MS = 40_000L;

    private final Map<String, MaterialData> materialDataMap = new LinkedHashMap<>();

    public void handleSackMessage(String messageText, String hoverText, ProfitPriceLookup prices, long nowMillis) {
        Integer seconds = ProfitTextParser.parseSackWindowSeconds(messageText);
        if (seconds == null || hoverText == null || hoverText.isBlank()) return;

        Map<String, Long> parsed = ProfitTextParser.parseSackHover(hoverText);
        for (Map.Entry<String, Long> entry : parsed.entrySet()) {
            trackMaterial(entry.getKey(), entry.getValue(), seconds, prices, nowMillis);
        }
    }

    public void tick(long nowMillis) {
        materialDataMap.values().removeIf(data ->
                data.sessionStartTime > 0 && (nowMillis - data.lastActivityTime) >= HIDE_DELAY_MS);
    }

    public void reset() {
        materialDataMap.clear();
    }

    public boolean hasActiveMaterials() {
        return !materialDataMap.isEmpty();
    }

    public List<MaterialProfitStats> getActiveStats(long nowMillis) {
        List<MaterialProfitStats> stats = new ArrayList<>();
        for (Map.Entry<String, MaterialData> entry : materialDataMap.entrySet()) {
            MaterialData data = entry.getValue();
            stats.add(new MaterialProfitStats(
                    entry.getKey(),
                    ProfitMaterials.getDisplayName(entry.getKey()),
                    data.totalRawEquivalent,
                    data.totalRawEquivalent / ProfitMaterials.getEnchantedRatio(entry.getKey()),
                    data.totalCoins,
                    data.cachedCoinsPerHour,
                    data.cachedEnchantedPerHour,
                    data.cachedCollectionPerHour,
                    Math.max(0L, nowMillis - data.sessionStartTime)
            ));
        }
        stats.sort(Comparator.comparingDouble(MaterialProfitStats::totalValue).reversed());
        return stats;
    }

    private void trackMaterial(
            String material,
            long rawEquivalentThisEvent,
            int seconds,
            ProfitPriceLookup prices,
            long nowMillis
    ) {
        MaterialData data = materialDataMap.computeIfAbsent(material, ignored -> new MaterialData());
        int ratio = ProfitMaterials.getEnchantedRatio(material);

        if (data.sessionStartTime == 0L) {
            data.sessionStartTime = nowMillis - (seconds * 1000L);
        }

        data.totalRawEquivalent += rawEquivalentThisEvent;
        data.lastActivityTime = nowMillis;

        double enchantedPrice = prices.getBlockPrice(ProfitMaterials.getEnchantedItemId(material));
        double totalEnchanted = data.totalRawEquivalent / (double) ratio;
        data.totalCoins = totalEnchanted * enchantedPrice;

        double elapsedHours = Math.max(0D, (nowMillis - data.sessionStartTime) / 3_600_000D);
        data.cachedCoinsPerHour = elapsedHours > 0 ? data.totalCoins / elapsedHours : 0;
        data.cachedEnchantedPerHour = elapsedHours > 0 ? totalEnchanted / elapsedHours : 0;
        data.cachedCollectionPerHour = elapsedHours > 0 ? data.totalRawEquivalent / elapsedHours : 0;
    }

    private static final class MaterialData {
        private long totalRawEquivalent;
        private double totalCoins;
        private long sessionStartTime;
        private long lastActivityTime;
        private double cachedCoinsPerHour;
        private double cachedEnchantedPerHour;
        private double cachedCollectionPerHour;
    }
}
