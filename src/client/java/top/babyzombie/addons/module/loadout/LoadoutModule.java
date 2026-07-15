package top.babyzombie.addons.module.loadout;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.ChestMenu;
import top.babyzombie.addons.config.ModConfigManager;
import top.babyzombie.addons.util.ChatUtils;
import top.babyzombie.addons.util.tracker.HypixelLocationTracker;

import java.util.regex.Pattern;

public final class LoadoutModule {

    private static final Pattern LOADOUT_TITLE = Pattern.compile("\\(\\d+/\\d+\\) Loadouts");

    private static boolean guiActive;
    static volatile int closingGuard;
    private static AbstractContainerScreen<?> cachedContainer;

    private LoadoutModule() {}

    public static void init() {
        // 每 tick 递减 closingGuard
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (closingGuard > 0) closingGuard--;
        });

        if (!FabricLoader.getInstance().isModLoaded("skyblocker")) {
            ModConfigManager.get().skyblock.loadout.enabled = false;
            return;
        }

        // 不等物品加载，直接替换屏幕（避免闪烁）
        ScreenEvents.BEFORE_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            if (!(screen instanceof AbstractContainerScreen<?> cs)) return;
            if (!ModConfigManager.get().skyblock.loadout.enabled) return;
            if (!HypixelLocationTracker.getInstance().isInSkyblock()) return;
            if (closingGuard > 0) return;
            // autoClose 开启 + pending 时不替换页面（PetManager 会关掉它）
            if (top.babyzombie.addons.util.pet.PetManager.getInstance().isLoadoutSwitchPending()
                && ModConfigManager.get().skyblock.loadout.autoClose) return;
            if (!LOADOUT_TITLE.matcher(ChatUtils.stripColor(cs.getTitle().getString())).matches()) return;
            if (!(cs.getMenu() instanceof ChestMenu)) return;
            if (client.screen instanceof LoadoutDisplayScreen) return;

            cachedContainer = cs;
            guiActive = true;
            client.execute(() -> client.setScreen(new LoadoutDisplayScreen(cs)));
        });

        // 非 Loadout 页面打开时重置状态
        ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            if (screen instanceof AbstractContainerScreen<?> cs
                && !LOADOUT_TITLE.matcher(ChatUtils.stripColor(cs.getTitle().getString())).matches()) {
                guiActive = false;
                cachedContainer = null;
            }
        });
    }

    public static AbstractContainerScreen<?> getCachedContainer() { return cachedContainer; }
    public static boolean isGuiActive() { return guiActive; }
    public static void onCustomScreenClosed() { guiActive = false; }
}
