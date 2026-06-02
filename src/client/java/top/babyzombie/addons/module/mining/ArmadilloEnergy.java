package top.babyzombie.addons.module.mining;

import java.util.regex.Pattern;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.Minecraft;
import top.babyzombie.addons.config.ModConfigManager;
import top.babyzombie.addons.config.hud.HudManager;
import top.babyzombie.addons.util.ChatUtils;
import top.babyzombie.addons.util.HypixelLocationTracker;

public final class ArmadilloEnergy {
    private static final Pattern ENERGY_PATTERN = Pattern.compile("Armadillo Energy:[ ]+([0-9]{1,3}[.]?[0-9]?)/([0-9]{1,3}[.]?[0-9]?)");
    private static boolean hasDillo;
    private static double energyNow, energyMax;

    private ArmadilloEnergy() {}

    public static void init() {
        // Track armadillo pet status
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (!ModConfigManager.get().mining.armadilloEnergy) return;
            if (!isInCrystalHollows()) return;
            var player = client.player;
            if (player == null) {
                hasDillo = false;
                return;
            }

            // Check if riding (armadillo pet is active)
            if (player.isPassenger() && energyMax > 0) {
                hasDillo = false;
                return;
            }

            // Energy regeneration (simplified)
            if (!hasDillo || energyMax == 0) return;
            if (energyNow + energyMax / 300 < energyMax)
                energyNow = Math.floor((energyNow + energyMax / 300) * 10) / 10;
            else energyNow = energyMax;
        });

        // Parse action bar for energy values
        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            if (!overlay) return;
            if (!ModConfigManager.get().mining.armadilloEnergy) return;
            if (!isInCrystalHollows()) return;
            String text = ChatUtils.stripColor(message.getString());
            var matcher = ENERGY_PATTERN.matcher(text);
            if (matcher.find()) {
                energyNow = Double.parseDouble(matcher.group(1));
                energyMax = Double.parseDouble(matcher.group(2));
                hasDillo = true;
            }
        });

        // Full energy alert
        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            if (overlay) return;
            if (!ModConfigManager.get().mining.armadilloEnergy) return;
            if (!isInCrystalHollows()) return;
            if (ChatUtils.stripColor(message.getString()).equals("Your armadillo is full of energy!")) {
                energyNow = energyMax;
            }
        });

        // HUD overlay
        HudRenderCallback.EVENT.register((gui, delta) -> {
            if (!ModConfigManager.get().mining.armadilloEnergy) return;
            if (!isInCrystalHollows() || !hasDillo || energyMax == 0) return;
            if (energyNow >= energyMax) return;

            var font = Minecraft.getInstance().font;
            String text = "§eArmadillo §f" + (int) energyNow + "/" + (int) energyMax;
            int x = HudManager.x("ArmadilloEnergy");
            int y = HudManager.y("ArmadilloEnergy");
            float s = HudManager.scale("ArmadilloEnergy");
            HudManager.drawScaled(gui, font, text, x, y, s);
        });
    }

    private static boolean isInCrystalHollows() {
        var t = HypixelLocationTracker.getInstance();
        return t.isInSkyblock() && "Crystal Hollows".equals(t.getMap());
    }
}
