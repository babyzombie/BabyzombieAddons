package top.babyzombie.addons.mixin.screen;

import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Screen.class)
public interface ScreenInvoker {
    /** 26.1 起 Screen 的 children(事件)/renderables(渲染)/narratables 是三个独立列表,
     *  只改 children 会"点得动但看不见";此调用同步加入三个列表 */
    @Invoker("addRenderableWidget")
    <T extends GuiEventListener & Renderable & NarratableEntry> T bzaAddRenderableWidget(T widget);

    /** 同步从三个列表移除 */
    @Invoker("removeWidget")
    void bzaRemoveWidget(GuiEventListener widget);
}