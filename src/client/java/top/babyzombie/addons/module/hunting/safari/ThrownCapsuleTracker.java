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

    private static long clientTick;
    private static boolean useWasDown;
    private static PendingThrow pendingThrow;
    private static int trackedEntityId = -1;
    private static long trackedAtTick;

    private ThrownCapsuleTracker() {
    }

    public static void init() {
        ClientTickEvents.END_CLIENT_TICK.register(ThrownCapsuleTracker::onClientTick);
        ClientEntityEvents.ENTITY_LOAD.register((entity, level) -> considerLoadedEntity(entity));
        ClientEntityEvents.ENTITY_UNLOAD.register((entity, level) -> {
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
        if (useDown && !useWasDown && client.screen == null
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

        if (trackedEntityId >= 0) {
            Entity tracked = client.level.getEntity(trackedEntityId);
            if (tracked == null || tracked.isRemoved() || clientTick - trackedAtTick > TRACK_TIMEOUT_TICKS) {
                clearTracked();
            }
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
        trackedEntityId = entity.getId();
        trackedAtTick = clientTick;
        pendingThrow = null;
    }

    public static boolean hasTrackedCapsule() {
        return trackedEntityId >= 0
                && CLIENT.level != null
                && CLIENT.level.getEntity(trackedEntityId) != null;
    }

    public static boolean shouldIgnoreCollision(Entity entity) {
        return mode() == ThrownCapsuleMode.UNOBSTRUCTED && entity.getId() == trackedEntityId;
    }

    public static boolean shouldHide(Entity entity) {
        return shouldIgnoreCollision(entity);
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
