package top.babyzombie.addons.module.hunting.safari;

import io.github.notenoughupdates.moulconfig.ChromaColour;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;
import top.babyzombie.addons.config.HuntingConfig;
import top.babyzombie.addons.config.HuntingConfig.ThrownCapsuleMode;
import top.babyzombie.addons.config.ModConfigManager;
import top.babyzombie.addons.util.ItemUtils;
import top.babyzombie.addons.util.render.RenderPhaseRegister;
import top.babyzombie.addons.util.render.WorldRenderContext;
import top.babyzombie.addons.util.render.WorldRenderUtils;
import top.babyzombie.addons.util.tracker.HypixelLocationTracker;

import java.util.ArrayList;
import java.util.List;

public final class SafariTrajectory {
    private static final Minecraft CLIENT = Minecraft.getInstance();

    // Fitted from 54 launch-to-impact ItemDisplay captures. Ordinary and
    // Masterful Critter Capsules use the same server-side trajectory.
    private static final double CAPSULE_INITIAL_VELOCITY = 0.85;
    private static final double CAPSULE_DRAG_PER_TICK = 1.0;
    private static final double CAPSULE_GRAVITY_PER_TICK = 0.02;
    private static final int MAX_TICKS = 128;
    private static final int SUBSTEPS_PER_TICK = 4;
    private static final double CAPSULE_SIDE_OFFSET = 0.16;
    private static final double CAPSULE_VERTICAL_OFFSET = 0.1;

    private SafariTrajectory() {
    }

    public static void init() {
        ThrownCapsuleTracker.init();
        RenderPhaseRegister.register(SafariTrajectory::render);
    }

    private static void render(WorldRenderContext context) {
        // 第二相机捕获期间同样渲染:重跑的 renderLevel 会再次触发 LevelRenderEvents 回调,
        // 线条按世界坐标绘制、目标跟随 mainRenderTarget(捕获期间是子相机输出),
        // 因此主视角和子视角都能看到同一套轨迹线
        Player player = CLIENT.player;
        HuntingConfig.SafariTrajectory config = config();
        if (!config.enabled || player == null || CLIENT.level == null || CLIENT.gui.screen() != null) {
            return;
        }

        HypixelLocationTracker tracker = HypixelLocationTracker.getInstance();
        boolean inSafari = tracker.isInSafari();
        boolean holdingCapsule = isCapsule(player.getMainHandItem()) || isCapsule(player.getOffhandItem());
        if (!inSafari || !holdingCapsule) {
            return;
        }

        ThrownCapsuleMode mode = config.thrownCapsuleMode == null
                ? ThrownCapsuleMode.CURRENT
                : config.thrownCapsuleMode;
        if (mode == ThrownCapsuleMode.OFF && ThrownCapsuleTracker.hasTrackedCapsule()) {
            return;
        }

        Trajectory trajectory = predict(player);
        if (trajectory.points().size() < 2) {
            return;
        }

        drawTrajectory(context, trajectory, config);
        if (mode == ThrownCapsuleMode.UNOBSTRUCTED) {
            drawTrackedCapsuleBox(context, config);
        }
    }

    private static Trajectory predict(Player player) {
        Vec3 view = player.getViewVector(1.0f).normalize();
        // 起点绑定玩家实体眼睛位置,不绑定相机:第二相机捕获期间 mainCamera 是子相机,
        // 用 camera.position() 会让轨迹线从第二相机射出,与主视角轨迹不一致
        // 用渲染插值位置:getEyePosition(1.0F) 是 tick 时刻位置,移动时线起点会跳
        Vec3 eyePos = player.getEyePosition(CLIENT.getDeltaTracker().getGameTimeDeltaPartialTick(false));
        double yaw = Math.toRadians(player.getYRot());
        // 侧向偏移 = 右眼;左右手切换开启时取反(左眼)
        double flip = ModConfigManager.get().general.handRender.swapHands ? 1.0 : -1.0;
        Vec3 pos = eyePos.add(
                flip * Math.cos(yaw) * CAPSULE_SIDE_OFFSET,
                -CAPSULE_VERTICAL_OFFSET,
                flip * Math.sin(yaw) * CAPSULE_SIDE_OFFSET
        );
        // The server-side capsule does not inherit the player's movement.
        Vec3 motion = view.scale(CAPSULE_INITIAL_VELOCITY);

        List<Vec3> points = new ArrayList<>(MAX_TICKS * SUBSTEPS_PER_TICK + 1);
        // Starting at the calibrated spawn position avoids a large near-plane bridge.
        points.add(pos);

        for (int tick = 0; tick < MAX_TICKS; tick++) {
            // Substeps only make collision checks and rendering denser. The
            // server applies drag and gravity once after each complete tick.
            for (int substep = 0; substep < SUBSTEPS_PER_TICK; substep++) {
                Vec3 next = pos.add(motion.scale(1.0 / SUBSTEPS_PER_TICK));
                Collision collision = findCollision(player, pos, next);
                if (collision != null) {
                    Vec3 landing = collision.location();
                    points.add(landing);
                    return new Trajectory(points, landing, true, collision.blockPos(), collision.face());
                }

                points.add(next);
                pos = next;
            }

            motion = motion.scale(CAPSULE_DRAG_PER_TICK).add(0.0, -CAPSULE_GRAVITY_PER_TICK, 0.0);
        }

        return new Trajectory(points, points.getLast(), false, null, null);
    }

