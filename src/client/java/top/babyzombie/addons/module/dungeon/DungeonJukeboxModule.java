package top.babyzombie.addons.module.dungeon;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLevelEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.Minecraft;
import net.minecraft.client.sounds.SoundEngine;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundSource;
import top.babyzombie.addons.config.ModConfig;
import top.babyzombie.addons.config.ModConfigManager;
import top.babyzombie.addons.config.hud.HudManager;
import top.babyzombie.addons.event.HypixelLocationEvents;
import top.babyzombie.addons.mixin.sound.SoundEngineAccessor;
import top.babyzombie.addons.mixin.sound.SoundManagerAccessor;
import top.babyzombie.addons.util.ChatUtils;
import top.babyzombie.addons.util.Scheduler;
import top.babyzombie.addons.util.tracker.HypixelLocationTracker;

import java.util.List;
import java.util.Random;

/**
 * 地牢唱片机 —— 进入地牢自动播放唱片，离开自动停止。
 * <ul>
 * <li>单曲：列表第一首循环</li>
 * <li>顺序：按序播放，跨对局续播（上次第2首没放完，下一把从第3首开始）</li>
 * <li>随机：随机选，排除刚刚播过的那首</li>
 * </ul>
 */
public final class DungeonJukeboxModule {

    private static boolean active;
    private static int currentIndex;
    private static int lastPlayedIndex = -1;
    /** 当前播放的实例，供 HUD 查询进度 */
    public static DungeonJukeboxSoundInstance currentInstance;
    private static boolean toggledMusic = true;
    private static boolean toggledMusicByThis;
    private static boolean pendingTogglemusic;
    private static boolean togglemusicCheckRegistered;
    private static final Random RNG = new Random();

    /** 当前播放的唱片显示名（含 §b 色码），供 HUD 读取。null 表示未播放 */
    public static String currentDiscName;

    /** 轮询复活：死人发不了 /togglemusic，等玩家可见（复活）后再发 */
    private static final Runnable TOGGLEMUSIC_CHECK = new Runnable() {
        @Override
        public void run() {
            var player = Minecraft.getInstance().player;
            if (player == null || !pendingTogglemusic) {
                togglemusicCheckRegistered = false;
                Scheduler.cancel(this);
                return;
            }
            if (!player.isInvisible()) {
                pendingTogglemusic = false;
                togglemusicCheckRegistered = false;
                Scheduler.cancel(this);
                ChatUtils.sendCommand("togglemusic");
            }
        }
    };

    /** 秒数 → mm:ss 格式 */
    private static String formatTime(int totalSeconds) {
        int min = totalSeconds / 60;
        int sec = totalSeconds % 60;
        return min + ":" + (sec < 10 ? "0" : "") + sec;
    }

    private DungeonJukeboxModule() {}

