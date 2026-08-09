package top.babyzombie.addons.util.tracker;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import top.babyzombie.addons.util.HttpUtils;

/**
 * 拉取并缓存 Hypixel Bazaar 全量数据（keyless 官方接口）。
 *
 * <p>数据源：{@code api.hypixel.net/v2/skyblock/bazaar}，无需 API key，服务端约 60s 刷新一次快照。
 * 全量 ~500KB（gzip），包含每个物品完整的 buy_summary / sell_summary 价位聚合桶。
 *
 * <p>刷新策略：60s 节流对齐官方缓存周期 + 服务端 {@code lastUpdated} 快照比对
 * （快照没变不重复解析覆盖）。拉取在虚拟线程异步执行，不阻塞渲染线程。
 *
 * <p>消费方（如 BazzarTopOrdersOverlay）需要数据时调用 {@link #ensureFresh()}，
 * 内部保证最多 60s 发起一次真实请求；之后直接读 {@link #getProduct(String)}。
 */
final class BazaarApiTracker {
    private static final String BAZAAR_URL = "https://api.hypixel.net/v2/skyblock/bazaar";
    /** 官方缓存周期 ~60s，对齐即可，不激进轮询 */
    private static final long REFRESH_INTERVAL_MS = 60_000;
    /** 磁盘缓存：config/babyzombieaddons/data/bazaar-cache.json（API 原始快照，避免反复开关游戏重复拉取） */
    private static final Path CACHE_FILE = FabricLoader.getInstance().getConfigDir()
            .resolve("babyzombieaddons").resolve("data").resolve("bazaar-cache.json");

    private static final BazaarApiTracker INSTANCE = new BazaarApiTracker();

    /** 当前缓存：productId -> 完整订单数据。volatile 原子换引用，读方无锁 */
    private volatile Map<String, ProductData> products = Map.of();
    /** 服务端快照时间（lastUpdated），-1 = 从未拉到 */
    private volatile long snapshotTs = -1;
    /** 上次发起拉取的墙钟时间（节流用） */
    private volatile long lastFetchMs = 0;
    private volatile boolean fetching = false;
    private boolean cacheLoaded = false;

    private BazaarApiTracker() {}

    public static BazaarApiTracker getInstance() { return INSTANCE; }

    /**
     * 节流 + 异步拉取。任何消费方在需要数据时调用即可：60s 内最多一次真实请求，
     * 已有新鲜缓存时直接返回。
     */
    public void ensureFresh() {
        if (fetching) return;
        if (System.currentTimeMillis() - lastFetchMs < REFRESH_INTERVAL_MS) return;
        // 首次查询：先落盘旧快照顶上（同步读，~几十 ms 一次性），异步拉取覆盖
        if (products.isEmpty() && !cacheLoaded) {
            cacheLoaded = loadCache();
        }
        fetching = true;
        Thread.startVirtualThread(this::fetchOnce);
    }

    private void fetchOnce() {
        try {
            HttpURLConnection conn = (HttpURLConnection) URI.create(BAZAAR_URL).toURL().openConnection();
            conn.setConnectTimeout(8000);
            conn.setReadTimeout(15000);
            conn.setRequestProperty("Accept", "application/json");
            conn.setRequestProperty("Accept-Encoding", "gzip");
            conn.setRequestProperty("User-Agent", HttpUtils.USER_AGENT);

            int code = conn.getResponseCode();
            if (code != 200) return;

            InputStream in = conn.getInputStream();
            if ("gzip".equalsIgnoreCase(conn.getContentEncoding())) {
                in = new GZIPInputStream(in);
            }
            String body;
            try (InputStream is = in) {
                body = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            }
            JsonObject root = JsonParser.parseString(body).getAsJsonObject();
            if (!"true".equals(root.get("success").getAsString())) return;

            long newTs = root.get("lastUpdated").getAsLong();
            // 服务端快照没变：不重复解析覆盖（价格未变化，界面也无需重渲染）
            if (newTs == snapshotTs) return;

            products = parseProducts(root.getAsJsonObject("products"));
            snapshotTs = newTs;
            saveCache(body);
        } catch (IOException | RuntimeException e) {
            // 静默失败，下一轮 ensureFresh 自然重试
        } finally {
            fetching = false;
            lastFetchMs = System.currentTimeMillis();
        }
    }

