package top.babyzombie.addons.module.garden;

public final class GardenModule {
    private GardenModule() {}

    public static void init() {
        PestDisplay.init();
        SignAutoRotate.init();
        XpOrbSoundReducer.init();
        TrevorAutoAccept.init();
        FarmingToolSwingSuppression.init();
    }
}
