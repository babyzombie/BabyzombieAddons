package top.babyzombie.addons.module.mining.profit;

import java.util.List;

public record ProfitSnapshot(
        List<MaterialProfitStats> materials,
        GemProfitStats gemStats,
        boolean usingNpcPrices
) {
    public static final ProfitSnapshot EMPTY = new ProfitSnapshot(List.of(), null, false);

    public boolean hasBlockStats() {
        return !materials.isEmpty();
    }

    public boolean hasGemStats() {
        return gemStats != null;
    }
}
