package top.babyzombie.addons.module.kuudra;

import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.client.Minecraft;
import top.babyzombie.addons.config.ModConfigManager;
import top.babyzombie.addons.util.ChatUtils;
import top.babyzombie.addons.util.HypixelLocationTracker;

import java.util.Calendar;
import java.util.Objects;
import java.util.TimeZone;

public final class KuudraFollowerHelmetPrice {
    private KuudraFollowerHelmetPrice() {}

    public static void init() {
        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            if (!ModConfigManager.get().kuudra.followerHelmetPrice) return;
            if (overlay) return;
            if (!HypixelLocationTracker.getInstance().isInSkyblock()) return;
            if (!Objects.equals(HypixelLocationTracker.getInstance().getMap(), "Crimson Isle")
                    || !Objects.equals(HypixelLocationTracker.getInstance().getLocation(), "Plhlegblast Pool")) return;

            String text = ChatUtils.stripColor(message.getString());
            if (!text.equals("[NPC] Kuudra Believer: Maybe Kuudra will show favor upon you.")) return;

            var player = Minecraft.getInstance().player;
            if (player == null) return;
            String name = player.getName().getString();
            int price = calculatePrice(name);
            int day = getDayOfYear();

            String msg = ChatUtils.translate("kuudra.follower_helmet", name, price, day);
            ChatUtils.showMessage(msg);
            ChatUtils.copyToClipboard(msg);
        });
    }

    private static int calculatePrice(String name) {
        return 13331996 + 2 * getDayOfYear() * getDayOfYear() - 58 * name.length();
    }

    private static int getDayOfYear() {
        var cal = Calendar.getInstance(TimeZone.getTimeZone("America/New_York"));
        return cal.get(Calendar.DAY_OF_YEAR);
    }
}
