package top.babyzombie.addons.module.events;


import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import top.babyzombie.addons.util.render.RenderPhaseRegister;
import net.minecraft.client.Minecraft;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import top.babyzombie.addons.config.ModConfigManager;
import top.babyzombie.addons.util.ChatUtils;
import top.babyzombie.addons.util.ItemUtils;
import top.babyzombie.addons.util.tracker.HypixelLocationTracker;
import top.babyzombie.addons.util.render.BeamRenderer;
import top.babyzombie.addons.util.render.WorldTextRenderer;


public final class FruitDiggingModule {

    private static final Map<String, String> FRUITS = Map.ofEntries(
            Map.entry("ewogICJ0aW1lc3RhbXAiIDogMTcxODIwMzE5MzY5OCwKICAicHJvZmlsZUlkIiA6ICIyMjAwZjYzOWI1YTU0YzM2YjA4ZThiNjZhNDNjNmJjNCIsCiAgInByb2ZpbGVOYW1lIiA6ICJCYXVvSmxlVCIsCiAgInNpZ25hdHVyZVJlcXVpcmVkIiA6IHRydWUsCiAgInRleHR1cmVzIiA6IHsKICAgICJTS0lOIiA6IHsKICAgICAgInVybCIgOiAiaHR0cDovL3RleHR1cmVzLm1pbmVjcmFmdC5uZXQvdGV4dHVyZS9jOTJiMDk5YTYyY2QyZmJmOGFkYTA5ZGVjMTQ1Yzc1ZDdmZGE0ZGM1N2I5NjhiZWEzYThmYTExZTM3YWE0OGIyIgogICAgfQogIH0KfQ==", "Cherry"),
            Map.entry("ewogICJ0aW1lc3RhbXAiIDogMTcxOTMyMDg4MTU1MCwKICAicHJvZmlsZUlkIiA6ICJiMTM1MDRmMjMxOGI0OWNjYWFkZDcyYWVhYmMyNTQ1MCIsCiAgInByb2ZpbGVOYW1lIiA6ICJUeXBrZW4iLAogICJzaWduYXR1cmVSZXF1aXJlZCIgOiB0cnVlLAogICJ0ZXh0dXJlcyIgOiB7CiAgICAiU0tJTiIgOiB7CiAgICAgICJ1cmwiIDogImh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMTdlYTI3OGQ2MjI1YzQ0N2M1OTQzZDY1Mjc5OGQwYmJiZDE0MTg0MzRjZThjNTRjNTRmZGFjNzk5OTRkZGQ2YyIKICAgIH0KICB9Cn0=", "Apple"),
            Map.entry("ewogICJ0aW1lc3RhbXAiIDogMTcxOTMyMDk0MjUyNSwKICAicHJvZmlsZUlkIiA6ICI1ZjU5NmViY2JlOTQ0NmQxYmI0M2JlNGYzZjRiOGJlNSIsCiAgInByb2ZpbGVOYW1lIiA6ICJUZWlsMHNzIiwKICAic2lnbmF0dXJlUmVxdWlyZWQiIDogdHJ1ZSwKICAidGV4dHVyZXMiIDogewogICAgIlNLSU4iIDogewogICAgICAidXJsIiA6ICJodHRwOi8vdGV4dHVyZXMubWluZWNyYWZ0Lm5ldC90ZXh0dXJlL2FjMjY4ZDM2YzJjNjA0N2ZmZWVjMDAxMjQwOTYzNzZiNTZkYmI0ZDc1NmE1NTMyOTM2M2ExYjI3ZmNkNjU5Y2QiCiAgICB9CiAgfQp9", "Durian"),
            Map.entry("ewogICJ0aW1lc3RhbXAiIDogMTcxODIwMzIxNTc3NiwKICAicHJvZmlsZUlkIiA6ICJmZmU5MzczY2YyMDM0OWFhYTJlN2NiYzJkZmY2M2I5MyIsCiAgInByb2ZpbGVOYW1lIiA6ICJNZWxvblR1bmExIiwKICAic2lnbmF0dXJlUmVxdWlyZWQiIDogdHJ1ZSwKICAidGV4dHVyZXMiIDogewogICAgIlNLSU4iIDogewogICAgICAidXJsIiA6ICJodHRwOi8vdGV4dHVyZXMubWluZWNyYWZ0Lm5ldC90ZXh0dXJlLzEwY2ViMTQ1NWI0NzFkMDE2YTlmMDZkMjVmNmU0NjhkZjlmY2YyMjNlMmMxZTQ3OTViMTZlODRmY2NhMjY0ZWUiCiAgICB9CiAgfQp9", "Coconut"),
            Map.entry("ewogICJ0aW1lc3RhbXAiIDogMTcxOTMyMDk5NTI0OCwKICAicHJvZmlsZUlkIiA6ICI4YWFlYTdlYjViOWM0ZWEwODUxNWU3MDhhZGIxODBkNyIsCiAgInByb2ZpbGVOYW1lIiA6ICJNYVBhODA3MTEiLAogICJzaWduYXR1cmVSZXF1aXJlZCIgOiB0cnVlLAogICJ0ZXh0dXJlcyIgOiB7CiAgICAiU0tJTiIgOiB7CiAgICAgICJ1cmwiIDogImh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZWZlNGVmODNiYWYxMDVlOGRlZTZjZjAzZGZlNzQwN2YxOTExYjNiOTk1MmM4OTFhZTM0MTM5NTYwZjI5MzFkNiIKICAgIH0KICB9Cn0=", "Watermelon"),
            Map.entry("ewogICJ0aW1lc3RhbXAiIDogMTcxOTMyMDk4MDg4NywKICAicHJvZmlsZUlkIiA6ICJiMmQ4MTA2YTJjM2Y0ZTY4ODA0ODkzOWU0NGM1NmUyMiIsCiAgInByb2ZpbGVOYW1lIiA6ICJSZWdoeGQiLAogICJzaWduYXR1cmVSZXF1aXJlZCIgOiB0cnVlLAogICJ0ZXh0dXJlcyIgOiB7CiAgICAiU0tJTiIgOiB7CiAgICAgICJ1cmwiIDogImh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNDA4MjRkMTgwNzkwNDJkNTc2OWYyNjRmNDQzOTRiOTViOWI5OWNlNjg5Njg4Y2MxMGM5ZWVjM2Y4ODJjY2MwOCIKICAgIH0KICB9Cn0=", "Pomegranate"),
            Map.entry("ewogICJ0aW1lc3RhbXAiIDogMTcxOTMyMDkyODM3MiwKICAicHJvZmlsZUlkIiA6ICJmNzg5OWI1ZGEzZGM0ZTY0YmFlM2QyMmYzMWFjMzBhZSIsCiAgInByb2ZpbGVOYW1lIiA6ICJwaXhlbGJsb2IxMjEyIiwKICAic2lnbmF0dXJlUmVxdWlyZWQiIDogdHJ1ZSwKICAidGV4dHVyZXMiIDogewogICAgIlNLSU4iIDogewogICAgICAidXJsIiA6ICJodHRwOi8vdGV4dHVyZXMubWluZWNyYWZ0Lm5ldC90ZXh0dXJlLzNjYzc2MWJjYjA1Nzk3NjNkOWI4YWI2YjdiOTZmYTc3ZWI2ZDk2MDVhODA0ZDgzOGZlYzM5ZTdiMjVmOTU1OTEiCiAgICB9CiAgfQp9", "Dragonfruit"),
            Map.entry("ewogICJ0aW1lc3RhbXAiIDogMTcxOTMyMDk2NDE1NSwKICAicHJvZmlsZUlkIiA6ICI4ZTFjZTM2ZGE2Mzk0ZjgwOTFmZjZjYTZiZTNhZTA5NSIsCiAgInByb2ZpbGVOYW1lIiA6ICJGdWxsY3JlbiIsCiAgInNpZ25hdHVyZVJlcXVpcmVkIiA6IHRydWUsCiAgInRleHR1cmVzIiA6IHsKICAgICJTS0lOIiA6IHsKICAgICAgInVybCIgOiAiaHR0cDovL3RleHR1cmVzLm1pbmVjcmFmdC5uZXQvdGV4dHVyZS9mMzYzYTYyMTI2YTM1NTM3ZjgxODkzNDNhMjI2NjBkZTc1ZTgxMGM2YWMwMDRhN2QzZGE2NWYxYzA0MGE4MzkiCiAgICB9CiAgfQp9", "Mango"),
            Map.entry("ewogICJ0aW1lc3RhbXAiIDogMTYyNDU0NjA4NjAxNiwKICAicHJvZmlsZUlkIiA6ICI0NWY3YTJlNjE3ODE0YjJjODAwODM5MmRmN2IzNWY0ZiIsCiAgInByb2ZpbGVOYW1lIiA6ICJfSnVzdERvSXQiLAogICJzaWduYXR1cmVSZXF1aXJlZCIgOiB0cnVlLAogICJ0ZXh0dXJlcyIgOiB7CiAgICAiU0tJTiIgOiB7CiAgICAgICJ1cmwiIDogImh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYTc2YTI4MTFkMWUxNzZhMDdiNmQwYTY1N2I5MTBmMTM0ODk2Y2UzMDg1MGY2ZTgwYzdjODM3MzJkODUzODFlYSIKICAgIH0KICB9Cn0=", "Bomb"),
            Map.entry("ewogICJ0aW1lc3RhbXAiIDogMTcxODIwMzIzMzA1NiwKICAicHJvZmlsZUlkIiA6ICIxODA1Y2E2MmM0ZDI0M2NiOWQxYmY4YmM5N2E1YjgyNCIsCiAgInByb2ZpbGVOYW1lIiA6ICJSdWxsZWQiLAogICJzaWduYXR1cmVSZXF1aXJlZCIgOiB0cnVlLAogICJ0ZXh0dXJlcyIgOiB7CiAgICAiU0tJTiIgOiB7CiAgICAgICJ1cmwiIDogImh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNDA3YjI3NWQyOGI5MjdiMWJmN2Y2ZGQ5ZjQ1ZmJkYWQyYWY4NTcxYzU0YzhmMDI3ZDFiZmY2OTU2ZmJmM2MxNiIKICAgIH0KICB9Cn0=", "Rum")
    );

