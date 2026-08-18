package top.babyzombie.addons.util.render;

import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.entity.EquipmentSlot;

import java.util.Set;

/**
 * 选择性发光辅助：在具体部位渲染期间临时打开/关闭 EntityRenderState.outlineColor。
 * 只有 SELECTIVE_SLOTS 里包含当前 EquipmentSlot 时才会写入颜色，其余部位保持 0。
 */
public final class SelectiveGlowHelper {
    private SelectiveGlowHelper() {}

    public static void applySlot(EntityRenderState state, EquipmentSlot slot) {
        if (state == null) return;
        Set<EquipmentSlot> slots = state.getDataOrDefault(GlowController.SELECTIVE_SLOTS, Set.of());
        if (slots.isEmpty()) {
            state.outlineColor = 0;
            return;
        }
        int color = state.getDataOrDefault(GlowController.GLOW_COLOR, 0xFFFFFF);
        state.outlineColor = slots.contains(slot) ? color : 0;
    }

    public static void restore(EntityRenderState state) {
        if (state != null) {
            state.outlineColor = 0;
        }
    }
}
