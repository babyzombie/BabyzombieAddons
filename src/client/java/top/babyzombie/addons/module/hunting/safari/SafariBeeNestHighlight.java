package top.babyzombie.addons.module.hunting.safari;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientEntityEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLevelEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BeehiveBlockEntity;
import top.babyzombie.addons.config.HuntingConfig;
import top.babyzombie.addons.config.ModConfigManager;
import top.babyzombie.addons.util.render.RenderPhaseRegister;
import top.babyzombie.addons.util.render.WorldRenderContext;
import top.babyzombie.addons.util.render.WorldRenderUtils;
import top.babyzombie.addons.util.tracker.HypixelLocationTracker;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 森林蜂巢提示：交互蜂巢会刷出一只蜜蜂（一次性），服务器不下发蜂巢状态，
 * 只能靠"蜜蜂实体出现"反推——Bee 实体加载时给周围 {@link #NEST_MARK_RADIUS}
 * 格内的蜂巢（bee_nest）打上已交互标记，渲染时只高亮未标记的蜂巢。
 * 蜂箱（beehive）代表该位置本次没刷蜜蜂，忽略。多人 Safari 队友交互的
 * 蜜蜂也会触发实体加载，标记同样生效。
 */
public final class SafariBeeNestHighlight {
    private static final Minecraft CLIENT = Minecraft.getInstance();
    /** 蜜蜂实体加载时，周围多少格内的蜂巢视为已交互 */
    private static final int NEST_MARK_RADIUS = 2;
    /** 扫描半径（chunk，Hypixel 视距 8 ≈ 128 格） */
    private static final int SCAN_CHUNK_RADIUS = 8;
    /** 方块扫描节流（tick） */
    private static final int SCAN_INTERVAL_TICKS = 10;

    /** 已交互（出过蜜蜂）的蜂巢坐标 */
    private static final Set<BlockPos> interactedNests = new HashSet<>();
    /** 未交互蜂巢缓存，按节流刷新 */
    private static final List<BlockPos> availableNests = new ArrayList<>();

    private static long nextScanTick;

    private SafariBeeNestHighlight() {}

    public static void init() {
        ClientEntityEvents.ENTITY_LOAD.register((entity, level) -> {
            // 主世界也有蜜蜂和蜂巢,只在 Safari 内标记,避免污染状态
            if (entity.getType() == EntityTypes.BEE && HypixelLocationTracker.getInstance().isInSafari()) {
                markNestsAround(entity.blockPosition());
            }
        });
        ClientLevelEvents.AFTER_CLIENT_LEVEL_CHANGE.register((_, _) -> resetAll());
        ClientTickEvents.END_CLIENT_TICK.register(SafariBeeNestHighlight::onTick);
        RenderPhaseRegister.register(SafariBeeNestHighlight::render);
    }

    /** Bee 实体出现 → 周围 2 格内的蜂巢视为已交互（蜜蜂消失不清，蜂巢是一次性的） */
    private static void markNestsAround(BlockPos beePos) {
        var level = CLIENT.level;
        if (level == null) return;
        int x = beePos.getX(), y = beePos.getY(), z = beePos.getZ();
        for (BlockPos pos : BlockPos.betweenClosed(
                x - NEST_MARK_RADIUS, y - NEST_MARK_RADIUS, z - NEST_MARK_RADIUS,
                x + NEST_MARK_RADIUS, y + NEST_MARK_RADIUS, z + NEST_MARK_RADIUS)) {
            if (level.getBlockState(pos).is(Blocks.BEE_NEST)) {
                interactedNests.add(pos.immutable());
            }
        }
    }

    private static void onTick(Minecraft client) {
        if (client.player == null || client.level == null) return;
        if (!HypixelLocationTracker.getInstance().isInSafari()) {
            resetAll();
            return;
        }
        if (!config().beeNestHighlight || client.level.getGameTime() < nextScanTick) return;
        nextScanTick = client.level.getGameTime() + SCAN_INTERVAL_TICKS;
        scanNests(client.player.blockPosition());
    }

    /** 扫描玩家周围未交互且属于森林分区的蜂巢（按 chunk 遍历方块实体，不逐格扫方块） */
    private static void scanNests(BlockPos center) {
        availableNests.clear();
        var level = CLIENT.level;
        if (level == null) return;
        int chunkX = center.getX() >> 4, chunkZ = center.getZ() >> 4;
        int y = center.getY();
        for (int dcx = -SCAN_CHUNK_RADIUS; dcx <= SCAN_CHUNK_RADIUS; dcx++) {
            for (int dcz = -SCAN_CHUNK_RADIUS; dcz <= SCAN_CHUNK_RADIUS; dcz++) {
                int cx = chunkX + dcx, cz = chunkZ + dcz;
                BlockPos chunkOrigin = new BlockPos(cx << 4, y, cz << 4);
                if (!level.isLoaded(chunkOrigin)) continue;
                var chunk = level.getChunkAt(chunkOrigin);
                for (var entry : chunk.getBlockEntities().entrySet()) {
                    if (!(entry.getValue() instanceof BeehiveBlockEntity)) continue;
                    BlockPos pos = entry.getKey();
                    // 蜂箱（beehive）与蜂巢共用 BeehiveBlockEntity，按方块区分，蜂箱忽略
                    if (!level.getBlockState(pos).is(Blocks.BEE_NEST)) continue;
                    BlockPos immutable = pos.immutable();
                    if (!interactedNests.contains(immutable)
                            && SafariZoneUtil.zoneOf(immutable) == SafariZoneUtil.SafariZone.FOREST) {
                        availableNests.add(immutable);
                    }
                }
            }
        }
    }

    private static void render(WorldRenderContext context) {
        if (!config().beeNestHighlight) return;
        if (CLIENT.player == null || CLIENT.level == null) return;
        if (!HypixelLocationTracker.getInstance().isInSafari()) return;
        // 蜂巢只在森林分区
        if (SafariZoneUtil.zoneOf(CLIENT.player.blockPosition()) != SafariZoneUtil.SafariZone.FOREST) return;
        if (availableNests.isEmpty()) return;

        int rgb = config().beeGlowColor.getEffectiveColourRGB();
        float red = (rgb >> 16 & 0xFF) / 255.0f;
        float green = (rgb >> 8 & 0xFF) / 255.0f;
        float blue = (rgb & 0xFF) / 255.0f;
        // 实心盒子比蜂巢方块外扩一点,不与方块表面贴合
        double expand = 0.01;
        for (BlockPos pos : availableNests) {
            WorldRenderUtils.drawFilledBox(context,
                    pos.getX() - expand, pos.getY() - expand, pos.getZ() - expand,
                    pos.getX() + 1.0 + expand, pos.getY() + 1.0 + expand, pos.getZ() + 1.0 + expand,
                    red, green, blue, 0.30f, true);
        }
    }

    private static HuntingConfig.Forest config() {
        return ModConfigManager.get().hunting.safari.forest;
    }

    private static void resetAll() {
        interactedNests.clear();
        availableNests.clear();
        nextScanTick = 0;
    }
}
