package top.babyzombie.addons.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.input.MouseButtonEvent;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import top.babyzombie.addons.config.ModConfigManager;
import top.babyzombie.addons.config.hud.HudManager;
import top.babyzombie.addons.module.chat.ChatChannel;
import top.babyzombie.addons.module.chat.ChatChannelModule;
import top.babyzombie.addons.util.ChatUtils;
import top.babyzombie.addons.util.tracker.HypixelLocationTracker;

@Mixin(ChatScreen.class)
public class ChatScreenMixin {

    private static final int BTN_W = 26;
    private static final int BTN_H = 14;
    private static final int GAP = 2;

    @Inject(method = "extractRenderState", at = @At("RETURN"))
    private void onRender(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a, CallbackInfo ci) {
        if (!ModConfigManager.get().chatChannel.chatChannelSwitcher) return;
        if (!HypixelLocationTracker.getInstance().isOnHypixel()) return;
        if (!HudManager.shouldShow("ChatChannelSwitcher")) return;

        var font = Minecraft.getInstance().font;
        int x = HudManager.x("ChatChannelSwitcher");
        int y = HudManager.y("ChatChannelSwitcher");
        float scale = HudManager.scale("ChatChannelSwitcher");

        var pose = graphics.pose();
        pose.pushMatrix();
        pose.translate(x, y);
        pose.scale(scale, scale);

        ChatChannel current = ChatChannelModule.getCurrentChannel();
        int bx = 0;
        for (ChatChannel ch : ChatChannel.values()) {
            boolean active = ch == current;
            int bg = 0x60000000;
            int tc = active ? ch.activeColor : 0xFFAAAAAA;

            graphics.fill(bx, 0, bx + BTN_W, BTN_H, bg);
            int tw = font.width(ch.label);
            graphics.text(font, ch.label, bx + (BTN_W - tw) / 2, (BTN_H - font.lineHeight) / 2, tc, false);

            bx += BTN_W + GAP;
        }

        pose.popMatrix();
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"))
    private void onMouseClicked(MouseButtonEvent event, boolean doubleClick, CallbackInfoReturnable<Boolean> cir) {
        if (event.button() != GLFW.GLFW_MOUSE_BUTTON_LEFT) return;
        if (!ModConfigManager.get().chatChannel.chatChannelSwitcher) return;;
        if (!HypixelLocationTracker.getInstance().isOnHypixel()) return;
        if (!HudManager.shouldShow("ChatChannelSwitcher")) return;

        int mouseX = (int) event.x();
        int mouseY = (int) event.y();
        int x = HudManager.x("ChatChannelSwitcher");
        int y = HudManager.y("ChatChannelSwitcher");
        float scale = HudManager.scale("ChatChannelSwitcher");

        int relX = (int) ((mouseX - x) / scale);
        int relY = (int) ((mouseY - y) / scale);

        if (relY < 0 || relY > BTN_H) return;

        int bx = 0;
        for (ChatChannel ch : ChatChannel.values()) {
            if (relX >= bx && relX < bx + BTN_W) {
                ChatUtils.sendCommand(ch.command);
                return;
            }
            bx += BTN_W + GAP;
        }
    }
}
