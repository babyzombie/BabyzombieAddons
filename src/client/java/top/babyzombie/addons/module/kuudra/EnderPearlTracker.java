package top.babyzombie.addons.module.kuudra;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLevelEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownEnderpearl;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;
import top.babyzombie.addons.util.ChatUtils;

/**
 * 玩家投掷的末影珍珠追踪:优先按 owner 过滤出自己的珍珠(单机/新版服务端);
 * Hypixel 1.8 核心下发的珍珠可能不带 owner,退化用"use 按键武装 + 新实体匹配"
 * (与胶囊同款机制)。
 * 多颗并存时取实体 ID 最大者(实体 id 全局递增,最新生成 = 最新投出;
 * tickCount 是存活时间,最大反而是最早丢的,不能用)。
 * 同时维护珍珠上一 tick 位置,供轨迹预测做速度差分(服务器位置同步下
 * deltaMovement 混合了客户端本地物理,差分更接近服务器真实速度)。
 */
public final class EnderPearlTracker {
    private static final Minecraft CLIENT = Minecraft.getInstance();

    /// use 武装匹配的超时(ticks):按下 use 后这么久没匹配到新珍珠就放弃
    private static final int PENDING_TIMEOUT_TICKS = 40;

    private static long clientTick;
    private static boolean useWasDown;
    private static @Nullable PendingThrow pendingThrow;
    private static @Nullable ThrownEnderpearl trackedPearl;
    private static @Nullable Vec3 lastTickPos;
    /** 最近两个 tick 的速度(位置差分):prevVelocity 为上一 tick,currentVelocity 为当前 tick */
    private static @Nullable Vec3 prevVelocity;
    private static @Nullable Vec3 currentVelocity;
    /** 服务器位置同步跳变校准的速度(跳变位移 ÷ 2);差分混有客户端本地物理,校准值更接近服务器真实速度 */
    private static @Nullable Vec3 serverVelocity;
    private static boolean lastWasJump;
    /// P1 阶段结束标记:收到"补给收集完"消息(P2 开始)后为 true,"只在阶段1生效"开关不再生效
    private static boolean phase1Ended;

    private EnderPearlTracker() {
    }

    public static void init() {
        ClientTickEvents.END_CLIENT_TICK.register(EnderPearlTracker::onClientTick);

        // ── P2 开始消息检测(与 KuudraPhaseTimer 同款消息,但不受其配置开关影响) ──
        ClientReceiveMessageEvents.ALLOW_GAME.register((message, overlay) -> {
            if (overlay) return true;
            String text = ChatUtils.stripColor(message.getString());
            if (text.contains("OMG! Great work collecting my supplies")) {
                phase1Ended = true;  // 补给收集完,阶段1结束
            }
            return true;
        });
        ClientLevelEvents.AFTER_CLIENT_LEVEL_CHANGE.register((client, world) -> {
            phase1Ended = false;
            pendingThrow = null;
            clearTracked();
        });
    }

    private static void onClientTick(Minecraft client) {
        clientTick++;
        Player player = client.player;
        if (player == null || client.level == null) {
            clear();
            return;
        }

        // ── use 按键武装(与胶囊同款:识别 Hypixel 无 owner 同步的珍珠) ──
        boolean useDown = client.options.keyUse.isDown();
        if (useDown && !useWasDown && client.screen == null
                && EnderPearlTrajectory.holdingPearl(player)) {
            pendingThrow = new PendingThrow(
                    clientTick,
                    player.getEyePosition(1.0F),
                    player.getViewVector(1.0f).normalize()
            );
            clearTracked();
        }
        useWasDown = useDown;

        // ── 1) owner 匹配优先(单机/新版服务端) ──
        ThrownEnderpearl best = null;
        for (Entity entity : client.level.entitiesForRendering()) {
            if (entity.getType() == EntityType.ENDER_PEARL && entity instanceof ThrownEnderpearl pearl
                    && pearl.getOwner() == player) {
                if (best == null || entity.getId() > best.getId()) {
                    best = pearl;
                }
            }
        }
        if (best != null) {
            adopt(best);
            pendingThrow = null;
            return;
        }

        // ── 2) owner 无效时,use 武装匹配新出现的珍珠 ──
        if (pendingThrow != null) {
            findPendingPearl();
            if (pendingThrow != null && clientTick - pendingThrow.tick > PENDING_TIMEOUT_TICKS) {
                pendingThrow = null;
            }
        }

        // ── 3) 已追踪珍珠失效清理 ──
        ThrownEnderpearl pearl = trackedPearl;
        if (pearl != null && (pearl.isRemoved() || pearl.level() != client.level)) {
            clearTracked();
        }
    }

