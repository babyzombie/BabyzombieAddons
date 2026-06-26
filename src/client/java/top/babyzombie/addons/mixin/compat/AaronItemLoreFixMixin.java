package top.babyzombie.addons.mixin.compat;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.component.ItemLore;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;

/**
 * Aaron Mod 的 ItemStackMixin 只检查 line.getSiblings()，
 * 单附魔行的 root component 不在 siblings 里，被漏掉了。
 * <p>
 * 在 ItemLore.lines() 里直接把 root content 包装成 sibling。
 * 因为 lines() 在任何 TooltipProvider 处理之前就被调用，
 * Aaron 的原生染色代码无论何时执行都能正确扫到。
 */
@Mixin(ItemLore.class)
public class AaronItemLoreFixMixin {

    @Unique
    private static final int BLUE = 0x5555FF;
    @Unique
    private static final Pattern ENCHANT_PATTERN = Pattern.compile("^[A-Za-z' -]+ [IVX]+$");

    @ModifyReturnValue(method = "lines", at = @At("RETURN"), require = 0)
    private List<Component> fixRootEnchantMoveToSibling(List<Component> lines) {
        List<Component> newLines = new ArrayList<>(lines.size());
        boolean modified = false;

        for (Component line : lines) {
            if (line.getSiblings().isEmpty() && line instanceof MutableComponent mutable) {
                String text = line.getString().trim();

                if (mutable.getStyle().getColor() != null
                        && mutable.getStyle().getColor().getValue() == BLUE
                        && ENCHANT_PATTERN.matcher(text).matches()) {
                    MutableComponent wrapper = Component.empty().setStyle(line.getStyle());
                    wrapper.append(mutable);
                    newLines.add(wrapper);
                    modified = true;
                    continue;
                }
            }
            newLines.add(line);
        }

        return modified ? newLines : lines;
    }
}
