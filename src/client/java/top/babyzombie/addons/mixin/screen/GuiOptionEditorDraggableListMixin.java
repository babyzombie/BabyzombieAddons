package top.babyzombie.addons.mixin.screen;

import io.github.notenoughupdates.moulconfig.common.text.StructuredText;
import io.github.notenoughupdates.moulconfig.gui.editors.GuiOptionEditorDraggableList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * MoulConfig 的 {@link GuiOptionEditorDraggableList} 用 HashMap 存储候选项文本，
 * 枚举常量以 identity hash 散列，导致下拉候选顺序与枚举声明顺序无关（看似随机乱序）。
 * <p>
 * 在构造完成后按 enumConstants 声明顺序将 exampleText 重建为 LinkedHashMap，
 * 使候选项显示顺序恢复为枚举声明顺序；int 档（String[] exampleText）则按索引升序。
 * 仅影响"候选列表"的显示顺序，用户已保存的 List 顺序不受影响。
 */
@Mixin(value = GuiOptionEditorDraggableList.class, remap = false)
public class GuiOptionEditorDraggableListMixin {

    @Shadow(remap = false)
    private Map<Object, StructuredText> exampleText;

    @Shadow(remap = false)
    private Enum<?>[] enumConstants;

    /** 3 参构造委托给 4 参构造，字段初始化只发生在 4 参构造中，故只需注入此处 */
    @Inject(
            method = "<init>(Lio/github/notenoughupdates/moulconfig/processor/ProcessedOption;[Ljava/lang/String;ZZ)V",
            at = @At("TAIL"),
            // 与 GuiOptionEditorDropdownMixin 保持一致:库升级签名变化时静默跳过,避免启动硬崩溃
            require = 0
    )
    private void bza$reorderExampleText(CallbackInfo ci) {
        Map<Object, StructuredText> ordered = new LinkedHashMap<>(exampleText.size() * 2);
        if (enumConstants != null) {
            // 枚举档:按声明顺序重排
            for (Enum<?> constant : enumConstants) {
                StructuredText text = exampleText.get(constant);
                if (text != null) ordered.put(constant, text);
            }
        } else {
            // int 档:key 为索引,按数值升序即 exampleText 数组顺序
            List<Object> keys = new ArrayList<>(exampleText.keySet());
            keys.sort((a, b) -> Integer.compare(((Number) a).intValue(), ((Number) b).intValue()));
            for (Object key : keys) ordered.put(key, exampleText.get(key));
        }
        exampleText.clear();
        exampleText.putAll(ordered);
    }
}
