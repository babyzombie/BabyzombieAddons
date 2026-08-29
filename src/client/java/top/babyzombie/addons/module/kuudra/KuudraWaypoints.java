package top.babyzombie.addons.module.kuudra;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLevelEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.projectile.FishingHook;
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
import java.util.regex.Pattern;

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
    /** 三色球:位置 + 头颅类型(小地图按类型显示对应头颅图标)。 */
    public record Chuck(Vec3 pos, SkullTextures kind) {}
    private static final List<Chuck> chucks = new ArrayList<>();
    private static final List<Zombie> supplyZombies = new ArrayList<>();
    private static final List<Giant> supplyGiants = new ArrayList<>();
    private static final List<Zombie> fuelZombies = new ArrayList<>();

    private static final double SUPPLY_PULL_RADIUS = 5.0;
    private static final double SUPPLY_CRATE_OFFSET = 3.7;
    private static final double SUPPLY_VERTICAL_MARGIN = 4.0;

    /** P1 补给阶段活跃标志：fish up 聊天即置位（不等计分板切换），收集完毕/P2 计分板/换图清除。
     *  解决"箱子刚浮出、计分板还显示旧阶段"时无标记的问题。 */
    private static boolean p1SupplyPhaseActive;

    /** 榜行文本后缀名归一：去 " (...)" 后缀。每 tick 扫描高频调用，静态复用避免内联 replaceAll 重复编译正则 */
    private static final Pattern SUFFIX_PAREN_PATTERN = Pattern.compile(" \\(.+\\)");


    /** 头颅纹理枚举:包内共享(KuudraMinimap 读取 base64 构建图标)。 */
    enum SkullTextures {
        SUPPLIES("ewogICJ0aW1lc3RhbXAiIDogMTU5NDAyOTYxNjQyNCwKICAicHJvZmlsZUlkIiA6ICJkZGVkNTZlMWVmOGI0MGZlOGFkMTYyOTIwZjdhZWNkYSIsCiAgInByb2ZpbGVOYW1lIiA6ICJEaXNjb3JkQXBwIiwKICAic2lnbmF0dXJlUmVxdWlyZWQiIDogdHJ1ZSwKICAidGV4dHVyZXMiIDogewogICAgIlNLSU4iIDogewogICAgICAidXJsIiA6ICJodHRwOi8vdGV4dHVyZXMubWluZWNyYWZ0Lm5ldC90ZXh0dXJlLzI0YmJmZDlkODRmNDI0NTZjZDAyYTRiYWE1Y2QwNTRiY2VkMGRkYjJkMWM4MzIxYzgzZTVkNjY3Y2Q4NTU3NWEiCiAgICB9CiAgfQp9"),
        FUEL("ewogICJ0aW1lc3RhbXAiIDogMTcyMDAyOTIzMDk5OSwKICAicHJvZmlsZUlkIiA6ICJkM2Y5MjEyMjY3YzM0YzEwYWNjOWZkNGI5MDFkYjI0ZiIsCiAgInByb2ZpbGVOYW1lIiA6ICJkYXl3ZSIsCiAgInNpZ25hdHVyZVJlcXVpcmVkIiA6IHRydWUsCiAgInRleHR1cmVzIiA6IHsKICAgICJTS0lOIiA6IHsKICAgICAgInVybCIgOiAiaHR0cDovL3RleHR1cmVzLm1pbmVjcmFmdC5uZXQvdGV4dHVyZS9mZDcyZGViMWFiMDAzM2I0MmIwYTEyZWZjZjQ4M2YwZmJhMjZkYzUxZGVkMzkxOWViYWRiNzBmOTY1N2ExZjYxIgogICAgfQogIH0KfQ=="),
        REDCHUCK("ewogICJ0aW1lc3RhbXAiIDogMTYwNzg1MjU5NjMwNCwKICAicHJvZmlsZUlkIiA6ICJlZDUzZGQ4MTRmOWQ0YTNjYjRlYjY1MWRjYmE3N2U2NiIsCiAgInByb2ZpbGVOYW1lIiA6ICI0MTQxNDE0MWgiLAogICJzaWduYXR1cmVSZXF1aXJlZCIgOiB0cnVlLAogICJ0ZXh0dXJlcyIgOiB7CiAgICAiU0tJTiIgOiB7CiAgICAgICJ1cmwiIDogImh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMmIwNTVjODEwYmRkZmQxNjI2NGVjOGQ0MzljNDMyODNlMzViY2E3MWE1MDk4M2UxNWUzNjRjZDhhYjdjNjY4ZiIKICAgIH0KICB9Cn0="),
        PURPLECHUCK("ewogICJ0aW1lc3RhbXAiIDogMTYwNzY5Njk5MDAzNywKICAicHJvZmlsZUlkIiA6ICI3MmNiMDYyMWU1MTA0MDdjOWRlMDA1OTRmNjAxNTIyZCIsCiAgInByb2ZpbGVOYW1lIiA6ICJNb3M5OTAiLAogICJzaWduYXR1cmVSZXF1aXJlZCIgOiB0cnVlLAogICJ0ZXh0dXJlcyIgOiB7CiAgICAiU0tJTiIgOiB7CiAgICAgICJ1cmwiIDogImh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZGI2OTc1YWY3MDcyNGQ2YTQ0ZmQ1OTQ2ZTYwYjI3MTc3MzdkZmRiNTQ1YjRkYWIxODkzMzUxYTljOWRkMTgzYyIKICAgIH0KICB9Cn0="),
        YELLOWCHUCK("ewogICJ0aW1lc3RhbXAiIDogMTYwNzg1NDk1NzQ1OCwKICAicHJvZmlsZUlkIiA6ICJjZGM5MzQ0NDAzODM0ZDdkYmRmOWUyMmVjZmM5MzBiZiIsCiAgInByb2ZpbGVOYW1lIiA6ICJSYXdMb2JzdGVycyIsCiAgInNpZ25hdHVyZVJlcXVpcmVkIiA6IHRydWUsCiAgInRleHR1cmVzIiA6IHsKICAgICJTS0lOIiA6IHsKICAgICAgInVybCIgOiAiaHR0cDovL3RleHR1cmVzLm1pbmVjcmFmdC5uZXQvdGV4dHVyZS81MjA3MTYyMDNkYTAzOWNhZmNhMjRiYmQ5ZjNlOWJkNWM2OTg1YzNjMjU1MjdiY2Q1MDZkMzg4ZTk5YmI3YWZlIgogICAgfQogIH0KfQ==");

        private final String texture;

        SkullTextures(String texture) {this.texture = texture;}

        /** 头颅纹理 base64 value(KuudraMinimap 构建图标 ItemStack 用)。 */
        String texture() { return texture; }

        boolean isHoldingThis(LivingEntity entity) {
            var item = entity.getItemBySlot(EquipmentSlot.MAINHAND);
            if (item.isEmpty()) return false;
            return Objects.equals(ItemUtils.getSkullTexture(item), texture);
        }
    }

    public static void init() {
        RenderPhaseRegister.register(ctx -> {
            if (!HypixelLocationTracker.getInstance().isInKuudra()) return;
            var cfg = ModConfigManager.get().kuudra;

            // Supply beams (crate 渲染中心：x+0.5, z+1.5)
            var supplyCol = cfg.phase1.supplyBeaconColor.getEffectiveColour();
            int supplyColor = supplyCol.getRGB();
            for (var v : supplies)
                BeamRenderer.drawBeam(ctx, v.x + 0.5, v.y, v.z + 1.5, 256f, 0.15f, supplyColor);

            // Supply interaction zone (invisible zombies) — 精确碰撞箱，独立颜色，范围内全透明、范围外减半
            if (cfg.phase1.supplyInteractionZone) {
                float[] zc = argbToFloats(cfg.phase1.supplyZombieBoxColor.getEffectiveColourRGB());
                for (var z : supplyZombies) {
                    if (z.isRemoved()) continue;
                    var cp = Minecraft.getInstance().player;
                    double dist = cp != null ? cp.distanceTo(z) : 99;
                    boolean inRange = dist <= 3;
                    AABB bb = z.getBoundingBox();
                    float zAlpha = inRange ? zc[3] : zc[3] * 0.5f;
                    WorldRenderUtils.drawFilledBox(ctx,
                            bb.minX, bb.minY, bb.minZ, bb.maxX, bb.maxY, bb.maxZ,
                            zc[0], zc[1], zc[2], zAlpha, true);
                }
            }
            // Supply giant hitbox — 只在玩家处于巨人碰撞箱（向外扩 1 格）内时显示，默认黄色
            if (cfg.phase1.supplyGiantHitbox) {
                float[] gc = argbToFloats(cfg.phase1.supplyGiantHitboxColor.getEffectiveColourRGB());
                var cp = Minecraft.getInstance().player;
                if (cp != null) {
                    for (var g : supplyGiants) {
                        if (g.isRemoved()) continue;
                        AABB bb = g.getBoundingBox();
                        if (!bb.inflate(1.0).contains(cp.getX(), cp.getY(), cp.getZ())) continue;
                        boolean inRange = Math.hypot(cp.getX() - g.getX(), cp.getZ() - g.getZ()) <= 3;
                        WorldRenderUtils.drawFilledBox(ctx,
                                bb.minX, bb.minY, bb.minZ, bb.maxX, bb.maxY, bb.maxZ,
                                inRange ? 0f : gc[0], 1f, inRange ? 0f : gc[2], gc[3], true);
                    }
                }
            }

            // Supply pull circle
            if (cfg.phase1.supplyPullCircle) {
                for (var v : supplies) {
                    boolean inRange = isBobberInsideRange(v);
                    float cr = inRange ? 0f : supplyCol.getRed() / 255f;
                    float cg = 1f;
                    float cb = inRange ? 0f : supplyCol.getBlue() / 255f;
                    WorldRenderUtils.drawCircle(ctx, v.x + 0.5, v.y, v.z + 1.5, (float)SUPPLY_PULL_RADIUS,
                            cr, cg, cb, 0.6f, true, 3f);
                }
            }

            // Ballista pile proximity circles
            if (!ballistaPiles.isEmpty() && cfg.phase2.ballistaProximityCircles) {
                var client = Minecraft.getInstance();
                if (client.player != null) {
                    double px = client.player.getX();
                    double pz = client.player.getZ();
                    for (var v : ballistaPiles) {
                        double dx = px - v.x;
                        double dz = pz - v.z;
                        boolean inside = (dx * dx + dz * dz) <= 6.25;
                        float cr = inside ? 0f : 1f;
                        float cg = 1f;
                        float cb = 0f;
                        WorldRenderUtils.drawCircle(ctx, v.x, 79.01, v.z, 2.5, cr, cg, cb, 0.7f, true, 2f);
                    }
                }
            }

            // Ballista build beacons — 与脚下的圈联动：圈内用 inCircleColor（默认绿），圈外黄色
            if (cfg.phase2.ballistaBuildBeacons) {
                var incCol = cfg.phase2.ballistaInCircleColor.getEffectiveColour();
                var bcCol = cfg.phase2.ballistaBeaconColor.getEffectiveColour();
                var player = Minecraft.getInstance().player;
                for (var v : ballistaPiles) {
                    // 与上方圈的判断一致：水平距离平方 <= 6.25（半径 2.5）
                    boolean inside = false;
                    if (player != null) {
                        double dx = player.getX() - v.x;
                        double dz = player.getZ() - v.z;
                        inside = (dx * dx + dz * dz) <= 6.25;
                    }
                    BeamRenderer.drawBeam(ctx, v.x, v.y, v.z, 10f, 0.15f,
                            inside ? incCol.getRGB() : bcCol.getRGB());
                }
            }

            // Fuel beams
            var fuelCol = cfg.phase3.fuelOrbBeaconColor.getEffectiveColour();
            int fuelColor = fuelCol.getRGB();
            for (var v : fuels)
                BeamRenderer.drawBeam(ctx, v.x + 0.5, v.y, v.z + 1.5, 256f, 0.15f, fuelColor);

            // Fuel orb pull circle
            if (cfg.phase3.fuelOrbPullCircle) {
                for (var v : fuels) {
                    boolean inRange = isBobberInsideRange(v);
                    float cr = inRange ? 0f : fuelCol.getRed() / 255f;
                    float cg = 1f;
                    float cb = inRange ? 0f : fuelCol.getBlue() / 255f;
                    WorldRenderUtils.drawCircle(ctx, v.x + 0.5, v.y, v.z + 1.5, (float)SUPPLY_PULL_RADIUS,
                            cr, cg, cb, 0.6f, true, 3f);
                }
            }

            // Fuel interaction zone (invisible zombies) — 独立颜色，范围内全透明、范围外减半
            if (cfg.phase3.fuelInteractionZone) {
                float[] fzc = argbToFloats(cfg.phase3.fuelZombieBoxColor.getEffectiveColourRGB());
                for (var z : fuelZombies) {
                    if (z.isRemoved()) continue;
                    var cp = Minecraft.getInstance().player;
                    double dist = cp != null ? cp.distanceTo(z) : 99;
                    boolean inRange = dist <= 3;
                    AABB bb = z.getBoundingBox();
                    float zAlpha = inRange ? fzc[3] : fzc[3] * 0.5f;
                    WorldRenderUtils.drawFilledBox(ctx,
                            bb.minX, bb.minY, bb.minZ, bb.maxX, bb.maxY, bb.maxZ,
                            fzc[0], fzc[1], fzc[2], zAlpha, true);
                }
            }

            // Chuck beams + ground circles
            int chuckColor = cfg.phase3.chuckBeaconColor.getEffectiveColour().getRGB();
            for (var c : chucks) {
                var v = c.pos();
                BeamRenderer.drawBeam(ctx, v.x, v.y, v.z, 256f, 0.15f, chuckColor);
                WorldRenderUtils.drawCircle(ctx, v.x, v.y + 0.01, v.z, KuudraLocationTracker.p4 || "p4".equals(KuudraLocationTracker.area) ? 10 : 20, 1, 0, 0, 1, true, 3);
            }

            // Unclassified beams (dropoff markers, ballista, etc.)
            for (var b : beams)
                BeamRenderer.drawBeam(ctx, b.x, b.y, b.z, b.h, 0.15f, new Color(b.r, b.g, b.b, b.a).getRGB());

            // 文字最后渲染，避免被信标光柱遮挡
            for (var t : textEntries.values())
                WorldTextRenderer.renderString(ctx, t.text, t.x, t.y, t.z, t.color, 0.08f, true);
        });

        // fish up 即进入 P1 补给阶段（无需等计分板切换）；收集完毕（P2 开始）后退出
        ClientReceiveMessageEvents.ALLOW_GAME.register((message, overlay) -> {
            if (overlay) return true;
            if (!ModConfigManager.get().kuudra.phase1.supplyBeacons
                    && !ModConfigManager.get().kuudra.phase1.supplyInteractionZone
                    && !ModConfigManager.get().kuudra.phase1.supplyGiantHitbox
                    && !ModConfigManager.get().kuudra.phase1.supplyPullCircle) return true;
            if (!HypixelLocationTracker.getInstance().isInKuudra()) return true;
            String text = ChatUtils.stripColor(message.getString());
            if (KuudraChatLines.isFishUpKuudra(text)) {
                p1SupplyPhaseActive = true;
            } else if (KuudraChatLines.isSuppliesCollected(text)) {
                p1SupplyPhaseActive = false;
            }
            return true;
        });
        // 换图/离开副本兜底清理
        ClientLevelEvents.AFTER_CLIENT_LEVEL_CHANGE.register((_, _) -> p1SupplyPhaseActive = false);

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            var cfg = ModConfigManager.get().kuudra;
            var mm = cfg.minimap;
            boolean anyOn = cfg.phase1.supplyBeacons
                    || cfg.phase1.supplyInteractionZone || cfg.phase1.supplyPullCircle
                    || cfg.phase2.ballistaProgressText || cfg.phase2.ballistaBuildBeacons
                    || cfg.phase2.ballistaProximityCircles
                    || cfg.phase3.fuelOrbBeacons || cfg.phase3.fuelOrbPullCircle
                    || cfg.phase3.chuckBeacons || cfg.phase3.fuelInteractionZone
                    || cfg.phase1.supplyGiantHitbox
                    || mm.enabled;
            if (!anyOn) return;
            if (!HypixelLocationTracker.getInstance().isInKuudra()) return;
            if (client.player == null || client.player.tickCount % 2 != 0) return; // 2 tick 扫描，补给一出水就显示

            String newPhase = getScoreboardPhase(client);

            // P2 计分板兜底清除（中途进房没收到收集消息时也能停）
            if ("Protect Elle".equals(newPhase)) p1SupplyPhaseActive = false;

            beams.clear();
            supplies.clear();
            ballistaPiles.clear();
            fuels.clear();
            chucks.clear();
            supplyZombies.clear();
            supplyGiants.clear();
            fuelZombies.clear();
            seenKeys.clear();

            // P1 补给：fish up 后即开始标记（不等计分板切换），P2 (Protect Elle) 前一直有效；
            // 计分板 "Rescue supplies" 作为中途进房/消息漏收的兜底
            if (p1SupplyPhaseActive || "Rescue supplies".equals(newPhase)) {
                boolean needZombies = cfg.phase1.supplyInteractionZone;
                boolean needSupplies = mm.enabled && mm.supplies;
                if (cfg.phase1.supplyBeacons || needZombies || cfg.phase1.supplyGiantHitbox || needSupplies) {
                    for (var g : client.player.level().getEntitiesOfClass(Giant.class,
                            new AABB(client.player.blockPosition()).inflate(64), SkullTextures.SUPPLIES::isHoldingThis)) {
                        // crate offset 3.7 at angle (yaw + 130°), Y=75
                        double angleRad = Math.toRadians(g.getYRot() + 130.0f);
                        double cx = g.getX() + (SUPPLY_CRATE_OFFSET * Math.cos(angleRad));
                        double cz = g.getZ() + (SUPPLY_CRATE_OFFSET * Math.sin(angleRad));
                        supplies.add(new Vec3(cx, 75.0, cz));
                        if (cfg.phase1.supplyGiantHitbox) supplyGiants.add(g);
                    }
                }
                if (needZombies) {
                    supplyZombies.addAll(client.player.level().getEntitiesOfClass(Zombie.class,
                            new AABB(client.player.blockPosition()).inflate(64),
                            z -> supplies.stream().anyMatch(s -> z.distanceToSqr(s) < 9)));
                }
            } else if ("Protect Elle".equals(newPhase)) {
                boolean needBallista = mm.enabled && mm.ballista;
                if (cfg.phase2.ballistaBuildBeacons || cfg.phase2.ballistaProgressText || cfg.phase2.ballistaProximityCircles || needBallista) {
                    for (var s : client.player.level().getEntitiesOfClass(
                            net.minecraft.world.entity.decoration.ArmorStand.class,
                            new AABB(client.player.blockPosition()).inflate(64),
                            e -> {
                                String name = ChatUtils.stripColor(e.getName().getString());
                                return name.startsWith("PROGRESS: ") && !name.endsWith("COMPLETE");
                            })) {
                        double x = s.getX(), y = s.getY(), z = s.getZ();
                        ballistaPiles.add(new Vec3(x, y, z)); // 光柱颜色在渲染时按圈内/圈外动态决定
                        if (cfg.phase2.ballistaProgressText) {
                            String[] parts = ChatUtils.stripColor(s.getName().getString()).split(" ");
                            String key = "p2_" + s.getId(); seenKeys.add(key);
                            textEntries.put(key, new TextData(parts.length > 1 ? parts[parts.length - 1] : "",
                                    x, y + 2.2, z, cfg.phase2.ballistaTextColor.getEffectiveColourRGB()));
                        }
                    }
                }
            } else {
                boolean needFuel = mm.enabled && mm.fuel;
                boolean wantFuel = cfg.phase3.fuelOrbBeacons || needFuel;
                // P4（Boss）的球单独控制；其他阶段（Eaten/Stun/DPS/Fuel）用 P3 的开关
                boolean needChuck = mm.enabled && mm.chucks;
                boolean inP4 = KuudraLocationTracker.p4 || "p4".equals(KuudraLocationTracker.area);
                boolean wantChuck = inP4 ? (cfg.phase4.p4ChuckBeacons || needChuck) : (cfg.phase3.chuckBeacons || needChuck);
                if (wantFuel || wantChuck) {
                    for (var g : client.player.level().getEntitiesOfClass(Giant.class,
                            new AABB(client.player.blockPosition()).inflate(64))) {
                        if (wantFuel && SkullTextures.FUEL.isHoldingThis(g)) {
                            double angleRad = Math.toRadians(g.getYRot() + 130.0f);
                            fuels.add(new Vec3(g.getX() + (SUPPLY_CRATE_OFFSET * Math.cos(angleRad)),
                                    75.0,
                                    g.getZ() + (SUPPLY_CRATE_OFFSET * Math.sin(angleRad))));
                        } else if (wantChuck) {
                            SkullTextures kind = null;
                            if (SkullTextures.YELLOWCHUCK.isHoldingThis(g)) kind = SkullTextures.YELLOWCHUCK;
                            else if (SkullTextures.REDCHUCK.isHoldingThis(g)) kind = SkullTextures.REDCHUCK;
                            else if (SkullTextures.PURPLECHUCK.isHoldingThis(g)) kind = SkullTextures.PURPLECHUCK;
                            if (kind != null) {
                                double angleRad = Math.toRadians(g.getYRot() + 130.0f);
                                chucks.add(new Chuck(new Vec3(g.getX() + (SUPPLY_CRATE_OFFSET * Math.cos(angleRad)),
                                        inP4 ? 6 : 78.0,
                                        g.getZ() + (SUPPLY_CRATE_OFFSET * Math.sin(angleRad))), kind));
                            }
                        }
                    }
                }
                if (cfg.phase3.fuelInteractionZone) {
                    fuelZombies.addAll(client.player.level().getEntitiesOfClass(Zombie.class,
                            new AABB(client.player.blockPosition()).inflate(64),
                            z -> fuels.stream().anyMatch(s -> z.distanceToSqr(s) < 9)));
                }
            }

            textEntries.keySet().removeIf(k -> !seenKeys.contains(k));
        });
    }

    // ── 小地图数据入口(KuudraMinimap 复用,tick 填充/渲染读取均在主线程) ──
    public static List<Vec3> getSupplies() { return supplies; }
    public static List<Vec3> getBallistaPiles() { return ballistaPiles; }
    public static List<Vec3> getFuels() { return fuels; }
    public static List<Chuck> getChucks() { return chucks; }

    /** Check if the player's fishing bobber is within pull range of a position (crate center x+0.5, z+1.5). */
    private static boolean isBobberInsideRange(Vec3 pos) {
        var player = Minecraft.getInstance().player;
        if (player == null) return false;
        FishingHook bobber = player.fishing;
        if (bobber == null) return false;
        Vec3 bp = bobber.position();
        double dx = bp.x - (pos.x + 0.5);
        double dz = bp.z - (pos.z + 1.5);
        if ((dx * dx) + (dz * dz) > SUPPLY_PULL_RADIUS * SUPPLY_PULL_RADIUS) return false;
        return bp.y >= pos.y - SUPPLY_VERTICAL_MARGIN && bp.y <= pos.y + SUPPLY_VERTICAL_MARGIN;
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
            String text = SUFFIX_PAREN_PATTERN.matcher(ChatUtils.stripColor(ChatUtils.removeEmoji(
                    team.getPlayerPrefix().getString() + team.getPlayerSuffix().getString())))
                    .replaceAll("");
            if (text.equals("Rescue supplies") || text.equals("Protect Elle")) return text;
        }
        return "";
    }
}
