package top.babyzombie.addons.util.pet;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.babyzombie.addons.util.pet.state.PlayerPetState;
import top.babyzombie.addons.util.tracker.HypixelLocationTracker;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;

/**
 * Fetches current mayor and perk status from the Hypixel API.
 * Used to determine if Diana is mayor and which perks are active.
 *
 * Refresh strategy:
 * - Fetches once per hour between minutes 20-30 of each real-world hour
 * - Also respects SkyBlock day tracking to avoid redundant requests
 */
public final class MayorFetcher {
    private static final Logger LOGGER = LoggerFactory.getLogger("BabyzombieAddons/MayorFetcher");
    private static final String ELECTION_URL = "https://api.hypixel.net/resources/skyblock/election";
    private static final long MIN_REFRESH_MS = 3600_000; // 1 hour

    private static final MayorFetcher INSTANCE = new MayorFetcher();
    private PlayerPetState state;

    private MayorFetcher() {}

    public static MayorFetcher getInstance() { return INSTANCE; }

    public void init(PlayerPetState petState) {
        this.state = petState;
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

    /** Force a fetch (e.g. on profile load if data is stale). */
    public void fetchIfStale() {
        long now = System.currentTimeMillis();
        if (now - state.mayorLastCheckTime >= MIN_REFRESH_MS) {
            fetch();
        }
    }

    private void fetch() {
        try {
            HttpURLConnection conn = (HttpURLConnection) URI.create(ELECTION_URL).toURL().openConnection();
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            conn.setRequestProperty("Accept", "application/json");

            int code = conn.getResponseCode();
            if (code != 200) {
                LOGGER.warn("[MayorFetcher] HTTP {}", code);
                return;
            }

            JsonObject obj;
            try (var reader = new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8)) {
                obj = JsonParser.parseReader(reader).getAsJsonObject();
            }

            if (!"true".equals(obj.get("success").getAsString())) return;

            // Current mayor
            JsonElement mayorElem = obj.get("mayor");
            String mayorName = null;
            if (mayorElem != null && mayorElem.isJsonObject()) {
                JsonObject mayor = mayorElem.getAsJsonObject();
                mayorName = mayor.get("name").getAsString();
                state.dianaMayor = "Diana".equals(mayorName);

                // Mayor's perks
                state.dianaPetXpBuff = false;
                state.dianaSharingIsCaring = false;
                if (state.dianaMayor) {
                    JsonElement perks = mayor.get("perks");
                    if (perks != null && perks.isJsonArray()) {
                        for (JsonElement p : perks.getAsJsonArray()) {
                            if (!p.isJsonObject()) continue;
                            String perkName = p.getAsJsonObject().get("name").getAsString();
                            if ("Pet XP Buff".equals(perkName)) state.dianaPetXpBuff = true;
                            if ("Sharing is Caring".equals(perkName)) state.dianaSharingIsCaring = true;
                        }
                    }
                }

                // Check minister (deputy mayor) — nested inside mayor.minister
                JsonElement ministerElem = mayor.get("minister");
                if (ministerElem != null && ministerElem.isJsonObject()) {
                    JsonObject minister = ministerElem.getAsJsonObject();
                    String ministerName = minister.get("name").getAsString();
                    if ("Diana".equals(ministerName)) {
                        JsonElement perkElem = minister.get("perk");
                        if (perkElem != null && perkElem.isJsonObject()) {
                            String perkName = perkElem.getAsJsonObject().get("name").getAsString();
                            if ("Pet XP Buff".equals(perkName)) state.dianaPetXpBuff = true;
                            if ("Sharing is Caring".equals(perkName)) state.dianaSharingIsCaring = true;
                        }
                    }
                }
            }
            updateTimestamp();
        } catch (IOException e) {
            LOGGER.warn("[MayorFetcher] Fetch failed: {}", e.getMessage());
        }
    }

    private void updateTimestamp() {
        state.mayorLastCheckTime = System.currentTimeMillis();
        PetManager.getInstance().saveCurrentProfile();
    }
}
