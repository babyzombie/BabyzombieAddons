package top.babyzombie.addons.util;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Simple scheduler for delayed and repeating tasks.
 * Replaces ChatTriggers' register("step", ...) pattern.
 */
public final class Scheduler {

    private static final List<ScheduledTask> tasks = new ArrayList<>();
    private static boolean registered;

    private Scheduler() {}

    static {
        ClientTickEvents.START_CLIENT_TICK.register(client -> tick());
    }

    private static void tick() {
        if (tasks.isEmpty()) return;
        var client = Minecraft.getInstance();
        if (client.player == null) return;

        Iterator<ScheduledTask> it = tasks.iterator();
        while (it.hasNext()) {
            ScheduledTask task = it.next();
            task.ticksRemaining--;
            if (task.ticksRemaining <= 0) {
                task.runnable.run();
                if (task.repeating) {
                    task.ticksRemaining = task.intervalTicks;
                } else {
                    it.remove();
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

    public static void cancel(Runnable runnable) {
        tasks.removeIf(t -> t.runnable == runnable);
    }

    private static class ScheduledTask {
        int ticksRemaining;
        int intervalTicks;
        Runnable runnable;
        boolean repeating;

        ScheduledTask(int delay, int interval, Runnable r, boolean rep) {
            this.ticksRemaining = delay;
            this.intervalTicks = interval;
            this.runnable = r;
            this.repeating = rep;
        }
    }
}
