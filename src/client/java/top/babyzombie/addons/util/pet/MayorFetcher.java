package top.babyzombie.addons.util.pet;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import top.babyzombie.addons.util.HttpUtils;
import net.minecraft.client.Minecraft;
import top.babyzombie.addons.util.pet.state.PlayerPetState;
import top.babyzombie.addons.util.tracker.HypixelLocationTracker;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * Fetches current mayor and perk status.
 *
 * <p>Two strategies:
 * <ul>
 *   <li><b>Skyblocker path</b> — if Skyblocker is installed, listen to
 *       {@code SkyblockEvents.MAYOR_CHANGE} and read {@code MayorUtils.getActivePerks()}</li>
 *   <li><b>Fallback path</b> — poll {@code api.hypixel.net/resources/skyblock/election}
 *       once per hour (free public endpoint, no API key)</li>
 * </ul>
 *
 * Used to determine if Diana is mayor and which perks are active.
 */
public final class MayorFetcher {
    private static final String ELECTION_URL = "https://api.hypixel.net/resources/skyblock/election";
    private static final long MIN_REFRESH_MS = 3600_000; // 1 hour

    private static final MayorFetcher INSTANCE = new MayorFetcher();
    private PlayerPetState state;
    private boolean useSkyblocker;

    private MayorFetcher() {}

    public static MayorFetcher getInstance() { return INSTANCE; }

    public void init(PlayerPetState petState) {
        this.state = petState;
        if (tryInitSkyblocker()) return;
        initFallback();
    }

    /** Force a refresh (e.g. on profile load if data is stale). */
    public void fetchIfStale() {
        if (useSkyblocker) {
            syncFromSkyblocker();
            return;
        }
        long now = System.currentTimeMillis();
        if (now - state.mayorLastCheckTime >= MIN_REFRESH_MS) {
            fetch();
        }
    }

    // ===== Skyblocker path =====

    private boolean tryInitSkyblocker() {
        try {
            if (!FabricLoader.getInstance().isModLoaded("skyblocker")) return false;
            useSkyblocker = true;

            de.hysky.skyblocker.events.SkyblockEvents.MAYOR_CHANGE.register(this::onMayorChange);
            syncFromSkyblocker();
            return true;
        } catch (NoClassDefFoundError | Exception e) {
            useSkyblocker = false;
            return false;
        }
    }

    private void onMayorChange() {
        syncFromSkyblocker();
    }

    private void syncFromSkyblocker() {
        try {
            // NPE guard: MayorUtils may not have loaded data yet
            List<String> perks = de.hysky.skyblocker.utils.mayor.MayorUtils.getActivePerks();
            if (perks == null) return;

            boolean petXpBuff = perks.contains("Pet XP Buff");
            boolean sharingIsCaring = perks.contains("Sharing is Caring");

            // Only save if something actually changed
            if (state.dianaPetXpBuff == petXpBuff
                    && state.dianaSharingIsCaring == sharingIsCaring) return;

            state.dianaPetXpBuff = petXpBuff;
            state.dianaSharingIsCaring = sharingIsCaring;
            state.dianaMayor = petXpBuff || sharingIsCaring;
            state.mayorLastCheckTime = System.currentTimeMillis();
            PetManager.getInstance().saveCurrentProfile();
        } catch (Exception e) {
        }
    }

    // ===== Fallback path =====

    private void initFallback() {
        ClientTickEvents.END_CLIENT_TICK.register(this::onTick);
    }

    private void onTick(net.minecraft.client.Minecraft client) {
        if (client.player == null) return;
        if (!HypixelLocationTracker.getInstance().isInSkyblock()) return;

        // Check every 1200 ticks (~60s)
        if (client.player.tickCount % 1200 != 0) return;

        long now = System.currentTimeMillis();
        if (now - state.mayorLastCheckTime < MIN_REFRESH_MS) return;

        fetch();
    }

    private void fetch() {
        // 异步 HTTP：避免阻塞渲染线程（connect+read 最坏各 5s，会整机冻结）
        CompletableFuture.runAsync(() -> {
            boolean dianaMayor = false;
            boolean petXpBuff = false;
            boolean sharingIsCaring = false;
            try {
                HttpURLConnection conn = (HttpURLConnection) URI.create(ELECTION_URL).toURL().openConnection();
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);
                conn.setRequestProperty("Accept", "application/json");
                conn.setRequestProperty("User-Agent", HttpUtils.USER_AGENT);
                try {
                    int code = conn.getResponseCode();
                    if (code != 200) return;

                    JsonObject obj;
                    try (var reader = new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8)) {
                        obj = JsonParser.parseReader(reader).getAsJsonObject();
                    }

                    if (!"true".equals(obj.get("success").getAsString())) return;

                    // Current mayor
                    JsonElement mayorElem = obj.get("mayor");
                    if (mayorElem != null && mayorElem.isJsonObject()) {
                        JsonObject mayor = mayorElem.getAsJsonObject();
                        dianaMayor = "Diana".equals(mayor.get("name").getAsString());

                        // Mayor's perks
                        if (dianaMayor) {
                            JsonElement perks = mayor.get("perks");
                            if (perks != null && perks.isJsonArray()) {
                                for (JsonElement p : perks.getAsJsonArray()) {
                                    if (!p.isJsonObject()) continue;
                                    String perkName = p.getAsJsonObject().get("name").getAsString();
                                    if ("Pet XP Buff".equals(perkName)) petXpBuff = true;
                                    if ("Sharing is Caring".equals(perkName)) sharingIsCaring = true;
                                }
                            }
                        }

                        // Check minister (deputy mayor) — nested inside mayor.minister
                        JsonElement ministerElem = mayor.get("minister");
                        if (ministerElem != null && ministerElem.isJsonObject()) {
                            JsonObject minister = ministerElem.getAsJsonObject();
                            if ("Diana".equals(minister.get("name").getAsString())) {
                                JsonElement perkElem = minister.get("perk");
                                if (perkElem != null && perkElem.isJsonObject()) {
                                    String perkName = perkElem.getAsJsonObject().get("name").getAsString();
                                    if ("Pet XP Buff".equals(perkName)) petXpBuff = true;
                                    if ("Sharing is Caring".equals(perkName)) sharingIsCaring = true;
                                }
                            }
                        }
                    }
                } finally {
                    conn.disconnect();
                }

                // 回主线程写状态（PlayerPetState 在 tick 中被读，避免跨线程竞争）
                boolean fMayor = dianaMayor, fPet = petXpBuff, fSharing = sharingIsCaring;
                Minecraft.getInstance().execute(() -> {
                    state.dianaMayor = fMayor;
                    state.dianaPetXpBuff = fPet;
                    state.dianaSharingIsCaring = fSharing;
                    state.mayorLastCheckTime = System.currentTimeMillis();
                    PetManager.getInstance().saveCurrentProfile();
                });
            } catch (IOException e) {
            }
        });
    }
}
