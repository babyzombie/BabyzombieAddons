package top.babyzombie.addons;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;
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
import top.babyzombie.addons.module.chat.ChatModule;
import top.babyzombie.addons.module.events.EventsModule;
import top.babyzombie.addons.module.fishing.*;
import top.babyzombie.addons.module.dungeon.CustomDiscScanner;
import top.babyzombie.addons.module.dungeon.DungeonModule;
import top.babyzombie.addons.module.garden.GardenModule;
import top.babyzombie.addons.module.kuudra.KuudraModule;
import top.babyzombie.addons.module.hunting.HuntingModule;
import top.babyzombie.addons.module.mining.MiningModule;
import top.babyzombie.addons.module.minigames.ravengard.RavengardModule;
import top.babyzombie.addons.module.misc.MiscModule;
import top.babyzombie.addons.module.slayer.SlayerModule;
import top.babyzombie.addons.module.misc.UpdateChecker;
import top.babyzombie.addons.util.ChatUtils;
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
    public static net.minecraft.client.KeyMapping entityHiderToggleKey;

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

        cancelKeyBindingRelease = KeyBindingUtil.register(
                "key.babyzombieaddons.cancel_key_release", -1);

        toggleHandRenderKey = KeyBindingUtil.register(
                "key.babyzombieaddons.toggle_hand_render", -1);

        entityHiderToggleKey = KeyBindingUtil.register(
                "key.babyzombieaddons.toggle_entity_hider", -1);

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null) return;
            while (toggleHandRenderKey.consumeClick()) {
                ModConfigManager.get().general.handRender.swapHands = !ModConfigManager.get().general.handRender.swapHands;
            }
            while (entityHiderToggleKey.consumeClick()) {
                var entityHider = ModConfigManager.get().general.entityHider;
                entityHider.enabled = !entityHider.enabled;
                ChatUtils.showTranslatable(entityHider.enabled
                        ? "babyzombieaddons.entityHider.enabled"
                        : "babyzombieaddons.entityHider.disabled");
            }
        });

        HypixelLocationTracker.getInstance().init();
        ServerVisitTracker.getInstance().init();
        PartyTracker.getInstance().init();
        HypixelPlayerInfoTracker.getInstance().init();
        ClientBossbarManager.init();
        DungeonCooldown.init();
        ServerTickCounter.init();
        Waypoints.init();
        PetManager.getInstance().init();

        BabyzombieAddonsCommand.init();

        DungeonModule.init();
        CustomDiscScanner.init();
        FishingModule.init();
        GardenModule.init();
        EventsModule.init();
        KuudraModule.init();
        MiningModule.init();
        MiscModule.init();
        ChatModule.init();
        SlayerModule.init();
        HuntingModule.init();

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
        // 说明：这里只负责全局的 GuiOverlayManager 拦截。分类 HUD 切换器（CHS）
        // 仅在 HudEditScreen 内生效，已内联到 HudEditScreen 自己处理，不再走全局
        // 注册，避免与 HudEditScreen 的手动调用双重触发。
        // =====================================================================
        ScreenEvents.AFTER_INIT.register((minecraft, screen, sw, sh) -> {
            // --- mouseClicked ---
            // 返回 false = 不允许（= 被消费，屏蔽 Screen 原本的处理）；返回 true = 允许继续。
            ScreenMouseEvents.allowMouseClick(screen).register((s, event) ->
                    !GuiOverlayManager.onMouseClicked(s, event.x(), event.y(), event.button()));

            // --- mouseReleased ---
            ScreenMouseEvents.allowMouseRelease(screen).register((s, event) ->
                    !GuiOverlayManager.onMouseReleased(s, event.x(), event.y(), event.button()));

            // --- mouseDragged ---
            ScreenMouseEvents.allowMouseDrag(screen).register((s, event, dx, dy) ->
                    !GuiOverlayManager.onMouseDragged(s, event.x(), event.y(), event.button(), dx, dy));
        });
    }
}
