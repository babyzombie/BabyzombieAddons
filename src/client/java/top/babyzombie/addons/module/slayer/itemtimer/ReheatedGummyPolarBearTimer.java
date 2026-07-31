package top.babyzombie.addons.module.slayer.itemtimer;

import com.google.gson.reflect.TypeToken;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;
import top.babyzombie.addons.config.ModConfig;
import top.babyzombie.addons.config.ModConfigManager;
import top.babyzombie.addons.util.ChatUtils;
import top.babyzombie.addons.util.DataPersistence;
import top.babyzombie.addons.util.tracker.HypixelLocationTracker;

import java.util.HashMap;
import java.util.Map;

/**
 * Tracks Re-heated Gummy Polar Bear duration with per-profile persistence.
 * Alerts at 5min, 2min, 1min remaining and at expiration.
 * Persisted as data/reheated_gummy_polar_bear.json, keyed by "uuid_profileId".
 */
public final class ReheatedGummyPolarBearTimer {
    static final Map<String, Integer> profileTimers = new HashMap<>();
    private static boolean alerted5min, alerted2min, alerted1min;

    private ReheatedGummyPolarBearTimer() {}

    public static void init() {
        load();

        ClientReceiveMessageEvents.ALLOW_GAME.register((message, overlay) -> {
            if (overlay) return true;
            var tracker = HypixelLocationTracker.getInstance();
            if (!tracker.isInSkyblock() || tracker.isInAlpha()) return true;

            String text = ChatUtils.stripColor(message.getString()).trim();
            if (text.startsWith("You ate a Re-heated Gummy Polar Bear")) {
                String key = profileKey();
                if (key != null) {
                    profileTimers.put(key, profileTimers.getOrDefault(key, 0) + 3600 * 20); // Add 60 minutes
                    alerted5min = false; alerted2min = false; alerted1min = false;
                    save();
                }
            }
            return true;
        });

        // Tick
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            var tracker = HypixelLocationTracker.getInstance();
            if (!tracker.isInSkyblock() || tracker.isInAlpha()) return;
            if (tracker.isInDungeon() || tracker.isInRift() || tracker.isInSafari()) return;

            String key = profileKey();
            if (key == null) return;
            Integer remaining = profileTimers.get(key);
            if (remaining == null || remaining <= 0) return;

            remaining--;
            profileTimers.put(key, remaining);

            var cfg = ModConfigManager.get().slayer;

            // Only show in Smoldering Tomb if mode == 1
            if (cfg.itemSkillTimers.reheatedGummyPolarBear == ModConfig.GummyPolarBearMode.EVERYWHERE_EXCEPT_DUNGEON
                    || "Smoldering Tomb".equals(tracker.getLocation())) switch (remaining) {
                case 300 * 20 -> {
                    if (!alerted5min) {
                        alerted5min = true;
                        ChatUtils.showTranslatableTitle("", "slayer.gummybear.5min", 0, 50, 10);
                        playSound();
                    }
                }
                case 120 * 20 -> {
                    if (!alerted2min) {
                        alerted2min = true;
                        ChatUtils.showTranslatableTitle("", "slayer.gummybear.2min", 0, 50, 10);
                        playSound();
                    }
                }
                case 60 * 20 -> {
                    if (!alerted1min) {
                        alerted1min = true;
                        ChatUtils.showTranslatableTitle("", "slayer.gummybear.1min", 0, 50, 10);
                        playSound();
                    }
                }
                case 0 -> {
                    ChatUtils.showTranslatableTitle("", "slayer.gummybear.expired", 0, 50, 10);
                    playAnvilSound();
                    profileTimers.remove(key);
                    alerted5min = false; alerted2min = false; alerted1min = false;
                }
            }
            if (remaining <= 0) profileTimers.remove(key);
            save();
        });
    }

    /** 文件内细分 key:uuid + "_" + profileId。 */
    private static String profileKey() {
        var tracker = HypixelLocationTracker.getInstance();
        String uuid = tracker.getUuid();
        String profileId = tracker.getProfileId();
        if (uuid == null || profileId == null) return null;
        return uuid + "_" + profileId;
    }

    private static void playSound() {
        var client = Minecraft.getInstance();
        if (client.player != null) {
            client.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.EXPERIENCE_ORB_PICKUP, 1.0F, 1.0F));
        }
    }

    private static void playAnvilSound() {
        var client = Minecraft.getInstance();
        if (client.player != null) {
            client.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.ANVIL_BREAK, 1.0F, 1.0F));
        }
    }

    public static String getTimeString(String key) {
        Integer remaining = profileTimers.get(key);
        if (remaining == null || remaining <= 0) return "";
        int m = remaining / 20 / 60;
        int s = remaining / 20 % 60;
        return String.format("%02d:%02d", m, s);
    }

    private static void load() {
        Map<String, Integer> saved = DataPersistence.load("reheated_gummy_polar_bear.json",
                new TypeToken<Map<String, Integer>>(){}.getType());
        if (saved != null) profileTimers.putAll(saved);
    }

    private static void save() {
        DataPersistence.save("reheated_gummy_polar_bear.json", profileTimers);
    }
}
