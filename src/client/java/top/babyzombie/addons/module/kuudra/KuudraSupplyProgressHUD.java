package top.babyzombie.addons.module.kuudra;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLevelEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import top.babyzombie.addons.config.ModConfigManager;
import top.babyzombie.addons.config.hud.HudManager;
import top.babyzombie.addons.util.ChatUtils;
import top.babyzombie.addons.util.tracker.HypixelLocationTracker;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 补给/燃料拾取进度 HUD — 拦截 title 进度条（[|||...] 69%），
 * 替换为 HUD 显示。同时跟踪 P1 补给计数 (0/6) 和 P3 燃料计数 (0/4)。
 */
public final class KuudraSupplyProgressHUD {
    private KuudraSupplyProgressHUD() {}

    // Title progress bar pattern: [|||||||||||||       ] 69%
    private static final Pattern TITLE_PROGRESS = Pattern.compile("^\\[[| ]+]\\s*(\\d+)%$");
    private static final Pattern SUPPLY_PLACE_PATTERN = Pattern.compile(".+? recovered.*?\\((\\d)/6\\)");
    private static final Pattern FUEL_CELL_PATTERN = Pattern.compile("recovered a Fuel Cell and charged the Ballista \\((\\d+)%\\)");

    private static int supplyCount;     // 0-6
    private static int fuelCount;       // 0-4
    private static int currentProgress; // 0-100 from title
    private static boolean inSuppliesPhase;
    private static boolean cancelledThisTitle;

    public static int getCurrentProgress() { return currentProgress; }
    public static int getSupplyCount() { return supplyCount; }
    public static boolean isInSuppliesPhase() { return inSuppliesPhase; }
    public static long getSuppliesStartMs() { return KuudraSupplyTimer.getStartMs(); }

    // Called from GuiTitleMixin. Returns true to cancel the vanilla title.
    public static boolean onTitle(String text) {
        var cfg = ModConfigManager.get().kuudra;
        if (!HypixelLocationTracker.getInstance().isInKuudra()) return false;

        Matcher m = TITLE_PROGRESS.matcher(ChatUtils.stripColor(text));
        if (m.matches()) {
            int pct = Integer.parseInt(m.group(1));
            currentProgress = pct;

            // Cancel vanilla title if either HUD is active
            if (inSuppliesPhase && cfg.phase1.supplyProgressHud) {
                cancelledThisTitle = true;
                return true;
            }
            if (!inSuppliesPhase && cfg.phase3.fuelProgressHud && fuelCount > 0) {
                cancelledThisTitle = true;
                return true;
            }
        }
        return false;
    }

    public static void reset() {
        supplyCount = 0;
        fuelCount = 0;
        currentProgress = 0;
        inSuppliesPhase = true;
        cancelledThisTitle = false;
    }

    public static void init() {
        ClientLevelEvents.AFTER_CLIENT_LEVEL_CHANGE.register((client, world) -> reset());

        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            if (overlay || !HypixelLocationTracker.getInstance().isInKuudra()) return;
            String text = ChatUtils.stripColor(message.getString());

            if (text.contains("Okay adventurers, I will go and fish up Kuudra")) {
                reset();
                return;
            }
            if (text.contains("OMG! Great work collecting my supplies")) {
                inSuppliesPhase = false;
                return;
            }
            if (text.contains("Phew! The Ballista is finally ready")) {
                fuelCount = 0;
                currentProgress = 0;
                return;
            }

            Matcher sm = SUPPLY_PLACE_PATTERN.matcher(text);
            if (sm.find() && inSuppliesPhase) {
                supplyCount = Integer.parseInt(sm.group(1));
                return;
            }

            Matcher fm = FUEL_CELL_PATTERN.matcher(text);
            if (fm.find() && !inSuppliesPhase) {
                int pct = Integer.parseInt(fm.group(1));
                fuelCount = pct / 25;
            }
        });

        HudElementRegistry.attachElementAfter(VanillaHudElements.OVERLAY_MESSAGE,
                Identifier.fromNamespaceAndPath("babyzombieaddons", "kuudra_supply_progress"),
                (context, tickCounter) -> {
                    var cfg = ModConfigManager.get().kuudra;
                    boolean showSupply = cfg.phase1.supplyProgressHud && inSuppliesPhase;
                    boolean showFuel = cfg.phase3.fuelProgressHud && !inSuppliesPhase && fuelCount > 0;
                    if (!showSupply && !showFuel) return;
                    if (currentProgress <= 0) return;

                    var font = Minecraft.getInstance().font;
                    int x = HudManager.x("SupplyProgress"), y = HudManager.y("SupplyProgress");
                    float s = HudManager.scale("SupplyProgress");

                    String text = progressBar(currentProgress, showSupply ? supplyCount : fuelCount, showSupply ? 6 : 4);

                    HudManager.drawScaled(context, font, text, x, y, s);
                });
    }

    private static String progressBar(int pct, int count, int max) {
        int barLen = 14;
        int filled = (pct * barLen) / 100;
        StringBuilder bar = new StringBuilder("§8[");
        if (filled > 0) bar.append("§a");
        for (int i = 0; i < barLen; i++) {
            if (i == filled) bar.append("§8");
            bar.append(i < filled ? '|' : ' ');
        }
        bar.append("§8]");
        return String.format("§3%s §b%d/%d §8(%d%%)", bar, count, max, pct);
    }
}
