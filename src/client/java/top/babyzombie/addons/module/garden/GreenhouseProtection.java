package top.babyzombie.addons.module.garden;

import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import top.babyzombie.addons.config.GardenConfig;
import top.babyzombie.addons.config.ModConfigManager;
import top.babyzombie.addons.util.tracker.HypixelLocationTracker;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.BooleanSupplier;

/**
 * 温室作物防破坏。
 *
 * <p>注册 {@link AttackBlockCallback} 和 {@link AttackEntityCallback}，
 * 在温室中根据配置阻止方块破坏和盔甲架作物攻击。
 */
public final class GreenhouseProtection {

    /// 方块 → 配置开关
    private static final Map<Block, BooleanSupplier> BLOCK_PROTECTION = new HashMap<>();
    /// 盔甲架头部展示名（纯小写，已去色码） → 配置开关
    private static final Map<String, BooleanSupplier> ARMOR_STAND_PROTECTION = new LinkedHashMap<>();

    static {
        // ═══ 原版作物 · 方块映射 ═══
        BLOCK_PROTECTION.put(Blocks.WHEAT,             () -> cfg().wheat);
        BLOCK_PROTECTION.put(Blocks.CARROTS,           () -> cfg().carrot);
        BLOCK_PROTECTION.put(Blocks.POTATOES,          () -> cfg().potato);
        BLOCK_PROTECTION.put(Blocks.BEETROOTS,         () -> cfg().beetroot);
        BLOCK_PROTECTION.put(Blocks.PUMPKIN_STEM,      () -> cfg().pumpkin);
        BLOCK_PROTECTION.put(Blocks.ATTACHED_PUMPKIN_STEM, () -> cfg().pumpkin);
        BLOCK_PROTECTION.put(Blocks.MELON_STEM,        () -> cfg().melon);
        BLOCK_PROTECTION.put(Blocks.ATTACHED_MELON_STEM,   () -> cfg().melon);
        BLOCK_PROTECTION.put(Blocks.CACTUS,            () -> cfg().cactus);
        BLOCK_PROTECTION.put(Blocks.SUGAR_CANE,        () -> cfg().sugarCane);
        BLOCK_PROTECTION.put(Blocks.ROSE_BUSH,         () -> cfg().roseBush);
        BLOCK_PROTECTION.put(Blocks.SUNFLOWER,         () -> cfg().sunflower);
        BLOCK_PROTECTION.put(Blocks.DEAD_BUSH,         () -> cfg().deadBush);
        BLOCK_PROTECTION.put(Blocks.NETHER_WART,       () -> cfg().netherWart);
        BLOCK_PROTECTION.put(Blocks.FIRE,              () -> cfg().fire);
        BLOCK_PROTECTION.put(Blocks.SOUL_FIRE,         () -> cfg().fire);
        BLOCK_PROTECTION.put(Blocks.RED_MUSHROOM,      () -> cfg().mushroom);
        BLOCK_PROTECTION.put(Blocks.BROWN_MUSHROOM,    () -> cfg().mushroom);
        BLOCK_PROTECTION.put(Blocks.RED_MUSHROOM_BLOCK, () -> cfg().mushroom);
        BLOCK_PROTECTION.put(Blocks.BROWN_MUSHROOM_BLOCK, () -> cfg().mushroom);
        BLOCK_PROTECTION.put(Blocks.MUSHROOM_STEM,     () -> cfg().mushroom);

        // ═══ 原版作物 · 盔甲架映射 ═══
        ARMOR_STAND_PROTECTION.put("pumpkin",   () -> cfg().pumpkin);
        ARMOR_STAND_PROTECTION.put("melon",     () -> cfg().melon);
        ARMOR_STAND_PROTECTION.put("cactus",    () -> cfg().cactus);
        ARMOR_STAND_PROTECTION.put("wildrose",  () -> cfg().roseBush);
        ARMOR_STAND_PROTECTION.put("sunflower", () -> cfg().sunflower);
        ARMOR_STAND_PROTECTION.put("coco",      () -> cfg().coco);

        // ═══ 杂交作物 · 盔甲架映射（驼峰字段名 → 实际盔甲架展示名，无空格连字符） ═══
        ARMOR_STAND_PROTECTION.put("allinaloe",          () -> cfg().allInAloe);
        ARMOR_STAND_PROTECTION.put("ashwreath",           () -> cfg().ashwreath);
        ARMOR_STAND_PROTECTION.put("blastberry",          () -> cfg().blastberry);
        ARMOR_STAND_PROTECTION.put("cheesebite",          () -> cfg().cheesebite);
        ARMOR_STAND_PROTECTION.put("chloronite",          () -> cfg().chloronite);
        ARMOR_STAND_PROTECTION.put("chocoberry",          () -> cfg().chocoberry);
        ARMOR_STAND_PROTECTION.put("choconut",            () -> cfg().choconut);
        ARMOR_STAND_PROTECTION.put("chorusfruit",         () -> cfg().chorusFruit);
        ARMOR_STAND_PROTECTION.put("cindershade",         () -> cfg().cindershade);
        ARMOR_STAND_PROTECTION.put("coalroot",            () -> cfg().coalroot);
        ARMOR_STAND_PROTECTION.put("creambloom",          () -> cfg().creambloom);
        ARMOR_STAND_PROTECTION.put("devourer",            () -> cfg().devourer);
        ARMOR_STAND_PROTECTION.put("donoteatshroom",      () -> cfg().doNotEatShroom);
        ARMOR_STAND_PROTECTION.put("duskbloom",           () -> cfg().duskbloom);
        ARMOR_STAND_PROTECTION.put("dustgrain",           () -> cfg().dustgrain);
        ARMOR_STAND_PROTECTION.put("fleshtrap",           () -> cfg().fleshtrap);
        ARMOR_STAND_PROTECTION.put("glasscorn",           () -> cfg().glasscorn);
        ARMOR_STAND_PROTECTION.put("gloomgourd",          () -> cfg().gloomgourd);
        ARMOR_STAND_PROTECTION.put("godseed",             () -> cfg().godseed);
        ARMOR_STAND_PROTECTION.put("lonelily",            () -> cfg().lonelily);
        ARMOR_STAND_PROTECTION.put("magicjellybean",      () -> cfg().magicJellybean);
        ARMOR_STAND_PROTECTION.put("noctilume",           () -> cfg().noctilume);
        ARMOR_STAND_PROTECTION.put("phantomleaf",         () -> cfg().phantomleaf);
        ARMOR_STAND_PROTECTION.put("plantboyadvance",     () -> cfg().plantboyAdvance);
        ARMOR_STAND_PROTECTION.put("puffercloud",         () -> cfg().puffercloud);
        ARMOR_STAND_PROTECTION.put("scourroot",           () -> cfg().scourroot);
        ARMOR_STAND_PROTECTION.put("shadevine",           () -> cfg().shadevine);
        ARMOR_STAND_PROTECTION.put("shellfruit",          () -> cfg().shellfruit);
        ARMOR_STAND_PROTECTION.put("snoozling",           () -> cfg().snoozling);
        ARMOR_STAND_PROTECTION.put("soggybud",            () -> cfg().soggybud);
        ARMOR_STAND_PROTECTION.put("startlevine",         () -> cfg().startlevine);
        ARMOR_STAND_PROTECTION.put("stoplightpetal",      () -> cfg().stoplightPetal);
        ARMOR_STAND_PROTECTION.put("thornshade",          () -> cfg().thornshade);
        ARMOR_STAND_PROTECTION.put("thunderling",         () -> cfg().thunderling);
        ARMOR_STAND_PROTECTION.put("timestalk",           () -> cfg().timestalk);
        ARMOR_STAND_PROTECTION.put("turtlellini",         () -> cfg().turtlellini);
        ARMOR_STAND_PROTECTION.put("veilshroom",          () -> cfg().veilshroom);
        ARMOR_STAND_PROTECTION.put("witherbloom",         () -> cfg().witherbloom);
        ARMOR_STAND_PROTECTION.put("zombud",              () -> cfg().zombud);
    }

