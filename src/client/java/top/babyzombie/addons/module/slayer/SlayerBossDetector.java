package top.babyzombie.addons.module.slayer;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientEntityEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
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

public final class SlayerBossDetector {

    // 火焰猎手元素附着循环顺序
    static final Map<String, String> NEXT_BLAZE_ATTUNED = Map.of(
            "ASHEN", "§f§lSPIRIT",
            "SPIRIT", "§e§lAURIC",
            "AURIC", "§b§lCRYSTAL",
            "CRYSTAL", "§8§lASHEN"
    );

    // boss 定义
    record BossDef(EntityType<?> type, double range, double wX, double wZ, double h, String name) {}
    static final Map<String, BossDef> BOSS_DEFS = new LinkedHashMap<>();
    static {
        BOSS_DEFS.put("Revenant Horror",         new BossDef(EntityType.ZOMBIE,  0.7, 1.0, 1.0, 2.0, "Revenant Horror"));
        BOSS_DEFS.put("Tarantula Broodfather",   new BossDef(EntityType.SPIDER,  1.2, 1.8, 1.8, 0.6, "Tarantula Broodfather"));
        BOSS_DEFS.put("Sven Packmaster",         new BossDef(EntityType.WOLF,    0.5, 1.0, 1.0, 1.0, "Sven Packmaster"));
        BOSS_DEFS.put("Voidgloom Seraph",        new BossDef(EntityType.ENDERMAN,0.5, 1.0, 1.0, 3.0, "Voidgloom Seraph"));
        BOSS_DEFS.put("Inferno Demonlord",       new BossDef(EntityType.BLAZE,   0.5, 1.0, 1.0, 2.0, "Inferno Demonlord"));
        BOSS_DEFS.put("Riftstalker Bloodfiend",  new BossDef(EntityType.PLAYER,  0.5, 1.0, 1.0, 2.0, "Bloodfiend"));
    }

    // 运行状态
    static String slayerType = "";
    static String bossTier = "";
    static Entity bossEntity;
    static String hp = "";
    static String hpTag = "";
    static String timeLeft = "";
    static String renderStr = "";
    static boolean spiderPhase2 = false;

    // ---- boss 归属判定 ----
    // 0.27.2 把 "Spawned by: <玩家>" 那行铭牌换成了通用的 "SLAYER BOSS",boss 头顶
    // 再也读不到召唤者,所以改用召唤时序判定:侧边栏切成 "Slay the boss!" 时服务端
    // 已经生成 boss,而实体本身要再过几秒才到客户端 —— 这个窗口里第一个出现、且带
    // 对应 boss 名字铭牌的实体就是我们的。
    private static final Map<Integer, Long> bossSpawns = new LinkedHashMap<>();
    /** 侧边栏翻转后多久内新生成的 boss 算我们的 */
    private static final long CLAIM_WINDOW_MS = 6000;
    /** 容忍实体包比侧边栏更新包先到 */
    private static final long CLAIM_LOOKBACK_MS = 500;
    /** 超过这个时间只认唯一候选或玩家主动命中 */
    private static final long CLAIM_FALLBACK_MS = 36000;
    /** 生成候选缓冲的保留时长 */
    private static final long SPAWN_BUFFER_MS = 15000;
    private static boolean bossPhase = false;
    private static long bossPhaseSince = 0;

    // 末影人猎手
    static final VoidgloomState voidgloom = new VoidgloomState();
    static class VoidgloomState {
        long lazer;
        String beaconStatus = "";
        Entity beaconEntity;
        BlockPos beaconLoc;
        long beaconTime;
        void reset() { lazer = 0; beaconStatus = ""; beaconEntity = null; beaconLoc = null; beaconTime = 0; }
    }

