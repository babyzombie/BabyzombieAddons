package top.babyzombie.addons.util;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLevelEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.BossHealthOverlay;
import net.minecraft.client.gui.components.LerpingBossEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.BossEvent;
import top.babyzombie.addons.mixin.render.BossHealthOverlayAccessor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ClientBossbarManager {
    private static final int BOSSBAR_WIDTH = 182;
    private static final int FIRST_BOSSBAR_Y = 12;
    private static final int BOSSBAR_Y_STEP = 19;
    private static final Pattern COUNTDOWN_PATTERN = Pattern.compile("(?<!\\d)(\\d{1,2}):(\\d{2})(?!\\d)");
    private static final Map<UUID, ManagedBossbarState> MANAGED_BOSSBARS = new HashMap<>();

    private ClientBossbarManager() {}

    public static void init() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> tickLocalBossbars());
        ClientLevelEvents.AFTER_CLIENT_LEVEL_CHANGE.register((client, world) -> clearManagedBossbars());
    }

    /**
     * Build a stable snapshot so commands and automation can inspect all current bossbars
     * without mutating vanilla state.
     */
    public static List<BossbarSnapshot> collectBossbars() {
        BossHealthOverlay overlay = getOverlay();
        if (overlay == null) return List.of();

        Map<UUID, LerpingBossEvent> events = ((BossHealthOverlayAccessor) overlay).getEvents();
        if (events.isEmpty()) return List.of();

        int screenWidth = Minecraft.getInstance().getWindow().getGuiScaledWidth();
        int renderX = screenWidth / 2 - BOSSBAR_WIDTH / 2;
        List<BossbarSnapshot> snapshots = new ArrayList<>(events.size());

        int index = 0;
        for (var entry : events.entrySet()) {
            UUID id = entry.getKey();
            LerpingBossEvent event = entry.getValue();
            boolean localOnly = isLocalBossbar(id);
            RemainingInfo remaining = describeRemaining(id, event);

            snapshots.add(new BossbarSnapshot(
                    id,
                    event.getName(),
                    event.getProgress(),
                    event.getProgress() * 100.0f,
                    100.0f,
                    remaining.seconds(),
                    remaining.display(),
                    renderX,
                    FIRST_BOSSBAR_Y + index * BOSSBAR_Y_STEP,
                    event.getColor(),
                    event.getOverlay(),
                    event.shouldDarkenScreen(),
                    event.shouldPlayBossMusic(),
                    event.shouldCreateWorldFog(),
                    localOnly
            ));
            index++;
        }

        return snapshots;
    }

    public static void showTestBossbar(boolean localOnly, int durationSeconds, String text) {
        BossHealthOverlay overlay = getOverlay();
        if (overlay == null) return;

        clearManagedTestBossbars();
        long durationMs = Math.max(1L, durationSeconds) * 1000L;
        UUID id = UUID.randomUUID();

        // Insert a client-side bossbar directly into the overlay map so we can simulate real bossbar logic
        // without sending any packet to the server.
        LerpingBossEvent event = new LerpingBossEvent(
                id,
                Component.literal(text),
                1.0f,
                BossEvent.BossBarColor.PURPLE,
                BossEvent.BossBarOverlay.PROGRESS,
                false,
                false,
                false
        );

        ((BossHealthOverlayAccessor) overlay).getEvents().put(id, event);
        MANAGED_BOSSBARS.put(id, new ManagedBossbarState(event,
                System.currentTimeMillis() + durationMs, durationMs, localOnly, true));
    }

    public static boolean isLocalBossbar(UUID id) {
        ManagedBossbarState state = MANAGED_BOSSBARS.get(id);
        return state != null && state.localOnly();
    }

    public static String sanitizeTestBossbarText(String rawText) {
        StringBuilder sanitized = new StringBuilder(rawText.length());

        for (int i = 0; i < rawText.length(); i++) {
            char ch = rawText.charAt(i);

            if (ch == '&') {
                if (i + 1 < rawText.length() && isLegacyFormattingCode(rawText.charAt(i + 1))) {
                    sanitized.append('§').append(Character.toLowerCase(rawText.charAt(i + 1)));
                    i++;
                }
                continue;
            }

            if (ch == '§') {
                if (i + 1 < rawText.length() && isLegacyFormattingCode(rawText.charAt(i + 1))) {
                    i++;
                }
                continue;
            }

            if (ch == '\r' || ch == '\n' || ch == '\t') {
                sanitized.append(' ');
                continue;
            }

            if (!Character.isISOControl(ch)) {
                sanitized.append(ch);
            }
        }

        return sanitized.toString().trim();
    }

    public static Integer extractRemainingSeconds(Component name) {
        String plain = ChatUtils.stripColor(name.getString()).toUpperCase(Locale.ROOT);
        Matcher matcher = COUNTDOWN_PATTERN.matcher(plain);
        if (!matcher.find()) return null;

        int minutes = Integer.parseInt(matcher.group(1));
        int seconds = Integer.parseInt(matcher.group(2));
        return minutes * 60 + seconds;
    }

    public static String formatSeconds(int totalSeconds) {
        int minutes = totalSeconds / 60;
        int seconds = totalSeconds % 60;
        return String.format(Locale.ROOT, "%02d:%02d", minutes, seconds);
    }

    private static BossHealthOverlay getOverlay() {
        var client = Minecraft.getInstance();
        if (client.gui == null) return null;
        return client.gui.getBossOverlay();
    }

    private static RemainingInfo describeRemaining(UUID id, LerpingBossEvent event) {
        ManagedBossbarState state = MANAGED_BOSSBARS.get(id);
        if (state != null) {
            long remainingMs = Math.max(0L, state.expiresAtMs() - System.currentTimeMillis());
            int remainingSeconds = (int) Math.ceil(remainingMs / 1000.0);
            String source = state.localOnly() ? " (local)" : " (simulated)";
            return new RemainingInfo(remainingSeconds, formatSeconds(remainingSeconds) + source);
        }

        Integer parsedSeconds = extractRemainingSeconds(event.getName());
        if (parsedSeconds != null) {
            return new RemainingInfo(parsedSeconds, formatSeconds(parsedSeconds) + " (title)");
        }

        return new RemainingInfo(null, "N/A");
    }

    private static void tickLocalBossbars() {
        if (MANAGED_BOSSBARS.isEmpty()) return;

        BossHealthOverlay overlay = getOverlay();
        if (overlay == null) {
            MANAGED_BOSSBARS.clear();
            return;
        }

        Map<UUID, LerpingBossEvent> events = ((BossHealthOverlayAccessor) overlay).getEvents();
        long now = System.currentTimeMillis();
        List<UUID> expired = new ArrayList<>();

        // Animate local test bossbars entirely on the client so they visibly expire on their own.
        for (var entry : MANAGED_BOSSBARS.entrySet()) {
            UUID id = entry.getKey();
            ManagedBossbarState state = entry.getValue();
            long remainingMs = state.expiresAtMs() - now;

            if (remainingMs <= 0L) {
                expired.add(id);
                continue;
            }

            float progress = Math.max(0.0f, remainingMs / (float) state.durationMs());
            state.event().setProgress(progress);
        }

        for (UUID id : expired) {
            events.remove(id);
            MANAGED_BOSSBARS.remove(id);
        }
    }

    private static void clearManagedBossbars() {
        BossHealthOverlay overlay = getOverlay();
        if (overlay != null) {
            Map<UUID, LerpingBossEvent> events = ((BossHealthOverlayAccessor) overlay).getEvents();
            for (UUID id : MANAGED_BOSSBARS.keySet()) {
                events.remove(id);
            }
        }
        MANAGED_BOSSBARS.clear();
    }

    private static void clearManagedTestBossbars() {
        BossHealthOverlay overlay = getOverlay();
        if (overlay == null) {
            MANAGED_BOSSBARS.entrySet().removeIf(entry -> entry.getValue().debugTest());
            return;
        }

        Map<UUID, LerpingBossEvent> events = ((BossHealthOverlayAccessor) overlay).getEvents();
        List<UUID> ids = new ArrayList<>();
        for (var entry : MANAGED_BOSSBARS.entrySet()) {
            if (entry.getValue().debugTest()) {
                ids.add(entry.getKey());
            }
        }
        for (UUID id : ids) {
            events.remove(id);
            MANAGED_BOSSBARS.remove(id);
        }
    }

    private static boolean isLegacyFormattingCode(char code) {
        char lower = Character.toLowerCase(code);
        return (lower >= '0' && lower <= '9') || (lower >= 'a' && lower <= 'f')
                || lower == 'k' || lower == 'l' || lower == 'm'
                || lower == 'n' || lower == 'o' || lower == 'r';
    }

    public record BossbarSnapshot(
            UUID id,
            Component name,
            float progress,
            float currentHealth,
            float maxHealth,
            Integer remainingSeconds,
            String remainingText,
            int renderX,
            int renderY,
            BossEvent.BossBarColor color,
            BossEvent.BossBarOverlay overlay,
            boolean darkenScreen,
            boolean playBossMusic,
            boolean createWorldFog,
            boolean localOnly
    ) {}

    private record ManagedBossbarState(
            LerpingBossEvent event,
            long expiresAtMs,
            long durationMs,
            boolean localOnly,
            boolean debugTest
    ) {}

    private record RemainingInfo(Integer seconds, String display) {}
}
