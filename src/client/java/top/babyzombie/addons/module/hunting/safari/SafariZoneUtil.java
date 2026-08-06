package top.babyzombie.addons.module.hunting.safari;

import net.minecraft.core.BlockPos;

/**
 * Safari 四个生物群系分区的坐标判定。
 * 用 wiki 的 NPC 候选点 + 铃铛地标拟合的边界（来自 HunterTradeTracker 实测）：
 * 雪地（x ≤ -52 且 z ≤ -2）→ Icy；
 * z > 10 为南侧（x < -50 → Cavern，否则 Forest）；
 * 其余（z ≤ 10 的北侧）→ x < -50 的过渡带归 Cavern，否则 Haunted。
 */
public final class SafariZoneUtil {

    /** Safari 四个生物群系分区 */
    public enum SafariZone { CAVERN, FOREST, HAUNTED, ICY }

    private SafariZoneUtil() {}

    /** 按坐标判定实体/玩家所在分区 */
    public static SafariZone zoneOf(BlockPos pos) {
        int x = pos.getX(), z = pos.getZ();
        // Icy（雪地）：与 WumpaRecord.isInSnowArea 的边界一致
        if (x <= -52 && z <= -2) return SafariZone.ICY;
        if (z > 10) return x < -50 ? SafariZone.CAVERN : SafariZone.FOREST;
        return x < -50 ? SafariZone.CAVERN : SafariZone.HAUNTED;
    }
}
