package top.babyzombie.addons.module.kuudra;

import com.google.common.collect.LinkedHashMultimap;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.properties.PropertyMap;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.monster.Ghast;
import net.minecraft.world.entity.monster.cubemob.MagmaCube;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import top.babyzombie.addons.config.ModConfigManager;
import top.babyzombie.addons.config.hud.HudManager;
import top.babyzombie.addons.util.tracker.HypixelLocationTracker;
import top.babyzombie.addons.util.tracker.PartyTracker;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Kuudra 小地图 - 固定全图俯视图(不旋转不移动):
 * 左上 = (x, z) 最大,右下 = (x, z) 最小。场地范围 (-170, -176) ~ (-30, -36)。
 * 底图为内置资源 PNG(assets/babyzombieaddons/textures/gui/kuudra_minimap.png)。
 * 标记图标:自己(朝向箭头)、队友(点/名字/头像)、Kuudra 本体(原版岩浆怪材质,
 * 仅岩浆怪形态显示)、触手("৫")、Dropship(原版 TNT)、补给箱/燃料箱(项目头颅纹理,
 * 与世界内识别同源)、三色球(各自头颅纹理)、两门固定小炮(坐标写死)。
 * 注册 HudManager 支持拖动/缩放。
 */
public final class KuudraMinimap {
    private KuudraMinimap() {}

    // ── 固定视图:整个 Kuudra 场地范围 ──
    static final double X_MIN = -170, X_MAX = -30, Z_MIN = -176, Z_MAX = -36;

    /** 实体查询范围:整个场地(y 0~124,覆盖全场高低差)。 */
    private static final AABB ARENA_AABB = new AABB(X_MIN, 0, Z_MIN, X_MAX, 124, Z_MAX);

    /** 触手判定:MagmaCube 尺寸 <= 此值视为触手(Kuudra 本体恒为 30 自动排除,触手实测 256 血 = size 16)。 */
    private static final int TENTACLE_MAX_SIZE = 16;

    /** 底图资源(assets/babyzombieaddons/textures/gui/kuudra_minimap.png,整图对应整个场地范围)。 */
    private static final Identifier BASE_TEX_ID =
            Identifier.fromNamespaceAndPath("babyzombieaddons", "textures/gui/kuudra_minimap.png");
    /** 原版岩浆怪实体纹理(左上 8x8 为外层面皮肤)。 */
    private static final Identifier MAGMACUBE_RES_ID =
            Identifier.fromNamespaceAndPath("minecraft", "textures/entity/slime/magmacube.png");
    private static final Identifier MAGMACUBE_TEX_ID =
            Identifier.fromNamespaceAndPath("babyzombieaddons", "kuudra_minimap_magmacube");

    private static final int COLOR_BG = 0xB00A0A0C;
    private static final int COLOR_BORDER = 0xFF555555;
    // 阶段目标物固定语义色(方块/字符标记染色)
    private static final int COLOR_PILE = 0xFFE5E52E;
    private static final int COLOR_PILE_DONE = 0xFF555555;
    private static final int COLOR_BALLISTA = 0xFFE5E52E;
    private static final int COLOR_CANNON = 0xFFC8C8D0;

    // ── 字符图标 ──
    private static final String ICON_TENTACLE = "৫";  // 触手
    private static final String ICON_CANNON = "💣";   // 固定小炮

    // ── 图标尺寸(逻辑像素,item 基准 16x16) ──
    private static final int ICON_PX_KUUDRA = 13;
    private static final int ICON_PX_HEAD = 10;
    private static final int ICON_PX_TNT = 10;

