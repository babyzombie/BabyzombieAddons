package top.babyzombie.addons.module.misc;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import top.babyzombie.addons.module.misc.abiphone.AbiphoneTracker;
import top.babyzombie.addons.module.misc.abiphone.CustomRingtoneModule;
import top.babyzombie.addons.module.misc.abiphone.IncomingCallHandler;
import top.babyzombie.addons.module.misc.autois.AutoISModule;
import top.babyzombie.addons.module.misc.autois.KickRecoveryModule;
import top.babyzombie.addons.module.misc.bazaar.BazaarSellFromSacks;
import top.babyzombie.addons.module.misc.bazaar.BazzarTopOrdersOverlay;
import top.babyzombie.addons.module.misc.loadout.LoadoutModule;
import top.babyzombie.addons.module.misc.pet.PetDisplayHud;
import top.babyzombie.addons.module.misc.raredrop.RareDropModule;
import top.babyzombie.addons.util.win32.WinToast;

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
        MinimizeToTrayModule.init();
        SystemNotifier.init();
        WinToast.init();
        // dev 环境 classpath 可能缺 mod 资源,注入 MC 资源管理器兜底读取图标
        WinToast.setModIconProvider(() -> {
            try {
                return Minecraft.getInstance().getResourceManager()
                    .open(Identifier.fromNamespaceAndPath("babyzombieaddons", "icon.png"));
            } catch (java.io.IOException e) {
                return null;
            }
        });
        PetDisplayHud.init();
        LoadoutModule.init();

        AbiphoneTracker.getInstance().init();
        IncomingCallHandler.init();
        CustomRingtoneModule.init();
    }
}
