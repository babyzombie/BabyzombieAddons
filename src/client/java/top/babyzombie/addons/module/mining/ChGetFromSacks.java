package top.babyzombie.addons.module.mining;

import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.client.Minecraft;
import top.babyzombie.addons.config.ModConfigManager;
import top.babyzombie.addons.util.ChatUtils;
import top.babyzombie.addons.util.HypixelLocationTracker;
import top.babyzombie.addons.util.ServerTick;

public final class ChGetFromSacks {
    private static long cooldown;
    private static boolean gettingApparatus;
    private static boolean triedGfsApparatus;

    private ChGetFromSacks() {}

    public static void init() {
        // NPC triggers
        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            if (overlay) return;
            if (!ModConfigManager.get().mining.getFromSacks) return;
            var tracker = HypixelLocationTracker.getInstance();
            if (!tracker.isInSkyblock() || !"Crystal Hollows".equals(tracker.getMap())) return;

            long now = ServerTick.getTime();
            if (cooldown > now) return;

            String text = ChatUtils.stripColor(message.getString());
            switch (text) {
                case "[NPC] Kalhuiki Door Guardian: This temple is locked, you will need to bring me a key to open the door!" -> {
                    if (hasItemInInventory("JUNGLE_KEY")) return;
                    ChatUtils.sendCommand("getfromsacks jungle key 1");
                    cooldown = now + 800;
                }
                case "[NPC] King Yolkar: Bring me any type of Goblin Egg and we can show the Goblin Queen what it's like to lose something she loves!",
                     "[NPC] King Yolkar: Where is my Goblin Egg? My Chef is waiting!",
                     "[NPC] King Yolkar: That is certainly not the meal I am looking for! Bring me back some Goblin Egg and you will satiate my hunger." -> {
                    if (hasItemInInventory("GOBLIN_EGG")) return;
                    ChatUtils.sendCommand("getfromsacks goblin egg 1");
                    cooldown = now + 800;
                }
                case "[NPC] Professor Robot: Bring me all 6 key components to the giant so that I can repair it!",
                     "[NPC] Professor Robot: That's not one of the components I need! Bring me one of the missing components:" -> {
                    if (hasItemInInventory("PRECURSOR_APPARATUS")) return;
                    ChatUtils.sendCommand("getfromsacks precursor apparatus 1");
                    triedGfsApparatus = true;
                    cooldown = now + 800;
                }
            }
        });

        // gfs failed → open recipe viewer
        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            if (overlay) return;
            if (!ModConfigManager.get().mining.getFromSacks) return;
            if (!triedGfsApparatus) return;
            if (ChatUtils.stripColor(message.getString()).equals("You have no Precursor Apparatus in your Sacks!")) {
                triedGfsApparatus = false;
                ChatUtils.sendCommand("viewrecipe PRECURSOR_APPARATUS");
                gettingApparatus = true;
            }
        });

        // Auto-close recipe viewer after supercraft
        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            if (overlay) return;
            if (!ModConfigManager.get().mining.getFromSacks) return;
            if (!gettingApparatus) return;
            if (ChatUtils.stripColor(message.getString()).equals("You Supercrafted Precursor Apparatus!")) {
                var client = Minecraft.getInstance();
                if (client.screen != null) client.screen.onClose();
                gettingApparatus = false;
            }
        });
    }

    private static boolean hasItemInInventory(String sbid) {
        var player = Minecraft.getInstance().player;
        if (player == null) return false;
        var inv = player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            var stack = inv.getItem(i);
            if (!stack.isEmpty()) {
                var customData = stack.getComponents().get(net.minecraft.core.component.DataComponents.CUSTOM_DATA);
                if (customData == null) continue;
                var tag = customData.copyTag();
                if (tag != null) {
                    var ea = tag.getCompound("ExtraAttributes").orElse(null);
                    if (ea != null && sbid.equals(ea.getString("id").orElse(""))) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
