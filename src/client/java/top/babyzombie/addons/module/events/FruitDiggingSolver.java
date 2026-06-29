package top.babyzombie.addons.module.events;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * 挖水果小游戏解题器。
 * 不依赖 Minecraft 类（纯逻辑），便于测试和调试。
 *
 * <h3>游戏规则摘要</h3>
 * <ul>
 *   <li>7×7 棋盘，34 个水果 + 10 炸弹 + 5 朗姆酒 = 49 格</li>
 *   <li>每局 15 次挖掘机会</li>
 *   <li>每次挖掘可同时使用一个探测技能（MINES / TREASURE / ANCHOR）</li>
 *   <li>Apple/Cherry 累积加分，越早挖越值钱</li>
 *   <li>Pomegranate 使下一次挖掘 ×1.5，Durian 使下一次 ×0.5，Coconut 防炸弹</li>
 *   <li>Watermelon 炸相邻随机一个水果，获得其一半分值并触发该水果技能</li>
 * </ul>
 */
public final class FruitDiggingSolver {

    private FruitDiggingSolver() {}

    // ═══════════════════════════════════════════════════════════════
    // 枚举定义
    // ═══════════════════════════════════════════════════════════════

    /** 水果类型 */
    public enum FruitType {
        MANGO       (300, 10, "Mango"),
        APPLE       (0,   8,  "Apple"),        // 基础 0，累积 +100/个（含自身）
        WATERMELON  (100, 4,  "Watermelon"),
        POMEGRANATE (200, 4,  "Pomegranate"),
        COCONUT     (200, 3,  "Coconut"),
        CHERRY      (200, 2,  "Cherry"),       // 基础 200，累积 +300/个（不计自身）
        DURIAN      (800, 2,  "Durian"),
        DRAGONFRUIT (1200,1,  "Dragonfruit");

        /** 基础分 */
        public final int basePoints;
        /** 该水果在每盘中的总数 */
        public final int totalPerBoard;
        /** 英文名 */
        public final String englishName;

        FruitType(int basePoints, int totalPerBoard, String englishName) {
            this.basePoints = basePoints;
            this.totalPerBoard = totalPerBoard;
            this.englishName = englishName;
        }
    }

    /** 探测模式（每次挖掘必须三选一） */
    public enum DowsingMode {
        /** 探测相邻炸弹数量 */
        MINES,
        /** 揭示相邻最高分水果 */
        TREASURE,
        /** 揭示相邻最低分水果 */
        ANCHOR
    }

    /** 挖掘后的乘数状态 */
    public enum MultiplierState {
        NORMAL,
        POMEGRANATE_ACTIVE,   // 下次 ×1.5
        DURIAN_ACTIVE,        // 下次 ×0.5
        COCONUT_ACTIVE        // 下次炸弹无效
    }

    // ═══════════════════════════════════════════════════════════════
    // 数据结构
    // ═══════════════════════════════════════════════════════════════

    /** 棋盘单格 */
    public static class Tile {
        /** 已知水果类型（null = 未知） */
        public FruitType fruit;
        /** 是否已挖开 */
        public boolean revealed;
        /** MINES 探测是否已覆盖此格 */
        public boolean hasMinesInfo;
        /** MINES 探测报告的相邻炸弹数（-1 表示未探测） */
        public int minesCount = -1;
        /** TREASURE/ANCHOR 探测是否已覆盖此格 */
        public boolean hasTreasureInfo;
        /** TREASURE 提示的水果类型（null=无水果或未探测） */
        public FruitType treasureHint;
        /** ANCHOR 提示（true=附近无水果） */
        public boolean anchorNoFruit;
        /** 相邻格有 TREASURE 提示的水果类型（传播来的） */
        public FruitType treasureAdjacent;

        public Tile() {}

        public boolean isKnownFruit() { return fruit != null && !revealed; }
        public boolean isUnknown() { return fruit == null && !revealed; }
    }

