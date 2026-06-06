package top.babyzombie.addons.module.slayer;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientEntityEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientWorldEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import top.babyzombie.addons.event.PlaySoundEvents;
import top.babyzombie.addons.util.ChatUtils;

public final class SlayerModule {
    private SlayerModule() {}

    public static void init() {
        // Initialize sub-modules (register their event listeners)
        PigmanSwordTimer.init();
        RagnarockAxeTimer.init();
        ReaperArmorTimer.init();
        EndStoneSwordTimer.init();
        ReheatedGummyPolarBearTimer.init();
        NoSlayerQuestWarning.init();
        BloodfiendLowHPBox.init();
        EffigyDisplay.init();
        SlayerBossDetector.init();
        SlayerBossBox.init();
        SlayerHUD.init();

        // ---- Wire sound events ----
        PlaySoundEvents.BEFORE_PLAY.register(sound -> {
            var snd = sound.getSound();
            if (snd == null) return false;
            String path = snd.getLocation().getPath().toLowerCase();
            String name;
            if (path.contains("zpigangry") || path.contains("angry") || path.contains("zombified_piglin")) {
                name = "zpigangry";
            } else if (path.contains("zombie/remedy") || path.contains("remedy")) {
                name = "zombie/remedy";
            } else if (path.contains("drink") || path.contains("generic/drink")) {
                name = "drink";
            } else {
                return false;
            }
            PigmanSwordTimer.onSound(name);
            float pitch = 1f;
            try { pitch = sound.getPitch(); } catch (Exception ignored) {}
            ReaperArmorTimer.onSound(name, pitch);
            return false;
        });

        // ---- Wire entity death for NoSlayerQuestWarning ----
        ClientEntityEvents.ENTITY_UNLOAD.register((entity, world) -> {
            if (entity == null || entity.isAlive()) return;
            NoSlayerQuestWarning.onEntityDeath();
        });

        // ---- Slayer quest lifecycle ----
        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            if (overlay) return;
            String text = message.getString();
            if (text.contains("SLAYER QUEST STARTED")) {
                NoSlayerQuestWarning.onSlayerStart();
            } else if (ChatUtils.stripColor(text).contains("SLAYER QUEST FAILED")) {
                NoSlayerQuestWarning.onSlayerFail();
            }
        });

        // ---- Reset on world load ----
        ClientWorldEvents.AFTER_CLIENT_WORLD_CHANGE.register((client, world) -> {
            if (world == null) return;
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
