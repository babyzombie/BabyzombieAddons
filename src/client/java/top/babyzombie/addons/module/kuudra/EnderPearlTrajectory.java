package top.babyzombie.addons.module.kuudra;

import io.github.notenoughupdates.moulconfig.ChromaColour;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownEnderpearl;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;
import top.babyzombie.addons.config.KuudraConfig.PearlTrajectoryCfg;
import top.babyzombie.addons.config.ModConfigManager;
import top.babyzombie.addons.util.render.RenderPhaseRegister;
import top.babyzombie.addons.util.tracker.HypixelLocationTracker;
import top.babyzombie.addons.util.render.WorldRenderContext;
import top.babyzombie.addons.util.render.WorldRenderUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * 末影珍珠轨迹:手持珍珠时预测投掷轨迹,投出后从珍珠实体继续预测剩余轨迹,
 * 落点显示与轨迹线渲染复用 Safari 精灵球的画法。
 * 物理参数按 Hypixel 服务端(1.8 EntityThrowable):水平 ×0.99、垂直 -0.03、初速 1.5;
 * 本地 26.1 客户端 ThrowableProjectile 是三轴统一 ×0.99,与服务器不一致,不能照抄。
 */
public final class EnderPearlTrajectory {
    private static final Minecraft CLIENT = Minecraft.getInstance();

    // Hypixel 实测校准(2026-08-12):
    // 珍珠显示轨迹 = 客户端本地物理(26.1 ThrowableProjectile:三轴 ×0.99 + vy -0.03),
    // 服务器每 ~20 tick(≈1 秒)才修正一次位置,预测必须模拟客户端本地物理而非 1.8。
    // 初速 = 服务器投掷初速 1.5(预测从眼睛模拟,第一 tick 应用 ×0.99 后 = 1.485,
    // 与珍珠实际第一 tick 差分一致;直接用差分均值 1.415 会偏低)
    private static final double PEARL_INITIAL_VELOCITY = 1.5;
    private static final double PEARL_DRAG_PER_TICK = 0.99;
    private static final double PEARL_GRAVITY_PER_TICK = 0.03;
    private static final int MAX_TICKS = 128;
    private static final int SUBSTEPS_PER_TICK = 4;
    /// 侧向偏移与胶囊一致,右眼
    private static final double PEARL_SIDE_OFFSET = 0.16;

    private EnderPearlTrajectory() {
    }

    public static void init() {
        EnderPearlTracker.init();
        RenderPhaseRegister.register(EnderPearlTrajectory::render);
    }

    private static void render(WorldRenderContext context) {
        PearlTrajectoryCfg cfg = config();
        Player player = CLIENT.player;
        if (!cfg.enabled || !phaseActive() || player == null || CLIENT.level == null || CLIENT.gui.screen() != null) {
            return;
        }
        PearlPrediction prediction = predict();
        if (prediction == null || prediction.points().size() < 2) {
            return;
        }
        drawTrajectory(context, prediction, cfg);
    }

    /// 当前预测:跟随珍珠开启且珍珠在飞时从珍珠继续预测(线跟珍珠、落地计时随飞行减少);
    /// 否则手持珍珠时从玩家眼睛位置预测(飞行中的旧珍珠不干扰瞄准);
    /// 再否则有投出的珍珠时从珍珠继续预测。无条件返回 null。
    static @Nullable PearlPrediction predict() {
        Player player = CLIENT.player;
        if (player == null || CLIENT.level == null) {
            return null;
        }
        float partialTick = CLIENT.getDeltaTracker().getGameTimeDeltaPartialTick(false);
        ThrownEnderpearl pearl = EnderPearlTracker.trackedPearl();
        if (config().followPearl && pearl != null) {
            return predictFrom(pearl.getPosition(partialTick), pearlVelocity(pearl, partialTick));
        }
        if (holdingPearl(player)) {
            // Hypixel 服务端投掷不继承玩家移动速度(1.8 机制),预测不加玩家 deltaMovement
            Vec3 motion = player.getViewVector(1.0f).normalize().scale(PEARL_INITIAL_VELOCITY);
            // 起点 = 眼睛位置(与准星同高),侧向偏移 = 右眼(与胶囊一致);
            // 左右手切换开启时取反(左眼)。用渲染插值位置:getEyePosition(1.0F) 是 tick
            // 时刻位置,移动时每 tick 跳一次,线起点会跟着跳
            double yaw = Math.toRadians(player.getYRot());
            double flip = ModConfigManager.get().general.handRender.swapHands ? 1.0 : -1.0;
            Vec3 eyePos = player.getEyePosition(partialTick);
            return predictFrom(eyePos.add(
                    flip * Math.cos(yaw) * PEARL_SIDE_OFFSET,
                    0.0,
                    flip * Math.sin(yaw) * PEARL_SIDE_OFFSET), motion);
        }
        if (pearl != null) {
            return predictFrom(pearl.getPosition(partialTick), pearlVelocity(pearl, partialTick));
        }
        return null;
    }

