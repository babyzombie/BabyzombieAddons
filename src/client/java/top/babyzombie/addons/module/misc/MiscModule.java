package top.babyzombie.addons.module.misc;

import top.babyzombie.addons.module.misc.abiphone.AbiphoneTracker;
import top.babyzombie.addons.module.misc.abiphone.CustomRingtoneModule;
import top.babyzombie.addons.module.misc.abiphone.IncomingCallHandler;
import top.babyzombie.addons.module.misc.autois.AutoISModule;
import top.babyzombie.addons.module.misc.autois.KickRecoveryModule;
import top.babyzombie.addons.module.misc.bazaar.BazzarTopOrdersOverlay;
import top.babyzombie.addons.module.misc.loadout.LoadoutModule;
import top.babyzombie.addons.module.misc.pet.PetDisplayHud;
import top.babyzombie.addons.module.misc.raredrop.RareDropModule;

public final class MiscModule {
    private MiscModule() {}

    public static void init() {
        AutoJoinModule.init();
        AutoISModule.init();
        KickRecoveryModule.init();
        AutoReconnectHelper.init();
        BazaarSellFromSacks.init();
        BazzarTopOrdersOverlay.init();
        CakeBuffTracker.init();
        MinionCollectAutoClose.init();
        NecronBladeModule.init();
        SecondPersonKey.init();
        SkipSecondPerson.init();
        CopyItemInfoKey.init();
        FallLandingSoundMute.init();
        EntityHider.init();
        RareDropModule.init();
        WindowTitleModule.init();
        PetDisplayHud.init();
        LoadoutModule.init();

        AbiphoneTracker.getInstance().init();
        IncomingCallHandler.init();
        CustomRingtoneModule.init();
    }
}
