package top.babyzombie.addons.module.slayer;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BeaconBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.ScoreHolder;
import net.minecraft.world.scores.Scoreboard;
import top.babyzombie.addons.config.ModConfig;
import top.babyzombie.addons.config.ModConfigManager;
import top.babyzombie.addons.util.ChatUtils;
import top.babyzombie.addons.util.ItemUtils;
import top.babyzombie.addons.util.ServerTick;
import top.babyzombie.addons.util.tracker.HypixelLocationTracker;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class SlayerBossDetector {

    private static final Pattern HEALTH_PATTERN =
            Pattern.compile("((§[ae]\\d+[a-zA-Z])§f/§a\\d+[a-zA-Z]§c❤)");

    // Blaze attunement
    static final Map<String, String> NEXT_BLAZE_ATTUNED = Map.of(
            "ASHEN", "§f§lSPIRIT",
            "SPIRIT", "§e§lAURIC",
            "AURIC", "§b§lCRYSTAL",
            "CRYSTAL", "§8§lASHEN"
    );

    // Boss definitions
    static class BossDef {
        final EntityType<?> type;
        final double range, wX, wZ, h;
        final String name;
        BossDef(EntityType<?> type, double range, double wX, double wZ, double h, String name) {
            this.type = type; this.range = range; this.wX = wX; this.wZ = wZ; this.h = h; this.name = name;
        }
    }
    static final Map<String, BossDef> BOSS_DEFS = new LinkedHashMap<>();
    static {
        BOSS_DEFS.put("Revenant Horror",         new BossDef(EntityTypes.ZOMBIE,  0.7, 1.0, 1.0, 2.0, "Revenant Horror"));
        BOSS_DEFS.put("Tarantula Broodfather",   new BossDef(EntityTypes.SPIDER,  1.2, 1.8, 1.8, 0.6, "Tarantula Broodfather"));
        BOSS_DEFS.put("Sven Packmaster",         new BossDef(EntityTypes.WOLF,    0.5, 1.0, 1.0, 1.0, "Sven Packmaster"));
        BOSS_DEFS.put("Voidgloom Seraph",        new BossDef(EntityTypes.ENDERMAN,0.5, 1.0, 1.0, 3.0, "Voidgloom Seraph"));
        BOSS_DEFS.put("Inferno Demonlord",       new BossDef(EntityTypes.BLAZE,   0.5, 1.0, 1.0, 2.0, "Inferno Demonlord"));
        BOSS_DEFS.put("Riftstalker Bloodfiend",  new BossDef(EntityTypes.PLAYER,  0.5, 1.0, 1.0, 2.0, "Bloodfiend"));
    }

    // State
    static String slayerType = "";
    static Entity bossEntity;
    static String hp = "";
    static String hpTag = "";
    static String timeLeft = "";
    static String renderStr = "";

    // Voidgloom
    static final VoidgloomState voidgloom = new VoidgloomState();
    static class VoidgloomState {
        long lazer;
        String beaconStatus = "";
        Entity beaconEntity;
        BlockPos beaconLoc;
        long beaconTime;
        void reset() { lazer = 0; beaconStatus = ""; beaconEntity = null; beaconLoc = null; beaconTime = 0; }
    }

    // Inferno
    static final List<Entity> infernoMinions = new ArrayList<>();
    static final InfernoStatus infernoStatus = new InfernoStatus();
    static class InfernoStatus {
        String status = "";
        int counts;
        boolean hit;
        final List<Long> repeat = new ArrayList<>();
        void reset() { status = ""; counts = 0; hit = false; repeat.clear(); }
    }

    private SlayerBossDetector() {}

    public static void init() {
        ClientTickEvents.END_CLIENT_TICK.register(SlayerBossDetector::tick);
    }

    // ---- Main tick ----

    private static void tick(Minecraft client) {
        var tracker = HypixelLocationTracker.getInstance();
        if (!tracker.isInSkyblock()) {
            reset();
            return;
        }

        ClientLevel level = client.level;
        if (level == null || client.player == null) return;

        // Clean up dead boss
        if (bossEntity != null && !bossEntity.isAlive()) reset();

        // Read scoreboard
        Scoreboard sb = level.getScoreboard();
        Objective obj = sb.getDisplayObjective(DisplaySlot.BY_ID.apply(1));
        if (obj == null) { reset(); return; }

        Integer slayerQuestScore = null;
        Map<Integer, String> scoreLines = new LinkedHashMap<>();
        for (ScoreHolder holder : sb.getTrackedPlayers()) {
            if (!sb.listPlayerScores(holder).containsKey(obj)) continue;
            PlayerTeam team = sb.getPlayersTeam(holder.getScoreboardName());
            if (team == null) continue;
            String text = team.getPlayerPrefix().getString() + team.getPlayerSuffix().getString();
            String plain = ChatUtils.removeEmoji(ChatUtils.stripColor(text)).trim();
            int score = sb.listPlayerScores(holder).getInt(obj);
            scoreLines.put(score, text);

            if ("Slayer Quest".equals(plain)) {
                slayerQuestScore = score;
            }
        }

        if (slayerQuestScore == null) { reset(); return; }

        // Boss name: next line below "Slayer Quest" (lower score)
        String bossLine = scoreLines.get(slayerQuestScore - 1);
        if (bossLine == null) { reset(); return; }

        String bossName = ChatUtils.removeEmoji(ChatUtils.stripColor(bossLine)).trim();
        String[] nameParts = bossName.split(" ");
        String bossTier = nameParts.length > 2 ? nameParts[nameParts.length - 1] : "";
        if (nameParts.length >= 2) {
            bossName = nameParts[0] + " " + nameParts[1];
        }
        if (!BOSS_DEFS.containsKey(bossName)) { reset(); return; }
        slayerType = bossName;
        var def = BOSS_DEFS.get(bossName);

        // "Slay the boss" / combat XP line: two lines below "Slayer Quest"
        String slayLine = scoreLines.get(slayerQuestScore - 2);
        if (slayLine == null || !ChatUtils.stripColor(slayLine).contains("Slay the boss")) {
            if (!"Inferno Demonlord".equals(bossName) || bossEntity == null) { reset(); return; }
            trackInfernoSplit(level, def);
            return;
        }

        // Find armor stand with player name + "Spawned"
        ArmorStand spawnArmorStand = null;
        String playerName = client.player.getName().getString();
        for (Entity e : level.entitiesForRendering()) {
            if (e instanceof ArmorStand as) {
                String name = as.getName().getString();
                if (name.contains("Spawned") && name.contains(playerName)) {
                    spawnArmorStand = as;
                    break;
                }
            }
        }
        if (spawnArmorStand == null) { reset(); return; }

        // Find boss entity near the armor stand.
        // Keep the existing boss as a fallback so the box doesn't flicker
        // when the boss briefly leaves the detection window during jumps/knockbacks.
        Entity best = bossEntity;
        double bestDist = Double.MAX_VALUE;

        // Score current boss
        if (best != null) {
            if (!best.isAlive() || best.getType() != def.type
                    || ("Riftstalker Bloodfiend".equals(bossName)
                        && !"Bloodfiend ".equals(best.getName().getString()))) {
                best = null;
            } else {
                double dx = best.getX() - spawnArmorStand.getX();
                double dy = (best.getY() + def.h) - spawnArmorStand.getY();
                double dz = best.getZ() - spawnArmorStand.getZ();
                bestDist = Math.sqrt(dx * dx + dy * dy + dz * dz);
            }
        }

        // Search for a closer entity
        for (Entity e : level.entitiesForRendering()) {
            if (e == best) continue;
            if (e.getType() != def.type) continue;
            if (!e.isAlive()) continue;
            if ("Riftstalker Bloodfiend".equals(bossName) && !"Bloodfiend ".equals(e.getName().getString())) continue;

            double dx = e.getX() - spawnArmorStand.getX();
            double dy = (e.getY() + def.h) - spawnArmorStand.getY();
            double dz = e.getZ() - spawnArmorStand.getZ();
            double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
            if (dist < def.range && e.getY() < spawnArmorStand.getY() && dist < bestDist) {
                best = e;
                bestDist = dist;
            }
        }

        bossEntity = best;

        // Find nearby armor stands for HP and time info
        if (bossEntity != null) {
            List<ArmorStand> nearbyStands = new ArrayList<>();
            for (Entity e : level.entitiesForRendering()) {
                if (e instanceof ArmorStand as && as != spawnArmorStand
                        && as.distanceTo(spawnArmorStand) < 1
                        && Math.abs(as.getX() - spawnArmorStand.getX()) < 0.5
                        && Math.abs(as.getZ() - spawnArmorStand.getZ()) < 0.5) {
                    nearbyStands.add(as);
                }
            }

            // HP tag (T5 uses different name, e.g. Atoned Horror)
            String searchName = def.name;
            if ("Revenant Horror".equals(bossName) && "V".equals(bossTier)) {
                searchName = "Atoned Horror";
            }
            hpTag = null;
            for (ArmorStand as : nearbyStands) {
                String nm = ChatUtils.toLegacyString(as.getName());
                if (ChatUtils.stripColor(nm).contains(searchName)) {
                    hpTag = nm;
                    break;
                }
            }
            hp = (hpTag != null && hpTag.contains("ᛤ") ? "§5ᛤ§r " : "")
                    + (bossEntity != null ? healthToString((LivingEntity) bossEntity) : extractHpFromTag(hpTag));

            // Time left
            timeLeft = "";
            for (ArmorStand as : nearbyStands) {
                String nm = ChatUtils.toLegacyString(as.getName());
                if (nm.contains(":")) {
                    timeLeft = nm;
                    break;
                }
            }

            // Voidgloom special handling
            if ("Voidgloom Seraph".equals(bossName)) {
                trackVoidgloom(level);
            }
        }

        // Build render string
        buildRenderStr();
    }

    // ---- Inferno split tracking ----

    private static void trackInfernoSplit(ClientLevel level, BossDef def) {
        if (bossEntity == null) return;

        infernoMinions.removeIf(e -> e == null || !e.isAlive());

        for (Entity e : level.entitiesForRendering()) {
            if (infernoMinions.size() >= 2) break;
            if (e.getType() == EntityTypes.ZOMBIFIED_PIGLIN
                    && e.distanceTo(bossEntity) < 3.5
                    && e.getY() > bossEntity.getY() - 0.5
                    && infernoMinions.stream().noneMatch(m -> m.getType() == EntityTypes.ZOMBIFIED_PIGLIN)) {
                infernoMinions.add(e);
            }
        }
        for (Entity e : level.entitiesForRendering()) {
            if (infernoMinions.size() >= 2) break;
            if (e.getType() == EntityTypes.SKELETON
                    && e instanceof LivingEntity le && le.isBaby()
                    && e.distanceTo(bossEntity) < 3.5
                    && e.getY() > bossEntity.getY() - 0.5
                    && infernoMinions.stream().noneMatch(m -> m.getType() == EntityTypes.SKELETON)) {
                infernoMinions.add(e);
            }
        }

        var cfg = ModConfigManager.get().slayer;
        if (cfg.blazeSlayerInfo == ModConfig.SlayerBossInfoMode.OFF) return;

        List<String> bossStatus = new ArrayList<>();
        String[] parts = timeLeft != null ? timeLeft.split(" ") : new String[0];
        if (parts.length > 1) {
            for (int i = 0; i < parts.length - 1; i++) bossStatus.add(parts[i]);
            bossStatus.add("§7→§r");
            bossStatus.add(NEXT_BLAZE_ATTUNED.getOrDefault(ChatUtils.stripColor(parts[0]), ""));
        }

        String currentlyShield = "";
        List<String> infernoMobsStr = new ArrayList<>();
        for (Entity e : infernoMinions) {
            String marker = e.getType() == EntityTypes.SKELETON ? "ⓆⓊⒶⓏⒾⒾ" : "ⓉⓎⓅⒽⓄⒺⓊⓈ";

            for (Entity a : level.entitiesForRendering()) {
                if (!(a instanceof ArmorStand as)) continue;
                if (as.distanceTo(e) >= 3) continue;
                String nm = ChatUtils.toLegacyString(as.getName());
                if (nm.contains("❤") && nm.contains(marker)) {
                    String[] hpParts = nm.split(" ");
                    String separator = e.getType() == EntityTypes.SKELETON ? "     " : " ";
                    infernoMobsStr.add('\n' + String.join(separator,
                            java.util.Arrays.copyOfRange(hpParts, 1, hpParts.length)));
                }
                if (nm.matches(".*[0-9]{1,2}:[0-9]{1,2}.*")) {
                    String[] tParts = nm.split(" ");
                    List<String> timeList = new ArrayList<>(java.util.Arrays.asList(tParts));
                    if (!timeList.isEmpty()) timeList.remove(timeList.size() - 1);
                    infernoMobsStr.add(String.join(" ", timeList));
                    if (tParts.length > 0 && !"IMMUNE".equals(ChatUtils.stripColor(tParts[0]))) {
                        currentlyShield = ChatUtils.stripColor(tParts[0]);
                    }
                }
            }
        }

        var player = Minecraft.getInstance().player;
        if (player != null) {
            ItemStack held = player.getItemInHand(InteractionHand.MAIN_HAND);
            if (isBlazeDagger(held)) {
                String daggerAttune = getBlazeDaggerAttunement(held);
                if (daggerAttune != null) {
                    bossStatus.add((daggerAttune.equals(currentlyShield) ? "\n§a" : "\n§e")
                            + "Holding: " + daggerAttune);
                }
            } else if (!held.isEmpty()) {
                bossStatus.add("\n§eHolding: " + held.getDisplayName().getString());
            }
        }

        renderStr = "§eInferno Demonlord " + (hp.contains(" ") ? hp.split(" ")[hp.split(" ").length - 1] : hp)
                + '\n' + String.join(" ", bossStatus) + String.join(" ", infernoMobsStr);
    }

    // ---- Voidgloom special handling ----

    private static void trackVoidgloom(ClientLevel level) {
        if (bossEntity instanceof EnderMan enderman) {
            // Lazer detection
            if (enderman.getVehicle() != null) {
                boolean hasGuardian = false;
                for (Entity e : level.entitiesForRendering()) {
                    if (e.getType() == EntityTypes.GUARDIAN && e.distanceTo(enderman) < 3) {
                        hasGuardian = true;
                        break;
                    }
                }
                if (hasGuardian) {
                    if (voidgloom.lazer < ServerTick.getTime()) {
                        voidgloom.lazer = ServerTick.getTime() + 6200;
                    }
                }
            } else {
                voidgloom.lazer = 0;
            }

            // Beacon tracking
            BlockState carriedBlock = enderman.getCarriedBlock();
            if (carriedBlock != null && carriedBlock.is(Blocks.BEACON)) {
                voidgloom.beaconStatus = "holding";
            } else if ("holding".equals(voidgloom.beaconStatus)) {
                ArmorStand beaconStand = null;
                for (Entity e : level.entitiesForRendering()) {
                    if (e instanceof ArmorStand as && as.distanceTo(enderman) < 3) {
                        ItemStack headItem = as.getItemBySlot(EquipmentSlot.HEAD);
                        if (headItem.getItem() == Items.BEACON) {
                            beaconStand = as;
                            break;
                        }
                    }
                }
                if (beaconStand != null) {
                    voidgloom.beaconEntity = beaconStand;
                    voidgloom.beaconStatus = "§ethrown";
                }
            } else if ("§ethrown".equals(voidgloom.beaconStatus) && voidgloom.beaconEntity != null
                    && !voidgloom.beaconEntity.isAlive()) {
                BlockPos standPos = voidgloom.beaconEntity.blockPosition();
                boolean foundBeacon = false;
                for (int dx = -3; dx <= 3 && !foundBeacon; dx++) {
                    for (int dz = -3; dz <= 3 && !foundBeacon; dz++) {
                        var be = level.getBlockEntity(standPos.offset(dx, 0, dz));
                        if (be instanceof BeaconBlockEntity) {
                            voidgloom.beaconStatus = "onTheGround";
                            voidgloom.beaconLoc = be.getBlockPos();
                            voidgloom.beaconEntity = null;
                            voidgloom.beaconTime = ServerTick.getTime() + 4800;
                            foundBeacon = true;
                        }
                    }
                }
                if (!foundBeacon) {
                    voidgloom.beaconStatus = "";
                    voidgloom.beaconEntity = null;
                }
            } else if ("onTheGround".equals(voidgloom.beaconStatus)) {
                if (voidgloom.beaconTime < ServerTick.getTime()
                        || (voidgloom.beaconLoc != null && level.getBlockState(voidgloom.beaconLoc).isAir())) {
                    voidgloom.reset();
                }
            }
        }
    }

    // ---- Build render string per boss ----

    private static void buildRenderStr() {
        var cfg = ModConfigManager.get().slayer;
        if (bossEntity == null && hpTag == null) { renderStr = ""; return; }

        switch (slayerType) {
            case "Revenant Horror" -> {
                if (cfg.zombieSlayerInfo == ModConfig.SlayerBossInfoMode.OFF) { renderStr = ""; return; }
                String status = extractBossStatus();
                renderStr = "§bRevenant Horror " + hp
                        + (cfg.zombieSlayerInfo == ModConfig.SlayerBossInfoMode.FULL && !status.isEmpty()
                        ? '\n' + status : "");
            }
            case "Tarantula Broodfather" -> {
                if (cfg.spiderSlayerInfo == ModConfig.SlayerBossInfoMode.OFF) { renderStr = ""; return; }
                String status = extractBossStatus();
                renderStr = "§4Tarantula Broodfather " + hp
                        + (cfg.spiderSlayerInfo == ModConfig.SlayerBossInfoMode.FULL && !status.isEmpty()
                        ? '\n' + status : "");
            }
            case "Sven Packmaster" -> {
                if (cfg.wolfSlayerInfo == ModConfig.SlayerBossInfoMode.OFF) { renderStr = ""; return; }
                String status = extractBossStatus();
                renderStr = "§fSven Packmaster " + hp
                        + (cfg.wolfSlayerInfo == ModConfig.SlayerBossInfoMode.FULL && !status.isEmpty()
                        ? '\n' + status : "");
            }
            case "Voidgloom Seraph" -> {
                if (cfg.endermanSlayerInfo == ModConfig.SlayerBossInfoMode.OFF) { renderStr = ""; return; }
                String hitInfo = (hpTag != null && hpTag.contains("Hit"))
                        ? "\n§fHit Phase: " + ChatUtils.stripColor(hpTag).replaceAll("[^0-9]", "") + " Hits"
                        : "";
                renderStr = "§5Voidgloom Seraph " + hp + hitInfo;
                if (cfg.endermanSlayerInfo == ModConfig.SlayerBossInfoMode.FULL) {
                    if (voidgloom.lazer > 0) {
                        long rem = voidgloom.lazer - ServerTick.getTime();
                        if (rem > 0) renderStr += "\n§aLazer: §e" + ChatUtils.formatTime(rem);
                    }
                    if (!voidgloom.beaconStatus.isEmpty()) {
                        renderStr += "\n§bBeacon: "
                                + ("onTheGround".equals(voidgloom.beaconStatus)
                                ? "§c" + ChatUtils.formatTime(voidgloom.beaconTime - ServerTick.getTime())
                                : voidgloom.beaconStatus);
                    }
                }
            }
            case "Inferno Demonlord" -> {
                if (cfg.blazeSlayerInfo == ModConfig.SlayerBossInfoMode.OFF) { renderStr = ""; return; }
                String[] parts = timeLeft != null ? timeLeft.split(" ") : new String[0];
                List<String> bossStatus = new ArrayList<>();
                if (parts.length > 1) {
                    for (int i = 0; i < parts.length - 1; i++) bossStatus.add(parts[i]);
                    bossStatus.add("§7→§r");
                    bossStatus.add(NEXT_BLAZE_ATTUNED.getOrDefault(
                            ChatUtils.stripColor(parts[0]), ""));
                }
                renderStr = "§bInferno Demonlord " + (hp.contains(" ") ? hp.split(" ")[hp.split(" ").length - 1] : hp)
                        + (cfg.blazeSlayerInfo == ModConfig.SlayerBossInfoMode.FULL && parts.length > 1
                        ? '\n' + String.join(" ", bossStatus) : "");
            }
            case "Riftstalker Bloodfiend" -> {
                if (cfg.vampireSlayerInfo == ModConfig.SlayerBossInfoMode.OFF) { renderStr = ""; return; }
                StringBuilder vampireStatus = new StringBuilder();
                String[] parts = timeLeft != null ? timeLeft.split(" ") : new String[0];
                if (parts.length > 1) {
                    String[] rest = java.util.Arrays.copyOfRange(parts, 1, parts.length);
                    for (int i = 0; i < rest.length; i++) {
                        vampireStatus.append(rest[i]);
                        if ("KILLER".equals(ChatUtils.stripColor(rest[i])) && i + 1 < rest.length) {
                            vampireStatus.append(" ").append(rest[++i]);
                        }
                        if (i + 1 < rest.length) {
                            vampireStatus.append(' ').append(rest[++i]).append('\n');
                        } else {
                            vampireStatus.append('\n');
                        }
                    }
                }
                String hpDisplay = hp;
                if (hp.contains("҉")) {
                    String[] hpSplit = hp.split(" ");
                    hpDisplay = String.join(" ", java.util.Arrays.copyOfRange(hpSplit,
                            Math.max(0, hpSplit.length - 2), hpSplit.length));
                } else if (hp.contains(" ")) {
                    hpDisplay = hp.split(" ")[hp.split(" ").length - 1];
                }
                renderStr = "§4Bloodfiend " + hpDisplay
                        + (cfg.vampireSlayerInfo == ModConfig.SlayerBossInfoMode.FULL
                        ? '\n' + vampireStatus.toString().stripTrailing() : "");
            }
        }
    }

    // ---- Helpers ----

    static void reset() {
        slayerType = "";
        bossEntity = null;
        hp = "";
        hpTag = "";
        timeLeft = "";
        renderStr = "";
        voidgloom.reset();
        infernoMinions.clear();
        infernoStatus.reset();
    }

    static String extractBossStatus() {
        if (timeLeft == null || timeLeft.isEmpty()) return "";
        String[] parts = timeLeft.split(" ");
        if (parts.length <= 1) return "";
        List<String> statusParts = new ArrayList<>(java.util.Arrays.asList(parts));
        statusParts.remove(statusParts.size() - 1);
        return String.join(" ", statusParts);
    }

    static String extractHpFromTag(String tag) {
        if (tag == null) return "§c❤";
        Matcher m = HEALTH_PATTERN.matcher(tag);
        if (m.find() && m.group(2) != null) {
            return m.group(2) + "§c❤";
        }
        return "§c❤";
    }

    static String healthToString(LivingEntity entity) {
        float hpVal = entity.getHealth();
        float maxHp = entity.getMaxHealth();
        if (maxHp < 10000) {
            return "§" + (hpVal / maxHp > 0.5f ? "a" : "e") + String.format("%,d", Math.round(hpVal)) + "§c❤";
        }
        double displayHp = hpVal;
        String[] suffix = {"", "k", "M", "B"};
        for (int tier = 0; tier < 4; tier++) {
            if (displayHp < 1000) {
                return "§" + (hpVal / maxHp > 0.5f ? "a" : "e")
                        + String.format("%,.2f", displayHp) + suffix[tier] + "§c❤";
            }
            displayHp /= 1000;
        }
        return "§" + (hpVal / maxHp > 0.5f ? "a" : "e") + String.format("%,.2f", displayHp) + "T§c❤";
    }

    static boolean isBlazeDagger(ItemStack item) {
        String id = ItemUtils.getSkyblockId(item);
        if (id == null) return false;
        return switch (id) {
            case "FIREDUST_DAGGER", "MAWDUST_DAGGER", "BURSTFIRE_DAGGER",
                 "BURSTMAW_DAGGER", "HEARTFIRE_DAGGER", "HEARTMAW_DAGGER" -> true;
            default -> false;
        };
    }

    static String getBlazeDaggerAttunement(ItemStack item) {
        String id = ItemUtils.getSkyblockId(item);
        if (id == null) return null;
        return switch (id) {
            case "FIREDUST_DAGGER", "MAWDUST_DAGGER" -> "§8§lASHEN";
            case "BURSTFIRE_DAGGER", "BURSTMAW_DAGGER" -> "§f§lSPIRIT";
            case "HEARTFIRE_DAGGER", "HEARTMAW_DAGGER" -> "§e§lAURIC";
            default -> null;
        };
    }

}
