package top.babyzombie.addons;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.KeyMapping;
import org.lwjgl.glfw.GLFW;
import top.babyzombie.addons.config.HudManager;
import top.babyzombie.addons.config.ModConfigManager;
import top.babyzombie.addons.module.abiphone.AbiphoneTracker;
import top.babyzombie.addons.module.abiphone.IncomingCallHandler;
import top.babyzombie.addons.module.autois.AutoISModule;
import top.babyzombie.addons.module.dungeon.DungeonModule;
import top.babyzombie.addons.module.fishing.FishingModule;
import top.babyzombie.addons.module.garden.GardenModule;
import top.babyzombie.addons.module.greatspook.GreatSpookModule;
import top.babyzombie.addons.module.heavypearls.HeavyPearlsModule;
import top.babyzombie.addons.module.kuudra.KuudraModule;
import top.babyzombie.addons.module.mining.MiningModule;
import top.babyzombie.addons.module.misc.MiscModule;
import top.babyzombie.addons.module.party.PartyModule;
import top.babyzombie.addons.module.playcmd.PlayCmdModule;
import top.babyzombie.addons.module.popup.PopupEventsModule;
import top.babyzombie.addons.module.raredrop.RareDropModule;
import top.babyzombie.addons.module.sack.SackModule;
import top.babyzombie.addons.module.skywars.SkywarsModule;
import top.babyzombie.addons.module.slayer.SlayerModule;
import top.babyzombie.addons.module.withercloak.WitherCloakModule;
import top.babyzombie.addons.util.HypixelLocationTracker;

public class BabyzombieAddonsClient implements ClientModInitializer {

    public static KeyMapping cancelKeyBindingRelease;

    @Override
    public void onInitializeClient() {
        ModConfigManager.init();
        HudManager.init();
        registerHudElements();

        cancelKeyBindingRelease = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.babyzombieaddons.cancel_key_release",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_LEFT_ALT,
                KeyMapping.Category.MISC
        ));

        HypixelLocationTracker.getInstance().init();
        AbiphoneTracker.getInstance().init();
        IncomingCallHandler.register();

        HudRenderCallback.EVENT.register((gui, delta) ->
                HudManager.renderEditOverlay(gui, (int)gui.guiWidth()/2, (int)gui.guiHeight()/2));

        BabyzombieAddonsCommand.init();

        AutoISModule.init();
        DungeonModule.init();
        FishingModule.init();
        GardenModule.init();
        GreatSpookModule.init();
        HeavyPearlsModule.init();
        KuudraModule.init();
        MiningModule.init();
        MiscModule.init();
        PartyModule.init();
        PlayCmdModule.init();
        PopupEventsModule.init();
        RareDropModule.init();
        SackModule.init();
        SkywarsModule.init();
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
                () -> cfg.kuudra.hpDisplay);
        HudManager.register("SuspiciousScrap", 400, 110, 1.0f, 34, 16,
                () -> cfg.mining.suspiciousScrapCounter);
        HudManager.register("SackItems", 200, 100, 1.0f, 80, 27,
                () -> cfg.misc.sackItemHUD);
        HudManager.register("DevilsContract", 400, 350, 1.0f, 68, 9,
                () -> true);
    }
}