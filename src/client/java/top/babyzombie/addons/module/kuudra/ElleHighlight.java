package top.babyzombie.addons.module.kuudra;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.scores.DisplaySlot;
import top.babyzombie.addons.config.ModConfigManager;
import top.babyzombie.addons.util.ChatUtils;
import top.babyzombie.addons.util.PlayerUtils;
import top.babyzombie.addons.util.render.GlowController;
import top.babyzombie.addons.util.tracker.HypixelLocationTracker;

/**
 * Elle 高亮 — P2 阶段给 Elle 发光（关闭深度测试），通过皮肤 hash 识别。
 */
public final class ElleHighlight {
    private ElleHighlight() {}

    private static final String ELLE_SKIN_HASH = "2333aa2414bcf1c291fddf6a9b0f805f996546ec4150ff3cef10bd529cc98261";
    private static Entity elleEntity;

    public static void init() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (!HypixelLocationTracker.getInstance().isInKuudra()) return;
            var cfg = ModConfigManager.get().kuudra.phase2;
            if (!cfg.elleHighlight) return;
            if (client.player == null) return;

            boolean inP2 = "Protect Elle".equals(getScoreboardPhase());

            if (inP2) {
                if (elleEntity == null || elleEntity.isRemoved()) {
                    ClientLevel level = client.level;
                    if (level == null) return;
                    for (var entity : level.entitiesForRendering()) {
                        String url = PlayerUtils.getSkinTextureUrl(PlayerUtils.getPlayerProfile(entity));
                        if (url != null && url.contains(ELLE_SKIN_HASH)) {
                            elleEntity = entity;
                            int color = cfg.elleHighlightColor.getEffectiveColourRGB();
                            GlowController.setGlow(elleEntity, true, color, false);
                            return;
                        }
                    }
                }
            } else if (elleEntity != null) {
                GlowController.setGlow(elleEntity, false);
                elleEntity = null;
            }
        });
    }

    private static String getScoreboardPhase() {
        var player = Minecraft.getInstance().player;
        if (player == null) return "";
        var obj = player.level().getScoreboard().getDisplayObjective(DisplaySlot.BY_ID.apply(1));
        if (obj == null) return "";
        for (var holder : player.level().getScoreboard().getTrackedPlayers()) {
            if (!player.level().getScoreboard().listPlayerScores(holder).containsKey(obj)) continue;
            var team = player.level().getScoreboard().getPlayersTeam(holder.getScoreboardName());
            if (team == null) continue;
            String text = ChatUtils.stripColor(ChatUtils.removeEmoji(
                    team.getPlayerPrefix().getString() + team.getPlayerSuffix().getString()))
                    .replaceAll(" \\(.+\\)", "");
            if (text.equals("Protect Elle")) return text;
        }
        return "";
    }
}
