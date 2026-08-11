package top.babyzombie.addons.module.fishing;

import com.mojang.blaze3d.GpuFormat;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.textures.AddressMode;
import com.mojang.blaze3d.textures.GpuSampler;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import net.minecraft.client.CameraType;
import net.minecraft.client.CloudStatus;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.TextureFilteringMethod;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.renderer.GlobalSettingsUniform;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.state.gui.BlitRenderState;
import net.minecraft.client.renderer.state.gui.ColoredRectangleRenderState;
import net.minecraft.client.renderer.state.gui.GuiRenderState;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;

import java.util.OptionalDouble;
import top.babyzombie.addons.config.FishingConfig.CameraAspectRatio;
import top.babyzombie.addons.config.FishingConfig.CameraYawMode;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3x2f;
import org.joml.Matrix4f;
import org.jspecify.annotations.Nullable;
import top.babyzombie.addons.config.ModConfigManager;
import top.babyzombie.addons.config.hud.HudManager;
import top.babyzombie.addons.mixin.render.CameraAccessor;
import top.babyzombie.addons.mixin.render.CameraFrustumAccessor;
import top.babyzombie.addons.mixin.render.CameraInvoker;
import top.babyzombie.addons.mixin.render.GameRendererAccessor;
import top.babyzombie.addons.mixin.render.LevelExtractorInvoker;
import top.babyzombie.addons.mixin.render.LevelRendererAccessor;
import top.babyzombie.addons.mixin.render.MainRenderTargetAccessor;
import top.babyzombie.addons.util.render.GlowDepthRenderer;
import top.babyzombie.addons.util.tracker.HypixelLocationTracker;

/// 钓鱼浮标第二相机(可行性验证版)。
/// <p>
/// 在 GameRenderer 世界渲染完成之后,把主渲染目标临时换成独立的小目标,
/// 用鱼漂上方 40° 俯视的临时相机( Marker 实体)重跑 extract + renderLevel,
/// 得到鱼漂特写画面;再由 {@link top.babyzombie.addons.mixin.render.FishingCameraHudMixin}
/// 在 HUD 提取阶段把画面贴到屏幕右下角。
/// <p>
/// 配置开关、HUD 注册、节流等后续再做。
public final class FishingCameraModule {

    /// 特写画面高度(物理像素),宽度按窗口比例,避免投影纵横比拉伸
    private static final int FEED_HEIGHT = 256;
    /// 子相机区块采样器(mag=NEAREST):近距放大时 UV 落在 texel 边界,
    /// LINEAR mag 过滤会混合图集相邻 texel 渗漏出白线;NEAREST 取单 texel 无渗漏,
    /// min 保持 LINEAR 缩小平滑。懒创建复用。
    private static @Nullable GpuSampler feedSampler;

    private static GpuSampler feedSampler() {
        if (feedSampler == null) {
            feedSampler = RenderSystem.getDevice().createSampler(
                    AddressMode.CLAMP_TO_EDGE, AddressMode.CLAMP_TO_EDGE,
                    FilterMode.LINEAR, FilterMode.NEAREST, 1, OptionalDouble.empty());
        }
        return feedSampler;
    }
    /// 是否已加载 Iris(光影):光影接管渲染管线,双相机渲染会导致主画面闪烁
    private static final boolean IRIS_LOADED = FabricLoader.getInstance().isModLoaded("iris");
    /// 帧率限制(渲染时间戳):secondCameraFrameRate 配置(帧/秒)
    private static long lastRenderMillis;
    /// 光影启用检测缓存(1 秒刷新一次,避免每帧反射)
    private static boolean shadersInUse;
    private static long lastShaderCheckMillis;
    /// 第二相机视锥远平面(格):限制区块挑选范围,减少提交量(视距配置,直接以格为单位)
    public static float secondCameraDepthFar() {
        return ModConfigManager.get().fishing.fishingCamera.viewDistance;
    }
    /// marker 相对浮漂的抬升(格):浮漂落地时 marker 会陷入方块内,
    /// 原版第三人称的射线避让从方块内开始检测会把相机压到贴脸,抬高后脱离方块。
    /// 保持低位:marker 高会让相机绕"浮标上方的空气"转、浮标偏离画面中心,
    /// 镜头高度由俯仰/距离配置控制(相机 = marker 斜上方)
    private static final double MARKER_LIFT = 0.3;

