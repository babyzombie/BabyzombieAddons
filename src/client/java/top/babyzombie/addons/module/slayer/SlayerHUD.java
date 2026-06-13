package top.babyzombie.addons.module.slayer;

import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.Minecraft;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ResolvableProfile;
import top.babyzombie.addons.config.ModConfig;
import top.babyzombie.addons.config.ModConfigManager;
import top.babyzombie.addons.util.ChatUtils;
import top.babyzombie.addons.config.hud.HudManager;
import top.babyzombie.addons.util.tracker.HypixelLocationTracker;
import top.babyzombie.addons.util.ServerTick;

import java.util.UUID;

public final class SlayerHUD {

    // Lazy-initialized item icons (must delay to avoid "Components not bound yet")
    private static ItemStack gummyBearIcon;
    private static ItemStack endStoneSwordIcon;

    private static ItemStack getGummyBearIcon() {
        if (gummyBearIcon == null) gummyBearIcon = createGummyBearIcon();
        return gummyBearIcon;
    }

    private static ItemStack getEndStoneSwordIcon() {
        if (endStoneSwordIcon == null) endStoneSwordIcon = createEndStoneSwordIcon();
        return endStoneSwordIcon;
    }

    private SlayerHUD() {}

    private static ItemStack createGummyBearIcon() {
        var stack = new ItemStack(Items.PLAYER_HEAD);
        var tag = new CompoundTag();
        tag.putString("name", "");
        var uuid = UUID.fromString("74ddb947-f95e-3d16-bfb8-8d7fdadba323");
        tag.putIntArray("id", new int[] {
            (int)(uuid.getMostSignificantBits() >> 32), (int)(uuid.getMostSignificantBits()),
            (int)(uuid.getLeastSignificantBits() >> 32), (int)(uuid.getLeastSignificantBits())});
        var props = new CompoundTag();
        var textures = new ListTag();
        var tex = new CompoundTag();
        tex.putString("Value", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNDMwNjU4N2VjMzhjMjQ0NmQzODlhNTgxZTA2OTE1NTZmYTU4ZmNlMGEwMmQwODQ2ZDIzZmQ2OGUzNjU2YTI0OSJ9fX0=");
        textures.add(tex);
        props.put("textures", textures);
        tag.put("properties", props);
        var profile = ResolvableProfile.CODEC.parse(NbtOps.INSTANCE, tag).result().orElse(null);
        stack.set(DataComponents.PROFILE, profile);
        return stack;
    }

    private static ItemStack createEndStoneSwordIcon() {
        var stack = new ItemStack(Items.GOLDEN_SWORD);
        var tag = new CompoundTag();
        tag.putInt("HideFlags", 254);
        var ea = new CompoundTag();
        ea.putString("id", "END_STONE_SWORD");
        tag.put("ExtraAttributes", ea);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        return stack;
    }

    public static void init() {
        HudElementRegistry.attachElementAfter(VanillaHudElements.OVERLAY_MESSAGE,
                Identifier.fromNamespaceAndPath("babyzombieaddons", "slayer_hud"),
                (context, tickCounter) -> {
            if (!HypixelLocationTracker.getInstance().isInSkyblock()) return;
            render(context);
        });
    }

    private static void render(GuiGraphicsExtractor gui) {
        var font = Minecraft.getInstance().font;
        var config = ModConfigManager.get().slayer;
        long now = ServerTick.getTime();

        // ---- Pigman Sword ----
        if (HudManager.shouldShow("PigmanSword")) {
            if (PigmanSwordTimer.time > 0) {
                long rem = 5000 - (now - PigmanSwordTimer.time);
                if (rem > 0) {
                    HudManager.drawScaled(gui, font, "§6Pigman: §a" + ChatUtils.formatTime(rem),
                        HudManager.x("PigmanSword"), HudManager.y("PigmanSword"),
                        HudManager.scale("PigmanSword"));
                } else {
                    PigmanSwordTimer.time = 0;
                }
            }
        }

        // ---- Holy Ice ----
        if (HudManager.shouldShow("HolyIce")) {
            HolyIceTimer.updateText();
            if (!HolyIceTimer.text.isEmpty()) {
                HudManager.drawScaled(gui, font, HolyIceTimer.text,
                    HudManager.x("HolyIce"), HudManager.y("HolyIce"),
                    HudManager.scale("HolyIce"));
            }
        }

        // ---- Ragnarock Axe ----
        if (HudManager.shouldShow("RagnarockAxe")) {
            RagnarockAxeTimer.update();
            if (!RagnarockAxeTimer.text.isEmpty()) {
                HudManager.drawScaled(gui, font, RagnarockAxeTimer.text,
                    HudManager.x("RagnarockAxe"), HudManager.y("RagnarockAxe"),
                    HudManager.scale("RagnarockAxe"));
            }
        }

        // ---- Reaper Armor ----
        if (HudManager.shouldShow("ReaperArmor")) {
            if (ReaperArmorTimer.cooldownEnd > 0) {
                long rNow = now;
                String text = "§2Reaper: ";
                if (ReaperArmorTimer.activeTime > rNow) {
                    text += "§a" + ChatUtils.formatTime(ReaperArmorTimer.activeTime - rNow);
                } else if (ReaperArmorTimer.cooldownEnd > rNow) {
                    text += "§e" + ChatUtils.formatTime(ReaperArmorTimer.cooldownEnd - rNow);
                } else {
                    ReaperArmorTimer.activeTime = 0;
                    ReaperArmorTimer.cooldownEnd = 0;
                    text = "";
                }
                if (!text.isEmpty()) {
                    HudManager.drawScaled(gui, font, text,
                        HudManager.x("ReaperArmor"), HudManager.y("ReaperArmor"),
                        HudManager.scale("ReaperArmor"));
                }
            }
        }

        // ---- End Stone Sword ----
        if (HudManager.shouldShow("EndStoneSword")) {
            if (EndStoneSwordTimer.isActive()) {
                int x = HudManager.x("EndStoneSword"), y = HudManager.y("EndStoneSword");
                float s = HudManager.scale("EndStoneSword");
                long rem = EndStoneSwordTimer.time - now;

                // Draw sword icon
                gui.item(getEndStoneSwordIcon(), x, y);

                // Draw resistance + time text beside the icon
                int tx = x + 18, ty = y + 4;
                var ps = gui.pose();
                ps.pushMatrix();
                ps.translate((float)tx, (float)ty);
                ps.scale(s, s);
                gui.text(font, String.format("§a❈ %d%%", EndStoneSwordTimer.resistance), 0, 0, 0xFFFFFFFF, true);
                gui.text(font, "§e" + ChatUtils.formatTime(rem), 0, (int)(9 * s), 0xFFFFFFFF, true);
                ps.popMatrix();
            }
        }

        // ---- Reheated Gummy Polar Bear ----
        if (HudManager.shouldShow("ReheatedGummyPolarBear")) {
            var cfg = config.reheatedGummyPolarBear;
            if (cfg != ModConfig.GummyPolarBearMode.OFF) {
                var tracker2 = HypixelLocationTracker.getInstance();
                if (!tracker2.isInDungeon() && !"The Rift".equals(tracker2.getMap())) {
                    if (cfg == ModConfig.GummyPolarBearMode.EVERYWHERE_EXCEPT_DUNGEON
                            || "Smoldering Tomb".equals(tracker2.getLocation())) {
                        String profileId = tracker2.getProfileId();
                        if (profileId != null) {
                            String timeStr = ReheatedGummyPolarBearTimer.getTimeString(profileId);
                            if (!timeStr.isEmpty()) {
                                int x = HudManager.x("ReheatedGummyPolarBear");
                                int y = HudManager.y("ReheatedGummyPolarBear");
                                float s = HudManager.scale("ReheatedGummyPolarBear");

                                // Draw the skull icon
                                gui.item(getGummyBearIcon(), x, y);

                                // Draw time text offset to the right of the icon
                                int textX = x + 18; // 16px icon + 2px gap
                                int textY = y + 4;  // centred vertically
                                var ps2 = gui.pose();
                                ps2.pushMatrix();
                                ps2.translate((float)textX, (float)textY);
                                ps2.scale(s, s);
                                gui.text(font, "§a" + timeStr, 0, 0, 0xFFFFFFFF, true);
                                ps2.popMatrix();
                            }
                        }
                    }
                }
            }
        }

        // ---- Slayer Boss Info ----
        if (HudManager.shouldShow("SlayerBoss")) {
            if (!SlayerBossDetector.renderStr.isEmpty()) {
                HudManager.drawScaled(gui, font, SlayerBossDetector.renderStr,
                        HudManager.x("SlayerBoss"), HudManager.y("SlayerBoss"),
                        HudManager.scale("SlayerBoss"));
            }
        }
    }
}
