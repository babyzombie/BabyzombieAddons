package top.babyzombie.addons.util.render;

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
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.jspecify.annotations.Nullable;
import top.babyzombie.addons.mixin.render.CameraAccessor;
import top.babyzombie.addons.mixin.render.CameraInvoker;
import top.babyzombie.addons.mixin.render.LevelRendererAccessor;
import top.babyzombie.addons.mixin.render.MainRenderTargetAccessor;

/// 第二相机渲染器:通用"临时相机实体 + 重跑 extract/renderLevel"机制。
/// 调用方提供相机参数与输出目标,capture() 渲染到目标并完整恢复主画面状态;
/// 恢复(相机位置/眼高/相机类型/Globals/渲染目标)做错任何一步都会污染主画面。
/// 26.1.2 版:视锥由 update 内重算,无 26.2 的 killFrustum/applyFrustum 适配。
public final class SecondCameraRenderer {

    /// 是否正在第二相机捕获中(供可见性相关 mixin 判断;同一时刻只有一个第二相机)
    public static boolean capturing;

    /// 临时相机实体:隐形 ArmorStand(带 CAMERA_DISTANCE 属性,控制相机距离)
    private static @Nullable ArmorStand cameraMarker;

    /// 子相机区块采样器(mag=NEAREST,禁 mip):近距放大时 UV 落在 texel 边界,
    /// LINEAR mag 过滤会混合图集相邻 texel 渗漏出白线;NEAREST 取单 texel 无渗漏,
    /// mipLevels=1 禁用 mipmap(RGSS 按视口算 mip 层级,子相机小视口会切到低分辨率
    /// mip 导致边界渗漏),min 保持 LINEAR 缩小平滑。懒创建复用。
    private static @Nullable GpuSampler feedSampler;

    private static GpuSampler feedSampler() {
        if (feedSampler == null) {
            feedSampler = RenderSystem.getDevice().createSampler(
                    AddressMode.CLAMP_TO_EDGE, AddressMode.CLAMP_TO_EDGE,
                    FilterMode.LINEAR, FilterMode.NEAREST, 1, java.util.OptionalDouble.empty());
        }
        return feedSampler;
    }

    /// 第二相机专用 outline 目标(子相机尺寸):第二遍渲染的发光实体画到这里,
    /// 不污染主画面共享的 entityOutlineTarget(主画面 doEntityOutline 会全屏 blit 它)。
    private static @Nullable TextureTarget secondOutlineTarget;

    /// outline 绘制前调用:RenderType 的输出是它自己的 outputTarget(构造时缓存的引用,
    /// 替换字段无效),draw 里 outputColorTextureOverride 优先;第二遍渲染时把 outline
    /// 导向子相机 outline target。深度不 override:深度测试发光(DepthTestGlowRenderer)
    /// 自己设置 outputDepthTextureOverride,设了会挡掉它。
    public static void beginOutlineOverride() {
        if (secondOutlineTarget == null) return;
        RenderSystem.outputColorTextureOverride = secondOutlineTarget.getColorTextureView();
        RenderSystem.outputDepthTextureOverride = null;
    }

