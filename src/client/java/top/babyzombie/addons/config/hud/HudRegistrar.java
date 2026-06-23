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
        HudManager.register("HolyIce", 50, 100, 1.5f,
                "§bHoly Ice: §a1.250s",
                "config.babyzombieaddons.option.holyIceTimer",
                () -> get().slayer.holyIceTimer);
        HudManager.register("RagnarockAxe", 50, 50, 1.5f,
                "§5Ragnarock: §b2.80s",
                "config.babyzombieaddons.option.ragnarockAxeTimer",
                () -> get().slayer.ragnarockAxeTimer != ModConfig.RagnarockAxeMode.OFF);
        HudManager.register("ReaperArmor", 60, 50, 1.5f,
                "§8Reaper Armor: §a10.00s",
                "config.babyzombieaddons.option.reaperArmorTimer",
                () -> get().slayer.reaperArmorTimer);
        HudManager.register("EndStoneSword", 60, 60, 1.0f,
                "     §a❈ 50%\n     §e3.14s",
                "config.babyzombieaddons.option.endStoneSwordTimer",
                () -> get().slayer.endStoneSwordTimer != ModConfig.EndStoneSwordMode.OFF);
        HudManager.register("ReheatedGummyPolarBear", 80, 80, 1.0f,
                "    §a12:34",
                "config.babyzombieaddons.option.reheatedGummyPolarBear",
                () -> get().slayer.reheatedGummyPolarBear != ModConfig.GummyPolarBearMode.OFF);
        HudManager.register("SlayerBoss", 200, 20, 1.5f,
                "§5Voidgloom Seraph §a205M§c❤",
                "config.babyzombieaddons.option.slayerBossInfo",
                () -> {
                    var s = get().slayer;
                    return s.zombieSlayerInfo != ModConfig.SlayerBossInfoMode.OFF
                            || s.spiderSlayerInfo != ModConfig.SlayerBossInfoMode.OFF
                            || s.wolfSlayerInfo != ModConfig.SlayerBossInfoMode.OFF
                            || s.endermanSlayerInfo != ModConfig.SlayerBossInfoMode.OFF
                            || s.blazeSlayerInfo != ModConfig.SlayerBossInfoMode.OFF
                            || s.vampireSlayerInfo != ModConfig.SlayerBossInfoMode.OFF;
                });

        // --- Kuudra ---
        HudManager.register("KuudraHP", 200, 20, 2.0f,
                "§4§l100,000§c/100,000",
                "config.babyzombieaddons.option.hpDisplay",
                () -> get().kuudra.hpDisplay == ModConfig.HpDisplayMode.HUD);
        HudManager.register("EnergyCharge", 400, 120, 1.0f,
                "§fEnergy Charge: §a100%",
                "config.babyzombieaddons.option.energyDisplay",
                () -> get().kuudra.energyDisplay);
        HudManager.register("KuudraStun", 400, 130, 1.0f,
                "§aKuudra is stunned, §4§l0:10 §aleft\n§4§lPHASE 4 - TRUE LAIR",
                "config.babyzombieaddons.option.stunTimer",
                () -> get().kuudra.stunTimer);
        // --- Wither Cloak ---
        HudManager.register("WitherCloakTimer", 120, 100, 1.0f,
                "§a§lWither Cloak\n§aactivated 5.00s",
                "config.babyzombieaddons.option.witherCloakTimer",
                () -> get().witherCloak.witherCloakTimer);
        HudManager.register("SoulwardTimer", 120, 140, 1.0f,
                "§1§lSoulward§7:§r §a3.50s",
                "config.babyzombieaddons.option.soulwardTimer",
                () -> get().witherCloak.soulwardTimer);
        HudManager.register("AlignedTimer", 120, 160, 1.0f,
                "§a§laligned §r§61.23s §a|||\n§e§lby §r§ayourself",
                "config.babyzombieaddons.option.alignedTimer",
                () -> get().witherCloak.alignedTimer);
        HudManager.register("GravityStormTimer", 120, 200, 1.0f,
                "§5§lGravity Storm §r§b24.12s",
                "config.babyzombieaddons.option.gravityStormTimer",
                () -> get().witherCloak.gravityStormTimer);

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

        // --- Chat Channel ---
        HudManager.register("ChatChannelSwitcher", 410, 480, 1.0f,
                "§aGC     §7OC     PC     CC     AC",
                "config.babyzombieaddons.option.chatChannelSwitcher",
                () -> get().chatChannel.chatChannelSwitcher);

        // --- Popup ---
        HudManager.register("Popup", 420, 50, 1.0f,
                "§6Party Invite\n§fPlayer invites you to party\n§a[Y] Accept  §e[N] Ignore",
                "config.babyzombieaddons.category.popup",
                () -> get().popup.popupPartyInvite || get().popup.popupGuildPartyInvite
                        || get().popup.popupFriendRequest || get().popup.popupDuelsRequest
                        || get().popup.popupSkyblockTrade || get().popup.popupDungeonRestart
                        || get().popup.popupBaitLow > 0);
    }

    private static ModConfig get() {
        return ModConfigManager.get();
    }
}
