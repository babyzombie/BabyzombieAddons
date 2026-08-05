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
                () -> get().slayer.itemSkillTimers.pigmanSwordTimer);
        HudManager.register("HolyIce", 50, 100, 1.5f,
                "§bHoly Ice: §a1.250s",
                "config.babyzombieaddons.option.holyIceTimer",
                () -> get().slayer.itemSkillTimers.holyIceTimer);
        HudManager.register("RagnarockAxe", 50, 50, 1.5f,
                "§5Ragnarock: §b2.80s",
                "config.babyzombieaddons.option.ragnarockAxeTimer",
                () -> get().slayer.itemSkillTimers.ragnarockAxeTimer != ModConfig.RagnarockAxeMode.OFF);
        HudManager.register("ReaperArmor", 60, 50, 1.5f,
                "§8Reaper Armor: §a10.00s",
                "config.babyzombieaddons.option.reaperArmorTimer",
                () -> get().slayer.itemSkillTimers.reaperArmorTimer);
        HudManager.register("EndStoneSword", 60, 60, 1.0f,
                "     §a❈ 50%\n     §e3.14s",
                "config.babyzombieaddons.option.endStoneSwordTimer",
                () -> get().slayer.itemSkillTimers.endStoneSwordTimer != ModConfig.EndStoneSwordMode.OFF);
        HudManager.register("ReheatedGummyPolarBear", 80, 80, 1.0f,
                "    §a12:34",
                "config.babyzombieaddons.option.reheatedGummyPolarBear",
                () -> get().slayer.itemSkillTimers.reheatedGummyPolarBear != ModConfig.GummyPolarBearMode.OFF);
        HudManager.register("SlayerBoss", 200, 20, 1.5f,
                "§5Voidgloom Seraph §a205M§c❤",
                "config.babyzombieaddons.option.slayerBossInfo",
                () -> {
                    var s = get().slayer;
                    return s.slayerBossInfo.zombieSlayerInfo != ModConfig.SlayerBossInfoMode.OFF
                            || s.slayerBossInfo.spiderSlayerInfo != ModConfig.SlayerBossInfoMode.OFF
                            || s.slayerBossInfo.wolfSlayerInfo != ModConfig.SlayerBossInfoMode.OFF
                            || s.slayerBossInfo.endermanSlayerInfo != ModConfig.SlayerBossInfoMode.OFF
                            || s.slayerBossInfo.blazeSlayerInfo != ModConfig.SlayerBossInfoMode.OFF
                            || s.slayerBossInfo.vampireSlayerInfo != ModConfig.SlayerBossInfoMode.OFF;
                });

        // --- Kuudra ---
        HudManager.register("KuudraHP", 200, 20, 2.0f,
                "§4§l100,000§c/100,000",
                "config.babyzombieaddons.option.hpDisplay",
                () -> get().kuudra.hpDisplay == ModConfig.HpDisplayMode.HUD);
        HudManager.register("EnergyCharge", 400, 120, 1.0f,
                "§fEnergy Charge: §a100%",
                "config.babyzombieaddons.option.energyDisplay",
                () -> get().kuudra.phase3.energyDisplay);
        HudManager.register("SupplyProgress", 10, 120, 1.0f,
                "§8[§a||||||||§8      ] §b4/6 §8(67%)",
                "config.babyzombieaddons.option.supplyProgressHud",
                () -> get().kuudra.phase1.supplyProgressHud || get().kuudra.phase3.fuelProgressHud);
        HudManager.register("SupplyTimes", 10, 140, 1.0f,
                "§b§lSupply Times §8[§a4§8/§a6§8]\n§bPlayer1 §8(1/6) §f§l14.85s\n§aPlayer2 §8(2/6) §f§l15.23s\n§aPlayer3 §8(3/6) §f§l22.10s\n§aPlayer4 §8(4/6) §f§l24.50s",
                "config.babyzombieaddons.option.supplyPlaceTimerHud",
                () -> get().kuudra.phase1.supplyPlaceTimerHud);
        HudManager.register("FreshHistory", 10, 250, 1.0f,
                "§b§lFresh Records\nPlayer1 §8@ §e14.5s\nPlayer2 §8@ §e18.2s",
                "config.babyzombieaddons.option.freshHistory",
                () -> get().kuudra.phase2.freshHistory);
        HudManager.register("ChestCounter", 10, 320, 1.5f,
                "§a30§7/60",
                "config.babyzombieaddons.option.chestCounter",
                () -> get().kuudra.chestCounterCfg.enabled);
        HudManager.register("BuildProgress", 10, 300, 1.0f,
                "§8[§e||||||||§8      ] §e69%",
                "config.babyzombieaddons.option.buildProgressHud",
                () -> get().kuudra.phase2.buildProgressHud);
        HudManager.register("KuudraDist", 200, 80, 1.5f,
                "§a12.5§fm",
                "config.babyzombieaddons.option.kuudraDistance",
                () -> get().kuudra.phase4.kuudraDistance);
        HudManager.register("KuudraDir", 200, 40, 2.0f,
                "§a§lFRONT",
                "config.babyzombieaddons.option.directionHud",
                () -> get().kuudra.phase4.directionHud);
        HudManager.register("KuudraSplits", 10, 20, 1.0f,
                "§b§lKuudra Splits\n§3Supplies §f22.45s\n§3Build §914.32s\n§3Eaten §a5.21s\n§3Stun §f0.53s\n§3DPS §63.89s\n§3Skip §f0.00s\n§3Boss §c5.12s\n§3Overall §a51.00s",
                "config.babyzombieaddons.option.phaseTimer",
                () -> get().kuudra.phaseTimer);
        HudManager.register("KuudraStun", 400, 130, 1.0f,
                "§aKuudra is stunned, §4§l0:10 §aleft\n§4§lPHASE 4 - TRUE LAIR",
                "config.babyzombieaddons.option.stunTimer",
                () -> get().kuudra.phase3.stunTimer);
        // --- Wither Cloak ---
        HudManager.register("WitherCloakTimer", 120, 100, 1.0f,
                "§a§lWither Cloak\n§aactivated 5.00s",
                "config.babyzombieaddons.option.witherCloakTimer",
                () -> get().dungeon.witherCloak.witherCloakTimer);
        HudManager.register("SoulwardTimer", 120, 140, 1.0f,
                "§1§lSoulward§7:§r §a3.50s",
                "config.babyzombieaddons.option.soulwardTimer",
                () -> get().dungeon.witherCloak.soulwardTimer);
        HudManager.register("AlignedTimer", 120, 160, 1.0f,
                "§a§laligned §r§61.23s §a|||\n§e§lby §r§ayourself",
                "config.babyzombieaddons.option.alignedTimer",
                () -> get().dungeon.witherCloak.alignedTimer);
        HudManager.register("GravityStormTimer", 120, 200, 1.0f,
                "§5§lGravity Storm §r§b24.12s",
                "config.babyzombieaddons.option.gravityStormTimer",
                () -> get().dungeon.witherCloak.gravityStormTimer);

        // --- Dungeon Jukebox ---
        HudManager.register("DungeonJukeboxDisc", 10, 100, 1.5f,
                "§b♫ §bPigstep\n§a§m           §7§m         §r §71:23 / 2:28",
                "config.babyzombieaddons.option.jukeboxShowHud",
                () -> get().dungeon.dungeonJukebox.showHud);

        // --- Mining ---
        HudManager.register("SuspiciousScrap", 400, 110, 1.0f,
                "§6Scraps: §e3/5",
                "config.babyzombieaddons.option.suspiciousScrapCounter",
                () -> get().mining.glaciteTunnels.suspiciousScrapCounter);
        HudManager.register("ArmadilloEnergy", 400, 10, 1.0f,
                "§eArmadillo §f100/200",
                "config.babyzombieaddons.option.armadilloEnergy",
                () -> get().mining.crystalHollows.armadilloEnergy);
        HudManager.register("ScathaCooldown", 50, 80, 1.0f,
                "§5§lScatha: §8§l29.999s",
                "config.babyzombieaddons.option.scathaCooldown",
                () -> get().mining.crystalHollows.scathaCooldown);

        // --- Chat Channel ---
        HudManager.register("ChatChannelSwitcher", 410, 480, 1.0f,
                "§aGC     §7OC     PC     CC     AC",
                "config.babyzombieaddons.option.chatChannelSwitcher",
                () -> get().general.chat.channelSwitcher);

        // --- Pet Display ---
        HudManager.register("PetDisplay", 10, 200, 1.0f,
                "§7Lv.100 §fRabbit\n§b95.5%",
                "config.babyzombieaddons.option.petDisplay",
                () -> get().skyblock.pet.enabled);

        // --- AutoIS ---
        HudManager.register("AutoIS", 10, 10, 1.0f,
                "AutoIS §aEnabled",
                "config.babyzombieaddons.option.autois",
                () -> get().skyblock.autois.enabled);

        // --- Wumpa Record ---
        HudManager.register("WumpaRecord", 10, 300, 1.0f,
                "§b§lWumpa Record\n§a✔ §fStrongarm\n§a✔ §fTepid\n§a✔ §fMantis Shrimp\n§a✔ §fNozzlenose\n§a✔ §fPolaris\n§a✔ §fShuddersquid\n§a✔ §fBillygoat\n§6§lWumpa 可以生成!",
                "config.babyzombieaddons.option.safariWumpaRecord",
                () -> get().hunting.safari.wumpaRecord);

        // --- Safari Hunter Trade ---
        HudManager.register("SafariHunter", 10, 330, 1.0f,
                "§6§l猎手交易\n§eHunter Billy §7@ -50 81 0\n§a 给 Mantis Shrimp Shard  §c要 Yogi Berry",
                "config.babyzombieaddons.option.safariHunterTradeHud",
                () -> get().hunting.safari.hunterTrade.hud);

        // --- Cake Buff ---
        HudManager.register("CakeBuffTracker", 10, 50, 1.0f,
                """
                        §c10\uE010 Health   §a✔
                        §a3\uE008 Defense   §a✔
                        §c2\uE00D Strength   §a✔
                        §f10\uE022 Speed   §c✘
                        §b5\uE003 Intelligence   §c✘
                        §c2\uE00B Ferocity   §c✘
                        §41\uE028 Vitality   §a✔
                        §f1\uE027 True Defense   §c✘
                        §31\uE021 Sea Creature Chance   §c✘
                        §b1\uE01A Magic Find   §a✔
                        §d1\uE013 Pet Luck   §c✘
                        §b1\uE006 Cold Resistance   §c✘
                        §a10\uE020 Rift Time   §a✔
                        §65\uE053 Mining Fortune   §c✘
                        §65\uE051 Farming Fortune   §a✔
                        §65\uE054 Foraging Fortune   §c✘
                        §61\uE025 Treasure Chance   §c✘
                        §d1\uE077 Tracking   §a✔
                        §25\uE023 Sweep   §c✘
                        §d1\uE05B Hunter Fortune   §c✘""",
                "config.babyzombieaddons.option.cakeBuffTracker",
                () -> get().skyblock.cakeBuffTracker);

        // --- Anniversary ---
        HudManager.register("RaffleTasks", 10, 100, 1.0f,
                "§6Raffle Tasks§7: §e15§7/21 remaining\n\n§7Obtain some Diamond Essence.\n§7Find a Suspicious Scrap while in a Glacite Mineshaft.\n§7Fish 30 Times",
                "config.babyzombieaddons.option.raffleTaskTracker",
                () -> get().events.anniversary.raffleTaskTracker);

        // --- Popup ---
        HudManager.register("Popup", 420, 50, 1.0f,
                "§6Party Invite\n§fPlayer invites you to party\n§a[Y] Accept  §e[N] Ignore",
                "config.babyzombieaddons.category.popup",
                () -> get().popup.popupPartyInvite || get().popup.popupGuildPartyInvite
                        || get().popup.popupFriendRequest || get().popup.popupDuelsRequest
                        || get().popup.popupSkyblockTrade || get().popup.popupDungeonRestart
                        || get().fishing.popupBaitLow > 0);
    }

    private static ModConfig get() {
        return ModConfigManager.get();
    }
}