    public static void endOutlineOverride() {
        RenderSystem.outputColorTextureOverride = null;
        RenderSystem.outputDepthTextureOverride = null;
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
        // (LevelRenderEvents.END_MAIN),capturing 期间直接拒绝
        if (capturing) return false;
        var mc = Minecraft.getInstance();
        var player = mc.player;
        if (mc.level == null || player == null) return false;
        var gameRenderer = mc.gameRenderer;
        var camera = gameRenderer.getMainCamera();

        // —— 临时相机实体:隐形 ArmorStand(带 CAMERA_DISTANCE 属性,控制相机距离) ——
        if (cameraMarker == null || cameraMarker.level() != mc.level) {
            cameraMarker = new ArmorStand(EntityType.ARMOR_STAND, mc.level);
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

        RenderTarget oldTarget = mc.getMainRenderTarget();
        RenderTarget oldOutlineTarget = ((LevelRendererAccessor) mc.levelRenderer).getEntityOutlineTarget();
        GpuSampler oldSampler = ((LevelRendererAccessor) mc.levelRenderer).getChunkLayerSampler();
        Entity oldEntity = camera.entity();
        var optionsState = gameRenderer.getGameRenderState().optionsRenderState;
        CameraType oldCameraType = optionsState.cameraType;
        capturing = true;
        var oldRealCameraType = mc.options.getCameraType();
        // 启用原版第三人称(detached):相机自动放到实体后方 4 格并射线避让方块
        if (oldRealCameraType != CameraType.THIRD_PERSON_BACK) {
            mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
        }
        // 视距限制由 CameraUpdateFrustumMixin 在 update 内改 depthFar(不碰区块加载)
        try {
            ((MainRenderTargetAccessor) mc).setMainRenderTarget(params.target);
            camera.setEntity(cameraMarker);
            // marker 眼高为 0:避免玩家蹲起/游泳的 eyeHeight 插值带动子视角高低
            ((CameraAccessor) camera).setEyeHeight(0.0F);
            ((CameraAccessor) camera).setEyeHeightOld(0.0F);
            // update 含 mainCamera.update + levelRenderer.update:按第二相机视锥重算可见区块,
            // 否则区块可见列表/可见性检查按主相机,目标会消失、视野外变虚空
            gameRenderer.update(DeltaTracker.ONE, true);
            // 强制相机旋转为配置值(实体 getViewYRot 的转换会吞掉 setYRot,直接调 setRotation)
            ((CameraInvoker) camera).invokeSetRotation(params.yaw, params.pitch);
            gameRenderer.extract(DeltaTracker.ONE, true);
            var grs = gameRenderer.getGameRenderState();
            // 投影比例按输出目标(不能改真实窗口尺寸,会触发 resize 闪烁):
            // 手动重算投影矩阵 + windowRenderState 宽高
            var camState = grs.levelRenderState.cameraRenderState;
            camState.projectionMatrix.set(new Matrix4f().perspective(
                    camera.getFov() * (float) Math.PI / 180.0F,
                    (float) params.target.width / params.target.height,
                    0.05F, camState.depthFar, RenderSystem.getDevice().isZZeroToOne()));
            int oldWinW = grs.windowRenderState.width;
            int oldWinH = grs.windowRenderState.height;
            grs.windowRenderState.width = params.target.width;
            grs.windowRenderState.height = params.target.height;
            // 重写 Globals uniform:主画面渲染时写入的是玩家的相机位置,
            // 不更新的话区块按玩家位置平移、实体按第二相机平移,两者错位(实体偏移)
            gameRenderer.getGlobalSettingsUniform().update(
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
            // outline 目标临时替换为子相机尺寸:第二遍渲染的发光实体(原版/Skyblocker 的
            // 标记不经过 shouldShowEntityOutlines)画进共享 entityOutlineTarget 后,
            // 主画面 doEntityOutline 会把它全屏 blit 到主画面(发光放大平移污染/闪烁);
            // 替换后子相机发光画进子相机 outline target,主画面 outline 不被污染
            var lrAccessor = (LevelRendererAccessor) mc.levelRenderer;
            if (secondOutlineTarget == null
                    || secondOutlineTarget.width != params.target.width
                    || secondOutlineTarget.height != params.target.height) {
                if (secondOutlineTarget != null) secondOutlineTarget.destroyBuffers();
                secondOutlineTarget = new TextureTarget(
                        "bza_second_outline", params.target.width, params.target.height, true);
            }
            lrAccessor.setEntityOutlineTarget(secondOutlineTarget);
            // 白线缓解(26.2 同步):区块采样器换 NEAREST mag + 禁 mip。
            // RGSS 的 mip 层级按视口算,子相机视口小 → mip 层级变化 → 图集边界
            // texel 渗漏出白线(草方块土/草交界最明显);NEAREST mag 取单 texel,
            // mipLevels=1 禁用 mipmap,min 保持 LINEAR 缩小平滑
            lrAccessor.setChunkLayerSampler(feedSampler());
            try {
                gameRenderer.renderLevel(DeltaTracker.ONE);
                // 手动把第二遍 outline(含 entity_outline.json sobel 描边后处理的结果)
                // 合成进子相机画面:原版 outline chain 的合成 pass 在第二遍 frame
                // 不输出到 main,直接 blit 子相机 outline target 到 feedTarget 等效且可靠
                if (secondOutlineTarget != null) {
                    secondOutlineTarget.blitAndBlendToTexture(params.target.getColorTextureView());
                }
                return true;
            } catch (Throwable t) {
                // 第二相机渲染异常不影响主画面(状态已在 finally 恢复),吞掉避免刷屏;
                // 记录日志便于定位(如其他 mod 的深度发光附件尺寸不匹配等渲染状态异常)
                org.slf4j.LoggerFactory.getLogger("BabyzombieAddons")
                        .warn("second camera render failed", t);
                return false;
            } finally {
                // 恢复子视角期间改动的共享 state:第二遍渲染无论成败都恢复,
                // 否则异常残留会让主画面云关闭(cloudStatus)/下一帧 resize 检查
                // 误触发(windowRenderState 尺寸残留)
                optionsState.cloudStatus = oldCloudStatus;
                grs.windowRenderState.width = oldWinW;
                grs.windowRenderState.height = oldWinH;
            }
        } finally {
            ((MainRenderTargetAccessor) mc).setMainRenderTarget(oldTarget);
            ((LevelRendererAccessor) mc.levelRenderer).setEntityOutlineTarget(oldOutlineTarget);
            ((LevelRendererAccessor) mc.levelRenderer).setChunkLayerSampler(oldSampler);
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
            // 恢复 Globals 为玩家位置:主画面的云等 pass 在帧尾执行时读共享 buffer,
            // 不恢复的话主画面云会按第二相机位置平移导致抖动
            var grs2 = gameRenderer.getGameRenderState();
            gameRenderer.getGlobalSettingsUniform().update(
                    grs2.windowRenderState.width,
                    grs2.windowRenderState.height,
                    grs2.optionsRenderState.glintStrength,
                    mc.level.getGameTime(),
                    realDelta,
                    grs2.optionsRenderState.menuBackgroundBlurriness,
                    mc.player.position(),
                    grs2.optionsRenderState.textureFiltering == TextureFilteringMethod.RGSS);
            // 恢复输出 override:第二遍渲染(executeOutline)异常时 beginOutlineOverride 设置的
            // outputColorTextureOverride 残留,主画面后续渲染会全部输出到子相机 target
            // (抽搐/闪烁);其他 mod 的发光在第二相机画面尺寸不匹配也会抛异常,统一恢复
            RenderSystem.outputColorTextureOverride = null;
            RenderSystem.outputDepthTextureOverride = null;
            // 恢复矩阵栈:第二遍渲染异常打断 push/pop 配对时,共享矩阵栈残留,
            // 主画面天空渲染 pushMatrix 会栈满崩端;渲染流程每帧 push/pop 配对,
            // 栈底为 identity,clear 后等效
            RenderSystem.getModelViewStack().clear();
            capturing = false;
        }
    }
}