    private static Collision findCollision(Player player, Vec3 from, Vec3 to) {
        BlockHitResult colliderHit = CLIENT.level.clip(new ClipContext(
                from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player
        ));
        BlockHitResult blockHit = colliderHit.getType() == HitResult.Type.MISS ? null : colliderHit;

        // Some decorative leaves have an outline but no normal collision shape.
        BlockHitResult outlineHit = CLIENT.level.clip(new ClipContext(
                from, to, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player
        ));
        if (outlineHit.getType() != HitResult.Type.MISS
                && CLIENT.level.getBlockState(outlineHit.getBlockPos()).is(BlockTags.LEAVES)
                && isNearer(from, outlineHit, blockHit)) {
            blockHit = outlineHit;
        }

        AABB searchBox = new AABB(from, to).inflate(0.3);
        EntityHitResult entityHit = ProjectileUtil.getEntityHitResult(
                CLIENT.level,
                player,
                from,
                to,
                searchBox,
                entity -> canHitEntity(player, entity),
                0.3f
        );

        if (entityHit != null && isNearer(from, entityHit, blockHit)) {
            return new Collision(entityHit.getLocation(), null, null);
        }
        if (blockHit != null) {
            return new Collision(blockHit.getLocation(), blockHit.getBlockPos(), blockHit.getDirection());
        }
        return null;
    }

    private static boolean canHitEntity(Player player, Entity entity) {
        return entity != player
                && !entity.isSpectator()
                && !entity.isRemoved()
                && !ThrownCapsuleTracker.shouldIgnoreCollision(entity)
                && (entity.canBeHitByProjectile() || entity instanceof Display);
    }

    private static boolean isNearer(Vec3 origin, HitResult candidate, HitResult current) {
        return current == null
                || origin.distanceToSqr(candidate.getLocation()) < origin.distanceToSqr(current.getLocation());
    }

    private static void drawTrajectory(
            WorldRenderContext context,
            Trajectory trajectory,
            HuntingConfig.SafariTrajectory config
    ) {
        List<Vec3> points = trajectory.points();
        int lineRgb = effectiveRgb(config.lineColor, 0x50FFDC);
        float red = channel(lineRgb >> 16);
        float green = channel(lineRgb >> 8);
        float blue = channel(lineRgb);
        float width = 1.5f + clamp(config.lineThickness, 0.0f, 1.0f) * 8.5f;

        // A soft outer line keeps the path readable against nearby and distant blocks.
        WorldRenderUtils.drawPolyline(context, points, red, green, blue,
                0.96f * 0.22f, 0.18f * 0.22f, true, width + 3.0f);
        WorldRenderUtils.drawPolyline(context, points, red, green, blue,
                0.96f, 0.18f, true, width);

        Vec3 landing = trajectory.landing();
        if (trajectory.hit()) {
            if (config.landingCubeEnabled) {
                int cubeRgb = effectiveRgb(config.landingColor, 0x50FF78);
                float cubeSize = clamp(config.landingSize, 0.0f, 1.0f);
                double cubeRadius = 0.10 + cubeSize * 0.72;
                drawLandingBox(context, landing, cubeRadius,
                        channel(cubeRgb >> 16), channel(cubeRgb >> 8), channel(cubeRgb),
                        0.95f, 1.5f + cubeSize * 5.0f);
            }

            if (config.landingDiscEnabled) {
                int discRgb = effectiveRgb(config.landingDiscColor, 0x50FF78);
                float discSize = clamp(config.landingDiscSize, 0.0f, 1.0f);
                double discRadius = 0.10 + discSize * 0.90;
                Direction face = trajectory.face() == null ? Direction.UP : trajectory.face();
                Vec3 normal = Vec3.atLowerCornerOf(face.getUnitVec3i());
                Vec3 discCenter = landing.add(normal.scale(0.012));
                WorldRenderUtils.drawFilledCircle(context,
                        discCenter.x, discCenter.y, discCenter.z, discRadius,
                        channel(discRgb >> 16), channel(discRgb >> 8), channel(discRgb),
                        0.30f, true, (float) normal.x, (float) normal.y, (float) normal.z);
            }

            if (config.blockHighlightEnabled && trajectory.blockPos() != null) {
                drawBlockHighlight(context, trajectory.blockPos(), config);
            }
        } else if (config.landingCubeEnabled) {
            float cubeSize = clamp(config.landingSize, 0.0f, 1.0f);
            drawLandingBox(context, landing, 0.10 + cubeSize * 0.72,
                    1.0f, 0.75f, 0.2f, 0.82f, 1.5f + cubeSize * 5.0f);
        }
    }

