package top.babyzombie.addons.module.slayer;

import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.Minecraft;
import net.minecraft.core.component.DataComponents;
import com.google.common.collect.LinkedHashMultimap;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.properties.PropertyMap;
import net.minecraft.resources.Identifier;
import net.minecraft.nbt.CompoundTag;
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

    private static final String GUMMY_BEAR_SKIN_URL = "http://textures.minecraft.net/texture/4306587ec38c2446d389a581e0691556fa58fce0a02d0846d23fd68e3656a249";

    private static ItemStack createGummyBearIcon() {
        var stack = new ItemStack(Items.PLAYER_HEAD);
        var uuid = UUID.fromString("74ddb947-f95e-3d16-bfb8-8d7fdadba323");
        var textureData = "{\"textures\":{\"SKIN\":{\"url\":\"" + GUMMY_BEAR_SKIN_URL + "\"}}}";
        var encoded = java.util.Base64.getEncoder().encodeToString(textureData.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        var multimap = LinkedHashMultimap.<String, Property>create();
        multimap.put("textures", new Property("textures", encoded, null));
        var gp = new GameProfile(uuid, "", new PropertyMap(multimap));
        stack.set(DataComponents.PROFILE, ResolvableProfile.createResolved(gp));
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
                if (!tracker2.isInDungeon() && !tracker2.isInRift()) {
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
