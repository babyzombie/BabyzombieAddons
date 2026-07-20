package top.babyzombie.addons.module.events;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.world.inventory.AbstractContainerMenu;
import top.babyzombie.addons.config.ModConfigManager;
import top.babyzombie.addons.config.hud.HudManager;
import top.babyzombie.addons.util.ChatUtils;
import top.babyzombie.addons.util.tracker.HypixelLocationTracker;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 周年庆 Raffle Tasks 追踪器。
 * <ul>
 *   <li>打开 "Raffle Tasks" 容器时解析 21 个任务</li>
 *   <li>监听聊天消息标记已完成任务</li>
 *   <li>在 HUD 上显示未完成的任务目标</li>
 * </ul>
 */
public final class RaffleTaskModule {

    // RAFFLE TASK! You completed the Diamond Collector raffle task and earned +1 Raffle Ticket and a slice of cake!
    private static final Pattern COMPLETION_PATTERN = Pattern.compile(
            "RAFFLE TASK! You completed the (.+) raffle task and earned \\+1 Raffle Ticket");

    // lore[0] stripped → 内部标识，用于聊天消息匹配
    private static final List<String> taskNames = new ArrayList<>();
    // lore[3..size-2] joined → 任务目标描述，HUD 显示用
    private static final List<String> taskDescriptions = new ArrayList<>();
    private static final boolean[] completed = new boolean[21];
    private static String hudText;
    private static boolean pendingParse;
    private static AbstractContainerScreen<?> pendingScreen;
    // 断线倒计时：-1=未断线，>=0=已断线的 tick 数，超过 100 判定为真正离开服务器
    private static int disconnectCountdown = -1;

    private RaffleTaskModule() {}

