package top.babyzombie.addons.module.hunting.safari;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientEntityEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import top.babyzombie.addons.config.HuntingConfig.ThrownCapsuleMode;
import top.babyzombie.addons.event.EntityRenderEvents;
import top.babyzombie.addons.util.tracker.HypixelLocationTracker;

import java.util.HashSet;
import java.util.Set;

public final class ThrownCapsuleTracker {
    private static final Minecraft CLIENT = Minecraft.getInstance();
    private static final int PENDING_TIMEOUT_TICKS = 40;
    private static final int TRACK_TIMEOUT_TICKS = 200;
    private static final double HIDE_NEARBY_RADIUS = 1.0;

    private static long clientTick;
    private static boolean useWasDown;
    private static PendingThrow pendingThrow;
    // 所有已识别为玩家投掷的精灵球实体。旧球可能还在飞行(玩家已举出下一个),
    // 轨迹预测必须忽略全部,否则飞行中的球会截断新的预测线。
    private static final Set<Integer> TRACKED_IDS = new HashSet<>();
    private static int trackedEntityId = -1;
    private static long trackedAtTick;

    private ThrownCapsuleTracker() {
    }

    public static void init() {
        ClientTickEvents.END_CLIENT_TICK.register(ThrownCapsuleTracker::onClientTick);
        ClientEntityEvents.ENTITY_LOAD.register((entity, level) -> considerLoadedEntity(entity));
        ClientEntityEvents.ENTITY_UNLOAD.register((entity, level) -> {
            TRACKED_IDS.remove(entity.getId());
            if (entity.getId() == trackedEntityId) {
                clearTracked();
            }
        });
        EntityRenderEvents.BEFORE_RENDER.register(ThrownCapsuleTracker::shouldHide);
    }

    private static void onClientTick(Minecraft client) {
        clientTick++;
        Player player = client.player;
        if (!SafariTrajectory.config().enabled || player == null || client.level == null || !isInSafari()) {
            reset();
            return;
        }

        boolean useDown = client.options.keyUse.isDown();
        if (useDown && !useWasDown && client.gui.screen() == null
                && (SafariTrajectory.isCapsule(player.getMainHandItem())
                || SafariTrajectory.isCapsule(player.getOffhandItem()))) {
            arm(player);
        }
        useWasDown = useDown;

        if (pendingThrow != null) {
            findPendingEntity();
            if (pendingThrow != null && clientTick - pendingThrow.tick > PENDING_TIMEOUT_TICKS) {
                pendingThrow = null;
            }
        }

        // 已投掷球离开世界即失效,防止实体 ID 复用后误忽略无关实体。
        TRACKED_IDS.removeIf(id -> {
            Entity entity = CLIENT.level.getEntity(id);
            return entity == null || entity.isRemoved();
        });
        if (trackedEntityId >= 0 && (clientTick - trackedAtTick > TRACK_TIMEOUT_TICKS
                || !TRACKED_IDS.contains(trackedEntityId))) {
            TRACKED_IDS.remove(trackedEntityId);
            clearTracked();
        }
    }

    private static void arm(Player player) {
        Set<Integer> existingDisplays = new HashSet<>();
        for (Entity entity : CLIENT.level.entitiesForRendering()) {
            if (entity instanceof Display.ItemDisplay) {
                existingDisplays.add(entity.getId());
            }
        }
        pendingThrow = new PendingThrow(
                clientTick,
                player.getEyePosition(),
                player.getViewVector(1.0f).normalize(),
                existingDisplays
        );
        clearTracked();
    }

    private static void considerLoadedEntity(Entity entity) {
        if (pendingThrow == null || trackedEntityId >= 0 || !matchesPending(entity)) {
            return;
        }
        Vec3 offset = entity.position().subtract(pendingThrow.eyePosition);
        double distance = offset.length();
        double alignment = distance < 1.0E-6
                ? 0.0
                : offset.scale(1.0 / distance).dot(pendingThrow.viewDirection);
        if (distance <= 6.0 && alignment >= 0.40) {
            track(entity);
        }
    }

