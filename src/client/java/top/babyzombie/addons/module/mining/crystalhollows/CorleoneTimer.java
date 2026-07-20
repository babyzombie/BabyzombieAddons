package top.babyzombie.addons.module.mining.crystalhollows;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.phys.Vec3;
import top.babyzombie.addons.config.ModConfigManager;
import top.babyzombie.addons.util.PlayerUtils;
import top.babyzombie.addons.util.ServerTick;
import top.babyzombie.addons.util.render.RenderPhaseRegister;
import top.babyzombie.addons.util.render.WorldRenderContext;
import top.babyzombie.addons.util.render.WorldTextRenderer;
import top.babyzombie.addons.util.tracker.HypixelLocationTracker;

import java.util.UUID;

/**
 * 水晶矿洞 Team Treasurite (Corleone) boss 死亡倒计时。
 * 检测方式：实体类型 PLAYER → 名字 "Team Treasurite" → 皮肤 URL 匹配。
 * 死亡后在原地显示 120 秒不透明浮空倒计时，分两行：名字 + 秒数。
 * 60 秒前为红色（安全），60 秒后变金色（可能刷新）。
 */
public final class CorleoneTimer {

    private static final String BOSS_NAME = "Team Treasurite";
    private static final String SKIN_URL =
        "faf0b0360f5629a38a074cd929b59f2769b2d0752a5d372c5469a372a567d918";
    private static final long COUNTDOWN_MS = 120_000;
    private static final long READY_THRESHOLD_MS = 60_000;
    private static final int NAME_COLOR = 0xFFAA0000;
    private static final int NAME_COLOR_READY = 0xFFFFAA00;
    private static final int COUNTDOWN_COLOR = 0xFFFF5555;
    private static final int COUNTDOWN_COLOR_READY = 0xFF55FF55;
    private static final float SCALE = 0.04f;

    private static UUID trackedUuid;
    private static Vec3 deathPos;
    private static long countdownEnd;

    private CorleoneTimer() {}

    public static void init() {
        ClientTickEvents.END_CLIENT_TICK.register(CorleoneTimer::onTick);
        RenderPhaseRegister.register(CorleoneTimer::onRender);
    }

    /** Called from MiningModule on world change. */
    public static void reset() {
        trackedUuid = null;
        deathPos = null;
        countdownEnd = 0;
    }

    // ===== Tick =====

    private static void onTick(Minecraft client) {
        if (!ModConfigManager.get().mining.crystalHollows.corleoneTimer) return;
        if (!isInCrystalHollows()) return;
        var player = client.player;
        if (player == null) return;

        long now = ServerTick.getTime();

        // 倒计时已过期 → 清理
        if (deathPos != null && now >= countdownEnd) {
            reset();
        }

        // 倒计时进行中 → 不扫描新 boss
        if (deathPos != null) return;

        // 检查已追踪的 boss 是否死了
        if (trackedUuid != null) {
            Entity tracked = findTrackedBoss(client);
            if (tracked == null) {
                // 实体已消失（超出范围或卸载），清追踪
                trackedUuid = null;
            } else if (!tracked.isAlive()) {
                // boss 死了 → 启动倒计时
                deathPos = tracked.position();
                countdownEnd = now + COUNTDOWN_MS;
                trackedUuid = null;
            }
            return;
        }

        // 扫描周围实体找 boss
        var aabb = player.getBoundingBox().inflate(32);
        for (var entity : player.level().getEntities(player, aabb, e -> !e.is(player))) {
            if (!entity.isAlive()) continue;
            if (entity.getType() != EntityTypes.PLAYER) continue;

            String name = entity.getName().getString();
            if (!BOSS_NAME.equals(name)) continue;

            String skinUrl = PlayerUtils.getSkinTextureUrl(PlayerUtils.getPlayerProfile(entity));
            if (skinUrl == null || !skinUrl.endsWith(SKIN_URL)) continue;

            trackedUuid = entity.getUUID();
            return;
        }
    }

    // ===== Render =====

    private static void onRender(WorldRenderContext ctx) {
        if (deathPos == null) return;

        long rem = countdownEnd - ServerTick.getTime();
        if (rem <= 0) return;

        String countText = String.format("%.1fs", rem / 1000.0);
        boolean ready = rem <= READY_THRESHOLD_MS;
        int nameCol = ready ? NAME_COLOR_READY : NAME_COLOR;
        int countCol = ready ? COUNTDOWN_COLOR_READY : COUNTDOWN_COLOR;

        // 第一行：boss 名字
        WorldTextRenderer.renderString(ctx, "Boss Corleone",
            deathPos.x, deathPos.y + 2, deathPos.z,
            nameCol, SCALE, false, 0);

        // 第二行：倒计时数字 (通过 fontYOffset 向下偏移)
        WorldTextRenderer.renderString(ctx, countText,
            deathPos.x, deathPos.y + 2, deathPos.z,
            countCol, SCALE, false, 10);
    }

    // ===== Helpers =====

    /** 在当前世界中找到已追踪的 boss 实体。找不到返回 null。 */
    private static Entity findTrackedBoss(Minecraft client) {
        if (trackedUuid == null) return null;
        var player = client.player;
        if (player == null) return null;

        var aabb = player.getBoundingBox().inflate(40);
        for (var entity : player.level().getEntities(player, aabb, e -> !e.is(player))) {
            if (entity.getUUID().equals(trackedUuid)) {
                return entity;
            }
        }
        return null;
    }

    private static boolean isInCrystalHollows() {
        return HypixelLocationTracker.getInstance().isIn("Crystal Hollows");
    }
}
