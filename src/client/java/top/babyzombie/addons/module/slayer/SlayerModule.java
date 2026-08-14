package top.babyzombie.addons.module.slayer;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientEntityEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLevelEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.sounds.SoundEvents;
import top.babyzombie.addons.event.PlaySoundEvents;
import top.babyzombie.addons.module.slayer.itemtimer.*;
import top.babyzombie.addons.util.ChatUtils;

public final class SlayerModule {
    private SlayerModule() {}

    public static void init() {
        // Initialize submodules (register their event listeners)
        PigmanSwordTimer.init();
        RagnarockAxeTimer.init();
        ReaperArmorTimer.init();
        EndStoneSwordTimer.init();
        ReheatedGummyPolarBearTimer.init();
        NoSlayerQuestWarning.init();
        BloodfiendLowHPBox.init();
        EffigyDisplay.init();
        SlayerBossRespawnAlert.init();
        SlayerBossDetector.init();
        SlayerBossBox.init();
        SlayerHUD.init();

        // ---- Wire sound events ----
        PlaySoundEvents.BEFORE_PLAY.register(sound -> {
            var snd = sound.getSound();
            if (snd == null) return false;
            var identifier  = sound.getIdentifier();
            float pitch = 1f;
            try { pitch = sound.getPitch(); } catch (Exception ignored) {}
            if (identifier.equals(SoundEvents.ZOMBIFIED_PIGLIN_ANGRY.location())) {
                PigmanSwordTimer.onSound(true);
            } else if (identifier.equals(SoundEvents.ZOMBIE_VILLAGER_CURE.location())) {
                ReaperArmorTimer.onSound(pitch);
            } else if (identifier.equals(SoundEvents.GENERIC_DRINK.value().location())) {
                PigmanSwordTimer.onSound(false);
            } else {
                return false;
            }
            return false;
        });

        // ---- Wire entity death for NoSlayerQuestWarning ----
        ClientEntityEvents.ENTITY_UNLOAD.register((entity, world) -> {
            if (entity.isAlive()) return;
            NoSlayerQuestWarning.onEntityDeath();
        });

        // ---- Slayer quest lifecycle ----
        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            if (overlay) return;
            String text = ChatUtils.stripColor(message.getString()).trim();
            if (text.equals("SLAYER QUEST STARTED!")) {
                NoSlayerQuestWarning.onSlayerStart();
            } else if (text.equals("SLAYER QUEST FAILED!")) {
                NoSlayerQuestWarning.onSlayerFail();
            }
        });

        // ---- Reset on world load ----
        ClientLevelEvents.AFTER_CLIENT_LEVEL_CHANGE.register((client, world) -> {
            PigmanSwordTimer.time = 0;
            HolyIceTimer.time = 0;
            HolyIceTimer.activated = false;
            RagnarockAxeTimer.castTime = 0;
            RagnarockAxeTimer.duration = 0;
            RagnarockAxeTimer.cooldown = 0;
            RagnarockAxeTimer.cancelled = false;
            RagnarockAxeTimer.finished = false;
            ReaperArmorTimer.soundTime = 0;
            ReaperArmorTimer.activeTime = 0;
            ReaperArmorTimer.cooldownEnd = 0;
            EndStoneSwordTimer.time = 0;
            EndStoneSwordTimer.resistance = 0;
            EndStoneSwordTimer.damage = 0;
            SlayerBossDetector.reset();
        });
    }
}
