package top.babyzombie.addons.module.playcmd;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import top.babyzombie.addons.util.ChatUtils;

/**
 * GUI for Hypixel /play command with all game modes categorized.
 */
public final class PlayCmdModule {
    private PlayCmdModule() {}
    public static void init() {}

    public static void openGUI() {
        Minecraft.getInstance().execute(() -> Minecraft.getInstance().setScreen(new PlayScreen()));
    }

    private static class PlayScreen extends Screen {
        private int scroll;
        private static final int BTN_W = 130, BTN_H = 20, GAP = 4, COLS = 6;
        private static final int COL_W = BTN_W + GAP;

        protected PlayScreen() { super(Component.literal("Play")); }

        @Override
        protected void init() {
            int y = 30;
            y = row(y, "SkyBlock","/play skyblock", "Housing","/hub housing",
                     "Pit","/play pit", null,null, null,null);
            y += 4;

            cat(y, "§cBedWars"); y += 12;
            y = row(y, "Solo","/play bedwars_eight_one", "Doubles","/play bedwars_eight_two",
                     "3v3v3v3","/play bedwars_four_three", "4v4v4v4","/play bedwars_four_four", "4v4","/play bedwars_two_four");
            y = row(y, "Castle 40v40","/play bedwars_castle", "Lucky Doubles","/play bedwars_eight_two_lucky",
                     "Lucky 4s","/play bedwars_four_four_lucky", "Armed Doubles","/play bedwars_eight_two_armed",
                     "Armed 4s","/play bedwars_four_four_armed");
            y = row(y, "Ult Doubles","/play bedwars_eight_two_ultimate", "Ult 4s","/play bedwars_four_four_ultimate",
                     "Rush Dbls","/play bedwars_eight_two_rush", "Rush 4s","/play bedwars_four_four_rush",
                     "Voidless Dbl","/play bedwars_eight_two_voidless");
            y = row(y, "Voidless 4s","/play bedwars_four_four_voidless", "Swap Dbls","/play bedwars_eight_two_swap",
                     "Swap 4s","/play bedwars_four_four_swap", null,null, null,null);
            y += 4;

            cat(y, "§bSkyWars"); y += 12;
            y = row(y, "Solo Normal","/play solo_normal", "Solo Insane","/play solo_insane",
                     "Teams Normal","/play teams_normal", "Teams Insane","/play teams_insane",
                     "Lucky Solo","/play solo_insane_lucky");
            y = row(y, "Lucky Teams","/play teams_insane_lucky", null,null, null,null, null,null, null,null);
            y += 4;

            cat(y, "§dMurder Mystery"); y += 12;
            y = row(y, "Classic","/play murder_classic", "Double Up","/play murder_double_up",
                     "Assassins","/play murder_assassins", "Infection","/play murder_infection", null,null);
            y += 4;

            cat(y, "§eArcade"); y += 12;
            y = row(y, "Party Games","/play arcade_party_games_1", "Pixel Party","/play arcade_pixel_party",
                     "Dropper","/play arcade_dropper", "Farm Hunt","/play arcade_farm_hunt",
                     "Blocking Dead","/play arcade_day_one");
            y = row(y, "Hypixel Says","/play arcade_simon_says", "Mini Walls","/play arcade_mini_walls",
                     "HoleInWall","/play arcade_hole_in_the_wall", "PixelPainters","/play arcade_pixel_painters",
                     "Prop Hunt","/play arcade_hide_and_seek_prop_hunt");
            y = row(y, "PartyPooper","/play arcade_hide_and_seek_party_pooper",
                     "BountyHunters","/play arcade_bounty_hunters", "DragonWars","/play arcade_dragon_wars",
                     "EnderSpleef","/play arcade_ender_spleef", "GalaxyWars","/play arcade_starwars");
            y = row(y, "Throw Out","/play arcade_throw_out", "Football","/play arcade_soccer",
                     "Zombies DE","/play arcade_zombies_dead_end", "Zombies BB","/play arcade_zombies_bad_blood",
                     "Zombies AA","/play arcade_zombies_alien_arcadium");
            y = row(y, "Zombies Prison","/play arcade_zombies_prison", null,null, null,null, null,null, null,null);
            y += 4;

            cat(y, "§7TNT Games"); y += 12;
            y = row(y, "TNT Run","/play tnt_tntrun", "PVP Run","/play tnt_pvprun",
                     "Bow Spleef","/play tnt_bowspleef", "TNT Tag","/play tnt_tntag",
                     "Wizards","/play tnt_capture");
            y += 4;

            cat(y, "§fWool Games"); y += 12;
            y = row(y, "Wool Wars","/play wool_wool_wars_two_four", "CTW","/play wool_capture_the_wool_two_twenty",
                     "SheepWars","/play wool_sheep_wars_two_six", null,null, null,null);
            y += 4;

            cat(y, "§aBuild Battle"); y += 12;
            y = row(y, "Solo","/play build_battle_solo_normal", "Teams","/play build_battle_teams_normal",
                     "Pro","/play build_battle_solo_pro", "GTB","/play build_battle_guess_the_build", null,null);
            y += 4;

            cat(y, "§9Mega Walls"); y += 12;
            y = row(y, "Standard","/play mw_standard", "Face Off","/play mw_face_off", null,null, null,null, null,null);
            y += 4;

            cat(y, "§aUHC"); y += 12;
            y = row(y, "Solo","/play uhc_solo", "Teams","/play uhc_teams",
                     "Speed Solo","/play speed_solo_normal", "Speed Teams","/play speed_team_normal", null,null);
            y += 4;

            cat(y, "§6Blitz SG"); y += 12;
            y = row(y, "Solo","/play blitz_solo_normal", "Teams","/play blitz_teams_normal",
                     null,null, null,null, null,null);
            y += 4;

            cat(y, "§7Cops & Crims"); y += 12;
            y = row(y, "Defusal","/play mcgo_normal", "Deathmatch","/play mcgo_deathmatch",
                     "Gun Game","/play mcgo_gungame", null,null, null,null);
            y += 4;

            cat(y, "§cWarlords"); y += 12;
            y = row(y, "CTF","/play warlords_ctf_mini", "TDM","/play warlords_team_deathmatch",
                     "Domination","/play warlords_domination", null,null, null,null);
            y += 4;

            cat(y, "§aSmash Heroes"); y += 12;
            y = row(y, "Solo","/play super_smash_solo_normal", "Teams","/play super_smash_teams_normal",
                     "1v1","/play super_smash_1v1_normal", "2v2","/play super_smash_2v2_normal",
                     "Friends","/play super_smash_friends_normal");
            y += 4;

            cat(y, "§eClassic"); y += 12;
            y = row(y, "TKR","/play tkr", "VampireZ","/play vampirez",
                     "Paintball","/play paintball", "The Walls","/play walls", "Quake Solo","/play quake_solo");
            y = row(y, "Quake Teams","/play quake_teams", "Arena 1v1","/play arena_1v1",
                     "Arena 2v2","/play arena_2v2", "Arena 4v4","/play arena_4v4", null,null);
        }

        private void cat(int y, String text) {
            addRenderableWidget(Button.builder(Component.literal(text), b -> {}).bounds(4, y, width-8, 12).build());
        }

        private int row(int y, String... namesAndCmds) {
            for (int i = 0; i < namesAndCmds.length; i += 2) {
                if (namesAndCmds[i] == null) continue;
                int col = i / 2;
                final String cmd = namesAndCmds[i + 1];
                String name = namesAndCmds[i];
                addRenderableWidget(Button.builder(Component.literal(name), b -> {
                    ChatUtils.sendCommand(cmd); onClose();
                }).bounds(4 + col * COL_W, y, BTN_W, BTN_H).build());
            }
            return y + BTN_H + GAP;
        }

        @Override
        public void render(GuiGraphics g, int mx, int my, float d) {
            renderBackground(g, mx, my, d);
            super.render(g, mx, my, d);
        }

        @Override public boolean isPauseScreen() { return false; }

        @Override public boolean mouseScrolled(double mx, double my, double sx, double sy) {
            scroll = (int)Math.max(0, scroll - sy * 20);
            return true;
        }
    }
}
