package top.babyzombie.addons.config.hud;

import top.babyzombie.addons.util.ChatUtils;

public enum HudTag {
    ALL,
    KUUDRA,
    DUNGEON,
    SLAYER,
    MINING,
    HUNTING,
    MISC,
    EVENTS,
    BAZAAR,
    FISHING;

    @Override
    public String toString() {
        return ChatUtils.translate("config.babyzombieaddons.hudTag." + name());
    }
}