    private static final int AREA_X_MIN = -113, AREA_X_MAX = -105;
    private static final int AREA_Z_MIN = 18, AREA_Z_MAX = 26;
    private static final int AREA_Y_MIN = 71, AREA_Y_MAX = 76;

    private static final List<Marker> fruits = new ArrayList<>();
    private static final List<Marker> treasures = new ArrayList<>();
    private static final List<Marker> bombs = new ArrayList<>();
    private static int digX, digZ;
    private static boolean hasDigLoc;
    private static long lastNpcDialogTime;
    private static String acceptCommand;

    // ── Solver state ──
    /** 7×7 棋盘左上角世界方块 X 坐标 */
    private static final int GRID_ORIGIN_X = -112;
    /** 7×7 棋盘左上角世界方块 Z 坐标 */
    private static final int GRID_ORIGIN_Z = 19;
    /** 当前最优推荐 */
    private static FruitDiggingSolver.Move bestMove;
    /** 已使用的挖掘次数 */
    private static int digsUsed;
    /** 是否正在游戏中（收到铲子后为 true） */
    private static boolean gameActive;

    /** 方块坐标 → 网格坐标（用于 digX/digZ 和 bomb/treasure marker） */
    private static int blockToGridX(int bx) { return bx - GRID_ORIGIN_X; }
    private static int blockToGridZ(int bz) { return bz - GRID_ORIGIN_Z; }

