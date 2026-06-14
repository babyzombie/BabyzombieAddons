package top.babyzombie.addons.module.mining;


import java.util.regex.Pattern;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientWorldEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import top.babyzombie.addons.util.render.RenderPhaseRegister;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.phys.AABB;
import top.babyzombie.addons.util.ChatUtils;
import top.babyzombie.addons.util.ItemUtils;
import top.babyzombie.addons.util.Scheduler;
import top.babyzombie.addons.util.ServerTick;
import top.babyzombie.addons.util.render.BeamRenderer;
import top.babyzombie.addons.util.render.WorldRenderUtils;
import top.babyzombie.addons.util.render.WorldTextRenderer;
import top.babyzombie.addons.util.tracker.HypixelLocationTracker;
import top.babyzombie.addons.util.tracker.PartyTracker;
import top.babyzombie.addons.config.ModConfig.MineshaftWarpMode;
import top.babyzombie.addons.config.ModConfigManager;


import java.awt.Color;

public final class GlaciteMineshaftWaypoints {
    private static final Pattern MINESHAFT_ENTER_PAT = Pattern.compile(
            "[-]+\\n(.+) entered Glacite Mineshafts!\\n[-]+");

    private static long portalTimer;
    private static int lastCorpseScanTick;
    private static boolean inMineshaft;
    private static boolean mineshaftOwner;
    private static long enterMineshaftTime;
    private static boolean waitingPartyTransfer;
    private static String ownServerName;

    private GlaciteMineshaftWaypoints() {}

