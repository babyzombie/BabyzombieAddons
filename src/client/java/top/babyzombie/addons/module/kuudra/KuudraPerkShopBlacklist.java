package top.babyzombie.addons.module.kuudra;

import top.babyzombie.addons.config.ModConfigManager;
import top.babyzombie.addons.event.ContainerClickEvents;
import top.babyzombie.addons.util.ChatUtils;
import top.babyzombie.addons.util.HypixelLocationTracker;

public final class KuudraPerkShopBlacklist {
    private KuudraPerkShopBlacklist() {}

    public static void init() {
        ContainerClickEvents.BEFORE_MOUSE_CLICK.register((screen, slot) -> {
            if (!ModConfigManager.get().kuudra.perkShopBlacklist) return false;
            if (!HypixelLocationTracker.getInstance().isInKuudra()) return false;

            String title = ChatUtils.stripColor(screen.getTitle().getString());
            if (!title.equals("Perk Menu")) return false;

            if (slot == null || !slot.hasItem()) return false;

            String name = ChatUtils.stripColor(slot.getItem().getDisplayName().getString());
            return ModConfigManager.get().kuudra.perkShopBlacklistItems.contains(name);
        });
    }
}
