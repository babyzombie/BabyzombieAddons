package top.babyzombie.addons;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;
import org.lwjgl.glfw.GLFW;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.fabricmc.fabric.api.resource.v1.pack.PackActivationType;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import top.babyzombie.addons.command.BabyzombieAddonsCommand;
import top.babyzombie.addons.config.hud.HudManager;
import top.babyzombie.addons.config.hud.HudRegistrar;
import top.babyzombie.addons.config.ModConfigManager;
import top.babyzombie.addons.module.misc.abiphone.AbiphoneTracker;
import top.babyzombie.addons.module.misc.abiphone.CustomRingtoneModule;
import top.babyzombie.addons.module.misc.abiphone.IncomingCallHandler;
import top.babyzombie.addons.module.misc.AutoJoinModule;
import top.babyzombie.addons.module.misc.autois.AutoISModule;
import top.babyzombie.addons.module.misc.autois.KickRecoveryModule;
import top.babyzombie.addons.module.misc.AutoReconnectHelper;
import top.babyzombie.addons.module.misc.BazaarSellFromSacks;
import top.babyzombie.addons.module.dungeon.CustomDiscScanner;
import top.babyzombie.addons.module.dungeon.DungeonJukeboxModule;
import top.babyzombie.addons.module.dungeon.DungeonModule;
import top.babyzombie.addons.module.fishing.PreventInstantReel;
import top.babyzombie.addons.module.fishing.RareSeaCreaturesAlert;
import top.babyzombie.addons.module.fishing.RareSeaCreaturesSelfAlert;
import top.babyzombie.addons.module.garden.GardenModule;
import top.babyzombie.addons.module.events.FruitDiggingModule;
import top.babyzombie.addons.module.events.GreatSpookModule;
import top.babyzombie.addons.module.events.RaffleTaskModule;
import top.babyzombie.addons.module.kuudra.KuudraModule;
import top.babyzombie.addons.module.hunting.HuntingModule;
import top.babyzombie.addons.module.misc.loadout.LoadoutModule;
import top.babyzombie.addons.module.misc.pet.PetDisplayHud;
import top.babyzombie.addons.module.kuudra.ArrowPoisonRefill;
import top.babyzombie.addons.module.mining.MiningModule;
import top.babyzombie.addons.module.minigames.ravengard.RavengardModule;
import top.babyzombie.addons.module.misc.MiscModule;
import top.babyzombie.addons.module.chat.PartyModule;
import top.babyzombie.addons.module.chat.playcmd.PlayCmdModule;
import top.babyzombie.addons.module.chat.AutotipModule;
import top.babyzombie.addons.module.chat.ChatChannelModule;
import top.babyzombie.addons.module.chat.ContainerChatModule;
import top.babyzombie.addons.module.chat.WaypointMarkerModule;
import top.babyzombie.addons.module.chat.popup.PopupEventsModule;
import top.babyzombie.addons.module.misc.raredrop.RareDropModule;
import top.babyzombie.addons.module.slayer.SlayerModule;
import top.babyzombie.addons.module.misc.UpdateChecker;
import top.babyzombie.addons.module.misc.WindowTitleModule;
import top.babyzombie.addons.config.hud.CategoryHudSwitcher;
import top.babyzombie.addons.module.dungeon.withercloak.WitherCloakModule;
import top.babyzombie.addons.module.misc.bazaar.BazzarTopOrdersOverlay;
import top.babyzombie.addons.util.DungeonCooldown;
import top.babyzombie.addons.util.gui.overlay.GuiOverlayManager;
import top.babyzombie.addons.util.PersistenceMigration;
import top.babyzombie.addons.util.render.WorldRenderUtils;
import top.babyzombie.addons.util.tracker.HypixelLocationTracker;
import top.babyzombie.addons.util.tracker.HypixelPlayerInfoTracker;
import top.babyzombie.addons.util.ClientBossbarManager;
import top.babyzombie.addons.util.KeyBindingUtil;
import top.babyzombie.addons.util.tracker.PartyTracker;
import top.babyzombie.addons.util.tracker.ServerVisitTracker;
import top.babyzombie.addons.util.ServerTickCounter;
import top.babyzombie.addons.util.pet.PetManager;
import top.babyzombie.addons.util.render.Waypoints;

public class BabyzombieAddonsClient implements ClientModInitializer {

    public static net.minecraft.client.KeyMapping cancelKeyBindingRelease;
    public static net.minecraft.client.KeyMapping toggleHandRenderKey;

