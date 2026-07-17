package top.babyzombie.addons.module.hunting;

import top.babyzombie.addons.module.hunting.safari.SafariBellDisplay;
import top.babyzombie.addons.module.hunting.safari.SafariEntitiesGlow;

public final class HuntingModule {
    private HuntingModule() {}

    public static void init() {
        SafariBellDisplay.init();
        SafariEntitiesGlow.init();
    }
}