    // ── 快照数据(tick 写 / 渲染读,均在主线程) ──
    private record Teammate(double x, double z, String name, Identifier skinTex) {}
    private static final List<Teammate> teammates = new ArrayList<>();
    private static final List<Vec3> tentacles = new ArrayList<>();
    private static final List<Vec3> dropships = new ArrayList<>();
    private record CannonSpot(String name, double x, double z) {}
    /** 两门固定小炮(位置恒定,写死)。 */
    private static final List<CannonSpot> CANNONS = List.of(
            new CannonSpot("Cannon 1", -131, -112),
            new CannonSpot("Cannon 2", -70, -103)
    );

    // ── 底图/纹理状态 ──
    private static NativeImage baseImage;
    private static DynamicTexture baseTexture;
    private static boolean textureReady;
    private static boolean baseMapFailed;
    private static DynamicTexture magmacubeTexture;
    private static boolean magmacubeReady;

    // ── 头颅图标缓存(懒建) ──
    private static final Map<KuudraWaypoints.SkullTextures, ItemStack> HEAD_ICONS = new HashMap<>();
    private static ItemStack tntIcon;

    public static void init() {
        HudElementRegistry.attachElementAfter(VanillaHudElements.OVERLAY_MESSAGE,
                Identifier.fromNamespaceAndPath("babyzombieaddons", "kuudra_minimap"),
                (context, tickCounter) -> drawHud(context));

        ClientTickEvents.END_CLIENT_TICK.register(KuudraMinimap::tick);
    }

    private static void tick(Minecraft client) {
        var cfg = ModConfigManager.get().kuudra.minimap;
        if (!cfg.enabled) return;
        if (!HypixelLocationTracker.getInstance().isInKuudra()) {
            // 离开 Kuudra 清实体快照;底图资源/纹理保留复用
            teammates.clear();
            tentacles.clear();
            dropships.clear();
            return;
        }
        if (client.player == null || client.level == null) return;

        ensureBaseMap(client);
        if (client.player.tickCount % 2 == 0) snapshotEntities(client);
    }

    // ─────────────────────────── 底图与纹理 ───────────────────────────

    /** 从内置资源加载底图 PNG(一次性;资源缺失时只画背景+标记层)。 */
    private static void ensureBaseMap(Minecraft client) {
        if (baseImage != null || baseMapFailed) return;
        try (var in = client.getResourceManager().open(BASE_TEX_ID)) {
            baseImage = NativeImage.read(in.readAllBytes());
        } catch (Exception e) {
            baseMapFailed = true;
        }
    }

    /** 渲染线程懒建纹理。 */
    private static void ensureTexture(Minecraft client) {
        if (!textureReady && baseImage != null && !baseImage.isClosed()) {
            var tm = client.getTextureManager();
            if (baseTexture != null) {
                try {
                    tm.release(BASE_TEX_ID);
                    baseTexture.close();
                } catch (Exception ignored) {}
                baseTexture = null;
            }
            baseTexture = new DynamicTexture(() -> "bza_kuudra_minimap", baseImage);
            baseTexture.upload();
            tm.register(BASE_TEX_ID, baseTexture);
            textureReady = true;
        }
        if (!magmacubeReady) {
            try (var in = client.getResourceManager().open(MAGMACUBE_RES_ID)) {
                var img = NativeImage.read(in.readAllBytes());
                var tex = new DynamicTexture(() -> "bza_kuudra_magmacube", img);
                tex.upload();
                client.getTextureManager().register(MAGMACUBE_TEX_ID, tex);
                magmacubeTexture = tex;
            } catch (Exception ignored) {}
            magmacubeReady = true;
        }
    }

    /** 头颅图标 ItemStack(与 KuudraWaypoints 世界内识别同一份 base64,懒建缓存)。 */
    private static ItemStack headIcon(KuudraWaypoints.SkullTextures kind) {
        return HEAD_ICONS.computeIfAbsent(kind, k -> {
            var stack = new ItemStack(Items.PLAYER_HEAD);
            var multimap = LinkedHashMultimap.<String, Property>create();
            multimap.put("textures", new Property("textures", k.texture(), null));
            var gp = new GameProfile(
                    UUID.nameUUIDFromBytes(k.texture().getBytes(StandardCharsets.UTF_8)), "", new PropertyMap(multimap));
            stack.set(DataComponents.PROFILE, ResolvableProfile.createResolved(gp));
            return stack;
        });
    }

