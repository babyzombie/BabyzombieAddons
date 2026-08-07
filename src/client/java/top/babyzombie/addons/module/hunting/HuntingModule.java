package top.babyzombie.addons.module.hunting;

import top.babyzombie.addons.module.hunting.safari.HunterTradeTracker;
import top.babyzombie.addons.module.hunting.safari.SafariBellDisplay;
import top.babyzombie.addons.module.hunting.safari.SafariEntitiesGlow;
import top.babyzombie.addons.module.hunting.safari.SafariTrajectory;
import top.babyzombie.addons.module.hunting.safari.WumpaRecord;
import top.babyzombie.addons.module.hunting.torrhuscanyon.TorrhusCanyonBeeheemoth;
import top.babyzombie.addons.module.hunting.torrhuscanyon.TorrhusCanyonTemple;

public final class HuntingModule {
    private HuntingModule() {}

    public static void init() {
        SafariBellDisplay.init();
        SafariEntitiesGlow.init();
        SafariTrajectory.init();
        WumpaRecord.init();
        HunterTradeTracker.init();
        TorrhusCanyonBeeheemoth.init();
        TorrhusCanyonTemple.init();
    }
}
