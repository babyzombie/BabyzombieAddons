package top.babyzombie.addons.module.misc;

import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.HugeExplosionParticle;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.StringTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import top.babyzombie.addons.config.ModConfigManager;
import top.babyzombie.addons.event.ParticleRenderEvents;
import top.babyzombie.addons.event.PlaySoundEvents;
import top.babyzombie.addons.util.ItemUtils;
import top.babyzombie.addons.util.tracker.HypixelLocationTracker;

import java.util.Set;

/**
 * Controls sound volume and particles for Necron's Blade (Hyperion/Scylla/Valkyrie/Astraea)
 * and teleport swords (Aspect of the End / Aspect of the Void).
 */
public final class NecronBladeModule {

    private static final Set<String> WITHER_BLADE_IDS = Set.of(
            "NECRON_BLADE", "HYPERION", "VALKYRIE", "SCYLLA", "ASTRAEA"
    );
    private static final Set<String> TELEPORT_SWORD_IDS = Set.of(
            "ASPECT_OF_THE_END", "ASPECT_OF_THE_VOID"
    );
    private static final double SELF_RADIUS = 3.0;
    private static final double OTHERS_RADIUS = 1.0;

    private NecronBladeModule() {}

    public static void init() {
        PlaySoundEvents.MODIFY.register(NecronBladeModule::modifySound);
        ParticleRenderEvents.BEFORE_ADD.register(NecronBladeModule::cancelParticle);
    }

    // ===== Sound =====

    private static SoundInstance modifySound(SoundInstance sound) {
        var theSound = sound.getSound();
        if (theSound == null) return sound;
        String path = theSound.getLocation().getPath();

        var player = Minecraft.getInstance().player;
        if (player == null) return sound;

        // Explosion sound — requires Implosion scroll + near player
        if (path.startsWith("random/explode") && shouldModifyExplosion() && isNearPlayerSound(sound, player)) {
            return applyVolume(sound, ModConfigManager.get().skyblock.necronBlade.explosionVolume);
        }

        // Only process ability sounds near the player
        if (!isNearPlayerSound(sound, player)) return sound;

        // Shadow warp / teleport sound
        if (path.startsWith("mob/endermen/portal")) {
            // Teleport sword takes priority
            if (isHoldingTeleportSword()) {
                return applyVolume(sound, ModConfigManager.get().skyblock.teleportSword.teleportVolume);
            }
            if (isHoldingWitherBlade()) {
                return applyVolume(sound, ModConfigManager.get().skyblock.necronBlade.shadowWarpVolume);
            }
        }

        // Wither shield sound
        if (path.equals("mob/zombie/remedy") && isHoldingWitherBlade()) {
            return applyVolume(sound, ModConfigManager.get().skyblock.necronBlade.witherShieldVolume);
        }

        // Etherwarp sound (teleport sword)
        if (path.startsWith("mob/enderdragon/hit") && isHoldingTeleportSword()) {
            return applyVolume(sound, ModConfigManager.get().skyblock.teleportSword.etherwarpVolume);
        }

        return sound;
    }

    private static boolean isNearPlayerSound(SoundInstance sound, Player player) {
        double dx = sound.getX() - player.getX();
        double dy = sound.getY() - player.getY();
        double dz = sound.getZ() - player.getZ();
        return dx * dx + dy * dy + dz * dz <= SELF_RADIUS * SELF_RADIUS;
    }

    /** Applies a volume override to the sound if volume < 1.0. */
    private static SoundInstance applyVolume(SoundInstance sound, float volume) {
        if (volume >= 1.0f) return sound;
        var resolved = sound.getSound();
        return new SimpleSoundInstance(
                sound.getIdentifier(), sound.getSource(), volume, sound.getPitch(),
                RandomSource.create(), sound.isLooping(), sound.getDelay(), sound.getAttenuation(),
                sound.getX(), sound.getY(), sound.getZ(), sound.isRelative()
        ) {{
            this.sound = resolved;
        }
        @Override public float getVolume() { return volume; }};
    }

    // ===== Particles =====

    /** @return true to cancel the particle */
    private static boolean cancelParticle(Particle particle) {
        if (!(particle instanceof HugeExplosionParticle)) return false;
        var cfg = ModConfigManager.get().skyblock;

        // Self: full check (ID + Implosion scroll + in Skyblock)
        if (cfg.necronBlade.hideExplosionParticles && shouldModifyExplosion()
                && isNearPlayer(particle, Minecraft.getInstance().player, SELF_RADIUS)) return true;

        // Others: ID whitelist only + distance 1
        if (cfg.necronBlade.hideOthersParticles && isNearWitherBladeUser(particle)) return true;

        return false;
    }

    private static boolean isNearWitherBladeUser(Particle particle) {
        var player = Minecraft.getInstance().player;
        if (player == null || player.level() == null) return false;
        var center = particle.getBoundingBox().getCenter();
        var range = new AABB(
                center.x - OTHERS_RADIUS, center.y - OTHERS_RADIUS, center.z - OTHERS_RADIUS,
                center.x + OTHERS_RADIUS, center.y + OTHERS_RADIUS, center.z + OTHERS_RADIUS
        );
        for (var other : player.level().getEntitiesOfClass(Player.class, range)) {
            if (other == player) continue;
            var held = other.getMainHandItem();
            if (ItemUtils.getSkyblockId(held) != null && WITHER_BLADE_IDS.contains(ItemUtils.getSkyblockId(held))) return true;
        }
        return false;
    }

    private static boolean isNearPlayer(Particle particle, Player player, double radius) {
        if (player == null) return false;
        var center = particle.getBoundingBox().getCenter();
        double dx = center.x - player.getX();
        double dy = center.y - player.getY();
        double dz = center.z - player.getZ();
        return dx * dx + dy * dy + dz * dz <= radius * radius;
    }

    // ===== Shared =====

    /** Checks if the player is holding a Necron's Blade with Implosion scroll in Skyblock. */
    private static boolean shouldModifyExplosion() {
        var player = Minecraft.getInstance().player;
        if (player == null) return false;
        if (!HypixelLocationTracker.getInstance().isInSkyblock()) return false;
        return hasImplosionScroll(player.getMainHandItem());
    }

    /** Checks if the player is holding any wither blade in Skyblock. */
    private static boolean isHoldingWitherBlade() {
        var player = Minecraft.getInstance().player;
        if (player == null) return false;
        if (!HypixelLocationTracker.getInstance().isInSkyblock()) return false;
        String id = ItemUtils.getSkyblockId(player.getMainHandItem());
        return id != null && WITHER_BLADE_IDS.contains(id);
    }

    /** Checks if the player is holding a teleport sword in Skyblock. */
    private static boolean isHoldingTeleportSword() {
        var player = Minecraft.getInstance().player;
        if (player == null) return false;
        if (!HypixelLocationTracker.getInstance().isInSkyblock()) return false;
        String id = ItemUtils.getSkyblockId(player.getMainHandItem());
        return id != null && TELEPORT_SWORD_IDS.contains(id);
    }

    static boolean hasImplosionScroll(ItemStack stack) {
        String id = ItemUtils.getSkyblockId(stack);
        if (id == null || !WITHER_BLADE_IDS.contains(id)) return false;
        var customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData == null) return false;
        var scrolls = customData.copyTag().getList("ability_scroll").orElse(null);
        if (scrolls == null) return false;
        return scrolls.stream().anyMatch(t -> t instanceof StringTag(String s) && "IMPLOSION_SCROLL".equals(s));
    }
}
