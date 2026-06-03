package top.babyzombie.addons.config.hud;

import top.babyzombie.addons.config.ModConfig;
import top.babyzombie.addons.config.ModConfigManager;

public final class HudRegistrar {
    private HudRegistrar() {}

    public static void register() {
        // --- Slayer ---
        HudManager.register("PigmanSword", 50, 60, 1.5f,
                "§6Pigman: §a2.50s",
                "config.babyzombieaddons.option.pigmanSwordTimer",
                () -> get().slayer.pigmanSwordTimer);
        HudManager.register("RagnarockAxe", 50, 50, 1.5f,
                "§5Ragnarock: §b2.80s",
                "config.babyzombieaddons.option.ragnarockAxeTimer",
                () -> get().slayer.ragnarockAxeTimer);
        HudManager.register("ReaperArmor", 60, 50, 1.5f,
                "§8Reaper Armor: §a10.00s",
                "config.babyzombieaddons.option.reaperArmorTimer",
                () -> get().slayer.reaperArmorTimer);
        HudManager.register("EndStoneSword", 60, 60, 1.0f,
                "§eEnd Stone Sword: §a100% §7DR",
                "config.babyzombieaddons.option.endStoneSwordTimer",
                () -> get().slayer.endStoneSwordTimer);
        HudManager.register("SlayerBoss", 190, 100, 1.5f,
                "§4§lRevenant V §c100% §7(1.5M/1.5M)",
                "config.babyzombieaddons.option.bossInfoHUD",
                () -> get().slayer.bossInfoHUD);

        // --- Kuudra ---
        HudManager.register("KuudraHP", 200, 20, 2.0f,
                "§4§l100,000§c/100,000",
                "config.babyzombieaddons.option.hpDisplay",
                () -> get().kuudra.hpDisplay != ModConfig.HpDisplayMode.OFF);
        HudManager.register("EnergyCharge", 400, 120, 1.0f,
                "§fEnergy Charge: §a100%",
                "config.babyzombieaddons.option.energyDisplay",
                () -> get().kuudra.energyDisplay);
        HudManager.register("KuudraStun", 400, 130, 1.0f,
                "§aKuudra is stunned, §4§l0:10 §aleft\n§4§lPHASE 4 - TRUE LAIR",
                "config.babyzombieaddons.option.stunTimer",
                () -> get().kuudra.stunTimer);
        HudManager.register("KuudraDir", 360, 360, 1.0f,
                " §4§l►§c∆§4§l◄ ",
                "config.babyzombieaddons.option.directionIndicator",
                () -> get().kuudra.directionIndicator);

        // --- Wither Cloak ---
        HudManager.register("WitherCloak", 120, 100, 1.0f,
                "§a§lWither Cloak\n§aactivated 5.00s\n§1§lSoulward§7:§r §a3.50s\n§a§laligned §r§61.23s §a|||\n§e§lby §r§ayourself\n§5§lGravity Storm §r§b24.12s",
                "config.babyzombieaddons.group.witherCloak",
                () -> get().witherCloak.witherCloakTimer || get().witherCloak.soulwardTimer
                        || get().witherCloak.alignedTimer || get().witherCloak.gravityStormTimer);

        // --- Mining ---
        HudManager.register("SuspiciousScrap", 400, 110, 1.0f,
                "§6Scraps: §e3/5",
                "config.babyzombieaddons.option.suspiciousScrapCounter",
                () -> get().mining.suspiciousScrapCounter);
        HudManager.register("ArmadilloEnergy", 400, 10, 1.0f,
                "§eArmadillo §f100/200",
                "config.babyzombieaddons.option.armadilloEnergy",
                () -> get().mining.armadilloEnergy);
        HudManager.register("ScathaCooldown", 50, 80, 1.0f,
                "§5§lScatha: §8§l29.999s",
                "config.babyzombieaddons.option.scathaCooldown",
                () -> get().mining.scathaCooldown);

        // --- Popup ---
        HudManager.register("Popup", 420, 50, 1.0f,
                "§6Party Invite\n§fPlayer invites you to party\n§a[Y] Accept  §e[N] Ignore",
                "config.babyzombieaddons.category.popup",
                () -> get().popup.popupPartyInvite || get().popup.popupGuildPartyInvite
                        || get().popup.popupFriendRequest || get().popup.popupDuelsRequest
                        || get().popup.popupSkyblockTrade || get().popup.popupDungeonRestart);
    }

    private static ModConfig get() {
        return ModConfigManager.get();
    }
}
