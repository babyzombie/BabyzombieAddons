package top.babyzombie.addons.config.hud;

import top.babyzombie.addons.config.ModConfig;
import top.babyzombie.addons.config.ModConfigManager;

public final class HudRegistrar {
    private HudRegistrar() {}

    public static void register() {
        // --- Slayer ---
        HudManager.register("PigmanSword", 50, 60, 1.5f, 50, 9, "Pigman: 2.5s",
                () -> get().slayer.pigmanSwordTimer);
        HudManager.register("RagnarockAxe", 50, 50, 1.5f, 60, 9, "Rag Axe: Ready",
                () -> get().slayer.ragnarockAxeTimer);
        HudManager.register("ReaperArmor", 60, 50, 1.5f, 50, 9, "Reaper: 10.0s",
                () -> get().slayer.reaperArmorTimer);
        HudManager.register("EndStoneSword", 60, 60, 1.0f, 45, 18, "EndStone: 10s",
                () -> get().slayer.endStoneSwordTimer);
        HudManager.register("SlayerBoss", 190, 100, 1.5f, 100, 9, "Revenant IV 1.5M❤",
                () -> get().slayer.bossInfoHUD);

        // --- Kuudra ---
        HudManager.register("KuudraHP", 200, 20, 2.0f, 35, 9, "Kuudra: 100%",
                () -> get().kuudra.hpDisplay != ModConfig.HpDisplayMode.OFF);
        HudManager.register("EnergyCharge", 400, 120, 1.0f, 60, 9, "Energy: 100%",
                () -> get().kuudra.energyDisplay);
        HudManager.register("KuudraStun", 400, 130, 1.0f, 80, 9, "Stun: 10.0s",
                () -> get().kuudra.stunTimer);
        HudManager.register("InKuudraStun", 400, 140, 0.7f, 60, 30, "In Stun",
                () -> get().kuudra.stunTimer);
        HudManager.register("KuudraDir", 360, 360, 1.0f, 24, 24, "Δ Kuudra",
                () -> get().kuudra.directionIndicator);

        // --- Wither Cloak ---
        HudManager.register("WitherCloak", 120, 100, 1.0f, 62, 45, "Shield: 5.0s",
                () -> get().witherCloak.witherCloakTimer || get().witherCloak.soulwardTimer
                        || get().witherCloak.alignedTimer || get().witherCloak.gravityStormTimer);

        // --- Mining ---
        HudManager.register("SuspiciousScrap", 400, 110, 1.0f, 34, 16, "Scraps: 3/5",
                () -> get().mining.suspiciousScrapCounter);
        HudManager.register("ArmadilloEnergy", 400, 10, 1.0f, 100, 9, "Armadillo 100/200",
                () -> get().mining.armadilloEnergy);
        HudManager.register("ScathaCooldown", 50, 80, 1.0f, 80, 9, "Scatha: 29.999s",
                () -> get().mining.scathaCooldown);

        // --- Popup ---
        HudManager.register("Popup", 420, 50, 1.0f, 152, 52, "Popup Demo",
                () -> get().popup.popupPartyInvite || get().popup.popupGuildPartyInvite
                        || get().popup.popupFriendRequest || get().popup.popupDuelsRequest
                        || get().popup.popupSkyblockTrade || get().popup.popupDungeonRestart);

    }

    private static ModConfig get() {
        return ModConfigManager.get();
    }
}
