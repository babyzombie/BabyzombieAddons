package top.babyzombie.addons.module.events;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import top.babyzombie.addons.config.ModConfigManager;
import top.babyzombie.addons.util.ChatUtils;
import top.babyzombie.addons.util.tracker.HypixelLocationTracker;
import top.babyzombie.addons.util.ServerTick;

public final class GreatSpookModule {

    private static final ScheduledExecutorService SCHEDULER = Executors.newSingleThreadScheduledExecutor();
    private static final String RANDOM_CHARS = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ~!@#$%^&*()_+-{}|:<>?";
    private static long publicSpeakingDemonCooldown = 0;

    private GreatSpookModule() {}

    public static void init() {
        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            if (overlay) return;
            if (!HypixelLocationTracker.getInstance().isInSkyblock()) return;
            if (!ModConfigManager.get().events.greatSpook) return;

            String rawText = message.getString();
            String text = ChatUtils.stripColor(rawText);

            // "Click HERE to sign" - auto click
            if (text.startsWith("Click HERE to sign ")) {
                String cmd = getCmdFromChat(message, "HERE");
                if (cmd == null || !cmd.startsWith("/spookysignpaper")) return;
                scheduleCommand(cmd);
                return;
            }

            // QUICK MATHS solver
            if (text.startsWith("QUICK MATHS! Solve: ")) {
                String expr = text.replace("QUICK MATHS! Solve: ", "")
                        .replaceAll("(?i)x", "*")
                        .replaceAll("(?i)[a-zA-Z]", "");
                double num = evalExpr(expr);
                if (Double.isNaN(num)) return;
                scheduleCommand("ac " + Math.round(num));
                return;
            }

            // Public Speaking Demon
            if (text.startsWith("[FEAR] Public Speaking Demon: Speak ")
                    || text.startsWith("[FEAR] Public Speaking Demon: Say something interesting ")) {
                if (ServerTick.getTime() < publicSpeakingDemonCooldown + 20_000) return;

                String cfgText = ModConfigManager.get().events.publicSpeakingDemon;
                String sayStr;
                if (ServerTick.getTime() > publicSpeakingDemonCooldown + 60_000
                        && cfgText != null && !cfgText.isEmpty()) {
                    sayStr = cfgText;
                } else {
                    sayStr = randomString((int) (Math.random() * 16) + 16);
                }
                publicSpeakingDemonCooldown = ServerTick.getTime();
                scheduleCommand("ac " + sayStr);
            }
        });
    }

    private static void scheduleCommand(String cmd) {
        boolean delay = ModConfigManager.get().events.greatSpookDelay;
        int ms = delay ? (int) (500 + Math.random() * 500) : 0;
        if (ms == 0) {
            ChatUtils.sendCommand(cmd);
        } else {
            SCHEDULER.schedule(() ->
                    Minecraft.getInstance().execute(() -> ChatUtils.sendCommand(cmd)),
                    ms, TimeUnit.MILLISECONDS);
        }
    }

    private static String getCmdFromChat(Component message, String target) {
        ClickEvent click = findClickEventInComponent(message, target);
        if (click instanceof ClickEvent.RunCommand runCommand) {
            return runCommand.command();
        }
        return null;
    }

    private static ClickEvent findClickEventInComponent(Component component, String target) {
        if (component.getString().contains(target)) {
            ClickEvent ce = component.getStyle().getClickEvent();
            if (ce != null) return ce;
        }
        for (Component child : component.getSiblings()) {
            ClickEvent ce = findClickEventInComponent(child, target);
            if (ce != null) return ce;
        }
        return null;
    }

    /**
     * Evaluates an arithmetic expression with parentheses support.
     * Resolves parenthesized sub-expressions first, then left-to-right
     * matching Hypixel's QUICK MATHS behavior.
     */
    private static double evalExpr(String expr) {
        // Normalize: add spaces around operators and parens for proper tokenization
        expr = expr.replace("(", " ( ")
                .replace(")", " ) ")
                .replace("+", " + ")
                .replace("*", " * ")
                .replace("/", " / ");
        // Handle minus: avoid breaking negative numbers by only spacing standalone operators
        expr = expr.replace(" - ", " - "); // already spaced above
        // Re-process: replace any unspaced minus with spaced version, then collapse spaces
        expr = expr.replace("-", " - ")
                .replaceAll("\\s+", " ")
                .trim();
        if (expr.isEmpty()) return Double.NaN;

        // Resolve parentheses recursively
        while (expr.contains("(")) {
            int start = expr.lastIndexOf('(');
            int end = expr.indexOf(')', start);
            if (end == -1) return Double.NaN;
            String inner = expr.substring(start + 1, end).trim();
            String before = expr.substring(0, start).trim();
            String after = expr.substring(end + 1).trim();
            double innerVal = evalTokens(inner.split("\\s+"));
            if (Double.isNaN(innerVal)) return Double.NaN;
            expr = (before.isEmpty() ? "" : before + " ") + innerVal + (after.isEmpty() ? "" : " " + after);
            expr = expr.trim();
        }

        return evalTokens(expr.split("\\s+"));
    }

    private static double evalTokens(String[] tokens) {
        if (tokens.length == 0) return Double.NaN;

        double result;
        try {
            result = Double.parseDouble(tokens[0]);
        } catch (NumberFormatException e) {
            return Double.NaN;
        }

        for (int i = 1; i < tokens.length - 1; i += 2) {
            String op = tokens[i];
            double num;
            try {
                num = Double.parseDouble(tokens[i + 1]);
            } catch (NumberFormatException e) {
                return Double.NaN;
            }
            switch (op) {
                case "+": result += num; break;
                case "-": result -= num; break;
                case "*": result *= num; break;
                case "/": if (num != 0) result /= num; else return Double.NaN; break;
                default: return Double.NaN;
            }
        }
        return result;
    }

    private static String randomString(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(RANDOM_CHARS.charAt((int) (Math.random() * RANDOM_CHARS.length())));
        }
        return sb.toString();
    }
}
