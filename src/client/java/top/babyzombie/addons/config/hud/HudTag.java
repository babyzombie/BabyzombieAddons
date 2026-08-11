package top.babyzombie.addons.config.hud;

import top.babyzombie.addons.util.ChatUtils;

public enum HudTag {
    ALL,
    KUUDRA,
    DUNGEON,
    SLAYER,
    MINING,
    HUNTING,
    PET,
    CHAT,
    MISC,
    EVENTS,
    POPUP,
    BAZAAR,
    FISHING;

    @Override
    public String toString() {
        return ChatUtils.translate("config.babyzombieaddons.hudTag." + name());
    }
}
