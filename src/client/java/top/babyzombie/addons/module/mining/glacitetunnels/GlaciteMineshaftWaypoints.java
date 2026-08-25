package top.babyzombie.addons.module.mining.glacitetunnels;


import java.util.*;
import java.util.regex.Pattern;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLevelEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import top.babyzombie.addons.config.ModConfig;
import top.babyzombie.addons.util.*;
import top.babyzombie.addons.util.render.*;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.phys.AABB;
import top.babyzombie.addons.event.HypixelLocationEvents;
import top.babyzombie.addons.mixin.render.PlayerTabOverlayAccessor;
import top.babyzombie.addons.util.tracker.HypixelLocationTracker;
import top.babyzombie.addons.util.tracker.PartyTracker;
import top.babyzombie.addons.config.ModConfig.GlaciteMineshaftPortalAction;
import top.babyzombie.addons.config.ModConfig.MineshaftCorpseRenderMode;
import top.babyzombie.addons.config.ModConfigManager;


import java.awt.Color;

public final class GlaciteMineshaftWaypoints {
    private static final Pattern MINESHAFT_ENTER_PAT = Pattern.compile(
            "[-]+\\n(.+) entered Glacite Mineshafts!\\n[-]+");

    private static long portalTimer;
    private static boolean inMineshaft;
    private static boolean mineshaftOwner;
    private static long enterMineshaftTime;
    private static boolean waitingPartyTransfer;
    private static String ownServerName;
    private static final Set<BlockPos> visitedCorpses = new HashSet<>();
    private static Runnable lapisGateTask;
    private static int lapisGateRemainingChecks;
    private static long lastOwnerDetectTime;
    private static long lastEnterActionTime;

    private static final Set<String> LANTERN_IDS = Set.of(
            "LANTERN", "MITHRIL_LANTERN", "TITANIUM_LANTERN", "GLACITE_LANTERN", "WILL_O_WASP");

    private static final List<Float> lllllava = Arrays.asList(0.9f, 2.45f, 40.7f, 42.2f);

    private GlaciteMineshaftWaypoints() {}

    private enum CorpseType {
        LAPIS("LAPIS_ARMOR_LEGGINGS", "§9Lapis"),
        UMBER("ARMOR_OF_YOG_LEGGINGS", "§6Umber"),
        TUNGSTEN("MINERAL_LEGGINGS", "§fTungsten"),
        VANGUARD("VANGUARD_LEGGINGS", "§bVanguard");

        final String skyblockId;
        final String displayName;

        CorpseType(String skyblockId, String displayName) {
            this.skyblockId = skyblockId;
            this.displayName = displayName;
        }
    }

    private static final Map<String, CorpseType> CORPSE_ID_MAP = Map.of(
            CorpseType.LAPIS.skyblockId, CorpseType.LAPIS,
            CorpseType.UMBER.skyblockId, CorpseType.UMBER,
            CorpseType.TUNGSTEN.skyblockId, CorpseType.TUNGSTEN,
            CorpseType.VANGUARD.skyblockId, CorpseType.VANGUARD
    );

    private record Corpse(ArmorStand stand, BlockPos pos, double x, double y, double z, CorpseType type) {}

    private static List<Corpse> scanCorpses(BlockPos center, double range) {
        var player = Minecraft.getInstance().player;
        if (player == null) return List.of();
        var level = player.level();
        var stands = level.getEntitiesOfClass(ArmorStand.class,
                new AABB(center).inflate(range),
                e -> !e.isDeadOrDying());
        if (stands.isEmpty()) return List.of();
        List<Corpse> corpses = new ArrayList<>();
        for (var stand : stands) {
            var legs = stand.getItemBySlot(EquipmentSlot.LEGS);
            if (legs.isEmpty()) continue;
            String id = ItemUtils.getSkyblockId(legs);
            if (id == null) continue;
            CorpseType type = CORPSE_ID_MAP.get(id);
            if (type == null) continue;
            corpses.add(new Corpse(stand, stand.blockPosition(), stand.getX(), stand.getY(), stand.getZ(), type));
        }
        return corpses;
    }

    private static boolean isSelfEnteredRecently() {
        return lastOwnerDetectTime > 0 && ServerTick.getTime() - lastOwnerDetectTime <= 30_000;
    }

    private static int countLapisFromTabList() {
        var client = Minecraft.getInstance();
        if (client.getConnection() == null) return -1;
        var tabList = client.gui.getTabList();
        var ta = (PlayerTabOverlayAccessor) tabList;
        var players = ta.invokeGetPlayerInfos();
        if (players == null || players.isEmpty()) return -1;
        boolean inFrozenCorpses = false;
        int lapis = 0;
        for (var info : players) {
            var dn = info.getTabListDisplayName();
            if (dn == null) continue;
            String line = ChatUtils.toLegacyString(dn).trim();
            if (line.isEmpty()) {
                if (inFrozenCorpses) break;
                continue;
            }
            String plain = ChatUtils.stripColor(line).trim();
            if (!inFrozenCorpses) {
                if (plain.startsWith("Frozen Corpses")) {
                    inFrozenCorpses = true;
                }
                continue;
            }
            if (plain.startsWith("Lapis:")) lapis++;
        }
        return inFrozenCorpses ? lapis : -1;
    }