    public static void init() {
        ClientLevelEvents.AFTER_CLIENT_LEVEL_CHANGE.register((client, world) -> {
            active = false;
            pendingTogglemusic = false;
            togglemusicCheckRegistered = false;
            currentDiscName = null;
            currentInstance = null;
        });

        ClientReceiveMessageEvents.ALLOW_GAME.register((component, o) -> {
            if (o || !HypixelLocationTracker.getInstance().isInSkyblock()) return true;
            var message = ChatUtils.stripColor(component.getString());
            if (message.equals("Play Music is now disabled!")) {
                toggledMusic = false;
            } else if (message.equals("Play Music is now enabled!")) {
                toggledMusic = true;
            }
            return true;
        });

        HypixelLocationEvents.LOCATION_UPDATE.register((hypixelLocationTracker) -> {
            if (!hypixelLocationTracker.isInSkyblock()) return;
            var cfg = ModConfigManager.get().dungeon.dungeonJukebox;
            if (!cfg.enabled || !cfg.autoToggleMusic) return;
            if (hypixelLocationTracker.isInDungeon()) {
                if (toggledMusic) {
                    trySendTogglemusic();
                    toggledMusicByThis = true;
                }
            } else if (!toggledMusic && toggledMusicByThis) {
                trySendTogglemusic();
                toggledMusicByThis = false;
            }
        });

        // HUD 渲染
        HudElementRegistry.attachElementAfter(VanillaHudElements.OVERLAY_MESSAGE,
                Identifier.fromNamespaceAndPath("babyzombieaddons", "dungeon_jukebox"),
                (context, tickCounter) -> {
            if (!HudManager.shouldShow("DungeonJukeboxDisc")) return;
            if (currentDiscName == null) return;
            var font = Minecraft.getInstance().font;
            int x = HudManager.x("DungeonJukeboxDisc"),
                y = HudManager.y("DungeonJukeboxDisc");
            float s = HudManager.scale("DungeonJukeboxDisc");

            // 第一行：歌名
            var line1 = "§b♫ " + currentDiscName;

            // 第二行：进度条 + 时间
            var line2 = "";
            if (currentInstance != null) {
                float duration = currentInstance.getDuration();
                float pos = Math.min(currentInstance.getCurrentPosition(), duration);
                float fraction = duration > 0 ? pos / duration : 0F;

                int barW = 20;
                int filled = Math.clamp((int) (fraction * barW), 0, barW);
                int empty = barW - filled;

                StringBuilder bar = new StringBuilder();
                if (filled > 0) bar.append("§a§m").repeat(" ", filled);
                if (empty > 0) bar.append("§7§m").repeat(" ", empty);
                bar.append("§r §7");
                bar.append(formatTime((int) pos));
                bar.append(" / ");
                bar.append(formatTime((int) duration));

                line2 = bar.toString();
            }

            HudManager.drawScaled(context, font,
                    line1 + (line2.isEmpty() ? "" : "\n" + line2),
                    x, y, s);
        });
    }

    /** 地牢实例开始时调用 */
    public static void onInstanceStart() {
        var cfg = ModConfigManager.get().dungeon.dungeonJukebox;
        if (!cfg.enabled) return;
        if (HypixelLocationTracker.getInstance().isInKuudra()) return;

        active = true;
        playNext();
    }

    /** 地牢实例结束时调用 */
    public static void onInstanceEnd() {
        active = false;
        stopCurrentSound();
    }

    /**
     * 尝试发送 /togglemusic。如果玩家处于死亡（隐身）状态，启动轮询等待复活后再发。
     * 参考 AutoRequeue 的 REVIVE_CHECK 模式。
     */
    private static void trySendTogglemusic() {
        var player = Minecraft.getInstance().player;
        if (player == null) return;
        if (player.isInvisible()) {
            pendingTogglemusic = true;
            if (!togglemusicCheckRegistered) {
                togglemusicCheckRegistered = true;
                Scheduler.scheduleRepeating(1, TOGGLEMUSIC_CHECK);
            }
        } else {
            ChatUtils.sendCommand("togglemusic");
        }
    }

    /** 播放下一首 */
    private static void playNext() {
        if (!active) return;

        var cfg = ModConfigManager.get().dungeon.dungeonJukebox;
        List<ModConfig.MusicDisc> playlist = cfg.playlist;
        if (playlist == null || playlist.isEmpty()) return;

        int index = switch (cfg.playMode) {
            case SINGLE -> 0;
            case SEQUENTIAL -> {
                int i = currentIndex % playlist.size();
                currentIndex++;
                yield i;
            }
            case RANDOM -> {
                if (playlist.size() == 1) {
                    yield 0;
                }
                int i;
                do {
                    i = RNG.nextInt(playlist.size());
                } while (i == lastPlayedIndex && playlist.size() > 1);
                yield i;
            }
        };

        lastPlayedIndex = index;
        ModConfig.MusicDisc disc = playlist.get(index);
        currentDiscName = disc.toString(); // 已含 §b 色码

        Identifier soundId = disc.getSoundId();
        var instance = new DungeonJukeboxSoundInstance(soundId, disc.getDurationSeconds(), DungeonJukeboxModule::playNext);
        currentInstance = instance;
        Minecraft.getInstance().getSoundManager().play(instance);
    }

    /** 停止当前播放的声音 */
    private static void stopCurrentSound() {
        if (currentInstance != null) {
            var manager = Minecraft.getInstance().getSoundManager();
            SoundEngine engine = ((SoundManagerAccessor) manager).getSoundEngine();
            ((SoundEngineAccessor) engine).invokeStop(null, SoundSource.RECORDS);
            currentInstance = null;
        }
        currentDiscName = null;
    }
}
