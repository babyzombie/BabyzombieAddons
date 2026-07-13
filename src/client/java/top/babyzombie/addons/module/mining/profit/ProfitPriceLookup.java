package top.babyzombie.addons.module.mining.profit;

public interface ProfitPriceLookup {
    double getGemPrice(String gemType, int tier);

    double getBlockPrice(String itemId);

    boolean isUsingNpcPrices();
}
