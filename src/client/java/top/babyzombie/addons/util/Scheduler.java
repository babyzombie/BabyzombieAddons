package top.babyzombie.addons.util;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;

import java.util.ArrayList;
import java.util.List;

/**
 * Simple scheduler for delayed and repeating tasks.
 * Uses wall-clock time internally so timing stays accurate even at low FPS.
 * <p>
 * Parameters are still given in ticks (20 ticks = 1 second) for API compatibility,
 * but they are converted to milliseconds (1 tick = 50ms) on construction.
 */
public final class Scheduler {

    private static final List<ScheduledTask> tasks = new ArrayList<>();

    private Scheduler() {}

    static {
        ClientTickEvents.START_CLIENT_TICK.register(client -> tick());
    }

    private static void tick() {
        if (tasks.isEmpty()) return;
        var client = Minecraft.getInstance();
        if (client.player == null) return;

        long now = System.currentTimeMillis();

        // Snapshot to avoid ConcurrentModificationException when a task's
        // runnable calls schedule() or cancel() during iteration.
        for (ScheduledTask task : new ArrayList<>(tasks)) {
            if (now >= task.executeAtMs) {
                task.runnable.run();
                if (task.repeating) {
                    task.executeAtMs = now + task.intervalMs;
                } else {
                    tasks.remove(task);
                }
            }
        }
    }

    /**
     * Run a task after a delay in ticks (20 ticks = 1 second).
     */
    public static void schedule(int delayTicks, Runnable runnable) {
        tasks.add(new ScheduledTask(delayTicks, -1, runnable, false));
    }

    /**
     * Run a task repeatedly with a given interval in ticks.
     */
    public static void scheduleRepeating(int intervalTicks, Runnable runnable) {
        tasks.add(new ScheduledTask(intervalTicks, intervalTicks, runnable, true));
    }

    /** Cancel a scheduled task by its runnable reference. */
    public static void cancel(Runnable runnable) {
        tasks.removeIf(t -> t.runnable == runnable);
    }

    private static class ScheduledTask {
        long executeAtMs;
        long intervalMs;
        Runnable runnable;
        boolean repeating;

        ScheduledTask(int delayTicks, int intervalTicks, Runnable r, boolean rep) {
            this.executeAtMs = System.currentTimeMillis() + delayTicks * 50L;
            this.intervalMs = intervalTicks < 0 ? -1 : intervalTicks * 50L;
            this.runnable = r;
            this.repeating = rep;
        }
    }
}
