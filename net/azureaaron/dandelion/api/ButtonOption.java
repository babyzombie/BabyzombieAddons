package net.azureaaron.dandelion.api;

import java.util.function.Consumer;

import net.azureaaron.dandelion.impl.ButtonOptionImpl;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

public interface ButtonOption extends Option<Consumer<Screen>> {

	static net.azureaaron.dandelion.api.ButtonOption.Builder createBuilder() {
		return new ButtonOptionImpl.ButtonOptionBuilderImpl();
	}

	Component prompt();

	Consumer<Screen> action();

	//TODO consider allowing to "gray out" the option
	interface Builder {
		net.azureaaron.dandelion.api.ButtonOption.Builder id(Identifier id);

		net.azureaaron.dandelion.api.ButtonOption.Builder name(Component name);

		net.azureaaron.dandelion.api.ButtonOption.Builder description(Component... texts);

		net.azureaaron.dandelion.api.ButtonOption.Builder tags(Component... tags);

		net.azureaaron.dandelion.api.ButtonOption.Builder prompt(Component prompt);

		net.azureaaron.dandelion.api.ButtonOption.Builder action(Consumer<Screen> action);

		ButtonOption build();
	}
}
