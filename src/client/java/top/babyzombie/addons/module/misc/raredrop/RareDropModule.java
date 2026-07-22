package top.babyzombie.addons.module.misc.raredrop;

import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.client.Minecraft;
import top.babyzombie.addons.event.PlaySoundEvents;
import top.babyzombie.addons.util.ChatUtils;
import top.babyzombie.addons.util.DataPersistence;
import top.babyzombie.addons.util.tracker.HypixelLocationTracker;
import top.babyzombie.addons.util.ServerTick;

import java.util.*;

/**
 * Rare drop filtering and auto-sharing.
 * Blacklist and share list (with per-item ac/pc/gc toggles) persisted to disk.
 */
public final class RareDropModule {

    private static final String DATA_FILE = "raredrop.json";
    private static final Map<String, Boolean> blacklist = new LinkedHashMap<>();
    private static final Map<String, ShareMode> shareList = new LinkedHashMap<>();
    private static long ignoreSoundTime;

    static {
        blacklist.put("potato", true);
        blacklist.put("carrot", true);
        blacklist.put("cropie", true);
        blacklist.put("squash", true);
        blacklist.put("compost", true);
        blacklist.put("tasty cheese", true);
        blacklist.put("enchanted bone", true);
        blacklist.put("enchanted spider eye", true);
        blacklist.put("enchanted ender pearl", true);

        shareList.put("phoenix", new ShareMode(false, false, false, false, false));
        shareList.put("scatha", new ShareMode(false, false, false, false, false));
        shareList.put("warden heart", new ShareMode(false, false, false, false, false));
        shareList.put("primordial eye", new ShareMode(false, false, false, false, false));
        shareList.put("overflux capacitor", new ShareMode(false, false, false, false, false));
        shareList.put("judgement core", new ShareMode(false, false, false, false, false));
        shareList.put("unfanged vampire part", new ShareMode(false, false, false, false, false));
        shareList.put("high class archfiend dice", new ShareMode(false, false, false, false, false));
    }

    private RareDropModule() {}

    public static void init() {
        loadLists();

        // Cancel blacklisted rare drop chat messages
        ClientReceiveMessageEvents.ALLOW_GAME.register((message, overlay) -> {
            if (overlay || !HypixelLocationTracker.getInstance().isInSkyblock()) return true;
            String text = ChatUtils.stripColor(message.getString());
            if (!text.matches("^(RARE|VERY RARE|CRAZY RARE|INSANE|PET) (DROP|CROP)!.*"))
                return true;

            String itemName = extractName(text);
            if (itemName == null) return true;

            if (blacklist.getOrDefault(itemName.toLowerCase(), false)) {
                ignoreSoundTime = ServerTick.getTime();
                return false; // Cancel the message
            }
            // Auto-share
            ShareMode mode = shareList.get(itemName.toLowerCase());
            if (mode != null) {
                String original = message.getString();
                if (mode.copy()) copyToClipboard(original);
                if (mode.ac()) ChatUtils.sendCommand("ac " + original);
                if (mode.pc()) ChatUtils.sendCommand("pc " + original);
                if (mode.gc()) ChatUtils.sendCommand("gc " + original);
                if (mode.cc()) ChatUtils.sendCommand("cc " + original);
            }
            return true;
        });

        // Suppress rare drop sounds within 1s of an ignored drop
        PlaySoundEvents.BEFORE_PLAY.register(sound -> {
            if (ignoreSoundTime == 0) return false;
            if (ServerTick.getTime() - ignoreSoundTime > 1000) {
                ignoreSoundTime = 0;
                return false;
            }
            var snd = sound.getSound();
            if (snd == null) return false;
            String path = snd.getLocation().getPath();
            float p = sound.getPitch();
            if (path.equals("note/pling")) {
                return Math.abs(p - 0.59f) < 0.01f || Math.abs(p - 0.79f) < 0.01f
                    || Math.abs(p - 1.05f) < 0.01f || Math.abs(p - 1.17f) < 0.01f;
            }
            if (path.equals("random/orb")) {
                return Math.abs(p - 0.70f) < 0.01f || Math.abs(p - 0.94f) < 0.01f
                    || Math.abs(p - 1.25f) < 0.01f || Math.abs(p - 1.41f) < 0.01f;
            }
            return false;
        });
    }

