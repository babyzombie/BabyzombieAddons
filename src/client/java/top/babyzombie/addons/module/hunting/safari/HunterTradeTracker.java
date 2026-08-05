package top.babyzombie.addons.module.hunting.safari;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLevelEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.phys.AABB;
import top.babyzombie.addons.config.ModConfigManager;
import top.babyzombie.addons.config.hud.HudManager;
import top.babyzombie.addons.module.chat.PartyModule;
import top.babyzombie.addons.module.chat.popup.PopupEventsModule;
import top.babyzombie.addons.util.ChatUtils;
import top.babyzombie.addons.util.ServerTick;
import top.babyzombie.addons.util.render.RenderPhaseRegister;
import top.babyzombie.addons.util.render.WorldTextRenderer;
import top.babyzombie.addons.util.tracker.HypixelLocationTracker;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Safari 猎手 NPC 交易追踪。
 *
 * <p>四个猎手 NPC（Hunter Billy / Dennis / Harry、Huntress Melissa）会随机要一种物品
 * 换一种碎片。流程：收到 First Interaction 第一句时扫描周围同名盔甲架记坐标，
 * 后续对话动态提取 shard/cost（碎片名以 "Shard" 结尾、所需物品在 "exchange for a ..." /
 * "give m-m-me a ..." 句式里），出现 "Select an option: [Trade] [No thanks]" 时：
 * 弹 popup 事件（同意发送 Trade 的点击命令）、发队伍消息、世界悬浮字记录、HUD 记录。
 * 队伍消息格式 {@code x: {x}, y: {y}, z: {z}, {npc}, want {cost}, offer {shard}}（英文），
 * 队友发来的同样会记位置。已有记录（含队友发的）的 NPC 附近再次触发时只弹 popup。</p>
 */
public final class HunterTradeTracker {

    /** 四个猎手 NPC：名字、First Interaction 第一句 */
    private record HunterNpc(String name, String firstLine) {}

    private static final HunterNpc[] NPCS = {
        new HunterNpc("Hunter Billy", "Hey there!"),
        new HunterNpc("Hunter Dennis", "Brr!"),
        new HunterNpc("Hunter Harry", "Phew!"),
        new HunterNpc("Huntress Melissa", "Heyyy!"),
    };

    // ── 动态提取：碎片名以 "Shard" 结尾，每个词首字母大写（如 Billygoat Shard / Mantis Shrimp Shard）──
    private static final Pattern SHARD_PATTERN = Pattern.compile("\\b([A-Z][a-z]*(?: [A-Z][a-z]*)* Shard)\\b");
    // ── 动态提取：所需物品在 "exchange for a X" / "exchange for, say, a X" / Dennis 的 "give m-m-me a X" 句式里 ──
    private static final Pattern COST_PATTERN = Pattern.compile(
            "(?:exchange for(?:, say, a| a)|give m-m-me a) ([A-Z][A-Za-z ]+?)(?:[.!?…]|$)");

    // ── 队伍消息格式（英文）：x: {x}, y: {y}, z: {z}, {npc}, want {cost}, offer {shard} ──
    private static final Pattern PARTY_PATTERN = Pattern.compile(
            "^x: (-?\\d+), y: (-?\\d+), z: (-?\\d+), ([A-Za-z ]+), want (.+), offer (.+)$");
    private static final Pattern PARTY_NO_POS_PATTERN = Pattern.compile(
            "^([A-Za-z ]+), want (.+), offer (.+)$");

    private static final int DIALOGUE_TIMEOUT_TICKS = 200; // 对话无更新 10 秒后视为结束
    private static final double SAME_NPC_DIST = 8;         // 判定"同一 NPC"的距离
    private static final double SCAN_RADIUS = 32;          // 扫描盔甲架的半径

    /** 记录表：自己或队友发现的猎手交易 */
    private static final List<HunterTrade> records = new ArrayList<>();

