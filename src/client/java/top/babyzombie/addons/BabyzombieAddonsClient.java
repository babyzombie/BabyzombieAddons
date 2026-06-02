package top.babyzombie.addons;

import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.api.ClientModInitializer;
import org.lwjgl.glfw.GLFW;
import top.babyzombie.addons.config.HudManager;
import top.babyzombie.addons.config.ModConfig;
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
import top.babyzombie.addons.module.withercloak.WitherCloakModule;
import top.babyzombie.addons.util.HypixelLocationTracker;
import top.babyzombie.addons.util.KeyBindingUtil;

public class BabyzombieAddonsClient implements ClientModInitializer {

    public static net.minecraft.client.KeyMapping cancelKeyBindingRelease;

    @Override
    public void onInitializeClient() {
        ModConfigManager.init();
        HudManager.init();
        registerHudElements();

        cancelKeyBindingRelease = KeyBindingUtil.register(
                "key.babyzombieaddons.cancel_key_release", GLFW.GLFW_KEY_LEFT_ALT);

        HypixelLocationTracker.getInstance().init();
        AbiphoneTracker.getInstance().init();
        IncomingCallHandler.register();

        HudRenderCallback.EVENT.register((gui, delta) ->
                HudManager.renderEditOverlay(gui, gui.guiWidth()/2, gui.guiHeight()/2));

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
    }

    private static void registerHudElements() {
        var cfg = ModConfigManager.get();
        HudManager.register("WitherCloak", 120, 100, 1.0f, 62, 45,
                () -> cfg.witherCloak.witherCloakTimer || cfg.witherCloak.soulwardTimer
                        || cfg.witherCloak.alignedTimer || cfg.witherCloak.gravityStormTimer);
        HudManager.register("KillCombo", 50, 100, 1.0f, 75, 38,
                () -> cfg.misc.killComboHUD);
        HudManager.register("PigmanSword", 50, 60, 1.5f, 50, 9,
                () -> cfg.slayer.pigmanSwordTimer);
        HudManager.register("RagnarockAxe", 50, 50, 1.5f, 60, 9,
                () -> cfg.slayer.ragnarockAxeTimer);
        HudManager.register("ReaperArmor", 60, 50, 1.5f, 50, 9,
                () -> cfg.slayer.reaperArmorTimer);
        HudManager.register("EndStoneSword", 60, 60, 1.0f, 45, 18,
                () -> cfg.slayer.endStoneSwordTimer);
        HudManager.register("SlayerBoss", 190, 100, 1.5f, 100, 9,
                () -> cfg.slayer.bossInfoHUD);
        HudManager.register("KuudraHP", 200, 20, 2.0f, 35, 9,
                () -> cfg.kuudra.hpDisplay != ModConfig.HpDisplayMode.OFF);
        HudManager.register("EnergyCharge", 400, 120, 1.0f, 60, 9,
                () -> cfg.kuudra.energyDisplay);
        HudManager.register("KuudraStun", 400, 130, 1.0f, 80, 9,
                () -> cfg.kuudra.stunTimer);
        HudManager.register("InKuudraStun", 400, 140, 0.7f, 60, 30,
                () -> cfg.kuudra.stunTimer);
        HudManager.register("SuspiciousScrap", 400, 110, 1.0f, 34, 16,
                () -> cfg.mining.suspiciousScrapCounter);

        HudManager.register("DevilsContract", 400, 350, 1.0f, 68, 9,
                () -> true);
    }
}