    // ---- accessors ----

    public static Map<String, Boolean> getBlacklist() { return blacklist; }
    public static Map<String, ShareMode> getShareList() { return shareList; }

    public static void addToBlacklist(String item) { blacklist.put(item.toLowerCase(), true); }
    public static void removeFromBlacklist(String item) { blacklist.remove(item.toLowerCase()); }
    public static void toggleBlacklist(String item) {
        blacklist.computeIfPresent(item, (k, v) -> !v);
    }

    public static void addToShareList(String item) {
        shareList.putIfAbsent(item.toLowerCase(), new ShareMode(false, false, false, false, false));
    }
    public static void removeFromShareList(String item) { shareList.remove(item.toLowerCase()); }
    public static void toggleShareMode(String item, char mode) {
        ShareMode sm = shareList.get(item);
        if (sm == null) return;
        shareList.put(item, switch (mode) {
            case 'a' -> new ShareMode(sm.copy, !sm.ac, sm.pc, sm.gc, sm.cc);
            case 'c' -> new ShareMode(!sm.copy, sm.ac, sm.pc, sm.gc, sm.cc);
            case 'p' -> new ShareMode(sm.copy, sm.ac, !sm.pc, sm.gc, sm.cc);
            case 'g' -> new ShareMode(sm.copy, sm.ac, sm.pc, !sm.gc, sm.cc);
            case 'o' -> new ShareMode(sm.copy, sm.ac, sm.pc, sm.gc, !sm.cc);
            default -> sm;
        });
    }

    // ---- persistence ----

    private static void loadLists() {
        try {
            var saved = DataPersistence.load(DATA_FILE, RareDropData.class);
            if (saved != null) {
                if (saved.blacklist != null) { blacklist.clear(); blacklist.putAll(saved.blacklist); }
                if (saved.share != null) { shareList.clear(); shareList.putAll(saved.share); }
            }
        } catch (Exception e) {
            // Migration: old format stored blacklist as array, convert to Map
            var legacy = DataPersistence.load(DATA_FILE, RareDropDataLegacy.class);
            if (legacy != null) {
                if (legacy.blacklist != null)
                    legacy.blacklist.forEach(item -> blacklist.put(item, true));
                if (legacy.share != null) { shareList.clear(); shareList.putAll(legacy.share); }
                saveLists(); // persist in new format
            }
        }
    }

    public static void saveLists() {
        DataPersistence.save(DATA_FILE, new RareDropData(
                new LinkedHashMap<>(blacklist), new LinkedHashMap<>(shareList)));
    }

    // ---- helpers ----

    private static String extractName(String text) {
        text = text.replaceAll(".*?(RARE|VERY RARE|CRAZY RARE|INSANE|PET) (DROP|CROP)!", "").trim();
        text = text.replaceAll("\\([^)]*Magic Find[^)]*\\)", "").trim();
        text = text.replaceAll("\\([^)]*[\uE01A✯✦\uE02B☀][^)]*\\)", "").trim();
        text = text.replaceAll("([0-9]+x )","").trim();
        if(text.startsWith("(") && text.endsWith(")")) text = text.substring(1, text.length() - 1).trim();
        return text.isEmpty() ? null : text;
    }

    private static void copyToClipboard(String text) {
        Minecraft.getInstance().keyboardHandler.setClipboard(text);
        ChatUtils.showTranslatable("babyzombieaddons.raredrop.copied");
    }

    public record ShareMode(boolean copy, boolean ac, boolean pc, boolean gc, boolean cc) {}

    private record RareDropData(Map<String, Boolean> blacklist, Map<String, ShareMode> share) {}
    private record RareDropDataLegacy(Set<String> blacklist, Map<String, ShareMode> share) {}
}
