package top.babyzombie.addons.module.kuudra;

import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.Minecraft;
import top.babyzombie.addons.config.hud.HudManager;
import top.babyzombie.addons.config.ModConfigManager;
import top.babyzombie.addons.util.ChatUtils;
import top.babyzombie.addons.util.HypixelLocationTracker;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class KuudraEnergyDisplay {
    private KuudraEnergyDisplay() {}

    private static final Pattern FUEL_PATTERN = Pattern.compile("\\(([0-9]+)%\\)");
    private static int fuel = -1;

    public static void init() {
        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            if (!ModConfigManager.get().kuudra.energyDisplay) return;
            if (overlay || !HypixelLocationTracker.getInstance().isInKuudra()) return;
            String text = message.getString();
            if (text.contains("recovered a Fuel Cell and charged the Ballista")) {
                Matcher m = FUEL_PATTERN.matcher(text);
                if (m.find()) fuel = Integer.parseInt(m.group(1));
            }
        });

        HudRenderCallback.EVENT.register((gui, delta) -> {
            if (!ModConfigManager.get().kuudra.energyDisplay) return;
            if (fuel < 0 || "p4".equals(KuudraLocationTracker.area)) return;
            var font = Minecraft.getInstance().font;
            int x = HudManager.x("EnergyCharge"), y = HudManager.y("EnergyCharge");
            float s = HudManager.scale("EnergyCharge");
            String text = ChatUtils.translate("kuudra.energy", fuel);
            HudManager.drawScaled(gui, font, text, x, y, s);
        });
    }

}
