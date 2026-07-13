package top.babyzombie.addons.module.mining.profit;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

public final class ProfitPriceService implements ProfitPriceLookup {
    private static final Logger LOGGER = LoggerFactory.getLogger("BZA-ProfitPriceService");
    private static final String BAZAAR_API = "https://api.hypixel.net/skyblock/bazaar";
    private static final long CACHE_DURATION_MS = 5 * 60 * 1000L;

    private static final Map<String, Double> BLOCK_NPC_PRICES = Map.ofEntries(
            Map.entry("ENCHANTED_COAL", 320.0),
            Map.entry("ENCHANTED_DIAMOND", 1280.0),
            Map.entry("ENCHANTED_GOLD", 640.0),
            Map.entry("ENCHANTED_IRON", 480.0),
            Map.entry("ENCHANTED_REDSTONE", 480.0),
            Map.entry("ENCHANTED_LAPIS_LAZULI", 480.0),
            Map.entry("ENCHANTED_OBSIDIAN", 1280.0),
            Map.entry("ENCHANTED_QUARTZ", 640.0),
            Map.entry("ENCHANTED_EMERALD", 800.0),
            Map.entry("ENCHANTED_GLOWSTONE", 640.0),
            Map.entry("ENCHANTED_HARD_STONE", 160.0),
            Map.entry("ENCHANTED_MITHRIL", 160.0),
            Map.entry("ENCHANTED_TITANIUM", 2560.0),
            Map.entry("ENCHANTED_SULPHUR", 480.0),
            Map.entry("ENCHANTED_UMBER", 480.0),
            Map.entry("ENCHANTED_MYCELIUM", 800.0),
            Map.entry("ENCHANTED_RED_SAND", 800.0)
    );

    private final Map<String, Double> gemPrices = new ConcurrentHashMap<>();
    private final Map<String, Double> blockPrices = new ConcurrentHashMap<>();
    private final AtomicBoolean fetchInProgress = new AtomicBoolean(false);

    private volatile long lastUpdate;
    private volatile boolean useNpcPrices;

    public void setUseNpcPrices(boolean useNpcPrices) {
        this.useNpcPrices = useNpcPrices;
    }

    @Override
    public boolean isUsingNpcPrices() {
        return useNpcPrices;
    }

    @Override
    public double getGemPrice(String gemType, int tier) {
        if (useNpcPrices) {
            return getGemNpcPrice(tier);
        }

        String itemId = getTierPrefix(tier) + "_" + gemType.toUpperCase() + "_GEM";
        double bazaarPrice = gemPrices.getOrDefault(itemId, 0.0);
        return Math.max(bazaarPrice, getGemNpcPrice(tier));
    }

    @Override
    public double getBlockPrice(String itemId) {
        if (useNpcPrices) {
            return BLOCK_NPC_PRICES.getOrDefault(itemId, 0.0);
        }
        return blockPrices.getOrDefault(itemId, 0.0);
    }

    public void updatePricesAsync() {
        if (useNpcPrices) return;
        if ((System.currentTimeMillis() - lastUpdate) < CACHE_DURATION_MS) return;
        if (!fetchInProgress.compareAndSet(false, true)) return;

        CompletableFuture.runAsync(() -> {
            try {
                JsonObject products = fetchProducts();
                if (products == null) return;
                updateGemPrices(products);
                updateBlockPrices(products);
                lastUpdate = System.currentTimeMillis();
            } catch (Exception e) {
                LOGGER.warn("Failed to refresh profit prices", e);
            } finally {
                fetchInProgress.set(false);
            }
        });
    }

    private JsonObject fetchProducts() throws Exception {
        HttpURLConnection conn = (HttpURLConnection) URI.create(BAZAAR_API).toURL().openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(5000);
        conn.setReadTimeout(5000);

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
            JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
            return json.getAsJsonObject("products");
        }
    }

    private void updateGemPrices(JsonObject products) {
        for (Map.Entry<String, com.google.gson.JsonElement> entry : products.entrySet()) {
            String itemId = entry.getKey();
            if (!isGemItem(itemId)) continue;

            JsonObject quickStatus = entry.getValue().getAsJsonObject().getAsJsonObject("quick_status");
            if (quickStatus == null || !quickStatus.has("sellPrice")) continue;

            int tier = getTierFromItemId(itemId);
            double sellPrice = quickStatus.get("sellPrice").getAsDouble();
            gemPrices.put(itemId, Math.max(sellPrice, getGemNpcPrice(tier)));
        }
    }

    private void updateBlockPrices(JsonObject products) {
        for (String itemId : BLOCK_NPC_PRICES.keySet()) {
            if (!products.has(itemId)) continue;
            JsonObject quickStatus = products.getAsJsonObject(itemId).getAsJsonObject("quick_status");
            if (quickStatus == null || !quickStatus.has("buyPrice")) continue;
            blockPrices.put(itemId, quickStatus.get("buyPrice").getAsDouble());
        }
    }

    private static double getGemNpcPrice(int tier) {
        return 3 * Math.pow(80, tier);
    }

    private static String getTierPrefix(int tier) {
        return switch (tier) {
            case 0 -> "ROUGH";
            case 1 -> "FLAWED";
            case 2 -> "FINE";
            case 3 -> "FLAWLESS";
            case 4 -> "PERFECT";
            default -> "FLAWED";
        };
    }

    private static boolean isGemItem(String itemId) {
        return (itemId.startsWith("ROUGH_")
                || itemId.startsWith("FLAWED_")
                || itemId.startsWith("FINE_")
                || itemId.startsWith("FLAWLESS_")
                || itemId.startsWith("PERFECT_"))
                && itemId.endsWith("_GEM");
    }

    private static int getTierFromItemId(String itemId) {
        if (itemId.startsWith("ROUGH_")) return 0;
        if (itemId.startsWith("FLAWED_")) return 1;
        if (itemId.startsWith("FINE_")) return 2;
        if (itemId.startsWith("FLAWLESS_")) return 3;
        if (itemId.startsWith("PERFECT_")) return 4;
        return 1;
    }
}
