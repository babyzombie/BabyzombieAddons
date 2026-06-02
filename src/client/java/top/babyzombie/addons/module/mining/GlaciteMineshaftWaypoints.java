package top.babyzombie.addons.module.mining;

import java.util.ArrayList;
import java.util.List;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.phys.AABB;
import top.babyzombie.addons.config.ModConfigManager;
import top.babyzombie.addons.util.ChatUtils;
import top.babyzombie.addons.util.HypixelLocationTracker;
import top.babyzombie.addons.util.WorldTextRenderer;
import top.babyzombie.addons.util.WorldTextRenderer.TextEntry;

public final class GlaciteMineshaftWaypoints {
    private static long portalTimer;
    private static final List<Waypoint> exits = new ArrayList<>();
    private static int scrapCount;
    private static boolean inMineshaft;

    private GlaciteMineshaftWaypoints() {}

    public static void init() {
        // Track entering mineshaft - detects world change from ClientTick
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (!ModConfigManager.get().mining.mineshaftWaypoints) return;
            var t = HypixelLocationTracker.getInstance();
            if (!t.isInSkyblock()) {
                inMineshaft = false;
                return;
            }
            boolean nowInMineshaft = "Mineshaft".equals(t.getLocation());
            if (nowInMineshaft && !inMineshaft) {
                if (client.player != null) {
                    exits.add(new Waypoint(client.player.getX(), client.player.getY(), client.player.getZ(), "§6出口"));
                }
                scrapCount = 0;
            }
            if (!nowInMineshaft) exits.clear();
            inMineshaft = nowInMineshaft;
        });

        // Corpses: scan armor stands with helmets in mineshaft
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (!ModConfigManager.get().mining.mineshaftWaypoints) return;
            if (!isInMineshaft() || client.player == null || client.player.tickCount % 20 != 0) return;

            var level = client.player.level();
            var stands = level.getEntitiesOfClass(ArmorStand.class,
                    new AABB(client.player.blockPosition()).inflate(64),
                    e -> !e.isDeadOrDying() && !e.isInvisible());

            for (var stand : stands) {
                var helm = stand.getItemBySlot(EquipmentSlot.HEAD);
                if (helm.isEmpty()) continue;
                var customData = helm.getComponents().get(net.minecraft.core.component.DataComponents.CUSTOM_DATA);
                if (customData == null) continue;
                var tag = customData.copyTag();
                if (tag == null) continue;
                var ea = tag.getCompound("ExtraAttributes").orElse(null);
                if (ea == null) continue;
                String id = ea.getString("id").orElse("");
                String name = switch (id) {
                    case "LAPIS_ARMOR_LEGGINGS" -> "§bLapis";
                    case "ARMOR_OF_YOG_LEGGINGS" -> "§6Umber";
                    case "MINERAL_LEGGINGS" -> "§fTungsten";
                    case "VANGUARD_LEGGINGS" -> "§bVanguard";
                    default -> null;
                };
                if (name != null) {
                    exits.add(new Waypoint(stand.getX(), stand.getY() + 2, stand.getZ(), name));
                }
            }
        });

        // Portal found
        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            if (overlay) return;
            if (!ModConfigManager.get().mining.mineshaftWaypoints) return;
            if (!isInDwarvenMines()) return;
            if (ChatUtils.stripColor(message.getString()).equals("WOW! You found a Glacite Mineshaft portal!")) {
                portalTimer = System.currentTimeMillis() + 30_000;
            }
        });

        // Scrap counter
        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            if (overlay) return;
            if (!ModConfigManager.get().mining.mineshaftWaypoints) return;
            if (!isInMineshaft()) return;
            if (ChatUtils.stripColor(message.getString()).startsWith("EXCAVATOR! You found a Suspicious Scrap!")) {
                scrapCount++;
            }
        });

        // World render
        WorldRenderEvents.BEFORE_ENTITIES.register(ctx -> {
            if (!ModConfigManager.get().mining.mineshaftWaypoints) return;

            var entries = new ArrayList<TextEntry>();
            var t = HypixelLocationTracker.getInstance();

            if (t.isInSkyblock() && "Mineshaft".equals(t.getLocation())) {
                for (var e : exits) {
                    entries.add(new TextEntry(e.label, e.x, e.y, e.z, 0xFFFF55));
                }
            }

            if (t.isInSkyblock() && "Dwarven Mines".equals(t.getLocation())) {
                long remaining = portalTimer - System.currentTimeMillis();
                if (remaining > 0) {
                    var player = Minecraft.getInstance().player;
                    if (player != null) {
                        entries.add(new TextEntry("§aPortal: " + (remaining / 1000) + "s",
                                player.getX(), player.getY() + 3, player.getZ(), 0x55FF55));
                    }
                }
            }

            WorldTextRenderer.render(ctx.matrices(), entries);
        });

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            exits.clear();
            scrapCount = 0;
            portalTimer = 0;
            inMineshaft = false;
        });
    }

    private static boolean isInMineshaft() {
        var t = HypixelLocationTracker.getInstance();
        return t.isInSkyblock() && "Mineshaft".equals(t.getLocation());
    }

    private static boolean isInDwarvenMines() {
        var t = HypixelLocationTracker.getInstance();
        return t.isInSkyblock() && "Dwarven Mines".equals(t.getLocation());
    }

    private record Waypoint(double x, double y, double z, String label) {}
}
