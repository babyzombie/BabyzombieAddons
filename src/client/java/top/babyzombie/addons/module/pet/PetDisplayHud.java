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

import java.util.ArrayList;
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
 *
 * Data is recomputed at most once per second to avoid per-frame overhead.
 */
public final class PetDisplayHud {

    private static final String ELEMENT_NAME = "PetDisplay";
    private static final long REFRESH_INTERVAL_MS = 1000;

    // ===== Cached display data (refreshed at most once per second) =====
    private static long lastRefreshTime;
    private static ItemStack cachedCurrentHead;
    private static String cachedCurrentLine1;
    private static String cachedCurrentXpLine;
    private static String cachedCurrentItemLine;
    private static ItemStack cachedCurrentItemIcon;
    private static final List<CachedSharedPet> cachedSharedPets = new ArrayList<>();

    private record CachedSharedPet(ItemStack head, String line1, String xpLine) {}

    private PetDisplayHud() {}

    public static void init() {
        HudElementRegistry.attachElementAfter(
            VanillaHudElements.OVERLAY_MESSAGE,
            Identifier.fromNamespaceAndPath("babyzombieaddons", "pet_display"),
            (context, tickCounter) -> {
                var tracker = HypixelLocationTracker.getInstance();
                if (!tracker.isInSkyblock() || tracker.isInRift() || tracker.isInSafari()) return;
                render(context);
            }
        );
    }

    /** Recompute all display strings and item stacks from current PetManager state. */
    private static void refreshCache() {
        var config = ModConfigManager.get().skyblock;

        PetManager pm = PetManager.getInstance();
        PetData current = pm.getCurrentPet();

        if (current == null) {
            cachedCurrentHead = null;
            cachedSharedPets.clear();
            return;
        }

        cachedCurrentHead = PetHeadTexture.getPetHead(current.type());
        LevelInfo info = current.getLevelInfo();
        cachedCurrentLine1 = "§7Lv.§f" + info.level() + " " + tierColor(current.tier()) + PetData.formatPetName(current.type());

        cachedCurrentXpLine = config.pet.expDisplay ? xpLine(info, current.exp()) : null;

        if (config.pet.itemDisplay && current.heldItem() != null) {
            cachedCurrentItemLine = PetHeadTexture.getItemDisplayName(current.heldItem());
            cachedCurrentItemIcon = config.pet.itemIconDisplay
                ? PetHeadTexture.getItemIcon(current.heldItem()) : null;
        } else {
            cachedCurrentItemLine = null;
            cachedCurrentItemIcon = null;
        }

        // Shared pets
        cachedSharedPets.clear();
        if (config.pet.sharedDisplay) {
            List<PetData> sharedPets = pm.getSharedPets();
            PlayerPetState state = pm.getPetState();
            int maxSlots = state.dianaSharingIsCaring ? 3 : 1;
            int activeCount = Math.min(sharedPets.size(), maxSlots);

            for (int i = 0; i < activeCount; i++) {
                PetData shared = sharedPets.get(i);
                LevelInfo si = shared.getLevelInfo();
                String sLine1 = "§7Lv.§f" + si.level() + " " + tierColor(shared.tier()) + PetData.formatPetName(shared.type());
                String sXpLine = config.pet.expDisplay ? xpLine(si, shared.exp()) : null;
                cachedSharedPets.add(new CachedSharedPet(
                    PetHeadTexture.getPetHead(shared.type()), sLine1, sXpLine));
            }
        }
    }

    private static void render(GuiGraphicsExtractor gui) {
        var config = ModConfigManager.get().skyblock;
        if (!config.pet.enabled) return;
        if (!HudManager.shouldShow(ELEMENT_NAME)) return;

        // Refresh cached data at most once per second
        long now = System.currentTimeMillis();
        if (now - lastRefreshTime >= REFRESH_INTERVAL_MS) {
            refreshCache();
            lastRefreshTime = now;
        }

        if (cachedCurrentHead == null) return;

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
            gui.item(cachedCurrentHead, 0, 0);
            gui.text(font, cachedCurrentLine1, textX, curY, 0xFFFFFFFF, true);

            if (cachedCurrentXpLine != null) {
                curY += lh;
                gui.text(font, cachedCurrentXpLine, textX, curY, 0xFFFFFFFF, true);
            }

            if (cachedCurrentItemLine != null) {
                curY += lh;
                int line3Width = font.width(cachedCurrentItemLine);

                if (cachedCurrentItemIcon != null && !cachedCurrentItemIcon.isEmpty()) {
                    int iconX = textX + line3Width + 2;
                    int iconY = curY + 1;
                    pose.pushMatrix();
                    pose.translate(iconX, iconY);
                    pose.scale(0.5f, 0.5f);
                    gui.item(cachedCurrentItemIcon, 0, 0);
                    pose.popMatrix();
                }
                gui.text(font, cachedCurrentItemLine, textX, curY, 0xFFFFFFFF, true);
            }
        }

        // ===== Shared Pets (0.75×) =====
        if (!cachedSharedPets.isEmpty()) {
            // Separator
            curY += lh;
            gui.text(font, "§8§m                              ", textX, curY, 0xFFFFFFFF, true);
            curY += lh / 2 + 1;

            int halfTextX = 18;
            int halfSpacing = lh;
            int row2line = Math.round(Math.max(12, lh * 1.5f)) + 1;
            int row1line = Math.round(Math.max(12, lh * 0.75f)) + 1;

            for (CachedSharedPet shared : cachedSharedPets) {
                pose.pushMatrix();
                pose.translate(0, curY);
                pose.scale(0.75f, 0.75f);

                gui.item(shared.head, 0, 0);
                gui.text(font, shared.line1, halfTextX, 0, 0xFFFFFFFF, true);

                if (shared.xpLine != null) {
                    gui.text(font, shared.xpLine, halfTextX, halfSpacing, 0xFFFFFFFF, true);
                    pose.popMatrix();
                    curY += row2line;
                } else {
                    pose.popMatrix();
                    curY += row1line;
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
