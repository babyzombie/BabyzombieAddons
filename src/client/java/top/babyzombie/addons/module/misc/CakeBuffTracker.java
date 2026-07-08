package top.babyzombie.addons.module.misc;

import java.util.LinkedHashMap;
import java.util.Map;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLevelEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import top.babyzombie.addons.config.ModConfigManager;
import top.babyzombie.addons.config.hud.HudManager;
import top.babyzombie.addons.util.ChatUtils;
import top.babyzombie.addons.util.ServerTick;
import top.babyzombie.addons.util.tracker.HypixelLocationTracker;

public final class CakeBuffTracker {

    private static final Map<String, Integer> CAKE_INDEX = new LinkedHashMap<>();
    private static final String[] NAMES;
    static {
        // PUA icons from Hypixel official Skyblock resource pack (assets/minecraft/font/default.json)
        // \uE010=health, \uE008=defense, \uE00D=strength, \uE022=speed,
        // \uE003=intelligence, \uE00B=ferocity, \uE028=vitality, \uE027=true defense,
        // \uE021=sea creature chance, \uE01A=magic find, \uE013=pet luck,
        // \uE006=cold resistance, \uE020=rift time,
        // \uE053=mining fortune, \uE051=farming fortune, \uE054=foraging fortune,
        // \uE025=treasure chance, \uE077=tracking, \uE023=sweep,
        // \uE05B=hunter fortune
        Object[][] data = {
                {"10\uE010 Health", "§c10\uE010 Health   "},
                {"3\uE008 Defense", "§a3\uE008 Defense   "},
                {"2\uE00D Strength", "§c2\uE00D Strength   "},
                {"10\uE022 Speed", "§f10\uE022 Speed   "},
                {"5\uE003 Intelligence", "§b5\uE003 Intelligence   "},
                {"2\uE00B Ferocity", "§c2\uE00B Ferocity   "},
                {"1\uE028 Vitality", "§41\uE028 Vitality   "},
                {"1\uE027 True Defense", "§f1\uE027 True Defense   "},
                {"1\uE021 Sea Creature Chance", "§31\uE021 Sea Creature Chance   "},
                {"1\uE01A Magic Find", "§b1\uE01A Magic Find   "},
                {"1\uE013 Pet Luck", "§d1\uE013 Pet Luck   "},
                {"1\uE006 Cold Resistance", "§b1\uE006 Cold Resistance   "},
                {"10\uE020 Rift Time", "§a10\uE020 Rift Time   "},
                {"5\uE053 Mining Fortune", "§65\uE053 Mining Fortune   "},
                {"5\uE051 Farming Fortune", "§65\uE051 Farming Fortune   "},
                {"5\uE054 Foraging Fortune", "§65\uE054 Foraging Fortune   "},
                {"1\uE025 Treasure Chance", "§61\uE025 Treasure Chance   "},
                {"1\uE077 Tracking", "§d1\uE077 Tracking   "},
                {"5\uE023 Sweep", "§25\uE023 Sweep   "},
                {"1\uE05B Hunter Fortune", "§d1\uE05B Hunter Fortune   "}
        };
        NAMES = new String[data.length];
        for (int i = 0; i < data.length; i++) {
            CAKE_INDEX.put((String) data[i][0], i);
            NAMES[i] = (String) data[i][1];
        }
    }

    private static final boolean[] found = new boolean[CAKE_INDEX.size()];
    private static long lastEatTime;
    private static String checklistText;

    private CakeBuffTracker() {}

    public static void init() {
        // Cancel the original Hypixel cake message
        ClientReceiveMessageEvents.ALLOW_GAME.register((message, overlay) -> {
            if (overlay) return true;
            if (!ModConfigManager.get().general.cakeBuffTracker) return true;
            var tracker = HypixelLocationTracker.getInstance();
            if (!tracker.isIn("Private Island")) return true;

            String text = ChatUtils.stripColor(message.getString());
            if (text.startsWith("Yum! You gain +") || text.startsWith("Big Yum! You refresh +")) {
                if (text.endsWith(" for 48 hours!")) {
                    onCakeEaten(text);
                    return false; // Cancel original message
                }
            }
            return true;
        });

        // Reset timer on world change
        ClientLevelEvents.AFTER_CLIENT_LEVEL_CHANGE.register((client, world) -> {
            lastEatTime = 0;
        });

        // HUD rendering — shows for 60 seconds after eating a cake
        HudElementRegistry.attachElementAfter(VanillaHudElements.OVERLAY_MESSAGE,
                Identifier.fromNamespaceAndPath("babyzombieaddons", "cake_buff_tracker"),
                (context, tickCounter) -> {
            if (!ModConfigManager.get().general.cakeBuffTracker) return;
            if (checklistText == null) return;
            long elapsed = ServerTick.getTime() - lastEatTime;
            if (elapsed > 30_000) {
                checklistText = null;
                return;
            }

            var font = Minecraft.getInstance().font;
            int x = HudManager.x("CakeBuffTracker");
            int y = HudManager.y("CakeBuffTracker");
            float s = HudManager.scale("CakeBuffTracker");
            HudManager.drawScaled(context, font, checklistText, x, y, s);
        });
    }

    private static void onCakeEaten(String text) {
        String cakeName = text
            .replace("Yum! You gain +", "")
            .replace("Big Yum! You refresh +", "")
            .replace(" for 48 hours!", "");

        if (cakeName.equals(text)) return;
        Integer idx = CAKE_INDEX.get(cakeName);
        if (idx == null) return;

        found[idx] = true;
        lastEatTime = ServerTick.getTime();
        checklistText = buildChecklist();

        if (!checklistText.contains("✘")) {
            ChatUtils.showMessage(
                    Component.translatable("babyzombieaddons.cake.all_eaten").getString());
        }
    }

    private static String buildChecklist() {
        var sb = new StringBuilder(512);
        for (int i = 0; i < NAMES.length; i++) {
            if (i > 0) sb.append('\n');
            sb.append(NAMES[i]);
            sb.append(found[i] ? "§a✔" : "§c✘");
        }
        return sb.toString();
    }
}
