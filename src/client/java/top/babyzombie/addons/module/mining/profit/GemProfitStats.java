package top.babyzombie.addons.module.mining.profit;

import top.babyzombie.addons.config.ModConfig;

public record GemProfitStats(
        long sessionTimeMs,
        double totalValue,
        double coinsPerHour,
        double flawlessPerHour,
        boolean includeRough,
        int pristineChance,
        ModConfig.ProfitTrackerGemTier gemTier
) {}
