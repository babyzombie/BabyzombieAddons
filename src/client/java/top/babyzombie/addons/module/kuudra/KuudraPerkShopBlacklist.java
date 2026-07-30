package top.babyzombie.addons.module.kuudra;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.ContainerInput;
import top.babyzombie.addons.config.ModConfig;
import top.babyzombie.addons.config.ModConfigManager;
import top.babyzombie.addons.event.ContainerClickEvents;
import top.babyzombie.addons.util.ChatUtils;

/**
 * Perk Shop 白名单 — 只有白名单内的物品可以购买。
 * 不在 PerkShopItem 枚举中的物品不做拦截。
 */
public final class KuudraPerkShopBlacklist {
    private KuudraPerkShopBlacklist() {}

    public static void init() {
        ContainerClickEvents.BEFORE_CONTAINER_INPUT.register((player, containerId, slotId, buttonNum, input) -> {
            if (input != ContainerInput.PICKUP) return false;

            var cfg = ModConfigManager.get().kuudra.perkShop;
            if (!cfg.perkShopWhitelist) return false;

            var screen = Minecraft.getInstance().screen;
            if (!(screen instanceof AbstractContainerScreen<?> cs)) return false;
            String title = ChatUtils.stripColor(cs.getTitle().getString());
            if (!title.equals("Perk Menu")) return false;

            var slots = cs.getMenu().slots;
            if (slotId < 0 || slotId >= slots.size()) return false;
            var slot = slots.get(slotId);
            if (!slot.hasItem()) return false;

            String name = ChatUtils.stripColor(slot.getItem().getDisplayName().getString());
            String clean = name.replaceAll("^[\\[\\]\\s]+|[\\[\\]\\s]+$", "");

            // Check if this item is in our enum at all
            ModConfig.PerkShopItem matchedEnum = null;
            for (ModConfig.PerkShopItem item : ModConfig.PerkShopItem.values()) {
                if (clean.contains(item.getDisplayName())) {
                    matchedEnum = item;
                    break;
                }
            }
            // Not in enum → don't block
            if (matchedEnum == null) return false;

            // In enum → block if NOT in whitelist
            for (ModConfig.PerkShopItem allowed : cfg.perkShopWhitelistItems) {
                if (allowed == matchedEnum) return false; // allowed
            }
            return true; // blocked
        });
    }
}
