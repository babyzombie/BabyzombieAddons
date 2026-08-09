package top.babyzombie.addons.util.toast;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.client.gui.components.toasts.ToastManager;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * 带物品图标的系统样式 Toast。
 * 样式复刻 SystemToast(黄色标题 + 白色正文),左侧多一个 16x16 物品图标;
 * 超长正文自动按 200px 折行,显示 5 秒。
 */
public class ItemToast implements Toast {

    // 用 advancement 背景而非 system:system 背景自带感叹号图案,会和物品图标重叠;
    // advancement 左侧只有边框装饰,图标区干净
    private static final Identifier BACKGROUND_SPRITE = Identifier.withDefaultNamespace("toast/advancement");
    private static final int MAX_LINE_SIZE = 200;
    private static final int LINE_SPACING = 12;
    private static final int ICON_LEFT = 8;
    private static final int ICON_TOP = 8;
    private static final int TEXT_LEFT = 30;
    private static final long DISPLAY_TIME_MS = 5000L;

    private final ItemStack icon;
    private final List<FormattedCharSequence> lines; // 首行标题,后续为正文
    private final int width;
    private long lastChanged;
    private boolean changed = true;
    private boolean forceHide;
    private Visibility wantedVisibility = Visibility.HIDE;

    public ItemToast(ItemStack icon, Component title, Component message) {
        // 调用方栈可能被后续刷新复用,深拷贝一份
        this.icon = icon.copy();
        Font font = Minecraft.getInstance().font;
        this.lines = new ArrayList<>();
        this.lines.add(title.getVisualOrderText());
        this.lines.addAll(font.split(message, MAX_LINE_SIZE));
        int contentWidth = this.lines.stream().mapToInt(font::width).max().orElse(0);
        this.width = Math.max(Toast.DEFAULT_WIDTH, TEXT_LEFT + contentWidth);
    }

    @Override
    public Visibility getWantedVisibility() {
        return this.wantedVisibility;
    }

    @Override
    public void update(ToastManager manager, long fullyVisibleForMs) {
        if (this.changed) {
            this.lastChanged = fullyVisibleForMs;
            this.changed = false;
        }
        long timeSinceUpdate = fullyVisibleForMs - this.lastChanged;
        this.wantedVisibility = !this.forceHide && timeSinceUpdate < DISPLAY_TIME_MS ? Visibility.SHOW : Visibility.HIDE;
    }

    @Override
    public int width() {
        return this.width;
    }

    @Override
    public int height() {
        // 标题行不占额外高度(基准 1 行),正文每多一行 +12,同 SystemToast 公式
        return 20 + Math.max(this.lines.size() - 1, 1) * LINE_SPACING;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, Font font, long fullyVisibleForMs) {
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, BACKGROUND_SPRITE, 0, 0, width(), height());
        graphics.item(this.icon, ICON_LEFT, ICON_TOP);
        if (this.lines.size() == 1) {
            graphics.text(font, this.lines.get(0), TEXT_LEFT, 12, -256, false);
        } else {
            graphics.text(font, this.lines.get(0), TEXT_LEFT, 7, -256, false);
            for (int i = 1; i < this.lines.size(); i++) {
                graphics.text(font, this.lines.get(i), TEXT_LEFT, 18 + (i - 1) * LINE_SPACING, -1, false);
            }
        }
    }
}
