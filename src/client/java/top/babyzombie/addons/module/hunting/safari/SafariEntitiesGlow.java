package top.babyzombie.addons.module.hunting.safari;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.ambient.Bat;
import net.minecraft.world.entity.monster.Shulker;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.entity.player.Player;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.entity.CalibratedSculkSensorBlockEntity;
import top.babyzombie.addons.config.ModConfigManager;
import top.babyzombie.addons.util.ServerTick;
import net.minecraft.client.Minecraft;
import top.babyzombie.addons.util.render.GlowController;
import top.babyzombie.addons.util.render.RenderPhaseRegister;
import top.babyzombie.addons.util.render.WorldRenderUtils;
import top.babyzombie.addons.util.tracker.HypixelLocationTracker;

import java.util.HashSet;
import java.util.Set;

/**
 * 在 Safari 区域高亮：
 * - 潜影贝（自定义颜色）
 * - Hideyho NPC（淡蓝色）
 * - Warden（冷却红色/可捕捉绿色，开深度测试）
 * - 较频幽匿感测体（紫色方块发光）
 * - 蝙蝠（自定义颜色，深度测试）
 * - Duplico 物品展示实体（书架/樱花木/深板岩圆石等，自定义颜色，深度测试）
 */
public final class SafariEntitiesGlow {

    private static final String HIDEYHO_NAME = "Hideyho ";
    private static final int HIDEYHO_COLOR = 0xFF80D8FF;
    private static final int SCULK_SENSOR_RANGE = 32;
    private static final int SCULK_SENSOR_RANGE_SQ = SCULK_SENSOR_RANGE * SCULK_SENSOR_RANGE;

    /** Duplico 物品展示实体可能展示的物品 id（path 部分） */
    private static final Set<String> DUPLICO_ITEMS = Set.of(
        "bookshelf",
        "cherry_wood",
        "cobbled_deepslate"
    );

    // Warden 战斗场地范围
    private static final int ARENA_X_MIN = -18, ARENA_X_MAX = 24;
    private static final int ARENA_Y_MIN = 45, ARENA_Y_MAX = 62;
    private static final int ARENA_Z_MIN = -39, ARENA_Z_MAX = -13;

    /** 上一次 tick 高亮的较频幽匿感测体位置 */
    private static final Set<BlockPos> sculkSensorHighlighted = new HashSet<>();

    private SafariEntitiesGlow() {}

