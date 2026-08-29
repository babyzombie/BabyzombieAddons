package top.babyzombie.addons.mixin.screen;

import io.github.notenoughupdates.moulconfig.common.text.StructuredText;
import io.github.notenoughupdates.moulconfig.gui.editors.GuiOptionEditorDraggableList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.HashMap;
import java.util.LinkedHashMap;

/**
 * GuiOptionEditorDraggableList 的 {@code exampleText} 字段用 HashMap 初始化，
 * 而 {@code getRemainingDropDownEntries()} 直接遍历 keySet() 决定下拉候选列表的显示顺序，
 * HashMap 迭代顺序与插入顺序无关（enum 以 identity hash 散列，看似随机乱序）。
 * <p>
 * 将构造函数字段初始化处内联的 {@code new HashMap<>()} 重定向为 LinkedHashMap，
 * 迭代顺序 = 插入顺序（enum 按声明顺序、int 档按配置数组顺序），候选列表显示顺序稳定。
 * 仅影响候选列表显示顺序，用户已保存的 List 顺序不受影响。
 */
@Mixin(value = GuiOptionEditorDraggableList.class, remap = false)
public class GuiOptionEditorDraggableListMixin {

    /**
     * 只重定向 4 参构造函数（字段初始化器所在，3 参构造只是委托调用没有 NEW HashMap），
     * require = 1：若 moulconfig 升级后字节码变化导致注入点消失，启动即报错而不是静默失效。
     */
    @Redirect(
            method = "<init>(Lio/github/notenoughupdates/moulconfig/processor/ProcessedOption;[Ljava/lang/String;ZZ)V",
            at = @At(value = "NEW", target = "Ljava/util/HashMap;"),
            require = 1)
    private static HashMap<Object, StructuredText> useLinkedHashMapForExampleText() {
        return new LinkedHashMap<>();
    }
}