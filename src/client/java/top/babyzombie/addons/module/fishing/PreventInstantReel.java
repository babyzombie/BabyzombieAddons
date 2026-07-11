package top.babyzombie.addons.module.fishing;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.FishingRodItem;
import top.babyzombie.addons.config.ModConfigManager;
import top.babyzombie.addons.util.ServerTick;
import top.babyzombie.addons.util.tracker.HypixelLocationTracker;

/// 防止抛竿后瞬间收杆：抛竿后的延迟窗口内取消右键收杆。
public final class PreventInstantReel {

    /// 抛竿时间戳（毫秒），-1 表示未在钓鱼
    private static long castTime = -1;
    /// 上一 tick 是否在钓鱼
    private static boolean wasFishing;

    private PreventInstantReel() {}

    public static void init() {
        // 追踪抛竿时刻
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            var player = client.player;
            if (player == null) return;

            boolean isFishing = player.fishing != null;
            if (isFishing && !wasFishing) {
                castTime = ServerTick.getTime();
            }
            if (!isFishing) {
                castTime = -1;
            }
            wasFishing = isFishing;
        });

        // 拦截右键收杆
        UseItemCallback.EVENT.register((player, world, hand) -> {
            var cfg = ModConfigManager.get().fishing;
            if (!cfg.preventInstantReel) return InteractionResult.PASS;
            if (!HypixelLocationTracker.getInstance().isInSkyblock()) return InteractionResult.PASS;
            if (HypixelLocationTracker.getInstance().isInKuudra()) return InteractionResult.PASS;
            if (player.fishing == null) return InteractionResult.PASS;
            if (castTime < 0) return InteractionResult.PASS;

            var held = player.getItemInHand(hand);
            if (!(held.getItem() instanceof FishingRodItem)) return InteractionResult.PASS;

            if (ServerTick.getTime() - castTime < cfg.preventInstantReelDelay) {
                return InteractionResult.FAIL;
            }
            return InteractionResult.PASS;
        });
    }
}
