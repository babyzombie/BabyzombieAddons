package top.babyzombie.addons.mixin.screen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import com.mojang.blaze3d.platform.InputConstants;
import top.babyzombie.addons.config.ModConfigManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import top.babyzombie.addons.module.chat.containerchat.ContainerChatHelper;
import top.babyzombie.addons.module.kuudra.ChestCounter;
import top.babyzombie.addons.util.StarIndicator;
import top.babyzombie.addons.util.gui.overlay.GuiOverlayManager;

@Mixin(InventoryScreen.class)
public class InventoryScreenMixin {

    @Inject(method = "extractRenderState*", at = @At("TAIL"))
    private void onRender(GuiGraphicsExtractor g, int mouseX, int mouseY, float a, CallbackInfo ci) {
        ChestCounter.renderOnScreen(g, mouseX, mouseY);
        GuiOverlayManager.onRender((InventoryScreen) (Object) this, g, mouseX, mouseY, a);

        if (ContainerChatHelper.isActive()) {
            ContainerChatHelper.getOverlay().extractRenderState(g, mouseX, mouseY, a);
        }
        if (InputConstants.isKeyDown(Minecraft.getInstance().getWindow(), InputConstants.KEY_LALT)) {
            boolean sharing = ContainerChatHelper.isActive() && ModConfigManager.get().general.chat.chatInContainer;
            StarIndicator.draw(g, mouseX, mouseY, sharing);
        }
    }
}
