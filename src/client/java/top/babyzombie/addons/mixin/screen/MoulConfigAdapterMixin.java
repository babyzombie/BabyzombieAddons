package top.babyzombie.addons.mixin.screen;

import java.util.List;

import net.azureaaron.dandelion.deps.moulconfig.gui.GuiElementComponent;
import net.azureaaron.dandelion.deps.moulconfig.gui.MoulConfigEditor;
import net.azureaaron.dandelion.deps.moulconfig.platform.MoulConfigScreenComponent;
import net.azureaaron.dandelion.impl.moulconfig.MoulConfigAdapter;
import net.minecraft.client.gui.screens.Screen;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import top.babyzombie.addons.config.ModConfigManager;

@Mixin(value = MoulConfigAdapter.class, remap = false)
public class MoulConfigAdapterMixin {

	/**
	 * 在 MoulConfigAdapter 生成设置屏幕之后，根据用户配置设置编辑器的宽屏模式。
	 */
	@Inject(method = "generateMoulConfigScreen", at = @At("RETURN"))
	private void setWideEditor(List<?> categories, Screen parent, String search, CallbackInfoReturnable<Screen> cir) {
		if (!ModConfigManager.get().debug.wideMoulConfig) return;

		if (cir.getReturnValue() instanceof MoulConfigScreenComponent msc
				&& msc.getGuiContext().getRoot() instanceof GuiElementComponent gec
				&& gec.getElement() instanceof MoulConfigEditor<?> editor) {
			editor.wide = true;
		}
	}
}
