package top.babyzombie.addons.mixin.sound;

import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.ChannelAccess;
import net.minecraft.client.sounds.SoundEngine;
import net.minecraft.client.sounds.SoundEngineExecutor;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundSource;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.Map;

@Mixin(SoundEngine.class)
public interface SoundEngineAccessor {
    @Accessor("instanceToChannel")
    Map<SoundInstance, ChannelAccess.ChannelHandle> getInstanceToChannel();

    @Accessor
    SoundEngineExecutor getExecutor();

    @Invoker("stop")
    void invokeStop(@Nullable Identifier sound, @Nullable SoundSource source);
}