    /** 原版 TNT 物品图标(懒建)。 */
    private static ItemStack tntIcon() {
        if (tntIcon == null) tntIcon = new ItemStack(Items.TNT);
        return tntIcon;
    }

    // ─────────────────────────── 实体快照 ───────────────────────────

    private static void snapshotEntities(Minecraft client) {
        var cfg = ModConfigManager.get().kuudra.minimap;
        var level = client.level;
        var self = client.player;
        if (level == null || self == null) return;
        teammates.clear();
        tentacles.clear();
        dropships.clear();

        // 队友:PartyTracker(ModAPI)UUID 匹配,同 TeamHighlight 判定
        var info = PartyTracker.getInstance().getLastInfo();
        if (info != null) {
            for (var p : level.players()) {
                if (p == self || !info.members().contains(p.getUUID())) continue;
                Identifier skin = null;
                try {
                    skin = p.getSkin().body().texturePath();
                } catch (Exception ignored) {}
                teammates.add(new Teammate(p.getX(), p.getZ(),
                        p.getGameProfile().name(), skin));
            }
        }

        // 触手:MagmaCube(尺寸 = TENTACLE_MAX_SIZE)
        if (cfg.tentacles) {
            for (var m : level.getEntitiesOfClass(MagmaCube.class,
                    ARENA_AABB, m -> m.getSize() == TENTACLE_MAX_SIZE)) {
                tentacles.add(m.position());
            }
        }

        // Dropship:Ghast 实体
        if (cfg.dropships) {
            for (var g : level.getEntitiesOfClass(Ghast.class, ARENA_AABB)) {
                dropships.add(g.position());
            }
        }
    }

    // ─────────────────────────── 渲染 ───────────────────────────

    private static void drawHud(GuiGraphicsExtractor context) {
        if (!HudManager.shouldShow("KuudraMinimap")) return;
        var client = Minecraft.getInstance();
        if (client.player == null || !HypixelLocationTracker.getInstance().isInKuudra()) return;
        var cfg = ModConfigManager.get().kuudra.minimap;

        int x = HudManager.x("KuudraMinimap"), y = HudManager.y("KuudraMinimap");
        float s = HudManager.scale("KuudraMinimap");

        var pose = context.pose();
        pose.pushMatrix();
        pose.translate((float) x, (float) y);
        pose.scale(s, s);
        try {
            renderMap(context, client, cfg, cfg.size);
        } finally {
            pose.popMatrix();
        }
    }