    private static void tryRunEnterActions(String source) {
        if (!isInMineshaft()) return;
        var cfg = ModConfigManager.get().mining.glaciteTunnels.glaciteMineshaft;
        var action = cfg.portalAction;
        boolean eligible = mineshaftOwner || isSelfEnteredRecently();
        if (!eligible) return;
        if (action != GlaciteMineshaftPortalAction.SEND_PTME && action != GlaciteMineshaftPortalAction.PTME_AND_WARP) return;
        long now = ServerTick.getTime();
        if (lastEnterActionTime > 0 && now - lastEnterActionTime < 3_000) return;
        lastEnterActionTime = now;
        enterMineshaftTime = now;
        checkLanternReminder();
        if (action == GlaciteMineshaftPortalAction.PTME_AND_WARP && cfg.requireTwoLapisForPtmeWarp) {
            var player = Minecraft.getInstance().player;
            if (player == null) return;
            if (lapisGateTask != null) {
                Scheduler.cancel(lapisGateTask);
                lapisGateTask = null;
            }
            lapisGateRemainingChecks = 40;
            lapisGateTask = () -> {
                if (!isInMineshaft()) {
                    Scheduler.cancel(lapisGateTask);
                    lapisGateTask = null;
                    return;
                }
                if (lapisGateRemainingChecks-- <= 0) {
                    Scheduler.cancel(lapisGateTask);
                    lapisGateTask = null;
                    return;
                }
                var p = Minecraft.getInstance().player;
                if (p == null) return;
                var c = ModConfigManager.get().mining.glaciteTunnels.glaciteMineshaft;
                if (c.portalAction != GlaciteMineshaftPortalAction.PTME_AND_WARP || !c.requireTwoLapisForPtmeWarp) {
                    Scheduler.cancel(lapisGateTask);
                    lapisGateTask = null;
                    return;
                }
                int tabLapis = countLapisFromTabList();
                if (tabLapis >= 2) {
                    runPartyAction(GlaciteMineshaftPortalAction.PTME_AND_WARP);
                    Scheduler.cancel(lapisGateTask);
                    lapisGateTask = null;
                }
            };
            Scheduler.scheduleRepeating(10, lapisGateTask);
            return;
        }
        runPartyAction(action);
    }

    private static void runPartyAction(GlaciteMineshaftPortalAction action) {
        if (action != GlaciteMineshaftPortalAction.SEND_PTME && action != GlaciteMineshaftPortalAction.PTME_AND_WARP) return;
        enterMineshaftTime = ServerTick.getTime();
        if (action == GlaciteMineshaftPortalAction.SEND_PTME) {
            Scheduler.schedule(6, () -> ChatUtils.sendCommand("pc !ptme"));
            return;
        }
        PartyTracker.getInstance().runWhenKnown(
                () -> {
                    ChatUtils.sendCommand("p warp");
                },
                () -> {
                    Scheduler.schedule(6, () -> ChatUtils.sendCommand("pc !ptme"));
                    waitingPartyTransfer = true;
                }
        );
    }