    /**
     * 珍珠当前速度:优先 tick 位置差分按 partialTick 插值(服务器同步位置,接近服务器真实速度,
     * 插值让轨迹线帧间平滑);差分未建立时兜底 deltaMovement。
     */
    private static Vec3 pearlVelocity(ThrownEnderpearl pearl, float partialTick) {
        Vec3 interpolated = EnderPearlTracker.velocity(partialTick);
        return interpolated != null ? interpolated : pearl.getDeltaMovement();
    }

    /// 1.8 物理模拟:每 tick 先移动(子步进碰撞检测),再水平 ×0.99、垂直 -0.03。
    private static PearlPrediction predictFrom(Vec3 start, Vec3 motion) {
        List<Vec3> points = new ArrayList<>(MAX_TICKS * SUBSTEPS_PER_TICK + 1);
        points.add(start);
        Vec3 pos = start;
        for (int tick = 0; tick < MAX_TICKS; tick++) {
            for (int substep = 0; substep < SUBSTEPS_PER_TICK; substep++) {
                Vec3 next = pos.add(motion.scale(1.0 / SUBSTEPS_PER_TICK));
                Collision collision = findCollision(pos, next);
                if (collision != null) {
                    points.add(collision.location());
                    return new PearlPrediction(points, collision.location(), true, tick + 1, collision.face());
                }
                points.add(next);
                pos = next;
            }
            // 客户端本地物理(26.1):先减重力再三轴 ×0.99(applyGravity → applyInertia)
            motion = motion.multiply(PEARL_DRAG_PER_TICK, PEARL_DRAG_PER_TICK, PEARL_DRAG_PER_TICK)
                    .add(0.0, -PEARL_GRAVITY_PER_TICK, 0.0);
        }
        return new PearlPrediction(points, points.getLast(), false, MAX_TICKS, null);
    }

    /** 方块碰撞 + 实体碰撞,取更近的命中点。 */
    private static @Nullable Collision findCollision(Vec3 from, Vec3 to) {
        BlockHitResult colliderHit = CLIENT.level.clip(new ClipContext(
                from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, CLIENT.player
        ));
        BlockHitResult blockHit = colliderHit.getType() == HitResult.Type.MISS ? null : colliderHit;

        // 部分装饰树叶只有 outline 没有碰撞体(与胶囊预测同款兜底)
        BlockHitResult outlineHit = CLIENT.level.clip(new ClipContext(
                from, to, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, CLIENT.player
        ));
        if (outlineHit.getType() != HitResult.Type.MISS
                && CLIENT.level.getBlockState(outlineHit.getBlockPos()).is(BlockTags.LEAVES)
                && isNearer(from, outlineHit, blockHit)) {
            blockHit = outlineHit;
        }

        AABB searchBox = new AABB(from, to).inflate(0.3);
        EntityHitResult entityHit = ProjectileUtil.getEntityHitResult(
                CLIENT.level,
                CLIENT.player,
                from,
                to,
                searchBox,
                EnderPearlTrajectory::canHitEntity,
                0.3f
        );

        if (entityHit != null && isNearer(from, entityHit, blockHit)) {
            return new Collision(entityHit.getLocation(), null);
        }
        return blockHit == null ? null : new Collision(blockHit.getLocation(), blockHit.getDirection());
    }

    private static boolean canHitEntity(Entity entity) {
        return entity != CLIENT.player
                // 排除珍珠自己:预测从珍珠位置出发,射线起点在珍珠包围盒内,
                // 不排除会把落点判为珍珠当前位置,线会一闪一闪
                && !(entity instanceof ThrownEnderpearl)
                && !entity.isSpectator()
                && !entity.isRemoved()
                && entity.canBeHitByProjectile();
    }

