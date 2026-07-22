package top.babyzombie.addons.module.kuudra;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.phys.Vec3;
import top.babyzombie.addons.util.ItemUtils;
import top.babyzombie.addons.util.render.RenderPhaseRegister;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.monster.Giant;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.scores.DisplaySlot;
import top.babyzombie.addons.config.ModConfigManager;
import top.babyzombie.addons.util.render.BeamRenderer;
import top.babyzombie.addons.util.ChatUtils;
import top.babyzombie.addons.util.render.WorldRenderUtils;
import top.babyzombie.addons.util.tracker.HypixelLocationTracker;
import top.babyzombie.addons.util.render.WorldTextRenderer;

import java.awt.Color;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class KuudraWaypoints {
    private KuudraWaypoints() {}

    private record Beam(double x, double y, double z, float r, float g, float b, float a, float h) {}

    private record TextData(String text, double x, double y, double z, int color) {}
    private static final Map<String, TextData> textEntries = new ConcurrentHashMap<>();
    private static final Set<String> seenKeys = ConcurrentHashMap.newKeySet();
    private static final List<Beam> beams = new ArrayList<>();
    private static final List<Vec3> supplies = new ArrayList<>();
    private static final List<Vec3> ballistaPiles = new ArrayList<>();
    private static final List<Vec3> fuels = new ArrayList<>();
    private static final List<Vec3> chucks = new ArrayList<>();

    private enum SkullTextures {
        SUPPLIES("ewogICJ0aW1lc3RhbXAiIDogMTU5NDAyOTYxNjQyNCwKICAicHJvZmlsZUlkIiA6ICJkZGVkNTZlMWVmOGI0MGZlOGFkMTYyOTIwZjdhZWNkYSIsCiAgInByb2ZpbGVOYW1lIiA6ICJEaXNjb3JkQXBwIiwKICAic2lnbmF0dXJlUmVxdWlyZWQiIDogdHJ1ZSwKICAidGV4dHVyZXMiIDogewogICAgIlNLSU4iIDogewogICAgICAidXJsIiA6ICJodHRwOi8vdGV4dHVyZXMubWluZWNyYWZ0Lm5ldC90ZXh0dXJlLzI0YmJmZDlkODRmNDI0NTZjZDAyYTRiYWE1Y2QwNTRiY2VkMGRkYjJkMWM4MzIxYzgzZTVkNjY3Y2Q4NTU3NWEiCiAgICB9CiAgfQp9"),
        FUEL("ewogICJ0aW1lc3RhbXAiIDogMTcyMDAyOTIzMDk5OSwKICAicHJvZmlsZUlkIiA6ICJkM2Y5MjEyMjY3YzM0YzEwYWNjOWZkNGI5MDFkYjI0ZiIsCiAgInByb2ZpbGVOYW1lIiA6ICJkYXl3ZSIsCiAgInNpZ25hdHVyZVJlcXVpcmVkIiA6IHRydWUsCiAgInRleHR1cmVzIiA6IHsKICAgICJTS0lOIiA6IHsKICAgICAgInVybCIgOiAiaHR0cDovL3RleHR1cmVzLm1pbmVjcmFmdC5uZXQvdGV4dHVyZS9mZDcyZGViMWFiMDAzM2I0MmIwYTEyZWZjZjQ4M2YwZmJhMjZkYzUxZGVkMzkxOWViYWRiNzBmOTY1N2ExZjYxIgogICAgfQogIH0KfQ=="),
        REDCHUCK("ewogICJ0aW1lc3RhbXAiIDogMTYwNzg1MjU5NjMwNCwKICAicHJvZmlsZUlkIiA6ICJlZDUzZGQ4MTRmOWQ0YTNjYjRlYjY1MWRjYmE3N2U2NiIsCiAgInByb2ZpbGVOYW1lIiA6ICI0MTQxNDE0MWgiLAogICJzaWduYXR1cmVSZXF1aXJlZCIgOiB0cnVlLAogICJ0ZXh0dXJlcyIgOiB7CiAgICAiU0tJTiIgOiB7CiAgICAgICJ1cmwiIDogImh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMmIwNTVjODEwYmRkZmQxNjI2NGVjOGQ0MzljNDMyODNlMzViY2E3MWE1MDk4M2UxNWUzNjRjZDhhYjdjNjY4ZiIKICAgIH0KICB9Cn0="),
        PURPLECHUCK("ewogICJ0aW1lc3RhbXAiIDogMTYwNzY5Njk5MDAzNywKICAicHJvZmlsZUlkIiA6ICI3MmNiMDYyMWU1MTA0MDdjOWRlMDA1OTRmNjAxNTIyZCIsCiAgInByb2ZpbGVOYW1lIiA6ICJNb3M5OTAiLAogICJzaWduYXR1cmVSZXF1aXJlZCIgOiB0cnVlLAogICJ0ZXh0dXJlcyIgOiB7CiAgICAiU0tJTiIgOiB7CiAgICAgICJ1cmwiIDogImh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZGI2OTc1YWY3MDcyNGQ2YTQ0ZmQ1OTQ2ZTYwYjI3MTc3MzdkZmRiNTQ1YjRkYWIxODkzMzUxYTljOWRkMTgzYyIKICAgIH0KICB9Cn0="),
        YELLOWCHUCK("ewogICJ0aW1lc3RhbXAiIDogMTYwNzg1NDk1NzQ1OCwKICAicHJvZmlsZUlkIiA6ICJjZGM5MzQ0NDAzODM0ZDdkYmRmOWUyMmVjZmM5MzBiZiIsCiAgInByb2ZpbGVOYW1lIiA6ICJSYXdMb2JzdGVycyIsCiAgInNpZ25hdHVyZVJlcXVpcmVkIiA6IHRydWUsCiAgInRleHR1cmVzIiA6IHsKICAgICJTS0lOIiA6IHsKICAgICAgInVybCIgOiAiaHR0cDovL3RleHR1cmVzLm1pbmVjcmFmdC5uZXQvdGV4dHVyZS81MjA3MTYyMDNkYTAzOWNhZmNhMjRiYmQ5ZjNlOWJkNWM2OTg1YzNjMjU1MjdiY2Q1MDZkMzg4ZTk5YmI3YWZlIgogICAgfQogIH0KfQ==");

        private final String texture;

        SkullTextures(String texture) {this.texture = texture;}

        boolean isHoldingThis(LivingEntity entity) {
            var item = entity.getItemBySlot(EquipmentSlot.MAINHAND);
            if (item.isEmpty()) return false;
            return Objects.equals(ItemUtils.getSkullTexture(item), texture);
        }
    }

    public static void init() {
        RenderPhaseRegister.register(ctx -> {
            for (var t : textEntries.values())
                WorldTextRenderer.renderString(ctx, t.text, t.x, t.y, t.z, t.color, 0.05f, true);

            var cfg = ModConfigManager.get().kuudra;

            // Supply beams
            float[] sc = argbToFloats(cfg.waypoints.supplyBeaconColor.getEffectiveColourRGB());
            int supplyColor = new Color(sc[0], sc[1], sc[2], sc[3]).getRGB();
            for (var v : supplies)
                BeamRenderer.drawBeam(ctx, v.x, v.y, v.z, 20f, 0.15f, supplyColor);

            // Ballista pile proximity circles
            if (!ballistaPiles.isEmpty() && cfg.waypoints.ballistaProximityCircles) {
                var client = Minecraft.getInstance();
                if (client.player != null) {
                    double px = client.player.getX();
                    double pz = client.player.getZ();
                    for (var v : ballistaPiles) {
                        double dx = px - v.x;
                        double dz = pz - v.z;
                        boolean inside = (dx * dx + dz * dz) <= 4.0;
                        float cr = inside ? 0f : 1f;
                        float cg = 1f;
                        float cb = 0f;
                        WorldRenderUtils.drawCircle(ctx, v.x, 79.01, v.z, 2.0, cr, cg, cb, 0.7f, true, 2f);
                    }
                }
            }

            // Fuel beams
            float[] fc = argbToFloats(cfg.waypoints.fuelOrbBeaconColor.getEffectiveColourRGB());
            int fuelColor = new Color(fc[0], fc[1], fc[2], fc[3]).getRGB();
            for (var v : fuels)
                BeamRenderer.drawBeam(ctx, v.x, v.y, v.z, 20f, 0.15f, fuelColor);

            // Chuck beams + ground circles
            float[] cc = argbToFloats(cfg.waypoints.chuckBeaconColor.getEffectiveColourRGB());
            int chuckColor = new Color(cc[0], cc[1], cc[2], cc[3]).getRGB();
            for (var v : chucks) {
                BeamRenderer.drawBeam(ctx, v.x, v.y, v.z, 20f, 0.15f, chuckColor);
                WorldRenderUtils.drawCircle(ctx, v.x, v.y, v.z, 20, 1, 0, 0, 1, true, 3);
            }

            // Unclassified beams (dropoff markers, ballista, etc.)
            for (var b : beams)
                BeamRenderer.drawBeam(ctx, b.x, b.y, b.z, b.h, 0.15f, new Color(b.r, b.g, b.b, b.a).getRGB());
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            var cfg = ModConfigManager.get().kuudra;
            boolean anyOn = cfg.waypoints.supplyBeacons || cfg.waypoints.supplyDropoffBeacons
                    || cfg.waypoints.ballistaProgressText || cfg.waypoints.ballistaBuildBeacons
                    || cfg.waypoints.ballistaProximityCircles
                    || cfg.waypoints.fuelOrbBeacons || cfg.waypoints.chuckBeacons;
            if (!anyOn) return;
            if (!HypixelLocationTracker.getInstance().isInKuudra()) return;
            if (client.player == null || client.player.tickCount % 20 != 0) return;

            beams.clear();
            supplies.clear();
            ballistaPiles.clear();
            fuels.clear();
            chucks.clear();
            seenKeys.clear();
            String phase = getScoreboardPhase(client);

            if ("Rescue supplies".equals(phase)) {
                if (cfg.waypoints.supplyBeacons) {
                    for (var g : client.player.level().getEntitiesOfClass(Giant.class,
                            new AABB(client.player.blockPosition()).inflate(64), SkullTextures.SUPPLIES::isHoldingThis)) {
                        supplies.add(new Vec3(g.getX() - 2, g.getY() + 8, g.getZ() + 2.5));
                    }
                }
                if (cfg.waypoints.supplyDropoffBeacons) {
                    float[] c = argbToFloats(cfg.waypoints.supplyDropoffBeaconColor.getEffectiveColourRGB());
                    for (var s : client.player.level().getEntitiesOfClass(
                            net.minecraft.world.entity.decoration.ArmorStand.class,
                            new AABB(client.player.blockPosition()).inflate(64),
                            e -> ChatUtils.stripColor(e.getName().getString()).contains("BRING SUPPLY CHEST HERE"))) {
                        double x = s.getX(), z = s.getZ();
                        beams.add(new Beam(x, s.getY(), z, c[0], c[1], c[2], c[3], 20f));
                    }
                }
            } else if ("Protect Elle".equals(phase)) {
                if (cfg.waypoints.ballistaBuildBeacons || cfg.waypoints.ballistaProgressText || cfg.waypoints.ballistaProximityCircles) {
                    float[] bc = argbToFloats(cfg.waypoints.ballistaBeaconColor.getEffectiveColourRGB());
                    for (var s : client.player.level().getEntitiesOfClass(
                            net.minecraft.world.entity.decoration.ArmorStand.class,
                            new AABB(client.player.blockPosition()).inflate(64),
                            e -> {
                                String name = ChatUtils.stripColor(e.getName().getString());
                                return name.startsWith("PROGRESS: ") && !name.endsWith("COMPLETE");
                            })) {
                        double x = s.getX(), y = s.getY(), z = s.getZ();
                        ballistaPiles.add(new Vec3(x, y, z));
                        if (cfg.waypoints.ballistaBuildBeacons)
                            beams.add(new Beam(x, y, z, bc[0], bc[1], bc[2], bc[3], 10f));
                        if (cfg.waypoints.ballistaProgressText) {
                            String[] parts = ChatUtils.stripColor(s.getName().getString()).split(" ");
                            String key = "p2_" + s.getId(); seenKeys.add(key);
                            textEntries.put(key, new TextData(parts.length > 1 ? parts[parts.length - 1] : "",
                                    x, y + 1.2, z, cfg.waypoints.ballistaTextColor.getEffectiveColourRGB()));
                        }
                    }
                }
            } else {
                boolean wantFuel = cfg.waypoints.fuelOrbBeacons;
                boolean wantChuck = cfg.waypoints.chuckBeacons;
                if (wantFuel || wantChuck) {
                    for (var g : client.player.level().getEntitiesOfClass(Giant.class,
                            new AABB(client.player.blockPosition()).inflate(64))) {
                        if (wantFuel && SkullTextures.FUEL.isHoldingThis(g)) {
                            fuels.add(new Vec3(g.getX() - 2, g.getY() + 8, g.getZ() + 2.5));
                        } else if (wantChuck && (SkullTextures.YELLOWCHUCK.isHoldingThis(g) || SkullTextures.REDCHUCK.isHoldingThis(g) || SkullTextures.PURPLECHUCK.isHoldingThis(g))) {
                            chucks.add(new Vec3(g.getX() - 2, g.getY() + 8, g.getZ() + 2.5));
                        }
                    }
                }
            }

            textEntries.keySet().removeIf(k -> !seenKeys.contains(k));
        });
    }

    private static float[] argbToFloats(int argb) {
        return new float[] {
            ((argb >> 16) & 0xFF) / 255f,
            ((argb >> 8) & 0xFF) / 255f,
            (argb & 0xFF) / 255f,
            ((argb >> 24) & 0xFF) / 255f
        };
    }

    private static String getScoreboardPhase(Minecraft client) {
        if (client.player == null) return "";
        var obj = client.player.level().getScoreboard().getDisplayObjective(DisplaySlot.BY_ID.apply(1));
        if (obj == null) return "";
        for (var holder : client.player.level().getScoreboard().getTrackedPlayers()) {
            if (!client.player.level().getScoreboard().listPlayerScores(holder).containsKey(obj)) continue;
            var team = client.player.level().getScoreboard().getPlayersTeam(holder.getScoreboardName());
            if (team == null) continue;
            String text = ChatUtils.stripColor(ChatUtils.removeEmoji(
                    team.getPlayerPrefix().getString() + team.getPlayerSuffix().getString()))
                    .replaceAll(" \\(.+\\)", "");
            if (text.equals("Rescue supplies") || text.equals("Protect Elle")) return text;
        }
        return "";
    }
}
