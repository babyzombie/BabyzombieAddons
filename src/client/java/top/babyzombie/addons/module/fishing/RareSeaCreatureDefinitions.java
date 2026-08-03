package top.babyzombie.addons.module.fishing;

import org.jetbrains.annotations.Nullable;
import top.babyzombie.addons.config.FishingConfig;
import top.babyzombie.addons.util.ChatUtils;

public final class RareSeaCreatureDefinitions {

    public enum MajorCategory {
        WATER,
        LAVA
    }

    public enum Rarity {
        LEGENDARY("§6", 0xFFFFAA00),
        MYTHIC("§d", 0xFFFF55FF);

        public final String titleColorCode;
        public final int beamColorArgb;

        Rarity(String titleColorCode, int beamColorArgb) {
            this.titleColorCode = titleColorCode;
            this.beamColorArgb = beamColorArgb;
        }
    }

    public enum SeaCreature {
        WATER_HYDRA(MajorCategory.WATER, "Default", "Water Hydra", Rarity.LEGENDARY, "The Water Hydra has come to test your strength.") {
            @Override
            public boolean isExcluded(FishingConfig.RareSeaCreatures cfg) {
                return cfg.excludeList.waterSeaCreatures.waterHydra;
            }
        },
        ABYSSAL_MINER(MajorCategory.WATER, "Default", "Abyssal Miner", Rarity.LEGENDARY, "An Abyssal Miner breaks out of the water!") {
            @Override
            public boolean isExcluded(FishingConfig.RareSeaCreatures cfg) {
                return cfg.excludeList.waterSeaCreatures.abyssalMiner;
            }
        },
        ALLIGATOR(MajorCategory.WATER, "Bayou", "Alligator", Rarity.LEGENDARY, "A long snout breaks the surface of the water. It's an Alligator!") {
            @Override
            public boolean isExcluded(FishingConfig.RareSeaCreatures cfg) {
                return cfg.excludeList.waterSeaCreatures.bayou.alligator;
            }
        },
        TITANOBOA(MajorCategory.WATER, "Bayou", "Titanoboa", Rarity.MYTHIC, "A massive Titanoboa surfaces. Its body stretches as far as the eye can see.") {
            @Override
            public boolean isExcluded(FishingConfig.RareSeaCreatures cfg) {
                return cfg.excludeList.waterSeaCreatures.bayou.titanoboa;
            }
        },
        THE_LOCH_EMPEROR(MajorCategory.WATER, "Galatea", "The Loch Emperor", Rarity.LEGENDARY, "The Loch Emperor arises from the depths.") {
            @Override
            public boolean isExcluded(FishingConfig.RareSeaCreatures cfg) {
                return cfg.excludeList.waterSeaCreatures.galatea.theLochEmperor;
            }
        },
        NESSIE(MajorCategory.WATER, "Galatea", "Nessie", Rarity.MYTHIC, "You've caused a disturbance in the loch. Could it be... Nessie?") {
            @Override
            public boolean isExcluded(FishingConfig.RareSeaCreatures cfg) {
                return cfg.excludeList.waterSeaCreatures.galatea.nessie;
            }
        },
        PUDDLE_JUMPER(MajorCategory.WATER, "Lotus", "Puddle Jumper", Rarity.LEGENDARY, "A Puddle Jumper is preparing for liftoff—cast your rod into it and hold on tight!") {
            @Override
            public boolean isExcluded(FishingConfig.RareSeaCreatures cfg) {
                return cfg.excludeList.waterSeaCreatures.lotus.puddleJumper;
            }
        },
        FROG_PRINCE(MajorCategory.WATER, "Lotus", "Frog Prince", Rarity.MYTHIC, "Bow down before the Frog Prince... or pay the hefty price!") {
            @Override
            public boolean isExcluded(FishingConfig.RareSeaCreatures cfg) {
                return cfg.excludeList.waterSeaCreatures.lotus.frogPrince;
            }
        },
        FLIPFLOPPER(MajorCategory.WATER, "Lotus", "Flipflopper", Rarity.LEGENDARY, null) {
            @Override
            public boolean isExcluded(FishingConfig.RareSeaCreatures cfg) {
                return cfg.excludeList.waterSeaCreatures.lotus.flipflopper;
            }
        },
        SEASHINE(MajorCategory.WATER, "Lotus", "Seashine", Rarity.LEGENDARY, null) {
            @Override
            public boolean isExcluded(FishingConfig.RareSeaCreatures cfg) {
                return cfg.excludeList.waterSeaCreatures.lotus.seashine;
            }
        },
        SILKBREEZE(MajorCategory.WATER, "Torrhus", "Silkbreeze", Rarity.LEGENDARY, "Something zips through the air - it's a Silkbreeze!") {
            @Override
            public boolean isExcluded(FishingConfig.RareSeaCreatures cfg) {
                return cfg.excludeList.waterSeaCreatures.torrhus.silkbreeze;
            }
        },
        GIANT_ISOPOD(MajorCategory.WATER, "Torrhus", "Giant Isopod", Rarity.MYTHIC, "A Giant Isopod was dredged up from the depths!") {
            @Override
            public boolean isExcluded(FishingConfig.RareSeaCreatures cfg) {
                return cfg.excludeList.waterSeaCreatures.torrhus.giantIsopod;
            }
        },
        BLUE_RINGED_OCTOPUS(MajorCategory.WATER, "Water Hotspot", "Blue Ringed Octopus", Rarity.LEGENDARY, "A garish set of tentacles arise. It's a Blue Ringed Octopus!") {
            @Override
            public boolean isExcluded(FishingConfig.RareSeaCreatures cfg) {
                return cfg.excludeList.waterSeaCreatures.waterHotspot.blueRingedOctopus;
            }
        },
        WIKI_TIKI(MajorCategory.WATER, "Water Hotspot", "Wiki Tiki", Rarity.MYTHIC, "The water bubbles and froths. A massive form emerges- you have disturbed the Wiki Tiki! You shall pay the price.") {
            @Override
            public boolean isExcluded(FishingConfig.RareSeaCreatures cfg) {
                return cfg.excludeList.waterSeaCreatures.waterHotspot.wikiTiki;
            }
        },
        YETI(MajorCategory.WATER, "Jerry's Workshop", "Yeti", Rarity.LEGENDARY, "What is this creature!?") {
            @Override
            public boolean isExcluded(FishingConfig.RareSeaCreatures cfg) {
                return cfg.excludeList.waterSeaCreatures.jerrysWorkshop.yeti;
            }
        },
        REINDRAKE(MajorCategory.WATER, "Jerry's Workshop", "Reindrake", Rarity.MYTHIC, "A Reindrake forms from the depths.") {
            @Override
            public boolean isExcluded(FishingConfig.RareSeaCreatures cfg) {
                return cfg.excludeList.waterSeaCreatures.jerrysWorkshop.reindrake;
            }
        },
        PHANTOM_FISHER(MajorCategory.WATER, "Spooky Festival", "Phantom Fisher", Rarity.LEGENDARY, "The spirit of a long lost Phantom Fisher has come to haunt you.") {
            @Override
            public boolean isExcluded(FishingConfig.RareSeaCreatures cfg) {
                return cfg.excludeList.waterSeaCreatures.spookyFestival.phantomFisher;
            }
        },
        GRIM_REAPER(MajorCategory.WATER, "Spooky Festival", "Grim Reaper", Rarity.MYTHIC, "This can't be! The manifestation of death himself!") {
            @Override
            public boolean isExcluded(FishingConfig.RareSeaCreatures cfg) {
                return cfg.excludeList.waterSeaCreatures.spookyFestival.grimReaper;
            }
        },
        GREAT_WHITE_SHARK(MajorCategory.WATER, "Fishing Festival", "Great White Shark", Rarity.LEGENDARY, "Hide no longer, a Great White Shark has tracked your scent and thirsts for your blood!") {
            @Override
            public boolean isExcluded(FishingConfig.RareSeaCreatures cfg) {
                return cfg.excludeList.waterSeaCreatures.fishingFestival.greatWhiteShark;
            }
        },
        THUNDER(MajorCategory.LAVA, "Default", "Thunder", Rarity.LEGENDARY, "You hear a massive rumble as Thunder emerges.") {
            @Override
            public boolean isExcluded(FishingConfig.RareSeaCreatures cfg) {
                return cfg.excludeList.lavaSeaCreatures.thunder;
            }
        },
        LORD_JAWBUS(MajorCategory.LAVA, "Default", "Lord Jawbus", Rarity.MYTHIC, "You have angered a legendary creature... Lord Jawbus has arrived.") {
            @Override
            public boolean isExcluded(FishingConfig.RareSeaCreatures cfg) {
                return cfg.excludeList.lavaSeaCreatures.lordJawbus;
            }
        },
        PLHLEGBLAST(MajorCategory.LAVA, "Default", "Plhlegblast", Rarity.MYTHIC, "WOAH! A Plhlegblast appeared.") {
            @Override
            public boolean isExcluded(FishingConfig.RareSeaCreatures cfg) {
                return cfg.excludeList.lavaSeaCreatures.plhlegblast;
            }
        },
        FIERY_SCUTTLER(MajorCategory.LAVA, "Lava Hotspot", "Fiery Scuttler", Rarity.LEGENDARY, "A Fiery Scuttler inconspicuously waddles up to you, friends in tow.") {
            @Override
            public boolean isExcluded(FishingConfig.RareSeaCreatures cfg) {
                return cfg.excludeList.lavaSeaCreatures.lavaHotspot.fieryScuttler;
            }
        },
        RAGNAROK(MajorCategory.LAVA, "Lava Hotspot", "Ragnarok", Rarity.MYTHIC, "The sky darkens and the air thickens. The end times are upon us: Ragnarok is here.") {
            @Override
            public boolean isExcluded(FishingConfig.RareSeaCreatures cfg) {
                return cfg.excludeList.lavaSeaCreatures.lavaHotspot.ragnarok;
            }
        };

