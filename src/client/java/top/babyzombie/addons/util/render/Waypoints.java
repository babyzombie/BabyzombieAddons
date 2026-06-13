package top.babyzombie.addons.util.render;

import java.util.ArrayList;
import java.util.List;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import top.babyzombie.addons.event.WorldChangeCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;
import top.babyzombie.addons.util.ChatUtils;
import top.babyzombie.addons.util.ServerTick;

public final class Waypoints {
    private static final List<Waypoint> list = new ArrayList<>();

    private Waypoints() {}

    public static void init() {
        WorldChangeCallback.register((client, world) -> {
            synchronized (list) { list.removeIf(e -> e.time == 0); }
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            synchronized (list) {
                long now = ServerTick.getTime();
                list.removeIf(e -> e.time > 0 && e.timestamp + e.time < now);
            }
        });

        RenderPhaseRegister.register(ctx -> {
            synchronized (list) {
                if (list.isEmpty()) return;
                var player = Minecraft.getInstance().player;
                if (player == null) return;
                var cam = ctx.levelState().cameraRenderState;

                for (var wp : list) {
                    int bx = wp.x, by = wp.y, bz = wp.z;
                    double dist = player.position().distanceTo(new Vec3(bx + 0.5, by + 0.5, bz + 0.5));
                    float scale = 0.025f + Math.min((float) dist / 50f, 1f) * 0.095f;
                    WorldTextRenderer.renderString(cam, wp.name, bx + 0.5, by + 0.5, bz + 0.5, 0xFFFFFF55, scale, true, -5.5f);
                    WorldTextRenderer.renderString(cam, "§6(" + (int) dist + "m)", bx + 0.5, by + 0.5, bz + 0.5, 0xFFFFFF55, scale, true, 5.5f);
                }
            }
        });
    }

    public static void addWaypoint(String name, int x, int y, int z, int timeSeconds, boolean showMsg) {
        name = name.replace('&', '§');
        long timeMs = timeSeconds * 1000L;
        synchronized (list) { list.add(new Waypoint(name, x, y, z, timeMs, ServerTick.getTime())); }
        if (showMsg) ChatUtils.showMessage(Component.translatable("babyzombieaddons.waypoint.added", x, y, z, name).getString());
    }

    public static void deleteWaypoint(String name, boolean showMsg) {
        String search = ChatUtils.stripColor(name).toLowerCase();
        int removed;
        synchronized (list) {
            var toRemove = list.stream().filter(e -> ChatUtils.stripColor(e.name).toLowerCase().equals(search)).toList();
            removed = toRemove.size();
            list.removeAll(toRemove);
        }
        if (showMsg) {
            if (removed > 0) ChatUtils.showMessage(Component.translatable("babyzombieaddons.waypoint.deleted", removed, name).getString());
            else ChatUtils.showMessage(Component.translatable("babyzombieaddons.waypoint.not_found", name).getString());
        }
    }

    public static List<WaypointInfo> getWaypoints() {
        synchronized (list) { return list.stream().map(wp -> new WaypointInfo(wp.name, wp.x, wp.y, wp.z, wp.time, wp.timestamp)).toList(); }
    }

    private record Waypoint(String name, int x, int y, int z, long time, long timestamp) {}
    public record WaypointInfo(String name, int x, int y, int z, long time, long timestamp) {}
}
