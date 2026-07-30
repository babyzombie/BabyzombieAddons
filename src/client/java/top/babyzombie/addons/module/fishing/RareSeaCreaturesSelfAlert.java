package top.babyzombie.addons.module.fishing;

import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import top.babyzombie.addons.config.ModConfigManager;
import top.babyzombie.addons.util.ChatUtils;
import top.babyzombie.addons.util.tracker.HypixelLocationTracker;

/// 钓出稀有海怪提示：监听聊天栏中的海怪刷出消息，
/// 匹配到自己钓出的时弹 Title、播放音效并通知队伍。
public final class RareSeaCreaturesSelfAlert {

    private RareSeaCreaturesSelfAlert() {}

    public static void init() {
        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            if (overlay) return;

            var selfCfg = ModConfigManager.get().fishing.rareSeaCreatures.selfCaught;
            if (!selfCfg.enabled) return;
            if (!HypixelLocationTracker.getInstance().isInSkyblock()) return;

            String plain = ChatUtils.stripColor(message.getString()).trim();
            RareSeaCreatureDefinitions.SeaCreature def = RareSeaCreatureDefinitions.matchByCatchMessage(plain);
            if (def == null) return;

            var player = Minecraft.getInstance().player;
            if (player == null) return;

            // 1. Title 提示（无冷却）
            if (selfCfg.showTitle) {
                String titleKey = def.rarity == RareSeaCreatureDefinitions.Rarity.MYTHIC
                        ? "fishing.rareSeaCreaturesSelfAlert.title.mythic"
                        : "fishing.rareSeaCreaturesSelfAlert.title.legendary";
                ChatUtils.showTitle(
                        Component.translatable(titleKey).getString(),
                        def.rarity.titleColorCode + def.displayName,
                        0, 50, 10);
            }

            // 2. 声音提示（音符盒）
            if (selfCfg.playSound) {
                var pos = player.blockPosition();
                player.level().playLocalSound(
                        pos.getX(), pos.getY(), pos.getZ(),
                        SoundEvents.NOTE_BLOCK_CHIME.value(),
                        SoundSource.PLAYERS, 1.0f, 1.0f, false);
            }

            // 3. 队伍聊天通知（先坐标后内容，参照 sendCoords 格式）
            if (selfCfg.partyChat) {
                var pos = player.blockPosition();
                String msg = "x: " + pos.getX() + ", y: " + pos.getY()
                        + ", z: " + pos.getZ() + " [" + def.displayName + "]";
                ChatUtils.sendCommand("pc " + msg);
            }
        });
    }
}
