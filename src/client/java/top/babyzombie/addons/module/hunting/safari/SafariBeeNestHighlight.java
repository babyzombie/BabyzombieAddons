package top.babyzombie.addons.module.hunting.safari;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientChunkEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientEntityEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLevelEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.level.block.Blocks;
import top.babyzombie.addons.config.HuntingConfig;
import top.babyzombie.addons.config.ModConfigManager;
import top.babyzombie.addons.event.HypixelLocationEvents;
import top.babyzombie.addons.util.render.RenderPhaseRegister;
import top.babyzombie.addons.util.render.WorldRenderContext;
import top.babyzombie.addons.util.render.WorldRenderUtils;
import top.babyzombie.addons.util.tracker.HypixelLocationTracker;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Set;

/**
 * 森林蜂巢提示：交互蜂巢会刷出一只蜜蜂（一次性），服务器不下发蜂巢状态，
 * 只能靠"蜜蜂实体出现"反推——Bee 实体加载时给周围 {@link #NEST_MARK_RADIUS}
 * 格内的蜂巢（bee_nest）打上已交互标记，渲染时只高亮未标记的蜂巢。
 * 蜂箱（beehive）代表该位置本次没刷蜜蜂，忽略。多人 Safari 队友交互的
 * 蜜蜂也会触发实体加载，标记同样生效。
 *
 * 扫描策略：事件驱动增量扫描，不做周期性全量重扫——区块 CHUNK_LOAD 时
 * 入队（每 tick 只扫一个，摊平开销），扫过的 chunk 在本次 Safari 内不再
 * 重扫（蜂巢状态不变）；非森林 chunk 粗判直接跳过。进入 Safari 时把周围
 * 已加载 chunk 批量入队（走过的 chunk 不会重新触发 CHUNK_LOAD）。
 */
public final class SafariBeeNestHighlight {
    private static final Minecraft CLIENT = Minecraft.getInstance();
    /** 蜜蜂实体加载时，周围多少格内的蜂巢视为已交互 */
    private static final int NEST_MARK_RADIUS = 2;
    /** 进入 Safari 时批量入队的扫描范围（chunk，3 ≈ 48 格，覆盖高亮可见范围） */
    private static final int SCAN_CHUNK_RADIUS = 3;

    /** 已交互（出过蜜蜂）的蜂巢坐标 */
    private static final Set<BlockPos> interactedNests = new HashSet<>();
    /** 未交互蜂巢（渲染用），增量维护：扫描时加入，出蜜蜂时移除 */
    private static final Set<BlockPos> availableNests = new HashSet<>();
    /** 已扫描过的 chunk（本次 Safari 内不重扫） */
    private static final Set<Long> scannedChunks = new HashSet<>();
    /** 待扫描 chunk 队列，每 tick 处理一个 */
    private static final ArrayDeque<Long> pendingChunks = new ArrayDeque<>();

    /** 当前是否在 Safari（由 LOCATION_UPDATE 维护，跟随区域变化重置状态） */
    private static boolean inSafari;

    private SafariBeeNestHighlight() {}

    public static void init() {
        ClientChunkEvents.CHUNK_LOAD.register((world, chunk) -> {
            // 区块刚加载 → 本次会话内首次扫描它的机会
            if (inSafari && config().beeNestHighlight) {
                enqueueChunk(chunk.getPos());
            }
        });
        ClientEntityEvents.ENTITY_LOAD.register((entity, level) -> {
            // 主世界也有蜜蜂和蜂巢,只在 Safari 内标记,避免污染状态
            if (entity.getType() == EntityTypes.BEE && inSafari) {
                markNestsAround(entity.blockPosition());
            }
        });
        HypixelLocationEvents.LOCATION_UPDATE.register(data -> {
            boolean now = data.isInSafari();
            if (now == inSafari) return;
            inSafari = now;
            if (now) {
                // 进入 Safari：把周围已加载 chunk 批量入队，不依赖 CHUNK_LOAD 补发
                if (config().beeNestHighlight) {
                    enqueueSurroundingChunks();
                }
            } else {
                resetAll();
            }
        });
        ClientLevelEvents.AFTER_CLIENT_LEVEL_CHANGE.register((_, _) -> {
            inSafari = false;
            resetAll();
        });
        ClientTickEvents.END_CLIENT_TICK.register(SafariBeeNestHighlight::onTick);
        RenderPhaseRegister.register(SafariBeeNestHighlight::render);
    }

    /** Bee 实体出现 → 周围 2 格内的蜂巢视为已交互（蜜蜂消失不清，蜂巢是一次性的），并从高亮列表移除 */
    private static void markNestsAround(BlockPos beePos) {
        var level = CLIENT.level;
        if (level == null) return;
        int x = beePos.getX(), y = beePos.getY(), z = beePos.getZ();
        for (BlockPos pos : BlockPos.betweenClosed(
                x - NEST_MARK_RADIUS, y - NEST_MARK_RADIUS, z - NEST_MARK_RADIUS,
                x + NEST_MARK_RADIUS, y + NEST_MARK_RADIUS, z + NEST_MARK_RADIUS)) {
            if (level.getBlockState(pos).is(Blocks.BEE_NEST)) {
                BlockPos immutable = pos.immutable();
                interactedNests.add(immutable);
                availableNests.remove(immutable);
            }
        }
    }

