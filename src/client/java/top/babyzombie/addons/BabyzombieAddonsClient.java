package top.babyzombie.addons;

import net.fabricmc.api.ClientModInitializer;
import org.lwjgl.glfw.GLFW;
import top.babyzombie.addons.config.hud.HudManager;
import top.babyzombie.addons.config.hud.HudRegistrar;
import top.babyzombie.addons.config.ModConfigManager;
import top.babyzombie.addons.module.abiphone.AbiphoneTracker;
import top.babyzombie.addons.module.abiphone.IncomingCallHandler;
import top.babyzombie.addons.module.autois.AutoISModule;
import top.babyzombie.addons.module.dungeon.DungeonModule;
import top.babyzombie.addons.module.garden.GardenModule;
import top.babyzombie.addons.module.events.FruitDiggingModule;
import top.babyzombie.addons.module.events.GreatSpookModule;
import top.babyzombie.addons.module.kuudra.KuudraModule;
import top.babyzombie.addons.module.mining.MiningModule;
import top.babyzombie.addons.module.misc.MiscModule;
import top.babyzombie.addons.module.party.PartyModule;
import top.babyzombie.addons.module.playcmd.PlayCmdModule;
import top.babyzombie.addons.module.popup.PopupEventsModule;
import top.babyzombie.addons.module.raredrop.RareDropModule;
import top.babyzombie.addons.module.slayer.SlayerModule;
import top.babyzombie.addons.module.misc.UpdateChecker;
import top.babyzombie.addons.module.withercloak.WitherCloakModule;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import top.babyzombie.addons.util.BeaconStateInjector;
import top.babyzombie.addons.util.DungeonCooldown;
import top.babyzombie.addons.util.HypixelLocationTracker;
import top.babyzombie.addons.util.KeyBindingUtil;
import top.babyzombie.addons.util.PartyTracker;
import top.babyzombie.addons.util.WorldRenderUtils;

public class BabyzombieAddonsClient implements ClientModInitializer {

    public static net.minecraft.client.KeyMapping cancelKeyBindingRelease;

    @Override
    public void onInitializeClient() {
        ModConfigManager.init();
        UpdateChecker.init();
        HudManager.init();
        BeaconStateInjector.init();
        HudRegistrar.register();

        cancelKeyBindingRelease = KeyBindingUtil.register(
                "key.babyzombieaddons.cancel_key_release", GLFW.GLFW_KEY_LEFT_ALT);

        HypixelLocationTracker.getInstance().init();
        AbiphoneTracker.getInstance().init();
        PartyTracker.getInstance().init();
        DungeonCooldown.init();
        IncomingCallHandler.register();

        BabyzombieAddonsCommand.init();

        AutoISModule.init();
        DungeonModule.init();
        GardenModule.init();
        GreatSpookModule.init();
        FruitDiggingModule.init();
        KuudraModule.init();
        MiningModule.init();
        MiscModule.init();
        PartyModule.init();
        PlayCmdModule.init();
        PopupEventsModule.init();
        RareDropModule.init();
        SlayerModule.init();
        WitherCloakModule.init();

        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> WorldRenderUtils.close());
    }
}
