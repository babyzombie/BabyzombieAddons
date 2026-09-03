package top.babyzombie.addons.config;
import com.google.gson.annotations.Expose;

import io.github.notenoughupdates.moulconfig.annotations.Accordion;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorButton;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDraggableList;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDropdown;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorKeybind;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorSlider;
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption;
import io.github.notenoughupdates.moulconfig.annotations.SearchTag;
import net.minecraft.client.Minecraft;
import com.mojang.blaze3d.platform.InputConstants;
import top.babyzombie.addons.config.ModConfig.*;
import top.babyzombie.addons.module.misc.abiphone.CustomRingtoneModule;
import top.babyzombie.addons.module.misc.raredrop.RareDropScreen;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class SkyblockConfig {

    @Expose @ConfigOption(name = "config.babyzombieaddons.option.abiphoneGui", desc = "config.babyzombieaddons.option.abiphoneGui.desc") @ConfigEditorBoolean @SearchTag("abiphone")
    public boolean abiphoneGui = false;

    @Expose @ConfigOption(name = "config.babyzombieaddons.option.fallSoundVolume", desc = "config.babyzombieaddons.option.fallSoundVolume.desc") @ConfigEditorSlider(minValue = 0.0f, maxValue = 1.0f, minStep = 0.01f) @SearchTag("fall") @SearchTag("sound") @SearchTag("volume")
    public float fallSoundVolume = 1.0f;

    @Expose @ConfigOption(name = "config.babyzombieaddons.option.bzGetFromSacks", desc = "config.babyzombieaddons.option.bzGetFromSacks.desc") @ConfigEditorDropdown @SearchTag("bazaar")
    public BzGetFromSacksMode bzGetFromSacks = BzGetFromSacksMode.OFF;

    @ConfigOption(name = "config.babyzombieaddons.option.raredropManage", desc = "config.babyzombieaddons.option.raredropManage.desc") @ConfigEditorButton(buttonText = "OPEN") @SearchTag("raredrop") @SearchTag("drop")
    public transient Runnable raredropManage = () -> Minecraft.getInstance().gui.setScreen(new RareDropScreen(Minecraft.getInstance().gui.screen()));

    @Expose @ConfigOption(name = "config.babyzombieaddons.option.cakeBuffTracker", desc = "config.babyzombieaddons.option.cakeBuffTracker.desc") @ConfigEditorBoolean @SearchTag("cake")
    public boolean cakeBuffTracker = false;
    @Expose @ConfigOption(name = "config.babyzombieaddons.option.minionCollectAutoClose", desc = "config.babyzombieaddons.option.minionCollectAutoClose.desc") @ConfigEditorBoolean @SearchTag("minion")
    public boolean minionCollectAutoClose = false;
    @Expose @ConfigOption(name = "config.babyzombieaddons.option.showInvisibleNameTags", desc = "config.babyzombieaddons.option.showInvisibleNameTags.desc") @ConfigEditorBoolean @SearchTag("invisible") @SearchTag("nametag")
    public boolean showInvisibleNameTags = false;
    @Expose @ConfigOption(name = "config.babyzombieaddons.option.disableBlockPlacePrediction", desc = "config.babyzombieaddons.option.disableBlockPlacePrediction.desc") @ConfigEditorBoolean @SearchTag("block") @SearchTag("place") @SearchTag("prediction")
    public boolean disableBlockPlacePrediction = false;

    @Expose @ConfigOption(name = "config.babyzombieaddons.group.autois", desc = "") @Accordion
    public AutoIS autois = new AutoIS();

    @Expose @ConfigOption(name = "config.babyzombieaddons.group.pet", desc = "") @Accordion
    public Pet pet = new Pet();

    @Expose @ConfigOption(name = "config.babyzombieaddons.group.necronBlade", desc = "") @Accordion
    public NecronBlade necronBlade = new NecronBlade();

    @Expose @ConfigOption(name = "config.babyzombieaddons.group.teleportSword", desc = "") @Accordion
    public TeleportSword teleportSword = new TeleportSword();

    @Expose @ConfigOption(name = "config.babyzombieaddons.group.loadout", desc = "") @Accordion
    public Loadout loadout = new Loadout();

    @Expose @ConfigOption(name = "config.babyzombieaddons.group.customRingtone", desc = "") @Accordion
    public CustomRingtone customRingtone = new CustomRingtone();

    @Expose @ConfigOption(name = "config.babyzombieaddons.group.bazzarTopOrders", desc = "") @Accordion
    public BazzarTopOrders bazzarTopOrders = new BazzarTopOrders();

    @Expose @ConfigOption(name = "config.babyzombieaddons.group.auctionQuickSell", desc = "") @Accordion
    public AuctionQuickSell auctionQuickSell = new AuctionQuickSell();

    public static class AutoIS {
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.autois", desc = "config.babyzombieaddons.option.autois.desc") @ConfigEditorBoolean @SearchTag("autois") @SearchTag("island")
        public boolean enabled = false;
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.autoisDelay", desc = "config.babyzombieaddons.option.autoisDelay.desc") @ConfigEditorSlider(minValue = 5, maxValue = 125, minStep = 1) @SearchTag("autois") @SearchTag("delay")
        public int delay = 5;
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.autoisDest", desc = "config.babyzombieaddons.option.autoisDest.desc") @ConfigEditorDropdown @SearchTag("autois") @SearchTag("destination")
        public AutoISDest dest = AutoISDest.ISLAND;
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.hideEntities", desc = "config.babyzombieaddons.option.hideEntities.desc") @ConfigEditorBoolean @SearchTag("entity") @SearchTag("hide")
        public boolean hideEntities = false;
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.backOnServerRestart", desc = "config.babyzombieaddons.option.backOnServerRestart.desc") @ConfigEditorBoolean @SearchTag("restart") @SearchTag("reconnect")
        public boolean backOnServerRestart = false;
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.autoBackToSkyblock", desc = "config.babyzombieaddons.option.autoBackToSkyblock.desc") @ConfigEditorDropdown @SearchTag("autoback") @SearchTag("skyblock")
        public KickRecovery autoBackToSkyblock = KickRecovery.OFF;
    }

    public static class Pet {
        // ── DraggableList element enum ──
        public enum PetDisplayElement {
            PET_NAME,
            PET_TOTAL_XP,
            PET_XP_PROGRESS,
            PET_ITEM,
            PET_ITEM_WITH_ICON;

            @Override
            public String toString() {
                return switch (this) {
                    case PET_NAME          -> "§7Lv.§f200 §6Golden Dragon";
                    case PET_TOTAL_XP      -> "§e59,160,153";
                    case PET_XP_PROGRESS   -> "§e114,514 §8/ §71,919,810 §b5.96%";
                    case PET_ITEM          -> "§6Hephaestus Relic";
                    case PET_ITEM_WITH_ICON -> "§6Hephaestus Relic ✿";
                };
            }
        }

        @Expose @ConfigOption(name = "config.babyzombieaddons.option.petDisplay", desc = "config.babyzombieaddons.option.petDisplay.desc") @ConfigEditorBoolean @SearchTag("pet")
        public boolean enabled = false;

        @Expose @ConfigOption(name = "config.babyzombieaddons.option.petShowIcon", desc = "config.babyzombieaddons.option.petShowIcon.desc") @ConfigEditorBoolean @SearchTag("pet") @SearchTag("icon")
        public boolean showPetIcon = true;

        @Expose @ConfigOption(name = "config.babyzombieaddons.option.petShowSkin", desc = "config.babyzombieaddons.option.petShowSkin.desc") @ConfigEditorBoolean @SearchTag("pet") @SearchTag("skin")
        public boolean showPetSkin = true;

        @Expose @ConfigOption(name = "config.babyzombieaddons.option.petMainLines", desc = "config.babyzombieaddons.option.petMainLines.desc") @SearchTag("pet") @SearchTag("display")
        @ConfigEditorDraggableList
        public List<PetDisplayElement> mainPetElements = new ArrayList<>(List.of(
            PetDisplayElement.PET_NAME, PetDisplayElement.PET_XP_PROGRESS, PetDisplayElement.PET_ITEM_WITH_ICON
        ));

        @Expose @ConfigOption(name = "config.babyzombieaddons.option.petSharedDisplay", desc = "config.babyzombieaddons.option.petSharedDisplay.desc") @ConfigEditorBoolean @SearchTag("pet") @SearchTag("shared")
        public boolean sharedDisplay = true;

        @Expose @ConfigOption(name = "config.babyzombieaddons.option.petHideSharedIfCurrent", desc = "config.babyzombieaddons.option.petHideSharedIfCurrent.desc") @ConfigEditorBoolean @SearchTag("pet") @SearchTag("shared")
        public boolean hideSharedIfCurrent = false;

        @Expose @ConfigOption(name = "config.babyzombieaddons.option.petSharedLines", desc = "config.babyzombieaddons.option.petSharedLines.desc") @SearchTag("pet") @SearchTag("shared")
        @ConfigEditorDraggableList
        public List<PetDisplayElement> sharedPetElements = new ArrayList<>(List.of(
            PetDisplayElement.PET_NAME, PetDisplayElement.PET_XP_PROGRESS
        ));

        @Expose @ConfigOption(name = "config.babyzombieaddons.group.petPageKeyBindings", desc = "config.babyzombieaddons.group.petPageKeyBindings.desc") @Accordion
        public PetPageKeyBindings petPageKeyBindings = new PetPageKeyBindings();

        public static class PetPageKeyBindings {
            // ── Row 2 (7 slots, columns 2-8) ──
            @Expose @ConfigOption(name = "config.babyzombieaddons.option.petPageKeySlot2_2", desc = "config.babyzombieaddons.option.petPageKeySlot.desc") @ConfigEditorKeybind(defaultKey = -1) @SearchTag("pet") @SearchTag("key")
            public int slot2_2 = -1;
            @Expose @ConfigOption(name = "config.babyzombieaddons.option.petPageKeySlot2_3", desc = "config.babyzombieaddons.option.petPageKeySlot.desc") @ConfigEditorKeybind(defaultKey = -1) @SearchTag("pet") @SearchTag("key")
            public int slot2_3 = -1;
            @Expose @ConfigOption(name = "config.babyzombieaddons.option.petPageKeySlot2_4", desc = "config.babyzombieaddons.option.petPageKeySlot.desc") @ConfigEditorKeybind(defaultKey = -1) @SearchTag("pet") @SearchTag("key")
            public int slot2_4 = -1;
            @Expose @ConfigOption(name = "config.babyzombieaddons.option.petPageKeySlot2_5", desc = "config.babyzombieaddons.option.petPageKeySlot.desc") @ConfigEditorKeybind(defaultKey = -1) @SearchTag("pet") @SearchTag("key")
            public int slot2_5 = -1;
            @Expose @ConfigOption(name = "config.babyzombieaddons.option.petPageKeySlot2_6", desc = "config.babyzombieaddons.option.petPageKeySlot.desc") @ConfigEditorKeybind(defaultKey = -1) @SearchTag("pet") @SearchTag("key")
            public int slot2_6 = -1;
            @Expose @ConfigOption(name = "config.babyzombieaddons.option.petPageKeySlot2_7", desc = "config.babyzombieaddons.option.petPageKeySlot.desc") @ConfigEditorKeybind(defaultKey = -1) @SearchTag("pet") @SearchTag("key")
            public int slot2_7 = -1;
            @Expose @ConfigOption(name = "config.babyzombieaddons.option.petPageKeySlot2_8", desc = "config.babyzombieaddons.option.petPageKeySlot.desc") @ConfigEditorKeybind(defaultKey = -1) @SearchTag("pet") @SearchTag("key")
            public int slot2_8 = -1;

            // ── Row 3 (7 slots, columns 2-8) ──
            @Expose @ConfigOption(name = "config.babyzombieaddons.option.petPageKeySlot3_2", desc = "config.babyzombieaddons.option.petPageKeySlot.desc") @ConfigEditorKeybind(defaultKey = -1) @SearchTag("pet") @SearchTag("key")
            public int slot3_2 = -1;
            @Expose @ConfigOption(name = "config.babyzombieaddons.option.petPageKeySlot3_3", desc = "config.babyzombieaddons.option.petPageKeySlot.desc") @ConfigEditorKeybind(defaultKey = -1) @SearchTag("pet") @SearchTag("key")
            public int slot3_3 = -1;
            @Expose @ConfigOption(name = "config.babyzombieaddons.option.petPageKeySlot3_4", desc = "config.babyzombieaddons.option.petPageKeySlot.desc") @ConfigEditorKeybind(defaultKey = -1) @SearchTag("pet") @SearchTag("key")
            public int slot3_4 = -1;
            @Expose @ConfigOption(name = "config.babyzombieaddons.option.petPageKeySlot3_5", desc = "config.babyzombieaddons.option.petPageKeySlot.desc") @ConfigEditorKeybind(defaultKey = -1) @SearchTag("pet") @SearchTag("key")
            public int slot3_5 = -1;
            @Expose @ConfigOption(name = "config.babyzombieaddons.option.petPageKeySlot3_6", desc = "config.babyzombieaddons.option.petPageKeySlot.desc") @ConfigEditorKeybind(defaultKey = -1) @SearchTag("pet") @SearchTag("key")
            public int slot3_6 = -1;
            @Expose @ConfigOption(name = "config.babyzombieaddons.option.petPageKeySlot3_7", desc = "config.babyzombieaddons.option.petPageKeySlot.desc") @ConfigEditorKeybind(defaultKey = -1) @SearchTag("pet") @SearchTag("key")
            public int slot3_7 = -1;
            @Expose @ConfigOption(name = "config.babyzombieaddons.option.petPageKeySlot3_8", desc = "config.babyzombieaddons.option.petPageKeySlot.desc") @ConfigEditorKeybind(defaultKey = -1) @SearchTag("pet") @SearchTag("key")
            public int slot3_8 = -1;

            // ── Row 4 (7 slots, columns 2-8) ──
            @Expose @ConfigOption(name = "config.babyzombieaddons.option.petPageKeySlot4_2", desc = "config.babyzombieaddons.option.petPageKeySlot.desc") @ConfigEditorKeybind(defaultKey = -1) @SearchTag("pet") @SearchTag("key")
            public int slot4_2 = -1;
            @Expose @ConfigOption(name = "config.babyzombieaddons.option.petPageKeySlot4_3", desc = "config.babyzombieaddons.option.petPageKeySlot.desc") @ConfigEditorKeybind(defaultKey = -1) @SearchTag("pet") @SearchTag("key")
            public int slot4_3 = -1;
            @Expose @ConfigOption(name = "config.babyzombieaddons.option.petPageKeySlot4_4", desc = "config.babyzombieaddons.option.petPageKeySlot.desc") @ConfigEditorKeybind(defaultKey = -1) @SearchTag("pet") @SearchTag("key")
            public int slot4_4 = -1;
            @Expose @ConfigOption(name = "config.babyzombieaddons.option.petPageKeySlot4_5", desc = "config.babyzombieaddons.option.petPageKeySlot.desc") @ConfigEditorKeybind(defaultKey = -1) @SearchTag("pet") @SearchTag("key")
            public int slot4_5 = -1;
            @Expose @ConfigOption(name = "config.babyzombieaddons.option.petPageKeySlot4_6", desc = "config.babyzombieaddons.option.petPageKeySlot.desc") @ConfigEditorKeybind(defaultKey = -1) @SearchTag("pet") @SearchTag("key")
            public int slot4_6 = -1;
            @Expose @ConfigOption(name = "config.babyzombieaddons.option.petPageKeySlot4_7", desc = "config.babyzombieaddons.option.petPageKeySlot.desc") @ConfigEditorKeybind(defaultKey = -1) @SearchTag("pet") @SearchTag("key")
            public int slot4_7 = -1;
            @Expose @ConfigOption(name = "config.babyzombieaddons.option.petPageKeySlot4_8", desc = "config.babyzombieaddons.option.petPageKeySlot.desc") @ConfigEditorKeybind(defaultKey = -1) @SearchTag("pet") @SearchTag("key")
            public int slot4_8 = -1;

            // ── Row 5 (7 slots, columns 2-8) ──
            @Expose @ConfigOption(name = "config.babyzombieaddons.option.petPageKeySlot5_2", desc = "config.babyzombieaddons.option.petPageKeySlot.desc") @ConfigEditorKeybind(defaultKey = -1) @SearchTag("pet") @SearchTag("key")
            public int slot5_2 = -1;
            @Expose @ConfigOption(name = "config.babyzombieaddons.option.petPageKeySlot5_3", desc = "config.babyzombieaddons.option.petPageKeySlot.desc") @ConfigEditorKeybind(defaultKey = -1) @SearchTag("pet") @SearchTag("key")
            public int slot5_3 = -1;
            @Expose @ConfigOption(name = "config.babyzombieaddons.option.petPageKeySlot5_4", desc = "config.babyzombieaddons.option.petPageKeySlot.desc") @ConfigEditorKeybind(defaultKey = -1) @SearchTag("pet") @SearchTag("key")
            public int slot5_4 = -1;
            @Expose @ConfigOption(name = "config.babyzombieaddons.option.petPageKeySlot5_5", desc = "config.babyzombieaddons.option.petPageKeySlot.desc") @ConfigEditorKeybind(defaultKey = -1) @SearchTag("pet") @SearchTag("key")
            public int slot5_5 = -1;
            @Expose @ConfigOption(name = "config.babyzombieaddons.option.petPageKeySlot5_6", desc = "config.babyzombieaddons.option.petPageKeySlot.desc") @ConfigEditorKeybind(defaultKey = -1) @SearchTag("pet") @SearchTag("key")
            public int slot5_6 = -1;
            @Expose @ConfigOption(name = "config.babyzombieaddons.option.petPageKeySlot5_7", desc = "config.babyzombieaddons.option.petPageKeySlot.desc") @ConfigEditorKeybind(defaultKey = -1) @SearchTag("pet") @SearchTag("key")
            public int slot5_7 = -1;
            @Expose @ConfigOption(name = "config.babyzombieaddons.option.petPageKeySlot5_8", desc = "config.babyzombieaddons.option.petPageKeySlot.desc") @ConfigEditorKeybind(defaultKey = -1) @SearchTag("pet") @SearchTag("key")
            public int slot5_8 = -1;

            // ── Page flip (row 6, columns 1 & 9) ──
            @Expose @ConfigOption(name = "config.babyzombieaddons.option.petPageKeyPrevPage", desc = "config.babyzombieaddons.option.petPageKeyPrevPage.desc") @ConfigEditorKeybind(defaultKey = InputConstants.KEY_A) @SearchTag("pet") @SearchTag("key")
            public int prevPage = InputConstants.KEY_A;
            @Expose @ConfigOption(name = "config.babyzombieaddons.option.petPageKeyNextPage", desc = "config.babyzombieaddons.option.petPageKeyNextPage.desc") @ConfigEditorKeybind(defaultKey = InputConstants.KEY_D) @SearchTag("pet") @SearchTag("key")
            public int nextPage = InputConstants.KEY_D;

            /** 获取所有 28 个宠物槽位的键码数组，按行优先排列。 */
            public int[] slotKeys() {
                return new int[]{
                    slot2_2, slot2_3, slot2_4, slot2_5, slot2_6, slot2_7, slot2_8,
                    slot3_2, slot3_3, slot3_4, slot3_5, slot3_6, slot3_7, slot3_8,
                    slot4_2, slot4_3, slot4_4, slot4_5, slot4_6, slot4_7, slot4_8,
                    slot5_2, slot5_3, slot5_4, slot5_5, slot5_6, slot5_7, slot5_8
                };
            }
        }
    }

    public static class NecronBlade {
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.necronBladeExplosionVolume", desc = "config.babyzombieaddons.option.necronBladeExplosionVolume.desc") @ConfigEditorSlider(minValue = 0.0f, maxValue = 1.0f, minStep = 0.01f)
        @SearchTag("necron") @SearchTag("explosion") @SearchTag("implosion") @SearchTag("wither") @SearchTag("hyperion") @SearchTag("valkyrie") @SearchTag("scylla") @SearchTag("astraea") @SearchTag("volume")
        public float explosionVolume = 1.0f;
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.necronBladeShadowWarpVolume", desc = "config.babyzombieaddons.option.necronBladeShadowWarpVolume.desc") @ConfigEditorSlider(minValue = 0.0f, maxValue = 1.0f, minStep = 0.01f)
        @SearchTag("necron") @SearchTag("shadow") @SearchTag("warp") @SearchTag("wither") @SearchTag("hyperion") @SearchTag("valkyrie") @SearchTag("scylla") @SearchTag("astraea") @SearchTag("volume")
        public float shadowWarpVolume = 1.0f;
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.necronBladeWitherShieldVolume", desc = "config.babyzombieaddons.option.necronBladeWitherShieldVolume.desc") @ConfigEditorSlider(minValue = 0.0f, maxValue = 1.0f, minStep = 0.01f)
        @SearchTag("necron") @SearchTag("wither") @SearchTag("shield") @SearchTag("hyperion") @SearchTag("valkyrie") @SearchTag("scylla") @SearchTag("astraea") @SearchTag("volume")
        public float witherShieldVolume = 1.0f;
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.necronBladeHideExplosionParticles", desc = "config.babyzombieaddons.option.necronBladeHideExplosionParticles.desc") @ConfigEditorBoolean
        @SearchTag("necron") @SearchTag("explosion") @SearchTag("implosion") @SearchTag("wither") @SearchTag("hyperion") @SearchTag("valkyrie") @SearchTag("scylla") @SearchTag("astraea") @SearchTag("particle")
        public boolean hideExplosionParticles = false;
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.necronBladeHideOthersParticles", desc = "config.babyzombieaddons.option.necronBladeHideOthersParticles.desc") @ConfigEditorBoolean
        @SearchTag("necron") @SearchTag("explosion") @SearchTag("implosion") @SearchTag("wither") @SearchTag("hyperion") @SearchTag("valkyrie") @SearchTag("scylla") @SearchTag("astraea") @SearchTag("particle")
        public boolean hideOthersParticles = false;
    }

    public static class TeleportSword {
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.teleportSwordTeleportVolume", desc = "config.babyzombieaddons.option.teleportSwordTeleportVolume.desc") @ConfigEditorSlider(minValue = 0.0f, maxValue = 1.0f, minStep = 0.01f)
        @SearchTag("teleport") @SearchTag("warp") @SearchTag("aote") @SearchTag("aotv") @SearchTag("aspect") @SearchTag("volume")
        public float teleportVolume = 1.0f;
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.teleportSwordEtherwarpVolume", desc = "config.babyzombieaddons.option.teleportSwordEtherwarpVolume.desc") @ConfigEditorSlider(minValue = 0.0f, maxValue = 1.0f, minStep = 0.01f)
        @SearchTag("teleport") @SearchTag("ewarp") @SearchTag("etherwarp") @SearchTag("aote") @SearchTag("aotv") @SearchTag("aspect") @SearchTag("volume")
        public float etherwarpVolume = 1.0f;
    }

    public static class Loadout {
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.loadoutGui", desc = "config.babyzombieaddons.option.loadoutGui.desc") @ConfigEditorBoolean @SearchTag("loadout")
        public boolean enabled = false;
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.loadoutEntityRenderMode", desc = "config.babyzombieaddons.option.loadoutEntityRenderMode.desc") @ConfigEditorDropdown @SearchTag("loadout") @SearchTag("entity") @SearchTag("render")
        public EntityRenderMode entityRenderMode = EntityRenderMode.ARMOR_STAND;
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.loadoutItemCache", desc = "config.babyzombieaddons.option.loadoutItemCache.desc") @ConfigEditorBoolean @SearchTag("loadout") @SearchTag("cache")
        public boolean cacheItemsFromCurrent = false;
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.loadoutAutoClose", desc = "config.babyzombieaddons.option.loadoutAutoClose.desc") @ConfigEditorBoolean @SearchTag("loadout") @SearchTag("autoclose")
        public boolean autoClose = false;
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.loadoutCloseOnNonSwitchClick", desc = "config.babyzombieaddons.option.loadoutCloseOnNonSwitchClick.desc") @ConfigEditorBoolean @SearchTag("loadout") @SearchTag("autoclose") @SearchTag("click")
        public boolean closeOnNonSwitchClick = false;
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.loadoutKuudraFastClose", desc = "config.babyzombieaddons.option.loadoutKuudraFastClose.desc") @ConfigEditorBoolean @SearchTag("loadout") @SearchTag("autoclose") @SearchTag("kuudra")
        public boolean kuudraFastClose = false;
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.loadoutDungeonFastClose", desc = "config.babyzombieaddons.option.loadoutDungeonFastClose.desc") @ConfigEditorBoolean @SearchTag("loadout") @SearchTag("autoclose") @SearchTag("dungeon")
        public boolean dungeonFastClose = false;

        @Expose @ConfigOption(name = "config.babyzombieaddons.group.loadoutKeyBindings", desc = "config.babyzombieaddons.group.loadoutKeyBindings.desc") @Accordion
        public KeyBindings keyBindings = new KeyBindings();

        public static class KeyBindings {
            @Expose @ConfigOption(name = "config.babyzombieaddons.option.loadoutKeyPreset1", desc = "config.babyzombieaddons.option.loadoutKeyPreset.desc") @ConfigEditorKeybind(defaultKey = InputConstants.KEY_1) @SearchTag("loadout") @SearchTag("key")
            public int preset1 = InputConstants.KEY_1;
            @Expose @ConfigOption(name = "config.babyzombieaddons.option.loadoutKeyPreset2", desc = "config.babyzombieaddons.option.loadoutKeyPreset.desc") @ConfigEditorKeybind(defaultKey = InputConstants.KEY_2) @SearchTag("loadout") @SearchTag("key")
            public int preset2 = InputConstants.KEY_2;
            @Expose @ConfigOption(name = "config.babyzombieaddons.option.loadoutKeyPreset3", desc = "config.babyzombieaddons.option.loadoutKeyPreset.desc") @ConfigEditorKeybind(defaultKey = InputConstants.KEY_3) @SearchTag("loadout") @SearchTag("key")
            public int preset3 = InputConstants.KEY_3;
            @Expose @ConfigOption(name = "config.babyzombieaddons.option.loadoutKeyPreset4", desc = "config.babyzombieaddons.option.loadoutKeyPreset.desc") @ConfigEditorKeybind(defaultKey = InputConstants.KEY_4) @SearchTag("loadout") @SearchTag("key")
            public int preset4 = InputConstants.KEY_4;
            @Expose @ConfigOption(name = "config.babyzombieaddons.option.loadoutKeyPreset5", desc = "config.babyzombieaddons.option.loadoutKeyPreset.desc") @ConfigEditorKeybind(defaultKey = InputConstants.KEY_5) @SearchTag("loadout") @SearchTag("key")
            public int preset5 = InputConstants.KEY_5;
            @Expose @ConfigOption(name = "config.babyzombieaddons.option.loadoutKeyPreset6", desc = "config.babyzombieaddons.option.loadoutKeyPreset.desc") @ConfigEditorKeybind(defaultKey = InputConstants.KEY_6) @SearchTag("loadout") @SearchTag("key")
            public int preset6 = InputConstants.KEY_6;
            @Expose @ConfigOption(name = "config.babyzombieaddons.option.loadoutKeyPreset7", desc = "config.babyzombieaddons.option.loadoutKeyPreset.desc") @ConfigEditorKeybind(defaultKey = InputConstants.KEY_7) @SearchTag("loadout") @SearchTag("key")
            public int preset7 = InputConstants.KEY_7;
            @Expose @ConfigOption(name = "config.babyzombieaddons.option.loadoutKeyPreset8", desc = "config.babyzombieaddons.option.loadoutKeyPreset.desc") @ConfigEditorKeybind(defaultKey = InputConstants.KEY_8) @SearchTag("loadout") @SearchTag("key")
            public int preset8 = InputConstants.KEY_8;
            @Expose @ConfigOption(name = "config.babyzombieaddons.option.loadoutKeyPreset9", desc = "config.babyzombieaddons.option.loadoutKeyPreset.desc") @ConfigEditorKeybind(defaultKey = InputConstants.KEY_9) @SearchTag("loadout") @SearchTag("key")
            public int preset9 = InputConstants.KEY_9;
            @Expose @ConfigOption(name = "config.babyzombieaddons.option.loadoutKeyPreset10", desc = "config.babyzombieaddons.option.loadoutKeyPreset.desc") @ConfigEditorKeybind(defaultKey = InputConstants.KEY_0) @SearchTag("loadout") @SearchTag("key")
            public int preset10 = InputConstants.KEY_0;
            @Expose @ConfigOption(name = "config.babyzombieaddons.option.loadoutKeyPreset11", desc = "config.babyzombieaddons.option.loadoutKeyPreset.desc") @ConfigEditorKeybind(defaultKey = InputConstants.KEY_MINUS) @SearchTag("loadout") @SearchTag("key")
            public int preset11 = InputConstants.KEY_MINUS;
            @Expose @ConfigOption(name = "config.babyzombieaddons.option.loadoutKeyPreset12", desc = "config.babyzombieaddons.option.loadoutKeyPreset.desc") @ConfigEditorKeybind(defaultKey = InputConstants.KEY_EQUALS) @SearchTag("loadout") @SearchTag("key")
            public int preset12 = InputConstants.KEY_EQUALS;

            @Expose @ConfigOption(name = "config.babyzombieaddons.option.loadoutKeyPrevPage", desc = "config.babyzombieaddons.option.loadoutKeyPrevPage.desc") @ConfigEditorKeybind(defaultKey = InputConstants.KEY_A) @SearchTag("loadout") @SearchTag("key")
            public int prevPage = InputConstants.KEY_A;
            @Expose @ConfigOption(name = "config.babyzombieaddons.option.loadoutKeyNextPage", desc = "config.babyzombieaddons.option.loadoutKeyNextPage.desc") @ConfigEditorKeybind(defaultKey = InputConstants.KEY_D) @SearchTag("loadout") @SearchTag("key")
            public int nextPage = InputConstants.KEY_D;

            @Expose @ConfigOption(name = "config.babyzombieaddons.option.loadoutKeyClose", desc = "config.babyzombieaddons.option.loadoutKeyClose.desc") @ConfigEditorKeybind(defaultKey = InputConstants.KEY_E) @SearchTag("loadout") @SearchTag("key")
            public int closeKey = InputConstants.KEY_E;

            /** 获取预设 1-12 的键码数组，方便 keyPressed 中遍历匹配 */
            public int[] presetKeys() {
                return new int[]{preset1, preset2, preset3, preset4, preset5, preset6, preset7, preset8, preset9, preset10, preset11, preset12};
            }
        }
    }

    public static class CustomRingtone {
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.customRingtoneEnabled", desc = "config.babyzombieaddons.option.customRingtoneEnabled.desc") @ConfigEditorBoolean @SearchTag("abiphone") @SearchTag("ringtone")
        public boolean enabled = false;

        @Expose @ConfigOption(name = "config.babyzombieaddons.option.customRingtoneDisc", desc = "config.babyzombieaddons.option.customRingtoneDisc.desc") @ConfigEditorDropdown @SearchTag("abiphone") @SearchTag("ringtone")
        public MusicDisc disc = MusicDisc.TEARS;

        @ConfigOption(name = "config.babyzombieaddons.option.customRingtonePreview", desc = "config.babyzombieaddons.option.customRingtonePreview.desc") @ConfigEditorButton(buttonText = "PLAY") @SearchTag("abiphone") @SearchTag("ringtone")
        public transient Runnable preview = CustomRingtoneModule::playPreview;

        @Expose @ConfigOption(name = "config.babyzombieaddons.option.customRingtonePitch", desc = "config.babyzombieaddons.option.customRingtonePitch.desc") @ConfigEditorSlider(minValue = 0.5f, maxValue = 2.0f, minStep = 0.01f) @SearchTag("abiphone") @SearchTag("ringtone")
        public float pitch = 1.0f;

        @Expose @ConfigOption(name = "config.babyzombieaddons.option.customRingtoneStartTime", desc = "config.babyzombieaddons.option.customRingtoneStartTime.desc") @ConfigEditorSlider(minValue = 0.0f, maxValue = 360.0f, minStep = 0.01f) @SearchTag("abiphone") @SearchTag("ringtone")
        public float startTime = 19.4f;

        @Expose @ConfigOption(name = "config.babyzombieaddons.option.customRingtoneDuration", desc = "config.babyzombieaddons.option.customRingtoneDuration.desc") @ConfigEditorSlider(minValue = 0.0f, maxValue = 5.0f, minStep = 0.01f) @SearchTag("abiphone") @SearchTag("ringtone")
        public float duration = 2.5f;
    }

    public static class BazzarTopOrders {
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.bazzarOverlayEnabled", desc = "config.babyzombieaddons.option.bazzarOverlayEnabled.desc") @ConfigEditorBoolean @SearchTag("bazaar") @SearchTag("bazzar")
        public boolean overlayEnabled = false;

        @Expose @ConfigOption(name = "config.babyzombieaddons.option.bazzarFlipEnabled", desc = "config.babyzombieaddons.option.bazzarFlipEnabled.desc") @ConfigEditorBoolean @SearchTag("bazaar") @SearchTag("flip")
        public boolean flipEnabled = false;

        @Expose @ConfigOption(name = "config.babyzombieaddons.option.bazzarFlipBuyEnabled", desc = "config.babyzombieaddons.option.bazzarFlipBuyEnabled.desc") @ConfigEditorBoolean @SearchTag("bazaar") @SearchTag("flip")
        public boolean flipBuyEnabled = false;

        @Expose @ConfigOption(name = "config.babyzombieaddons.option.bazzarFlipSellEnabled", desc = "config.babyzombieaddons.option.bazzarFlipSellEnabled.desc") @ConfigEditorBoolean @SearchTag("bazaar") @SearchTag("flip")
        public boolean flipSellEnabled = false;

        @Expose @ConfigOption(name = "config.babyzombieaddons.option.bazzarApiEnabled", desc = "config.babyzombieaddons.option.bazzarApiEnabled.desc") @ConfigEditorBoolean @SearchTag("bazaar") @SearchTag("api")
        public boolean apiEnabled = false;

        @Expose @ConfigOption(name = "config.babyzombieaddons.option.bazzarShowActionBar", desc = "config.babyzombieaddons.option.bazzarShowActionBar.desc") @ConfigEditorBoolean @SearchTag("bazaar")
        public boolean showActionBar = false;

        @Expose @ConfigOption(name = "config.babyzombieaddons.option.bazzarShowBuyOrders", desc = "config.babyzombieaddons.option.bazzarShowBuyOrders.desc") @ConfigEditorBoolean @SearchTag("bazaar")
        public boolean showBuyOrders = false;

        @Expose @ConfigOption(name = "config.babyzombieaddons.option.bazzarShowSellOffers", desc = "config.babyzombieaddons.option.bazzarShowSellOffers.desc") @ConfigEditorBoolean @SearchTag("bazaar")
        public boolean showSellOffers = false;

        @Expose @ConfigOption(name = "config.babyzombieaddons.option.bazzarOrdersPageEnabled", desc = "config.babyzombieaddons.option.bazzarOrdersPageEnabled.desc") @ConfigEditorBoolean @SearchTag("bazaar") @SearchTag("bazzar") @SearchTag("orders") @SearchTag("订单页")
        public boolean ordersPageEnabled = false;

        @Expose @ConfigOption(name = "config.babyzombieaddons.option.bazzarMaxLines", desc = "config.babyzombieaddons.option.bazzarMaxLines.desc") @ConfigEditorSlider(minValue = 1, maxValue = 30, minStep = 1) @SearchTag("bazaar") @SearchTag("行数") @SearchTag("lines")
        public int maxLines = 7;

        @Expose @ConfigOption(name = "config.babyzombieaddons.option.bazzarBuyOrderHistory", desc = "config.babyzombieaddons.option.bazzarBuyOrderHistory.desc") @ConfigEditorBoolean @SearchTag("bazaar") @SearchTag("求购历史")
        public boolean buyOrderHistoryEnabled = false;

        @Expose @ConfigOption(name = "config.babyzombieaddons.option.bazzarBuyOrderHistoryMaxLines", desc = "config.babyzombieaddons.option.bazzarBuyOrderHistoryMaxLines.desc") @ConfigEditorSlider(minValue = 1, maxValue = 30, minStep = 1) @SearchTag("bazaar") @SearchTag("行数")
        public int buyOrderHistoryMaxLines = 7;

        @Expose @ConfigOption(name = "config.babyzombieaddons.option.bazzarSignPasteAmount", desc = "config.babyzombieaddons.option.bazzarSignPasteAmount.desc") @ConfigEditorBoolean @SearchTag("bazaar") @SearchTag("告示牌") @SearchTag("剪贴板") @SearchTag("数量")
        public boolean signPasteAmount = false;

        // ── 告示牌快捷数量按钮 ──
        /** DraggableList 候选按声明顺序显示:整组(64 的倍数)在前,其余在后,各组内按数量升序,便于在配置界面中查找。 */
        public enum SignQuickAmount {
            // ── 整组(64 的倍数),从小到大 ──
            GROUP_1(64), GROUP_2(128), GROUP_3(192), GROUP_4(256),
            GROUP_5(320), GROUP_6(384), GROUP_7(448), GROUP_8(512),
            GROUP_9(576), GROUP_10(640), GROUP_11(704), GROUP_12(768),
            GROUP_13(832), GROUP_14(896), GROUP_15(960), GROUP_16(1024),
            GROUP_17(1088), GROUP_18(1152), GROUP_19(1216), GROUP_20(1280),
            GROUP_21(1344), GROUP_22(1408), GROUP_23(1472), GROUP_24(1536), GROUP_25(1600),
            GROUP_26(1664), GROUP_27(1728), GROUP_28(1792), GROUP_29(1856), GROUP_30(1920),
            GROUP_31(1984), GROUP_32(2048), GROUP_33(2112), GROUP_34(2176), GROUP_35(2240),

            // ── 非整组,从小到大 ──
            ONE(1), EIGHT(8), SIXTEEN(16), THIRTY_TWO(32), FORTY_EIGHT(48), ROUND_80(80),
            ROUND_100(100), ROUND_160(160), ROUND_240(240), ROUND_250(250), ROUND_500(500),
            ROUND_750(750), ROUND_1000(1000), ROUND_1250(1250), ROUND_1500(1500), ROUND_1750(1750),
            ROUND_2000(2000), ROUND_5000(5000), ROUND_10000(10000),

            MAX_ORDER(71680);

            private final int amount;

            SignQuickAmount(int amount) { this.amount = amount; }

            public int amount() { return amount; }

            public String displayText() {
                String text = String.format(Locale.ROOT, "%,d", amount);
                if (amount % 64 == 0) text += " (" + (amount / 64) + "组)";
                return text;
            }

            /** 配置列表与告示牌按钮统一显示文本，无需翻译。 */
            @Override
            public String toString() {
                return displayText();
            }
        }

        @Expose @ConfigOption(name = "config.babyzombieaddons.option.bazzarSignQuickAmountsEnabled", desc = "config.babyzombieaddons.option.bazzarSignQuickAmountsEnabled.desc") @ConfigEditorBoolean @SearchTag("bazaar") @SearchTag("告示牌") @SearchTag("快捷") @SearchTag("数量")
        public boolean signQuickAmountsEnabled = false;

        @Expose @ConfigOption(name = "config.babyzombieaddons.option.bazzarSignQuickAmounts", desc = "config.babyzombieaddons.option.bazzarSignQuickAmounts.desc") @ConfigEditorDraggableList @SearchTag("bazaar") @SearchTag("告示牌") @SearchTag("快捷") @SearchTag("数量")
        public List<SignQuickAmount> signQuickAmounts = new ArrayList<>(List.of(
                SignQuickAmount.SIXTEEN, SignQuickAmount.THIRTY_TWO, SignQuickAmount.FORTY_EIGHT, SignQuickAmount.ROUND_80, SignQuickAmount.ROUND_160,
                SignQuickAmount.GROUP_1, SignQuickAmount.GROUP_2, SignQuickAmount.GROUP_5, SignQuickAmount.GROUP_10, SignQuickAmount.MAX_ORDER
        ));
    }

    public static class AuctionQuickSell {
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.auctionQuickSellEnabled", desc = "config.babyzombieaddons.option.auctionQuickSellEnabled.desc") @ConfigEditorBoolean @SearchTag("auction") @SearchTag("ah") @SearchTag("上架") @SearchTag("告示牌")
        public boolean auctionQuickSellEnabled = false;

        @Expose @ConfigOption(name = "config.babyzombieaddons.option.auctionQuickSellUndercut", desc = "config.babyzombieaddons.option.auctionQuickSellUndercut.desc") @ConfigEditorSlider(minValue = 1, maxValue = 10000, minStep = 1) @SearchTag("auction") @SearchTag("ah") @SearchTag("压价")
        public int auctionQuickSellUndercut = 1;
    }
}
