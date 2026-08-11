package top.babyzombie.addons.util.render;

import com.mojang.blaze3d.ProjectionType;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.AddressMode;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuSampler;
import net.minecraft.client.CameraType;
import net.minecraft.client.CloudStatus;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.TextureFilteringMethod;
import net.minecraft.client.renderer.DynamicUniforms;
import net.minecraft.client.renderer.GlobalSettingsUniform;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.jspecify.annotations.Nullable;
import top.babyzombie.addons.mixin.render.CameraAccessor;
import top.babyzombie.addons.mixin.render.CameraFrustumAccessor;
import top.babyzombie.addons.mixin.render.CameraInvoker;
import top.babyzombie.addons.mixin.render.GameRendererAccessor;
import top.babyzombie.addons.mixin.render.GlobalSettingsUniformAccessor;
import top.babyzombie.addons.mixin.render.RenderSystemAccessor;
import top.babyzombie.addons.mixin.render.SkyRendererAccessor;
import top.babyzombie.addons.mixin.render.LevelExtractorInvoker;
import top.babyzombie.addons.mixin.render.LevelRendererAccessor;
import top.babyzombie.addons.mixin.render.MainRenderTargetAccessor;

import java.util.OptionalDouble;

/// 第二相机渲染器:通用"临时相机实体 + 重跑 extract/renderLevel"机制。
/// 调用方提供相机参数与输出目标,capture() 渲染到目标并完整恢复主画面状态;
/// 恢复(相机位置/眼高/相机类型/可见区块/渲染目标)做错任何一步都会污染主画面。
/// 26.2 适配版:Vulkan reversed-Z 深度、killFrustum/手动视锥注入/applyFrustum、
/// feedSampler 区块采样器、Globals 共享实例、GlowDepthRenderer。
public final class SecondCameraRenderer {

    /// 是否正在第二相机捕获中(供可见性相关 mixin 判断;同一时刻只有一个第二相机)
    public static boolean capturing;

    /// 临时相机实体:隐形 ArmorStand(带 CAMERA_DISTANCE 属性,控制相机距离)
    private static @Nullable ArmorStand cameraMarker;

    /// 第二相机专用 uniform 实例:第二遍渲染的 transform/区块矩阵写入独立存储,
    /// 不碰主画面的共享环形缓冲(避免主画面区块按子相机矩阵渲染 + fence 语义冲突)
    private static @Nullable DynamicUniforms secondUniforms;

    private static DynamicUniforms secondUniforms() {
        if (secondUniforms == null) {
            secondUniforms = new DynamicUniforms();
        }
        secondUniforms.reset();
        return secondUniforms;
    }

    /// 第二相机专用 Globals 实例:Globals 是共享 uniform buffer,主画面 renderLevel
    /// 编码的区块命令引用它,GPU 异步执行时读值。第二遍 update 若写共享实例,
    /// 主画面地形会按子相机位置平移(黄块);独立实例 + finally 恢复全局状态即可隔离。
    private static @Nullable GlobalSettingsUniform secondGlobals;

    private static GlobalSettingsUniform secondGlobals() {
        if (secondGlobals == null) {
            secondGlobals = new GlobalSettingsUniform();
        }
        return secondGlobals;
    }

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

    /// 第二相机捕获参数
    public record CaptureParams(
            Vec3 anchorPos,      // 相机锚点(观察目标的位置)
            double markerLift,   // 锚点抬升(格):锚点落地时 marker 会陷入方块内,
                                 // 原版第三人称的射线避让从方块内开始检测会把相机压到贴脸,
                                 // 抬高后脱离方块。保持低位:marker 高会让相机绕"锚点上方的
                                 // 空气"转、目标偏离画面中心,镜头高度由俯仰/距离控制
            float yaw, float pitch, // 朝向(度)
            double distance,     // 相机距离(格,CAMERA_DISTANCE 属性)
            float depthFar,      // 视锥远平面(格,限制区块挑选范围)
            TextureTarget target // 输出目标(第二相机画面渲染到这里)
    ) {}

