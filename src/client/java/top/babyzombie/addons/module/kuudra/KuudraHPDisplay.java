package top.babyzombie.addons.module.kuudra;

import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.BossHealthOverlay;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.scores.DisplaySlot;
import top.babyzombie.addons.config.ModConfig;
import top.babyzombie.addons.config.ModConfigManager;
import top.babyzombie.addons.config.hud.HudManager;
import top.babyzombie.addons.mixin.BossHealthOverlayAccessor;
import top.babyzombie.addons.util.ChatUtils;
import top.babyzombie.addons.util.HypixelLocationTracker;

public final class KuudraHPDisplay {
    private KuudraHPDisplay() {}

    private static final String DELIMITER = " §8❯§r ";

    public static void init() {
        HudElementRegistry.attachElementAfter(VanillaHudElements.OVERLAY_MESSAGE,
                Identifier.fromNamespaceAndPath("babyzombieaddons", "kuudra_hp"),
                (context, tickCounter) -> {
            if (ModConfigManager.get().kuudra.hpDisplay != ModConfig.HpDisplayMode.HUD) return;
            var t = KuudraLocationTracker.kuudraEntity;
            float h = KuudraLocationTracker.hp;
            if (t == null || t.isDeadOrDying() || h < 2) return;

            var font = Minecraft.getInstance().font;
            int x = HudManager.x("KuudraHP"), y = HudManager.y("KuudraHP");
            float s = HudManager.scale("KuudraHP");
            String text = formatHP(h);
            HudManager.drawScaled(context, font, text, x, y, s);
        });
    }

    /** Called from BossHealthOverlayMixin on every bossbar render. */
    public static void onBossbarRender(BossHealthOverlay overlay) {
        if (ModConfigManager.get().kuudra.hpDisplay != ModConfig.HpDisplayMode.BOSSBAR) return;
        if (!HypixelLocationTracker.getInstance().isInKuudra()) return;

        float h = KuudraLocationTracker.hp;
        if (h < 2) return;

        var events = ((BossHealthOverlayAccessor) overlay).getEvents();
        for (var event : events.values()) {
            String name = ChatUtils.stripColor(event.getName().getString());
            if (!name.contains("Kuudra")) continue;

            String baseName = event.getName().getString();
            int idx = baseName.indexOf(DELIMITER);
            if (idx > 0) baseName = baseName.substring(0, idx);

            String hpStr = buildHpStr(h);
            String submerge = getSubmerge();
            event.setName(Component.literal(baseName + DELIMITER + hpStr + submerge));
            break;
        }
    }

    private static String buildHpStr(float h) {
        var loc = KuudraLocationTracker.area;
        boolean isT5 = HypixelLocationTracker.getInstance().getLocation() != null
                && HypixelLocationTracker.getInstance().getLocation().contains("T5");
        if (isT5 && "p4".equals(loc) && h < 25000) {
            return String.format("§4§l%.1fM§c❤", h / 2500f * 24f);
        }
        String hpNum = numFormat((int) h);
        String color = h > 60000 ? "§a" : (h > 40000 ? "§e" : "§4");
        return color + "§l" + hpNum + "§c❤";
    }

    private static String getSubmerge() {
        var client = Minecraft.getInstance();
        if (client.player == null) return "";
        var obj = client.player.level().getScoreboard().getDisplayObjective(DisplaySlot.BY_ID.apply(1));
        if (obj == null) return "";
        for (var holder : client.player.level().getScoreboard().getTrackedPlayers()) {
            if (!client.player.level().getScoreboard().listPlayerScores(holder).containsKey(obj)) continue;
            var team = client.player.level().getScoreboard().getPlayersTeam(holder.getScoreboardName());
            if (team == null) continue;
            String line = ChatUtils.stripColor(ChatUtils.removeEmoji(
                    team.getPlayerPrefix().getString() + team.getPlayerSuffix().getString()));
            if (line.startsWith("Submerges :")) {
                return "   §r" + line.replace(" :", ":");
            }
        }
        return "";
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

    private static String numFormat(int num) {
        return String.format("%,d", num);
    }
}
