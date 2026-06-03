package top.babyzombie.addons.module.slayer;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientEntityEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientWorldEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import top.babyzombie.addons.event.PlaySoundEvents;
import top.babyzombie.addons.util.BeaconBeamRenderer;

public final class SlayerModule {
    private SlayerModule() {}

    public static void init() {
        // Initialize sub-modules (register their event listeners)
        BossDetector.init();
        PigmanSwordTimer.init();
        RagnarockAxeTimer.init();
        ReaperArmorTimer.init();
        EndStoneSwordTimer.init();
        ReheatedGummyPolarBearTimer.init();
        NoSlayerQuestWarning.init();
        SlayerBossRenderer.init();
        SlayerHUD.init();
        EffigyDisplay.init();

        // ---- Wire sound events ----
        PlaySoundEvents.BEFORE_PLAY.register(sound -> {
            // Detect sound by parsing toString representation
            String raw = sound.toString();
            String name;
            if (raw.contains("zombified_piglin") || raw.contains("zpigangry")) {
                name = "zpigangry";
            } else if (raw.contains("zombie.remedy")) {
                name = "zombie.remedy";
            } else if (raw.contains("generic.drink") || raw.contains("random.drink")) {
                name = "drink";
            } else {
                return false;
            }
            PigmanSwordTimer.onSound(name);
            ReaperArmorTimer.onSound(name, sound.getPitch());
            return false; // Don't cancel the sound
        });

        // ---- Wire entity death for NoSlayerQuestWarning ----
        ClientEntityEvents.ENTITY_UNLOAD.register((entity, world) -> {
            if (entity == null || entity.isAlive()) return;
            // Entity died/unloaded - check if we need to warn about missing slayer quest
            NoSlayerQuestWarning.onEntityDeath();
        });

        // ---- Boss detector tick ----
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            BossDetector.tick();
        });

        // ---- Slayer quest start detection ----
        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            if (overlay) return;
            String text = message.getString();
            if (text.contains("SLAYER QUEST STARTED")) {
                NoSlayerQuestWarning.onSlayerStart();
            }
        });

        // ---- Reset on world load ----
        ClientWorldEvents.AFTER_CLIENT_WORLD_CHANGE.register((client, world) -> {
            if (world == null) return;
            BossDetector.reset();
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
            BeaconBeamRenderer.clearAll();
        });
    }
}
