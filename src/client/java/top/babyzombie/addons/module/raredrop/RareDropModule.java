package top.babyzombie.addons.module.raredrop;

import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.client.Minecraft;
import top.babyzombie.addons.event.PlaySoundEvents;
import top.babyzombie.addons.util.ChatUtils;
import top.babyzombie.addons.util.DataPersistence;
import top.babyzombie.addons.util.HypixelLocationTracker;
import top.babyzombie.addons.util.ServerTick;

import java.util.*;

/**
 * Rare drop filtering and auto-sharing.
 * Blacklist and share list (with per-item ac/pc/gc toggles) persisted to disk.
 */
public final class RareDropModule {

    private static final String DATA_FILE = "raredrop.json";
    private static final Set<String> blacklist = new LinkedHashSet<>();
    private static final Map<String, ShareMode> shareList = new LinkedHashMap<>();
    private static long ignoreSoundTime;

    static {
        blacklist.add("potato");
        blacklist.add("carrot");
        blacklist.add("cropie");
        blacklist.add("squash");
        blacklist.add("compost");
        blacklist.add("tasty cheese");
        blacklist.add("enchanted bone");
        blacklist.add("enchanted ender pearl");

        shareList.put("phoenix", new ShareMode(false, false, false, false, false));
        shareList.put("warden heart", new ShareMode(false, false, false, false, false));
        shareList.put("overflux capacitor", new ShareMode(false, false, false, false, false));
        shareList.put("judgement core", new ShareMode(false, false, false, false, false));
    }

    private RareDropModule() {}

    public static void init() {
        loadLists();

        // Cancel blacklisted rare drop chat messages
        ClientReceiveMessageEvents.ALLOW_GAME.register((message, overlay) -> {
            if (overlay || !HypixelLocationTracker.getInstance().isInSkyblock()) return true;
            String text = ChatUtils.stripColor(message.getString());
            if (!text.matches(".*(RARE|VERY RARE|CRAZY RARE|INSANE|PET) (DROP|CROP)!.*"))
                return true;

            String itemName = extractName(text);
            if (itemName == null) return true;

            if (blacklist.contains(itemName.toLowerCase())) {
                ignoreSoundTime = ServerTick.getTime();
                return false; // Cancel the message
            }
            // Auto-share
            ShareMode mode = shareList.get(itemName.toLowerCase());
            if (mode != null) {
                String original = message.getString();
                if (mode.copy()) copyToClipboard(original);
                if (mode.ac()) ChatUtils.sendMessage(original);
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
            String id = "?";
            try { var loc = sound.getIdentifier(); id = loc != null ? loc.toString() : "?"; } catch (Exception ignored) {}
            return id.contains("note_block") || id.contains("experience_orb");
        });
    }

    // ---- accessors ----

    public static Set<String> getBlacklist() { return blacklist; }
    public static Map<String, ShareMode> getShareList() { return shareList; }

    public static void addToBlacklist(String item) { blacklist.add(item.toLowerCase()); }
    public static void removeFromBlacklist(String item) { blacklist.remove(item.toLowerCase()); }

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
        var saved = DataPersistence.load(DATA_FILE, RareDropData.class);
        if (saved != null) {
            if (saved.blacklist != null) { blacklist.clear(); blacklist.addAll(saved.blacklist); }
            if (saved.share != null) { shareList.clear(); shareList.putAll(saved.share); }
        }
    }

    public static void saveLists() {
        DataPersistence.save(DATA_FILE, new RareDropData(
                new LinkedHashSet<>(blacklist), new LinkedHashMap<>(shareList)));
    }

    // ---- helpers ----

    private static String extractName(String text) {
        text = text.replaceAll(".*?(RARE|VERY RARE|CRAZY RARE|INSANE|PET) (DROP|CROP)!", "").trim();
        text = text.replaceAll("\\(.*Magic Find.*\\)", "").trim();
        text = text.replaceAll("\\(.*[✯✦].*\\)", "").trim();
        return text.isEmpty() ? null : text;
    }

    private static void copyToClipboard(String text) {
        Minecraft.getInstance().keyboardHandler.setClipboard(text);
        var p = Minecraft.getInstance().player;
        if (p != null) p.displayClientMessage(
                net.minecraft.network.chat.Component.literal("§6§l§aCopied to clipboard"), false);
    }

    public record ShareMode(boolean copy, boolean ac, boolean pc, boolean gc, boolean cc) {}

    private record RareDropData(Set<String> blacklist, Map<String, ShareMode> share) {}
}
