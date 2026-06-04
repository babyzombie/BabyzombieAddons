package top.babyzombie.addons.module.kuudra;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientWorldEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.phys.AABB;
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
        ClientWorldEvents.AFTER_CLIENT_WORLD_CHANGE.register((client, world) -> {
            fuel = -1;
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (!ModConfigManager.get().kuudra.energyDisplay) return;
            if (!HypixelLocationTracker.getInstance().isInKuudra()) return;
            if (fuel == 0 || fuel == 25) return;
            if (client.player == null) return;
            if (!client.player.level().getEntitiesOfClass(ArmorStand.class,
                    new AABB(client.player.blockPosition()).inflate(64),
                    e -> "§fEnergy Charge: §a0%".equals(e.getName().getString())).isEmpty()) {
                fuel = 0;
            }
        });

        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            if (!ModConfigManager.get().kuudra.energyDisplay) return;
            if (overlay || !HypixelLocationTracker.getInstance().isInKuudra()) return;
            String text = message.getString();
            if (text.contains("recovered a Fuel Cell and charged the Ballista")) {
                Matcher m = FUEL_PATTERN.matcher(text);
                if (m.find()) fuel = Integer.parseInt(m.group(1));
            }
        });

        HudElementRegistry.attachElementAfter(VanillaHudElements.OVERLAY_MESSAGE,
                Identifier.fromNamespaceAndPath("babyzombieaddons", "kuudra_energy"),
                (context, tickCounter) -> {
            if (!ModConfigManager.get().kuudra.energyDisplay) return;
            if (!KuudraLocationTracker.inKuudra || fuel < 0 || "p4".equals(KuudraLocationTracker.area)) return;
            var font = Minecraft.getInstance().font;
            int x = HudManager.x("EnergyCharge"), y = HudManager.y("EnergyCharge");
            float s = HudManager.scale("EnergyCharge");
            String text = ChatUtils.translate("kuudra.energy", fuel);
            HudManager.drawScaled(context, font, text, x, y, s);
        });
    }

}