    public static void init() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null || client.level == null) return;
            if (!HypixelLocationTracker.getInstance().isInSafari()) return;

            var cfg = ModConfigManager.get().hunting;
            boolean glowShulker = cfg.safari.shulkerGlow;
            boolean glowHideyho = cfg.safari.hideyhoGlow;
            boolean glowWarden = cfg.safari.wardenGlow;
            boolean glowSculkSensor = cfg.safari.sculkSensorGlow;
            boolean glowBat = cfg.safari.batGlow;
            boolean glowDuplico = cfg.safari.duplicoGlow;

            // === 实体发光 ===
            if (glowShulker || glowHideyho || glowBat || glowDuplico) {
                for (var entity : client.level.entitiesForRendering()) {
                    if (glowShulker) {
                        int argb = cfg.safari.shulkerGlowColor.getEffectiveColourRGB();
                        if (entity instanceof Shulker shulker) {
                            GlowController.setGlow(shulker, true, argb, true);
                        } else if (entity instanceof Display.ItemDisplay itemDisplay
                                && BuiltInRegistries.ITEM.getKey(itemDisplay.getItemStack().getItem())
                                    .getPath().contains("shulker_box")) {
                            GlowController.setGlow(itemDisplay, true, argb, true);
                        }
                    }
                    if (glowHideyho && entity instanceof Player player
                            && HIDEYHO_NAME.equals(player.getName().getString())) {
                        GlowController.setGlow(player, true, HIDEYHO_COLOR, true);
                    }
                    if (glowBat && entity instanceof Bat bat) {
                        GlowController.setGlow(bat, true, cfg.safari.batGlowColor.getEffectiveColourRGB(), true);
                    }
                    if (glowDuplico && entity instanceof Display.ItemDisplay itemDisplay) {
                        var stack = itemDisplay.getItemStack();
                        if (!stack.isEmpty() && DUPLICO_ITEMS.contains(
                                BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath())) {
                            GlowController.setGlow(itemDisplay, true,
                                cfg.safari.duplicoGlowColor.getEffectiveColourRGB(), true);
                        }
                    }
                }
            }

            // === Warden 高亮（战斗场地内，深度测试，冷却红/可捕捉绿） ===
            if (glowWarden && isInArena(client.player.blockPosition())) {
                int cooldownColor = cfg.safari.wardenGlowCooldownColor.getEffectiveColourRGB();
                int readyColor = cfg.safari.wardenGlowReadyColor.getEffectiveColourRGB();
                int cooldownTicks = cfg.safari.wardenCooldownTicks;
                for (var entity : client.level.entitiesForRendering()) {
                    if (entity instanceof Warden warden && isInArena(warden.blockPosition())) {
                        int color;
                        var pose = warden.getPose();
                        if (pose == net.minecraft.world.entity.Pose.EMERGING
                                || pose == net.minecraft.world.entity.Pose.DIGGING) {
                            // 登场动画 / 钻地 → 一定无敌
                            color = cooldownColor;
                        } else {
                            int ping = ServerTick.getPing();
                            int delay = ping > 0 ? (int) Math.ceil(ping / 50.0) : 0;
                            int compensated = warden.tickCount + delay;
                            color = compensated < cooldownTicks ? cooldownColor : readyColor;
                        }
                        GlowController.setGlow(warden, true, color, true);
                    }
                }
            }

            // === 较频幽匿感测体方块高亮（维护位置集合，渲染由下面的回调每帧批量绘制） ===
            if (glowSculkSensor && isInArena(client.player.blockPosition())) {
                var level = client.level;
                var playerPos = client.player.blockPosition();
                int chunkX = playerPos.getX() >> 4;
                int chunkZ = playerPos.getZ() >> 4;
                int chunkR = (SCULK_SENSOR_RANGE >> 4) + 1;

                Set<BlockPos> found = new HashSet<>();

                for (int dcx = -chunkR; dcx <= chunkR; dcx++) {
                    for (int dcz = -chunkR; dcz <= chunkR; dcz++) {
                        int cx = chunkX + dcx;
                        int cz = chunkZ + dcz;
                        BlockPos chunkOrigin = new BlockPos(cx << 4, playerPos.getY(), cz << 4);

                        if (!level.isLoaded(chunkOrigin)) continue;

                        var chunk = level.getChunkAt(chunkOrigin);
                        for (var entry : chunk.getBlockEntities().entrySet()) {
                            if (entry.getValue() instanceof CalibratedSculkSensorBlockEntity) {
                                BlockPos pos = entry.getKey();
                                if (pos.distSqr(playerPos) <= SCULK_SENSOR_RANGE_SQ) {
                                    found.add(pos.immutable());
                                }
                            }
                        }
                    }
                }

                // Diff：更新集合
                sculkSensorHighlighted.retainAll(found);
                sculkSensorHighlighted.addAll(found);
            } else if (!sculkSensorHighlighted.isEmpty()) {
                sculkSensorHighlighted.clear();
            }
        });

        // 每帧批量绘制感测体高亮（半透明填充盒子，同 hitresult 风格，深度测试不穿墙）
        RenderPhaseRegister.register(ctx -> {
            if (sculkSensorHighlighted.isEmpty()) return;
            if (!ModConfigManager.get().hunting.safari.sculkSensorGlow) return;
            if (Minecraft.getInstance().player == null) return;
            if (!HypixelLocationTracker.getInstance().isInSafari()) return;

            int color = ModConfigManager.get().hunting.safari.sculkSensorGlowColor.getEffectiveColourRGB();
            float r = ((color >> 16) & 0xFF) / 255f;
            float g = ((color >> 8) & 0xFF) / 255f;
            float b = (color & 0xFF) / 255f;

            // 取方块实际 shape 的 hitboxes（同 hitresult），微外扩避免 Z-fighting
            var level = Minecraft.getInstance().level;
            if (level == null) return;
            var boxes = new java.util.ArrayList<double[]>();
            for (var pos : sculkSensorHighlighted) {
                var state = level.getBlockState(pos);
                if (state.isAir()) continue;
                var shape = state.getShape(level, pos);
                if (shape.isEmpty()) continue;
                for (var box : shape.toAabbs()) {
                    boxes.add(new double[]{
                        box.minX + pos.getX() - 0.01, box.minY + pos.getY() - 0.01, box.minZ + pos.getZ() - 0.01,
                        box.maxX + pos.getX() + 0.01, box.maxY + pos.getY() + 0.01, box.maxZ + pos.getZ() + 0.01});
                }
            }
            if (boxes.isEmpty()) return;
            WorldRenderUtils.drawFilledBoxes(ctx, boxes, r, g, b, 0.3f, true);
        });
    }

    private static boolean isInArena(BlockPos pos) {
        return pos.getX() >= ARENA_X_MIN && pos.getX() <= ARENA_X_MAX
            && pos.getY() >= ARENA_Y_MIN && pos.getY() <= ARENA_Y_MAX
            && pos.getZ() >= ARENA_Z_MIN && pos.getZ() <= ARENA_Z_MAX;
    }
}
