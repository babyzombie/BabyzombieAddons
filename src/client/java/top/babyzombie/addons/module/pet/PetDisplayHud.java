package top.babyzombie.addons.module.pet;

import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import top.babyzombie.addons.config.ModConfigManager;
import top.babyzombie.addons.config.hud.HudManager;
import top.babyzombie.addons.util.pet.PetData;
import top.babyzombie.addons.util.pet.PetData.LevelInfo;
import top.babyzombie.addons.util.pet.PetManager;
import top.babyzombie.addons.util.pet.state.PlayerPetState;
import top.babyzombie.addons.util.tracker.HypixelLocationTracker;

import java.util.List;

/**
 * HUD overlay that displays the current summoned pet and exp-sharing pets.
 *
 * Layout (summoned pet, full size):
 *   [head icon]  Lvl {level} {Name}          ← name coloured by rarity
 *                {xp} / {toNext} ({pct}%)     ← toggleable
 *                {heldItem} [icon]            ← toggleable + icon toggleable
 *
 * Shared pets are printed at half size below a strikethrough separator.
 * Only active slots are shown (1 by default, 3 with Diana's Sharing is Caring).
 */
public final class PetDisplayHud {

    private static final String ELEMENT_NAME = "PetDisplay";

    private PetDisplayHud() {}

    public static void init() {
        HudElementRegistry.attachElementAfter(
            VanillaHudElements.OVERLAY_MESSAGE,
            Identifier.fromNamespaceAndPath("babyzombieaddons", "pet_display"),
            (context, tickCounter) -> {
                if (!HypixelLocationTracker.getInstance().isInSkyblock()) return;
                render(context);
            }
        );
    }

    private static void render(GuiGraphicsExtractor gui) {
        var config = ModConfigManager.get().pet;
        if (!config.petDisplay) return;
        if (!HudManager.shouldShow(ELEMENT_NAME)) return;

        PetManager pm = PetManager.getInstance();
        PetData current = pm.getCurrentPet();
        if (current == null) return;

        var font = Minecraft.getInstance().font;
        int x = HudManager.x(ELEMENT_NAME);
        int y = HudManager.y(ELEMENT_NAME);
        float s = HudManager.scale(ELEMENT_NAME);

        var pose = gui.pose();
        pose.pushMatrix();
        pose.translate(x, y);
        pose.scale(s, s);

        int textX = 18; // 16px icon + 2px gap
        int curY = 1;
        int lh = font.lineHeight; // 9

        // ===== Summoned Pet (full size) =====
        {
            ItemStack petHead = PetHeadTexture.getPetHead(current.type());
            gui.item(petHead, 0, 0);

            LevelInfo info = current.getLevelInfo();

            // Line 1: Lvl {level} {name}  — name coloured by rarity
            String line1 = "§7Lv.§f" + info.level() + " " + tierColor(current.tier()) + PetData.formatPetName(current.type());
            gui.text(font, line1, textX, curY, 0xFFFFFFFF, true);
            curY += lh;

            // Line 2: XP (toggleable)
            if (config.petExpDisplay) {
                gui.text(font, xpLine(info, current.exp()), textX, curY, 0xFFFFFFFF, true);
                curY += lh;
            }

            // Line 3: Held item (toggleable)
            if (config.petItemDisplay && current.heldItem() != null) {
                String line3 = PetHeadTexture.getItemDisplayName(current.heldItem());
                int line3Width = font.width(line3);

                if (config.petItemIconDisplay) {
                    ItemStack itemIcon = PetHeadTexture.getItemIcon(current.heldItem());
                    if (itemIcon != null && !itemIcon.isEmpty()) {
                        int iconX = textX + line3Width + 2;
                        int iconY = curY + 1;
                        pose.pushMatrix();
                        pose.translate(iconX, iconY);
                        pose.scale(0.5f, 0.5f);
                        gui.item(itemIcon, 0, 0);
                        pose.popMatrix();
                    }
                }
                gui.text(font, line3, textX, curY, 0xFFFFFFFF, true);
                curY += lh;
            }
        }

        // ===== Shared Pets (0.75×) =====
        if (config.petSharedDisplay) {
            List<PetData> sharedPets = pm.getSharedPets();
            PlayerPetState state = pm.getPetState();

            // Only show active slots: 1 by default, 3 with Diana
            int maxSlots = state.dianaSharingIsCaring ? 3 : 1;
            int activeCount = Math.min(sharedPets.size(), maxSlots);

            if (activeCount > 0) {
                // Separator
                curY += lh / 2;
                gui.text(font, "§8§m                              ", textX, curY, 0xFFFFFFFF, true);
                curY += lh / 2 + 1;

                // Shared pets at 0.75× — inner spacing = lh, outer = lh * 0.75
                int halfTextX = 18;
                int halfSpacing = lh;
                // icon(16) at 0.75 = 12 outer px; 2 text lines ≈ lh*2*0.75 = 1.5*lh outer px
                int row2line = Math.round(Math.max(12, lh * 1.5f)) + 1; // 15 for lh=9
                int row1line = Math.round(Math.max(12, lh * 0.75f)) + 1; // 13 for lh=9

                for (int i = 0; i < activeCount; i++) {
                    PetData shared = sharedPets.get(i);
                    LevelInfo si = shared.getLevelInfo();
                    ItemStack sharedHead = PetHeadTexture.getPetHead(shared.type());

                    String sLine1 = "§7Lv.§f" + si.level() + " " + tierColor(shared.tier()) + PetData.formatPetName(shared.type());

                    pose.pushMatrix();
                    pose.translate(0, curY);
                    pose.scale(0.75f, 0.75f);

                    gui.item(sharedHead, 0, 0);
                    gui.text(font, sLine1, halfTextX, 0, 0xFFFFFFFF, true);

                    if (config.petExpDisplay) {
                        gui.text(font, xpLine(si, shared.exp()), halfTextX, halfSpacing, 0xFFFFFFFF, true);
                        pose.popMatrix();
                        curY += row2line;
                    } else {
                        pose.popMatrix();
                        curY += row1line;
                    }
                }
            }
        }

        pose.popMatrix();
    }

    // ===== Helpers =====

    /** Build the XP line string. */
    private static String xpLine(LevelInfo info, double totalExp) {
        if (info.isMaxed()) {
            return "§e" + fmt(totalExp);
        }
        double pct = info.xpToNext() > 0
            ? (info.xpInLevel() / info.xpToNext()) * 100.0
            : 0.0;
        return "§e" + fmt(info.xpInLevel()) + " §8/ §7" + fmt(info.xpToNext())
            + "  §b" + String.format("%.1f", pct) + "%";
    }

    /** Format a double with thousands separators, no decimal places. */
    private static String fmt(double n) {
        return String.format("%,.0f", n);
    }

    /** Hypixel rarity → colour code. */
    private static String tierColor(int tier) {
        return switch (tier) {
            case 0 -> "§f";   // COMMON
            case 1 -> "§a";   // UNCOMMON
            case 2 -> "§9";   // RARE
            case 3 -> "§5";   // EPIC
            case 4 -> "§6";   // LEGENDARY
            case 5 -> "§d";   // MYTHIC
            default -> "§f";
        };
    }
}
