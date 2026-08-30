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
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.state.gui.BlitRenderState;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.monster.Ghast;
import net.minecraft.world.entity.monster.MagmaCube;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3x2f;
import top.babyzombie.addons.config.ModConfig;
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
 * 标记:自己/队友(原版地图样式箭头;头像模式为双层皮肤头像)、Kuudra 本体
 * (原版岩浆怪材质裁剪+不透明化)、触手("৫")、Dropship(原版 TNT)、
 * 补给箱/燃料箱(项目头颅纹理,与世界内识别同源)、三色球(各自头颅纹理)、
 * 两门固定小炮(自定义图片图标,坐标写死)。注册 HudManager 支持拖动/缩放。
 */
public final class KuudraMinimap {
    private KuudraMinimap() {}

    // ── 固定视图:整个 Kuudra 场地范围 ──
    static final double X_MIN = -170, X_MAX = -30, Z_MIN = -176, Z_MAX = -36;

    /** 实体查询范围:整个场地(y 0~124,覆盖全场高低差)。 */
    private static final AABB ARENA_AABB = new AABB(X_MIN, 0, Z_MIN, X_MAX, 124, Z_MAX);

    /** 触手判定:MagmaCube 尺寸 = 此值视为触手(Kuudra 本体恒为 30 自动排除,触手实测 256 血 = size 16)。 */
    private static final int TENTACLE_MAX_SIZE = 16;

    /** 底图资源(assets/babyzombieaddons/textures/gui/kuudra_minimap.png,整图对应整个场地范围)。 */
    private static final Identifier BASE_TEX_ID =
            Identifier.fromNamespaceAndPath("babyzombieaddons", "textures/gui/kuudra_minimap.png");
    /** 岩浆怪图标资源(assets/babyzombieaddons/textures/gui/kuudra_minimap_magma.png,用户提供的图片)。 */
    private static final Identifier MAGMA_ICON_RES_ID =
            Identifier.fromNamespaceAndPath("babyzombieaddons", "textures/gui/kuudra_minimap_magma.png");
    private static final Identifier MAGMA_ICON_TEX_ID =
            Identifier.fromNamespaceAndPath("babyzombieaddons", "kuudra_minimap_magma");
    /** 原版地图玩家箭头图标(8x8 白色,可染色)。 */
    private static final Identifier PLAYER_ARROW_RES_ID =
            Identifier.fromNamespaceAndPath("minecraft", "textures/map/decorations/player.png");
    private static final Identifier PLAYER_ARROW_TEX_ID =
            Identifier.fromNamespaceAndPath("babyzombieaddons", "kuudra_minimap_player_arrow");
    /** 固定小炮图标资源(assets/babyzombieaddons/textures/gui/kuudra_minimap_cannon.png)。 */
    private static final Identifier CANNON_TEX_ID =
            Identifier.fromNamespaceAndPath("babyzombieaddons", "textures/gui/kuudra_minimap_cannon.png");
    /** Kuudra 本体标记直径:size 30 岩浆怪占地 0.51*30 ≈ 15.3 格,按默认图 160px/140 格折算 ≈ 17px。 */
    private static final int KUUDRA_ICON_PX = 17;

    private static final int COLOR_BG = 0xB00A0A0C;
    private static final int COLOR_BORDER = 0xFF555555;

    // ── 字符图标 ──
    private static final String ICON_TENTACLE = "§l৫";  // 触手

    // ── 图标尺寸(逻辑像素,item 基准 16x16) ──
    private static final int ICON_PX_HEAD = 10;
    private static final int ICON_PX_TNT = 10;
    private static final int ICON_PX_CANNON = 10;
    /** 玩家箭头显示边长(原版图标 8x8,放大到 9px 标记)。 */
    private static final int ARROW_PX = 9;

    // ── 快照数据(tick 写 / 渲染读,均在主线程) ──
    private record Teammate(double x, double z, float yaw, String name, Identifier skinTex) {}
    private record KuudraSpot(double x, double z) {}
    private static final List<Teammate> teammates = new ArrayList<>();
    private static final List<Vec3> tentacles = new ArrayList<>();
    private static final List<Vec3> dropships = new ArrayList<>();
    /** Kuudra 本体位置快照(全场扫描,不依赖玩家位置)。 */
    private static KuudraSpot kuudraSpot;
    private record CannonSpot(String name, double x, double z) {}
    /** 两门固定小炮(位置恒定,写死)。 */
    private static final List<CannonSpot> CANNONS = List.of(
            new CannonSpot("Cannon 1", -131, -112),
            new CannonSpot("Cannon 2", -70, -103)
    );

