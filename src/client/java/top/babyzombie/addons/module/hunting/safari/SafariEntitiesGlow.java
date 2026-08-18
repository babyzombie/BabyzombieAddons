package top.babyzombie.addons.module.hunting.safari;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ambient.Bat;
import net.minecraft.world.entity.animal.parrot.Parrot;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.monster.Shulker;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.CalibratedSculkSensorBlockEntity;
import top.babyzombie.addons.config.HuntingConfig;
import top.babyzombie.addons.config.ModConfigManager;
import top.babyzombie.addons.util.ItemUtils;
import top.babyzombie.addons.util.ServerTick;
import top.babyzombie.addons.util.render.GlowController;
import top.babyzombie.addons.util.tracker.HypixelLocationTracker;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashSet;
import java.util.Set;

/**
 * 在 Safari 区域按群系分区高亮目标生物，每个生物独立开关和颜色（深度测试发光）：
 * - Icy 雪地：热带鱼/海豚/荧光鱿鱼/北极熊/雪傀儡/山羊/劫掠兽 + 头颅目标（Mantis Shrimp/Troodon 物品展示）
 * - Haunted：Hideonwall（潜影贝）/Hideyho NPC/蝙蝠/Duplico 物品展示/Warden/较频幽匿感测体/末影螨/洞穴蜘蛛/幻翼
 *           + 头颅目标（Gimmiegold 物品展示）
 * - Cavern 洞穴：热带鱼/犰狳/嗅探兽/蠹虫/恼鬼 + 头颅目标（Flitter/Chuckwalla 物品展示）
 * - Forest 森林：Hideonfloor（潜影贝）/狐狸/熊猫/嘎吱/青蛙/鹦鹉/蜜蜂
 * 头颅目标用 skull texture 材质匹配；重复生物（热带鱼）按实体所在分区区分；
 * 隐身的实体不发光（Display 例外：隐身时仍渲染展示内容）。
 */
public final class SafariEntitiesGlow {

    private static final String HIDEYHO_NAME = "Hideyho ";
    private static final int SCULK_SENSOR_RANGE = 32;
    private static final int SCULK_SENSOR_RANGE_SQ = SCULK_SENSOR_RANGE * SCULK_SENSOR_RANGE;

    /** Duplico 物品展示实体可能展示的物品 */
    private static final Set<Item> DUPLICO_ITEMS = Set.of(
        Items.BOOKSHELF,Items.CHERRY_WOOD,Items.CHERRY_LOG,Items.DEEPSLATE,Items.COBBLED_DEEPSLATE
    );

    // ── 头颅目标：skull texture URL 的唯一片段（每个 Safari 目标生物一个）──
    private static final String MANTIS_SHRIMP_SKULL = "9924c105aa431dab";
    private static final String TROODON_SKULL = "53de4135a3b19a21";
    private static final String FLITTER_SKULL = "a89a76deedd42b41";
    private static final String CHUCKWALLA_SKULL = "fc63cd0d480971a7";
    private static final String GIMMIEGOLD_SKULL = "8b329e108ac28b0b";
    private static final String GAZER_SKULL = "407b3c3d2c3fe259";
    private static final String SHYWORM_SKULL = "a684e00e7394cb0c";
    private static final String DRIFTLING_SKULL = "f4c4f8e5fce1ec2d";

    // Warden 战斗场地范围
    private static final int ARENA_X_MIN = -18, ARENA_X_MAX = 24;
    private static final int ARENA_Y_MIN = 45, ARENA_Y_MAX = 62;
    private static final int ARENA_Z_MIN = -39, ARENA_Z_MAX = -13;

    /** 上一次 tick 高亮的较频幽匿感测体位置 */
    private static final Set<BlockPos> sculkSensorHighlighted = new HashSet<>();

    private SafariEntitiesGlow() {}

