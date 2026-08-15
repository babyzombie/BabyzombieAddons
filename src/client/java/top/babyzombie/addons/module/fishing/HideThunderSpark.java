package top.babyzombie.addons.module.fishing;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLevelEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.decoration.ArmorStand;
import top.babyzombie.addons.config.ModConfigManager;
import top.babyzombie.addons.event.EntityRenderEvents;
import top.babyzombie.addons.event.ParticleRenderEvents;
import top.babyzombie.addons.event.PlaySoundEvents;
import top.babyzombie.addons.util.ChatUtils;
import top.babyzombie.addons.util.ItemUtils;
import top.babyzombie.addons.util.PlaySoundHelper;
import top.babyzombie.addons.util.tracker.HypixelLocationTracker;

import java.util.ArrayList;
import java.util.List;

public class HideThunderSpark {
    private final static String SPARK_TEXTURE = "ewogICJ0aW1lc3RhbXAiIDogMTY0MzUwNDM3MjI1NiwKICAicHJvZmlsZUlkIiA6ICI2MzMyMDgwZTY3YTI0Y2MxYjE3ZGJhNzZmM2MwMGYxZCIsCiAgInByb2ZpbGVOYW1lIiA6ICJUZWFtSHlkcmEiLAogICJzaWduYXR1cmVSZXF1aXJlZCIgOiB0cnVlLAogICJ0ZXh0dXJlcyIgOiB7CiAgICAiU0tJTiIgOiB7CiAgICAgICJ1cmwiIDogImh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvN2IzMzI4ZDNlOWQ3MTA0MjAzMjI1NTViMTcyMzkzMDdmMTIyNzBhZGY4MWJmNjNhZmM1MGZhYTA0YjVjMDZlMSIsCiAgICAgICJtZXRhZGF0YSIgOiB7CiAgICAgICAgIm1vZGVsIiA6ICJzbGltIgogICAgICB9CiAgICB9CiAgfQp9";
    private final static String SPARK_MESSAGE = "Try clicking this Thunder Spark with an Empty Thunder Bottle to collect it!";
    private final static double ARMOR_STAND_SCAN_RANGE = 32.0;

    private final static List<ArmorStand> sparks = new ArrayList<>();

    private HideThunderSpark() {}

    public static void init() {
        //先寻找周围的碎片
        ClientTickEvents.END_CLIENT_TICK.register((client -> {
            if (!HypixelLocationTracker.getInstance().isInCrimson()) return;
            if (client.level == null || client.player == null) return;
            if (client.player.tickCount % 10 != 0) return;
            sparks.clear();
            var ab = client.player.getBoundingBox().inflate(ARMOR_STAND_SCAN_RANGE);
            var entities = client.level.getEntitiesOfClass(ArmorStand.class, ab, e -> {
                var item = e.getMainHandItem();
                return !item.isEmpty() && SPARK_TEXTURE.equals(ItemUtils.getSkullTexture(item));
            });
            sparks.addAll(entities);
        }));

        //跨世界清除列表
        ClientLevelEvents.AFTER_CLIENT_LEVEL_CHANGE.register((_, _) -> sparks.clear());

        //点击消息屏蔽
        ClientReceiveMessageEvents.ALLOW_GAME.register(((message, overlay) -> {
            if (overlay || !HypixelLocationTracker.getInstance().isInCrimson() || !ModConfigManager.get().fishing.thunderSpark.message) return true;
            return !ChatUtils.stripColor(message.getString()).equals(SPARK_MESSAGE);
        }));

        //声音屏蔽
        PlaySoundEvents.MODIFY.register((sound) -> {
            if (!HypixelLocationTracker.getInstance().isInCrimson() || ModConfigManager.get().fishing.thunderSpark.sound >= 1) return sound;
            if (!sound.getIdentifier().equals(SoundEvents.ELDER_GUARDIAN_AMBIENT.location())) return sound;
            for (ArmorStand armorStand : sparks) {
                if (armorStand.getBoundingBox().inflate(1.0).contains(sound.getX(), sound.getY(), sound.getZ()))
                    return PlaySoundHelper.withVolume(sound, ModConfigManager.get().fishing.thunderSpark.sound);
            }
            return sound;
        });

        //盔甲架渲染屏蔽
        EntityRenderEvents.BEFORE_RENDER.register(entity -> {
            if (!HypixelLocationTracker.getInstance().isInCrimson()) return false;
            if (!ModConfigManager.get().fishing.thunderSpark.hide) return false;
            if (entity instanceof ArmorStand armorStand) {
                var item = armorStand.getMainHandItem();
                return !item.isEmpty() && SPARK_TEXTURE.equals(ItemUtils.getSkullTexture(item));
            }
            return false;
        });

        //粒子渲染屏蔽
        ParticleRenderEvents.BEFORE_CREATE.register((options, x, y, z, xa, ya, za) -> {
            if (!HypixelLocationTracker.getInstance().isInCrimson() || !ModConfigManager.get().fishing.thunderSpark.hide) return false;
            if (!options.getType().equals(ParticleTypes.FIREWORK)) return false;
            for (ArmorStand armorStand : sparks) {
                if (armorStand.getBoundingBox().inflate(1.0).contains(x, y, z)) return true;
            }
            return false;
        });
    }
}
