package top.babyzombie.addons.module.mining;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.phys.AABB;
import top.babyzombie.addons.util.ItemUtils;
import top.babyzombie.addons.config.ModConfig.MineshaftWarpMode;
import top.babyzombie.addons.config.ModConfigManager;
import top.babyzombie.addons.util.BeaconBeamRenderer;
import top.babyzombie.addons.util.ChatUtils;
import top.babyzombie.addons.util.HypixelLocationTracker;
import top.babyzombie.addons.util.PartyTracker;
import top.babyzombie.addons.util.Scheduler;
import top.babyzombie.addons.util.ServerTick;
import top.babyzombie.addons.util.WorldTextRenderer;
import top.babyzombie.addons.util.WorldTextRenderer.TextEntry;

import java.awt.Color;

public final class GlaciteMineshaftWaypoints {
    private static final Pattern MINESHAFT_ENTER_PAT = Pattern.compile(
            "[-]+\\n(.+) entered Glacite Mineshafts!\\n[-]+", Pattern.DOTALL);

    private static long portalTimer;
    private static final List<Waypoint> exits = new ArrayList<>();
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
                if (client.player != null) {
                    var wp = new Waypoint(client.player.getX(), client.player.getY(), client.player.getZ(),
                            tr("babyzombieaddons.glacite.exit"));
                    exits.add(wp);
                }
                enterMineshaftTime = ServerTick.getTime();
                // Auto warp if owner
                var warpMode = ModConfigManager.get().mining.glaciteMineshaftWarp;
                if (mineshaftOwner && (warpMode == MineshaftWarpMode.SEND_PTME
                        || warpMode == MineshaftWarpMode.PTME_AND_WARP)) {
                    enterMineshaftTime = ServerTick.getTime();
                    if (PartyTracker.getInstance().isSelfLeader()) {
                        if (warpMode == MineshaftWarpMode.PTME_AND_WARP) {
                            Scheduler.schedule(10, () -> ChatUtils.sendCommand("p warp"));
                        }
                    } else {
                        ChatUtils.sendCommand("pc !ptme");
                        if (warpMode == MineshaftWarpMode.PTME_AND_WARP)
                            waitingPartyTransfer = true;
                    }
                }
            }
            if (!nowIn) exits.clear();
            inMineshaft = nowIn;
        });

        // Corpses
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (!ModConfigManager.get().mining.mineshaftWaypoints) return;
            if (!inMineshaft || client.player == null || client.player.tickCount % 20 != 0) return;
            var level = client.player.level();
            var stands = level.getEntitiesOfClass(ArmorStand.class,
                    new AABB(client.player.blockPosition()).inflate(64),
                    e -> !e.isDeadOrDying() && !e.isInvisible());
            for (var stand : stands) {
                var helm = stand.getItemBySlot(EquipmentSlot.HEAD);
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
                if (name != null) exits.add(new Waypoint(stand.getX(), stand.getY() + 2, stand.getZ(), name));
            }
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
            String text = message.getString();
            // Each line of multiline comes separately, match middle line
            if (text.contains("entered Glacite Mineshafts!")) {
                var self = Minecraft.getInstance().player;
                if (self != null && text.contains(self.getName().getString())) {
                    mineshaftOwner = true;
                    ownServerName = HypixelLocationTracker.getInstance().getServerName();
                }
            }
        });

        // Party transfer response → warp after becoming leader
        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            if (overlay) return;
            if (!waitingPartyTransfer) return;
            if (!inMineshaft) return;
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
        WorldRenderEvents.BEFORE_ENTITIES.register(ctx -> {
            var t = HypixelLocationTracker.getInstance();
            var entries = new ArrayList<TextEntry>();

            // Exit/corpse waypoints in mineshaft
            if (t.isInSkyblock() && "Mineshaft".equals(t.getMap())) {
                for (var e : exits) entries.add(new TextEntry(e.label, e.x, e.y, e.z, 0xFFFF55));
            }

            // Portal timer in Dwarven Mines
            if (t.isInSkyblock() && "Dwarven Mines".equals(t.getMap()) && portalTimer > ServerTick.getTime()) {
                long remaining = portalTimer - ServerTick.getTime();
                // Find portal stand and render beam
                var player = Minecraft.getInstance().player;
                if (player != null) {
                    var stands = player.level().getEntitiesOfClass(ArmorStand.class,
                            new AABB(player.blockPosition()).inflate(64),
                            e -> e.getName().getString().contains(player.getName().getString())
                                    && ChatUtils.stripColor(e.getName().getString()).endsWith("'s Mineshaft Portal"));
                    for (var s : stands) {
                        BeaconBeamRenderer.addBeam(s.getX() - 0.5, s.getY(), s.getZ() - 0.5,
                                new Color(0, 1, 1, 1), 20f, 1000);
                        entries.add(new TextEntry("§a" + formatTime(remaining),
                                s.getX(), s.getY() + 1, s.getZ(), 0x55FFFF));
                    }
                }
            }

            WorldTextRenderer.render(ctx.matrices(), entries);
        });

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            exits.clear(); portalTimer = 0; inMineshaft = false;
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

    private record Waypoint(double x, double y, double z, String label) {}
}
