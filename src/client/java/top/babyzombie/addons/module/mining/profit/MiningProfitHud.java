package top.babyzombie.addons.module.mining.profit;

import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import top.babyzombie.addons.config.ModConfigManager;
import top.babyzombie.addons.config.hud.HudManager;
import top.babyzombie.addons.util.ChatUtils;
import top.babyzombie.addons.util.tracker.HypixelLocationTracker;

import java.util.ArrayList;
import java.util.List;

public final class MiningProfitHud {
    public static final String ELEMENT_NAME = "MiningProfit";

    private MiningProfitHud() {}

    public static void init() {
        HudElementRegistry.attachElementAfter(VanillaHudElements.OVERLAY_MESSAGE,
                Identifier.fromNamespaceAndPath("babyzombieaddons", "mining_profit"),
                (context, tickCounter) -> {
                    if (!ModConfigManager.get().mining.profitTracker) return;
                    if (!HypixelLocationTracker.getInstance().isInSkyblock()) return;

                    ProfitSnapshot snapshot = MiningProfitModule.getSnapshot();
                    if (!snapshot.hasBlockStats() && !snapshot.hasGemStats()) return;

                    List<String> lines = buildLines(snapshot);
                    if (lines.isEmpty()) return;

                    var font = Minecraft.getInstance().font;
                    HudManager.drawScaled(
                            context,
                            font,
                            String.join("\n", lines),
                            HudManager.x(ELEMENT_NAME),
                            HudManager.y(ELEMENT_NAME),
                            HudManager.scale(ELEMENT_NAME)
                    );
                });
    }

    private static List<String> buildLines(ProfitSnapshot snapshot) {
        List<String> lines = new ArrayList<>();
        lines.add(ChatUtils.translate("babyzombieaddons.profit.hud.title"));

        if (snapshot.hasGemStats()) {
            GemProfitStats gem = snapshot.gemStats();
            lines.add(ChatUtils.translate("babyzombieaddons.profit.hud.gem_rate",
                    ProfitFormatters.formatCoins(gem.coinsPerHour())));
            lines.add(ChatUtils.translate("babyzombieaddons.profit.hud.gem_flawless",
                    String.format("%.1f", gem.flawlessPerHour())));
            lines.add(ChatUtils.translate("babyzombieaddons.profit.hud.time",
                    ProfitFormatters.formatTime(gem.sessionTimeMs())));
        }

        if (snapshot.hasBlockStats()) {
            double totalValue = snapshot.materials().stream().mapToDouble(MaterialProfitStats::totalValue).sum();
            double totalRate = snapshot.materials().stream().mapToDouble(MaterialProfitStats::coinsPerHour).sum();
            long longestSession = snapshot.materials().stream().mapToLong(MaterialProfitStats::sessionTimeMs).max().orElse(0L);

            lines.add(ChatUtils.translate("babyzombieaddons.profit.hud.block_rate",
                    ProfitFormatters.formatCoins(totalRate)));
            lines.add(ChatUtils.translate("babyzombieaddons.profit.hud.block_total",
                    ProfitFormatters.formatCoins(totalValue)));

            int materialCount = Math.min(2, snapshot.materials().size());
            for (int i = 0; i < materialCount; i++) {
                MaterialProfitStats material = snapshot.materials().get(i);
                lines.add(ChatUtils.translate("babyzombieaddons.profit.hud.material_line",
                        material.displayName(),
                        ProfitFormatters.formatCoins(material.totalValue())));
            }

            if (!snapshot.hasGemStats()) {
                lines.add(ChatUtils.translate("babyzombieaddons.profit.hud.time",
                        ProfitFormatters.formatTime(longestSession)));
            }
        }

        String sourceKey = snapshot.usingNpcPrices()
                ? "babyzombieaddons.profit.price_source.npc"
                : "babyzombieaddons.profit.price_source.bazaar";
        lines.add(ChatUtils.translate("babyzombieaddons.profit.hud.price_source",
                ChatUtils.translate(sourceKey)));
        return lines;
    }
}
