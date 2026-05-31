package top.babyzombie.addons.module.misc;

public final class MiscModule {
    private MiscModule() {}

    public static void init() {
        KillComboHUD.init();
        VanquisherAlert.init();
        CakeBuffTracker.init();
    }
}