    public static void init() {
        // 1. 检测 "Raffle Tasks" 容器打开
        ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            if (!ModConfigManager.get().events.anniversary.raffleTaskTracker) return;
            if (!HypixelLocationTracker.getInstance().isInSkyblock()) return;
            if (!(screen instanceof AbstractContainerScreen<?> cs)) return;

            String title = ChatUtils.stripColor(screen.getTitle().getString());
            if (!"Raffle Tasks".equals(title)) return;

            if (cs.getMenu().slots.size() < 54) return;

            pendingParse = true;
            pendingScreen = cs;
        });

        // 2. 等待容器物品加载完成后解析 + 断线检测
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            // 断线倒计时：DISCONNECT 后每 tick +1，超过 5 秒没重连视为真正离开服务器
            if (disconnectCountdown >= 0) {
                disconnectCountdown++;
                if (disconnectCountdown > 100) {
                    taskNames.clear();
                    taskDescriptions.clear();
                    hudText = null;
                    pendingParse = false;
                    pendingScreen = null;
                    disconnectCountdown = -1;
                }
            }

            if (!pendingParse || pendingScreen == null) return;

            var cs = pendingScreen;
            // 容器在加载前关闭
            if (client.gui.screen() != cs) {
                pendingParse = false;
                pendingScreen = null;
                return;
            }

            var menu = cs.getMenu();
            // 等待第四排玻璃（slot 35）加载完成
            if (menu.slots.get(35).getItem().isEmpty()) return;

            parseTasks(menu);
            pendingParse = false;
            pendingScreen = null;
        });

        // 3. 监听聊天中的任务完成消息
        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            if (overlay) return;
            if (!ModConfigManager.get().events.anniversary.raffleTaskTracker) return;
            if (!HypixelLocationTracker.getInstance().isInSkyblock()) return;
            if (taskNames.isEmpty()) return;

            String text = ChatUtils.stripColor(message.getString());
            Matcher matcher = COMPLETION_PATTERN.matcher(text);
            if (!matcher.find()) return;

            String completedName = matcher.group(1).trim();
            boolean changed = false;
            for (int i = 0; i < taskNames.size(); i++) {
                if (!completed[i] && taskNames.get(i).equalsIgnoreCase(completedName)) {
                    completed[i] = true;
                    changed = true;
                    break;
                }
            }

            if (changed) {
                rebuildHudText();
                if (allCompleted()) {
                    ChatUtils.showMessage("§a§lAll Raffle Tasks completed!");
                }
            }
        });

        // 4. 真正离开服务器时重置（Hypixel 子服切换不重置）
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            // 开始倒计时，如果 5 秒内未重连则判定为真正离开
            disconnectCountdown = 0;
        });

        ClientPlayConnectionEvents.JOIN.register((handler, packetSender, client) -> {
            // 重连成功（子服切换），取消倒计时
            disconnectCountdown = -1;
        });

        // 5. HUD 渲染
        HudElementRegistry.attachElementAfter(VanillaHudElements.OVERLAY_MESSAGE,
                Identifier.fromNamespaceAndPath("babyzombieaddons", "raffle_tasks"),
                (context, tickCounter) -> {
            if (!ModConfigManager.get().events.anniversary.raffleTaskTracker) return;
            if (!HypixelLocationTracker.getInstance().isInSkyblock()) return;
            if (hudText == null) return;

            var font = Minecraft.getInstance().font;
            int x = HudManager.x("RaffleTasks");
            int y = HudManager.y("RaffleTasks");
            float s = HudManager.scale("RaffleTasks");
            HudManager.drawScaled(context, font, hudText, x, y, s);
        });
    }

    /**
     * 解析 Raffle Tasks 容器中的任务数据。
     * <p>
     * 任务名从物品显示名获取（格式：§f[§aDiamond Collector§f]）。
     * DataComponents.LORE 不含物品名，结构：
     * <pre>
     *   lines[0]      → 难度（如 "Easy Task"）
     *   lines[1]      → 空行
     *   lines[2..N-3] → 任务目标描述（HUD 显示用，可能多行）
     *   lines[N-2]    → 空行
     *   lines[N-1]    → "COMPLETE" 或 "INCOMPLETE"
     * </pre>
     */
    private static void parseTasks(AbstractContainerMenu menu) {
        taskNames.clear();
        taskDescriptions.clear();
        Arrays.fill(completed, false);

        int index = 0;
        // Row 2-4 (0-indexed)，每行：玻璃 | 任务×7 | 玻璃
        for (int row = 0; row < 3 && index < 21; row++) {
            int rowStart = 9 + row * 9; // 9, 18, 27
            for (int col = 1; col <= 7 && index < 21; col++) {
                int slotIdx = rowStart + col;
                var stack = menu.slots.get(slotIdx).getItem();
                if (stack.isEmpty()) {
                    index++;
                    continue;
                }

                // 任务名：从物品显示名获取（格式 §f[§aDiamond Collector§f]）
                String rawName = ChatUtils.stripColor(stack.getHoverName().getString());
                String name = rawName.replace("[", "").replace("]", "").trim();
                if (name.isEmpty()) {
                    index++;
                    continue;
                }
                taskNames.add(name);

                var loreComp = stack.get(DataComponents.LORE);
                if (loreComp == null) {
                    taskDescriptions.add("");
                    index++;
                    continue;
                }

                // 转为去色字符串列表（DataComponents.LORE 不含物品名）
                List<String> lines = new ArrayList<>();
                for (var line : loreComp.lines()) {
                    lines.add(ChatUtils.stripColor(line.getString()));
                }

                if (lines.size() < 3) {
                    taskDescriptions.add("");
                    index++;
                    continue;
                }

                // 从末尾找到 COMPLETE / INCOMPLETE 行
                int statusIdx = -1;
                for (int i = lines.size() - 1; i >= 0; i--) {
                    String l = lines.get(i);
                    if (l.contains("COMPLETE") || l.contains("INCOMPLETE")) {
                        statusIdx = i;
                        // 注意：INCOMPLETE 包含 "COMPLETE" 子串
                        completed[index] = !l.contains("INCOMPLETE");
                        break;
                    }
                }

                if (statusIdx <= 0) {
                    taskDescriptions.add("");
                    index++;
                    continue;
                }

                // 描述结束位置：状态行之前，跳过空行
                int descEnd = statusIdx - 1;
                while (descEnd >= 0 && lines.get(descEnd).isEmpty()) {
                    descEnd--;
                }

                // 描述开始位置：跳过难度(0)、空行(1)
                int descStart = 2;
                while (descStart <= descEnd && lines.get(descStart).isEmpty()) {
                    descStart++;
                }

                // 构建任务目标描述
                StringBuilder desc = new StringBuilder();
                for (int li = descStart; li <= descEnd; li++) {
                    if (!desc.isEmpty()) desc.append(' ');
                    desc.append(lines.get(li));
                }
                taskDescriptions.add(desc.toString());

                index++;
            }
        }

        rebuildHudText();
    }

    private static void rebuildHudText() {
        if (taskDescriptions.isEmpty()) {
            hudText = null;
            return;
        }

        int incomplete = 0;
        for (int i = 0; i < taskDescriptions.size(); i++) {
            if (!completed[i]) incomplete++;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("§6Raffle Tasks§7: §e").append(incomplete)
                .append("§7/").append(taskDescriptions.size()).append(" remaining");

        if (incomplete > 0) {
            for (int i = 0; i < taskDescriptions.size(); i++) {
                if (!completed[i]) {
                    sb.append("\n§7").append(taskDescriptions.get(i));
                }
            }
        } else {
            sb.append("\n§aAll completed!");
        }

        hudText = sb.toString();
    }

    private static boolean allCompleted() {
        if (taskDescriptions.isEmpty()) return false;
        for (int i = 0; i < taskDescriptions.size(); i++) {
            if (!completed[i]) return false;
        }
        return true;
    }
}
