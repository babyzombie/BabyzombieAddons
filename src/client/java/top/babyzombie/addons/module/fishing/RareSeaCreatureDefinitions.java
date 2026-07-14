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
        WATER_HYDRA(MajorCategory.WATER, "Default", "Water Hydra", Rarity.LEGENDARY) {
            @Override
            public boolean isExcluded(FishingConfig.RareSeaCreatures cfg) {
                return cfg.excludeList.waterSeaCreatures.waterHydra;
            }
        },
        ABYSSAL_MINER(MajorCategory.WATER, "Default", "Abyssal Miner", Rarity.LEGENDARY) {
            @Override
            public boolean isExcluded(FishingConfig.RareSeaCreatures cfg) {
                return cfg.excludeList.waterSeaCreatures.abyssalMiner;
            }
        },
        ALLIGATOR(MajorCategory.WATER, "Bayou", "Alligator", Rarity.LEGENDARY) {
            @Override
            public boolean isExcluded(FishingConfig.RareSeaCreatures cfg) {
                return cfg.excludeList.waterSeaCreatures.bayou.alligator;
            }
        },
        TITANOBOA(MajorCategory.WATER, "Bayou", "Titanoboa", Rarity.MYTHIC) {
            @Override
            public boolean isExcluded(FishingConfig.RareSeaCreatures cfg) {
                return cfg.excludeList.waterSeaCreatures.bayou.titanoboa;
            }
        },
        THE_LOCH_EMPEROR(MajorCategory.WATER, "Galatea", "The Loch Emperor", Rarity.LEGENDARY) {
            @Override
            public boolean isExcluded(FishingConfig.RareSeaCreatures cfg) {
                return cfg.excludeList.waterSeaCreatures.galatea.theLochEmperor;
            }
        },
        NESSIE(MajorCategory.WATER, "Galatea", "Nessie", Rarity.MYTHIC) {
            @Override
            public boolean isExcluded(FishingConfig.RareSeaCreatures cfg) {
                return cfg.excludeList.waterSeaCreatures.galatea.nessie;
            }
        },
        PUDDLE_JUMPER(MajorCategory.WATER, "Lotus", "Puddle Jumper", Rarity.LEGENDARY) {
            @Override
            public boolean isExcluded(FishingConfig.RareSeaCreatures cfg) {
                return cfg.excludeList.waterSeaCreatures.lotus.puddleJumper;
            }
        },
        FROG_PRINCE(MajorCategory.WATER, "Lotus", "Frog Prince", Rarity.MYTHIC) {
            @Override
            public boolean isExcluded(FishingConfig.RareSeaCreatures cfg) {
                return cfg.excludeList.waterSeaCreatures.lotus.frogPrince;
            }
        },
        SILKBREEZE(MajorCategory.WATER, "Torrhus", "Silkbreeze", Rarity.LEGENDARY) {
            @Override
            public boolean isExcluded(FishingConfig.RareSeaCreatures cfg) {
                return cfg.excludeList.waterSeaCreatures.torrhus.silkbreeze;
            }
        },
        GIANT_ISOPOD(MajorCategory.WATER, "Torrhus", "Giant Isopod", Rarity.MYTHIC) {
            @Override
            public boolean isExcluded(FishingConfig.RareSeaCreatures cfg) {
                return cfg.excludeList.waterSeaCreatures.torrhus.giantIsopod;
            }
        },
        BLUE_RINGED_OCTOPUS(MajorCategory.WATER, "Water Hotspot", "Blue Ringed Octopus", Rarity.LEGENDARY) {
            @Override
            public boolean isExcluded(FishingConfig.RareSeaCreatures cfg) {
                return cfg.excludeList.waterSeaCreatures.waterHotspot.blueRingedOctopus;
            }
        },
        WIKI_TIKI(MajorCategory.WATER, "Water Hotspot", "Wiki Tiki", Rarity.MYTHIC) {
            @Override
            public boolean isExcluded(FishingConfig.RareSeaCreatures cfg) {
                return cfg.excludeList.waterSeaCreatures.waterHotspot.wikiTiki;
            }
        },
        YETI(MajorCategory.WATER, "Jerry's Workshop", "Yeti", Rarity.LEGENDARY) {
            @Override
            public boolean isExcluded(FishingConfig.RareSeaCreatures cfg) {
                return cfg.excludeList.waterSeaCreatures.jerrysWorkshop.yeti;
            }
        },
        REINDRAKE(MajorCategory.WATER, "Jerry's Workshop", "Reindrake", Rarity.MYTHIC) {
            @Override
            public boolean isExcluded(FishingConfig.RareSeaCreatures cfg) {
                return cfg.excludeList.waterSeaCreatures.jerrysWorkshop.reindrake;
            }
        },
        PHANTOM_FISHER(MajorCategory.WATER, "Spooky Festival", "Phantom Fisher", Rarity.LEGENDARY) {
            @Override
            public boolean isExcluded(FishingConfig.RareSeaCreatures cfg) {
                return cfg.excludeList.waterSeaCreatures.spookyFestival.phantomFisher;
            }
        },
        GRIM_REAPER(MajorCategory.WATER, "Spooky Festival", "Grim Reaper", Rarity.MYTHIC) {
            @Override
            public boolean isExcluded(FishingConfig.RareSeaCreatures cfg) {
                return cfg.excludeList.waterSeaCreatures.spookyFestival.grimReaper;
            }
        },
        GREAT_WHITE_SHARK(MajorCategory.WATER, "Fishing Festival", "Great White Shark", Rarity.LEGENDARY) {
            @Override
            public boolean isExcluded(FishingConfig.RareSeaCreatures cfg) {
                return cfg.excludeList.waterSeaCreatures.fishingFestival.greatWhiteShark;
            }
        },
        THUNDER(MajorCategory.LAVA, "Default", "Thunder", Rarity.LEGENDARY) {
            @Override
            public boolean isExcluded(FishingConfig.RareSeaCreatures cfg) {
                return cfg.excludeList.lavaSeaCreatures.thunder;
            }
        },
        LORD_JAWBUS(MajorCategory.LAVA, "Default", "Lord Jawbus", Rarity.MYTHIC) {
            @Override
            public boolean isExcluded(FishingConfig.RareSeaCreatures cfg) {
                return cfg.excludeList.lavaSeaCreatures.lordJawbus;
            }
        },
        PLHLEGBLAST(MajorCategory.LAVA, "Default", "Plhlegblast", Rarity.MYTHIC) {
            @Override
            public boolean isExcluded(FishingConfig.RareSeaCreatures cfg) {
                return cfg.excludeList.lavaSeaCreatures.plhlegblast;
            }
        },
        FIERY_SCUTTLER(MajorCategory.LAVA, "Lava Hotspot", "Fiery Scuttler", Rarity.LEGENDARY) {
            @Override
            public boolean isExcluded(FishingConfig.RareSeaCreatures cfg) {
                return cfg.excludeList.lavaSeaCreatures.lavaHotspot.fieryScuttler;
            }
        },
        RAGNAROK(MajorCategory.LAVA, "Lava Hotspot", "Ragnarok", Rarity.MYTHIC) {
            @Override
            public boolean isExcluded(FishingConfig.RareSeaCreatures cfg) {
                return cfg.excludeList.lavaSeaCreatures.lavaHotspot.ragnarok;
            }
        };

        public final MajorCategory majorCategory;
        public final String subCategory;
        public final String displayName;
        public final Rarity rarity;

        SeaCreature(MajorCategory majorCategory, String subCategory, String displayName, Rarity rarity) {
            this.majorCategory = majorCategory;
            this.subCategory = subCategory;
            this.displayName = displayName;
            this.rarity = rarity;
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
        for (SeaCreature sc : SeaCreature.values()) {
            if (cleaned.equalsIgnoreCase(sc.displayName)) return sc;
        }
        return null;
    }
}
