package top.babyzombie.addons.module.playcmd;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import top.babyzombie.addons.config.ModConfigManager;
import top.babyzombie.addons.event.SendCommandEvents;
import top.babyzombie.addons.util.ChatUtils;
import top.babyzombie.addons.util.tracker.HypixelLocationTracker;

public final class PlayCmdModule {
    private PlayCmdModule() {}

    public static void init() {
        PlayAutocomplete.init();

        SendCommandEvents.BEFORE_SEND.register(command -> {
            if (command.trim().equals("play") && ModConfigManager.get().general.playCmd
                    && HypixelLocationTracker.getInstance().isOnHypixel()) {
                openGUI();
                return true;
            }
            return false;
        });
    }

    public static boolean isPlayCmdEnabled() {
        return ModConfigManager.get().general.playCmd;
    }

    public static void openGUI() {
        Minecraft.getInstance().execute(() -> Minecraft.getInstance().setScreen(new PlayScreen()));
    }

    // Each game: {en, cmd, zh}
    static final Object[][][] GAMES = {
        {{"Main","主分类"}, {"SkyBlock","/play skyblock","SkyBlock"}, {"Housing","/hub housing","Housing (家园)"}, {"Pit","/play pit","Pit (天坑乱斗)"}},
        {{"BedWars","起床战争"},
         {"Solo","/play bedwars_eight_one","单挑"},{"Doubles","/play bedwars_eight_two","双人"},{"3v3v3v3","/play bedwars_four_three"},
         {"4v4v4v4","/play bedwars_four_four"},{"4v4","/play bedwars_two_four"},{"Castle 40v40","/play bedwars_castle","城池攻防战"},
         {"Lucky Doubles","/play bedwars_eight_two_lucky","幸运方块 双人"},{"Lucky 4v4v4v4","/play bedwars_four_four_lucky","幸运方块 4v4v4v4"},
         {"Armed Doubles","/play bedwars_eight_two_armed","枪械 双人"},{"Armed 4v4v4v4","/play bedwars_four_four_armed","枪械 4v4v4v4"},
         {"Ultimate Doubles","/play bedwars_eight_two_ultimate","超能力 双人"},{"Ultimate 4v4v4v4","/play bedwars_four_four_ultimate","超能力 4v4v4v4"},
         {"Rush Doubles","/play bedwars_eight_two_rush","极速 双人"},{"Rush 4v4v4v4","/play bedwars_four_four_rush","极速 4v4v4v4"},
         {"Voidless Doubles","/play bedwars_eight_two_voidless","无虚空 双人"},{"Voidless 4v4v4v4","/play bedwars_four_four_voidless","无虚空 4v4v4v4"},
         {"Swap Doubles","/play bedwars_eight_two_swap","交换 双人"},{"Swap 4v4v4v4","/play bedwars_four_four_swap","交换 4v4v4v4"}},
        {{"SkyWars","空岛战争"},
         {"Solo Normal","/play solo_normal","普通 单人"},{"Solo Insane","/play solo_insane","疯狂 单人"},{"Teams Normal","/play teams_normal","普通 双人"},
         {"Teams Insane","/play teams_insane","疯狂 双人"},{"Lucky Solo","/play solo_insane_lucky","幸运方块 单人"},{"Lucky Teams","/play teams_insane_lucky","幸运方块 双人"}},
        {{"Murder Mystery","密室杀手"},
         {"Classic","/play murder_classic","经典模式"},{"Double Up","/play murder_double_up","双倍模式"},
         {"Assassins","/play murder_assassins","刺客模式"},{"Infection","/play murder_infection","感染模式"}},
        {{"Arcade","街机游戏"},
         {"Party Games","/play arcade_party_games_1","派对游戏"},{"Pixel Party","/play arcade_pixel_party","像素派对"},
         {"Dropper","/play arcade_dropper","心跳水立方"},{"Farm Hunt","/play arcade_farm_hunt","农场躲猫猫"},
         {"Blocking Dead","/play arcade_day_one","行尸走肉"},{"Hypixel Says","/play arcade_simon_says","我说你做"},
         {"Mini Walls","/play arcade_mini_walls","迷你战墙"},{"Hole In The Wall","/play arcade_hole_in_the_wall","人体打印机"},
         {"Pixel Painters","/play arcade_pixel_painters","像素画师"},{"Prop Hunt","/play arcade_hide_and_seek_prop_hunt","道具躲猫猫"},
         {"Party Pooper","/play arcade_hide_and_seek_party_pooper","派对躲猫猫"},{"Bounty Hunters","/play arcade_bounty_hunters","赏金猎人"},
         {"Dragon Wars","/play arcade_dragon_wars","龙之战"},{"Ender Spleef","/play arcade_ender_spleef","末影掘战"},
         {"Galaxy Wars","/play arcade_starwars","星际战争"},{"Throw Out","/play arcade_throw_out","乱棍之战"},
         {"Football","/play arcade_soccer","足球"},{"Zombies Dead End","/play arcade_zombies_dead_end","僵尸末日 穷途末路"},
         {"Zombies Bad Blood","/play arcade_zombies_bad_blood","僵尸末日 坏血之宫"},{"Zombies Alien Arcadium","/play arcade_zombies_alien_arcadium","僵尸末日 外星游乐园"},
         {"Zombies Prison","/play arcade_zombies_prison","僵尸末日 监狱"}},
        {{"TNT Games","TNT游戏"},
         {"TNT Run","/play tnt_tntrun","方块掘战"},{"PVP Run","/play tnt_pvprun","PVP方块掘战"},{"Bow Spleef","/play tnt_bowspleef","掘一死箭"},
         {"TNT Tag","/play tnt_tntag","烫手TNT"},{"Wizards","/play tnt_capture","法师掘战"}},
        {{"Wool Games","羊毛游戏"},
         {"Wool Wars","/play wool_wool_wars_two_four","羊毛战争"},{"Capture The Wool","/play wool_capture_the_wool_two_twenty","捕捉羊毛大作战"},
         {"SheepWars","/play wool_sheep_wars_two_six","羊战争"}},
        {{"Build Battle","建筑大师"},
         {"Solo","/play build_battle_solo_normal","单人"},{"Teams","/play build_battle_teams_normal","团队"},
         {"Pro","/play build_battle_solo_pro","大师"},{"Guess The Build","/play build_battle_guess_the_build","建筑猜猜乐"}},
        {{"Mega Walls","超级战墙"}, {"Standard","/play mw_standard","标准"},{"Face Off","/play mw_face_off","对决"}},
        {{"UHC","极限生存冠军"}, {"Solo","/play uhc_solo","单人"},{"Teams","/play uhc_teams","三人"},
         {"Speed Solo","/play speed_solo_normal","单人极速"},{"Speed Teams","/play speed_team_normal","三人极速"}},
        {{"Blitz SG","闪电饥饿游戏"}, {"Solo","/play blitz_solo_normal","单挑模式"},{"Teams","/play blitz_teams_normal","组队模式"}},
        {{"Cops & Crims","警匪大战"}, {"Defusal","/play mcgo_normal","爆破模式"},{"Deathmatch","/play mcgo_deathmatch","团队死斗"},{"Gun Game","/play mcgo_gungame","Gun Game"}},
        {{"Warlords","战争领主"}, {"Capture The Flag","/play warlords_ctf_mini","夺旗"},{"TDM","/play warlords_team_deathmatch","团队死斗"},{"Domination","/play warlords_domination","占点"}},
        {{"Smash Heroes","星碎英雄"},
         {"Solo","/play super_smash_solo_normal","单挑"},{"Teams","/play super_smash_teams_normal","团队"},
         {"1v1","/play super_smash_1v1_normal"},{"2v2","/play super_smash_2v2_normal"},{"Friends","/play super_smash_friends_normal","好友 单挑"}},
        {{"Classic","经典游戏"},
         {"TKR","/play tkr","方块赛车"},{"VampireZ","/play vampirez","吸血鬼"},{"Paintball","/play paintball","彩蛋射击"},
         {"The Walls","/play walls","战墙"},{"Quake Solo","/play quake_solo","未来射击 单挑"},{"Quake Teams","/play quake_teams","未来射击 团队"},
         {"Arena 1v1","/play arena_1v1","竞技场 1v1"},{"Arena 2v2","/play arena_2v2","竞技场 2v2"},{"Arena 4v4","/play arena_4v4","竞技场 4v4"}},
    };

