package top.babyzombie.addons.module.kuudra;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLevelEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.player.Player;
import top.babyzombie.addons.config.ModConfigManager;
import top.babyzombie.addons.mixin.render.PlayerTabOverlayAccessor;
import top.babyzombie.addons.util.ChatUtils;
import top.babyzombie.addons.util.render.GlowController;
import top.babyzombie.addons.util.tracker.HypixelLocationTracker;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 队友高亮 — 只在 Kuudra 中生效（不深度测试）。Kuudra 是私有实例，tab 列表里的玩家即队友；
 * 拿玩家实体的名字去 tab 里找，能找到说明是真人（过滤 Elle/商人等 NPC），给其施加彩色发光。
 * 与 fresh 等其它发光功能互不干扰：别人的发光不顶掉，清理时只关自己颜色还在的。
 */
public final class TeamHighlight {
    private TeamHighlight() {}

    /** 当前由本模块发光的玩家 UUID */
    private static final Set<UUID> glowing = ConcurrentHashMap.newKeySet();

    public static void init() {
        // 每 tick diff 更新发光状态
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            var cfg = ModConfigManager.get().kuudra;
            var world = client.level;
            int color = cfg.teamHighlightColor.getEffectiveColourRGB();
            if (!cfg.teamHighlight || !HypixelLocationTracker.getInstance().isInKuudra()) {
                clearGlows(world, color);
                return;
            }
            if (world == null) return;

            // tab 列表中的名字（真人玩家；Kuudra 是私有实例，tab 里即队友）
            var tabAccessor = (PlayerTabOverlayAccessor) client.gui.hud.getTabList();
            Set<String> tabNames = new HashSet<>();
            for (var info : tabAccessor.invokeGetPlayerInfos()) {
                tabNames.add(info.getProfile().name());
            }

            // 玩家实体名能在 tab 中找到 → 真人队友 → 发光
            Set<UUID> current = new HashSet<>();
            for (var p : world.players()) {
                if (p == client.player) continue; // 不给自己发光
                if (tabNames.contains(ChatUtils.stripColor(p.getName().getString()))) {
                    current.add(p.getUUID());
                }
            }

            for (UUID uuid : current) {
                var p = findPlayer(world, uuid);
                if (p == null) continue;
                // 已被其它功能发光（如 fresh）且颜色不是我们的 → 不顶掉，让给人家
                if (GlowController.shouldGlow(p) && GlowController.getGlowColor(p) != color) {
                    glowing.remove(uuid);
                    continue;
                }
                GlowController.setGlow(p, true, color, false);
                glowing.add(uuid);
            }
            for (UUID uuid : glowing) {
                var p = findPlayer(world, uuid);
                if (p == null) continue;
                if (!current.contains(uuid)) {
                    // 颜色仍是我们的才关闭，避免关掉 fresh 的发光
                    if (GlowController.getGlowColor(p) == color) {
                        GlowController.setGlow(p, false);
                    }
                    glowing.remove(uuid);
                }
            }
        });

        // 世界切换：发光数据由 GlowController 自行清理，这里只清本模块的集合
        ClientLevelEvents.AFTER_CLIENT_LEVEL_CHANGE.register((client, world) -> {
            glowing.clear();
        });
    }

    private static Player findPlayer(ClientLevel world, UUID uuid) {
        for (var p : world.players()) {
            if (p.getUUID().equals(uuid)) return p;
        }
        return null;
    }

    private static void clearGlows(ClientLevel world, int color) {
        if (world == null) return;
        for (UUID uuid : glowing) {
            var p = findPlayer(world, uuid);
            if (p != null && GlowController.getGlowColor(p) == color) {
                GlowController.setGlow(p, false);
            }
        }
        glowing.clear();
    }
}
