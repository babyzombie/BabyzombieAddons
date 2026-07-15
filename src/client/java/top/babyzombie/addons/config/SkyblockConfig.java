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
import org.lwjgl.glfw.GLFW;
import top.babyzombie.addons.config.ModConfig.*;
import top.babyzombie.addons.module.raredrop.RareDropScreen;

import java.util.ArrayList;
import java.util.List;

public class SkyblockConfig {

    @Expose @ConfigOption(name = "config.babyzombieaddons.option.abiphoneGui", desc = "config.babyzombieaddons.option.abiphoneGui.desc") @ConfigEditorBoolean @SearchTag("abiphone")
    public boolean abiphoneGui = false;

    @Expose @ConfigOption(name = "config.babyzombieaddons.option.fallSoundVolume", desc = "config.babyzombieaddons.option.fallSoundVolume.desc") @ConfigEditorSlider(minValue = 0.0f, maxValue = 1.0f, minStep = 0.01f) @SearchTag("fall") @SearchTag("sound") @SearchTag("volume")
    public float fallSoundVolume = 1.0f;

    @Expose @ConfigOption(name = "config.babyzombieaddons.option.bzGetFromSacks", desc = "config.babyzombieaddons.option.bzGetFromSacks.desc") @ConfigEditorDropdown @SearchTag("bazaar")
    public BzGetFromSacksMode bzGetFromSacks = BzGetFromSacksMode.OFF;

    @Expose @ConfigOption(name = "config.babyzombieaddons.option.raredropManage", desc = "config.babyzombieaddons.option.raredropManage.desc") @ConfigEditorButton(buttonText = "OPEN") @SearchTag("raredrop") @SearchTag("drop")
    public transient Runnable raredropManage = () -> Minecraft.getInstance().setScreen(new RareDropScreen(Minecraft.getInstance().screen));

    @Expose @ConfigOption(name = "config.babyzombieaddons.option.cakeBuffTracker", desc = "config.babyzombieaddons.option.cakeBuffTracker.desc") @ConfigEditorBoolean @SearchTag("cake")
    public boolean cakeBuffTracker = false;
    @Expose @ConfigOption(name = "config.babyzombieaddons.option.minionCollectAutoClose", desc = "config.babyzombieaddons.option.minionCollectAutoClose.desc") @ConfigEditorBoolean @SearchTag("minion")
    public boolean minionCollectAutoClose = false;

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