    /** 完整的棋盘状态（不可变风格：apply 返回新实例） */
    public static class BoardState {
        public final Tile[][] grid;               // [x][z], 0-6
        public final int[] remaining;             // 每种水果剩余数量，索引 = FruitType.ordinal()
        public int remainingBombs;
        public int remainingRum;
        public int applesTriggered;               // 已触发 Apple 总数（含已挖+被西瓜炸）
        public int cherriesTriggered;              // 已触发 Cherry 总数
        public int digsRemaining;
        public MultiplierState multiplierState;

        public BoardState() {
            this.grid = new Tile[7][7];
            for (int x = 0; x < 7; x++)
                for (int z = 0; z < 7; z++)
                    grid[x][z] = new Tile();
            this.remaining = new int[FruitType.values().length];
            for (FruitType f : FruitType.values())
                this.remaining[f.ordinal()] = f.totalPerBoard;
            this.remainingBombs = 10;
            this.remainingRum = 5;
            this.digsRemaining = 15;
            this.multiplierState = MultiplierState.NORMAL;
        }

        /** 浅拷贝（grid 内的 Tile 共享引用） */
        public BoardState copy() {
            BoardState s = new BoardState();
            for (int x = 0; x < 7; x++)
                System.arraycopy(this.grid[x], 0, s.grid[x], 0, 7);
            System.arraycopy(this.remaining, 0, s.remaining, 0, this.remaining.length);
            s.remainingBombs = this.remainingBombs;
            s.remainingRum = this.remainingRum;
            s.applesTriggered = this.applesTriggered;
            s.cherriesTriggered = this.cherriesTriggered;
            s.digsRemaining = this.digsRemaining;
            s.multiplierState = this.multiplierState;
            return s;
        }

        /** 从 ItemInfo 构建棋盘 */
        public static BoardState fromFruitPositions(Map<Integer, Map<Integer, FruitType>> knownFruits) {
            BoardState s = new BoardState();
            int[] foundCount = new int[FruitType.values().length];
            for (var xEntry : knownFruits.entrySet()) {
                for (var zEntry : xEntry.getValue().entrySet()) {
                    int gx = xEntry.getKey();
                    int gz = zEntry.getKey();
                    FruitType f = zEntry.getValue();
                    if (gx >= 0 && gx < 7 && gz >= 0 && gz < 7) {
                        s.grid[gx][gz].fruit = f;
                        foundCount[f.ordinal()]++;
                    }
                }
            }
            // 更新剩余计数
            for (FruitType f : FruitType.values())
                s.remaining[f.ordinal()] = Math.max(0, f.totalPerBoard - foundCount[f.ordinal()]);
            // 未知格 = 炸弹或朗姆酒或未扫描到的水果
            int unknownCells = s.countUnknownCells();
            int missingFruits = 0;
            for (FruitType f : FruitType.values())
                missingFruits += s.remaining[f.ordinal()];
            // 剩余未知格中，部分是炸弹+朗姆
            s.remainingBombs = Math.min(10, unknownCells - missingFruits > 0 ? Math.min(10, (unknownCells - missingFruits) * 10 / 15) : 0);
            s.remainingRum = Math.min(5, unknownCells - missingFruits - s.remainingBombs);
            return s;
        }

        public int countUnknownCells() {
            int c = 0;
            for (int x = 0; x < 7; x++)
                for (int z = 0; z < 7; z++)
                    if (grid[x][z].isUnknown()) c++;
            return c;
        }

        public double currentMultiplier() {
            return switch (multiplierState) {
                case POMEGRANATE_ACTIVE -> 1.5;
                case DURIAN_ACTIVE -> 0.5;
                default -> 1.0;
            };
        }
    }

    /** 一次推荐移动 */
    public static class Move {
        public int gridX, gridZ;
        public DowsingMode dowsing;
        public double score;
        public String reason;  // 推荐理由（调试用）

        public Move(int x, int z, DowsingMode mode, double score, String reason) {
            this.gridX = x;
            this.gridZ = z;
            this.dowsing = mode;
            this.score = score;
            this.reason = reason;
        }

