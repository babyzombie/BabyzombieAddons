package top.babyzombie.addons.module.fishing;

public class FishingModule {
    private FishingModule() {}

    public static void init() {
        PreventInstantReel.init();
        RareSeaCreaturesAlert.init();
        RareSeaCreaturesSelfAlert.init();
        FishingCameraModule.init();
        MuteVanquisher.init();
        HideThunderSpark.init();
    }
}