    /** 水果 Marker 坐标 → 网格坐标（m.x = floor(itemX)+1，需先减 1 还原方块坐标） */
    private static int fruitMarkerToGridX(int mx) { return (mx - 1) - GRID_ORIGIN_X; }
    private static int fruitMarkerToGridZ(int mz) { return (mz - 1) - GRID_ORIGIN_Z; }

    /** 网格坐标 → 世界方块中心坐标 */
    private static double toWorldX(int gx) { return GRID_ORIGIN_X + gx + 0.5; }
    private static double toWorldZ(int gz) { return GRID_ORIGIN_Z + gz + 0.5; }

    /** 清除 BoardState 中所有指向指定水果的 treasureAdjacent */
    private static void clearTreasureHints(FruitDiggingSolver.FruitType ft, FruitDiggingSolver.BoardState state) {
        for (int x = 0; x < 7; x++)
            for (int z = 0; z < 7; z++)
                if (state.grid[x][z].treasureAdjacent == ft)
                    state.grid[x][z].treasureAdjacent = null;
    }

    private FruitDiggingModule() {}

    public static void init() {
        AttackBlockCallback.EVENT.register((player, world, hand, pos, direction) -> {
            if (!ModConfigManager.get().events.carnival.fruitDiggingHelper) return InteractionResult.PASS;
            if (!isInCarnival()) return InteractionResult.PASS;
            if (!isInDigArea(pos.getX(), pos.getY(), pos.getZ())) return InteractionResult.PASS;
            digX = pos.getX();
            digZ = pos.getZ();
            hasDigLoc = true;
            digsUsed++;
            return InteractionResult.PASS;
        });

        net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            if (overlay) return;
            if (!ModConfigManager.get().events.carnival.fruitDiggingHelper) return;
            if (!isInCarnival() || !hasDigLoc) return;
            String text = ChatUtils.stripColor(message.getString());

            Matcher m;
            m = Pattern.compile("TREASURE! There is a[n]? ([a-zA-Z]+) nearby\\.").matcher(text);
            if (m.matches()) {
                treasures.add(new Marker(digX, digZ, Component.translatable("babyzombieaddons.fruitdigging.treasure_nearby", m.group(1)).getString()));
                hasDigLoc = false;
                return;
            }
            if (text.matches("(ANCHOR|TREASURE)! There are no fruits nearby!")) {
                treasures.add(new Marker(digX, digZ, Component.translatable("babyzombieaddons.fruitdigging.no_fruit").getString()));
                hasDigLoc = false;
                return;
            }
            m = Pattern.compile("MINES! There (is|are) ([0-9]+) bomb[s]? hidden nearby\\.").matcher(text);
            if (m.matches()) {
                bombs.add(new Marker(digX, digZ, Component.translatable("babyzombieaddons.fruitdigging.bombs_nearby", m.group(2)).getString()));
                hasDigLoc = false;
            }
        });

        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            if (overlay) return;
            if (!ModConfigManager.get().events.carnival.fruitDiggingHelper) return;
            if (!HypixelLocationTracker.getInstance().isInSkyblock()) return;
            String text = ChatUtils.stripColor(message.getString());

            // 游戏开始：只响应铲子消息，不用 "Fruit Digging"（会和结束画面混淆）
            if (text.equals("[NPC] Carnival Pirateman: Here's yer shovel, then.")) {
                fruits.clear();
                treasures.clear();
                bombs.clear();
                hasDigLoc = false;
                acceptCommand = null;
                bestMove = null;
                digsUsed = 0;
                gameActive = true;
                FruitDiggingSolver.huntIndex = 0;
            }

            // 游戏结束：计分板出现分数摘要
            if (gameActive && text.contains("Your Score")) {
                gameActive = false;
                bestMove = null;
                fruits.clear();
                treasures.clear();
                bombs.clear();
            }
        });

        // Auto-accept: capture accept command from options message
        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            if (overlay) return;
            if (!ModConfigManager.get().events.carnival.autoAccept) return;
            if (!HypixelLocationTracker.getInstance().isInSkyblock()) return;
            String text = ChatUtils.stripColor(message.getString());

            if (text.contains("[NPC] Carnival Pirateman: Would ye like to do some Fruit Digging?")) {
                lastNpcDialogTime = System.currentTimeMillis();
                acceptCommand = null;
                return;
            }
            if (text.contains("[NPC] Carnival Fisherman: Are you here to play?")) {
                lastNpcDialogTime = System.currentTimeMillis();
                acceptCommand = null;
                return;
            }
            if (text.contains("[NPC] Carnival Cowboy: Wouldja like to play Zombie Shootout?")) {
                lastNpcDialogTime = System.currentTimeMillis();
                acceptCommand = null;
                return;
            }

            if (text.contains("Select an option:")) {
                if (text.contains("[Aye sure do!]")) acceptCommand = findClickCommand(message, "Aye sure do!");
                else if (text.contains("[You guessed it!]")) acceptCommand = findClickCommand(message, "You guessed it!");
                else if (text.contains("[Sure thing, partner!]")) acceptCommand = findClickCommand(message, "Sure thing, partner!");
            }
        });

        // Right-click on NPC within 2s → cancel and auto-accept
        UseEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (!ModConfigManager.get().events.carnival.autoAccept) return InteractionResult.PASS;
            if (acceptCommand == null) return InteractionResult.PASS;
            if (System.currentTimeMillis() - lastNpcDialogTime > 2000) {
                acceptCommand = null;
                return InteractionResult.PASS;
            }
            if (entity instanceof net.minecraft.world.entity.player.Player) {
                ChatUtils.sendCommand(acceptCommand);
                acceptCommand = null;
                return InteractionResult.FAIL;
            }
            return InteractionResult.PASS;
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (!ModConfigManager.get().events.carnival.fruitDiggingHelper) return;
            if (!isInCarnival()) {
                gameActive = false;
                return;
            }
            if (client.player == null || client.player.tickCount % 10 != 0) return;

            var items = client.player.level().getEntitiesOfClass(ItemEntity.class,
                    new AABB(AREA_X_MIN, AREA_Y_MIN, AREA_Z_MIN, AREA_X_MAX, AREA_Y_MAX, AREA_Z_MAX));

            for (var item : items) {
                var stack = item.getItem();
                if (stack.getItem() != Items.PLAYER_HEAD) continue;

                int bx = (int) Math.floor(item.getX());
                int bz = (int) Math.floor(item.getZ());
                boolean dug = client.player.level()
                        .getBlockState(new net.minecraft.core.BlockPos(bx, 72, bz)).getBlock() != Blocks.SAND;

                String textureValue = ItemUtils.getSkullTexture(stack);
                if (textureValue == null) continue;
                String fruitName = FRUITS.get(textureValue);
                if (fruitName == null) continue;

                String label = (dug ? "§a" : "§e") + fruitName;
                int fx = (int) Math.floor(item.getX());
                int fz = (int) Math.floor(item.getZ());
                fruits.removeIf(m -> m.x == fx + 1 && m.z == fz + 1);
                fruits.add(new Marker(fx + 1, fz + 1, label));
            }

            // ── Solver：综合 fruits + bombs + treasures + dugTiles 构建 BoardState ──
            boolean solverEnabled = ModConfigManager.get().events.fruitDiggingSolver.enabled && gameActive;
            FruitDiggingSolver.BoardState state = new FruitDiggingSolver.BoardState();
            int totalKnown = 0;

            if (solverEnabled) {
                // 0. 直接扫描 49 格方块状态（y=72）
                //    沙子 = 未挖 | 砂岩 = 已挖 | 砂岩台阶 = 被炸弹炸毁
                for (int gx = 0; gx < 7; gx++) {
                    for (int gz = 0; gz < 7; gz++) {
                        int bx = GRID_ORIGIN_X + gx;
                        int bz = GRID_ORIGIN_Z + gz;
                        var block = client.player.level()
                                .getBlockState(new net.minecraft.core.BlockPos(bx, 72, bz)).getBlock();
                        if (block != Blocks.SAND) {
                            state.grid[gx][gz].revealed = true;
                            state.grid[gx][gz].treasureAdjacent = null; // 已挖，候选提示失效
                        }
                    }
                }

                // 1. 从 fruits 列表读取：已知水果类型 + 累积计数
                for (var m : fruits) {
                    int gx = fruitMarkerToGridX(m.x);
                    int gz = fruitMarkerToGridZ(m.z);
                    if (gx < 0 || gx >= 7 || gz < 0 || gz >= 7) continue;

                    String name = ChatUtils.stripColor(m.label);
                    FruitDiggingSolver.FruitType ft = FruitDiggingSolver.fromScannerName(name);
                    if (ft == null) continue; // Bomb/Rum 不算水果

                    if (m.label.startsWith("§a")) {
                        // 已挖水果 → 更新累积计数 + 减少剩余库存 + 清除对应的宝藏提示
                        state.remaining[ft.ordinal()] = Math.max(0, state.remaining[ft.ordinal()] - 1);
                        if (ft == FruitDiggingSolver.FruitType.APPLE) state.applesTriggered++;
                        if (ft == FruitDiggingSolver.FruitType.CHERRY) state.cherriesTriggered++;
                        // 此水果已找到，清除全局 treasureAdjacent 提示
                        for (int x = 0; x < 7; x++)
                            for (int z = 0; z < 7; z++)
                                if (state.grid[x][z].treasureAdjacent == ft)
                                    state.grid[x][z].treasureAdjacent = null;
                    } else if (!state.grid[gx][gz].revealed) {
                        // 未挖水果 → 已知位置
                        state.grid[gx][gz].fruit = ft;
                        state.remaining[ft.ordinal()] = Math.max(0, state.remaining[ft.ordinal()] - 1);
                        totalKnown++;
                    }
                }

                // 龙果/榴莲已挖完 → 强制清空对应宝藏提示
                if (state.remaining[FruitDiggingSolver.FruitType.DRAGONFRUIT.ordinal()] <= 0)
                    clearTreasureHints(FruitDiggingSolver.FruitType.DRAGONFRUIT, state);
                if (state.remaining[FruitDiggingSolver.FruitType.DURIAN.ordinal()] <= 0)
                    clearTreasureHints(FruitDiggingSolver.FruitType.DURIAN, state);

                // 2. 从 bombs 列表（marker.x = digX = 方块坐标，用 blockToGridX）
                for (var b : bombs) {
                    int gx = blockToGridX(b.x);
                    int gz = blockToGridZ(b.z);
                    if (gx < 0 || gx >= 7 || gz < 0 || gz >= 7) continue;
                    state.grid[gx][gz].hasMinesInfo = true;
                    String stripped = ChatUtils.stripColor(b.label);
                    java.util.regex.Matcher bm = java.util.regex.Pattern.compile("(\\d+)").matcher(stripped);
                    if (bm.find()) {
                        state.grid[gx][gz].minesCount = Integer.parseInt(bm.group(1));
                    }
                }

                // 3. 从 treasures 列表 + 传播到相邻格
                for (var t : treasures) {
                    int gx = blockToGridX(t.x);
                    int gz = blockToGridZ(t.z);
                    if (gx < 0 || gx >= 7 || gz < 0 || gz >= 7) continue;
                    state.grid[gx][gz].hasTreasureInfo = true;
                    String stripped = ChatUtils.stripColor(t.label);
                    FruitDiggingSolver.FruitType hinted = null;
                    if (stripped.contains("附近有 ")) {
                        for (FruitDiggingSolver.FruitType ft : FruitDiggingSolver.FruitType.values()) {
                            if (stripped.contains(ft.englishName)) {
                                state.grid[gx][gz].treasureHint = ft;
                                hinted = ft;
                                break;
                            }
                        }
                    }
                    // 传播：标记相邻未知格（保留最高分水果提示）
                    if (hinted != null) {
                        for (int dx = -1; dx <= 1; dx++) {
                            for (int dz = -1; dz <= 1; dz++) {
                                if (dx == 0 && dz == 0) continue;
                                int nx = gx + dx, nz = gz + dz;
                                if (nx < 0 || nx >= 7 || nz < 0 || nz >= 7) continue;
                                if (state.grid[nx][nz].isUnknown() && !state.grid[nx][nz].revealed) {
                                    // 保留价值更高的提示
                                    if (state.grid[nx][nz].treasureAdjacent == null
                                            || hinted.basePoints > state.grid[nx][nz].treasureAdjacent.basePoints) {
                                        state.grid[nx][nz].treasureAdjacent = hinted;
                                    }
                                }
                            }
                        }
                    }
                }

                state.digsRemaining = 15 - digsUsed;

                // 注入运行参数
                var cfg = ModConfigManager.get().events;
                var params = FruitDiggingSolver.getParams();
                params.bombPenalty = cfg.fruitDiggingSolver.bombPenalty;
                params.rumPenalty = cfg.fruitDiggingSolver.rumPenalty;
                params.minesInfoWeight = cfg.fruitDiggingSolver.minesInfoWeight;
                params.treasureInfoWeight = cfg.fruitDiggingSolver.treasureInfoWeight;
                params.anchorInfoWeight = cfg.fruitDiggingSolver.anchorInfoWeight;
                params.earlyAppleBonus = cfg.fruitDiggingSolver.earlyAppleBonus;
                params.earlyCherryBonus = cfg.fruitDiggingSolver.earlyCherryBonus;
                params.lateGameDigs = cfg.fruitDiggingSolver.lateGameDigs;

                bestMove = FruitDiggingSolver.findBestMove(state);
            } else {
                bestMove = null;
            }
        });

        RenderPhaseRegister.register(ctx -> {
            if (!ModConfigManager.get().events.carnival.fruitDiggingHelper) return;
            boolean inCarnival = isInCarnival();
            if (!inCarnival) return;

            // ── 信标光柱：Solver 推荐位置 ──
            if (ModConfigManager.get().events.fruitDiggingSolver.enabled && bestMove != null) {
                double wx = toWorldX(bestMove.gridX);
                double wz = toWorldZ(bestMove.gridZ);
                int color = dowsingColor(bestMove.dowsing);

                // 光柱：沙面往上 7 格
                BeamRenderer.drawBeam(ctx, wx, 72.0, wz, 7.0, 0.15f, color);

                // 光柱顶端：探测模式文字
                String modeText = switch (bestMove.dowsing) {
                    case MINES -> "§cMINES";
                    case TREASURE -> "§6TREASURE";
                    case ANCHOR -> "§9ANCHOR";
                };
                WorldTextRenderer.renderString(ctx, modeText + " §7[" + String.format("%.0f", bestMove.score) + "]",
                        wx, 79.5, wz, color, 0.04f, false);
            }

            // ── 已有标记渲染 ──
            int total = fruits.size() + treasures.size() + bombs.size();
            if (total == 0) return;

            for (var m : bombs)
                WorldTextRenderer.renderString(ctx, m.label, m.x + 0.5, 74.7, m.z + 0.5, 0xFFFF5555, 0.025f, false);
            for (var m : treasures)
                WorldTextRenderer.renderString(ctx, m.label, m.x + 0.5, 74.5, m.z + 0.5, 0xFFFFFF55, 0.025f, false);
            for (var m : fruits)
                WorldTextRenderer.renderString(ctx, m.label, m.x - 0.5, 74.3, m.z - 0.5, 0xFFFFFFFF, 0.025f, false);
        });
    }

    private static boolean isInCarnival() {
        var tracker = HypixelLocationTracker.getInstance();
        return tracker.isInSkyblock() && "Carnival".equals(tracker.getLocation());
    }

    /** 统计已挖的某种水果数量（从 fruits 列表中已挖标记统计） */
    private static int countDugFruit(String name) {
        int count = 0;
        for (var m : fruits) {
            if (m.label.startsWith("§a") && m.label.contains(name)) count++;
        }
        return count;
    }

    /** 探测模式 → ARGB 信标颜色 */
    private static int dowsingColor(FruitDiggingSolver.DowsingMode mode) {
        return switch (mode) {
            case MINES -> 0xFFFF5555;    // 红
            case TREASURE -> 0xFFFFAA00;  // 金
            case ANCHOR -> 0xFF5555FF;    // 蓝
        };
    }

    private static boolean isInDigArea(double x, double y, double z) {
        return x >= AREA_X_MIN && x <= AREA_X_MAX
                && z >= AREA_Z_MIN && z <= AREA_Z_MAX
                && y >= AREA_Y_MIN && y <= AREA_Y_MAX;
    }

    private static String findClickCommand(Component component, String targetText) {
        var clickEvent = component.getStyle().getClickEvent();
        if (clickEvent != null && component.getString().contains(targetText)) {
            if (clickEvent instanceof ClickEvent.RunCommand runCommand) {
                return runCommand.command();
            }
        }
        for (var sibling : component.getSiblings()) {
            String result = findClickCommand(sibling, targetText);
            if (result != null) return result;
        }
        return null;
    }

    private record Marker(int x, int z, String label) {}
}
