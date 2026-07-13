package top.babyzombie.addons.module.mining.profit;

public record MaterialProfitStats(
        String material,
        String displayName,
        long totalRawEquivalent,
        long totalEnchantedItems,
        double totalValue,
        double coinsPerHour,
        double enchantedPerHour,
        double collectionPerHour,
        long sessionTimeMs
) {}
