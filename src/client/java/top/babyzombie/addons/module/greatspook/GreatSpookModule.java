package top.babyzombie.addons.module.greatspook;

import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.client.Minecraft;
import top.babyzombie.addons.config.ModConfigManager;
import top.babyzombie.addons.util.ChatUtils;
import top.babyzombie.addons.util.HypixelLocationTracker;

/**
 * Halloween Great Spook event automation:
 * Auto math (QUICK MATHS solver), auto speech response,
 * auto sign click ("HERE" button).
 */
public final class GreatSpookModule {

    private GreatSpookModule() {}

    public static void init() {
        // QUICK MATHS solver
        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            if (overlay) return;
            if (!HypixelLocationTracker.getInstance().isInSkyblock()) return;
            String text = ChatUtils.stripColor(message.getString());

            if (text.contains("QUICK MATHS") || text.contains("Solve:")) {
                String answer = solveMath(text);
                if (answer != null) {
                    ChatUtils.sendCommand("ac " + answer);
                }
            }
        });

        // Public Speaking Demon
        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            if (overlay) return;
            String text = ChatUtils.stripColor(message.getString());
            if (text.contains("Public Speaking") && text.contains("say")) {
                // Auto respond with configured or random text
                ChatUtils.sendCommand("ac I love SkyBlock!");
            }
        });

        // Auto sign click ("HERE" button in messages)
        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            if (overlay) return;
            String text = message.getString();
            if (text.contains("HERE") && text.contains("Great Spook")) {
                // Auto-click the HERE button - would need click event detection
                // Similar to IncomingCallHandler's PICK UP pattern
            }
        });
    }

    /**
     * Attempt to parse and solve a QUICK MATHS problem from chat text.
     */
    private static String solveMath(String text) {
        try {
            // Parse expression like "Solve: 15 + 27 * 3"
            String expr = text.replaceAll(".*?(\\d+\\s*[+\\-*/]\\s*\\d+(\\s*[+\\-*/]\\s*\\d+)?).*", "$1");
            if (expr.equals(text)) return null; // No match

            // Simple evaluator (no precedence, left-to-right for Hypixel's QUICK MATHS)
            String[] tokens = expr.trim().split("\\s+");
            if (tokens.length < 3) return null;

            double result = Double.parseDouble(tokens[0]);
            for (int i = 1; i < tokens.length - 1; i += 2) {
                String op = tokens[i];
                double num = Double.parseDouble(tokens[i + 1]);
                switch (op) {
                    case "+": result += num; break;
                    case "-": result -= num; break;
                    case "*": result *= num; break;
                    case "/": result /= num; break;
                    default: return null;
                }
            }
            // Round to integer
            return String.valueOf(Math.round(result));
        } catch (Exception e) {
            return null;
        }
    }
}