        public final MajorCategory majorCategory;
        public final String subCategory;
        public final String displayName;
        public final Rarity rarity;
        @Nullable
        public final String catchMessage;

        SeaCreature(MajorCategory majorCategory, String subCategory, String displayName, Rarity rarity, @Nullable String catchMessage) {
            this.majorCategory = majorCategory;
            this.subCategory = subCategory;
            this.displayName = displayName;
            this.rarity = rarity;
            this.catchMessage = catchMessage;
        }

        public abstract boolean isExcluded(FishingConfig.RareSeaCreatures cfg);
    }

    public static final String UNKNOWN_TITLE_COLOR_CODE = "§4";
    public static final int UNKNOWN_BEAM_COLOR = 0xFFAA0000;

    private RareSeaCreatureDefinitions() {}

    private static String stripNonAlphaNumericEdges(String s) {
        int start = 0;
        int end = s.length();
        while (start < end) {
            int cp = s.codePointAt(start);
            if (Character.isLetterOrDigit(cp)) break;
            start += Character.charCount(cp);
        }
        while (start < end) {
            int cp = s.codePointBefore(end);
            if (Character.isLetterOrDigit(cp)) break;
            end -= Character.charCount(cp);
        }
        return s.substring(start, end);
    }

    public static String cleanNameForMatch(String rawName) {
        String s = ChatUtils.removeEmoji(rawName);
        // §k（乱码）格式码后的单个字符是乱码占位字符，如腐化海怪名 "§5§ka§5Corrupted Puddle Jumper§5§ka"，
        // 连格式码一起剥离，否则会残留占位字符导致无法匹配
        s = s.replaceAll("§k(?![§&]).", "");
        s = ChatUtils.stripColor(s);
        s = s.replaceAll("\\[[Ll][Vv]\\s*\\d+\\]", "");
        s = s.replaceAll("[\\d,./]+[kKmMbB]?", "").replace("❤", "");
        s = s.replaceAll(" {2,}", " ").trim();
        s = stripNonAlphaNumericEdges(s).replaceAll(" {2,}", " ").trim();
        return s;
    }

    public static @Nullable SeaCreature match(String rawName) {
        String cleaned = cleanNameForMatch(rawName);
        if (cleaned.isEmpty()) return null;
        // 腐化海怪（名字带 Corrupted 前缀，如 "Corrupted Puddle Jumper"）是普通稀有海怪的腐化版，按对应定义匹配
        String base = cleaned.replaceFirst("(?i)^Corrupted\\s*", "");
        for (SeaCreature sc : SeaCreature.values()) {
            if (base.equalsIgnoreCase(sc.displayName)) return sc;
        }
        return null;
    }

    public static @Nullable SeaCreature matchByCatchMessage(String strippedChat) {
        if (strippedChat == null || strippedChat.isEmpty()) return null;
        for (SeaCreature sc : SeaCreature.values()) {
            if (sc.catchMessage == null) continue;
            if (strippedChat.equalsIgnoreCase(sc.catchMessage)) return sc;
        }
        return null;
    }
}
