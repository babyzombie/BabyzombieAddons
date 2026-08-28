package top.babyzombie.addons.module.misc.loadout;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
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

        // 不等物品加载，直接替换屏幕（避免闪烁）
        ScreenEvents.BEFORE_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            if (!(screen instanceof AbstractContainerScreen<?> cs)) return;
            if (!ModConfigManager.get().skyblock.loadout.enabled) return;
            if (!HypixelLocationTracker.getInstance().isInSkyblock()) return;
            if (closingGuard > 0) return;
            // autoClose 开启 + pending 时不替换页面（PetManager 会关掉它）；
            // 快速关闭（地牢/Kuudra 开关）同理，新页面注册时直接关，不等加载
            if (top.babyzombie.addons.util.pet.PetManager.getInstance().isRecentLoadoutSwitch()
                && (ModConfigManager.get().skyblock.loadout.autoClose || fastCloseEnabled())) return;
            if (!LOADOUT_TITLE.matcher(ChatUtils.stripColor(cs.getTitle().getString())).matches()) return;
            if (!(cs.getMenu() instanceof ChestMenu)) return;
            if (client.gui.screen() instanceof LoadoutDisplayScreen) return;

            cachedContainer = cs;
            guiActive = true;
            client.execute(() -> client.gui.setScreen(new LoadoutDisplayScreen(cs)));
        });

        // 快速切装：切换后新页面在注册（AFTER_INIT）时立即关闭，不等加载完成，
        // 避免服务器重开的 Loadout 页面在 Kuudra/地牢里闪出来
        ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            if (closingGuard > 0) return;
            if (!(screen instanceof AbstractContainerScreen<?> cs)) return;
            if (!LOADOUT_TITLE.matcher(ChatUtils.stripColor(cs.getTitle().getString())).matches()) return;
            if (!fastCloseEnabled()) return;
            var pm = top.babyzombie.addons.util.pet.PetManager.getInstance();
            if (!pm.isRecentLoadoutSwitch()) return;
            // 页面内容还没加载完，直接关掉，跳过宠物/装备扫描
            pm.setLoadoutSwitchPending(false);
            client.execute(() -> {
                if (client.player != null) client.player.closeContainer();
            });
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

    /**
     * 快速切装关闭是否启用：在 Kuudra/地牢且对应开关开启时，切换装备后
     * 不等新页面加载完成，立刻关闭当前页面，新页面也在注册时直接关闭。
     */
    public static boolean fastCloseEnabled() {
        var loadout = ModConfigManager.get().skyblock.loadout;
        var tracker = HypixelLocationTracker.getInstance();
        return (loadout.kuudraFastClose && tracker.isInKuudra())
            || (loadout.dungeonFastClose && tracker.isInDungeon());
    }
}
