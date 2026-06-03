package top.babyzombie.addons.module.slayer;

import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BeaconBlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.ScoreHolder;
import net.minecraft.world.scores.Scoreboard;
import top.babyzombie.addons.config.ModConfigManager;
import top.babyzombie.addons.util.ChatUtils;
import top.babyzombie.addons.util.HypixelLocationTracker;
import top.babyzombie.addons.util.ServerTick;

import java.util.*;
import java.util.regex.Pattern;

/**
 * Full slayer boss detection with scoreboard parsing, per-boss mechanics tracking.
 * Uses generic entity detection to be compatible with 1.21+ mojang mappings.
 */
public final class BossDetector {
    static LivingEntity currentBoss;
    static String bossType = "";
    static float bossHP;
    static float bossMaxHP;
    static long spawnTime;
    static String hpTag = "";
    static String timeLeft = "";
    static String renderStr = "";

    // Voidgloom
    static long lazerEnd = 0;
    static BeaconState beacon = new BeaconState();

    // Inferno Demonlord
    static final List<LivingEntity> infernoMobs = new ArrayList<>();
    static final InfernoStatus infernoStatus = new InfernoStatus();

    private static final Pattern HP_PATTERN = Pattern.compile(".*[0-9,]+[a-zA-Z]?§c❤.*");

    private static String cachedBossName = "";
    private static int slayerQuestLine = -1;
    private static int serverTickCounter = 0;

    // Inferno attunement cycle
    private static final Map<String, String> NEXT_ATTUNED = Map.of(
        "ASHEN", "§f§lSPIRIT", "SPIRIT", "§e§lAURIC",
        "AURIC", "§b§lCRYSTAL", "CRYSTAL", "§8§lASHEN"
    );

    // Known boss types for validation
    private static final Set<String> KNOWN_BOSSES = Set.of(
        "Revenant Horror", "Tarantula Broodfather", "Sven Packmaster",
        "Voidgloom Seraph", "Inferno Demonlord", "Riftstalker Bloodfiend"
    );

    private BossDetector() {}

