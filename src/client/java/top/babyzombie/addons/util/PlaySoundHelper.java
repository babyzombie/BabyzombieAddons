package top.babyzombie.addons.util;

import com.mojang.blaze3d.audio.Channel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.AudioStream;
import net.minecraft.client.sounds.ChannelAccess;
import net.minecraft.client.sounds.SoundEngine;
import top.babyzombie.addons.mixin.sound.ChannelAccessor;
import top.babyzombie.addons.mixin.sound.ChannelHandleAccessor;
import top.babyzombie.addons.mixin.sound.ChannelStreamAccessor;
import top.babyzombie.addons.mixin.sound.SoundEngineAccessor;
import top.babyzombie.addons.mixin.sound.SoundManagerAccessor;
import org.lwjgl.openal.AL10;
import org.lwjgl.openal.AL11;

import javax.sound.sampled.AudioFormat;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.IdentityHashMap;
import java.util.Map;

/**
 * 播放声音并支持 seek（从中间开始播放）和定时关闭。
 *
 * <p>架构说明：
 * <ul>
 *   <li><b>静态源</b>（短音频，&lt;1MB）：直接使用 {@code AL_SEC_OFFSET}</li>
 *   <li><b>流式源</b>（长音频，≥1MB）：通过拦截 {@code Channel.attachBufferStream()}
 *       在初始缓冲区排队之前将 AudioStream 推进到目标位置，彻底消除竞态条件
 *       和 4 秒缓冲区窗口限制</li>
 * </ul>
 */
public final class PlaySoundHelper {
    private PlaySoundHelper() {}

    /**
     * 等待 Channel.attachBufferStream() 被调用时应用的 seek 偏移。
     * 由 {@code ChannelMixin} 在拦截点消费。
     */
    static final Map<Channel, Float> PENDING_SEEKS = new IdentityHashMap<>();

    /**
     * 播放声音，从指定偏移开始，持续指定时长后自动停止。
     *
     * @param instance        声音实例
     * @param seekSeconds     从第几秒开始播放
     * @param durationSeconds 播放多少秒后停止
     */
    public static void playSeeked(SoundInstance instance, float seekSeconds, float durationSeconds) {
        var client = Minecraft.getInstance();
        client.getSoundManager().play(instance);

        SoundEngine engine = ((SoundManagerAccessor) client.getSoundManager()).getSoundEngine();
        ((SoundEngineAccessor) engine).getExecutor().execute(() -> {
            Map<SoundInstance, ChannelAccess.ChannelHandle> map =
                    ((SoundEngineAccessor) engine).getInstanceToChannel();
            ChannelAccess.ChannelHandle handle = map.get(instance);
            if (handle == null) return;

            Channel channel = ((ChannelHandleAccessor) handle).getChannel();
            if (channel == null) return;

            int source = ((ChannelAccessor) channel).getSource();

            // 静态源：AL_SEC_OFFSET 直接生效
            if (AL10.alGetSourcei(source, AL10.AL_BUFFER) != 0) {
                AL11.alSourcef(source, AL11.AL_SEC_OFFSET, seekSeconds);
                return;
            }

            // 流式源：先注册 pending seek（供 mixin 在 attachBufferStream 时消费），
            // 再检查是否已经 attached（缓存命中时 stream 已存在）
            PENDING_SEEKS.put(channel, seekSeconds);

            AudioStream stream = ((ChannelStreamAccessor) channel).getStream();
            if (stream != null) {
                // 流已附加（缓存命中）：mixin 不会再被调用，这里直接处理
                int queued = AL10.alGetSourcei(source, AL10.AL_BUFFERS_QUEUED);
                if (queued > 0) {
                    applySeekToStream(source, stream, queued, seekSeconds);
                    PENDING_SEEKS.remove(channel);
                }
            }
            // 流未附加（异步加载）：mixin 会在 attachBufferStream 时消费 PENDING_SEEKS
        });

        Scheduler.schedule((int) (durationSeconds * 20),
                () -> client.getSoundManager().stop(instance));
    }

    /**
     * 由 {@code ChannelMixin} 在 {@code attachBufferStream} 开头调用。
     * 如果存在为此 channel 注册的 pending seek，则推进 stream 并返回 true。
     */
    public static void applyPendingSeek(Channel channel, AudioStream stream) {
        Float seekSeconds = PENDING_SEEKS.remove(channel);
        if (seekSeconds != null && seekSeconds > 0) {
            advanceStream(stream, seekSeconds);
        }
    }

    /**
     * 对流式源执行 seek（流已附加、缓冲区已排队的情况）。
     *
     * <p>此时初始缓冲区已从位置 0 排队，需要推进 stream + 用 AL_SEC_OFFSET
     * 跳过这些缓冲区。推进量为 {@code seekSeconds - queued}。</p>
     */
    private static void applySeekToStream(int source, AudioStream stream,
                                          int queuedBuffers, float seekSeconds) {
        float maxSeekable = (float) queuedBuffers; // BUFFER_DURATION_SECONDS = 1

        if (seekSeconds <= maxSeekable) {
            AL11.alSourcef(source, AL11.AL_SEC_OFFSET, seekSeconds);
        } else {
            advanceStream(stream, seekSeconds - maxSeekable);
            AL11.alSourcef(source, AL11.AL_SEC_OFFSET, maxSeekable);
        }
    }

    /**
     * 从 AudioStream 读取并丢弃指定秒数的数据，推进解码器位置。
     * 同时供 {@code ChannelMixin} 调用。
     */
    public static void advanceStream(AudioStream stream, float seconds) {
        AudioFormat format = stream.getFormat();
        int bytesPerSecond = (int) (format.getSampleRate()
                * format.getSampleSizeInBits() / 8.0f
                * format.getChannels());
        int remaining = (int) (seconds * bytesPerSecond);

        while (remaining > 0) {
            try {
                ByteBuffer buf = stream.read(Math.min(bytesPerSecond, remaining));
                if (buf == null || buf.remaining() == 0) break;
                remaining -= buf.remaining();
            } catch (IOException e) {
                break;
            }
        }
    }
}
