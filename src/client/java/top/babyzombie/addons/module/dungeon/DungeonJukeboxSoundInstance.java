package top.babyzombie.addons.module.dungeon;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.resources.sounds.TickableSoundInstance;
import net.minecraft.client.sounds.ChannelAccess;
import net.minecraft.client.sounds.SoundEngine;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import top.babyzombie.addons.mixin.sound.SoundEngineAccessor;
import top.babyzombie.addons.mixin.sound.SoundManagerAccessor;
import top.babyzombie.addons.util.Scheduler;

/**
 * 地牢唱片机专用的 TickableSoundInstance。
 * 每 tick 检查底层 Channel 是否已停止，结束时通过 Scheduler 延迟触发回调切歌，
 * 避免在 SoundEngine 遍历 tickingSounds 时直接修改列表导致 CME。
 *
 * <p>进度由 tick 计数驱动而非 OpenAL SEC_OFFSET，因为唱片为 streaming 音源，
 * SEC_OFFSET 只反映当前缓冲区组内偏移（约 1 秒粒度），会反复跳变。</p>
 */
public final class DungeonJukeboxSoundInstance extends AbstractSoundInstance implements TickableSoundInstance {

    private final Runnable onComplete;
    private final float durationSeconds;
    private int ticksElapsed;
    private boolean stopped;
    private boolean completed;

    public DungeonJukeboxSoundInstance(Identifier soundId, float durationSeconds, Runnable onComplete) {
        super(soundId, SoundSource.RECORDS, RandomSource.create());
        this.durationSeconds = durationSeconds;
        this.onComplete = onComplete;
        this.volume = 1.0F;
        this.pitch = 1.0F;
        this.looping = false;
        this.relative = true;
        this.attenuation = SoundInstance.Attenuation.NONE;
        this.x = 0;
        this.y = 0;
        this.z = 0;
    }

    @Override
    public boolean isStopped() {
        return this.stopped;
    }

    /** 获取唱片总时长（秒） */
    public float getDuration() {
        return durationSeconds;
    }

    /** 获取当前播放位置（秒），基于 tick 计数 */
    public float getCurrentPosition() {
        return ticksElapsed / 20.0F;
    }

    @Override
    public void tick() {
        if (stopped) return;
        ticksElapsed++;
        var manager = Minecraft.getInstance().getSoundManager();
        SoundEngine engine = ((SoundManagerAccessor) manager).getSoundEngine();
        ChannelAccess.ChannelHandle handle = ((SoundEngineAccessor) engine).getInstanceToChannel().get(this);
        if (handle != null && handle.isStopped()) {
            stopped = true;
            this.looping = false;
            if (!completed && onComplete != null) {
                completed = true;
                // 延迟到下一 tick，避免在 SoundEngine 遍历 tickingSounds 时新增元素 → CME
                Scheduler.schedule(1, onComplete);
            }
        }
    }
}
