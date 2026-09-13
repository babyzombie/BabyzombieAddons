package top.babyzombie.addons.module.misc.sign;

import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.SignEditScreen;
import net.minecraft.network.chat.Component;
import top.babyzombie.addons.mixin.screen.AbstractSignEditScreenAccessor;
import top.babyzombie.addons.mixin.screen.ScreenInvoker;
import top.babyzombie.addons.util.ChatUtils;
import top.babyzombie.addons.util.Scheduler;

import java.util.ArrayList;
import java.util.List;

/** 告示牌快捷按钮公共引擎:bazaar 数量按钮与 bank 金额按钮共用安装/布局/重试/注入逻辑 */
public final class SignQuickButtons {

    /** 按钮规格:按钮显示文本 + 点击后填入告示牌第一行的文本 */
    public record Spec(String display, String fill) {}

    /** 一个快捷按钮功能的描述:开关/告示牌匹配器/按钮列表/最大行数 */
    public interface Feature {
        boolean enabled();
        /** 按钮列表,空列表表示不安装 */
        List<Spec> specs();
        /** 告示牌身份匹配,匹配成功才安装按钮 */
        boolean matches(String[] messages);
        /** 按钮最大行数 */
        int maxRows();
        /** 点击按钮填入后是否自动关闭告示牌完成提交 */
        boolean autoCloseSign();
    }

    /** 已安装到当前屏幕的按钮引用,用于移除残留(窗口 resize 重 init 等场景) */
    private static final List<Button> quickButtons = new ArrayList<>();

    private SignQuickButtons() {}

    public static void register(Feature feature) {
        ScreenEvents.AFTER_INIT.register((client, screen, sw, sh) -> tryInstall(screen, feature));
    }

    /** 去色并去除首尾空白,供各 feature 匹配告示牌固定内容 */
    public static String plainLine(String s) {
        return s == null ? "" : ChatUtils.stripColor(s).trim();
    }

    private static void tryInstall(Screen screen, Feature feature) {
        if (!(screen instanceof SignEditScreen signScreen)) return;
        if (!feature.enabled()) return;
        List<Spec> specs = feature.specs();
        if (specs.isEmpty()) return;
        // 与剪贴板贴入同节奏:告示牌行内容可能比屏幕打开晚到,延迟重试等待布局可判
        Scheduler.schedule(0, new InstallTask(signScreen, feature, new ArrayList<>(specs)));
    }

    /** 布局匹配后在告示牌下方安装快捷按钮(最多 maxRows 行,每行居中);成功返回 true */
    private static boolean install(SignEditScreen screen, Feature feature, List<Spec> specs) {
        String[] messages = ((AbstractSignEditScreenAccessor) screen).messages();
        if (!feature.matches(messages)) return false;

        // 移除上次可能残留的按钮,保证始终只有一排
        for (Button old : new ArrayList<>(quickButtons)) removeButtonFromScreen(screen, old);
        quickButtons.clear();

        Font font = Minecraft.getInstance().font;
        int btnH = 20;
        int gap = 4;
        int baseY = 174; // 告示牌(66~168)正下方
        int maxRows = feature.maxRows();
        // 一排最大宽度限制在屏宽与 320 的较小值,按钮集中在画面中部,不会横贯整个屏幕
        int maxRowWidth = Math.min(screen.width - 16, 320);
        int row = 0;
        int yPos = baseY;
        List<Button> fresh = new ArrayList<>();
        int i = 0;
        while (i < specs.size() && row < maxRows) {
            // 逐行填装:统计这一行能放下的按钮
            int rowW = -gap;
            int count = 0;
            int start = i;
            while (i < specs.size()) {
                int w = buttonWidth(font, specs.get(i));
                if (count > 0 && rowW + gap + w > maxRowWidth) break;
                rowW += gap + w;
                count++;
                i++;
            }
            int startX = screen.width / 2 - rowW / 2;
            for (int j = start; j < i; j++) {
                Spec spec = specs.get(j);
                Button b = Button.builder(Component.literal(spec.display()),
                                _ -> onQuickClick(screen, spec.fill(), feature))
                        .bounds(startX, yPos, buttonWidth(font, spec), btnH)
                        .build();
                fresh.add(b);
                startX += buttonWidth(font, spec) + gap;
            }
            row++;
            yPos = baseY + row * (btnH + gap);
        }
        fresh.forEach(b -> addButtonToScreen(screen, b));
        quickButtons.addAll(fresh);
        return true;
    }

    private static int buttonWidth(Font font, Spec spec) {
        return Math.max(36, font.width(spec.display()) + 14);
    }

    private static void onQuickClick(SignEditScreen screen, String fill, Feature feature) {
        ((AbstractSignEditScreenAccessor) screen).messages()[0] = fill;
        playClickSound();
        // 自动确认:onClose → removed() 把第一行发给服务器,与剪贴板贴入自动确认同节奏
        if (feature.autoCloseSign()) Scheduler.schedule(1, screen::onClose);
    }

    /** 26.1 起 Screen 的 children(事件)/renderables(渲染)/narratables 是三个独立列表,
     *  只改 children 会"点得动但看不见",必须走 ScreenInvoker 同步加入三个列表 */
    private static void addButtonToScreen(Screen screen, Button button) {
        ((ScreenInvoker) screen).bzaAddRenderableWidget(button);
    }

    private static void removeButtonFromScreen(Screen screen, Button button) {
        ((ScreenInvoker) screen).bzaRemoveWidget(button);
    }

    private static void playClickSound() {
        try {
            var p = Minecraft.getInstance().player;
            if (p != null) p.playSound(net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK.value(), 0.3f, 1.0f);
        } catch (Exception ignored) {}
    }

    /** 告示牌行内容晚到时,带最大尝试次数的按钮安装重试任务 */
    private static final class InstallTask implements Runnable {
        private static final int MAX_ATTEMPTS = 5;
        private final SignEditScreen screen;
        private final Feature feature;
        private final List<Spec> specs;
        private int attempts;

        InstallTask(SignEditScreen screen, Feature feature, List<Spec> specs) {
            this.screen = screen;
            this.feature = feature;
            this.specs = specs;
        }

        @Override
        public void run() {
            if (Minecraft.getInstance().screen != screen) return; // 屏幕已关闭/切换,放弃
            if (install(screen, feature, specs)) return;          // 已安装,结束
            if (++attempts < MAX_ATTEMPTS) Scheduler.schedule(3, this); // 行内容未到,稍后重试
        }
    }
}