    private static class PlayScreen extends Screen {
        private double scroll;
        private final int PAD = 6, BTN_W = 130, BTN_H = 20, GAP = 4;
        private String hoverCmd;
        private int maxScroll;

        protected PlayScreen() { super(Component.translatable("playcmd.title")); }

        private int cols() { return Math.max(3, Math.min(6, (width - PAD*2 - 8) / (BTN_W + GAP))); }
        private int btnW() { return Math.min(BTN_W, (width - PAD*2 - 8 - GAP * (cols() - 1)) / cols()); }

        private boolean isChinese() {
            return Minecraft.getInstance().getLanguageManager().getSelected().equals("zh_cn");
        }

        private String catColor(Object name) {
            return switch ((String)name) {
                case "Main" -> "§e§l";
                case "BedWars" -> "§c§l";
                case "SkyWars" -> "§b§l";
                case "Murder Mystery" -> "§d§l";
                case "Arcade" -> "§e§l";
                case "TNT Games" -> "§7§l";
                case "Wool Games" -> "§f§l";
                case "Build Battle" -> "§a§l";
                case "Mega Walls" -> "§9§l";
                case "UHC" -> "§a§l";
                case "Blitz SG" -> "§6§l";
                case "Cops & Crims" -> "§7§l";
                case "Warlords" -> "§c§l";
                case "Smash Heroes" -> "§a§l";
                case "Classic" -> "§e§l";
                default -> "§f";
            };
        }

