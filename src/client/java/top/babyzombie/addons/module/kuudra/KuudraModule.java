package top.babyzombie.addons.module.kuudra;

public final class KuudraModule {
    private KuudraModule() {}

    /**
     * Kuudra HUD 倒计时公共渲染工具：紧迫度颜色 + 秒数格式。
     * 补给刷出倒计时（KuudraSupplyProgressHUD）与建造开始倒计时（FreshSystem）
     * 共用同一套阈值/格式，避免两份复制逻辑漂移。
     */

    /** 剩余比例 → 紧迫色：>75% 绿 → >50% 黄 → >25% 金 → 红 */
    public static String colorForRatio(double ratio) {
        if (ratio > 0.75) return "§a";
        if (ratio > 0.50) return "§e";
        if (ratio > 0.25) return "§6";
        return "§c";
    }

    /** 秒 → "%.2fs"（两位小数 + s 单位） */
    public static String formatSeconds(double seconds) {
        return String.format("%.2fs", seconds);
    }

    /** 组合：紧迫色 + 秒文本，可直接作语言 key 的 %s 参数 */
    public static String coloredTime(double ratio, double seconds) {
        return colorForRatio(ratio) + formatSeconds(seconds);
    }

    public static void init() {
        ArrowPoisonRefill.init();
        KuudraLocationTracker.init();
        KuudraScreenProtector.init();
        KuudraHPDisplay.init();
        KuudraPhaseTimer.init();
        KuudraBoxRenderer.init();
        KuudraEnergyDisplay.init();
        KuudraStunTimer.init();
        KuudraWaypoints.init();
        EnderPearlTrajectory.init();
        EnderPearlCamera.init();
        KuudraPerkShopBlacklist.init();
        EnderPearlRefill.init();
        KuudraFollowerHelmetPrice.init();
        CrimsonArmorPistonMute.init();
        KuudraNopeMagmafish.init();
        KuudraSupplyTimer.init();
        KuudraSupplyProgressHUD.init();
        KuudraPileWaypoints.init();
        PearlWaypoints.init();
        FreshSystem.init();
        NoPreAlert.init();
        AlreadyPickingAlert.init();
        ElleHighlight.init();
        TeamHighlight.init();
        KuudraDirectionHUD.init();
        KuudraP4Features.init();
        ChestCounter.init();
        KuudraEtherwarpLavaPrevent.init();
        KuudraMinimap.init();
    }
}