    /** 异步写磁盘缓存（原始 API body，下次启动直接读回，不重复拉取） */
    private static void saveCache(String body) {
        Thread.startVirtualThread(() -> {
            try {
                Files.createDirectories(CACHE_FILE.getParent());
                Files.writeString(CACHE_FILE, body,
                        StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            } catch (IOException ignored) {
            }
        });
    }

    /** 从磁盘缓存恢复上次快照；无缓存或解析失败返回 false */
    private boolean loadCache() {
        try {
            if (!Files.exists(CACHE_FILE)) return false;
            String raw = Files.readString(CACHE_FILE);
            JsonObject root = JsonParser.parseString(raw).getAsJsonObject();
            if (!"true".equals(root.get("success").getAsString())) return false;
            long ts = root.get("lastUpdated").getAsLong();
            if (ts <= 0) return false;
            products = parseProducts(root.getAsJsonObject("products"));
            snapshotTs = ts;
            return true;
        } catch (IOException | RuntimeException e) {
            return false;
        }
    }

    private static Map<String, ProductData> parseProducts(JsonObject productsObj) {
        Map<String, ProductData> map = new HashMap<>(productsObj.size() * 2);
        for (Map.Entry<String, JsonElement> e : productsObj.entrySet()) {
            JsonObject p = e.getValue().getAsJsonObject();
            JsonObject qs = p.getAsJsonObject("quick_status");
            map.put(e.getKey(), new ProductData(
                    e.getKey(),
                    parseSummary(p, "buy_summary"),
                    parseSummary(p, "sell_summary"),
                    qs != null ? getDouble(qs, "buyPrice") : 0,
                    qs != null ? getDouble(qs, "sellPrice") : 0,
                    qs != null ? getLong(qs, "buyVolume") : 0,
                    qs != null ? getLong(qs, "sellVolume") : 0,
                    qs != null ? getInt(qs, "buyOrders") : 0,
                    qs != null ? getInt(qs, "sellOrders") : 0));
        }
        return Map.copyOf(map);
    }

    private static double getDouble(JsonObject o, String key) {
        return o.has(key) ? o.get(key).getAsDouble() : 0;
    }

    private static long getLong(JsonObject o, String key) {
        return o.has(key) ? o.get(key).getAsLong() : 0;
    }

    private static int getInt(JsonObject o, String key) {
        return o.has(key) ? o.get(key).getAsInt() : 0;
    }

    private static List<BazaarItemInfo.SummaryTier> parseSummary(JsonObject product, String field) {
        JsonElement arr = product.get(field);
        if (arr == null || !arr.isJsonArray()) return List.of();
        List<BazaarItemInfo.SummaryTier> list = new ArrayList<>(arr.getAsJsonArray().size());
        for (JsonElement el : arr.getAsJsonArray()) {
            JsonObject tier = el.getAsJsonObject();
            list.add(new BazaarItemInfo.SummaryTier(
                    tier.get("amount").getAsLong(),
                    tier.get("pricePerUnit").getAsDouble(),
                    tier.get("orders").getAsInt()));
        }
        return List.copyOf(list);
    }

    /** @return 物品完整订单数据，未知 productId 返回 null */
    public ProductData getProduct(String productId) {
        return products.get(productId);
    }

    /** @return 是否已拉到过至少一次数据 */
    public boolean isReady() { return snapshotTs > 0; }

    /** @return 服务端快照时间（ms），-1 表示还没拉到 */
    public long getSnapshotTs() { return snapshotTs; }

    /** 单个物品的完整 bazaar 数据（quick_status + 订单阶梯） */
    public record ProductData(
            String productId,
            List<BazaarItemInfo.SummaryTier> buySummary,
            List<BazaarItemInfo.SummaryTier> sellSummary,
            double buyPrice,
            double sellPrice,
            long buyVolume,
            long sellVolume,
            int buyOrders,
            int sellOrders
    ) {}
}
