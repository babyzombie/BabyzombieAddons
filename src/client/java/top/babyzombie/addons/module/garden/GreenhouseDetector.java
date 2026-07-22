package top.babyzombie.addons.module.garden;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLevelEvents;
import top.babyzombie.addons.util.PlayerUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * 花园温室检测工具。
 *
 * <p>通过扫描地皮范围内的 Player 实体皮肤判断该地皮是否为温室。
 * 温室 NPC 的皮肤材质 URL 以特定 hash 结尾。
 */
public final class GreenhouseDetector {

    /// 温室 NPC 皮肤材质 hash
    private static final String GREENHOUSE_NPC_SKIN_HASH =
            "cc8d2a5af975c53cf081b531566148bae366141314b35aa5726e97cc97ef118b";

    /// 每个地皮的温室检测结果永久缓存（避免 NPC 未加载时误判为普通地皮）
    private static final Map<Integer, Boolean> greenhouseCache = new HashMap<>();

    private GreenhouseDetector() {}

    static {
        ClientLevelEvents.AFTER_CLIENT_LEVEL_CHANGE.register((client, level) -> greenhouseCache.clear());
    }

    /**
     * 检测指定地皮是否为温室。
     *
     * @param plot 目标地皮
     * @return 该地皮是否包含温室 NPC
     */
    public static boolean isGreenhouse(Plot plot) {
        if (plot == null) return false;

        Boolean cached = greenhouseCache.get(plot.id());
        if (cached != null) return cached;

        var level = Minecraft.getInstance().level;
        if (level == null) return false;

        boolean found = false;
        for (var entity : level.getEntities((Entity) null, plot.aabb(),
                e -> e instanceof Player && e != Minecraft.getInstance().player)) {
            var profile = PlayerUtils.getPlayerProfile(entity);
            String skinUrl = PlayerUtils.getSkinTextureUrl(profile);
            if (skinUrl != null && skinUrl.endsWith(GREENHOUSE_NPC_SKIN_HASH)) {
                found = true;
                break;
            }
        }

        greenhouseCache.put(plot.id(), found);
        return found;
    }

    /**
     * 检测当前玩家所在地皮是否为温室。
     */
    public static boolean isCurrentPlotGreenhouse() {
        return isGreenhouse(PlotUtils.getCurrentPlot());
    }
}