    private static @Nullable TextureTarget feedTarget;
    private static @Nullable ArmorStand cameraMarker;
    /// 本帧是否成功捕获,供 HUD 判断是否绘制
    private static boolean feedReady;
    /// 是否正在第二相机捕获中(供可见性相关 mixin 判断)
    public static boolean capturing;

    private FishingCameraModule() {}

    public static void init() {
        // 捕获与绘制均由 mixin 驱动;这里注册"Hypixel 上强制开启仅大厅/SkyBlock 限制"
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            var cfg = ModConfigManager.get().fishing.fishingCamera;
            if (cfg.onlyLobbyOrSkyblock) return;
            if (HypixelLocationTracker.getInstance().isOnHypixel()) {
                cfg.onlyLobbyOrSkyblock = true;
            }
        });
    }

    /// 世界渲染完成后调用:渲染第二相机画面进 feedTarget。
    public static void capture(DeltaTracker realDelta) {
        var mc = Minecraft.getInstance();
        var player = mc.player;
        var bobber = player == null ? null : player.fishing;
        if (mc.level == null) return;
        if (bobber == null) {
            // 收杆后不再显示最后一帧
            feedReady = false;
            return;
        }
        // —— 配置条件(不满足时清掉残留画面) ——
        var cfg = ModConfigManager.get().fishing.fishingCamera;
        var loc = HypixelLocationTracker.getInstance();
        if (!cfg.enabled
                || (cfg.onlyLobbyOrSkyblock && loc.isOnHypixel()
                    && loc.getLobbyName() == null && !loc.isInSkyblock())
                || (cfg.disabledInKuudra && loc.isInKuudra())
                || (cfg.disabledInDungeon && loc.isInDungeon())) {
            feedReady = false;
            return;
        }
        // 光影(Iris)启用时禁用:双相机渲染与 shader 状态冲突,主画面会闪烁
        if (cfg.disableWithShaders && isShaderPackInUse()) {
            feedReady = false;
            return;
        }
        // 帧率限制(按秒):距离上次渲染不足 1/fps 秒则跳过,保留上一帧画面(feedReady 不清)
        long now = Util.getMillis();
        if (now - lastRenderMillis < 1000L / Math.max(1, cfg.frameRate)) return;
        lastRenderMillis = now;

        var gameRenderer = mc.gameRenderer;
        var camera = gameRenderer.mainCamera();

        // —— 懒创建 feed target(宽高按配置的画面比例) ——
        var ratio = cfg.aspectRatio == null ? CameraAspectRatio.R2_1 : cfg.aspectRatio;
        int feedWidth = Math.max(1, FEED_HEIGHT * ratio.w / ratio.h);
        if (feedTarget == null || feedTarget.width != feedWidth || feedTarget.height != FEED_HEIGHT) {
            if (feedTarget != null) feedTarget.destroyBuffers();
            feedTarget = new TextureTarget("bza_fishing_feed", feedWidth, FEED_HEIGHT, true, GpuFormat.RGBA8_UNORM);
        }

        // —— 临时相机实体:隐形 ArmorStand(带 CAMERA_DISTANCE 属性,控制相机距离) ——
        if (cameraMarker == null || cameraMarker.level() != mc.level) {
            cameraMarker = new ArmorStand(EntityTypes.ARMOR_STAND, mc.level);
            cameraMarker.setInvisible(true);
        }
        // 虚拟相机实体:放在浮标位置,朝向按配置(yawMode),俯视配置的 pitch。
        // 原版第三人称把相机放到 marker 斜上方 distance 格(射线避让方块),浮标落在画面中心。
        Vec3 bobberPos = bobber.position();
        Vec3 playerPos = player.position();
        double toPlayerX = playerPos.x - bobberPos.x;
        double toPlayerZ = playerPos.z - bobberPos.z;
        // 兼容旧配置:枚举值可能已删除(yawMode = null),兜底为固定角度
        var yawMode = cfg.yawMode == null ? CameraYawMode.FIXED : cfg.yawMode;
        float yaw = switch (yawMode) {
            case FIXED -> 0.0F;
            case FRONT -> player.getYRot();
            case BACK -> player.getYRot() + 180.0F;
            case LEFT -> player.getYRot() - 90.0F;
            case RIGHT -> player.getYRot() + 90.0F;
        };
        // 偏航偏移 + 动态旋转只在固定角度生效
        if (yawMode == CameraYawMode.FIXED) {
            if (cfg.yawSpinSpeed > 0) {
                // 动态旋转:相机绕鱼漂持续旋转,速度(度/秒),0 = 关闭
                yaw += (mc.level.getGameTime() * cfg.yawSpinSpeed / 20.0F) % 360.0F;
            }
            yaw += cfg.yawOffset;
        }
        // 相机实体放浮标位置(眼高由 camera.eyeHeight 置 0 处理,不随玩家蹲起/游泳变化)
        cameraMarker.setPos(bobberPos.x, bobberPos.y + MARKER_LIFT, bobberPos.z);
        // 同步旧坐标:Camera 用 xo/yo/zo 插值取位置,marker 不 tick,残留值会导致相机位置错误
        cameraMarker.xo = cameraMarker.getX();
        cameraMarker.yo = cameraMarker.getY();
        cameraMarker.zo = cameraMarker.getZ();
        // 设置身体+头部旋转:Camera 的第三人称偏移方向读 getViewYRot(头部视角旋转),
        // 只 setYRot 的话偏移方向不随偏航转(相机只转头不绕浮漂转)
        cameraMarker.setYRot(yaw);
        cameraMarker.setXRot(cfg.pitch);
        cameraMarker.setYHeadRot(yaw);
        cameraMarker.yRotO = yaw;
        cameraMarker.xRotO = cfg.pitch;
        cameraMarker.yHeadRotO = yaw;
        // 相机距离(CAMERA_DISTANCE 属性)按配置
        var distanceAttr = cameraMarker.getAttribute(Attributes.CAMERA_DISTANCE);
        if (distanceAttr != null) {
            distanceAttr.setBaseValue(cfg.distance);
        }

        RenderTarget oldTarget = gameRenderer.mainRenderTarget();
        GlobalSettingsUniform mainGlobals = ((GameRendererAccessor) gameRenderer).globalSettingsUniform();
        Entity oldEntity = camera.entity();
        // 子相机区块采样器临时替换(mag=NEAREST 防图集渗漏白线)
        var lrAccessor = (LevelRendererAccessor) mc.levelRenderer;
        GpuSampler oldSampler = lrAccessor.getChunkLayerSampler();
        // 26.2 主画面流程捕获的 frustum 状态(第二相机捕获期间临时清除,finally 恢复)
        boolean oldCaptureFrustum = ((CameraFrustumAccessor) camera).captureFrustum();
        // SecurityCraft 26.2 同款:第二相机期间 visibleSections 会被重算,
        // 主画面 render 后的 doEntityOutline 等逻辑要用玩家视角的区块列表,需恢复
        var oldVisibleSections = mc.levelRenderer.visibleSections().clone();
        var optionsState = gameRenderer.gameRenderState().optionsRenderState;
        CameraType oldCameraType = optionsState.cameraType;
        GlowDepthRenderer.suppressCopy = true;
        capturing = true;
        var oldRealCameraType = mc.options.getCameraType();
        // 启用原版第三人称(detached):相机自动放到实体后方 4 格并射线避让方块
        if (oldRealCameraType != CameraType.THIRD_PERSON_BACK) {
            mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
        }
        // 视距限制由 CameraUpdateFrustumMixin 在 update 内改 depthFar(不碰区块加载)
        try {
            ((MainRenderTargetAccessor) gameRenderer).setMainRenderTarget(feedTarget);
            camera.setEntity(cameraMarker);
            // marker 眼高为 0:避免玩家蹲起/游泳的 eyeHeight 插值带动子视角高低
            ((CameraAccessor) camera).setEyeHeight(0.0F);
            ((CameraAccessor) camera).setEyeHeightOld(0.0F);
            // 26.2 主画面流程会 captureFrustum() 缓存视锥;extract 里 detected 到
            // getCapturedFrustum() != null 就跳过可见区块更新,第二相机视角的区块列表不会生成。
            // 临时清除捕获状态,让 update 重算 cullFrustum、extract 按第二相机视锥更新可见区块。
            camera.killFrustum();
            ((CameraFrustumAccessor) camera).setCaptureFrustum(false);
            // update 含 mainCamera.update + levelRenderer.update:按第二相机视锥重算可见区块,
            // 否则区块可见列表/可见性检查按主相机,浮标会消失、视野外变虚空
            gameRenderer.update(DeltaTracker.ONE);
            // 强制相机旋转为配置值(实体 getViewYRot 的转换会吞掉 setYRot,直接调 setRotation)
            ((CameraInvoker) camera).invokeSetRotation(yaw, cfg.pitch);
            // 26.2 视锥不随 setRotation 更新(update 时算的 cullFrustum 是默认朝向):
            // 手动重建第二相机视锥并注入,再强制 applyFrustum 刷新可见区块,
            // 否则 extract 的实体裁剪/区块可见性按错误视锥,第二画面地形错乱、盖住实体。
            var viewRot = camera.getViewRotationMatrix(new Matrix4f());
            var projForCulling = new Matrix4f().perspective(
                    camera.getFov() * (float) Math.PI / 180.0F,
                    (float) feedWidth / FEED_HEIGHT,
                    0.05F, secondCameraDepthFar(), RenderSystem.getDevice().getDeviceInfo().isZZeroToOne());
            Frustum captureCullFrustum = new Frustum(viewRot, projForCulling);
            captureCullFrustum.prepare(camera.position().x, camera.position().y, camera.position().z);
            ((CameraFrustumAccessor) camera).setCullFrustum(captureCullFrustum);
            ((LevelExtractorInvoker) mc.levelExtractor).invokeApplyFrustum(captureCullFrustum);
            gameRenderer.extract(DeltaTracker.ONE, true);
            var grs = gameRenderer.gameRenderState();
            // 投影比例按 feed target(不能改真实窗口尺寸,会触发 resize 闪烁):
            // 手动重算投影矩阵 + windowRenderState 宽高
            var camState = grs.levelRenderState.cameraRenderState;
            // 26.2 是 Vulkan reversed-Z:原版 Projection.getMatrix 故意反传 near/far
            // (setPerspective(near=depthFar, far=0.05)),深度比较用 GREATER_THAN_OR_EQUAL。
            // 手动投影必须同样反传,否则实体深度值方向与地形相反,实体会被地形遮挡。
            camState.projectionMatrix.set(new Matrix4f().perspective(
                    camera.getFov() * (float) Math.PI / 180.0F,
                    (float) feedWidth / FEED_HEIGHT,
                    camState.depthFar, 0.05F, RenderSystem.getDevice().getDeviceInfo().isZZeroToOne()));
            int oldWinW = grs.windowRenderState.width;
            int oldWinH = grs.windowRenderState.height;
            grs.windowRenderState.width = feedWidth;
            grs.windowRenderState.height = FEED_HEIGHT;
            // Globals(相机位置)写共享实例:主画面命令已在第一个 renderLevel 编码完毕,
            // 这里覆盖只影响第二相机的绘制命令;26.2 每帧 render 开头会重新 update(玩家位置),
            // 无需手动恢复(SC 26.2 同款做法)。
            mainGlobals.update(
                    feedWidth,
                    FEED_HEIGHT,
                    grs.optionsRenderState.glintStrength,
                    mc.level.getGameTime(),
                    DeltaTracker.ONE,
                    grs.optionsRenderState.menuBackgroundBlurriness,
                    camState.pos,
                    grs.optionsRenderState.textureFiltering == TextureFilteringMethod.RGSS);
            // 第三人称:避免特写画面里出现玩家手持的钓鱼竿/屏幕特效
            optionsState.cameraType = CameraType.THIRD_PERSON_BACK;
            // 关闭子视角的云:Globals(相机位置)是全局共享 buffer,主画面/子视角无法同时正确,
            // 云顶点按浮标生成而 shader 用玩家位置平移会错位;俯视画面里云占比小,直接关掉
            CloudStatus oldCloudStatus = optionsState.cloudStatus;
            optionsState.cloudStatus = CloudStatus.OFF;
            // 子相机近距放大时 UV 落在 texel 边界,LINEAR mag 过滤会混合相邻 texel 渗漏出白线;
            // mag 用 NEAREST(放大取单 texel)、min 保持 LINEAR(缩小平滑),既无渗漏又保留平滑
            lrAccessor.setChunkLayerSampler(feedSampler());
            try {
                gameRenderer.renderLevel(DeltaTracker.ONE);
                feedReady = true;
            } catch (Throwable t) {
                // 第二相机渲染异常不影响主画面(状态已在 finally 恢复),吞掉避免刷屏
                feedReady = false;
            } finally {
                lrAccessor.setChunkLayerSampler(oldSampler);
            }
            optionsState.cloudStatus = oldCloudStatus;
            grs.windowRenderState.width = oldWinW;
            grs.windowRenderState.height = oldWinH;
        } finally {
            ((MainRenderTargetAccessor) gameRenderer).setMainRenderTarget(oldTarget);
            if (oldEntity != null) {
                camera.setEntity(oldEntity);
            }
            // 恢复玩家眼高:capture 期间置 0 用于子视角,不恢复的话主视角会一直贴地
            ((CameraAccessor) camera).setEyeHeight(player.getEyeHeight());
            ((CameraAccessor) camera).setEyeHeightOld(player.getEyeHeight());
            optionsState.cameraType = oldCameraType;
            if (oldRealCameraType != CameraType.THIRD_PERSON_BACK) {
                mc.options.setCameraType(oldRealCameraType);
            }
            // 恢复主视角可见区块列表(doEntityOutline 等在 capture 后使用)
            var visibleSections = mc.levelRenderer.visibleSections();
            visibleSections.clear();
            visibleSections.addAll(oldVisibleSections);
            // 恢复 frustum 捕获标志(主画面流程每帧设置,恢复保证其语义不丢)
            ((CameraFrustumAccessor) camera).setCaptureFrustum(oldCaptureFrustum);
            GlowDepthRenderer.suppressCopy = false;
            capturing = false;
        }
    }

    /// HUD 提取阶段调用:把特写画面贴到 HUD 元素位置(支持编辑/缩放)。
    /// 光影是否实际启用(反射 IrisApi,1 秒缓存;未装 Iris 或 API 不可用时回退为按安装检测)
    private static boolean isShaderPackInUse() {
        if (!IRIS_LOADED) return false;
        long now = Util.getMillis();
        if (now - lastShaderCheckMillis < 1000L) return shadersInUse;
        lastShaderCheckMillis = now;
        try {
            Class<?> apiClass = Class.forName("net.irisshaders.iris.api.v0.IrisApi");
            Object api = apiClass.getMethod("getInstance").invoke(null);
            shadersInUse = (boolean) apiClass.getMethod("isShaderPackInUse").invoke(api);
        } catch (Exception ignored) {
            // API 不可用(版本差异):回退为按安装检测
            shadersInUse = true;
        }
        return shadersInUse;
    }

    /// 画中画边框宽度(逻辑像素)
    private static final int FRAME_BORDER = 2;

    public static void drawHud() {
        if (!feedReady || feedTarget == null) return;
        if (!HudManager.shouldShow("FishingCamera")) return;
        var textureView = feedTarget.getColorTextureView();
        if (textureView == null) return;
        float s = HudManager.scale("FishingCamera");
        // 高度固定、宽度按画面比例(2:1 更宽、1:2 更高);基准高度 192(渲染 256,显示缩小)
        int dh = Math.max(1, Math.round(192 * s));
        int dw = Math.max(1, Math.round(dh * feedTarget.width / (float) Math.max(1, feedTarget.height)));
        int x0 = HudManager.x("FishingCamera");
        int y0 = HudManager.y("FishingCamera");
        GuiRenderState guiRenderState = Minecraft.getInstance().gameRenderer.gameRenderState().guiRenderState;
        // 边框(背景色,像小地图):先画稍大的矩形,画中画覆盖在上面,四周露出边框
        var borderColor = ModConfigManager.get().fishing.fishingCamera.borderColor.getEffectiveColourRGB();
        guiRenderState.addGuiElement(new ColoredRectangleRenderState(
                RenderPipelines.GUI, TextureSetup.noTexture(), new Matrix3x2f(),
                x0 - FRAME_BORDER, y0 - FRAME_BORDER, x0 + dw + FRAME_BORDER, y0 + dh + FRAME_BORDER,
                borderColor, borderColor, null));
        guiRenderState.addBlitToCurrentLayer(new BlitRenderState(
                RenderPipelines.GUI_TEXTURED_PREMULTIPLIED_ALPHA,
                TextureSetup.singleTexture(textureView,
                        RenderSystem.getSamplerCache().getRepeat(FilterMode.NEAREST)),
                new Matrix3x2f(), x0, y0, x0 + dw, y0 + dh,
                0.0F, 1.0F, 1.0F, 0.0F, -1, null, null));
    }
}
