package top.babyzombie.addons.util;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Utility for waiting until a container screen's specific slot becomes
 * non-empty (meaning the server has sent all items for that row), then
 * executing an action exactly once.
 * <p>
 * Uses a single persistent tick listener with an internal waiter queue —
 * no per-call listener leaks.
 */
public final class ScreenLoadWaiter {

    private ScreenLoadWaiter() {}

    private static final List<Waiter> waiters = new ArrayList<>();
    private static volatile boolean tickRegistered;

    // ── Public API ──

    /**
     * Registers a persistent AFTER_INIT listener. Every time a container
     * screen whose raw title matches {@code titlePredicate} opens, waits
     * for the given slot to become non-empty, then calls {@code action}
     * with the screen reference.
     * <p>
     * Call once per page type during mod init.
     *
     * @param titlePredicate tested against the raw (colored) screen title
     * @param loadedSlot     slot index that signals the page is fully loaded
     * @param timeoutTicks   maximum ticks to wait for the slot, 0 = wait indefinitely
     * @param action         receives the loaded {@link AbstractContainerScreen}
     */
    public static void whenScreenOpened(Predicate<String> titlePredicate,
                                         int loadedSlot,
                                         int timeoutTicks,
                                         Consumer<AbstractContainerScreen<?>> action) {
        ScreenEvents.AFTER_INIT.register((client, screen, sw, sh) -> {
            if (!(screen instanceof AbstractContainerScreen<?> cs)) return;
            if (!titlePredicate.test(cs.getTitle().getString())) return;
            enqueue(cs, loadedSlot, timeoutTicks, () -> action.accept(cs));
        });
    }

    /**
     * Waits for an already-open screen's slot to become non-empty, then
     * runs {@code action}. Useful when the screen might already be open
     * before the trigger fires (e.g. loadout switch via chat command
     * while the Loadouts menu is already showing).
     *
     * @param screen       the container screen to watch
     * @param loadedSlot   slot index that signals the page is fully loaded
     * @param timeoutTicks maximum ticks to wait, 0 = wait indefinitely
     * @param action       code to run once the page is loaded
     */
    public static void onPageLoaded(AbstractContainerScreen<?> screen,
                                     int loadedSlot,
                                     int timeoutTicks,
                                     Runnable action) {
        enqueue(screen, loadedSlot, timeoutTicks, action);
    }

    // ── Internals ──

    private static void enqueue(AbstractContainerScreen<?> screen,
                                int loadedSlot,
                                int timeoutTicks,
                                Runnable action) {
        synchronized (waiters) {
            waiters.add(new Waiter(screen, loadedSlot, timeoutTicks, action));
            if (!tickRegistered) {
                tickRegistered = true;
                ClientTickEvents.END_CLIENT_TICK.register(ScreenLoadWaiter::tick);
            }
        }
    }

    private static void tick(Minecraft client) {
        List<Runnable> ready = new ArrayList<>();
        synchronized (waiters) {
            Iterator<Waiter> it = waiters.iterator();
            while (it.hasNext()) {
                Waiter w = it.next();

                // Screen closed or replaced
                if (client.screen != w.screen) {
                    it.remove();
                    continue;
                }

                // Timeout
                if (w.timeoutTicks > 0 && ++w.elapsed > w.timeoutTicks) {
                    it.remove();
                    continue;
                }

                // Bounds check — screen may have fewer slots than expected
                if (w.loadedSlot >= w.screen.getMenu().slots.size()) {
                    it.remove();
                    continue;
                }

                // Still loading
                if (w.screen.getMenu().slots.get(w.loadedSlot).getItem().isEmpty()) continue;

                // Loaded — remove and collect for deferred execution
                it.remove();
                ready.add(w.action);
            }
        }
        // Run actions outside the lock so they can safely enqueue new waiters
        for (Runnable action : ready) {
            action.run();
        }
    }

    private static final class Waiter {
        final AbstractContainerScreen<?> screen;
        final int loadedSlot;
        final int timeoutTicks;
        final Runnable action;
        int elapsed;

        Waiter(AbstractContainerScreen<?> screen, int loadedSlot, int timeoutTicks, Runnable action) {
            this.screen = screen;
            this.loadedSlot = loadedSlot;
            this.timeoutTicks = timeoutTicks;
            this.action = action;
        }
    }
}
