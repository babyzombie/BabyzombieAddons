package top.babyzombie.addons.mixin.network;

import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import top.babyzombie.addons.module.chat.PartyCommandAutocomplete;
import top.babyzombie.addons.module.chat.playcmd.PlayAutocomplete;
import top.babyzombie.addons.util.tracker.HypixelLocationTracker;

@Mixin(targets = "net.minecraft.network.protocol.game.ClientboundCommandsPacket$NodeResolver")
public class ClientboundCommandsPacketMixin {

    @Inject(
            method = "resolve(I)Lcom/mojang/brigadier/tree/CommandNode;",
            at = @At("RETURN"),
            cancellable = true
    )
    private void babyzombieaddons$replaceCommandNodes(CallbackInfoReturnable<CommandNode> cir) {
        CommandNode node = cir.getReturnValue();
        if (HypixelLocationTracker.getInstance().isOnHypixel() && node instanceof LiteralCommandNode literalNode) {
            String literal = literalNode.getLiteral();
            switch (literal) {
                case "play":
                    if (PlayAutocomplete.commandNode != null) cir.setReturnValue(PlayAutocomplete.commandNode);
                    break;
                case "pc":
                    if (PartyCommandAutocomplete.commandNode != null) cir.setReturnValue(PartyCommandAutocomplete.commandNode);
                    break;
            }
        }
    }
}