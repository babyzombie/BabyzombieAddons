package top.babyzombie.addons.module.kuudra;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLevelEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.player.Player;
import top.babyzombie.addons.config.ModConfigManager;
import top.babyzombie.addons.util.render.GlowController;
import top.babyzombie.addons.util.tracker.HypixelLocationTracker;
import top.babyzombie.addons.util.tracker.PartyTracker;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 队友高亮 — 只在 Kuudra 中生效。Kuudra 是私有实例，队伍成员即队友；
 * 玩家实体的 UUID 与 PartyTracker（Hypixel ModAPI）里的队伍成员 UUID 比对，
 * 命中即真人队友（NPC 实体不在队伍里），给其施加彩色发光。
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

            // 队伍成员 UUID（Hypixel ModAPI）。不主动 request——Kuudra 开局时
            // 别处（重开判定）会拉取一次，用那次数据即可持续整局（成员一局内不变）
            var tracker = PartyTracker.getInstance();
            var info = tracker.getLastInfo();
            if (info == null) return; // 队伍信息还没拉到，等其它功能拉取后自动生效

            // 玩家实体 UUID 在队伍成员中 → 真人队友 → 发光
            Set<UUID> members = info.members();
            Set<UUID> current = new HashSet<>();
            for (var p : world.players()) {
                if (p == client.player) continue; // 不给自己发光
                if (members.contains(p.getUUID())) {
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