    public static void init() {
        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            if (overlay) return;
            String text = ChatUtils.stripColor(message.getString());
            if (text.contains("SLAYER QUEST FAILED")) {
                cachedBossName = "";
                slayerQuestLine = -1;
                reset();
                NoSlayerQuestWarning.onSlayerFail();
            }
        });
    }

    static void tick() {
        var tracker = HypixelLocationTracker.getInstance();
        if (!tracker.isInSkyblock()) { reset(); return; }

        var player = Minecraft.getInstance().player;
        if (player == null) return;

        // Run scoreboard check every 10 ticks
        if (++serverTickCounter % 10 == 0) {
            parseScoreboard(player);
        }

        // If boss died, reset
        if (currentBoss != null && currentBoss.isDeadOrDying()) {
            reset();
            return;
        }
        if (cachedBossName.isEmpty()) return;

        updateHP();
        updateBossInfo(player);
    }

    private static void parseScoreboard(Player player) {
        var level = player.level();
        if (level == null) return;
        Scoreboard sb = level.getScoreboard();
        Objective obj = sb.getDisplayObjective(DisplaySlot.BY_ID.apply(1));
        if (obj == null) { cachedBossName = ""; slayerQuestLine = -1; return; }

        String bossName = "";
        List<String> lines = new ArrayList<>();
        Map<Integer, String> idxToText = new TreeMap<>(Collections.reverseOrder());

        for (ScoreHolder holder : sb.getTrackedPlayers()) {
            if (!sb.listPlayerScores(holder).containsKey(obj)) continue;
            PlayerTeam team = sb.getPlayersTeam(holder.getScoreboardName());
            if (team == null) continue;
            String text = team.getPlayerPrefix().getString() + team.getPlayerSuffix().getString();
            String plain = ChatUtils.removeEmoji(ChatUtils.stripColor(text.toString())).trim();
            int score = sb.listPlayerScores(holder).get(obj);
            idxToText.putIfAbsent(score, plain);
        }

        for (var entry : idxToText.entrySet()) {
            lines.add(entry.getValue());
        }
        Collections.reverse(lines);

        int sqIdx = -1;
        for (int i = 0; i < lines.size(); i++) {
            if (lines.get(i).equals("Slayer Quest")) {
                sqIdx = i;
                if (i + 1 < lines.size()) {
                    String[] parts = lines.get(i + 1).split(" ");
                    if (parts.length >= 2) {
                        bossName = parts[0] + " " + parts[1];
                    }
                }
                break;
            }
        }

        if (sqIdx < 0 || !KNOWN_BOSSES.contains(bossName)) {
            if (cachedBossName.isEmpty() && bossName.isEmpty()) reset();
            if (cachedBossName.isEmpty()) return;
            cachedBossName = "";
            slayerQuestLine = -1;
            reset();
            return;
        }

        cachedBossName = bossName;
        slayerQuestLine = sqIdx;
        bossType = bossName;

        // Check if in boss fight
        if (sqIdx >= 2) {
            String statusLine = lines.get(sqIdx - 2);
            if (statusLine != null && !statusLine.contains("Slay the boss")) {
                reset();
                return;
            }
        }
    }

    private static void updateBossInfo(Player player) {
        var level = player.level();

        // Find the armor stand with player's name
        ArmorStand markerStand = null;
        for (ArmorStand as : level.getEntitiesOfClass(ArmorStand.class,
                new AABB(player.blockPosition()).inflate(32))) {
            String name = as.getName().getString();
            if (name.contains("Spawned") && name.contains(player.getName().getString())) {
                markerStand = as;
                break;
            }
        }

        if (markerStand == null) {
            // Handle Inferno split mobs
            if (cachedBossName.equals("Inferno Demonlord") && currentBoss != null) {
                handleInfernoSplitMobs(level);
            }
            return;
        }

        // Find boss entity near the armor stand - use LivingEntity generically
        var candidates = level.getEntitiesOfClass(LivingEntity.class,
            new AABB(markerStand.blockPosition()).inflate(4));
        for (var candidate : candidates) {
            if (candidate == player) continue;
            if (candidate instanceof ArmorStand) continue;
            if (candidate.getY() < markerStand.getY() && candidate.getMaxHealth() > 100) {
                currentBoss = candidate;
                break;
            }
        }

        // Find nearby armor stands for HP and time info
        for (ArmorStand as : level.getEntitiesOfClass(ArmorStand.class,
                new AABB(markerStand.blockPosition()).inflate(1.5))) {
            if (as == markerStand) continue;
            String name = as.getName().getString();
            String stripped = ChatUtils.stripColor(name);

            if (stripped.contains("❤")) {
                hpTag = name;
            }
            if (stripped.contains(":")) {
                timeLeft = name;
            }
        }

        // Build HP string for display
        if (currentBoss != null) {
            StringBuilder sb = new StringBuilder();
            if (hpTag.contains("ᛤ")) sb.append("§5ᛤ§r ");
            sb.append(healthToString(currentBoss.getHealth(), currentBoss.getMaxHealth()));
            renderStr = buildBossRenderString(sb.toString());
        } else if (!hpTag.isEmpty()) {
            var matcher = HP_PATTERN.matcher(hpTag);
            if (matcher.find()) {
                renderStr = buildBossRenderString(matcher.group(0));
            }
        }

        // Special mechanics per boss type
        switch (cachedBossName) {
            case "Voidgloom Seraph" -> handleVoidgloomMechanics(level);
            case "Inferno Demonlord" -> handleInfernoMechanics(level);
        }
    }

    private static void handleVoidgloomMechanics(Level level) {
        if (currentBoss == null) return;
        var cfg = ModConfigManager.get().slayer;
        if (cfg.endermanSlayerInfo.ordinal() < 2) return;

        // Lazer detection - boss riding an entity (vehicle check) and guardian nearby
        if (currentBoss.isPassenger()) {
            var entities = level.getEntitiesOfClass(LivingEntity.class,
                new AABB(currentBoss.blockPosition()).inflate(3));
            boolean hasGuardian = entities.stream().anyMatch(e ->
                e.getClass().getSimpleName().equals("Guardian"));
            if (hasGuardian) {
                if (lazerEnd < ServerTick.getTime()) {
                    lazerEnd = ServerTick.getTime() + 7200;
                }
            } else {
                lazerEnd = 0;
            }
        } else {
            lazerEnd = 0;
        }

        // Beacon detection
        var heldItem = currentBoss.getMainHandItem();
        boolean holdingBeacon = heldItem.getItem().toString().contains("beacon");
        boolean holdingAir = heldItem.isEmpty();

        switch (beacon.status) {
            case "" -> {
                if (holdingBeacon) beacon.status = "holding";
            }
            case "holding" -> {
                if (holdingAir) {
                    var beaconStands = level.getEntitiesOfClass(ArmorStand.class,
                        new AABB(currentBoss.blockPosition()).inflate(3)).stream()
                        .filter(e -> {
                            var head = e.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.HEAD);
                            return !head.isEmpty() && head.getDisplayName().getString().contains("tile.beacon");
                        }).toList();
                    if (beaconStands.size() == 1) {
                        beacon.entity = beaconStands.get(0);
                        beacon.status = "§ethrown";
                    }
                }
            }
            case "§ethrown" -> {
                if (beacon.entity != null && beacon.entity.isDeadOrDying()) {
                    boolean foundBeacon = false;
                    var searchPos = beacon.entity.blockPosition();
                    for (BlockPos bp : BlockPos.betweenClosed(
                            searchPos.offset(-4, -4, -4),
                            searchPos.offset(4, 4, 4))) {
                        if (level.getBlockEntity(bp) instanceof BeaconBlockEntity be) {
                            beacon.status = "onTheGround";
                            beacon.loc = bp;
                            beacon.time = ServerTick.getTime() + 4800;
                            beacon.entity = null;
                            foundBeacon = true;
                            break;
                        }
                    }
                    if (!foundBeacon) {
                        beacon.reset();
                    }
                }
            }
            case "onTheGround" -> {
                if (beacon.time < ServerTick.getTime()
                    || level.getBlockState(beacon.loc).getBlock() == Blocks.AIR) {
                    beacon.reset();
                }
            }
        }
    }

    private static void handleInfernoMechanics(Level level) {
        infernoMobs.removeIf(e -> e == null || e.isDeadOrDying());
    }

    private static void handleInfernoSplitMobs(Level level) {
        if (currentBoss == null) return;
        infernoMobs.removeIf(e -> e == null || e.isDeadOrDying());

        // Find any nearby LivingEntity that could be a split mob
        var nearby = level.getEntitiesOfClass(LivingEntity.class,
            new AABB(currentBoss.blockPosition()).inflate(3.5)).stream()
            .filter(e -> e != currentBoss && !(e instanceof ArmorStand) && !(e instanceof Player)
                && e.getY() > currentBoss.getY() - 0.5)
            .toList();

        for (var mob : nearby) {
            if (infernoMobs.size() >= 2) break;
            if (infernoMobs.contains(mob)) continue;
            infernoMobs.add(mob);
        }
    }

    private static String buildBossRenderString(String hpText) {
        var cfg = ModConfigManager.get().slayer;
        StringBuilder sb = new StringBuilder();

        switch (cachedBossName) {
            case "Revenant Horror" -> {
                if (cfg.zombieSlayerInfo.ordinal() == 0) return "";
                sb.append("§bRevenant Horror ").append(hpText);
                if (cfg.zombieSlayerInfo.ordinal() >= 2 && !timeLeft.isEmpty()) {
                    String[] parts = timeLeft.split(" ");
                    String status = String.join(" ", Arrays.copyOf(parts, Math.max(0, parts.length - 1)));
                    if (!status.isEmpty()) sb.append("\n").append(status);
                }
            }
            case "Tarantula Broodfather" -> {
                if (cfg.spiderSlayerInfo.ordinal() == 0) return "";
                sb.append("§4Tarantula Broodfather ").append(hpText);
                if (cfg.spiderSlayerInfo.ordinal() >= 2 && !timeLeft.isEmpty()) {
                    String[] parts = timeLeft.split(" ");
                    String status = String.join(" ", Arrays.copyOf(parts, Math.max(0, parts.length - 1)));
                    if (!status.isEmpty()) sb.append("\n").append(status);
                }
            }
            case "Sven Packmaster" -> {
                if (cfg.wolfSlayerInfo.ordinal() == 0) return "";
                sb.append("§fSven Packmaster ").append(hpText);
                if (cfg.wolfSlayerInfo.ordinal() >= 2 && !timeLeft.isEmpty()) {
                    String[] parts = timeLeft.split(" ");
                    String status = String.join(" ", Arrays.copyOf(parts, Math.max(0, parts.length - 1)));
                    if (!status.isEmpty()) sb.append("\n").append(status);
                }
            }
            case "Voidgloom Seraph" -> {
                if (cfg.endermanSlayerInfo.ordinal() == 0) return "";
                sb.append("§5Voidgloom Seraph ").append(hpText);
                if (hpTag.contains("Hit") && hpTag.contains("Hits")) {
                    String hits = ChatUtils.stripColor(hpTag).replaceAll("[^0-9]", "");
                    if (!hits.isEmpty()) sb.append("\n§fHit Phase: ").append(hits).append(" Hits");
                }
                if (cfg.endermanSlayerInfo.ordinal() >= 2) {
                    if (lazerEnd > ServerTick.getTime()) {
                        sb.append("\n§aLazer: §e").append(formatTime(lazerEnd - ServerTick.getTime()));
                    }
                    if (!beacon.status.isEmpty()) {
                        sb.append("\n§bBeacon: ");
                        if (beacon.status.equals("onTheGround")) {
                            sb.append("§c").append(formatTime(beacon.time - ServerTick.getTime()));
                        } else {
                            sb.append(beacon.status);
                        }
                    }
                }
            }
            case "Inferno Demonlord" -> {
                if (cfg.blazeSlayerInfo.ordinal() == 0) return "";
                String[] hpParts = hpText.split(" ");
                sb.append("§bInferno Demonlord ").append(hpParts.length > 0 ? hpParts[hpParts.length - 1] : hpText);
                if (cfg.blazeSlayerInfo.ordinal() >= 2 && !timeLeft.isEmpty()) {
                    buildInfernoStatus(sb);
                }
            }
            case "Riftstalker Bloodfiend" -> {
                if (cfg.vampireSlayerInfo.ordinal() == 0) return "";
                sb.append("§4Bloodfiend ");
                if (hpText.contains("҉")) {
                    String[] parts = hpText.split(" ");
                    if (parts.length >= 2)
                        sb.append(parts[parts.length - 2]).append(" ").append(parts[parts.length - 1]);
                } else {
                    int lastSpace = hpText.lastIndexOf(' ');
                    sb.append(lastSpace >= 0 ? hpText.substring(lastSpace + 1) : hpText);
                }
                if (cfg.vampireSlayerInfo.ordinal() >= 2 && !timeLeft.isEmpty()) {
                    String[] parts = timeLeft.split(" ");
                    sb.append("\n");
                    for (int i = 1; i < parts.length; i++) {
                        String s = ChatUtils.stripColor(parts[i]);
                        sb.append(parts[i]);
                        if (s.equals("KILLER") && i + 1 < parts.length) {
                            sb.append(" ").append(parts[++i]);
                        }
                        if (i + 1 < parts.length) {
                            sb.append(" ").append(parts[++i]);
                        }
                        if (i + 1 < parts.length) sb.append("\n");
                    }
                }
            }
        }
        return sb.toString();
    }

    private static void buildInfernoStatus(StringBuilder sb) {
        String[] parts = timeLeft.split(" ");
        if (parts.length <= 1) return;
        List<String> statusParts = new ArrayList<>(Arrays.asList(parts));
        statusParts.remove(statusParts.size() - 1);

        String currentAttune = ChatUtils.stripColor(statusParts.get(0));
        statusParts.add("§7→§r");
        statusParts.add(NEXT_ATTUNED.getOrDefault(currentAttune, ""));

        if (ModConfigManager.get().slayer.blazeSlayerInfo.ordinal() >= 3) {
            infernoStatus.update(currentAttune, ChatUtils.stripColor(statusParts.get(1)));
        }

        sb.append("\n").append(String.join(" ", statusParts));
    }

    static void updateHP() {
        if (currentBoss != null && !currentBoss.isDeadOrDying()) {
            bossHP = currentBoss.getHealth();
            bossMaxHP = currentBoss.getMaxHealth();
        }
    }

    static void reset() {
        currentBoss = null;
        bossType = "";
        bossHP = 0;
        bossMaxHP = 0;
        hpTag = "";
        timeLeft = "";
        renderStr = "";
        lazerEnd = 0;
        beacon.reset();
        infernoMobs.clear();
        infernoStatus.reset();
    }

    private static String healthToString(float hp, float maxHp) {
        String color = maxHp > 0 && hp / maxHp > 0.5f ? "a" : "e";
        if (maxHp < 10000) return "§" + color + Math.round(hp) + "§c❤";
        double val = hp;
        for (int i = 0; i < 4; i++) {
            if (val >= 1000) { val /= 1000; continue; }
            String suffix = switch (i) {
                case 0 -> "§c❤";
                case 1 -> "k§c❤";
                case 2 -> "M§c❤";
                case 3 -> "B§c❤";
                default -> "T§c❤";
            };
            return "§" + color + String.format("%.2f", val) + suffix;
        }
        return "§" + color + String.format("%.2f", val) + "T§c❤";
    }

    static String formatTime(long ms) {
        long s = ms / 1000, m = (ms % 1000) / 10;
        return String.format("%d.%02ds", s, m);
    }

    // ---- Inner classes ----

    static class BeaconState {
        String status = "";
        ArmorStand entity;
        net.minecraft.core.BlockPos loc;
        long time;

        void reset() { status = ""; entity = null; loc = null; time = 0; }
    }

    static class InfernoStatus {
        String status = "";
        int counts;
        boolean hit;
        final List<Long> repeat = new ArrayList<>();

        void update(String currentAttune, String countStr) {
            int newCounts;
            try { newCounts = Integer.parseInt(countStr.replaceAll("[^0-9]", "")); }
            catch (NumberFormatException e) { newCounts = 0; }

            if (hit) {
                if (!status.equals(currentAttune) || counts > newCounts) {
                    repeat.add(System.currentTimeMillis());
                    hit = false;
                }
            }
            status = currentAttune;
            counts = newCounts;
            repeat.removeIf(e -> System.currentTimeMillis() - e > 6000);
        }

        void reset() { status = ""; counts = 0; hit = false; repeat.clear(); }
    }
}