    private static void findPendingPearl() {
        if (CLIENT.level == null || pendingThrow == null || trackedPearl != null) {
            return;
        }
        ThrownEnderpearl best = null;
        double bestScore = Double.NEGATIVE_INFINITY;
        for (Entity entity : CLIENT.level.entitiesForRendering()) {
            if (!(entity instanceof ThrownEnderpearl pearl) || entity.tickCount > 20) {
                continue;
            }
            Vec3 offset = entity.position().subtract(pendingThrow.eyePosition);
            double distance = offset.length();
            double alignment = distance < 1.0E-6
                    ? 0.0
                    : offset.scale(1.0 / distance).dot(pendingThrow.viewDirection);
            double score = alignment * 20.0 - distance;
            if (alignment >= 0.30 && score > bestScore) {
                best = pearl;
                bestScore = score;
            }
        }
        if (best != null) {
            adopt(best);
            pendingThrow = null;
        }
    }

    /** 接管珍珠:换球时重置速度历史,并推进位置差分。 */
    private static void adopt(ThrownEnderpearl pearl) {
        if (trackedPearl != pearl) {
            trackedPearl = pearl;
            lastTickPos = null;
            prevVelocity = null;
            currentVelocity = null;
            serverVelocity = null;
            lastWasJump = false;
        }
        Vec3 pos = pearl.position();
        if (lastTickPos != null) {
            Vec3 delta = pos.subtract(lastTickPos);
            if (prevVelocity != null && delta.length() > prevVelocity.length() * 1.5) {
                // 服务器位置同步:该 tick 位移是 2 tick 的(跳变),校准服务器速度。
                // 差分混有客户端本地物理(26.1),跳变后的校准值更接近服务器真实速度,
                // 预测落点不再每 tick 抖动
                serverVelocity = delta.scale(0.5);
                lastWasJump = true;
            } else {
                lastWasJump = false;
            }
            prevVelocity = currentVelocity;
            currentVelocity = delta;
        }
        lastTickPos = pos;
    }

    private static void clear() {
        pendingThrow = null;
        clearTracked();
    }

    private static void clearTracked() {
        trackedPearl = null;
        lastTickPos = null;
        prevVelocity = null;
        currentVelocity = null;
        serverVelocity = null;
        lastWasJump = false;
    }

    private record PendingThrow(long tick, Vec3 eyePosition, Vec3 viewDirection) {
    }

    /** 玩家最新投出的珍珠(客户端可见);无则 null。 */
    public static @Nullable ThrownEnderpearl trackedPearl() {
        ThrownEnderpearl pearl = trackedPearl;
        if (pearl == null || pearl.isRemoved() || pearl.level() != CLIENT.level) {
            return null;
        }
        return pearl;
    }

    /** P1 是否已结束(P2 开始消息收到后为 true)。 */
    public static boolean isPhase1Ended() {
        return phase1Ended;
    }

    /**
     * 珍珠当前速度,按 partialTick 在最近两个 tick 差分速度间插值;
     * 同步跳变后的第一个 tick 用服务器校准速度(更接近服务器真实速度)。
     * 直接读 tick 差分(20Hz)会让轨迹预测线/相机朝向每 tick 跳变,
     * 插值后帧间平滑过渡。换球后的第一个 tick 内返回 null。
     */
    public static @Nullable Vec3 velocity(float partialTick) {
        if (lastWasJump && serverVelocity != null) {
            return serverVelocity;
        }
        if (currentVelocity == null) {
            return null;
        }
        if (prevVelocity == null) {
            return currentVelocity;
        }
        return prevVelocity.lerp(currentVelocity, partialTick);
    }
}