    // ── 当前进行中的对话状态 ──
    private static String currentNpc;
    private static BlockPos currentPos;
    private static String currentShard;
    private static String currentCost;
    private static long lastDialogueTick;

    private static final class HunterTrade {
        String npcName;
        BlockPos pos;
        String shard;
        String cost;

        HunterTrade(String npcName, BlockPos pos, String shard, String cost) {
            this.npcName = npcName;
            this.pos = pos;
            this.shard = shard;
            this.cost = cost;
        }
    }

    private HunterTradeTracker() {}

    public static void init() {
        // ── 切换世界时清空所有记录与对话状态 ──
        ClientLevelEvents.AFTER_CLIENT_LEVEL_CHANGE.register((_, _) -> resetAll());

        // ── 消息监听：ALLOW_GAME 确保不被取消的消息也能检测 ──
        ClientReceiveMessageEvents.ALLOW_GAME.register((message, overlay) -> {
            if (overlay) return true;
            if (!ModConfigManager.get().hunting.safari.hunterTrade.enabled) return true;
            if (!HypixelLocationTracker.getInstance().isInSafari()) return true;

            String raw = ChatUtils.stripColor(message.getString());

            // 1. NPC 对话：First Interaction 第一句 → 开始新对话
            for (HunterNpc npc : NPCS) {
                if (raw.startsWith("[NPC] " + npc.name + ": " + npc.firstLine)) {
                    startConversation(npc.name);
                    return true;
                }
            }

            // 2. 对话中：解析 shard/cost，或收到 Trade 选项
            if (currentNpc != null) {
                if (raw.startsWith("[NPC] " + currentNpc + ": ")) {
                    parseOffer(raw);
                    return true;
                }
                if (raw.contains("Select an option:") && raw.contains("[Trade]")) {
                    onTradeOptions(message);
                    return true;
                }
            }

            // 3. 队友的队伍消息（自己发的本地已直接记录，跳过），匹配不到猎手格式就忽略
            var party = PartyModule.PARTY_CHAT.matcher(raw);
            if (party.find()) {
                var self = Minecraft.getInstance().player;
                if (self != null && party.group(1).equals(self.getName().getString())) return true;
                onPartyMessage(ChatUtils.stripColor(party.group(2)).trim());
            }

            return true;
        });

        // ── 世界悬浮字：NPC 位置显示提供/需要的物品 ──
        RenderPhaseRegister.register(ctx -> {
            if (!ModConfigManager.get().hunting.safari.hunterTrade.worldText) return;
            if (!HypixelLocationTracker.getInstance().isInSafari()) return;
            for (HunterTrade t : records) {
                if (t.pos == null) continue;
                double x = t.pos.getX() + 0.5, y = t.pos.getY() + 2.4, z = t.pos.getZ() + 0.5;
                // 悬浮字显示在 NPC 头顶上方：三行（8px 字高 + 9px 行距）共约 1 格高，
                // 多行用 fontYOffset 分行（缩放后像素，9 = 一行行高）
                final float scale = 0.04f;
                WorldTextRenderer.renderString(ctx, t.npcName, x, y, z, 0xFFFFFF55, scale, true, 0);
                if (t.shard != null) {
                    String give = Component.translatable("babyzombieaddons.hunterTrade.give", t.shard).getString();
                    WorldTextRenderer.renderString(ctx, give, x, y, z, 0xFF55FF55, scale, true, 9);
                }
                if (t.cost != null) {
                    String want = Component.translatable("babyzombieaddons.hunterTrade.want", t.cost).getString();
                    WorldTextRenderer.renderString(ctx, want, x, y, z, 0xFFFF5555, scale, true, 18);
                }
            }
        });

        // ── HUD：列出所有记录的猎手交易 ──
        HudElementRegistry.attachElementAfter(VanillaHudElements.OVERLAY_MESSAGE,
                Identifier.fromNamespaceAndPath("babyzombieaddons", "safari_hunter_trade"),
                (context, tickCounter) -> {
            if (!ModConfigManager.get().hunting.safari.hunterTrade.hud) return;
            if (records.isEmpty()) return;

            var font = Minecraft.getInstance().font;
            int x = HudManager.x("SafariHunter"), y = HudManager.y("SafariHunter");
            float s = HudManager.scale("SafariHunter");

            StringBuilder sb = new StringBuilder("§6§l"
                    + Component.translatable("hud.babyzombieaddons.hunterTrade.title").getString());
            for (HunterTrade t : records) {
                sb.append('\n').append("§e").append(t.npcName);
                if (t.pos != null) {
                    sb.append(" §7@ ").append(t.pos.getX()).append(' ').append(t.pos.getY()).append(' ').append(t.pos.getZ());
                }
                String give = Component.translatable("babyzombieaddons.hunterTrade.give", t.shard == null ? "?" : t.shard).getString();
                String want = Component.translatable("babyzombieaddons.hunterTrade.want", t.cost == null ? "?" : t.cost).getString();
                sb.append('\n').append("§a ").append(give).append("  §c").append(want);
            }
            HudManager.drawScaled(context, font, sb.toString(), x, y, s);
        });

        // ── 对话超时清理 ──
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (currentNpc != null && ServerTick.getTime() - lastDialogueTick > DIALOGUE_TIMEOUT_TICKS) {
                resetConversation();
            }
        });
    }

    /** 对话开始：扫描盔甲架记坐标，清空上轮解析结果 */
    private static void startConversation(String npcName) {
        currentNpc = npcName;
        currentShard = null;
        currentCost = null;
        currentPos = scanNpc(npcName);
        lastDialogueTick = ServerTick.getTime();
    }

    /** 从 NPC 消息里动态提取 shard / cost（不依赖白名单，新物品也能识别） */
    private static void parseOffer(String raw) {
        lastDialogueTick = ServerTick.getTime();

        // 物品名可能显示为 [名字] 带括号，去掉再匹配
        String clean = raw.replaceAll("[\\[\\]]", "");
        if (currentShard == null) {
            Matcher m = SHARD_PATTERN.matcher(clean);
            if (m.find()) currentShard = m.group(1);
        }
        if (currentCost == null) {
            Matcher m = COST_PATTERN.matcher(clean);
            if (m.find()) currentCost = m.group(1);
        }
    }

    /** Select an option 出现：弹 popup，未记录过则发队伍消息 + 记位置 */
    private static void onTradeOptions(Component message) {
        var ht = ModConfigManager.get().hunting.safari.hunterTrade;
        HunterNpc npc = findNpc(currentNpc);
        if (npc == null) { resetConversation(); return; }

        // 兜底：还没扫到坐标就再扫一次
        if (currentPos == null) currentPos = scanNpc(npc.name);

        // 弹出事件：同意就发送 Trade 选项上的点击命令
        if (ht.popup) {
            String cmd = findClickCommand(message, "Trade");
            PopupEventsModule.showHunterTrade(npc.name,
                    currentCost == null ? "?" : currentCost,
                    currentShard == null ? "?" : currentShard,
                    cmd == null ? "" : cmd);
        }

        // 已有记录（含队友发的）→ 只弹 popup，不再发队伍消息 / 记位置
        if (!hasRecordNear(npc.name, currentPos)) {
            if (ht.party) {
                sendPartyMessage(npc.name, currentPos, currentCost, currentShard);
            }
            if (currentShard != null || currentCost != null) {
                records.add(new HunterTrade(npc.name, currentPos, currentShard, currentCost));
            }
        }
        resetConversation();
    }

    /** 解析队友的队伍消息并记录 */
    private static void onPartyMessage(String body) {
        Matcher m = PARTY_PATTERN.matcher(body);
        BlockPos pos = null;
        String npc, cost, shard;
        try {
            if (m.find()) {
                pos = new BlockPos(Integer.parseInt(m.group(1)), Integer.parseInt(m.group(2)), Integer.parseInt(m.group(3)));
                npc = m.group(4).trim();
                cost = m.group(5).trim();
                shard = m.group(6).trim();
            } else {
                m = PARTY_NO_POS_PATTERN.matcher(body);
                if (!m.find()) return;
                npc = m.group(1).trim();
                cost = m.group(2).trim();
                shard = m.group(3).trim();
            }
        } catch (NumberFormatException e) {
            return; // 畸形消息直接忽略
        }
        // 只接受四个猎手 NPC 的消息，避免误匹配普通聊天
        if (findNpc(npc) == null) return;
        upsertRecord(npc, pos, shard, cost);
    }

    /** 已有同 NPC 附近记录则更新，否则新增 */
    private static void upsertRecord(String npc, BlockPos pos, String shard, String cost) {
        String s = "?".equals(shard) ? null : shard;
        String c = "?".equals(cost) ? null : cost;
        for (HunterTrade t : records) {
            if (!t.npcName.equals(npc)) continue;
            if (pos == null || t.pos == null
                    || distSq(t.pos, pos) <= SAME_NPC_DIST * SAME_NPC_DIST) {
                if (pos != null) t.pos = pos;
                if (s != null) t.shard = s;
                if (c != null) t.cost = c;
                return;
            }
        }
        records.add(new HunterTrade(npc, pos, s, c));
    }

    private static void sendPartyMessage(String npc, BlockPos pos, String cost, String shard) {
        StringBuilder sb = new StringBuilder();
        if (pos != null) {
            sb.append("x: ").append(pos.getX())
              .append(", y: ").append(pos.getY())
              .append(", z: ").append(pos.getZ()).append(", ");
        }
        sb.append(npc).append(", want ").append(cost == null ? "?" : cost)
          .append(", offer ").append(shard == null ? "?" : shard);
        ChatUtils.sendCommand("pc " + sb);
    }

    /** 该 NPC（坐标附近）是否已有记录，包括队友发的 */
    private static boolean hasRecordNear(String npcName, BlockPos pos) {
        for (HunterTrade t : records) {
            if (!t.npcName.equals(npcName)) continue;
            if (pos == null) return true; // 没扫到坐标，但已有同名记录
            if (t.pos == null) continue;
            if (distSq(t.pos, pos) <= SAME_NPC_DIST * SAME_NPC_DIST) return true;
        }
        return false;
    }

    /** 扫描周围同名盔甲架，返回最近一个的方块坐标 */
    private static BlockPos scanNpc(String npcName) {
        var player = Minecraft.getInstance().player;
        if (player == null) return null;
        var box = new AABB(player.blockPosition()).inflate(SCAN_RADIUS);
        ArmorStand best = null;
        double bestDist = Double.MAX_VALUE;
        for (var stand : player.level().getEntitiesOfClass(ArmorStand.class, box)) {
            String name = ChatUtils.stripColor(stand.getName().getString()).trim();
            if (!name.equals(npcName)) continue;
            double d = stand.distanceToSqr(player);
            if (d < bestDist) { bestDist = d; best = stand; }
        }
        return best == null ? null : best.blockPosition();
    }

    private static HunterNpc findNpc(String name) {
        for (HunterNpc npc : NPCS) {
            if (npc.name.equals(name)) return npc;
        }
        return null;
    }

    /** 递归查找文本为 targetText 的组件的点击命令（参考 FruitDiggingModule） */
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

    private static double distSq(BlockPos a, BlockPos b) {
        double dx = a.getX() - b.getX(), dy = a.getY() - b.getY(), dz = a.getZ() - b.getZ();
        return dx * dx + dy * dy + dz * dz;
    }

    private static void resetConversation() {
        currentNpc = null;
        currentPos = null;
        currentShard = null;
        currentCost = null;
        lastDialogueTick = 0;
    }

    private static void resetAll() {
        records.clear();
        resetConversation();
    }
}
