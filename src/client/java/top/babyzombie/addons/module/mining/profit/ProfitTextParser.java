package top.babyzombie.addons.module.mining.profit;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ProfitTextParser {
    private static final Pattern SACK_PATTERN = Pattern.compile("\\[Sacks\\] \\+[\\d,]+ items(?: \\(Last (\\d+)s\\.?\\))?");
    private static final Pattern HOVER_ITEM_PATTERN = Pattern.compile("\\+([\\d,]+) (.+?) \\(");
    private static final Pattern PRISTINE_PATTERN = Pattern.compile(
            "PRISTINE! You found .*?Flawed (.+?) Gemstone x(\\d+)!");

    private ProfitTextParser() {}

    public static Integer parseSackWindowSeconds(String messageText) {
        Matcher matcher = SACK_PATTERN.matcher(messageText);
        if (!matcher.find()) return null;
        String seconds = matcher.group(1);
        if (seconds == null || seconds.isBlank()) return 30;
        try {
            int value = Integer.parseInt(seconds);
            return value > 0 ? value : 30;
        } catch (NumberFormatException ignored) {
            return 30;
        }
    }

    public static Map<String, Long> parseSackHover(String hoverText) {
        Map<String, Long> materialRawAmounts = new LinkedHashMap<>();
        Map<String, Long> materialEnchantedRawEquiv = new LinkedHashMap<>();

        for (String line : hoverText.split("\n")) {
            Matcher itemMatcher = HOVER_ITEM_PATTERN.matcher(line);
            if (!itemMatcher.find()) continue;

            long amount;
            try {
                amount = Long.parseLong(itemMatcher.group(1).replace(",", ""));
            } catch (NumberFormatException ignored) {
                continue;
            }

            String itemName = itemMatcher.group(2).trim();
            boolean isEnchanted = itemName.startsWith("Enchanted ");
            String baseName = isEnchanted ? itemName.substring("Enchanted ".length()) : itemName;

            String material = ProfitMaterials.findMaterialByItemName(baseName);
            if (material == null) continue;

            int ratio = ProfitMaterials.getEnchantedRatio(material);
            long rawEquivalent = isEnchanted ? amount * ratio : amount;

            if (isEnchanted) {
                materialEnchantedRawEquiv.merge(material, rawEquivalent, Long::sum);
            } else {
                materialRawAmounts.merge(material, rawEquivalent, Long::sum);
            }
        }

        Map<String, Long> result = new LinkedHashMap<>();
        List<String> order = new ArrayList<>();
        order.addAll(materialRawAmounts.keySet());
        for (String material : materialEnchantedRawEquiv.keySet()) {
            if (!order.contains(material)) {
                order.add(material);
            }
        }

        for (String material : order) {
            if (materialEnchantedRawEquiv.containsKey(material)) {
                result.put(material, materialEnchantedRawEquiv.get(material));
            } else if (materialRawAmounts.containsKey(material)) {
                result.put(material, materialRawAmounts.get(material));
            }
        }
        return result;
    }

    public static ParsedPristine parsePristineMessage(String messageText) {
        Matcher matcher = PRISTINE_PATTERN.matcher(messageText);
        if (!matcher.find()) return null;
        try {
            return new ParsedPristine(matcher.group(1).trim(), Integer.parseInt(matcher.group(2)));
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    public record ParsedPristine(String gemType, int amount) {}
}
