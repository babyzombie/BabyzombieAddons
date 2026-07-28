package top.babyzombie.addons.mixin.resource;

import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.PackLocationInfo;
import net.minecraft.server.packs.PackSelectionConfig;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.KnownPack;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.server.packs.repository.RepositorySource;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import top.babyzombie.addons.module.dungeon.CustomDiscPackResources;

import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;

/**
 * 在资源包仓库里注入自定义唱片的虚拟资源包，
 * 使 {@code config/babyzombieaddons/custom_discs/} 下的 .ogg 文件
 * 能被 MC 的 SoundManager 正常加载和播放。
 */
@Mixin(PackRepository.class)
public abstract class PackRepositoryMixin {

    @Shadow
    @Final
    @Mutable
    private Set<RepositorySource> sources;

    /** 自定义唱片资源包的虚拟 ID，确保不被用户手动禁用。 */
    @Unique
    private static final String CUSTOM_DISC_PACK_ID = "babyzombieaddons_custom_discs";

    @Unique
    private static final PackSource CUSTOM_DISC_PACK_SOURCE = PackSource.BUILT_IN;

    @SuppressWarnings("OptionalGetWithoutIsPresent")
    @Inject(method = "<init>", at = @At("RETURN"))
    private void addCustomDiscPackSource(CallbackInfo ci) {
        // Fabric 的 PackRepositoryMixin 已把 sources 转为 LinkedHashSet；
        // 手动再确保一次以处理混入加载顺序不确定的情况。
        if (!(this.sources instanceof LinkedHashSet)) {
            this.sources = new LinkedHashSet<>(this.sources);
        }

        this.sources.add((RepositorySource) consumer -> {
            PackLocationInfo location = new PackLocationInfo(
                    CUSTOM_DISC_PACK_ID,
                    Component.translatable("resourcepack.babyzombieaddons.custom_discs.name"),
                    CUSTOM_DISC_PACK_SOURCE,
                    Optional.of(new KnownPack("babyzombieaddons", "custom_discs", "1"))
            );

            CustomDiscPackResources packResources = new CustomDiscPackResources(location);

            Pack profile = Pack.readMetaAndCreate(
                    location,
                    new Pack.ResourcesSupplier() {
                        @Override
                        public net.minecraft.server.packs.PackResources openPrimary(
                                PackLocationInfo loc) {
                            return packResources;
                        }

                        @Override
                        public net.minecraft.server.packs.PackResources openFull(
                                PackLocationInfo loc, Pack.Metadata metadata) {
                            return packResources;
                        }
                    },
                    PackType.CLIENT_RESOURCES,
                    new PackSelectionConfig(true, Pack.Position.TOP, false)
            );

            if (profile != null) {
                consumer.accept(profile);
            }
        });
    }
}
