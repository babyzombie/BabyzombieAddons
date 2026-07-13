package top.babyzombie.addons.config;
import com.google.gson.annotations.Expose;

import io.github.notenoughupdates.moulconfig.annotations.Accordion;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorButton;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDropdown;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorSlider;
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption;
import io.github.notenoughupdates.moulconfig.annotations.SearchTag;
import net.minecraft.client.Minecraft;
import top.babyzombie.addons.config.ModConfig.*;
import top.babyzombie.addons.module.raredrop.RareDropScreen;

public class SkyblockConfig {

    @Expose @ConfigOption(name = "config.babyzombieaddons.option.abiphoneGui", desc = "config.babyzombieaddons.option.abiphoneGui.desc") @ConfigEditorBoolean @SearchTag("abiphone")
    public boolean abiphoneGui = false;

    @Expose @ConfigOption(name = "config.babyzombieaddons.option.bzGetFromSacks", desc = "config.babyzombieaddons.option.bzGetFromSacks.desc") @ConfigEditorDropdown @SearchTag("bazaar")
    public BzGetFromSacksMode bzGetFromSacks = BzGetFromSacksMode.OFF;

    @Expose @ConfigOption(name = "config.babyzombieaddons.option.raredropManage", desc = "config.babyzombieaddons.option.raredropManage.desc") @ConfigEditorButton(buttonText = "OPEN")
    public transient Runnable raredropManage = () -> Minecraft.getInstance().setScreen(new RareDropScreen(null));

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

    @Expose @ConfigOption(name = "config.babyzombieaddons.group.loadout", desc = "") @Accordion
    public Loadout loadout = new Loadout();

    public static class AutoIS {
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.autois", desc = "config.babyzombieaddons.option.autois.desc") @ConfigEditorBoolean
        public boolean enabled = false;
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.autoisDelay", desc = "config.babyzombieaddons.option.autoisDelay.desc") @ConfigEditorSlider(minValue = 5, maxValue = 125, minStep = 5)
        public int delay = 5;
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.autoisDest", desc = "config.babyzombieaddons.option.autoisDest.desc") @ConfigEditorDropdown
        public AutoISDest dest = AutoISDest.GARDEN;
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.hideEntities", desc = "config.babyzombieaddons.option.hideEntities.desc") @ConfigEditorBoolean
        public boolean hideEntities = false;
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.backOnServerRestart", desc = "config.babyzombieaddons.option.backOnServerRestart.desc") @ConfigEditorBoolean
        public boolean backOnServerRestart = false;
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.autoBackToSkyblock", desc = "config.babyzombieaddons.option.autoBackToSkyblock.desc") @ConfigEditorDropdown
        public KickRecovery autoBackToSkyblock = KickRecovery.LOBBY_ONLY;
    }

    public static class Pet {
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.petDisplay", desc = "config.babyzombieaddons.option.petDisplay.desc") @ConfigEditorBoolean
        public boolean enabled = false;
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.petExpDisplay", desc = "config.babyzombieaddons.option.petExpDisplay.desc") @ConfigEditorBoolean
        public boolean expDisplay = true;
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.petItemDisplay", desc = "config.babyzombieaddons.option.petItemDisplay.desc") @ConfigEditorBoolean
        public boolean itemDisplay = true;
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.petItemIconDisplay", desc = "config.babyzombieaddons.option.petItemIconDisplay.desc") @ConfigEditorBoolean
        public boolean itemIconDisplay = true;
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.petSharedDisplay", desc = "config.babyzombieaddons.option.petSharedDisplay.desc") @ConfigEditorBoolean
        public boolean sharedDisplay = true;
    }

    public static class NecronBlade {
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.necronBladeExplosionVolume", desc = "config.babyzombieaddons.option.necronBladeExplosionVolume.desc") @ConfigEditorSlider(minValue = 0.0f, maxValue = 1.0f, minStep = 0.05f)
        public float explosionVolume = 1.0f;
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.necronBladeHideExplosionParticles", desc = "config.babyzombieaddons.option.necronBladeHideExplosionParticles.desc") @ConfigEditorBoolean
        public boolean hideExplosionParticles = false;
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.necronBladeHideOthersParticles", desc = "config.babyzombieaddons.option.necronBladeHideOthersParticles.desc") @ConfigEditorBoolean
        public boolean hideOthersParticles = false;
    }

    public static class Loadout {
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.loadoutGui", desc = "config.babyzombieaddons.option.loadoutGui.desc") @ConfigEditorBoolean
        public boolean enabled = false;
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.loadoutEntityRenderMode", desc = "config.babyzombieaddons.option.loadoutEntityRenderMode.desc") @ConfigEditorDropdown
        public EntityRenderMode entityRenderMode = EntityRenderMode.ARMOR_STAND;
        @Expose @ConfigOption(name = "config.babyzombieaddons.option.loadoutAutoClose", desc = "config.babyzombieaddons.option.loadoutAutoClose.desc") @ConfigEditorBoolean
        public boolean autoClose = false;
    }
}
