package top.babyzombie.addons;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.fabricmc.fabric.api.resource.v1.pack.PackActivationType;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;
import top.babyzombie.addons.command.BabyzombieAddonsCommand;
import top.babyzombie.addons.config.hud.HudManager;
import top.babyzombie.addons.config.hud.HudRegistrar;
import top.babyzombie.addons.config.ModConfigManager;
import top.babyzombie.addons.module.abiphone.AbiphoneTracker;
import top.babyzombie.addons.module.abiphone.IncomingCallHandler;
import top.babyzombie.addons.module.autoconnect.AutoJoinModule;
import top.babyzombie.addons.module.autois.AutoISModule;
import top.babyzombie.addons.module.misc.AutoReconnectHelper;
import top.babyzombie.addons.module.misc.BazaarSellFromSacks;
import top.babyzombie.addons.module.dungeon.DungeonModule;
import top.babyzombie.addons.module.garden.GardenModule;
import top.babyzombie.addons.module.events.FruitDiggingModule;
import top.babyzombie.addons.module.events.GreatSpookModule;
import top.babyzombie.addons.module.kuudra.KuudraModule;
import top.babyzombie.addons.module.kuudra.ArrowPoisonRefill;
import top.babyzombie.addons.module.mining.MiningModule;
import top.babyzombie.addons.module.misc.MiscModule;
import top.babyzombie.addons.module.party.PartyModule;
import top.babyzombie.addons.module.playcmd.PlayCmdModule;
import top.babyzombie.addons.module.chat.ChatChannelModule;
import top.babyzombie.addons.module.chat.ContainerChatModule;
import top.babyzombie.addons.module.popup.PopupEventsModule;
import top.babyzombie.addons.module.raredrop.RareDropModule;
import top.babyzombie.addons.module.slayer.SlayerModule;
import top.babyzombie.addons.module.misc.UpdateChecker;
import top.babyzombie.addons.module.withercloak.WitherCloakModule;
import top.babyzombie.addons.util.DungeonCooldown;
import top.babyzombie.addons.util.tracker.HypixelLocationTracker;
import top.babyzombie.addons.util.KeyBindingUtil;
import top.babyzombie.addons.util.tracker.PartyTracker;
import top.babyzombie.addons.util.ServerTickCounter;
import top.babyzombie.addons.util.pet.PetManager;
import top.babyzombie.addons.util.render.Waypoints;

public class BabyzombieAddonsClient implements ClientModInitializer {

    public static net.minecraft.client.KeyMapping cancelKeyBindingRelease;
    public static net.minecraft.client.KeyMapping toggleHandRenderKey;
    public static boolean handRenderSwapActive;

    @Override
    public void onInitializeClient() {
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

        cancelKeyBindingRelease = KeyBindingUtil.register(
                "key.babyzombieaddons.cancel_key_release", GLFW.GLFW_KEY_LEFT_ALT);

        toggleHandRenderKey = KeyBindingUtil.register(
                "key.babyzombieaddons.toggle_hand_render", GLFW.GLFW_KEY_UNKNOWN);

        handRenderSwapActive = ModConfigManager.get().handRender.swapHands;

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null) return;
            while (toggleHandRenderKey.consumeClick()) {
                handRenderSwapActive = !handRenderSwapActive;
            }
        });

        HypixelLocationTracker.getInstance().init();
        AbiphoneTracker.getInstance().init();
        PartyTracker.getInstance().init();
        DungeonCooldown.init();
        ServerTickCounter.init();
        Waypoints.init();
        IncomingCallHandler.register();

        BabyzombieAddonsCommand.init();

        AutoJoinModule.init();
        AutoISModule.init();
        AutoReconnectHelper.init();
        BazaarSellFromSacks.init();
        DungeonModule.init();
        GardenModule.init();
        GreatSpookModule.init();
        FruitDiggingModule.init();
        KuudraModule.init();
        ArrowPoisonRefill.init();
        MiningModule.init();
        MiscModule.init();
        PartyModule.init();
        PlayCmdModule.init();
        ChatChannelModule.init();
        ContainerChatModule.init();
        PopupEventsModule.init();
        RareDropModule.init();
        SlayerModule.init();
        WitherCloakModule.init();
        PetManager.getInstance().init();
    }
}