    public static void init() {
        HypixelLocationEvents.LOCATION_UPDATE.register(data -> {
            if (!data.isInSkyblock()) { inMineshaft = false; return; }
            boolean nowIn = data.isIn("Mineshaft");
            if (nowIn && !inMineshaft) {
                tryRunEnterActions("location-update");
            }
            inMineshaft = nowIn;
        });

        // Portal detection
        ClientReceiveMessageEvents.ALLOW_GAME.register((message, overlay) -> {
            if (overlay) return true;
            var cfg = ModConfigManager.get().mining.glaciteTunnels.glaciteMineshaft;
            if (!cfg.portalTitleAlert
                    && !cfg.portalSoundAlert
                    && cfg.portalAction == GlaciteMineshaftPortalAction.NONE) return true;
            if (!isInDwarvenMines()) return true;
            if (ChatUtils.stripColor(message.getString()).equals("WOW! You found a Glacite Mineshaft portal!")) {
                portalTimer = ServerTick.getTime() + 30_000;
                if (cfg.portalSoundAlert) {
                    var player = Minecraft.getInstance().player;
                    if (player != null) {
                        if (cfg.portalSound == ModConfig.GlaciteMineshaftPortalSound.LAVA_CHICKEN) {
                            var instance = new SimpleSoundInstance(
                                    cfg.portalSound.sound.location(), SoundSource.MASTER,
                                    1f, 1f,
                                    SoundInstance.createUnseededRandom(),
                                    false, 0,
                                    SoundInstance.Attenuation.NONE,
                                    0, 0, 0, true
                            );
                            PlaySoundHelper.playSeeked(instance, lllllava.get((int) (Math.random() * lllllava.size())), 1.5f);
                        } else {
                            player.level().playSound(player, player.blockPosition(),
                                    cfg.portalSound.sound,
                                    net.minecraft.sounds.SoundSource.MASTER, 1f, 1f);
                        }
                    }
                }
                if (cfg.portalTitleAlert) {
                    ChatUtils.showTitle(
                            tr("babyzombieaddons.glacite.portal_title"),
                            cfg.portalAction == GlaciteMineshaftPortalAction.PTME_AND_WARP
                                    ? tr("babyzombieaddons.glacite.portal_sub_auto")
                                    : tr("babyzombieaddons.glacite.portal_sub"),
                            0, 50, 10);
                }
            }
            return true;
        });

        // Mineshaft owner detection
        ClientReceiveMessageEvents.ALLOW_GAME.register((message, overlay) -> {
            if (overlay) return true;
            if (ModConfigManager.get().mining.glaciteTunnels.glaciteMineshaft.portalAction == GlaciteMineshaftPortalAction.NONE) return true;
            if (!isInDwarvenMines()) return true;
            var m = MINESHAFT_ENTER_PAT.matcher(message.getString());
            if (m.find()) {
                var self = Minecraft.getInstance().player;
                String name = ChatUtils.stripRank(ChatUtils.removeEmoji(ChatUtils.stripColor(m.group(1))));
                if (self != null && name.equals(self.getName().getString())) {
                    mineshaftOwner = true;
                    ownServerName = HypixelLocationTracker.getInstance().getServerName();
                    lastOwnerDetectTime = ServerTick.getTime();
                    tryRunEnterActions("owner-detect");
                }
            }
            return true;
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
                    if (ServerTick.getTime() - lastOwnerDetectTime > 10_000) {
                        mineshaftOwner = false;
                        ownServerName = null;
                    }
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
            if (ModConfigManager.get().mining.glaciteTunnels.mineshaftWaypoints
                    && t.isIn("Mineshaft")) {
                var player = Minecraft.getInstance().player;
                if (player != null) {
                    MineshaftCorpseRenderMode mode = ModConfigManager.get().mining.glaciteTunnels.mineshaftCorpseRenderMode;
                    for (var corpse : scanCorpses(player.blockPosition(), 96)) {
                        var stand = corpse.stand;
                        var pos = corpse.pos;
                        if (player.distanceTo(stand) <= 3) {
                            visitedCorpses.add(pos);
                        }
                        if (visitedCorpses.contains(pos)) {
                            if (mode == MineshaftCorpseRenderMode.GLOW) GlowController.setGlow(stand, false);
                            continue;
                        };
                        String name = corpse.type.displayName;
                        var x = corpse.x;
                        var y = corpse.y + 2;
                        var z = corpse.z;
                        var color = mcColorToAwt(name);
                        float r = color.getRed() / 255f;
                        float g = color.getGreen() / 255f;
                        float b = color.getBlue() / 255f;
                        switch (mode) {
                            case FILLED -> WorldRenderUtils.drawFilledBox(ctx,
                                    x - 0.4, y - 2.0, z - 0.4,
                                    x + 0.4, y + 0.2, z + 0.4,
                                    r, g, b, 0.6f * 0.5f, false);
                            case WIREFRAME -> WorldRenderUtils.drawWireframeBox(ctx,
                                            x - 0.4, y - 2.0, z - 0.4,
                                            x + 0.4, y + 0.2, z + 0.4,
                                            r, g, b, 0.6f, false, 4.0f);
                            case GLOW -> GlowController.setGlow(stand, true, color.getRGB());
                        }
                        WorldTextRenderer.renderString(ctx, name, x, y, z, 0xFFFFFF55, 0.05f, true);
                    }
                }
            }

            // Portal timer in Dwarven Mines
            if (t.isIn("Dwarven Mines") && portalTimer > ServerTick.getTime()) {
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

        ClientLevelEvents.AFTER_CLIENT_LEVEL_CHANGE.register((client, world) -> {
            portalTimer = 0; inMineshaft = false;
            waitingPartyTransfer = false;
            if (ServerTick.getTime() - lastOwnerDetectTime > 10_000) {
                mineshaftOwner = false;
                ownServerName = null;
            } else {
                ownServerName = null;
            }
            visitedCorpses.clear();
            if (lapisGateTask != null) {
                Scheduler.cancel(lapisGateTask);
                lapisGateTask = null;
            }
        });
    }

    private static boolean isInMineshaft() {
        return HypixelLocationTracker.getInstance().isIn("Mineshaft");
    }

    private static boolean isInDwarvenMines() {
        return HypixelLocationTracker.getInstance().isIn("Dwarven Mines");
    }

    private static void checkLanternReminder() {
        if (!ModConfigManager.get().mining.glaciteTunnels.lanternReminder) return;
        var player = Minecraft.getInstance().player;
        if (player == null) return;
        var inv = player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            var item = inv.getItem(i);
            if (item.isEmpty()) continue;
            String id = ItemUtils.getSkyblockId(item);
            if (id != null && LANTERN_IDS.contains(id)) {
                String displayName = ChatUtils.toLegacyString(item.getDisplayName());
                ChatUtils.showTitle("",
                        ChatUtils.translate("babyzombieaddons.glacite.lantern_reminder", displayName),
                        0, 50, 10);
                return;
            }
        }
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