    private SecondCameraRenderer() {}

    /// 渲染第二相机画面进 target;返回是否成功(失败时主画面状态已恢复)。
    public static boolean capture(DeltaTracker realDelta, CaptureParams params) {
        // 防递归:capture 内部重跑的 renderLevel 会再次触发注入点
        // (AfterWorldRenderEvents),capturing 期间直接拒绝
        if (capturing) return false;
        var mc = Minecraft.getInstance();
        var player = mc.player;
        if (mc.level == null || player == null) return false;
        var gameRenderer = mc.gameRenderer;
        var camera = gameRenderer.mainCamera();

        // —— 临时相机实体:隐形 ArmorStand(带 CAMERA_DISTANCE 属性,控制相机距离) ——
        if (cameraMarker == null || cameraMarker.level() != mc.level) {
            cameraMarker = new ArmorStand(EntityTypes.ARMOR_STAND, mc.level);
            cameraMarker.setInvisible(true);
        }
        // 相机实体放锚点位置(眼高由 camera.eyeHeight 置 0 处理,不随玩家蹲起/游泳变化)
        cameraMarker.setPos(params.anchorPos.x, params.anchorPos.y + params.markerLift, params.anchorPos.z);
        // 同步旧坐标:Camera 用 xo/yo/zo 插值取位置,marker 不 tick,残留值会导致相机位置错误
        cameraMarker.xo = cameraMarker.getX();
        cameraMarker.yo = cameraMarker.getY();
        cameraMarker.zo = cameraMarker.getZ();
        // 设置身体+头部旋转:Camera 的第三人称偏移方向读 getViewYRot(头部视角旋转),
        // 只 setYRot 的话偏移方向不随偏航转(相机只转头不绕锚点转)
        cameraMarker.setYRot(params.yaw);
        cameraMarker.setXRot(params.pitch);
        cameraMarker.setYHeadRot(params.yaw);
        cameraMarker.yRotO = params.yaw;
        cameraMarker.xRotO = params.pitch;
        cameraMarker.yHeadRotO = params.yaw;
        // 相机距离(CAMERA_DISTANCE 属性)按参数
        var distanceAttr = cameraMarker.getAttribute(Attributes.CAMERA_DISTANCE);
        if (distanceAttr != null) {
            distanceAttr.setBaseValue(params.distance);
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
        // 保存主画面投影矩阵:第二遍 renderLevel 会覆盖共享投影 buffer 的内容,
        // 结束需写回主投影,否则主画面后续渲染(GUI 等)用第二相机投影错乱
        var savedProjection = new Matrix4f(gameRenderer.gameRenderState().levelRenderState.cameraRenderState.projectionMatrix);
        // 保存发光标志:第二遍 extract(ExtractEntityOutlineSkipMixin)会把它改成 false,
        // 主画面 capture 后的 doEntityOutline 读共享 state 会跳过发光合成
        boolean savedShowOutlines = gameRenderer.gameRenderState().levelRenderState.shouldShowEntityOutlines;
        // —— 第二相机 uniform 隔离 ——
        // transforms/chunkSections 的 uniform 存在共享环形缓冲(每帧一个 slot,帧内多次
        // 写入覆盖 + fence 绑定当前 submit)。第二遍渲染用独立 DynamicUniforms 实例,
        // 不写主画面的 slot(否则主画面区块命令读到子相机矩阵,按子相机视角画到主画面;
        // 手动操控 ring slot 会破坏 fence 语义导致崩溃)。
        var savedDynamicUniforms = RenderSystem.getDynamicUniforms();
        RenderSystemAccessor.setDynamicUniforms(secondUniforms());
        var oldRealCameraType = mc.options.getCameraType();
        // 启用原版第三人称(detached):相机自动放到实体后方 4 格并射线避让方块
        if (oldRealCameraType != CameraType.THIRD_PERSON_BACK) {
            mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
        }
        // 视距限制由 CameraUpdateFrustumMixin 在 update 内改 depthFar(不碰区块加载)
        try {
            ((MainRenderTargetAccessor) gameRenderer).setMainRenderTarget(params.target);
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
            // 否则区块可见列表/可见性检查按主相机,目标会消失、视野外变虚空
            gameRenderer.update(DeltaTracker.ONE);
            // 强制相机旋转为配置值(实体 getViewYRot 的转换会吞掉 setYRot,直接调 setRotation)
            ((CameraInvoker) camera).invokeSetRotation(params.yaw, params.pitch);
            // 26.2 视锥不随 setRotation 更新(update 时算的 cullFrustum 是默认朝向):
            // 手动重建第二相机视锥并注入,再强制 applyFrustum 刷新可见区块,
            // 否则 extract 的实体裁剪/区块可见性按错误视锥,第二画面地形错乱、盖住实体。
            var viewRot = camera.getViewRotationMatrix(new Matrix4f());
            var projForCulling = new Matrix4f().perspective(
                    camera.getFov() * (float) Math.PI / 180.0F,
                    (float) params.target.width / params.target.height,
                    0.05F, params.depthFar, RenderSystem.getDevice().getDeviceInfo().isZZeroToOne());
            Frustum captureCullFrustum = new Frustum(viewRot, projForCulling);
            captureCullFrustum.prepare(camera.position().x, camera.position().y, camera.position().z);
            ((CameraFrustumAccessor) camera).setCullFrustum(captureCullFrustum);
            ((LevelExtractorInvoker) mc.levelExtractor).invokeApplyFrustum(captureCullFrustum);
            gameRenderer.extract(DeltaTracker.ONE, true);
            var grs = gameRenderer.gameRenderState();
            // 投影比例按输出目标(不能改真实窗口尺寸,会触发 resize 闪烁):
            // 手动重算投影矩阵 + windowRenderState 宽高
            var camState = grs.levelRenderState.cameraRenderState;
            // 26.2 是 Vulkan reversed-Z:原版 Projection.getMatrix 故意反传 near/far
            // (setPerspective(near=depthFar, far=0.05)),深度比较用 GREATER_THAN_OR_EQUAL。
            // 手动投影必须同样反传,否则实体深度值方向与地形相反,实体会被地形遮挡。
            camState.projectionMatrix.set(new Matrix4f().perspective(
                    camera.getFov() * (float) Math.PI / 180.0F,
                    (float) params.target.width / params.target.height,
                    camState.depthFar, 0.05F, RenderSystem.getDevice().getDeviceInfo().isZZeroToOne()));
            int oldWinW = grs.windowRenderState.width;
            int oldWinH = grs.windowRenderState.height;
            grs.windowRenderState.width = params.target.width;
            grs.windowRenderState.height = params.target.height;
            // Globals(相机位置)写独立实例:主画面命令已在第一个 renderLevel 编码完毕,
            // 但 GPU 异步执行时读共享 globals buffer,第二遍 update 若覆盖共享实例,
            // 主画面区块会按子相机位置渲染(黄块);独立实例只影响第二相机的绘制命令,
            // finally 再把 RenderSystem 全局状态指回主画面实例。
            secondGlobals().update(
                    params.target.width,
                    params.target.height,
                    grs.optionsRenderState.glintStrength,
                    mc.level.getGameTime(),
                    DeltaTracker.ONE,
                    grs.optionsRenderState.menuBackgroundBlurriness,
                    camState.pos,
                    grs.optionsRenderState.textureFiltering == TextureFilteringMethod.RGSS);
            // 第三人称:避免特写画面里出现玩家手持的钓鱼竿/屏幕特效
            optionsState.cameraType = CameraType.THIRD_PERSON_BACK;
            // 关闭子视角的云:Globals(相机位置)是全局共享 buffer,主画面/子视角无法同时正确,
            // 云顶点按锚点生成而 shader 用玩家位置平移会错位;俯视画面里云占比小,直接关掉
            CloudStatus oldCloudStatus = optionsState.cloudStatus;
            optionsState.cloudStatus = CloudStatus.OFF;
            // 恢复窗口尺寸:不恢复的话下一帧主画面 render 的 resize 检查
            // (windowRenderState != mainRenderTarget)会误触发,把主渲染目标改成
            // 第二相机尺寸,画面在小窗/全屏之间闪
            grs.windowRenderState.width = oldWinW;
            grs.windowRenderState.height = oldWinH;
            // 子相机近距放大时 UV 落在 texel 边界,LINEAR mag 过滤会混合相邻 texel 渗漏出白线;
            // mag 用 NEAREST(放大取单 texel)、min 保持 LINEAR(缩小平滑),既无渗漏又保留平滑
            lrAccessor.setChunkLayerSampler(feedSampler());
            // SkyRenderer 缓存构造时的主画面 target,第二遍渲染的天空盘仍画到主画面
            // (斜向黄/蓝块,跟随子相机视角);临时指向子相机输出(SC 26.2 同款修复)
            var skyRenderer = mc.levelRenderer.skyRenderer();
            if (skyRenderer != null) {
                ((SkyRendererAccessor) skyRenderer).setRenderTarget(params.target);
            }
            try {
                gameRenderer.renderLevel(DeltaTracker.ONE);
                return true;
            } catch (Throwable t) {
                // 第二相机渲染异常不影响主画面(状态已在 finally 恢复),吞掉避免刷屏
                return false;
            } finally {
                lrAccessor.setChunkLayerSampler(oldSampler);
            }
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
            // 恢复相机位置/朝向:capture 期间 update 把相机放到第二相机视角,只恢复 entity 时
            // position/forwards/up/left 仍停在第二相机,下一帧 tick 的 soundEngine.updateSource
            // 会用第二相机视角设置声音 listener(衰减/定位错乱 = 电音卡顿)。
            // 必须放在 cameraType 恢复之后:alignWithEntity 内部按 options.getCameraType()
            // 重算 detached,顺序反了会把相机按第三人称放到玩家后方 4 格
            ((CameraInvoker) camera).invokeAlignWithEntity(realDelta.getGameTimeDeltaPartialTick(true));
            // 恢复主视角可见区块列表(doEntityOutline 等在 capture 后使用)
            var visibleSections = mc.levelRenderer.visibleSections();
            visibleSections.clear();
            visibleSections.addAll(oldVisibleSections);
            // 恢复 frustum 捕获标志(主画面流程每帧设置,恢复保证其语义不丢)
            ((CameraFrustumAccessor) camera).setCaptureFrustum(oldCaptureFrustum);
            // 写回主画面投影矩阵(第二遍 renderLevel 覆盖了共享 buffer 的内容)
            RenderSystem.setProjectionMatrix(
                    ((GameRendererAccessor) gameRenderer).levelProjectionMatrixBuffer().getBuffer(savedProjection),
                    ProjectionType.PERSPECTIVE);
            // 恢复主画面 Globals 全局状态(第二遍 update 把它指向了独立实例的 buffer)
            RenderSystem.setGlobalSettingsUniform(
                    ((GlobalSettingsUniformAccessor) mainGlobals).buffer());
            // 恢复主画面 uniform 实例
            RenderSystemAccessor.setDynamicUniforms(savedDynamicUniforms);
            // SkyRenderer 指回主画面 target(第二遍可能重建了 skyRenderer,用当前实例恢复)
            var currentSky = mc.levelRenderer.skyRenderer();
            if (currentSky != null) {
                ((SkyRendererAccessor) currentSky).setRenderTarget(oldTarget);
            }
            // 恢复发光标志(ExtractEntityOutlineSkipMixin 在第二遍 extract 改的)
            gameRenderer.gameRenderState().levelRenderState.shouldShowEntityOutlines = savedShowOutlines;
            GlowDepthRenderer.suppressCopy = false;
            capturing = false;
        }
    }
}