    public static void init() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null || client.level == null) return;
            if (!HypixelLocationTracker.getInstance().isInSafari()) return;

            var safari = ModConfigManager.get().hunting.safari;

            // === 实体发光（按群系分区 + 每生物独立开关） ===
            if (anyEnabled(safari.icy) || anyEnabled(safari.haunted)
                    || anyEnabled(safari.cavern) || anyEnabled(safari.forest)) {
                for (var entity : client.level.entitiesForRendering()) {
                    // 隐身的实体不发光（Display 例外：隐身时仍渲染展示内容）
                    if (entity.isInvisible() && !(entity instanceof Display)) continue;

                    switch (SafariZoneUtil.zoneOf(entity.blockPosition())) {
                        case ICY -> {
                            var icy = safari.icy;
                            if (icy.tropicalFishGlow && entity.getType() == EntityType.TROPICAL_FISH) {
                                setGlow(entity, icy.tropicalFishGlowColor.getEffectiveColourRGB());
                            }
                            if (icy.dolphinGlow && entity.getType() == EntityType.DOLPHIN) {
                                setGlow(entity, icy.dolphinGlowColor.getEffectiveColourRGB());
                            }
                            if (icy.glowSquidGlow && entity.getType() == EntityType.GLOW_SQUID) {
                                setGlow(entity, icy.glowSquidGlowColor.getEffectiveColourRGB());
                            }
                            if (icy.polarBearGlow && entity.getType() == EntityType.POLAR_BEAR) {
                                setGlow(entity, icy.polarBearGlowColor.getEffectiveColourRGB());
                            }
                            if (icy.snowGolemGlow && entity.getType() == EntityType.SNOW_GOLEM) {
                                setGlow(entity, icy.snowGolemGlowColor.getEffectiveColourRGB());
                            }
                            if (icy.goatGlow && entity.getType() == EntityType.GOAT) {
                                setGlow(entity, icy.goatGlowColor.getEffectiveColourRGB());
                            }
                            if (icy.ravagerGlow && entity.getType() == EntityType.RAVAGER) {
                                setGlow(entity, icy.ravagerGlowColor.getEffectiveColourRGB());
                            }
                            if (icy.mantisShrimpGlow && isSkullItemDisplay(entity, MANTIS_SHRIMP_SKULL)) {
                                setGlow(entity, icy.mantisShrimpGlowColor.getEffectiveColourRGB());
                            }
                            if (icy.troodonGlow && isSkullItemDisplay(entity, TROODON_SKULL)) {
                                setGlow(entity, icy.troodonGlowColor.getEffectiveColourRGB());
                            }
                        }
                        case HAUNTED -> {
                            var haunted = safari.haunted;
                            if (haunted.hideonwallGlow && isShulkerLike(entity)) {
                                setGlow(entity, haunted.hideonwallGlowColor.getEffectiveColourRGB());
                            }
                            if (haunted.hideyhoGlow && entity instanceof Player player
                                    && HIDEYHO_NAME.equals(player.getName().getString())) {
                                setGlow(player, haunted.hideyhoGlowColor.getEffectiveColourRGB());
                            }
                            if (haunted.batGlow && entity instanceof Bat) {
                                setGlow(entity, haunted.batGlowColor.getEffectiveColourRGB());
                            }
                            if (haunted.duplicoGlow && isDuplico(entity)) {
                                setGlow(entity, haunted.duplicoGlowColor.getEffectiveColourRGB());
                            }
                            if (haunted.wardenGlow && entity instanceof Warden warden
                                    && isInArena(warden.blockPosition())) {
                                int cooldownColor = haunted.wardenGlowCooldownColor.getEffectiveColourRGB();
                                int readyColor = haunted.wardenGlowReadyColor.getEffectiveColourRGB();
                                int cooldownTicks = haunted.wardenCooldownTicks;
                                int color;
                                var pose = warden.getPose();
                                if (pose == net.minecraft.world.entity.Pose.EMERGING
                                        || pose == net.minecraft.world.entity.Pose.DIGGING) {
                                    // 登场动画 / 钻地 → 一定无敌
                                    color = cooldownColor;
                                } else {
                                    int ping = ServerTick.getPing();
                                    int delay = ping > 0 ? (int) Math.ceil(ping / 50.0) : 0;
                                    int compensated = warden.tickCount + delay;
                                    color = compensated < cooldownTicks ? cooldownColor : readyColor;
                                }
                                GlowController.setGlow(warden, true, color, true);
                            }
                            if (haunted.endermiteGlow && entity.getType() == EntityType.ENDERMITE) {
                                setGlow(entity, haunted.endermiteGlowColor.getEffectiveColourRGB());
                            }
                            if (haunted.caveSpiderGlow && entity.getType() == EntityType.CAVE_SPIDER) {
                                setGlow(entity, haunted.caveSpiderGlowColor.getEffectiveColourRGB());
                            }
                            if (haunted.phantomGlow && entity.getType() == EntityType.PHANTOM) {
                                setGlow(entity, haunted.phantomGlowColor.getEffectiveColourRGB());
                            }
                            if (haunted.gimmiegoldGlow && isSkullItemDisplay(entity, GIMMIEGOLD_SKULL)) {
                                setGlow(entity, haunted.gimmiegoldGlowColor.getEffectiveColourRGB());
                            }
                            if (haunted.gazerGlow && isArmorStandSkull(entity, EquipmentSlot.HEAD, GAZER_SKULL)) {
                                GlowController.setGlowSlots(entity, true,
                                    haunted.gazerGlowColor.getEffectiveColourRGB(), true, EquipmentSlot.HEAD);
                            }
                        }
                        case CAVERN -> {
                            var cavern = safari.cavern;
                            if (cavern.tropicalFishGlow && entity.getType() == EntityType.TROPICAL_FISH) {
                                setGlow(entity, cavern.tropicalFishGlowColor.getEffectiveColourRGB());
                            }
                            if (cavern.armadilloGlow && entity.getType() == EntityType.ARMADILLO) {
                                setGlow(entity, cavern.armadilloGlowColor.getEffectiveColourRGB());
                            }
                            if (cavern.snifferGlow && entity.getType() == EntityType.SNIFFER) {
                                setGlow(entity, cavern.snifferGlowColor.getEffectiveColourRGB());
                            }
                            if (cavern.silverfishGlow && entity.getType() == EntityType.SILVERFISH) {
                                setGlow(entity, cavern.silverfishGlowColor.getEffectiveColourRGB());
                            }
                            if (cavern.vexGlow && entity.getType() == EntityType.VEX) {
                                setGlow(entity, cavern.vexGlowColor.getEffectiveColourRGB());
                            }
                            if (cavern.flitterGlow && isSkullItemDisplay(entity, FLITTER_SKULL)) {
                                setGlow(entity, cavern.flitterGlowColor.getEffectiveColourRGB());
                            }
                            if (cavern.chuckwallaGlow && isSkullItemDisplay(entity, CHUCKWALLA_SKULL)) {
                                setGlow(entity, cavern.chuckwallaGlowColor.getEffectiveColourRGB());
                            }
                            if (cavern.shywormGlow && isArmorStandSkull(entity, EquipmentSlot.MAINHAND, SHYWORM_SKULL)) {
                                GlowController.setGlowSlots(entity, true,
                                    cavern.shywormGlowColor.getEffectiveColourRGB(), true, EquipmentSlot.MAINHAND);
                            }
                            if (cavern.driftlingGlow && isArmorStandSkull(entity, EquipmentSlot.HEAD, DRIFTLING_SKULL)) {
                                GlowController.setGlowSlots(entity, true,
                                    cavern.driftlingGlowColor.getEffectiveColourRGB(), true, EquipmentSlot.HEAD);
                            }
                        }
                        case FOREST -> {
                            var forest = safari.forest;
                            if (forest.hideonfloorGlow && isShulkerLike(entity)) {
                                setGlow(entity, forest.hideonfloorGlowColor.getEffectiveColourRGB());
                            }
                            if (forest.foxGlow && entity.getType() == EntityType.FOX) {
                                setGlow(entity, forest.foxGlowColor.getEffectiveColourRGB());
                            }
                            if (forest.pandaGlow && entity.getType() == EntityType.PANDA) {
                                setGlow(entity, forest.pandaGlowColor.getEffectiveColourRGB());
                            }
                            if (forest.creakingGlow && entity.getType() == EntityType.CREAKING) {
                                setGlow(entity, forest.creakingGlowColor.getEffectiveColourRGB());
                            }
                            if (forest.frogGlow && entity.getType() == EntityType.FROG) {
                                setGlow(entity, forest.frogGlowColor.getEffectiveColourRGB());
                            }
                            // 鹦鹉按颜色变体区分三个 critter：Bluebird(蓝)/Parakeet(绿)/Macaw(红蓝)
                            if (entity instanceof Parrot parrot) {
                                var variant = parrot.getVariant();
                                if (forest.bluebirdGlow && variant == Parrot.Variant.BLUE) {
                                    setGlow(parrot, forest.bluebirdGlowColor.getEffectiveColourRGB());
                                }
                                if (forest.parakeetGlow && variant == Parrot.Variant.GREEN) {
                                    setGlow(parrot, forest.parakeetGlowColor.getEffectiveColourRGB());
                                }
                                if (forest.macawGlow && variant == Parrot.Variant.RED_BLUE) {
                                    setGlow(parrot, forest.macawGlowColor.getEffectiveColourRGB());
                                }
                            }
                            if (forest.beeGlow && entity.getType() == EntityType.BEE) {
                                setGlow(entity, forest.beeGlowColor.getEffectiveColourRGB());
                            }
                        }
                    }
                }
            }

            // === 较频幽匿感测体方块发光 ===
            if (safari.haunted.sculkSensorGlow && isInArena(client.player.blockPosition())) {
                int sculkColor = safari.haunted.sculkSensorGlowColor.getEffectiveColourRGB();
                var level = client.level;
                var playerPos = client.player.blockPosition();
                int chunkX = playerPos.getX() >> 4;
                int chunkZ = playerPos.getZ() >> 4;
                int chunkR = (SCULK_SENSOR_RANGE >> 4) + 1;

                Set<BlockPos> found = new HashSet<>();

                for (int dcx = -chunkR; dcx <= chunkR; dcx++) {
                    for (int dcz = -chunkR; dcz <= chunkR; dcz++) {
                        int cx = chunkX + dcx;
                        int cz = chunkZ + dcz;
                        BlockPos chunkOrigin = new BlockPos(cx << 4, playerPos.getY(), cz << 4);

                        if (!level.isLoaded(chunkOrigin)) continue;

                        var chunk = level.getChunkAt(chunkOrigin);
                        for (var entry : chunk.getBlockEntities().entrySet()) {
                            if (entry.getValue() instanceof CalibratedSculkSensorBlockEntity) {
                                BlockPos pos = entry.getKey();
                                if (pos.distSqr(playerPos) <= SCULK_SENSOR_RANGE_SQ) {
                                    found.add(pos.immutable());
                                }
                            }
                        }
                    }
                }

                // Diff：移除
                var iter = sculkSensorHighlighted.iterator();
                while (iter.hasNext()) {
                    var pos = iter.next();
                    if (!found.contains(pos)) {
                        GlowController.setBlockGlow(level, pos, false);
                        iter.remove();
                    }
                }

                // Diff：新增
                for (var pos : found) {
                    if (!sculkSensorHighlighted.contains(pos)) {
                        GlowController.setBlockGlow(level, pos, true, sculkColor, true);
                        sculkSensorHighlighted.add(pos);
                    }
                }
            } else if (!sculkSensorHighlighted.isEmpty()) {
                // 玩家离开战斗场地 / 功能关闭：清除所有发光
                var level = client.level;
                for (var pos : sculkSensorHighlighted) {
                    GlowController.setBlockGlow(level, pos, false);
                }
                sculkSensorHighlighted.clear();
            }
        });
    }

    private static boolean isInArena(BlockPos pos) {
        return pos.getX() >= ARENA_X_MIN && pos.getX() <= ARENA_X_MAX
            && pos.getY() >= ARENA_Y_MIN && pos.getY() <= ARENA_Y_MAX
            && pos.getZ() >= ARENA_Z_MIN && pos.getZ() <= ARENA_Z_MAX;
    }

    /** 深度测试发光 */
    private static void setGlow(Entity entity, int color) {
        GlowController.setGlow(entity, true, color, true);
    }

    /** Hideonwall / Hideonfloor：潜影贝实体或展示潜影贝箱的物品展示实体 */
    private static boolean isShulkerLike(Entity entity) {
        if (entity instanceof Shulker) return true;
        if (entity instanceof Display.ItemDisplay itemDisplay) {
            return itemDisplay.getItemStack().is(Items.GREEN_SHULKER_BOX) || itemDisplay.getItemStack().is(Items.PURPLE_SHULKER_BOX);
        }
        return false;
    }

    /** Duplico：物品展示实体且展示物品是书架/樱花木/深板岩等之一 */
    private static boolean isDuplico(Entity entity) {
        if (entity instanceof Display.ItemDisplay itemDisplay) {
            for (Item item : DUPLICO_ITEMS) {
                if (itemDisplay.getItemStack().is(item)) return true;
            }
        }
        return false;
    }

    /** 物品展示实体展示的头颅材质是否匹配（物品展示目标直接整体发光） */
    private static boolean isSkullItemDisplay(Entity entity, String urlSegment) {
        if (!(entity instanceof Display.ItemDisplay itemDisplay)) return false;
        return isSkullWithTexture(itemDisplay.getItemStack(), urlSegment);
    }

    /** 盔甲架指定槽位（头戴/手持）的头颅材质是否匹配（只给该槽位选择性发光） */
    private static boolean isArmorStandSkull(Entity entity, EquipmentSlot slot, String urlSegment) {
        if (!(entity instanceof ArmorStand stand)) return false;
        return isSkullWithTexture(stand.getItemBySlot(slot), urlSegment);
    }

    /** 玩家头颅物品的 skull texture（base64）里是否包含目标材质 URL 片段 */
    private static boolean isSkullWithTexture(ItemStack stack, String urlSegment) {
        String texture = ItemUtils.getSkullTexture(stack);
        if (texture == null) return false;
        try {
            JsonObject obj = JsonParser.parseString(
                new String(Base64.getDecoder().decode(texture), StandardCharsets.UTF_8)).getAsJsonObject();
            String url = obj.getAsJsonObject("textures").getAsJsonObject("SKIN").get("url").getAsString();
            return url.contains(urlSegment);
        } catch (Exception e) {
            return false;
        }
    }

    private static boolean anyEnabled(HuntingConfig.Icy c) {
        return c.tropicalFishGlow || c.dolphinGlow || c.glowSquidGlow || c.polarBearGlow
            || c.snowGolemGlow || c.goatGlow || c.ravagerGlow
            || c.mantisShrimpGlow || c.troodonGlow;
    }

    private static boolean anyEnabled(HuntingConfig.Haunted c) {
        return c.hideonwallGlow || c.hideyhoGlow || c.batGlow || c.duplicoGlow || c.wardenGlow
            || c.endermiteGlow || c.caveSpiderGlow || c.phantomGlow
            || c.gimmiegoldGlow || c.gazerGlow;
    }

    private static boolean anyEnabled(HuntingConfig.Cavern c) {
        return c.tropicalFishGlow || c.armadilloGlow || c.snifferGlow
            || c.silverfishGlow || c.vexGlow
            || c.flitterGlow || c.chuckwallaGlow
            || c.shywormGlow || c.driftlingGlow;
    }

    private static boolean anyEnabled(HuntingConfig.Forest c) {
        return c.hideonfloorGlow || c.foxGlow || c.pandaGlow || c.creakingGlow
            || c.frogGlow || c.bluebirdGlow || c.parakeetGlow || c.macawGlow || c.beeGlow;
    }
}