    private static void findPendingEntity() {
        if (CLIENT.level == null || pendingThrow == null || trackedEntityId >= 0) {
            return;
        }
        Entity best = null;
        double bestScore = Double.NEGATIVE_INFINITY;
        for (Entity entity : CLIENT.level.entitiesForRendering()) {
            if (!matchesPending(entity)) {
                continue;
            }
            Vec3 offset = entity.position().subtract(pendingThrow.eyePosition);
            double distance = offset.length();
            double alignment = distance < 1.0E-6
                    ? 0.0
                    : offset.scale(1.0 / distance).dot(pendingThrow.viewDirection);
            double score = alignment * 20.0 - distance;
            if (alignment >= 0.30 && score > bestScore) {
                best = entity;
                bestScore = score;
            }
        }
        if (best != null) {
            track(best);
        }
    }

    private static boolean matchesPending(Entity entity) {
        return pendingThrow != null
                && entity instanceof Display.ItemDisplay
                && !pendingThrow.existingDisplayIds.contains(entity.getId())
                && entity.tickCount <= 20
                && entity.position().distanceToSqr(pendingThrow.eyePosition) <= 576.0;
    }

    private static void track(Entity entity) {
        TRACKED_IDS.add(entity.getId());
        trackedEntityId = entity.getId();
        trackedAtTick = clientTick;
        pendingThrow = null;
    }

    public static boolean hasTrackedCapsule() {
        return trackedEntityId >= 0
                && CLIENT.level != null
                && CLIENT.level.getEntity(trackedEntityId) != null;
    }

    /**
     * 服务器把投出的球作为携带 CRITTER_CAPSULE item 的 ItemDisplay 下发,
     * 直接按物品判断即可,不依赖投掷时机。
     */
    public static boolean isThrownCapsule(Entity entity) {
        return entity instanceof Display.ItemDisplay itemDisplay
                && SafariTrajectory.isCapsule(itemDisplay.getItemStack());
    }

    public static boolean shouldIgnoreCollision(Entity entity) {
        // 优先按物品判断(服务器下发的球带 CRITTER_CAPSULE item);
        // TRACKED_IDS 兜底实体数据同步延迟/异常的情况。
        return isThrownCapsule(entity) || TRACKED_IDS.contains(entity.getId());
    }

    public static boolean shouldHide(Entity entity) {
        if (!(entity instanceof Display.ItemDisplay)) {
            return false;
        }
        // UNOBSTRUCTED 模式隐藏当前投出的球(用框替代显示);隐藏必须与画框配对,
        // 否则识别失败时球会消失却没有框代替。
        if (mode() == ThrownCapsuleMode.UNOBSTRUCTED && entity.getId() == trackedEntityId) {
            return true;
        }
        // 隐身附近精灵球:独立开关,隐藏玩家头附近的全部球,对任意模式生效。
        if (SafariTrajectory.config().hideNearbyCapsules && isThrownCapsule(entity)) {
            Player player = CLIENT.player;
            return player != null
                    && player.getEyePosition().distanceToSqr(entity.position()) <= HIDE_NEARBY_RADIUS * HIDE_NEARBY_RADIUS;
        }
        return false;
    }

    public static Vec3 trackedRenderPosition() {
        if (mode() != ThrownCapsuleMode.UNOBSTRUCTED || CLIENT.level == null || trackedEntityId < 0) {
            return null;
        }
        Entity entity = CLIENT.level.getEntity(trackedEntityId);
        if (entity == null || entity.isRemoved()) {
            return null;
        }
        float partialTick = CLIENT.getDeltaTracker().getGameTimeDeltaPartialTick(false);
        return entity.getPosition(partialTick);
    }

    private static ThrownCapsuleMode mode() {
        ThrownCapsuleMode mode = SafariTrajectory.config().thrownCapsuleMode;
        return mode == null ? ThrownCapsuleMode.CURRENT : mode;
    }

    private static boolean isInSafari() {
        HypixelLocationTracker tracker = HypixelLocationTracker.getInstance();
        return tracker != null && tracker.isInSafari();
    }

    private static void reset() {
        useWasDown = false;
        pendingThrow = null;
        TRACKED_IDS.clear();
        clearTracked();
    }

    private static void clearTracked() {
        trackedEntityId = -1;
        trackedAtTick = 0L;
    }

    private record PendingThrow(
            long tick,
            Vec3 eyePosition,
            Vec3 viewDirection,
            Set<Integer> existingDisplayIds
    ) {
    }
}
