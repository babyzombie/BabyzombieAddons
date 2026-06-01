package top.babyzombie.addons.module.sack;

import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.client.Minecraft;
import top.babyzombie.addons.config.HudManager;
import top.babyzombie.addons.config.ModConfigManager;
import top.babyzombie.addons.util.ChatUtils;
import top.babyzombie.addons.util.DataPersistence;
import top.babyzombie.addons.util.HypixelLocationTracker;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Sack item tracking with persisted HUD display.
 */

public final class SackModule {

    private static final Pattern ADD = Pattern.compile("\\+(\\d+) (.+) \\(Sack\\)");
    private static final Pattern REMOVE = Pattern.compile("-(\\d+) (.+) \\(Sack\\)");
    private static final String DATA_FILE = "sack.json";

    private static final Map<String, Integer> items = new HashMap<>();
    private static final Set<String> displayItems = new HashSet<>();

    private SackModule() {}

    public static void init() {
        if (!ModConfigManager.get().misc.sackItemHUD) return;

        load();

        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            if (overlay) return;
            String text = ChatUtils.stripColor(message.getString());

            Matcher m = ADD.matcher(text);
            if (m.find()) {
                items.merge(m.group(2).trim(), Integer.parseInt(m.group(1)), Integer::sum);
                save();
                return;
            }
            m = REMOVE.matcher(text);
            if (m.find()) {
                items.merge(m.group(2).trim(), -Integer.parseInt(m.group(1)), Integer::sum);
                if (items.getOrDefault(m.group(2).trim(), 0) <= 0) items.remove(m.group(2).trim());
                save();
            }
        });

        HudRenderCallback.EVENT.register((gui, delta) -> {
            if (!HypixelLocationTracker.getInstance().isInSkyblock() || displayItems.isEmpty()) return;
            var font = Minecraft.getInstance().font;
            int x = HudManager.x("SackItems"), y = HudManager.y("SackItems");
            gui.drawString(font, "§6§lSack Items:", x, y, 0xFFFFFFFF, true);
            int dy = y + 12;
            for (String item : displayItems) {
                int c = items.getOrDefault(item, 0);
                if (c > 0) { gui.drawString(font, "§7" + item + ": §f" + c, x, dy, 0xFFFFFFFF, true); dy += 10; }
            }
        });
    }

    public static void toggleDisplayItem(String name) {
        if (displayItems.contains(name)) displayItems.remove(name);
        else displayItems.add(name);
        save();
    }

    @SuppressWarnings("unchecked")
    private static void load() {
        var saved = DataPersistence.load(DATA_FILE, SackData.class);
        if (saved != null) {
            if (saved.items != null) items.putAll(saved.items);
            if (saved.display != null) displayItems.addAll(saved.display);
        }
    }

    private static void save() {
        DataPersistence.save(DATA_FILE, new SackData(new HashMap<>(items), new HashSet<>(displayItems)));
    }

    private record SackData(Map<String, Integer> items, Set<String> display) {}
}