    @Override
    public void onInitializeClient() {
        // 先迁移旧持久化文件布局,再让各模块读取
        PersistenceMigration.run();

        // 内置 Chroma x Modern UI 兼容材质包
        ResourceLoader.registerBuiltinPack(
                Identifier.fromNamespaceAndPath("babyzombieaddons", "chroma_modernui"),
                FabricLoader.getInstance().getModContainer("babyzombieaddons").orElseThrow(),
                Component.translatable("resourcepack.babyzombieaddons.chroma_modernui.name"),
                PackActivationType.ALWAYS_ENABLED
        );

        ModConfigManager.init();
        UpdateChecker.init();
        HudManager.init();
        HudRegistrar.register();
        GuiOverlayManager.init();
        CategoryHudSwitcher.init();
        BazzarTopOrdersOverlay.init();

        cancelKeyBindingRelease = KeyBindingUtil.register(
                "key.babyzombieaddons.cancel_key_release", GLFW.GLFW_KEY_LEFT_ALT);

        toggleHandRenderKey = KeyBindingUtil.register(
                "key.babyzombieaddons.toggle_hand_render", GLFW.GLFW_KEY_UNKNOWN);

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null) return;
            while (toggleHandRenderKey.consumeClick()) {
                ModConfigManager.get().general.handRender.swapHands = !ModConfigManager.get().general.handRender.swapHands;
            }
        });

        HypixelLocationTracker.getInstance().init();
        ServerVisitTracker.getInstance().init();
        AbiphoneTracker.getInstance().init();
        PartyTracker.getInstance().init();
        HypixelPlayerInfoTracker.getInstance().init();
        ClientBossbarManager.init();
        DungeonCooldown.init();
        ServerTickCounter.init();
        Waypoints.init();
        IncomingCallHandler.register();
        CustomRingtoneModule.init();

        BabyzombieAddonsCommand.init();

        AutoJoinModule.init();
        AutoISModule.init();
        KickRecoveryModule.init();
        AutoReconnectHelper.init();
        BazaarSellFromSacks.init();
        DungeonModule.init();
        CustomDiscScanner.init();
        DungeonJukeboxModule.init();
        RareSeaCreaturesAlert.init();
        RareSeaCreaturesSelfAlert.init();
        PreventInstantReel.init();
        GardenModule.init();
        GreatSpookModule.init();
        FruitDiggingModule.init();
        RaffleTaskModule.init();
        KuudraModule.init();
        ArrowPoisonRefill.init();
        MiningModule.init();
        MiscModule.init();
        PartyModule.init();
        PlayCmdModule.init();
        AutotipModule.init();
        ChatChannelModule.init();
        WaypointMarkerModule.init();
        ContainerChatModule.init();
        PopupEventsModule.init();
        RareDropModule.init();
        SlayerModule.init();
        WitherCloakModule.init();
        WindowTitleModule.init();
        PetManager.getInstance().init();
        PetDisplayHud.init();
        HuntingModule.init();
        LoadoutModule.init();
        RavengardModule.init();

        ClientLifecycleEvents.CLIENT_STOPPING.register(_ -> WorldRenderUtils.close());

        // =====================================================================
        // Screen 全局鼠标事件统一入口（替代原来不可行的 ScreenMixin 三方法注入 +
        // GuiEventHandlerMixin 接口 Mixin 方案）。
        //
        // MC 26.1.2 的 Screen 类不重写 ContainerEventHandler 的 mouseClicked /
        // mouseReleased / mouseDragged 三 default 方法，直接 @Mixin(Screen.class)
        // 注入会失败；尝试 @Mixin(ContainerEventHandler.class) 也会被
        // sponge-mixin 0.8.7 的 SubType$Standard 校验拒绝。因此使用官方提供的
        // Fabric Screen Mouse Events API 完成相同的全局拦截。
        //
        // 事件注册基于 per-screen instance，每次 AFTER_INIT（新建/尺寸变更）
        // 时重新挂到具体 screen 实例上，符合 Fabric 设计。
        //
        // 说明：AbstractContainerScreen 子类重写了这三个方法，ContainerClickMixin
        // 原本也对 CHS / GuiOverlayManager 进行拦截；为避免 AbstractContainerScreen
        // 子类出现双重触发，ContainerClickMixin 中 CHS + GuiOverlayManager 的
        // 重复片段已移除，统一由这里的 Screen API 全局注册生效。
        // =====================================================================
        ScreenEvents.AFTER_INIT.register((mc, screen, sw, sh) -> {
            // --- mouseClicked ---
            // 返回 false = 不允许（= 被消费，屏蔽 Screen 原本的处理）；返回 true = 允许继续。
            // 重要：CHS 与 GuiOverlayManager 的回调具有副作用（设置 pressed 等状态），
            // 只允许在 allowMouseClick 中调用一次，after 阶段不再重复触发。
            ScreenMouseEvents.allowMouseClick(screen).register((s, event) -> {
                if (CategoryHudSwitcher.onMouseClicked(event)) return false;
                return !GuiOverlayManager.onMouseClicked(s, event.x(), event.y(), event.button());
            });

            // --- mouseReleased ---
            ScreenMouseEvents.allowMouseRelease(screen).register((s, event) -> {
                if (CategoryHudSwitcher.onMouseReleased(event)) return false;
                return !GuiOverlayManager.onMouseReleased(s, event.x(), event.y(), event.button());
            });

            // --- mouseDragged ---
            ScreenMouseEvents.allowMouseDrag(screen).register((s, event, dx, dy) -> {
                if (CategoryHudSwitcher.onMouseDragged(event, dx, dy)) return false;
                return !GuiOverlayManager.onMouseDragged(s, event.x(), event.y(), event.button(), dx, dy);
            });
        });
    }
}
