package top.babyzombie.addons.mixin.network;

import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import top.babyzombie.addons.module.chat.playcmd.PlayAutocomplete;

@Mixin(targets = "net.minecraft.network.protocol.game.ClientboundCommandsPacket$NodeResolver")
public class ClientboundCommandsPacketMixin {

    @Inject(
            method = "resolve(I)Lcom/mojang/brigadier/tree/CommandNode;",
            at = @At("RETURN"),
            cancellable = true
    )
    private void babyzombieaddons$replacePlayNode(CallbackInfoReturnable<CommandNode> cir) {
        CommandNode node = cir.getReturnValue();
        if (node instanceof LiteralCommandNode literalNode
                && "play".equals(literalNode.getLiteral())
                && PlayAutocomplete.commandNode != null) {
            cir.setReturnValue(PlayAutocomplete.commandNode);
        }
    }
}