    private static void renderMap(GuiGraphicsExtractor g, Minecraft client,
                                  top.babyzombie.addons.config.KuudraConfig.MinimapCfg cfg, int size) {
        Font font = client.font;

        // 背景与底图(整图 = 整个场地范围,拉伸到 size;缩小用 LINEAR 平滑采样)
        g.fill(0, 0, size, size, COLOR_BG);
        ensureTexture(client);
        if (baseTexture != null) {
            try {
                g.blit(baseTexture.getTextureView(),
                        RenderSystem.getSamplerCache().getRepeat(FilterMode.LINEAR),
                        0, 0, size, size, 0F, 0F, 1F, 1F);
            } catch (Exception ignored) {}
        }

        // 两门固定小炮(json 坐标)
        if (cfg.cannons) {
            for (var c : CANNONS) drawGlyph(g, font, ICON_CANNON, c.x(), c.z(), size, COLOR_CANNON);
        }

        // 放置点(piles):已放补给的变灰
        if (cfg.piles) {
            var completed = KuudraPileWaypoints.getCompletedPiles();
            var piles = KuudraPileWaypoints.getPiles();
            for (int i = 0; i < piles.size(); i++) {
                var p = piles.get(i);
                drawMark(g, p.x() + 0.5, p.z() + 0.5, size,
                        completed.contains(i) ? COLOR_PILE_DONE : COLOR_PILE);
            }
        }

        // 阶段目标物(KuudraWaypoints 数据,仅在对应阶段非空)
        if (cfg.supplies) {
            var icon = headIcon(KuudraWaypoints.SkullTextures.SUPPLIES);
            for (var v : KuudraWaypoints.getSupplies()) drawItem(g, icon, v.x, v.z, size, ICON_PX_HEAD);
        }
        if (cfg.fuel) {
            var icon = headIcon(KuudraWaypoints.SkullTextures.FUEL);
            for (var v : KuudraWaypoints.getFuels()) drawItem(g, icon, v.x, v.z, size, ICON_PX_HEAD);
        }
        if (cfg.ballista) {
            for (var v : KuudraWaypoints.getBallistaPiles()) drawMark(g, v.x, v.z, size, COLOR_BALLISTA);
        }
        if (cfg.chucks) {
            for (var c : KuudraWaypoints.getChucks()) {
                drawItem(g, headIcon(c.kind()), c.pos().x, c.pos().z, size, ICON_PX_HEAD);
            }
        }

        // Kuudra 本体:仅岩浆怪形态(MagmaCube size 30)显示,原版岩浆怪材质
        if (cfg.showKuudra) {
            var e = KuudraLocationTracker.kuudraEntity;
            if (e instanceof MagmaCube && !e.isRemoved()) {
                drawMagmacube(g, e.getX(), e.getZ(), size);
            }
        }

        // 触手(字符)/ Dropship(原版 TNT 物品)
        if (cfg.tentacles) {
            int c = cfg.tentacleColor.getEffectiveColourRGB();
            for (var v : tentacles) drawGlyph(g, font, ICON_TENTACLE, v.x, v.z, size, c);
        }
        if (cfg.dropships) {
            var icon = tntIcon();
            for (var v : dropships) drawItem(g, icon, v.x, v.z, size, ICON_PX_TNT);
        }

        // 队友(点 / 点+名字 / 头像)
        if (cfg.teammates) drawTeammates(g, client, cfg, size);

        // 自己(朝向箭头,最上层)
        drawSelf(g, client, cfg, size);

        // 边框最后画,盖住贴边标记
        g.fill(0, 0, size, 1, COLOR_BORDER);
        g.fill(0, size - 1, size, size, COLOR_BORDER);
        g.fill(0, 0, 1, size, COLOR_BORDER);
        g.fill(size - 1, 0, size, size, COLOR_BORDER);
    }

    /** 世界坐标 -> 地图内局部像素(左上 = x/z 最大)。 */
    private static float mapX(double x, int size) {
        return (float) ((X_MAX - x) / (X_MAX - X_MIN) * size);
    }

    private static float mapY(double z, int size) {
        return (float) ((Z_MAX - z) / (Z_MAX - Z_MIN) * size);
    }

    /** 单色方块标记(放置点/建造点)。 */
    private static void drawMark(GuiGraphicsExtractor g, double x, double z, int size, int color) {
        int x0 = Math.round(mapX(x, size)) - 1, y0 = Math.round(mapY(z, size)) - 1;
        g.fill(x0, y0, x0 + 3, y0 + 3, color);
    }

    /** 字符图标:居中画一个字形(触手/炮)。 */
    private static void drawGlyph(GuiGraphicsExtractor g, Font font, String glyph,
                                  double x, double z, int size, int color) {
        float fx = mapX(x, size), fy = mapY(z, size);
        g.text(font, glyph, Math.round(fx - font.width(glyph) / 2f),
                Math.round(fy - font.lineHeight / 2f), color, false);
    }

