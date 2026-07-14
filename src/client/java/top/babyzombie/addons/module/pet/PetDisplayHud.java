package top.babyzombie.addons.module.pet;

import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import top.babyzombie.addons.config.ModConfigManager;
import top.babyzombie.addons.config.SkyblockConfig.Pet.PetDisplayElement;
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
 *   [head icon]  (controlled by showPetIcon toggle)
 *   DraggableList elements in user-defined order:
 *     PET_NAME          → Lv.{level} {Name} coloured by rarity
 *     PET_TOTAL_XP      → {totalExp}
 *     PET_XP_PROGRESS   → {xpInLevel} / {toNext} ({pct}%)  (overflow / 0 if maxed)
 *     PET_ITEM          → {heldItem} text only
 *     PET_ITEM_WITH_ICON → {heldItem} text + small icon to the right
 *
 * Shared pets are printed at 0.75× size below a strikethrough separator.
 * Only active slots are shown (1 by default, 3 with Diana's Sharing is Caring).
 *
 * Data is recomputed at most once per second to avoid per-frame overhead.
 */
public final class PetDisplayHud {

    private static final String ELEMENT_NAME = "PetDisplay";
    private static final long REFRESH_INTERVAL_MS = 1000;

    /** A single renderable line: text always present, icon only for PET_ITEM_WITH_ICON. */
    private record CachedLine(String text, @Nullable ItemStack icon) {}

    // ===== Cached display data (refreshed at most once per second) =====
    private static long lastRefreshTime;
    private static ItemStack cachedCurrentHead;
    private static List<CachedLine> cachedCurrentLines = List.of();
    private static final List<CachedSharedPet> cachedSharedPets = new ArrayList<>();

    private record CachedSharedPet(ItemStack head, List<CachedLine> lines) {}

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
            cachedCurrentLines = List.of();
            cachedSharedPets.clear();
            return;
        }

        cachedCurrentHead = PetHeadTexture.getPetHead(current.type());
        cachedCurrentLines = buildLines(current, config.pet.mainPetElements);

        // Shared pets
        cachedSharedPets.clear();
        if (config.pet.sharedDisplay) {
            List<PetData> sharedPets = pm.getSharedPets();
            PlayerPetState state = pm.getPetState();
            int maxSlots = state.dianaSharingIsCaring ? 3 : 1;
            int activeCount = Math.min(sharedPets.size(), maxSlots);

            for (int i = 0; i < activeCount; i++) {
                PetData shared = sharedPets.get(i);
                cachedSharedPets.add(new CachedSharedPet(
                    PetHeadTexture.getPetHead(shared.type()),
                    buildLines(shared, config.pet.sharedPetElements)
                ));
            }
        }
    }

    /** Build the list of CachedLine for a pet based on the given element list. */
    private static List<CachedLine> buildLines(PetData pet, List<PetDisplayElement> elements) {
        if (pet == null || elements == null || elements.isEmpty()) return List.of();

        List<CachedLine> lines = new ArrayList<>();
        LevelInfo info = pet.getLevelInfo();

        for (PetDisplayElement elem : elements) {
            switch (elem) {
                case PET_NAME -> lines.add(buildNameLine(pet, info));
                case PET_TOTAL_XP -> lines.add(buildTotalXpLine(pet));
                case PET_XP_PROGRESS -> {
                    // If pet is maxed and total XP is also shown, skip — total XP already covers it
                    if (info.isMaxed() && elements.contains(PetDisplayElement.PET_TOTAL_XP)) {
                        break;
                    }
                    lines.add(buildXpProgressLine(pet, info));
                }
                case PET_ITEM -> {
                    if (pet.heldItem() != null) {
                        lines.add(new CachedLine(PetHeadTexture.getItemDisplayName(pet.heldItem()), null));
                    }
                }
                case PET_ITEM_WITH_ICON -> {
                    if (pet.heldItem() != null) {
                        lines.add(new CachedLine(
                            PetHeadTexture.getItemDisplayName(pet.heldItem()),
                            PetHeadTexture.getItemIcon(pet.heldItem())
                        ));
                    }
                }
            }
        }
        return lines;
    }

    // ── Individual line builders ──

    private static CachedLine buildNameLine(PetData pet, LevelInfo info) {
        return new CachedLine(
            "§7Lv.§f" + info.level() + " " + tierColor(pet.tier()) + PetData.formatPetName(pet.type()),
            null
        );
    }

    private static CachedLine buildTotalXpLine(PetData pet) {
        return new CachedLine("§e" + fmt(pet.exp()), null);
    }

    private static CachedLine buildXpProgressLine(PetData pet, LevelInfo info) {
        if (info.isMaxed()) {
            // Overflow experience / 0
            return new CachedLine("§e" + fmt(pet.exp()) + " §8/ §70", null);
        }
        double pct = info.xpToNext() > 0
            ? (info.xpInLevel() / info.xpToNext()) * 100.0
            : 0.0;
        return new CachedLine(
            "§e" + fmt(info.xpInLevel()) + " §8/ §7" + fmt(info.xpToNext())
                + "  §b" + String.format("%.1f", pct) + "%",
            null
        );
    }

    // ── Render ──

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

        int curY = 1;
        int lh = font.lineHeight;

        // ===== Summoned Pet (full size) =====
        {
            if (config.pet.showPetIcon) {
                gui.item(cachedCurrentHead, -16, 0);
            }
            renderLines(gui, font, cachedCurrentLines, 0, curY, lh);
        }

        // ===== Shared Pets (0.75×) =====
        if (!cachedSharedPets.isEmpty()) {
            // Calculate main pet height to place separator correctly
            curY += cachedCurrentLines.size() * lh;
            // Separator — dynamic width based on the widest main-pet line
            {
                int maxWidth = 0;
                for (CachedLine line : cachedCurrentLines) {
                    int w = font.width(line.text());
                    if (w > maxWidth) maxWidth = w;
                }
                int spaceW = font.width(" ");
                int count = Math.max(10, maxWidth / spaceW + 1);
                gui.text(font, "§8§m" + " ".repeat(count), 0, curY, 0xFFFFFFFF, true);
            }
            curY += lh / 2 + 1;

            for (CachedSharedPet shared : cachedSharedPets) {
                pose.pushMatrix();
                pose.translate(0, curY);
                pose.scale(0.75f, 0.75f);

                if (config.pet.showPetIcon) {
                    gui.item(shared.head, -16, 0);
                }
                renderLines(gui, font, shared.lines, 0, 0, lh);

                pose.popMatrix();

                // Calculate how tall this shared pet block is
                int sharedLineCount = shared.lines.size();
                if (sharedLineCount == 0) {
                    curY += Math.round(Math.max(12, lh * 0.75f)) + 1;
                } else {
                    curY += Math.round(Math.max(12, lh * 1.5f)) + 1;
                }
            }
        }

        pose.popMatrix();
    }

    /** Render a list of CachedLine at the given position. */
    private static void renderLines(GuiGraphicsExtractor gui, Font font,
                                     List<CachedLine> lines, int textX, int startY,
                                     int lineHeight) {
        int curY = startY;
        var pose = gui.pose();
        for (CachedLine line : lines) {
            String displayText = line.text();
            if (line.icon() != null && !line.icon().isEmpty()) {
                // Draw text first, then icon to the right
                int textWidth = font.width(displayText);
                int iconX = textX + textWidth + 2;
                int iconY = curY + 1;
                pose.pushMatrix();
                pose.translate(iconX, iconY);
                pose.scale(0.5f, 0.5f);
                gui.item(line.icon(), 0, 0);
                pose.popMatrix();
            }
            gui.text(font, displayText, textX, curY, 0xFFFFFFFF, true);
            curY += lineHeight;
        }
    }

    // ===== Helpers =====

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
