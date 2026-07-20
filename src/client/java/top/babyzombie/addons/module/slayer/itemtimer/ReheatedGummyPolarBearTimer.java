package top.babyzombie.addons.module.slayer.itemtimer;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;
import top.babyzombie.addons.config.ModConfig;
import top.babyzombie.addons.config.ModConfigManager;
import top.babyzombie.addons.util.ChatUtils;
import top.babyzombie.addons.util.tracker.HypixelLocationTracker;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * Tracks Re-heated Gummy Polar Bear duration with per-profile persistence.
 * Alerts at 5min, 2min, 1min remaining and at expiration.
 */
public final class ReheatedGummyPolarBearTimer {
    private static final Gson GSON = new Gson();
    private static final Path SAVE_FILE = FabricLoader.getInstance().getConfigDir()
            .resolve("babyzombieaddons").resolve("ReheatedGummyPolarBear.json");

    static final Map<String, Integer> profileTimers = new HashMap<>();
    private static boolean alerted5min, alerted2min, alerted1min;

    private ReheatedGummyPolarBearTimer() {}

    public static void init() {
        load();

        ClientReceiveMessageEvents.ALLOW_GAME.register((message, overlay) -> {
            if (overlay) return true;
            if (!HypixelLocationTracker.getInstance().isInSkyblock()) return true;

            String text = ChatUtils.stripColor(message.getString()).trim();
            if (text.startsWith("You ate a Re-heated Gummy Polar Bear")) {
                String profileId = HypixelLocationTracker.getInstance().getProfileId();
                if (profileId != null) {
                    profileTimers.put(profileId, profileTimers.getOrDefault(profileId, 0) + 3600 * 20); // Add 60 minutes
                    alerted5min = false; alerted2min = false; alerted1min = false;
                    save();
                }
            }
            return true;
        });

        // Tick
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            var tracker = HypixelLocationTracker.getInstance();
            if (!tracker.isInSkyblock()) return;
            if (tracker.isInDungeon() || tracker.isInRift() || tracker.isInSafari()) return;

            String profileId = tracker.getProfileId();
            if (profileId == null) return;
            Integer remaining = profileTimers.get(profileId);
            if (remaining == null || remaining <= 0) return;

            remaining--;
            profileTimers.put(profileId, remaining);

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
                    profileTimers.remove(profileId);
                    alerted5min = false; alerted2min = false; alerted1min = false;
                }
            }
            if (remaining <= 0) profileTimers.remove(profileId);
            save();
        });
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

    public static String getTimeString(String profileId) {
        Integer remaining = profileTimers.get(profileId);
        if (remaining == null || remaining <= 0) return "";
        int m = remaining / 20 / 60;
        int s = remaining / 20 % 60;
        return String.format("%02d:%02d", m, s);
    }

    private static void load() {
        if (!Files.exists(SAVE_FILE)) return;
        try {
            String json = Files.readString(SAVE_FILE);
            Map<String, Integer> saved = GSON.fromJson(json, new TypeToken<Map<String, Integer>>(){}.getType());
            if (saved != null) profileTimers.putAll(saved);
        } catch (IOException ignored) {}
    }

    private static void save() {
        try {
            Files.createDirectories(SAVE_FILE.getParent());
            Files.writeString(SAVE_FILE, GSON.toJson(profileTimers));
        } catch (IOException ignored) {}
    }
}
