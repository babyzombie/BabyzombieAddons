package top.babyzombie.addons.module.mining.profit;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public final class ProfitMaterials {
    private static final Map<String, MaterialDefinition> MATERIALS = new LinkedHashMap<>();
    private static final Map<String, String> NAME_TO_MATERIAL = new LinkedHashMap<>();

    static {
        register(new MaterialDefinition("COAL", "ENCHANTED_COAL", "Coal", 160,
                "Coal"));
        register(new MaterialDefinition("DIAMOND", "ENCHANTED_DIAMOND", "Diamond", 160,
                "Diamond"));
        register(new MaterialDefinition("GOLD", "ENCHANTED_GOLD", "Gold", 160,
                "Gold Ingot", "Gold"));
        register(new MaterialDefinition("IRON", "ENCHANTED_IRON", "Iron", 160,
                "Iron Ingot", "Iron"));
        register(new MaterialDefinition("REDSTONE", "ENCHANTED_REDSTONE", "Redstone", 160,
                "Redstone"));
        register(new MaterialDefinition("LAPIS", "ENCHANTED_LAPIS_LAZULI", "Lapis Lazuli", 160,
                "Lapis Lazuli", "Lapis"));
        register(new MaterialDefinition("MYCELIUM", "ENCHANTED_MYCELIUM", "Mycelium", 160,
                "Mycelium"));
        register(new MaterialDefinition("RED_SAND", "ENCHANTED_RED_SAND", "Red Sand", 160,
                "Red Sand"));
        register(new MaterialDefinition("OBSIDIAN", "ENCHANTED_OBSIDIAN", "Obsidian", 160,
                "Obsidian"));
        register(new MaterialDefinition("QUARTZ", "ENCHANTED_QUARTZ", "Quartz", 160,
                "Nether Quartz", "Quartz"));
        register(new MaterialDefinition("EMERALD", "ENCHANTED_EMERALD", "Emerald", 160,
                "Emerald"));
        register(new MaterialDefinition("GLOWSTONE", "ENCHANTED_GLOWSTONE", "Glowstone", 160,
                "Glowstone Dust", "Glowstone"));
        register(new MaterialDefinition("HARDSTONE", "ENCHANTED_HARD_STONE", "Hard Stone", 160,
                "Hard Stone", "Hardstone"));
        register(new MaterialDefinition("MITHRIL", "ENCHANTED_MITHRIL", "Mithril", 160,
                "Mithril"));
        register(new MaterialDefinition("TITANIUM", "ENCHANTED_TITANIUM", "Titanium", 160,
                "Titanium"));
        register(new MaterialDefinition("SULPHUR", "ENCHANTED_SULPHUR", "Sulphur", 160,
                "Sulphur"));
        register(new MaterialDefinition("UMBER", "ENCHANTED_UMBER", "Umber", 160,
                "Umber"));
    }

    private ProfitMaterials() {}

    private static void register(MaterialDefinition definition) {
        MATERIALS.put(definition.material(), definition);
        for (String alias : definition.aliases()) {
            NAME_TO_MATERIAL.put(normalizeName(alias), definition.material());
        }
    }

    public static MaterialDefinition get(String material) {
        return MATERIALS.get(material);
    }

    public static String findMaterialByItemName(String itemName) {
        if (itemName == null) return null;
        return NAME_TO_MATERIAL.get(normalizeName(itemName));
    }

    public static int getEnchantedRatio(String material) {
        MaterialDefinition definition = MATERIALS.get(material);
        return definition != null ? definition.ratio() : 160;
    }

    public static String getDisplayName(String material) {
        MaterialDefinition definition = MATERIALS.get(material);
        return definition != null ? definition.displayName() : material;
    }

    public static String getEnchantedItemId(String material) {
        MaterialDefinition definition = MATERIALS.get(material);
        return definition != null ? definition.enchantedItemId() : material;
    }

    private static String normalizeName(String name) {
        return name.trim().toLowerCase(Locale.ROOT);
    }

    public record MaterialDefinition(
            String material,
            String enchantedItemId,
            String displayName,
            int ratio,
            String... aliases
    ) {}
}
