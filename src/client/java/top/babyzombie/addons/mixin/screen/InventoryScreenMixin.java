package top.babyzombie.addons.mixin.screen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import org.lwjgl.glfw.GLFW;
import top.babyzombie.addons.config.ModConfigManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import top.babyzombie.addons.module.chat.ContainerChatHelper;
import top.babyzombie.addons.module.kuudra.ChestCounter;
import top.babyzombie.addons.util.StarIndicator;

@Mixin(InventoryScreen.class)
public class InventoryScreenMixin {

    @Inject(method = "extractRenderState*", at = @At("TAIL"))
    private void onRender(GuiGraphicsExtractor g, int mouseX, int mouseY, float a, CallbackInfo ci) {
        // 背包页面上的箱子计数 HUD（hover 提示）；点击走 AbstractContainerScreen 的注入
        ChestCounter.renderOnScreen(g, mouseX, mouseY);

        if (ContainerChatHelper.isActive()) {
            ContainerChatHelper.getOverlay().extractRenderState(g, mouseX, mouseY, a);
        }
        if (GLFW.glfwGetKey(Minecraft.getInstance().getWindow().handle(), GLFW.GLFW_KEY_LEFT_ALT) == GLFW.GLFW_PRESS) {
            boolean sharing = ContainerChatHelper.isActive() && ModConfigManager.get().general.chat.chatInContainer;
            StarIndicator.draw(g, mouseX, mouseY, sharing);
        }
    }
}