    /** 物品图标:居中画 ItemStack(头颅/TNT),px 为目标边长(item 基准 16x16)。 */
    private static void drawItem(GuiGraphicsExtractor g, ItemStack stack,
                                 double x, double z, int size, int px) {
        float fx = mapX(x, size), fy = mapY(z, size);
        var pose = g.pose();
        pose.pushMatrix();
        pose.translate(fx - px / 2f, fy - px / 2f);
        float s = px / 16f;
        pose.scale(s, s);
        g.item(stack, 0, 0);
        pose.popMatrix();
    }

    /** 原版岩浆怪材质:blit magmacube.png 左上 8x8 外层面皮肤,居中显示。 */
    private static void drawMagmacube(GuiGraphicsExtractor g, double x, double z, int size) {
        if (magmacubeTexture == null) return;
        float fx = mapX(x, size), fy = mapY(z, size);
        int px = ICON_PX_KUUDRA;
        try {
            g.blit(magmacubeTexture.getTextureView(),
                    RenderSystem.getSamplerCache().getRepeat(FilterMode.NEAREST),
                    Math.round(fx - px / 2f), Math.round(fy - px / 2f), px, px,
                    0F, 0F, 8 / 64f, 8 / 64f);
        } catch (Exception ignored) {}
    }

    private static void drawTeammates(GuiGraphicsExtractor g, Minecraft client,
                                      top.babyzombie.addons.config.KuudraConfig.MinimapCfg cfg, int size) {
        Font font = client.font;
        int color = cfg.teammateColor.getEffectiveColourRGB();
        for (var t : teammates) {
            float fx = mapX(t.x(), size), fy = mapY(t.z(), size);
            int x0 = Math.round(fx) - 1, y0 = Math.round(fy) - 1;
            switch (cfg.teammateStyle) {
                case DOT -> g.fill(x0, y0, x0 + 3, y0 + 3, color);
                case DOT_NAME -> {
                    g.fill(x0, y0, x0 + 3, y0 + 3, color);
                    g.text(font, t.name(), Math.round(fx) + 3, Math.round(fy) - 4, color, true);
                }
                case HEAD -> {
                    if (!drawHead(g, client, t, Math.round(fx) - 4, Math.round(fy) - 4)) {
                        g.fill(x0, y0, x0 + 3, y0 + 3, color);
                    }
                }
            }
        }
    }

    /** 皮肤脸部 8x8 区域 blit;纹理异常时返回 false 走色点兜底。 */
    private static boolean drawHead(GuiGraphicsExtractor g, Minecraft client, Teammate t, int x, int y) {
        if (t.skinTex() == null) return false;
        try {
            var view = client.getTextureManager().getTexture(t.skinTex()).getTextureView();
            g.blit(view, RenderSystem.getSamplerCache().getRepeat(FilterMode.NEAREST),
                    x, y, 8, 8, 8 / 64f, 8 / 64f, 16 / 64f, 16 / 64f);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private static void drawSelf(GuiGraphicsExtractor g, Minecraft client,
                                 top.babyzombie.addons.config.KuudraConfig.MinimapCfg cfg, int size) {
        Player p = client.player;
        if (p == null) return;
        float fx = mapX(p.getX(), size), fy = mapY(p.getZ(), size);
        int color = cfg.selfColor.getEffectiveColourRGB();

        // 朝向:世界前进方向 (-sin yaw, cos yaw) -> 屏幕 (-dx, -dz)
        double yawRad = Math.toRadians(p.getYRot());
        double sdx = Math.sin(yawRad), sdy = -Math.cos(yawRad);
        for (int i = 3; i >= 0; i--) {
            int r = Math.max(1, 2 - (i == 0 ? 1 : i / 2));
            int px = Math.round(fx + (float) (sdx * i));
            int py = Math.round(fy + (float) (sdy * i));
            g.fill(px - r, py - r, px + r + 1, py + r + 1, color);
        }
    }
}
