package top.babyzombie.addons.mixin.compat;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.component.ItemLore;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import top.babyzombie.addons.util.tracker.HypixelLocationTracker;

/**
 * Aaron Mod 的 ItemStackMixin 只检查 line.getSiblings()，
 * 单附魔行的 root component 不在 siblings 里，被漏掉了。
 * <p>
 * 在 ItemLore.lines() 里直接把 root content 包装成 sibling。
 * 因为 lines() 在任何 TooltipProvider 处理之前就被调用，
 * Aaron 的原生染色代码无论何时执行都能正确扫到。
 * <p>
 * 性能优化：不在 SkyBlock / 没装 Aaron Mod / 没有蓝色附魔行时直接跳过，
 * 避免每帧每物品都分配 ArrayList 和跑正则。
 */
@Mixin(ItemLore.class)
public class AaronItemLoreFixMixin {

    @Unique
    private static final int BLUE = 0x5555FF;
    @Unique
    private static final Pattern ENCHANT_PATTERN = Pattern.compile("^[A-Za-z' -]+ [IVX]+$");

    @ModifyReturnValue(method = "lines", at = @At("RETURN"), require = 0)
    private List<Component> fixRootEnchantMoveToSibling(List<Component> lines) {
        // 1. 不在 SkyBlock → 不处理
        if (!HypixelLocationTracker.getInstance().isInSkyblock()) return lines;

        // 2. 没装 Aaron Mod → 不需要兼容修复
        if (!FabricLoader.getInstance().isModLoaded("aaron-mod")) return lines;

        // 3. 单遍扫描：先找有没有需要修的蓝色附魔行，同时懒分配新 list
        List<Component> fixed = null;

        for (int i = 0; i < lines.size(); i++) {
            Component line = lines.get(i);

            if (line.getSiblings().isEmpty() && line instanceof MutableComponent mutable) {
                if (mutable.getStyle().getColor() != null
                        && mutable.getStyle().getColor().getValue() == BLUE
                        && ENCHANT_PATTERN.matcher(mutable.getString().trim()).matches()) {
                    // 需要修复：懒分配并补上之前的行
                    if (fixed == null) {
                        fixed = new ArrayList<>(lines.size());
                        for (int j = 0; j < i; j++) {
                            fixed.add(lines.get(j));
                        }
                    }
                    MutableComponent wrapper = Component.empty().setStyle(line.getStyle());
                    wrapper.append(mutable);
                    fixed.add(wrapper);
                    continue;
                }
            }

            if (fixed != null) {
                fixed.add(line);
            }
        }

        return fixed != null ? fixed : lines;
    }
}