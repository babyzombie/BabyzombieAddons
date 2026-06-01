package top.babyzombie.addons.module.kuudra;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.scores.DisplaySlot;
import top.babyzombie.addons.config.HudManager;
import top.babyzombie.addons.config.ModConfig;
import top.babyzombie.addons.config.ModConfigManager;
import top.babyzombie.addons.util.ChatUtils;
import top.babyzombie.addons.util.HypixelLocationTracker;

public final class KuudraHPDisplay {
    private KuudraHPDisplay() {}

    public static void init() {
        HudRenderCallback.EVENT.register((gui, delta) -> {
            if (ModConfigManager.get().kuudra.hpDisplay != ModConfig.HpDisplayMode.HUD) return;
            var t = KuudraLocationTracker.kuudraEntity;
            float h = KuudraLocationTracker.hp;
            if (t == null || t.isDeadOrDying() || h < 2) return;

            var font = Minecraft.getInstance().font;
            int x = HudManager.x("KuudraHP"), y = HudManager.y("KuudraHP");
            String text = formatHP(h);
            gui.drawString(font, text, x, y, 0xFFFFFFFF, true);
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (ModConfigManager.get().kuudra.hpDisplay != ModConfig.HpDisplayMode.BOSSBAR) return;
            if (client.player == null) return;
            updateBossBar(client);
        });
    }

    private static String formatHP(float h) {
        var loc = KuudraLocationTracker.area;
        boolean isT5 = HypixelLocationTracker.getInstance().getLocation() != null
                && HypixelLocationTracker.getInstance().getLocation().contains("T5");
        if (isT5 && "p4".equals(loc) && h < 25000) {
            int displayHP = (int) (h / 25000f * 240f);
            return String.format("§4§l%dM§c/240M", displayHP);
        }
        return String.format("§4§l%s§c/100,000", numFormat((int) h));
    }

    private static void updateBossBar(Minecraft client) {
        float h = KuudraLocationTracker.hp;
        if (h < 2) return;

        var withers = client.player.level().getEntitiesOfClass(WitherBoss.class,
                new AABB(client.player.blockPosition()).inflate(128),
                e -> ChatUtils.stripColor(e.getName().getString()).contains("\uDCC0 Kuudra \uDCFF"));
        if (withers.isEmpty()) return;
        var wither = withers.get(0);

        var loc = KuudraLocationTracker.area;
        boolean isT5 = HypixelLocationTracker.getInstance().getLocation() != null
                && HypixelLocationTracker.getInstance().getLocation().contains("T5");
        String hpStr;

        if (isT5 && "p4".equals(loc) && h < 25000) {
            hpStr = String.format("§4§l%.1fM§c❤", h / 2500f * 24f);
        } else {
            String hpNum = numFormat((int) h);
            String color = h > 60000 ? "§a" : (h > 40000 ? "§e" : "§4");
            hpStr = color + "§l" + hpNum + "§c❤";
        }

        String submerge = "";
        var obj = client.player.level().getScoreboard().getDisplayObjective(DisplaySlot.BY_ID.apply(1));
        if (obj != null) {
            for (var holder : client.player.level().getScoreboard().getTrackedPlayers()) {
                if (!client.player.level().getScoreboard().listPlayerScores(holder).containsKey(obj)) continue;
                var team = client.player.level().getScoreboard().getPlayersTeam(holder.getScoreboardName());
                if (team == null) continue;
                String line = ChatUtils.stripColor(ChatUtils.removeEmoji(
                        team.getPlayerPrefix().getString() + team.getPlayerSuffix().getString()));
                if (line.startsWith("Submerges :")) {
                    submerge = "   §r" + line.replace(" :", ":");
                    break;
                }
            }
        }

        wither.setCustomName(Component.literal("§6\uDCC0 §c§lKuudra§6 \uDCFF " + hpStr + submerge));
    }

    private static String numFormat(int num) {
        if (num < 1000) return String.valueOf(num);
        String s = String.valueOf(num);
        return s.substring(0, s.length() - 3) + "," + s.substring(s.length() - 3);
    }
}