    /** 每 tick 从队列扫一个 chunk；队列空时兜底补扫玩家所在 chunk（开关中途打开的场景） */
    private static void onTick(Minecraft client) {
        if (client.level == null || client.player == null || !inSafari) return;
        Long key = pendingChunks.poll();
        if (key != null) {
            scanChunk(key);
            return;
        }
        // 开关中途打开时玩家所在 chunk 早已加载、不会重发 CHUNK_LOAD，补扫一次
        if (config().beeNestHighlight) {
            int cx = client.player.blockPosition().getX() >> 4;
            int cz = client.player.blockPosition().getZ() >> 4;
            if (!scannedChunks.contains(ChunkPos.pack(cx, cz))) {
                enqueueChunk(cx, cz);
            }
        }
    }

    /**
     * 扫描一个 chunk 里的蜂巢。每个 chunk 本次 Safari 内只扫一次。
     * 不用 chunk.getBlockEntities()：26.2 客户端对 BlockEntity 惰性实例化，
     * 远处的蜂巢未实例化时不会出现在列表里，要等玩家走近/渲染才补上。
     * 方块状态数据总是完整，直接逐格 getBlockState 找 bee_nest 最可靠。
     */
    private static void scanChunk(long key) {
        if (scannedChunks.contains(key)) return;
        var level = CLIENT.level;
        var player = CLIENT.player;
        if (level == null || player == null) return;
        int cx = ChunkPos.getX(key), cz = ChunkPos.getZ(key);
        BlockPos origin = new BlockPos(cx << 4, player.blockPosition().getY(), cz << 4);
        if (!level.isLoaded(origin)) return; // 未加载成功，等 CHUNK_LOAD / 兜底重新入队
        scannedChunks.add(key);

        var chunk = level.getChunkAt(origin);
        var sections = chunk.getSections();
        int xBase = cx << 4, zBase = cz << 4;
        for (int i = 0; i < sections.length; i++) {
            if (sections[i].hasOnlyAir()) continue;
            int yBase = chunk.getSectionYFromSectionIndex(i) << 4;
            for (int dy = 0; dy < 16; dy++) {
                int py = yBase + dy;
                for (int dz = 0; dz < 16; dz++) {
                    int pz = zBase + dz;
                    for (int dx = 0; dx < 16; dx++) {
                        BlockPos pos = new BlockPos(xBase + dx, py, pz);
                        // 蜂箱（beehive）不是 bee_nest，自然被过滤
                        if (!level.getBlockState(pos).is(Blocks.BEE_NEST)) continue;
                        if (SafariZoneUtil.zoneOf(pos) != SafariZoneUtil.SafariZone.FOREST) continue;
                        if (!interactedNests.contains(pos)) {
                            availableNests.add(pos.immutable());
                        }
                    }
                }
            }
        }
    }

    /** 区块入队：只扫森林分区相关的 chunk（块级还有 zoneOf 精确过滤） */
    private static void enqueueChunk(ChunkPos pos) {
        enqueueChunk(pos.x(), pos.z());
    }

    private static void enqueueChunk(int cx, int cz) {
        long key = ChunkPos.pack(cx, cz);
        if (scannedChunks.contains(key) || pendingChunks.contains(key)) return;
        if (!isForestChunk(cx, cz)) return;
        pendingChunks.add(key);
    }

    /** 进入 Safari 时把周围已加载 chunk 全部入队（走过的 chunk 不会重新触发 CHUNK_LOAD） */
    private static void enqueueSurroundingChunks() {
        var player = CLIENT.player;
        var level = CLIENT.level;
        if (player == null || level == null) return;
        int chunkX = player.blockPosition().getX() >> 4, chunkZ = player.blockPosition().getZ() >> 4;
        for (int dcx = -SCAN_CHUNK_RADIUS; dcx <= SCAN_CHUNK_RADIUS; dcx++) {
            for (int dcz = -SCAN_CHUNK_RADIUS; dcz <= SCAN_CHUNK_RADIUS; dcz++) {
                int cx = chunkX + dcx, cz = chunkZ + dcz;
                if (!level.isLoaded(new BlockPos(cx << 4, player.blockPosition().getY(), cz << 4))) continue;
                enqueueChunk(cx, cz);
            }
        }
    }

    /**
     * chunk 级森林粗判（块级还有 zoneOf 精确过滤）：
     * 森林分区 = x ≥ -50 且 z > 10（见 {@link SafariZoneUtil}），
     * chunk 与森林相交 ⟺ 其 x/z 最大值满足条件。
     */
    private static boolean isForestChunk(int cx, int cz) {
        return (cx << 4) + 15 >= -50 && (cz << 4) + 15 > 10;
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
        // 实心盒子比蜂巢方块外扩一点,贴合表面但又不重叠
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
        scannedChunks.clear();
        pendingChunks.clear();
    }
}
