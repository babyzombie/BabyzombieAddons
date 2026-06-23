package top.babyzombie.addons.util;

import com.mojang.blaze3d.audio.Channel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.ChannelAccess;
import net.minecraft.client.sounds.SoundEngine;
import top.babyzombie.addons.mixin.ChannelAccessor;
import top.babyzombie.addons.mixin.ChannelHandleAccessor;
import top.babyzombie.addons.mixin.SoundEngineAccessor;
import top.babyzombie.addons.mixin.SoundManagerAccessor;
import org.lwjgl.openal.AL11;

import java.util.Map;

/**
 * 播放声音并支持 seek（从中间开始播放）和定时关闭。
 */
public final class PlaySoundHelper {
    private PlaySoundHelper() {}

    /**
     * 播放声音，从指定偏移开始，持续指定时长后自动停止。
     *
     * @param instance       声音实例
     * @param seekSeconds    从第几秒开始播放
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
            if (handle != null) {
                Channel channel = ((ChannelHandleAccessor) handle).getChannel();
                if (channel != null) {
                    int source = ((ChannelAccessor) channel).getSource();
                    AL11.alSourcef(source, AL11.AL_SEC_OFFSET, seekSeconds);
                }
            }
        });

        Scheduler.schedule((int) (durationSeconds * 20),
                () -> client.getSoundManager().stop(instance));
    }
}