        @Expose @ConfigOption(name = "config.babyzombieaddons.option.petSharedLines", desc = "config.babyzombieaddons.option.petSharedLines.desc") @SearchTag("pet") @SearchTag("shared")
        @ConfigEditorDraggableList
        public List<PetDisplayElement> sharedPetElements = new ArrayList<>(List.of(
            PetDisplayElement.PET_NAME, PetDisplayElement.PET_XP_PROGRESS
        ));
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
        @SearchTag("teleport") @SearchTag("etherwarp") @SearchTag("aote") @SearchTag("aotv") @SearchTag("aspect") @SearchTag("volume")
        public float etherwarpVolume = 1.0f;
    }

    public static class Loadout {
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.loadoutGui", desc = "config.babyzombieaddons.option.loadoutGui.desc") @ConfigEditorBoolean @SearchTag("loadout")
        public boolean enabled = false;
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.loadoutEntityRenderMode", desc = "config.babyzombieaddons.option.loadoutEntityRenderMode.desc") @ConfigEditorDropdown @SearchTag("loadout") @SearchTag("entity") @SearchTag("render")
        public EntityRenderMode entityRenderMode = EntityRenderMode.ARMOR_STAND;
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.loadoutAutoClose", desc = "config.babyzombieaddons.option.loadoutAutoClose.desc") @ConfigEditorBoolean @SearchTag("loadout") @SearchTag("autoclose")
        public boolean autoClose = false;

        @Expose @ConfigOption(name = "config.babyzombieaddons.group.loadoutKeyBindings", desc = "config.babyzombieaddons.group.loadoutKeyBindings.desc") @Accordion
        public KeyBindings keyBindings = new KeyBindings();

        public static class KeyBindings {
            @Expose @ConfigOption(name = "config.babyzombieaddons.option.loadoutKeyPreset1", desc = "config.babyzombieaddons.option.loadoutKeyPreset.desc") @ConfigEditorKeybind(defaultKey = GLFW.GLFW_KEY_1) @SearchTag("loadout") @SearchTag("key")
            public int preset1 = GLFW.GLFW_KEY_1;
            @Expose @ConfigOption(name = "config.babyzombieaddons.option.loadoutKeyPreset2", desc = "config.babyzombieaddons.option.loadoutKeyPreset.desc") @ConfigEditorKeybind(defaultKey = GLFW.GLFW_KEY_2) @SearchTag("loadout") @SearchTag("key")
            public int preset2 = GLFW.GLFW_KEY_2;
            @Expose @ConfigOption(name = "config.babyzombieaddons.option.loadoutKeyPreset3", desc = "config.babyzombieaddons.option.loadoutKeyPreset.desc") @ConfigEditorKeybind(defaultKey = GLFW.GLFW_KEY_3) @SearchTag("loadout") @SearchTag("key")
            public int preset3 = GLFW.GLFW_KEY_3;
            @Expose @ConfigOption(name = "config.babyzombieaddons.option.loadoutKeyPreset4", desc = "config.babyzombieaddons.option.loadoutKeyPreset.desc") @ConfigEditorKeybind(defaultKey = GLFW.GLFW_KEY_4) @SearchTag("loadout") @SearchTag("key")
            public int preset4 = GLFW.GLFW_KEY_4;
            @Expose @ConfigOption(name = "config.babyzombieaddons.option.loadoutKeyPreset5", desc = "config.babyzombieaddons.option.loadoutKeyPreset.desc") @ConfigEditorKeybind(defaultKey = GLFW.GLFW_KEY_5) @SearchTag("loadout") @SearchTag("key")
            public int preset5 = GLFW.GLFW_KEY_5;
            @Expose @ConfigOption(name = "config.babyzombieaddons.option.loadoutKeyPreset6", desc = "config.babyzombieaddons.option.loadoutKeyPreset.desc") @ConfigEditorKeybind(defaultKey = GLFW.GLFW_KEY_6) @SearchTag("loadout") @SearchTag("key")
            public int preset6 = GLFW.GLFW_KEY_6;
            @Expose @ConfigOption(name = "config.babyzombieaddons.option.loadoutKeyPreset7", desc = "config.babyzombieaddons.option.loadoutKeyPreset.desc") @ConfigEditorKeybind(defaultKey = GLFW.GLFW_KEY_7) @SearchTag("loadout") @SearchTag("key")
            public int preset7 = GLFW.GLFW_KEY_7;
            @Expose @ConfigOption(name = "config.babyzombieaddons.option.loadoutKeyPreset8", desc = "config.babyzombieaddons.option.loadoutKeyPreset.desc") @ConfigEditorKeybind(defaultKey = GLFW.GLFW_KEY_8) @SearchTag("loadout") @SearchTag("key")
            public int preset8 = GLFW.GLFW_KEY_8;
            @Expose @ConfigOption(name = "config.babyzombieaddons.option.loadoutKeyPreset9", desc = "config.babyzombieaddons.option.loadoutKeyPreset.desc") @ConfigEditorKeybind(defaultKey = GLFW.GLFW_KEY_9) @SearchTag("loadout") @SearchTag("key")
            public int preset9 = GLFW.GLFW_KEY_9;
            @Expose @ConfigOption(name = "config.babyzombieaddons.option.loadoutKeyPreset10", desc = "config.babyzombieaddons.option.loadoutKeyPreset.desc") @ConfigEditorKeybind(defaultKey = GLFW.GLFW_KEY_0) @SearchTag("loadout") @SearchTag("key")
            public int preset10 = GLFW.GLFW_KEY_0;
            @Expose @ConfigOption(name = "config.babyzombieaddons.option.loadoutKeyPreset11", desc = "config.babyzombieaddons.option.loadoutKeyPreset.desc") @ConfigEditorKeybind(defaultKey = GLFW.GLFW_KEY_MINUS) @SearchTag("loadout") @SearchTag("key")
            public int preset11 = GLFW.GLFW_KEY_MINUS;
            @Expose @ConfigOption(name = "config.babyzombieaddons.option.loadoutKeyPreset12", desc = "config.babyzombieaddons.option.loadoutKeyPreset.desc") @ConfigEditorKeybind(defaultKey = GLFW.GLFW_KEY_EQUAL) @SearchTag("loadout") @SearchTag("key")
            public int preset12 = GLFW.GLFW_KEY_EQUAL;

            @Expose @ConfigOption(name = "config.babyzombieaddons.option.loadoutKeyPrevPage", desc = "config.babyzombieaddons.option.loadoutKeyPrevPage.desc") @ConfigEditorKeybind(defaultKey = GLFW.GLFW_KEY_A) @SearchTag("loadout") @SearchTag("key")
            public int prevPage = GLFW.GLFW_KEY_A;
            @Expose @ConfigOption(name = "config.babyzombieaddons.option.loadoutKeyNextPage", desc = "config.babyzombieaddons.option.loadoutKeyNextPage.desc") @ConfigEditorKeybind(defaultKey = GLFW.GLFW_KEY_D) @SearchTag("loadout") @SearchTag("key")
            public int nextPage = GLFW.GLFW_KEY_D;

            /** 获取预设 1-12 的键码数组，方便 keyPressed 中遍历匹配 */
            public int[] presetKeys() {
                return new int[]{preset1, preset2, preset3, preset4, preset5, preset6, preset7, preset8, preset9, preset10, preset11, preset12};
            }
        }
    }
}
