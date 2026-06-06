package top.babyzombie.addons.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PingDebugMonitor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import top.babyzombie.addons.util.ServerTickCounter;

@Mixin(PingDebugMonitor.class)
public class PingDebugMonitorMixin {

    @ModifyArg(method = "onPongReceived", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/debugchart/LocalSampleLogger;logSample(J)V"))
    private long babyzombieAddons$onPingResult(long ping) {
        Minecraft.getInstance().execute(() -> ServerTickCounter.onPingResult(ping));
        return ping;
    }
}
