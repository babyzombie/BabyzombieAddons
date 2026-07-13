package top.babyzombie.addons.module.mining.profit;

public final class ProfitFormatters {
    private ProfitFormatters() {}

    public static String formatTime(long millis) {
        if (millis <= 0) return "n/a";
        long seconds = millis / 1000;
        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;
        long secs = seconds % 60;

        if (hours > 0) {
            return String.format("%dh %dm %ds", hours, minutes, secs);
        }
        if (minutes > 0) {
            return String.format("%dm %ds", minutes, secs);
        }
        return String.format("%ds", secs);
    }

    public static String formatCoins(double coins) {
        if (coins >= 1_000_000_000) {
            return String.format("$%.2fB", coins / 1_000_000_000.0);
        }
        if (coins >= 1_000_000) {
            return String.format("$%.2fM", coins / 1_000_000.0);
        }
        if (coins >= 1_000) {
            return String.format("$%.2fK", coins / 1_000.0);
        }
        return String.format("$%.0f", coins);
    }

    public static String formatPlainNumber(double value) {
        return String.format("%,.0f", value);
    }
}
