package top.babyzombie.addons.module.misc.abiphone;

import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import top.babyzombie.addons.config.ModConfigManager;
import top.babyzombie.addons.event.PlaySoundEvents;
import top.babyzombie.addons.util.PlaySoundHelper;
import top.babyzombie.addons.util.Scheduler;
import top.babyzombie.addons.util.tracker.HypixelLocationTracker;

/**
 * 自定义 Abiphone 铃声模块。
 * <ul>
 * <li>检测以 "✆" 开头的聊天消息（来电 / 呼出）</li>
 * <li>3 秒内拦截原版 pling(pitch 1.67) / bassattack(pitch 0.49) 叮铃声</li>
 * <li>播放用户自选的唱片铃声，支持 seek 起始位置和限时</li>
 * <li>声音跟随玩家</li>
 * </ul>
 */
public final class CustomRingtoneModule {

    private static long callDetectedAt = -1;
    private static final long CANCEL_WINDOW_MS = 1200;
    private static SoundInstance currentRingtone;
    private static boolean playing;

    private CustomRingtoneModule() {}

    public static void init() {
        // 消息检测：以 "✆" 开头的聊天消息
        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            if (overlay) return;
            if (!HypixelLocationTracker.getInstance().isInSkyblock()) return;

            var cfg = ModConfigManager.get().skyblock.customRingtone;
            if (!cfg.enabled) return;

            String text = message.getString();
            if (text.startsWith("✆")) {
                onCallDetected();
            }
        });

        // 声音拦截（始终注册，内部窗口判断）
        PlaySoundEvents.BEFORE_PLAY.register(CustomRingtoneModule::cancelVanillaRingtone);
    }

    private static void onCallDetected() {
        callDetectedAt = System.currentTimeMillis();

        // 冷却中：上一次铃声还没播完，跳过
        if (playing) return;

        playCustomRingtone();
    }

    private static boolean cancelVanillaRingtone(SoundInstance sound) {
        if (callDetectedAt < 0
                || System.currentTimeMillis() - callDetectedAt > CANCEL_WINDOW_MS) {
            return false;
        }
        var snd = sound.getSound();
        if (snd == null) return false;
        String path = snd.getLocation().getPath();
        float pitch = sound.getPitch();

        // pling pitch ≈ 1.67   /   bassattack pitch ≈ 0.49
        if (path.contains("pling") && Math.abs(pitch - 1.67f) < 0.02f) return true;
        if (path.contains("bassattack") && Math.abs(pitch - 0.49f) < 0.02f) return true;

        return false;
    }

    private static void playCustomRingtone() {
        var cfg = ModConfigManager.get().skyblock.customRingtone;
        if (!cfg.enabled) return;

        var player = Minecraft.getInstance().player;
        if (player == null) return;

        playing = true;

        Identifier soundId = cfg.disc.getSoundId();

        var instance = new AbstractSoundInstance(soundId, SoundSource.RECORDS, RandomSource.create()) {{
            this.volume = 1.0f;
            this.pitch = cfg.pitch;
            this.looping = false;
            this.relative = true;
            this.attenuation = SoundInstance.Attenuation.NONE;
            this.x = 0; this.y = 0; this.z = 0;
        }};

        currentRingtone = instance;

        if (cfg.startTime > 0) {
            PlaySoundHelper.playSeeked(instance, cfg.startTime, cfg.duration);
        } else {
            Minecraft.getInstance().getSoundManager().play(instance);
            Scheduler.schedule((int) (cfg.duration * 20), () -> {
                Minecraft.getInstance().getSoundManager().stop(instance);
            });
        }

        // 冷却：duration 秒后允许下一次播放
        Scheduler.schedule((int) (cfg.duration * 20), () -> {
            playing = false;
            currentRingtone = null;
        });
    }

    /** 试听按钮回调 */
    public static void playPreview() {
        var player = Minecraft.getInstance().player;
        if (player == null) return;

        var cfg = ModConfigManager.get().skyblock.customRingtone;
        Identifier soundId = cfg.disc.getSoundId();

        // 如果正在播放铃声，先停掉
        if (currentRingtone != null) {
            Minecraft.getInstance().getSoundManager().stop(currentRingtone);
            playing = false;
        }

        var instance = new AbstractSoundInstance(soundId, SoundSource.RECORDS, RandomSource.create()) {{
            this.volume = 1.0f;
            this.pitch = cfg.pitch;
            this.relative = true;
            this.attenuation = SoundInstance.Attenuation.NONE;
            this.x = 0; this.y = 0; this.z = 0;
        }};

        currentRingtone = instance;

        if (cfg.startTime > 0) {
            PlaySoundHelper.playSeeked(instance, cfg.startTime, cfg.duration);
        } else {
            Minecraft.getInstance().getSoundManager().play(instance);
            Scheduler.schedule((int) (cfg.duration * 20), () -> {
                Minecraft.getInstance().getSoundManager().stop(instance);
            });
        }

        Scheduler.schedule((int) (cfg.duration * 20), () -> {
            currentRingtone = null;
        });
    }
}
