package top.babyzombie.addons.module.misc.bazaar;

import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.SignEditScreen;
import net.minecraft.network.chat.Component;
import top.babyzombie.addons.config.ModConfigManager;
import top.babyzombie.addons.config.SkyblockConfig.BazzarTopOrders;
import top.babyzombie.addons.config.SkyblockConfig.BazzarTopOrders.SignQuickAmount;
import top.babyzombie.addons.mixin.screen.AbstractSignEditScreenAccessor;
import top.babyzombie.addons.mixin.screen.ScreenInvoker;
import top.babyzombie.addons.util.ChatUtils;
import top.babyzombie.addons.util.Scheduler;

import java.util.ArrayList;
import java.util.List;

public final class BazaarSignQuickAmountButtons {

    /** 告示牌箭头行(全字匹配) */
    private static final String SIGN_CARET_LINE = "^^^^^^^^^^^^^^^";

    /** 已安装到当前屏幕的按钮引用,用于移除残留(窗口 resize 重 init 等场景) */
    private static final List<Button> quickButtons = new ArrayList<>();

    private BazaarSignQuickAmountButtons() {}

    public static void init() {
        ScreenEvents.AFTER_INIT.register((client, screen, sw, sh) -> tryAddQuickAmountButtons(screen));
    }

    private static BazzarTopOrders getCfg() {
        try { return ModConfigManager.get().skyblock.bazzarTopOrders; } catch (Exception e) { return null; }
    }

    /** 26.1 起 Screen 的 children(事件)/renderables(渲染)/narratables 是三个独立列表,
     *  只改 children 会"点得动但看不见",必须走 ScreenInvoker 同步加入三个列表 */
    private static void addButtonToScreen(Screen screen, Button button) {
        ((ScreenInvoker) screen).bzaAddRenderableWidget(button);
    }

    private static void removeButtonFromScreen(Screen screen, Button button) {
        ((ScreenInvoker) screen).bzaRemoveWidget(button);
    }

    private static void tryAddQuickAmountButtons(Screen screen) {
        var cfg = getCfg();
        if (cfg == null || !cfg.signQuickAmountsEnabled) return;
        if (!(screen instanceof SignEditScreen signScreen)) return;
        if (cfg.signQuickAmounts == null || cfg.signQuickAmounts.isEmpty()) return;
        // 与剪贴板贴入同节奏:告示牌行内容可能比屏幕打开晚到,延迟重试等待布局可判
        Scheduler.schedule(0, new QuickButtonsTask(signScreen, new ArrayList<>(cfg.signQuickAmounts)));
    }

    /** 布局匹配后在告示牌下方安装一排快捷按钮(最多两行,每行居中);成功返回 true */
    private static boolean installQuickButtons(SignEditScreen screen, List<SignQuickAmount> amounts) {
        String[] messages = ((AbstractSignEditScreenAccessor) screen).messages();
        if (!isAmountSignLayout(messages)) return false;

        // 移除上次可能残留的按钮,保证始终只有一排
        for (Button old : new ArrayList<>(quickButtons)) removeButtonFromScreen(screen, old);
        quickButtons.clear();

        Font font = Minecraft.getInstance().font;
        int btnH = 20;
        int gap = 4;
        int baseY = 174; // 告示牌(66~168)正下方
        int maxRows = 2;
        // 一排最大宽度限制在屏宽与 320 的较小值,按钮集中在画面中部,不会横贯整个屏幕
        int maxRowWidth = Math.min(screen.width - 16, 320);
        int row = 0;
        int yPos = baseY;
        List<Button> fresh = new ArrayList<>();
        int i = 0;
        while (i < amounts.size() && row < maxRows) {
            // 逐行填装:统计这一行能放下的按钮
            int rowW = -gap;
            int count = 0;
            int start = i;
            while (i < amounts.size()) {
                int w = buttonWidth(font, amounts.get(i));
                if (count > 0 && rowW + gap + w > maxRowWidth) break;
                rowW += gap + w;
                count++;
                i++;
            }
            int startX = screen.width / 2 - rowW / 2;
            for (int j = start; j < i; j++) {
                SignQuickAmount quickAmount = amounts.get(j);
                Button b = Button.builder(Component.literal(quickAmount.displayText()),
                        _ -> onQuickAmountClick(screen, quickAmount.amount()))
                        .bounds(startX, yPos, buttonWidth(font, quickAmount), btnH)
                        .build();
                fresh.add(b);
                startX += buttonWidth(font, quickAmount) + gap;
            }
            row++;
            yPos = baseY + row * (btnH + gap);
        }
        fresh.forEach(b -> addButtonToScreen(screen, b));
        quickButtons.addAll(fresh);
        return true;
    }

    private static int buttonWidth(Font font, SignQuickAmount amount) {
        return Math.max(36, font.width(amount.displayText()) + 14);
    }

    private static void onQuickAmountClick(SignEditScreen screen, int amount) {
        ((AbstractSignEditScreenAccessor) screen).messages()[0] = String.valueOf(amount);
        playClickSound();
    }

    /** 匹配 Bazaar 输入数量的告示牌布局:第一行空 / ^^^ 箭头行 / Enter amount / to order 或 to sell(三行全字匹配) */
    private static boolean isAmountSignLayout(String[] messages) {
        if (messages.length < 4) return false;
        if (!lineOf(messages[0]).isEmpty()) return false;
        if (!SIGN_CARET_LINE.equals(lineOf(messages[1]))) return false;
        if (!"Enter amount".equals(lineOf(messages[2]))) return false;
        String last = lineOf(messages[3]);
        return "to order".equals(last) || "to sell".equals(last);
    }

    private static String lineOf(String s) {
        return s == null ? "" : ChatUtils.stripColor(s).trim();
    }

    private static void playClickSound() {
        try {
            var p = Minecraft.getInstance().player;
            if (p != null) p.playSound(net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK.value(), 0.3f, 1.0f);
        } catch (Exception ignored) {}
    }

    /** 告示牌行内容晚到时,带最大尝试次数的按钮安装重试任务 */
    private static final class QuickButtonsTask implements Runnable {
        private static final int MAX_ATTEMPTS = 5;
        private final SignEditScreen screen;
        private final List<SignQuickAmount> amounts;
        private int attempts;

        QuickButtonsTask(SignEditScreen screen, List<SignQuickAmount> amounts) {
            this.screen = screen;
            this.amounts = amounts;
        }

        @Override
        public void run() {
            if (Minecraft.getInstance().screen != screen) return; // 屏幕已关闭/切换,放弃
            if (installQuickButtons(screen, amounts)) return;     // 已安装,结束
            if (++attempts < MAX_ATTEMPTS) Scheduler.schedule(3, this); // 行内容未到,稍后重试
        }
    }
}
