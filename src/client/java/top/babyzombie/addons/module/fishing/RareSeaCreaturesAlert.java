package top.babyzombie.addons.module.fishing;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.phys.AABB;
import top.babyzombie.addons.config.ModConfigManager;
import top.babyzombie.addons.util.ChatUtils;
import top.babyzombie.addons.util.render.BeamRenderer;
import top.babyzombie.addons.util.render.RenderPhaseRegister;
import top.babyzombie.addons.util.tracker.HypixelLocationTracker;

/// 标记周围的稀有海怪：扫描玩家周围 24 格内的盔甲架，
/// 过滤出名字含  或  且有  的，插橙色信标光柱。
public final class RareSeaCreaturesAlert {

    /// 扫描半径（格）
    private static final double SCAN_RANGE = 16.0;

    /// 橙色信标光柱颜色 (ARGB)
    private static final int BEAM_COLOR = 0xFFFF6600;

    /// Title 提示冷却（tick）= 5 秒
    private static final int TITLE_COOLDOWN_TICKS = 100;

    /// 稀有海怪标记字符
    private static final String AQUATIC = "\uE072";
    private static final String MAGMATIC = "\uE07D";
    private static final String ELUSIVE = "\uE077";

    /// Title 上次显示的 tick 时间
    private static long lastTitleTick = -TITLE_COOLDOWN_TICKS;

    private RareSeaCreaturesAlert() {}

    public static void init() {
        RenderPhaseRegister.register(ctx -> {
            var cfg = ModConfigManager.get().fishing;
            if (!cfg.rareSeaCreatures.alert) return;
            if (!HypixelLocationTracker.getInstance().isInSkyblock()) return;

            var player = Minecraft.getInstance().player;
            if (player == null) return;

            var level = player.level();
            if (level == null) return;

            var stands = level.getEntitiesOfClass(
                    ArmorStand.class,
                    new AABB(player.blockPosition()).inflate(SCAN_RANGE, 128, SCAN_RANGE),
                    e -> !e.isDeadOrDying()
            );

            int count = 0;
            String firstCleanedName = null;
            for (var stand : stands) {
                String name = stand.getName().getString();
                if ((name.contains(AQUATIC) || name.contains(MAGMATIC)) && name.contains(ELUSIVE)) {
                    BeamRenderer.drawBeam(ctx, stand.getX(), stand.getY() - 5, stand.getZ(),
                            2048, 0.15f, BEAM_COLOR);
                    if (firstCleanedName == null) {
                        firstCleanedName = cleanSeaCreatureName(name);
                    }
                    count++;
                }
            }

            // Title 提示（带冷却）
            if (cfg.rareSeaCreatures.alertTitle && count > 0 && firstCleanedName != null) {
                long tick = level.getGameTime();
                if (tick - lastTitleTick >= TITLE_COOLDOWN_TICKS) {
                    lastTitleTick = tick;
                    ChatUtils.showTitle(
                            Component.translatable("fishing.rareSeaCreatures.alert.title").getString(),
                            firstCleanedName,
                            0, 30, 10);
                }
            }
        });
    }

    /// 清理盔甲架名称：移除表情/私有区字符、等级标签、血量数字，
    /// 保留颜色符号。
    private static String cleanSeaCreatureName(String rawName) {
        // 1. 复用 ChatUtils 移除表情和私有区字符
        String s = ChatUtils.removeEmoji(rawName);
        // 2. 移除等级标签 [Lv100] / [LV100]
        s = s.replaceAll("\\[[Ll][Vv]\\s*\\d+\\]", "");
        // 3. 移除血量数字 (如 1,145,140, 10k, 1.2M)
        s = s.replaceAll("[\\d,./]+[kKmMbB]?", "").replace("❤", "");
        // 4. 合并多余空格并 trim
        s = s.replaceAll(" {2,}", " ").trim();
        return "§6" + s;
    }
}