    public static void init() {
        // Track entering/exiting mineshaft
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            var t = HypixelLocationTracker.getInstance();
            if (!t.isInSkyblock()) { inMineshaft = false; return; }
            boolean nowIn = "Mineshaft".equals(t.getMap());
            if (nowIn && !inMineshaft) {
                enterMineshaftTime = ServerTick.getTime();
                // Auto warp if owner
                var warpMode = ModConfigManager.get().mining.glaciteMineshaftWarp;
                if (mineshaftOwner && (warpMode == MineshaftWarpMode.SEND_PTME
                        || warpMode == MineshaftWarpMode.PTME_AND_WARP)) {
                    enterMineshaftTime = ServerTick.getTime();
                    PartyTracker.getInstance().runWhenKnown(
                        () -> {
                            if (warpMode == MineshaftWarpMode.PTME_AND_WARP)
                                Scheduler.schedule(10, () -> ChatUtils.sendCommand("p warp"));
                        },
                        () -> {
                            Scheduler.schedule(6, () -> ChatUtils.sendCommand("pc !ptme"));
                            if (warpMode == MineshaftWarpMode.PTME_AND_WARP)
                                waitingPartyTransfer = true;
                        }
                    );
                }
            }
            inMineshaft = nowIn;
        });

        // Portal detection
        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            if (overlay) return;
            var warpMode = ModConfigManager.get().mining.glaciteMineshaftWarp;
            if (warpMode == MineshaftWarpMode.OFF) return;
            if (!isInDwarvenMines()) return;
            if (ChatUtils.stripColor(message.getString()).equals("WOW! You found a Glacite Mineshaft portal!")) {
                portalTimer = ServerTick.getTime() + 30_000;
                if (warpMode != MineshaftWarpMode.TITLE_ONLY) {
                    var player = Minecraft.getInstance().player;
                    if (player != null) {
                        player.level().playSound(player, player.blockPosition(),
                                net.minecraft.sounds.SoundEvents.ENDER_DRAGON_GROWL,
                                net.minecraft.sounds.SoundSource.MASTER, 1f, 1f);
                    }
                }
                ChatUtils.showTitle(
                        tr("babyzombieaddons.glacite.portal_title"),
                        warpMode == MineshaftWarpMode.PTME_AND_WARP
                                ? tr("babyzombieaddons.glacite.portal_sub_auto")
                                : tr("babyzombieaddons.glacite.portal_sub"),
                        0, 50, 10);
            }
        });

        // Mineshaft owner detection
        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            if (overlay) return;
            if (ModConfigManager.get().mining.glaciteMineshaftWarp == MineshaftWarpMode.OFF) return;
            if (!isInDwarvenMines()) return;
            var m = MINESHAFT_ENTER_PAT.matcher(message.getString());
            if (m.find()) {
                var self = Minecraft.getInstance().player;
                String name = ChatUtils.stripRank(ChatUtils.stripColor(m.group(1)));
                if (self != null && name.equals(self.getName().getString())) {
                    mineshaftOwner = true;
                    ownServerName = HypixelLocationTracker.getInstance().getServerName();
                }
            }
        });

        // Party transfer response → warp after becoming leader
        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            if (overlay) return;
            if (!waitingPartyTransfer) return;
            if (!isInMineshaft()) return;
            String text = ChatUtils.stripColor(message.getString());
            if (text.matches(".+ has promoted .+ to Party Leader")
                    || text.matches(".+将.+提拔为组队队长.+")
                    || text.matches("The party was transferred to .+ by .+")
                    || text.matches(".+已将组队移交给了.+")) {
                var self = Minecraft.getInstance().player;
                if (self != null && text.contains(self.getName().getString())) {
                    ChatUtils.sendCommand("p warp");
                    waitingPartyTransfer = false;
                }
            }
        });

        // Reset mineshaftOwner after timeout
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (!mineshaftOwner) return;
            if (client.player == null || client.player.tickCount % 20 != 0) return;
            if (ownServerName != null) {
                var currentServer = HypixelLocationTracker.getInstance().getServerName();
                if (currentServer != null && !currentServer.equals(ownServerName)) {
                    mineshaftOwner = false;
                    ownServerName = null;
                }
            }
            if (enterMineshaftTime > 0 && ServerTick.getTime() - enterMineshaftTime > 60_000) {
                mineshaftOwner = false;
                ownServerName = null;
            }
        });

        // World render
        RenderPhaseRegister.register(ctx -> {
            var t = HypixelLocationTracker.getInstance();

            // Corpse waypoints in mineshaft — detect and render
            if (t.isInSkyblock() && "Mineshaft".equals(t.getMap())) {
                var player = Minecraft.getInstance().player;
                if (player != null && player.tickCount != lastCorpseScanTick) {
                    lastCorpseScanTick = player.tickCount;
                    var level = player.level();
                    var stands = level.getEntitiesOfClass(ArmorStand.class,
                            new AABB(player.blockPosition()).inflate(96),
                            e -> !e.isDeadOrDying());
                    for (var stand : stands) {
                        var helm = stand.getItemBySlot(EquipmentSlot.LEGS);
                        if (helm.isEmpty()) continue;
                        String id = ItemUtils.getSkyblockId(helm);
                        if (id == null) continue;
                        String name = switch (id) {
                            case "LAPIS_ARMOR_LEGGINGS" -> "§bLapis";
                            case "ARMOR_OF_YOG_LEGGINGS" -> "§6Umber";
                            case "MINERAL_LEGGINGS" -> "§fTungsten";
                            case "VANGUARD_LEGGINGS" -> "§bVanguard";
                            default -> null;
                        };
                        if (name == null) continue;
                        var x = stand.getX();
                        var y = stand.getY() + 2;
                        var z = stand.getZ();
                        var color = mcColorToAwt(name);
                        WorldRenderUtils.drawWireframeBox(ctx,
                                x - 0.4, y - 2.0, z - 0.4,
                                x + 0.4, y + 0.2, z + 0.4,
                                color.getRed() / 255f, color.getGreen() / 255f, color.getBlue() / 255f, 0.6f,
                                false, 4.0f);
                        WorldTextRenderer.renderString(ctx, name, x, y, z, 0xFFFFFF55, 0.05f, true);
                    }
                }
            }

            // Portal timer in Dwarven Mines
            if (t.isInSkyblock() && "Dwarven Mines".equals(t.getMap()) && portalTimer > ServerTick.getTime()) {
                long remaining = portalTimer - ServerTick.getTime();
                var player = Minecraft.getInstance().player;
                if (player != null) {
                    var stands = player.level().getEntitiesOfClass(ArmorStand.class,
                            new AABB(player.blockPosition()).inflate(64),
                            e -> e.getName().getString().contains(player.getName().getString())
                                    && ChatUtils.stripColor(e.getName().getString()).endsWith("'s Mineshaft Portal"));
                    for (var s : stands) {
                        BeamRenderer.drawBeam(ctx, s.getX() - 0.5, s.getY(), s.getZ() - 0.5,
                                20, 0.15f, new Color(0.4f, 0.7f, 1.0f, 0.4f).getRGB());
                        WorldTextRenderer.renderString(ctx, "§a" + formatTime(remaining),
                                s.getX(), s.getY() + 2.5, s.getZ(), 0xFF55FFFF, 0.04f, false);
                    }
                }
            }
        });

        ClientWorldEvents.AFTER_CLIENT_WORLD_CHANGE.register((client, world) -> {
            portalTimer = 0; inMineshaft = false;
            mineshaftOwner = false; waitingPartyTransfer = false; ownServerName = null;
        });
    }

    private static boolean isInMineshaft() {
        var t = HypixelLocationTracker.getInstance();
        return t.isInSkyblock() && "Mineshaft".equals(t.getMap());
    }

    private static boolean isInDwarvenMines() {
        var t = HypixelLocationTracker.getInstance();
        return t.isInSkyblock() && "Dwarven Mines".equals(t.getMap());
    }

    private static String tr(String key) {
        return Component.translatable(key).getString();
    }

    private static String formatTime(long ms) {
        long s = ms / 1000; long m = s / 60; s %= 60;
        return String.format("%d:%02d", m, s);
    }

    private static Color mcColorToAwt(String text) {
        if (text.length() >= 2 && text.charAt(0) == '§') {
            return switch (text.charAt(1)) {
                case '0' -> new Color(0x000000);
                case '1' -> new Color(0x0000AA);
                case '2' -> new Color(0x00AA00);
                case '3' -> new Color(0x00AAAA);
                case '4' -> new Color(0xAA0000);
                case '5' -> new Color(0xAA00AA);
                case '6' -> new Color(0xFFAA00);
                case '7' -> new Color(0xAAAAAA);
                case '8' -> new Color(0x555555);
                case '9' -> new Color(0x5555FF);
                case 'a' -> new Color(0x55FF55);
                case 'b' -> new Color(0x55FFFF);
                case 'c' -> new Color(0xFF5555);
                case 'd' -> new Color(0xFF55FF);
                case 'e' -> new Color(0xFFFF55);
                case 'f' -> new Color(0xFFFFFF);
                default -> new Color(0xFFFFFF);
            };
        }
        return new Color(0xFFFFFF);
    }
}
