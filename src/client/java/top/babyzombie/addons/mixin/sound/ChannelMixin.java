package top.babyzombie.addons.mixin.sound;

import com.mojang.blaze3d.audio.Channel;
import net.minecraft.client.sounds.AudioStream;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import top.babyzombie.addons.util.PlaySoundHelper;

/**
 * 在 Channel.attachBufferStream() 调用 pumpBuffers 之前拦截，
 * 将 AudioStream 推进到目标 seek 位置，使初始排队的缓冲区
 * 直接从正确位置开始。
 */
@Mixin(Channel.class)
public class ChannelMixin {
    @Inject(method = "attachBufferStream", at = @At("HEAD"))
    private void onAttachBufferStream(AudioStream stream, CallbackInfo ci) {
        PlaySoundHelper.applyPendingSeek((Channel) (Object) this, stream);
    }
}