    // 火焰猎手分身
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
        ClientEntityEvents.ENTITY_LOAD.register((entity, level) -> onEntityLoad(entity));
        AttackEntityCallback.EVENT.register((player, level, hand, entity, hitResult) -> {
            onAttackEntity(entity);
            return InteractionResult.PASS;
        });
    }

    // ---- 主 tick ----

    private static void tick(Minecraft client) {
        var tracker = HypixelLocationTracker.getInstance();
        if (!tracker.isInSkyblock()) {
            reset();
            return;
        }

        ClientLevel level = client.level;
        if (level == null || client.player == null) { reset(); return; }

        // 清掉已经死掉的 boss
        if (bossEntity != null && !bossEntity.isAlive()) reset();

        // 读计分板
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

        // boss 名:"Slayer Quest" 下面一行(分数更低)
        String bossLine = scoreLines.get(slayerQuestScore - 1);
        if (bossLine == null) { reset(); return; }

        String bossName = ChatUtils.removeEmoji(ChatUtils.stripColor(bossLine)).trim();
        String[] nameParts = bossName.split(" ");
        bossTier = nameParts.length > 2 ? nameParts[nameParts.length - 1] : "";
        if (nameParts.length >= 2) {
            bossName = nameParts[0] + " " + nameParts[1];
        }
        if (!BOSS_DEFS.containsKey(bossName)) { reset(); return; }
        slayerType = bossName;
        var def = BOSS_DEFS.get(bossName);

        // "Slay the boss" / 刷怪进度行:"Slayer Quest" 往下两行
        String slayLine = scoreLines.get(slayerQuestScore - 2);
        if (slayLine == null || !ChatUtils.stripColor(slayLine).contains("Slay the boss")) {
            if (!"Inferno Demonlord".equals(bossName) || bossEntity == null) { reset(); return; }
            trackInfernoSplit(level, def);
            return;
        }

        // boss 战阶段。侧边栏切成 "Slay the boss!" 时服务端已经生成 boss,实体要过几秒
        // 才到 —— 在认领到候选之前一直停在这个状态,不要 reset。
        if (!bossPhase) {
            bossPhase = true;
            bossPhaseSince = ServerTick.getTime();
        }
        if (bossEntity != null && !isTrackedBossValid(bossEntity, def, bossName)) bossEntity = null;
        if (bossEntity == null) claimBoss(level, def, bossName);
        if (bossEntity == null) { renderStr = ""; return; }

        // 铭牌堆(名字 + 血量 + 状态)就叠在 boss 身上,作为后续搜索的锚点。
        ArmorStand bossStand = findBossStand(level, bossEntity, def, bossName);
        if (bossStand == null) return;

        // 找锚点附近的铭牌,用来取血量和计时
        List<ArmorStand> nearbyStands = new ArrayList<>();
        for (Entity e : level.entitiesForRendering()) {
            if (e instanceof ArmorStand as && as != bossStand
                    && as.distanceTo(bossStand) < 1
                    && Math.abs(as.getX() - bossStand.getX()) < 0.5
                    && Math.abs(as.getZ() - bossStand.getZ()) < 0.5) {
                nearbyStands.add(as);
            }
        }

        // 血量铭牌。T5 可能换名:Rev T5 是 Atoned Horror;蜘蛛 T5 一阶段仍叫
        // Tarantula Broodfather(和 T1-T4 同名),二阶段才变 Conjoined Brood,
        // 所以两个名字都收,二阶段继续靠铭牌里有没有 Conjoined Brood 来判。
        String bossStandTag = ChatUtils.toLegacyString(bossStand.getName());
        hpTag = null;
        if (hasBossName(bossStandTag, bossName)) {
            hpTag = bossStandTag;
        } else {
            for (ArmorStand as : nearbyStands) {
                String nm = ChatUtils.toLegacyString(as.getName());
                if (hasBossName(nm, bossName)) {
                    hpTag = nm;
                    break;
                }
            }
        }

        // 识别蜘蛛 T5 第二阶段(Conjoined Brood → 三只叠在一起的蜘蛛)
        spiderPhase2 = "Tarantula Broodfather".equals(bossName) && "V".equals(bossTier)
                && hpTag != null && ChatUtils.stripColor(hpTag).contains("Conjoined Brood");

        hp = (hpTag != null && hpTag.contains("ᛤ") ? "§5ᛤ§r " : "")
                + healthToString((LivingEntity) bossEntity);

        // 剩余时间
        timeLeft = "";
        for (ArmorStand as : nearbyStands) {
            String nm = ChatUtils.toLegacyString(as.getName());
            if (nm.contains(":")) {
                timeLeft = nm;
                break;
            }
        }

        // 末影人猎手专用逻辑
        if ("Voidgloom Seraph".equals(bossName)) {
            trackVoidgloom(level);
        }

        // 拼 HUD 文本
        buildRenderStr();
    }

    // ---- boss 归属判定 ----

    private static void onEntityLoad(Entity entity) {
        long now = ServerTick.getTime();
        bossSpawns.entrySet().removeIf(e -> now - e.getValue() > SPAWN_BUFFER_MS);
        if (isBossType(entity.getType())) bossSpawns.put(entity.getId(), now);
    }

    private static boolean isBossType(EntityType<?> type) {
        for (BossDef def : BOSS_DEFS.values()) {
            if (def.type == type) return true;
        }
        return false;
    }

    /**
     * 认领属于我们这次任务的 boss。
     * <p>1) 侧边栏切成 "Slay the boss!" 之后窗口内第一个生成的带名字猎手 boss —— boss
     * 一定刷在我们补刀的位置,也就是贴着我们加载,而别人的 boss 早就加载完了。
     * <p>2) 窗口过期后(中途开模块、实体离开视野距离后重新加载):只认全场唯一的那只,
     * 有多只候选时宁可不显示,也不显示别人的 boss。
     * <p>3) 期间玩家主动命中的那只,见 {@link #onAttackEntity}。
     */
    private static void claimBoss(ClientLevel level, BossDef def, String bossName) {
        long now = ServerTick.getTime();
        bossSpawns.entrySet().removeIf(e -> now - e.getValue() > SPAWN_BUFFER_MS);
        long sinceFlip = now - bossPhaseSince;

        if (sinceFlip <= CLAIM_WINDOW_MS) {
            Entity best = null;
            long bestSpawn = Long.MAX_VALUE;
            for (Map.Entry<Integer, Long> entry : bossSpawns.entrySet()) {
                if (entry.getValue() < bossPhaseSince - CLAIM_LOOKBACK_MS) continue;
                Entity e = level.getEntity(entry.getKey());
                if (!isOwnBoss(level, e, def, bossName)) continue;
                if (entry.getValue() < bestSpawn) {
                    bestSpawn = entry.getValue();
                    best = e;
                }
            }
            if (best != null) bossEntity = best;
            return;
        }

        if (sinceFlip > CLAIM_FALLBACK_MS) return;

        Entity only = null;
        for (Entity e : level.entitiesForRendering()) {
            if (!isOwnBoss(level, e, def, bossName)) continue;
            if (only != null) return;
            only = e;
        }
        bossEntity = only;
    }

    /**
     * 命中兜底:还没认领到 boss 时,玩家打中的那只带名字猎手 boss 直接绑定。
     * 这里的铭牌判定就是防止"打普通小怪(同实体类型、没有 boss 铭牌)被误绑过去"。
     */
    private static void onAttackEntity(Entity entity) {
        if (entity == null || bossEntity != null || !bossPhase) return;
        BossDef def = BOSS_DEFS.get(slayerType);
        ClientLevel level = Minecraft.getInstance().level;
        if (def == null || level == null) return;
        if (isOwnBoss(level, entity, def, slayerType)) bossEntity = entity;
    }

    /** 当前跟踪的 boss 还符合正在进行的任务吗 */
    private static boolean isTrackedBossValid(Entity e, BossDef def, String bossName) {
        if (!e.isAlive() || e.getType() != def.type) return false;
        if ("Riftstalker Bloodfiend".equals(bossName) && !"Bloodfiend ".equals(e.getName().getString())) {
            return false;
        }
        return !spiderPhase2 || ChatUtils.stripColor(e.getName().getString()).contains("Dinnerbone");
    }

    /** 候选判定:实体类型对得上,且身上挂着带 boss 名字的铭牌 */
    private static boolean isOwnBoss(ClientLevel level, Entity e, BossDef def, String bossName) {
        return e != null
                && isTrackedBossValid(e, def, bossName)
                && findBossStand(level, e, def, bossName) != null;
    }

    /** 实体正上方那块带 boss 名字和血量的铭牌 */
    private static ArmorStand findBossStand(ClientLevel level, Entity boss, BossDef def, String bossName) {
        ArmorStand best = null;
        double bestDist = Double.MAX_VALUE;
        for (Entity e : level.entitiesForRendering()) {
            if (!(e instanceof ArmorStand as)) continue;
            if (!hasBossName(ChatUtils.toLegacyString(as.getName()), bossName)) continue;

            double dx = as.getX() - boss.getX();
            double dz = as.getZ() - boss.getZ();
            if (Math.abs(dx) > 2 || Math.abs(dz) > 2) continue;
            if (as.getY() < boss.getY() - 1 || as.getY() > boss.getY() + getEffectiveH(def) + 3) continue;

            double dist = dx * dx + dz * dz;
            if (dist < bestDist) {
                bestDist = dist;
                best = as;
            }
        }
        return best;
    }

    /** 这块铭牌写的是当前任务的 boss 名吗(含 T5 的备选名) */
    private static boolean hasBossName(String nametag, String bossName) {
        String plain = ChatUtils.stripColor(nametag);
        BossDef def = BOSS_DEFS.get(bossName);
        if (def != null && plain.contains(def.name)) return true;
        String t5 = t5AltName(bossName, bossTier);
        return t5 != null && plain.contains(t5);
    }

    /**
     * T5 的备选显示名。
     * <p>Rev T5 一阶段起就叫 Atoned Horror;蜘蛛 T5 一阶段还是 Tarantula Broodfather
     * (与 T1-T4 同名),只有二阶段换成 Conjoined Brood。
     */
    private static String t5AltName(String bossName, String tier) {
        if (!"V".equals(tier)) return null;
        return switch (bossName) {
            case "Revenant Horror" -> "Atoned Horror";
            case "Tarantula Broodfather" -> "Conjoined Brood";
            default -> null;
        };
    }

    // ---- 火焰猎手分身追踪 ----

    private static void trackInfernoSplit(ClientLevel level, BossDef def) {
        if (bossEntity == null) return;

        infernoMinions.removeIf(e -> e == null || !e.isAlive());

        for (Entity e : level.entitiesForRendering()) {
            if (infernoMinions.size() >= 2) break;
            if (e.getType() == EntityType.ZOMBIFIED_PIGLIN
                    && e.distanceTo(bossEntity) < 3.5
                    && e.getY() > bossEntity.getY() - 0.5
                    && infernoMinions.stream().noneMatch(m -> m.getType() == EntityType.ZOMBIFIED_PIGLIN)) {
                infernoMinions.add(e);
            }
        }
        for (Entity e : level.entitiesForRendering()) {
            if (infernoMinions.size() >= 2) break;
            if (e.getType() == EntityType.SKELETON
                    && e instanceof LivingEntity le && le.isBaby()
                    && e.distanceTo(bossEntity) < 3.5
                    && e.getY() > bossEntity.getY() - 0.5
                    && infernoMinions.stream().noneMatch(m -> m.getType() == EntityType.SKELETON)) {
                infernoMinions.add(e);
            }
        }

        var cfg = ModConfigManager.get().slayer;
        if (cfg.slayerBossInfo.blazeSlayerInfo == ModConfig.SlayerBossInfoMode.OFF) return;

        List<String> bossStatus = new ArrayList<>();
        String[] parts = timeLeft != null ? timeLeft.split(" ") : new String[0];
        if (parts.length > 1) {
            bossStatus.addAll(Arrays.asList(parts).subList(0, parts.length - 1));
            bossStatus.add("§7→§r");
            bossStatus.add(NEXT_BLAZE_ATTUNED.getOrDefault(ChatUtils.stripColor(parts[0]), ""));
        }

        String currentlyShield = "";
        List<String> infernoMobsStr = new ArrayList<>();
        for (Entity e : infernoMinions) {
            String marker = e.getType() == EntityType.SKELETON ? "ⓆⓊⒶⓏⒾⒾ" : "ⓉⓎⓅⒽⓄⒺⓊⓈ";

            for (Entity a : level.entitiesForRendering()) {
                if (!(a instanceof ArmorStand as)) continue;
                if (as.distanceTo(e) >= 3) continue;
                String nm = ChatUtils.toLegacyString(as.getName());
                if (nm.contains("❤") && nm.contains(marker)) {
                    String[] hpParts = nm.split(" ");
                    String separator = e.getType() == EntityType.SKELETON ? "     " : " ";
                    infernoMobsStr.add('\n' + String.join(separator,
                            java.util.Arrays.copyOfRange(hpParts, 1, hpParts.length)));
                }
                if (nm.matches(".*[0-9]{1,2}:[0-9]{1,2}.*")) {
                    String[] tParts = nm.split(" ");
                    List<String> timeList = new ArrayList<>(java.util.Arrays.asList(tParts));
                    if (!timeList.isEmpty()) timeList.removeLast();
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

    // ---- 末影人猎手专用逻辑 ----

    private static void trackVoidgloom(ClientLevel level) {
        if (bossEntity instanceof EnderMan enderman) {
            // 激光(骑乘守卫者)检测
            if (enderman.getVehicle() != null) {
                boolean hasGuardian = false;
                for (Entity e : level.entitiesForRendering()) {
                    if (e.getType() == EntityType.GUARDIAN && e.distanceTo(enderman) < 3) {
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

            // 信标追踪
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
                for (int dy = -3; dy <= 3 && !foundBeacon; dy++) {
                    for (int dx = -3; dx <= 3 && !foundBeacon; dx++) {
                        for (int dz = -3; dz <= 3 && !foundBeacon; dz++) {
                            var be = level.getBlockEntity(standPos.offset(dx, dy, dz));
                            if (be instanceof BeaconBlockEntity) {
                                voidgloom.beaconStatus = "onTheGround";
                                voidgloom.beaconLoc = be.getBlockPos();
                                voidgloom.beaconEntity = null;
                                voidgloom.beaconTime = ServerTick.getTime() + 4800;
                                foundBeacon = true;
                            }
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

    // ---- 按 boss 类型拼 HUD 文本 ----

    private static void buildRenderStr() {
        var cfg = ModConfigManager.get().slayer;
        if (bossEntity == null && hpTag == null) { renderStr = ""; return; }

        switch (slayerType) {
            case "Revenant Horror" -> {
                if (cfg.slayerBossInfo.zombieSlayerInfo == ModConfig.SlayerBossInfoMode.OFF) { renderStr = ""; return; }
                String status = extractBossStatus();
                renderStr = "§bRevenant Horror " + hp
                        + (cfg.slayerBossInfo.zombieSlayerInfo == ModConfig.SlayerBossInfoMode.FULL && !status.isEmpty()
                        ? '\n' + status : "");
            }
            case "Tarantula Broodfather" -> {
                if (cfg.slayerBossInfo.spiderSlayerInfo == ModConfig.SlayerBossInfoMode.OFF) { renderStr = ""; return; }
                String status = extractBossStatus();
                renderStr = "§4Tarantula Broodfather " + hp
                        + (cfg.slayerBossInfo.spiderSlayerInfo == ModConfig.SlayerBossInfoMode.FULL && !status.isEmpty()
                        ? '\n' + status : "");
            }
            case "Sven Packmaster" -> {
                if (cfg.slayerBossInfo.wolfSlayerInfo == ModConfig.SlayerBossInfoMode.OFF) { renderStr = ""; return; }
                String status = extractBossStatus();
                renderStr = "§fSven Packmaster " + hp
                        + (cfg.slayerBossInfo.wolfSlayerInfo == ModConfig.SlayerBossInfoMode.FULL && !status.isEmpty()
                        ? '\n' + status : "");
            }
            case "Voidgloom Seraph" -> {
                if (cfg.slayerBossInfo.endermanSlayerInfo == ModConfig.SlayerBossInfoMode.OFF) { renderStr = ""; return; }
                String hitInfo = (hpTag != null && hpTag.contains("Hit"))
                        ? "\n§fHit Phase: " + ChatUtils.stripColor(hpTag).replaceAll("[^0-9]", "") + " Hits"
                        : "";
                renderStr = "§5Voidgloom Seraph " + hp + hitInfo;
                if (cfg.slayerBossInfo.endermanSlayerInfo == ModConfig.SlayerBossInfoMode.FULL) {
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
                if (cfg.slayerBossInfo.blazeSlayerInfo == ModConfig.SlayerBossInfoMode.OFF) { renderStr = ""; return; }
                String[] parts = timeLeft != null ? timeLeft.split(" ") : new String[0];
                List<String> bossStatus = new ArrayList<>();
                if (parts.length > 1) {
                    bossStatus.addAll(Arrays.asList(parts).subList(0, parts.length - 1));
                    bossStatus.add("§7→§r");
                    bossStatus.add(NEXT_BLAZE_ATTUNED.getOrDefault(
                            ChatUtils.stripColor(parts[0]), ""));
                }
                renderStr = "§bInferno Demonlord " + (hp.contains(" ") ? hp.split(" ")[hp.split(" ").length - 1] : hp)
                        + (cfg.slayerBossInfo.blazeSlayerInfo == ModConfig.SlayerBossInfoMode.FULL && parts.length > 1
                        ? '\n' + String.join(" ", bossStatus) : "");
            }
            case "Riftstalker Bloodfiend" -> {
                if (cfg.slayerBossInfo.vampireSlayerInfo == ModConfig.SlayerBossInfoMode.OFF) { renderStr = ""; return; }
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
                        + (cfg.slayerBossInfo.vampireSlayerInfo == ModConfig.SlayerBossInfoMode.FULL
                        ? '\n' + vampireStatus.toString().stripTrailing() : "");
            }
        }
    }

    // ---- 工具方法 ----

    /**
     * 取 boss 的有效高度,蜘蛛 T5 第二阶段要按叠起来的三只蜘蛛算。
     * 蜘蛛 T5 第二阶段是 3 只叠在一起的蜘蛛(Dinnerbone + 洞穴蜘蛛 + 普通蜘蛛)。
     */
    static double getEffectiveH(BossDef def) {
        if (spiderPhase2) return 1.7;
        return def.h;
    }

    static void reset() {
        slayerType = "";
        bossTier = "";
        bossEntity = null;
        hp = "";
        hpTag = "";
        timeLeft = "";
        renderStr = "";
        spiderPhase2 = false;
        bossPhase = false;
        bossPhaseSince = 0;
        voidgloom.reset();
        infernoMinions.clear();
        infernoStatus.reset();
    }

    static String extractBossStatus() {
        if (timeLeft == null || timeLeft.isEmpty()) return "";
        String[] parts = timeLeft.split(" ");
        if (parts.length <= 1) return "";
        List<String> statusParts = new ArrayList<>(java.util.Arrays.asList(parts));
        statusParts.removeLast();
        return String.join(" ", statusParts);
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