    private static boolean isNearer(Vec3 origin, HitResult candidate, HitResult current) {
        return current == null
                || origin.distanceToSqr(candidate.getLocation()) < origin.distanceToSqr(current.getLocation());
    }

    private static void drawTrajectory(
            WorldRenderContext context,
            PearlPrediction prediction,
            PearlTrajectoryCfg cfg
    ) {
        List<Vec3> points = prediction.points();
        if (cfg.trajectoryEnabled) {
            int rgb = effectiveRgb(cfg.trajectoryColor, 0xAA00FF);
            float red = channel(rgb >> 16);
            float green = channel(rgb >> 8);
            float blue = channel(rgb);
            float width = 2.5f;
            // 软外圈保证路径在远近方块上都可读(同胶囊画法)
            WorldRenderUtils.drawPolyline(context, points, red, green, blue,
                    0.96f * 0.22f, 0.18f * 0.22f, true, width + 3.0f);
            WorldRenderUtils.drawPolyline(context, points, red, green, blue,
                    0.96f, 0.18f, true, width);
        }

        if (prediction.hit() && cfg.landingDiscEnabled) {
            int rgb = effectiveRgb(cfg.landingDiscColor, 0xAA00FF);
            Vec3 landing = prediction.landing();
            // 圆盘按碰撞面法线铺设:撞墙时圆盘横过来铺在墙上(同胶囊画法)
            Direction face = prediction.face() == null ? Direction.UP : prediction.face();
            Vec3 normal = Vec3.atLowerCornerOf(face.getUnitVec3i());
            Vec3 discCenter = landing.add(normal.scale(0.012));
            WorldRenderUtils.drawFilledCircle(context,
                    discCenter.x, discCenter.y, discCenter.z, 0.5,
                    channel(rgb >> 16), channel(rgb >> 8), channel(rgb),
                    0.30f, true, (float) normal.x, (float) normal.y, (float) normal.z);
        }
    }

    /** 当前预测(供第二相机/HUD);不在手持/投掷状态或条件不满足时返回 null。 */
    public static @Nullable PearlPrediction currentPrediction() {
        if (CLIENT.player == null || CLIENT.level == null || CLIENT.gui.screen() != null) {
            return null;
        }
        return predict();
    }

    /// 生效条件:无条件仅在 Hypixel SkyBlock;再按区域/阶段开关限制
    /// ("只在 Kuudra 生效"需 inKuudra;"只在阶段1生效"需 inKuudra 且未收到
    /// P2 开始消息——消息检测比区域判断准,P1/P2 共用场地区域分不开)。
    static boolean phaseActive() {
        HypixelLocationTracker tracker = HypixelLocationTracker.getInstance();
        if (tracker == null || !tracker.isInSkyblock()) {
            return false;
        }
        PearlTrajectoryCfg cfg = config();
        if (!cfg.onlyInKuudra && !cfg.onlyPhase1) {
            return true;
        }
        if (!KuudraLocationTracker.inKuudra) {
            return false;
        }
        return !cfg.onlyPhase1 || !EnderPearlTracker.isPhase1Ended();
    }

    /** 手持原版珍珠(主手或副手)。 */
    static boolean holdingPearl(Player player) {
        return player.getMainHandItem().is(Items.ENDER_PEARL)
                || player.getOffhandItem().is(Items.ENDER_PEARL);
    }

    static PearlTrajectoryCfg config() {
        return ModConfigManager.get().kuudra.phase1.pearlTrajectory;
    }

    private static float channel(int value) {
        return (value & 0xFF) / 255.0f;
    }

    private static int effectiveRgb(ChromaColour colour, int fallback) {
        return colour == null ? fallback : colour.getEffectiveColourRGB();
    }

    /** 预测结果:轨迹点、落点、是否命中、落地 tick 数(MAX_TICKS = 未命中上限)、落点碰撞面。 */
    public record PearlPrediction(List<Vec3> points, Vec3 landing, boolean hit, int ticks, Direction face) {
    }

    /** 碰撞结果:命中位置与碰撞面(实体碰撞面为 null,渲染时兜底 UP)。 */
    private record Collision(Vec3 location, Direction face) {
    }
}
