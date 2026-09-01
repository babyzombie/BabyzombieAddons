package top.babyzombie.addons.mixin.resource;

import net.minecraft.client.gui.screens.packs.PackSelectionModel;
import net.minecraft.server.packs.repository.Pack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.Set;

/**
 * 在"资源包"选择页面里隐藏模组内置资源包(chroma_modernui / 自定义唱片)。
 *
 * <p>这两个包都是 <b>required</b> 注册的:
 * <ul>
 *     <li>{@code babyzombieaddons:chroma_modernui} —— Fabric
 *         {@code ResourceLoader.registerBuiltinPack(..., ALWAYS_ENABLED)},required=true;</li>
 *     <li>{@code babyzombieaddons_custom_discs} —— {@link PackRepositoryMixin} 注入,
 *         {@code PackSelectionConfig(true, TOP, false)},required=true。</li>
 * </ul>
 * 因此无论仓库的选中列表怎么被提交,{@code PackRepository.rebuildSelected()} 都会把
 * required 包重新插入选中集合——资源包始终保持启用。
 *
 * <p>这里只做 UI 层的剔除:在 {@link PackSelectionModel} 的已选/可选列表里移除这两个
 * 包 ID,使它们在页面里完全不显示、也无法被玩家手动开关;不触碰仓库层的实际状态。
 */
@Mixin(PackSelectionModel.class)
public abstract class PackSelectionModelMixin {

    /** 需要在资源包页面隐藏的模组内置资源包 ID。 */
    @Unique
    private static final Set<String> HIDDEN_PACK_IDS = Set.of(
            "babyzombieaddons:chroma_modernui",
            "babyzombieaddons_custom_discs"
    );

    @Shadow
    private List<Pack> selected;

    @Shadow
    private List<Pack> unselected;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void bza$hideBuiltinPacksOnInit(CallbackInfo ci) {
        hideBuiltinPacks();
    }

    @Inject(method = "findNewPacks", at = @At("RETURN"))
    private void bza$hideBuiltinPacksOnReload(CallbackInfo ci) {
        hideBuiltinPacks();
    }

    /** 把隐藏包从已选/可选列表里剔除(列表保持可变,removeIf 安全)。 */
    @Unique
    private void hideBuiltinPacks() {
        this.selected.removeIf(pack -> HIDDEN_PACK_IDS.contains(pack.getId()));
        this.unselected.removeIf(pack -> HIDDEN_PACK_IDS.contains(pack.getId()));
    }
}