        private String loc(Object[] entry, int enIdx, int zhIdx) {
            if (zhIdx < entry.length && isChinese()) return (String)entry[zhIdx];
            return (String)entry[enIdx];
        }

        @Override
        public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float d) {
            g.fill(0, 0, width, height, 0xC0101010);
            int sy = (int)scroll;
            g.pose().pushMatrix();
            g.pose().translate(0f, (float)-sy);
            my += sy;
            hoverCmd = null;

            int y = 30;
            int colN = cols(), bw = btnW();
            for (Object[][] cat : GAMES) {
                // Category label
                String color = catColor(cat[0][0]);
                String catName = isChinese() && cat[0].length > 1 ? (String)cat[0][1] : (String)cat[0][0];
                g.text(font, color + catName, PAD, y, 0xFFFFFFFF);
                y += 14;
                int rowStartY = y;
                for (int i = 1; i < cat.length; i++) {
                    int col = (i - 1) % colN;
                    int row = (i - 1) / colN;
                    int bx = PAD + col * (bw + GAP);
                    int by = rowStartY + row * (BTN_H + GAP);
                    g.fill(bx, by, bx + bw, by + BTN_H, 0x40A0A0A0);
                    String label = isChinese() && cat[i].length > 2 ? (String)cat[i][2] : (String)cat[i][0];
                    if (mx >= bx && mx <= bx + bw && my >= by && my <= by + BTN_H) {
                        g.fill(bx, by, bx + bw, by + BTN_H, 0x60FFAA00);
                        hoverCmd = (String)cat[i][1];
                    }
                    g.centeredText(font, label, bx + bw / 2, by + (BTN_H - 8) / 2, 0xFFFFFFFF);
                }
                int rows = (cat.length - 1 + colN - 1) / colN;
                y = rowStartY + rows * (BTN_H + GAP) + 8;
            }
            g.pose().popMatrix();

            int contentH = y;
            int barH = Math.max(30, height * height / Math.max(contentH, 1));
            int barY = (int)(scroll / Math.max(contentH - height, 1) * (height - barH));
            maxScroll = Math.max(0, contentH - height);
            g.fill(width - 6, 0, width, height, 0x30FFFFFF);
            g.fill(width - 6, barY, width, barY + barH, 0x90FFFFFF);
        }

        @Override public boolean isPauseScreen() { return false; }

        @Override
        public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
            if (hoverCmd != null) { ChatUtils.sendCommand(hoverCmd); onClose(); return true; }
            return super.mouseClicked(event, doubleClick);
        }

        @Override public boolean mouseScrolled(double mx, double my, double sx, double sy) {
            scroll = Math.max(0, Math.min(scroll - sy * 20, maxScroll));
            return true;
        }
    }
}