    // ── 纹理状态 ──
    private static NativeImage baseImage;
    private static DynamicTexture baseTexture;
    private static boolean baseMapFailed;
    private static DynamicTexture magmaIconTexture;
    private static DynamicTexture playerArrowTexture;
    private static DynamicTexture cannonTexture;

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
            // 离开 Kuudra 清实体快照;底图/图标纹理保留复用
            teammates.clear();
            tentacles.clear();
            dropships.clear();
            kuudraSpot = null;
            return;
        }
        if (client.player == null || client.level == null) return;

        ensureBaseMap(client);
        if (client.player.tickCount % 2 == 0) snapshotEntities(client);
    }

    // ─────────────────────────── 底图与图标纹理 ───────────────────────────

    /** 从内置资源加载底图 PNG(一次性;资源缺失时只画背景+标记层)。 */
    private static void ensureBaseMap(Minecraft client) {
        if (baseImage != null || baseMapFailed) return;
        try (var in = client.getResourceManager().open(BASE_TEX_ID)) {
            baseImage = NativeImage.read(in.readAllBytes());
        } catch (Exception e) {
            baseMapFailed = true;
        }
    }

    /** 渲染线程懒建纹理:底图一次注册;岩浆怪图标(用户提供的 PNG)、箭头一次生成/加载。 */
    private static void ensureTexture(Minecraft client) {
        var tm = client.getTextureManager();

        if (baseTexture == null && baseImage != null && !baseImage.isClosed()) {
            baseTexture = new DynamicTexture(() -> "bza_kuudra_minimap", baseImage);
            baseTexture.upload();
            tm.register(BASE_TEX_ID, baseTexture);
        }

        // 岩浆怪图标(用户提供的 PNG,直接渲染,一次)
        if (magmaIconTexture == null) {
            try (var in = client.getResourceManager().open(MAGMA_ICON_RES_ID)) {
                var img = NativeImage.read(in.readAllBytes());
                var tex = new DynamicTexture(() -> "bza_kuudra_magma", img);
                tex.upload();
                tm.register(MAGMA_ICON_TEX_ID, tex);
                magmaIconTexture = tex;
            } catch (Exception ignored) {}
        }

        // 原版地图玩家箭头图标(8x8 白色,加载即用,一次)
        if (playerArrowTexture == null) {
            try (var in = client.getResourceManager().open(PLAYER_ARROW_RES_ID)) {
                var img = NativeImage.read(in.readAllBytes());
                var tex = new DynamicTexture(() -> "bza_kuudra_player_arrow", img);
                tex.upload();
                tm.register(PLAYER_ARROW_TEX_ID, tex);
                playerArrowTexture = tex;
            } catch (Exception ignored) {}
        }

        // 固定小炮图标(用户提供的 PNG,一次)
        if (cannonTexture == null) {
            try (var in = client.getResourceManager().open(CANNON_TEX_ID)) {
                var img = NativeImage.read(in.readAllBytes());
                var tex = new DynamicTexture(() -> "bza_kuudra_cannon", img);
                tex.upload();
                tm.register(CANNON_TEX_ID, tex);
                cannonTexture = tex;
            } catch (Exception ignored) {}
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
        kuudraSpot = null;

        // 队友:PartyTracker(ModAPI)UUID 匹配,同 TeamHighlight 判定
        var info = PartyTracker.getInstance().getLastInfo();
        if (info != null) {
            for (var p : level.players()) {
                if (p == self || !info.members().contains(p.getUUID())) continue;
                Identifier skin = null;
                try {
                    skin = p.getSkin().body().texturePath();
                } catch (Exception ignored) {}
                teammates.add(new Teammate(p.getX(), p.getZ(), p.getYRot(),
                        p.getGameProfile().name(), skin));
            }
        }

        // Kuudra 本体:全场扫 size >= 30 的 MagmaCube(不依赖玩家位置),取最高者
        if (cfg.showKuudra) {
            var cubes = level.getEntitiesOfClass(MagmaCube.class, ARENA_AABB, m -> m.getSize() >= 30);
            if (!cubes.isEmpty()) {
                cubes.sort((a, b) -> Double.compare(b.getY(), a.getY()));
                var k = cubes.getFirst();
                kuudraSpot = new KuudraSpot(k.getX(), k.getZ());
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
            ensureTexture(client);
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
        if (baseTexture != null) {
            try {
                // blit 参数 (x0, y0, x1, y1, u0, u1, v0, v1):对角点 + UV
                g.blit(baseTexture.getTextureView(),
                        RenderSystem.getSamplerCache().getRepeat(FilterMode.LINEAR),
                        0, 0, size, size, 0F, 1F, 0F, 1F);
            } catch (Exception ignored) {}
        }

        // 两门固定小炮(自定义图标,可染色)
        if (cfg.cannons) {
            int cannonColor = cfg.cannonColor.getEffectiveColourRGB();
            for (var c : CANNONS) drawCannon(g, c.x(), c.z(), size, cannonColor);
        }

        // 放置点(piles):未放/已放颜色都可配
        if (cfg.piles) {
            var completed = KuudraPileWaypoints.getCompletedPiles();
            var piles = KuudraPileWaypoints.getPiles();
            int pileColor = cfg.pileColor.getEffectiveColourRGB();
            int doneColor = cfg.pileDoneColor.getEffectiveColourRGB();
            for (int i = 0; i < piles.size(); i++) {
                var p = piles.get(i);
                drawMark(g, p.x() + 0.5, p.z() + 0.5, size,
                        completed.contains(i) ? doneColor : pileColor);
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
            int color = cfg.ballistaColor.getEffectiveColourRGB();
            for (var v : KuudraWaypoints.getBallistaPiles()) drawMark(g, v.x, v.z, size, color);
        }
        if (cfg.chucks) {
            for (var c : KuudraWaypoints.getChucks()) {
                drawItem(g, headIcon(c.kind()), c.pos().x, c.pos().z, size, ICON_PX_HEAD);
            }
        }

        // Kuudra 本体:自绘岩浆怪图标,固定尺寸(写死)
        if (cfg.showKuudra && kuudraSpot != null) {
            drawMagmacube(g, kuudraSpot.x(), kuudraSpot.z(), size);
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

        // 自己(原版地图箭头 / 头像模式为头像+朝向线,最上层)
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

    /** 岩浆怪图标(用户提供的图片,直接渲染),固定尺寸。 */
    private static void drawMagmacube(GuiGraphicsExtractor g, double x, double z, int size) {
        if (magmaIconTexture == null || magmaIconTexture.getTextureView() == null) return;
        float fx = mapX(x, size), fy = mapY(z, size);
        // 尺寸写死:基准 17px @ 默认图 160px,随 cfg.size 等比缩放
        int px = Math.max(8, KUUDRA_ICON_PX * size / 160);
        int x0 = Math.round(fx - px / 2f), y0 = Math.round(fy - px / 2f);
        try {
            // blit 参数 (x0, y0, x1, y1, u0, u1, v0, v1):对角点 + UV
            g.blit(magmaIconTexture.getTextureView(),
                    RenderSystem.getSamplerCache().getRepeat(FilterMode.NEAREST),
                    x0, y0, x0 + px, y0 + px, 0F, 1F, 0F, 1F);
        } catch (Exception ignored) {}
    }

    /** 固定小炮图标:带顶点色的 blit(白色底图可染任意色)。 */
    private static void drawCannon(GuiGraphicsExtractor g, double x, double z, int size, int color) {
        if (cannonTexture == null || cannonTexture.getTextureView() == null) return;
        float fx = mapX(x, size), fy = mapY(z, size);
        int px = ICON_PX_CANNON;
        int x0 = Math.round(fx - px / 2f), y0 = Math.round(fy - px / 2f);
        try {
            g.guiRenderState.addGuiElement(new BlitRenderState(
                    RenderPipelines.GUI_TEXTURED,
                    TextureSetup.singleTexture(cannonTexture.getTextureView(),
                            RenderSystem.getSamplerCache().getRepeat(FilterMode.NEAREST)),
                    new Matrix3x2f(g.pose()),
                    x0, y0, x0 + px, y0 + px, 0F, 1F, 0F, 1F, color, null));
        } catch (Exception ignored) {}
    }

    /** 原版地图玩家箭头:绕中心按 yaw 旋转 + 顶点色染色(白图可染任意色),Yaw 0=南=屏幕上方。 */
    private static void drawPlayerArrow(GuiGraphicsExtractor g, float fx, float fy, float yaw, int color) {
        if (playerArrowTexture == null || playerArrowTexture.getTextureView() == null) return;
        int half = ARROW_PX / 2;
        var pose = g.pose();
        pose.pushMatrix();
        pose.translate(fx, fy);
        pose.rotate((float) Math.toRadians(yaw));
        try {
            g.guiRenderState.addGuiElement(new BlitRenderState(
                    RenderPipelines.GUI_TEXTURED,
                    TextureSetup.singleTexture(playerArrowTexture.getTextureView(),
                            RenderSystem.getSamplerCache().getRepeat(FilterMode.NEAREST)),
                    new Matrix3x2f(g.pose()),
                    -half, -half, half, half, 0F, 1F, 0F, 1F, color, null));
        } catch (Exception ignored) {}
        pose.popMatrix();
    }

    private static void drawTeammates(GuiGraphicsExtractor g, Minecraft client,
                                      top.babyzombie.addons.config.KuudraConfig.MinimapCfg cfg, int size) {
        Font font = client.font;
        int color = cfg.teammateColor.getEffectiveColourRGB();
        for (var t : teammates) {
            float fx = mapX(t.x(), size), fy = mapY(t.z(), size);
            int x0 = Math.round(fx) - 1, y0 = Math.round(fy) - 1;
            switch (cfg.teammateStyle) {
                // 非头像模式:原版地图玩家箭头(带方向,队友色)
                case DOT -> drawPlayerArrow(g, fx, fy, t.yaw(), color);
                case DOT_NAME -> {
                    drawPlayerArrow(g, fx, fy, t.yaw(), color);
                    g.text(font, t.name(), Math.round(fx) + 5, Math.round(fy) - 4, color, true);
                }
                case HEAD -> {
                    if (!drawSkinHead(g, client, t.skinTex(), fx, fy, t.yaw(), cfg.headRotate)) {
                        g.fill(x0, y0, x0 + 3, y0 + 3, color);
                    }
                }
            }
        }
    }

    /**
     * 皮肤头像:底层脸 + 双层皮肤 overlay 脸(40,8)-(48,16)叠加;
     * 可按 yaw 绕头像中心旋转跟随朝向(rotate=false 时固定朝上)。失败返回 false。
     */
    private static boolean drawSkinHead(GuiGraphicsExtractor g, Minecraft client, Identifier skinTex,
                                        float cx, float cy, float yaw, boolean rotate) {
        if (skinTex == null) return false;
        try {
            var pose = g.pose();
            pose.pushMatrix();
            pose.translate(cx, cy);
            if (rotate) pose.rotate((float) Math.toRadians(yaw));
            boolean ok = blitSkinHead(g, client, skinTex);
            pose.popMatrix();
            return ok;
        } catch (Exception e) {
            return false;
        }
    }

    /** 底层脸 + overlay 层(无 overlay 的皮肤该区域透明,自动无效果);纹理异常返回 false。 */
    private static boolean blitSkinHead(GuiGraphicsExtractor g, Minecraft client, Identifier skinTex) {
        try {
            var view = client.getTextureManager().getTexture(skinTex).getTextureView();
            var sampler = RenderSystem.getSamplerCache().getRepeat(FilterMode.NEAREST);
            // blit 参数 (x0, y0, x1, y1, u0, u1, v0, v1):对角点 + UV;中心为 (0,0) 已在调用方平移
            g.blit(view, sampler, -4, -4, 4, 4, 8 / 64f, 16 / 64f, 8 / 64f, 16 / 64f);
            g.blit(view, sampler, -4, -4, 4, 4, 40 / 64f, 48 / 64f, 8 / 64f, 16 / 64f);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private static void drawSelf(GuiGraphicsExtractor g, Minecraft client,
                                 top.babyzombie.addons.config.KuudraConfig.MinimapCfg cfg, int size) {
        var p = client.player; // LocalPlayer:getSkin() 在 AbstractClientPlayer
        if (p == null) return;
        float fx = mapX(p.getX(), size), fy = mapY(p.getZ(), size);

        // 头像模式:自己显示皮肤头像,可旋转跟随朝向(不再画方向线)
        if (cfg.teammateStyle == ModConfig.KuudraMinimapTeammateStyle.HEAD) {
            Identifier skin = null;
            try {
                skin = p.getSkin().body().texturePath();
            } catch (Exception ignored) {}
            if (drawSkinHead(g, client, skin, fx, fy, p.getYRot(), cfg.headRotate)) {
                return;
            }
        }

        // 其他模式:原版地图玩家箭头(朝向旋转,自己颜色)
        drawPlayerArrow(g, fx, fy, p.getYRot(), cfg.selfColor.getEffectiveColourRGB());
    }
}