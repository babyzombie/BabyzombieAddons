package top.babyzombie.addons.module.skywars;

import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.Minecraft;
import top.babyzombie.addons.config.HudManager;
import top.babyzombie.addons.config.ModConfigManager;
import top.babyzombie.addons.util.ChatUtils;

/**
 * Skywars Lucky Blocks features: fast devil's contract,
 * AOTE counter, compass display.
 */
public final class SkywarsModule {

    private static long devilContractTime;
    private static boolean inSkywars;
    private static boolean hasCompass;

    private SkywarsModule() {}

    public static void init() {
        // Devil's contract detection
        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            if (overlay) return;
            String text = ChatUtils.stripColor(message.getString());
            if (text.contains("Devil's Contract")) {
                devilContractTime = System.currentTimeMillis();
            }
        });

        // Skywars game detection
        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            if (overlay) return;
            String text = ChatUtils.stripColor(message.getString());
            if (text.contains("SkyWars") && (text.contains("joined") || text.contains("started"))) {
                inSkywars = true;
            }
            if (text.contains("won") || text.contains("died") || text.contains("eliminated")) {
                inSkywars = false;
            }
        });

        // Devil's contract timer HUD
        HudRenderCallback.EVENT.register((gui, delta) -> {
            if (devilContractTime <= 0) return;
            long remaining = 60000 - (System.currentTimeMillis() - devilContractTime);
            if (remaining <= 0) {
                devilContractTime = 0;
                return;
            }
            long seconds = remaining / 1000;
            String color = seconds <= 10 ? "§c" : "§e";
            var font = Minecraft.getInstance().font;
            int x = HudManager.x("DevilsContract"), y = HudManager.y("DevilsContract");
            gui.drawString(font, color + "Devil's Contract: §f" + seconds + "s", x, y, 0xFFFFFFFF, true);
        });
    }
}
