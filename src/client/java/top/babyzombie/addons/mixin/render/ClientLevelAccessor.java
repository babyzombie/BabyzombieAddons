package top.babyzombie.addons.mixin.render;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.entity.TransientEntitySectionManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * 暴露 ClientLevel 的 entityStorage，用于客户端创建 BlockDisplay 实体。
 */
@Mixin(ClientLevel.class)
public interface ClientLevelAccessor {
    @Accessor("entityStorage")
    TransientEntitySectionManager<Entity> getEntityStorage();
}
