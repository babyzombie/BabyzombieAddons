package top.babyzombie.addons.mixin.network;

import net.minecraft.client.multiplayer.ClientCommonPacketListenerImpl;
import net.minecraft.network.protocol.common.ClientboundPingPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import top.babyzombie.addons.util.ServerTickCounter;

@Mixin(ClientCommonPacketListenerImpl.class)
public class ClientCommonPacketListenerImplMixin {

    @Inject(method = "handlePing", at = @At("RETURN"))
    private void babyzombieAddons$onServerTick(ClientboundPingPacket packet, CallbackInfo ci) {
        ServerTickCounter.onServerTick(packet);
    }
}
