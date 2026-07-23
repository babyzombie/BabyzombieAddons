package top.babyzombie.addons.module.hunting;

import top.babyzombie.addons.module.hunting.safari.SafariBellDisplay;
import top.babyzombie.addons.module.hunting.safari.SafariEntitiesGlow;
import top.babyzombie.addons.module.hunting.safari.WumpaRecord;
import top.babyzombie.addons.module.hunting.torrhuscanyon.TorrhusCanyonBeeheemoth;
import top.babyzombie.addons.module.hunting.torrhuscanyon.TorrhusCanyonTemple;

public final class HuntingModule {
    private HuntingModule() {}

    public static void init() {
        SafariBellDisplay.init();
        SafariEntitiesGlow.init();
        WumpaRecord.init();
        TorrhusCanyonBeeheemoth.init();
        TorrhusCanyonTemple.init();
    }
}
