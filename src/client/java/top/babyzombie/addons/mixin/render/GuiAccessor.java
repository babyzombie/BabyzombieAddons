package top.babyzombie.addons.mixin.render;

import net.minecraft.client.gui.Gui;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Gui.class)
public interface GuiAccessor {

    @Accessor("title")
    @Nullable Component getTitle();

    @Accessor("subtitle")
    @Nullable Component getSubtitle();

    @Accessor("overlayMessageString")
    @Nullable Component getOverlayMessageString();

    @Accessor("titleTime")
    int getTitleTime();

    @Accessor("titleFadeInTime")
    int getTitleFadeInTime();

    @Accessor("titleStayTime")
    int getTitleStayTime();

    @Accessor("titleFadeOutTime")
    int getTitleFadeOutTime();

    @Accessor("overlayMessageTime")
    int getOverlayMessageTime();
}
