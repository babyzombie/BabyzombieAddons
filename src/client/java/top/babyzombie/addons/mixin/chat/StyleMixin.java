package top.babyzombie.addons.mixin.chat;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import top.babyzombie.addons.config.ModConfigManager;

/**
 * 当读取 hover event 时，若有 ClickEvent，在 hover 底部追加点击操作说明。
 */
@Mixin(Style.class)
public class StyleMixin {

    @Inject(method = "getHoverEvent", at = @At("RETURN"), cancellable = true)
    private void onGetHoverEvent(CallbackInfoReturnable<HoverEvent> cir) {
        if (!ModConfigManager.get().misc.showClickEventInHover) return;

        ClickEvent clickEvent = ((Style) (Object) this).getClickEvent();
        if (clickEvent == null) return;

        HoverEvent original = cir.getReturnValue();
        Component clickInfo = formatClickEvent(clickEvent)
                .copy().withStyle(ChatFormatting.GRAY);

        Component newHoverValue;
        if (original instanceof HoverEvent.ShowText showText) {
            newHoverValue = Component.empty()
                    .append(showText.value())
                    .append(Component.literal("\n"))
                    .append(clickInfo);
        } else {
            newHoverValue = clickInfo;
        }

        cir.setReturnValue(new HoverEvent.ShowText(newHoverValue));
    }

    @Unique
    private static Component formatClickEvent(ClickEvent event) {
        return switch (event) {
            case ClickEvent.RunCommand c ->
                    Component.translatable("babyzombieaddons.clickhover.run_command", c.command());
            case ClickEvent.OpenUrl c ->
                    Component.translatable("babyzombieaddons.clickhover.open_url", c.uri().toString());
            case ClickEvent.SuggestCommand c ->
                    Component.translatable("babyzombieaddons.clickhover.suggest_command", c.command());
            case ClickEvent.CopyToClipboard c ->
                    Component.translatable("babyzombieaddons.clickhover.copy_to_clipboard", truncateValue(c.value()));
            case ClickEvent.ChangePage c ->
                    Component.translatable("babyzombieaddons.clickhover.change_page", c.page());
            default ->
                    Component.translatable("babyzombieaddons.clickhover.unknown");
        };
    }

    @Unique
    private static String truncateValue(String value) {
        if (value == null || value.isEmpty()) return "";
        int newlineIdx = value.indexOf('\n');
        boolean hasMoreLines = newlineIdx >= 0;
        String firstLine = hasMoreLines ? value.substring(0, newlineIdx) : value;
        if (firstLine.length() > 50) {
            return firstLine.substring(0, 47) + "...";
        }
        return hasMoreLines ? firstLine + "..." : firstLine;
    }
}
