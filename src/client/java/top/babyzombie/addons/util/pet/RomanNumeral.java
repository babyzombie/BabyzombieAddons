package top.babyzombie.addons.util.pet;

import java.util.Locale;

/**
 * Simple Roman numeral parser supporting I, V, X, L, C.
 * Used for Taming level (max LX=60) and Battle Experience level (max X=10).
 */
public final class RomanNumeral {
    private RomanNumeral() {}

    /**
     * Parse a Roman numeral string to int. Returns -1 on failure.
     */
    public static int parse(String roman) {
        if (roman == null || roman.isEmpty()) return -1;
        String upper = roman.trim().toUpperCase(Locale.ROOT);
        int result = 0;
        int prevValue = 0;

        for (int i = upper.length() - 1; i >= 0; i--) {
            int value = charValue(upper.charAt(i));
            if (value < 0) return -1;
            if (value < prevValue) {
                result -= value;
            } else {
                result += value;
            }
            prevValue = value;
        }
        return result;
    }

    private static int charValue(char c) {
        return switch (c) {
            case 'I' -> 1;
            case 'V' -> 5;
            case 'X' -> 10;
            case 'L' -> 50;
            case 'C' -> 100;
            default -> -1;
        };
    }
}
