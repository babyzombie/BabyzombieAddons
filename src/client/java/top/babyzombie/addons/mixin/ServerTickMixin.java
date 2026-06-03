package top.babyzombie.addons.mixin;

import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundSetTimePacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import top.babyzombie.addons.util.ServerTick;

/**
 * Hooks ClientPacketListener.handleSetTime to drive ServerTick,
 * matching the JS behavior of incrementing on each per-tick packet.
 * ClientboundSetTimePacket is the 1.21 equivalent of 1.8.9's
 * S32PacketConfirmTransaction — one per server tick.
 */
@Mixin(ClientPacketListener.class)
public class ServerTickMixin {

    @Inject(method = "handleSetTime", at = @At("HEAD"))
    private void onSetTime(ClientboundSetTimePacket packet, CallbackInfo ci) {
        ServerTick.onPacket();
    }
}