    private static void drawLandingBox(
            WorldRenderContext context,
            Vec3 center,
            double radius,
            float red,
            float green,
            float blue,
            float alpha,
            float lineWidth
    ) {
        WorldRenderUtils.drawWireframeBox(
                context,
                center.x - radius, center.y - radius, center.z - radius,
                center.x + radius, center.y + radius, center.z + radius,
                red, green, blue, alpha, true, lineWidth
        );
    }

    private static void drawTrackedCapsuleBox(
            WorldRenderContext context,
            HuntingConfig.SafariTrajectory config
    ) {
        Vec3 capsulePosition = ThrownCapsuleTracker.trackedRenderPosition();
        if (capsulePosition == null || !config.trackedCapsuleBoxEnabled) {
            return;
        }
        int rgb = effectiveRgb(config.landingColor, 0x50FF78);
        float size = clamp(config.landingSize, 0.0f, 1.0f);
        drawLandingBox(
                context,
                capsulePosition,
                0.10 + size * 0.72,
                channel(rgb >> 16), channel(rgb >> 8), channel(rgb),
                0.95f,
                1.5f + size * 5.0f
        );
    }

    private static void drawBlockHighlight(
            WorldRenderContext context,
            BlockPos pos,
            HuntingConfig.SafariTrajectory config
    ) {
        int rgb = effectiveRgb(config.blockHighlightColor, 0xFFDC3C);
        float thickness = clamp(config.blockHighlightThickness, 0.0f, 1.0f);
        double inset = 0.002;
        WorldRenderUtils.drawWireframeBox(
                context,
                pos.getX() - inset, pos.getY() - inset, pos.getZ() - inset,
                pos.getX() + 1.0 + inset, pos.getY() + 1.0 + inset, pos.getZ() + 1.0 + inset,
                channel(rgb >> 16), channel(rgb >> 8), channel(rgb),
                0.95f, true, 1.5f + thickness * 5.0f
        );
    }

    /// 当前预测落点(Safari 内手持胶囊时);不在 Safari/未手持返回 null。
    /// 供精灵球落地镜头等模块使用。
    public static @Nullable Vec3 predictedLanding() {
        Player player = CLIENT.player;
        if (player == null || CLIENT.level == null || CLIENT.gui.screen() != null) {
            return null;
        }
        HypixelLocationTracker tracker = HypixelLocationTracker.getInstance();
        if (!tracker.isInSafari()) {
            return null;
        }
        if (!isCapsule(player.getMainHandItem()) && !isCapsule(player.getOffhandItem())) {
            return null;
        }
        Trajectory trajectory = predict(player);
        return trajectory.points().size() < 2 ? null : trajectory.landing();
    }

    static HuntingConfig.SafariTrajectory config() {
        return ModConfigManager.get().hunting.safari.trajectory;
    }

    static boolean isCapsule(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        String id = ItemUtils.getSkyblockId(stack);
        return "CRITTER_CAPSULE".equals(id) || "MASTERFUL_CRITTER_CAPSULE".equals(id);
    }

    private static float channel(int value) {
        return (value & 0xFF) / 255.0f;
    }

    private static int effectiveRgb(ChromaColour colour, int fallback) {
        return colour == null ? fallback : colour.getEffectiveColourRGB();
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    private record Collision(Vec3 location, BlockPos blockPos, Direction face) {
    }

    private record Trajectory(List<Vec3> points, Vec3 landing, boolean hit, BlockPos blockPos, Direction face) {
    }
}
