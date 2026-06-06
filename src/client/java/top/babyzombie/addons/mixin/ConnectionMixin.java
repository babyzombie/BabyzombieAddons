package top.babyzombie.addons.mixin;

import net.minecraft.network.Connection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import top.babyzombie.addons.util.ServerTickCounter;

@Mixin(Connection.class)
public class ConnectionMixin {

    @Inject(method = "genericsFtw", at = @At("HEAD"))
    private static void babyzombieAddons$onReceivePacket(CallbackInfo ci) {
        ServerTickCounter.onReceivePacket();
    }
}