        @Override
        public String toString() {
            return String.format("Move[%d,%d] mode=%s score=%.1f %s", gridX, gridZ, dowsing, score, reason);
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // 全局可调参数（运行时由 ModConfig 注入）
    // ═══════════════════════════════════════════════════════════════

    /** 从英文名查找水果类型 */
    public static FruitType fromEnglishName(String name) {
        for (FruitType f : FruitType.values()) {
            if (f.englishName.equalsIgnoreCase(name)) return f;
        }
        return null;
    }

    /** 从英文名查找水果类型（Bomb/Rum 返回 null） */
    public static FruitType fromScannerName(String name) {
        return switch (name) {
            case "Mango" -> FruitType.MANGO;
            case "Apple" -> FruitType.APPLE;
            case "Watermelon" -> FruitType.WATERMELON;
            case "Pomegranate" -> FruitType.POMEGRANATE;
            case "Coconut" -> FruitType.COCONUT;
            case "Cherry" -> FruitType.CHERRY;
            case "Durian" -> FruitType.DURIAN;
            case "Dragonfruit" -> FruitType.DRAGONFRUIT;
            default -> null; // Bomb, Rum, etc.
        };
    }

    public static class Params {
        public float bombPenalty = 200f;
        public float rumPenalty = 100f;
        public float minesInfoWeight = 30f;
        public float treasureInfoWeight = 25f;
        public float anchorInfoWeight = 15f;
        public float earlyAppleBonus = 50f;
        public float earlyCherryBonus = 80f;
        public int watermelonMCSamples = 200;
        public int earlyGameDigs = 3;
        public int lateGameDigs = 3;
    }

    private static final Params params = new Params();

    public static Params getParams() { return params; }

    // ═══════════════════════════════════════════════════════════════
    // 核心评估
    // ═══════════════════════════════════════════════════════════════

    /**
     * 计算某个水果在当前状态下的分值。
     * 乘数不在这里应用，由调用方处理。
     */
    public static double fruitValue(FruitType fruit, BoardState state) {
        return switch (fruit) {
            case APPLE -> {
                // 第 N 个 = N × 100（含自身）：当前苹果是第 (applesTriggered+1) 个
                int n = state.applesTriggered + 1;
                yield n * 100.0;
            }
            case CHERRY -> {
                // 第 1 个 = 200, 第 2 个 = 200 + 1×300 = 500
                int prev = state.cherriesTriggered;
                yield 200.0 + prev * 300.0;
            }
            case MANGO       -> 300.0;
            case WATERMELON  -> 100.0;  // 西瓜基本值，连锁在阶段 4 单独处理
            case POMEGRANATE -> 200.0;
            case COCONUT     -> 200.0;
            case DURIAN      -> 800.0;
            case DRAGONFRUIT -> 1200.0;
        };
    }

    /**
     * 计算挖掘后乘数状态的转换
     */
    public static MultiplierState nextMultiplierState(FruitType fruit, MultiplierState currentState) {
        // 如果当前有 Coconut 护盾，不改变乘数
        return switch (fruit) {
            case POMEGRANATE -> MultiplierState.POMEGRANATE_ACTIVE;
            case DURIAN -> MultiplierState.DURIAN_ACTIVE;
            case COCONUT -> MultiplierState.COCONUT_ACTIVE;
            default -> MultiplierState.NORMAL;
        };
    }

    // ═══════════════════════════════════════════════════════════════
    // 概率计算
    // ═══════════════════════════════════════════════════════════════

    /** 未知格是某种水果的概率 */
    public static double fruitProbability(FruitType fruit, BoardState state) {
        int unknown = state.countUnknownCells();
        if (unknown <= 0) return 0;
        return (double) state.remaining[fruit.ordinal()] / unknown;
    }

    /** 未知格是炸弹的概率 */
    public static double bombProbability(BoardState state) {
        int unknown = state.countUnknownCells();
        if (unknown <= 0) return 0;
        return (double) state.remainingBombs / unknown;
    }

    /** 未知格是朗姆酒的概率 */
    public static double rumProbability(BoardState state) {
        int unknown = state.countUnknownCells();
        if (unknown <= 0) return 0;
        return (double) state.remainingRum / unknown;
    }

    /** 未知格的期望水果得分 */
    public static double expectedFruitScore(BoardState state) {
        double sum = 0;
        double totalWeight = 0;
        for (FruitType f : FruitType.values()) {
            int rem = state.remaining[f.ordinal()];
            if (rem > 0) {
                double weight = rem;
                sum += weight * fruitValue(f, state);
                totalWeight += weight;
            }
        }
        return totalWeight > 0 ? sum / totalWeight : 0;
    }

    // ═══════════════════════════════════════════════════════════════
    // 探测信息增益
    // ═══════════════════════════════════════════════════════════════

    /** 计算某个格子的相邻未知格数量（含对角线，8 方向） */
    public static int countAdjacentUnknown(int gx, int gz, BoardState state) {
        int count = 0;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (dx == 0 && dz == 0) continue;
                int nx = gx + dx, nz = gz + dz;
                if (nx < 0 || nx >= 7 || nz < 0 || nz >= 7) continue;
                if (state.grid[nx][nz].isUnknown()) count++;
            }
        }
        return count;
    }

