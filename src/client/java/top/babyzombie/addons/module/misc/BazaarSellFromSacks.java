package top.babyzombie.addons.module.misc;

import net.minecraft.client.Minecraft;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.component.ItemLore;
import top.babyzombie.addons.config.ModConfigManager;
import top.babyzombie.addons.event.ContainerClickEvents;
import top.babyzombie.addons.util.ChatUtils;
import top.babyzombie.addons.util.tracker.HypixelLocationTracker;
import top.babyzombie.addons.util.Scheduler;
import top.babyzombie.addons.config.ModConfig.BzGetFromSacksMode;
import top.babyzombie.addons.util.ServerTick;

/**
 * When creating a Bazaar sell offer and the hovered "Create Sell Offer"
 * button says "None in inventory!", auto-extract from sacks via
 * /getfromsacks, then re-click the slot.
 */
public final class BazaarSellFromSacks {

    private static long cooldown;

    private BazaarSellFromSacks() {}

    public static void init() {
        ContainerClickEvents.BEFORE_MOUSE_CLICK.register((screen, slot, event) -> {
            var mode = ModConfigManager.get().misc.bzGetFromSacks;
            if (mode == BzGetFromSacksMode.OFF) return false;
            if (cooldown > ServerTick.getTime()) return false;
            if (!HypixelLocationTracker.getInstance().isInSkyblock()) return false;
            if (slot == null || !slot.hasItem()) return false;

            var stack = slot.getItem();
            String name = ChatUtils.stripColor(stack.getHoverName().getString());
            if (!name.equals("Create Sell Offer")) return false;

            ItemLore itemLore = stack.get(DataComponents.LORE);
            if (itemLore == null) return false;

            String itemName = null;
            boolean noneInInv = false;
            int i = 0;
            for (Component line : itemLore.lines()) {
                String text = ChatUtils.stripColor(line.getString());
                if (i == 0) itemName = text;
                if (text.equals("None in inventory!")) noneInInv = true;
                i++;
            }

            if (!noneInInv || itemName == null) return false;

            ChatUtils.sendCommand("getfromsacks " + itemName + " 2304");
            cooldown = ServerTick.getTime() + 2000;

            if (mode == BzGetFromSacksMode.GET_AND_RECLICK) {
                int containerId = screen.getMenu().containerId;
                int slotIndex = slot.index;
                Scheduler.schedule(10, () -> { // 500ms = 10 ticks
                    var client = Minecraft.getInstance();
                    if (client.player != null && client.gameMode != null) {
                        client.gameMode.handleContainerInput(
                                containerId, slotIndex, 0,
                                ContainerInput.PICKUP, client.player);
                    }
                });
            }

            return true; // cancel the original click
        });
    }
}
