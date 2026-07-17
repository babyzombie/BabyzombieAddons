package top.babyzombie.addons.module.fishing;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLevelEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.phys.AABB;
import top.babyzombie.addons.config.ModConfigManager;
import top.babyzombie.addons.util.ChatUtils;
import top.babyzombie.addons.util.render.BeamRenderer;
import top.babyzombie.addons.util.render.RenderPhaseRegister;
import top.babyzombie.addons.util.tracker.HypixelLocationTracker;

import java.util.HashSet;
import java.util.Set;

/// 标记周围的稀有海怪：扫描玩家周围 24 格内的盔甲架，
/// 过滤出名字含  或  且有  的，插橙色信标光柱。
public final class RareSeaCreaturesAlert {

    /// 稀有海怪标记字符
    private static final String AQUATIC = "\uE072";
    private static final String MAGMATIC = "\uE07D";
    private static final String ELUSIVE = "\uE077";

    /// 已弹出过 Title 的实体 ID 集合，每个实体只弹一次
    private static final Set<Integer> alertedEntityIds = new HashSet<>();

    private RareSeaCreaturesAlert() {}

    public static void init() {
        ClientLevelEvents.AFTER_CLIENT_LEVEL_CHANGE.register((level, world) -> alertedEntityIds.clear());

        RenderPhaseRegister.register(ctx -> {
            var cfg = ModConfigManager.get().fishing;
            var rareCfg = cfg.rareSeaCreatures;
            if (!rareCfg.alert) return;
            if (!HypixelLocationTracker.getInstance().isInSkyblock()) return;

            var player = Minecraft.getInstance().player;
            if (player == null) return;

            var level = player.level();
            if (level == null) return;

            double scanRange = Math.min(25.0, Math.max(1.0, rareCfg.scanRange));
            var stands = level.getEntitiesOfClass(
                    ArmorStand.class,
                    new AABB(player.blockPosition()).inflate(scanRange, 128, scanRange),
                    e -> !e.isDeadOrDying()
            );

            int count = 0;
            String firstCleanedName = null;
            Integer titleEntityId = null;
            for (var stand : stands) {
                String rawName = stand.getName().getString();
                if (!((rawName.contains(AQUATIC) || rawName.contains(MAGMATIC)) && rawName.contains(ELUSIVE))) continue;

                var def = RareSeaCreatureDefinitions.match(rawName);
                if (def != null && rareCfg.excludeHighlightEnabled && def.isExcluded(rareCfg)) continue;

                int beamColor = def != null ? def.rarity.beamColorArgb : RareSeaCreatureDefinitions.UNKNOWN_BEAM_COLOR;
                BeamRenderer.drawBeam(ctx, stand.getX(), stand.getY() - 5, stand.getZ(),
                        2048, 0.15f, beamColor);

                if (firstCleanedName == null && !alertedEntityIds.contains(stand.getId())) {
                    String cleaned = RareSeaCreatureDefinitions.cleanNameForMatch(rawName);
                    if (!cleaned.isEmpty()) {
                        String titleColor = def != null ? def.rarity.titleColorCode : RareSeaCreatureDefinitions.UNKNOWN_TITLE_COLOR_CODE;
                        firstCleanedName = titleColor + cleaned;
                        titleEntityId = stand.getId();
                    }
                }
                count++;
            }

            // Title 提示（每个实体只弹一次）
            if (rareCfg.alertTitle && count > 0 && firstCleanedName != null && titleEntityId != null) {
                alertedEntityIds.add(titleEntityId);
                ChatUtils.showTitle(
                        Component.translatable("fishing.rareSeaCreaturesAlert.title").getString(),
                        firstCleanedName,
                        0, 50, 10);
            }
        });
    }
}