    /** 计算探测信息增益（考虑衰减：已被探测过的相邻格权重降低） */
    public static double infoGain(int gx, int gz, DowsingMode mode, BoardState state) {
        double total = 0;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (dx == 0 && dz == 0) continue;
                int nx = gx + dx, nz = gz + dz;
                if (nx < 0 || nx >= 7 || nz < 0 || nz >= 7) continue;
                Tile neighbor = state.grid[nx][nz];
                if (neighbor.revealed) continue;
                // 衰减：已有探测信息的格贡献降低
                double decay = 1.0;
                if (neighbor.hasMinesInfo) decay = 0.2;
                if (neighbor.hasTreasureInfo) decay = Math.min(decay, 0.3);
                total += decay;
            }
        }
        if (total <= 0) return 0;
        return switch (mode) {
            case MINES -> total * params.minesInfoWeight;
            case TREASURE -> total * params.treasureInfoWeight;
            case ANCHOR -> total * params.anchorInfoWeight;
        };
    }

    // ═══════════════════════════════════════════════════════════════
    // 单格评估
    // ═══════════════════════════════════════════════════════════════

    /**
     * 评估单个 (格子, 探测模式) 组合的期望得分。
     */
    public static double evaluate(int gx, int gz, DowsingMode mode, BoardState state) {
        Tile tile = state.grid[gx][gz];
        if (tile == null || tile.revealed) return Double.NEGATIVE_INFINITY;

        double multiplier = state.currentMultiplier();

        // ── 已知水果 ──
        if (tile.isKnownFruit()) {
            FruitType f = tile.fruit;
            // Durian 惩罚：如果当前是 ×1.5 状态，Durian 后清理，收益低于预期
            // 在 evaluate 层面做简单修正
            double base = fruitValue(f, state);

            // Pomegranate → Durian 惩罚检查
            if (state.multiplierState == MultiplierState.POMEGRANATE_ACTIVE && f == FruitType.DURIAN) {
                base -= 400; // 1.5× 浪费在 Durian 上，下步还 ×0.5
            }

            double fruitScore = base * multiplier;
            double info = infoGain(gx, gz, mode, state);

            // 早期奖励 Apple/Cherry
            if (f == FruitType.APPLE && state.digsRemaining > params.lateGameDigs) {
                fruitScore += params.earlyAppleBonus * state.digsRemaining / 15.0;
            }
            if (f == FruitType.CHERRY && state.digsRemaining > params.lateGameDigs) {
                fruitScore += params.earlyCherryBonus * state.digsRemaining / 15.0;
            }

            return fruitScore + info;
        }

        // ── 未知格 ──
        double expScore = expectedFruitScore(state) * multiplier;
        // 宝藏提示：加权混合均匀期望和被提示水果值
        // 高分水果（龙果1200）占主导，低分水果（芒果300）适度提升
        if (tile.treasureAdjacent != null) {
            double hintVal = fruitValue(tile.treasureAdjacent, state);
            // 混合权重：被提示水果本身分越高，占的比重越大
            double weight = Math.min(0.8, hintVal / 1200.0); // 龙果80%，芒果20%
            expScore = expScore * (1 - weight) + hintVal * weight;
        }
        double bombRisk = bombProbability(state) * params.bombPenalty;
        double rumRisk = rumProbability(state) * params.rumPenalty;

        // Coconut 护盾消除炸弹风险
        if (state.multiplierState == MultiplierState.COCONUT_ACTIVE) {
            bombRisk = 0;
        }

        double info = infoGain(gx, gz, mode, state);
        return expScore - bombRisk - rumRisk + info;
    }

    // ═══════════════════════════════════════════════════════════════
    // 状态机驱动的最优移动搜索
    // ═══════════════════════════════════════════════════════════════

    /** 策略阶段 */
    public enum StrategyPhase {
        /** 开局：MINES 探测炸弹（中心 + 对角） */
        SWEEP_BOMBS,
        /** 中期：TREASURE 定位龙果/榴莲 */
        HUNT_DRAGONFRUIT,
        /** 追踪：已探测到高价值水果，挖相邻候选格 */
        DIG_CANDIDATES,
        /** 后期：清理安全区 + 已知水果 */
        CLEANUP
    }

    /** 开局 TREASURE 探测顺序：中心优先（覆盖最多），再四角 */
    private static final int[][] HUNT_START_POS = {{3,3}, {1,1}, {1,5}, {5,1}, {5,5}};
    static int huntIndex = 0;

    /** 判断当前策略阶段 */
    public static StrategyPhase determinePhase(BoardState state) {
        // 1. 龙果提示 + 还有库存 → 追踪（每次探测假定最多 1 个）
        if (hasTreasureHint(FruitType.DRAGONFRUIT, state)
                && state.remaining[FruitType.DRAGONFRUIT.ordinal()] > 0)
            return StrategyPhase.DIG_CANDIDATES;
        // 榴莲：龙果已找到 + 探测假定 1 个/次 → 追
        if (hasTreasureHint(FruitType.DURIAN, state)
                && state.remaining[FruitType.DURIAN.ordinal()] > 0
                && state.remaining[FruitType.DRAGONFRUIT.ordinal()] <= 0) {
            return StrategyPhase.DIG_CANDIDATES;
        }

        // 2. 龙果+榴莲都找到了 → 收尾
        if (state.remaining[FruitType.DRAGONFRUIT.ordinal()] <= 0
                && state.remaining[FruitType.DURIAN.ordinal()] <= 0) {
            return StrategyPhase.CLEANUP;
        }

        // 3. 已知石榴+龙果在候选区+还有库存 → 先挖石榴
        if (hasTreasureHint(FruitType.DRAGONFRUIT, state)
                && state.remaining[FruitType.DRAGONFRUIT.ordinal()] > 0
                && hasKnownFruit(FruitType.POMEGRANATE, state)) {
            return StrategyPhase.CLEANUP;
        }

        // 4. 继续猎龙
        return StrategyPhase.HUNT_DRAGONFRUIT;
    }

    private static boolean hasTreasureHint(FruitType ft, BoardState state) {
        for (int x = 0; x < 7; x++)
            for (int z = 0; z < 7; z++)
                if (state.grid[x][z].treasureAdjacent == ft) return true;
        return false;
    }

    private static boolean hasKnownFruit(FruitType ft, BoardState state) {
        for (int x = 0; x < 7; x++)
            for (int z = 0; z < 7; z++)
                if (state.grid[x][z].isKnownFruit() && state.grid[x][z].fruit == ft) return true;
        return false;
    }

    /** 找到得分最高的未挖格子（不限模式） */
    private static Move bestOfAll(BoardState state, DowsingMode... allowedModes) {
        Move best = null;
        double bestScore = Double.NEGATIVE_INFINITY;
        var modeSet = java.util.EnumSet.of(allowedModes[0], allowedModes);
        for (int x = 0; x < 7; x++) {
            for (int z = 0; z < 7; z++) {
                if (state.grid[x][z].revealed) continue;
                for (DowsingMode mode : modeSet) {
                    double s = evaluate(x, z, mode, state);
                    if (s > bestScore) {
                        bestScore = s;
                        best = new Move(x, z, mode, s, "");
                    }
                }
            }
        }
        return best;
    }

    /** 找已知但未挖的特定水果 */
    private static Move findKnownFruit(FruitType ft, BoardState state) {
        for (int x = 0; x < 7; x++) {
            for (int z = 0; z < 7; z++) {
                if (state.grid[x][z].isKnownFruit() && state.grid[x][z].fruit == ft) {
                    return new Move(x, z, DowsingMode.ANCHOR, fruitValue(ft, state), "已知" + ft.englishName);
                }
            }
        }
        return null;
    }

    /** 找 treasureAdjacent 候选格中最好的 */
    private static Move bestCandidate(BoardState state) {
        Move best = null;
        double bestScore = Double.NEGATIVE_INFINITY;
        for (int x = 0; x < 7; x++) {
            for (int z = 0; z < 7; z++) {
                if (state.grid[x][z].revealed) continue;
                if (state.grid[x][z].treasureAdjacent == null) continue;
                // 候选格用 ANCHOR（最轻探测，相当于直接挖）
                double s = evaluate(x, z, DowsingMode.ANCHOR, state);
                if (s > bestScore) {
                    bestScore = s;
                    best = new Move(x, z, DowsingMode.ANCHOR, s, "追踪宝藏");
                }
            }
        }
        return best;
    }

    /**
     * 状态机驱动的搜索。
     */
    public static Move findBestMove(BoardState state) {
        StrategyPhase phase = determinePhase(state);

        return switch (phase) {
            case HUNT_DRAGONFRUIT -> {
                // 按预设顺序探测：中心(3,3) → 四角，确保覆盖不重复
                for (int i = huntIndex; i < HUNT_START_POS.length; i++) {
                    int[] pos = HUNT_START_POS[i];
                    if (!state.grid[pos[0]][pos[1]].revealed) {
                        huntIndex = i;
                        Move m = new Move(pos[0], pos[1], DowsingMode.TREASURE, 0, "猎龙#" + i);
                        yield m;
                    }
                }
                // 预设位置都挖过了 → 全域搜索
                Move m = bestOfAll(state, DowsingMode.TREASURE);
                if (m != null) m.reason = "全域猎龙";
                yield m != null ? m : bestOfAll(state, DowsingMode.ANCHOR);
            }
            case DIG_CANDIDATES -> {
                Move m = bestCandidate(state);
                if (m != null) yield m;
                yield bestOfAll(state, DowsingMode.TREASURE);
            }
            case CLEANUP -> {
                // 石榴→龙果 combo：如果龙果在候选区且石榴已知 → 先挖石榴
                if (hasTreasureHint(FruitType.DRAGONFRUIT, state)) {
                    Move pom = findKnownFruit(FruitType.POMEGRANATE, state);
                    if (pom != null) {
                        pom.reason = "石榴→龙果";
                        yield pom;
                    }
                }
                // 已知龙果 → 直接挖
                Move df = findKnownFruit(FruitType.DRAGONFRUIT, state);
                if (df != null) { df.reason = "挖龙果"; yield df; }
                // 已知榴莲
                Move du = findKnownFruit(FruitType.DURIAN, state);
                if (du != null) { du.reason = "挖榴莲"; yield du; }
                // 已知高价值水果
                for (FruitType ft : new FruitType[]{FruitType.CHERRY, FruitType.COCONUT, FruitType.POMEGRANATE, FruitType.MANGO, FruitType.WATERMELON, FruitType.APPLE}) {
                    Move m = findKnownFruit(ft, state);
                    if (m != null) { m.reason = "已知" + ft.englishName; yield m; }
                }
                // 安全未知格
                Move m = bestOfAll(state, DowsingMode.ANCHOR);
                if (m != null) m.reason = "收尾";
                yield m;
            }
            default -> bestOfAll(state, DowsingMode.ANCHOR);
        };
    }

    // ═══════════════════════════════════════════════════════════════
    // 游戏阶段判断（供外部展示用）
    // ═══════════════════════════════════════════════════════════════

    public enum GamePhase { SWEEP, HUNT, DIG, END }

    public static GamePhase getPhase(int digsUsed) {
        return GamePhase.SWEEP; // 实际阶段由 determinePhase 决定
    }
}
