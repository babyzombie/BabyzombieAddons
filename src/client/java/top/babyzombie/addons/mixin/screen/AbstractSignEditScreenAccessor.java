package top.babyzombie.addons.mixin.screen;

import net.minecraft.client.gui.screens.inventory.AbstractSignEditScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(AbstractSignEditScreen.class)
public interface AbstractSignEditScreenAccessor {
    /** 告示牌四行内容:SignEditScreen 关闭(removed)时原样发给服务器的就是它 */
    @Accessor("messages")
    String[] messages();
}