    private GreenhouseProtection() {}

    public static void init() {
        AttackBlockCallback.EVENT.register((player, world, hand, pos, direction) -> {
            if (!cfg().enabled) return InteractionResult.PASS;
            if (!HypixelLocationTracker.getInstance().isIn("Garden")) return InteractionResult.PASS;
            if (!GreenhouseDetector.isCurrentPlotGreenhouse()) return InteractionResult.PASS;

            Block block = world.getBlockState(pos).getBlock();
            BooleanSupplier enabled = BLOCK_PROTECTION.get(block);
            if (enabled != null && enabled.getAsBoolean()) {
                return InteractionResult.FAIL;
            }
            return InteractionResult.PASS;
        });

        AttackEntityCallback.EVENT.register((player, level, hand, entity, hitResult) -> {
            if (!cfg().enabled) return InteractionResult.PASS;
            if (!HypixelLocationTracker.getInstance().isIn("Garden")) return InteractionResult.PASS;
            if (!GreenhouseDetector.isCurrentPlotGreenhouse()) return InteractionResult.PASS;

            if (!(entity instanceof ArmorStand armorStand)) return InteractionResult.PASS;

            var headItem = armorStand.getItemBySlot(EquipmentSlot.HEAD);
            if (headItem.isEmpty()) return InteractionResult.PASS;

            String rawName = headItem.getHoverName().getString();
            // 去色码 → 纯小写
            String clean = net.minecraft.ChatFormatting.stripFormatting(rawName).toLowerCase().trim();
            if (clean.isEmpty()) return InteractionResult.PASS;

            // 去掉末尾 1-3 位数字
            String baseName = clean.replaceAll("\\d{1,3}$", "");

            BooleanSupplier enabled = ARMOR_STAND_PROTECTION.get(baseName);
            if (enabled != null && enabled.getAsBoolean()) {
                return InteractionResult.FAIL;
            }
            return InteractionResult.PASS;
        });
    }

    private static GardenConfig.GreenhouseProtection cfg() {
        return ModConfigManager.get().garden.greenhouseProtection;
    }
}
