package top.babyzombie.addons.module.mining.profit;

import top.babyzombie.addons.config.ModConfig;

public final class GemProfitTracker {
    private static final long RESET_DELAY_MS = 30_000L;

    private boolean tracking;
    private long sessionStartTime;
    private long lastGemTime;
    private double totalValue;
    private int pristineChance = 20;
    private boolean includeRough;
    private ModConfig.ProfitTrackerGemTier gemTier = ModConfig.ProfitTrackerGemTier.FLAWED;

    public void setPristineChance(int pristineChance) {
        this.pristineChance = Math.max(0, Math.min(100, pristineChance));
    }

    public void setIncludeRough(boolean includeRough) {
        this.includeRough = includeRough;
    }

    public void setGemTier(ModConfig.ProfitTrackerGemTier gemTier) {
        this.gemTier = gemTier != null ? gemTier : ModConfig.ProfitTrackerGemTier.FLAWED;
    }

    public void handlePristineMessage(String messageText, ProfitPriceLookup prices, long nowMillis) {
        ProfitTextParser.ParsedPristine pristine = ProfitTextParser.parsePristineMessage(messageText);
        if (pristine == null) return;

        if (!tracking) {
            sessionStartTime = nowMillis;
            tracking = true;
        }

        lastGemTime = nowMillis;
        double gemPrice = prices.getGemPrice(pristine.gemType(), gemTier.tier());
        double gemValue = (gemPrice / gemTier.divisor()) * pristine.amount();
        totalValue += gemValue;
    }

    public void tick(long nowMillis) {
        if (tracking && (nowMillis - lastGemTime) > RESET_DELAY_MS) {
            reset();
        }
    }

    public void reset() {
        tracking = false;
        sessionStartTime = 0L;
        lastGemTime = 0L;
        totalValue = 0D;
    }

    public GemProfitStats getStats(long nowMillis, ProfitPriceLookup prices) {
        if (!tracking) return null;

        long sessionTime = Math.max(0L, nowMillis - sessionStartTime);
        double hours = sessionTime / 3_600_000D;
        double baseCoinsPerHour = hours > 0 ? totalValue / hours : 0;
        double coinsPerHour = baseCoinsPerHour;

        if (includeRough && pristineChance > 0) {
            double roughMultiplier = (1 - (pristineChance / 100.0)) / (pristineChance / 100.0);
            double roughValue = baseCoinsPerHour / 80.0 * roughMultiplier;
            coinsPerHour += roughValue;
        }

        double flawlessPrice = prices.getGemPrice("RUBY", 3);
        double flawlessPerHour = flawlessPrice > 0 ? coinsPerHour / flawlessPrice : 0;

        return new GemProfitStats(
                sessionTime,
                totalValue,
                coinsPerHour,
                flawlessPerHour,
                includeRough,
                pristineChance,
                gemTier
        );
    }
}
