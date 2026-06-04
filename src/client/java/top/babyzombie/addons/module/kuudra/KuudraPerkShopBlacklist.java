package top.babyzombie.addons.module.kuudra;

import top.babyzombie.addons.config.ModConfigManager;
import top.babyzombie.addons.event.ContainerClickEvents;
import top.babyzombie.addons.util.ChatUtils;

public final class KuudraPerkShopBlacklist {
    private KuudraPerkShopBlacklist() {}

    public static void init() {
        ContainerClickEvents.BEFORE_MOUSE_CLICK.register((screen, slot) -> {
            if (!ModConfigManager.get().kuudra.perkShopBlacklist) return false;

            String title = ChatUtils.stripColor(screen.getTitle().getString());
            if (!title.equals("Perk Menu")) return false;

            if (slot == null || !slot.hasItem()) return false;

            String name = ChatUtils.stripColor(slot.getItem().getDisplayName().getString());
            String clean = name.replaceAll("^[\\[\\]\\s]+|[\\[\\]\\s]+$", "");
            for (String entry : ModConfigManager.get().kuudra.perkShopBlacklistItems.split(",")) {
                String e = ChatUtils.stripColor(entry).trim();
                if (!e.isEmpty() && clean.contains(e)) {
                    return true;
                }
            }
            return false;
        });
    }
}
