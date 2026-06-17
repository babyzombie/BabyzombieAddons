package top.babyzombie.addons.event;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

public final class WorldChangeCallback {
    private static final List<BiConsumer<Minecraft, ClientLevel>> listeners = new ArrayList<>();

    public static void register(BiConsumer<Minecraft, ClientLevel> listener) {
        listeners.add(listener);
    }

    public static void fire(Minecraft client, ClientLevel world) {
        for (var listener : listeners) {
            listener.accept(client, world);
        }
    }
}
