package top.babyzombie.addons.module.misc;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import top.babyzombie.addons.util.ChatUtils;

import java.util.List;
import java.util.Locale;

/**
 * 断线原因黑名单枚举。
 * <p>
 * 匹配依据：组件翻译 key（服务端发来的可翻译原因）+ 解析后的明文（小写包含匹配，
 * 覆盖服务端自定义文字原因，如 Hypixel 的原始踢出消息）。
 * 命中黑名单的原因将跳过自动重连，或按 autoReconnect.blacklistMaxRetries 限制尝试次数。
 * 展示名称走 draggable list 语言 key：config.babyzombieaddons.option.autoReconnectBlacklist.<NAME>
 */
public enum DisconnectReason {

    AUTH_SERVERS_DOWN(new String[]{
            "multiplayer.disconnect.authservers_down",
            "disconnect.loginFailedInfo.serversUnavailable"
    }, new String[]{
            "authentication servers are down",
            "authentication servers are currently not reachable",
            "auth servers are down"
    }),
    INVALID_SESSION(new String[]{
            "disconnect.loginFailedInfo.invalidSession",
            "multiplayer.disconnect.invalid_session",
            // 登录/会话验证失败类："Failed to verify username!"（未验证用户名，通常需重新登录）
            "multiplayer.disconnect.unverified_username"
    }, new String[]{
            "invalid session"
    }),
    ACCOUNT_BANNED(new String[]{
            "disconnect.loginFailedInfo.userBanned"
    }, new String[]{
            "you are banned from playing online"
    }),
    // Xbox 隐私设置 "You can join multiplayer games" 被阻止时，登录阶段被踢：
    // disconnect.loginFailedInfo.insufficientPrivileges = "Multiplayer is disabled. Please check your Microsoft account settings."
    MULTIPLAYER_DISABLED(new String[]{
            "disconnect.loginFailedInfo.insufficientPrivileges"
    }, new String[]{
            "multiplayer is disabled",
            "insufficient privileges"
    }),
    SERVER_BANNED(new String[]{
            "multiplayer.disconnect.banned",
            "multiplayer.disconnect.banned.reason",
            "multiplayer.disconnect.banned.reason.default",
            "multiplayer.disconnect.banned_ip.reason",
            "multiplayer.disconnect.ip_banned"
    }, new String[]{
            // 宽匹配：覆盖 "You are banned from this server" / "banned from Hypixel" / "Your account is banned" 等措辞
            "banned from",
            "is banned"
    }),
    NOT_WHITELISTED(new String[]{
            "multiplayer.disconnect.not_whitelisted"
    }, new String[]{
            "not white-listed",
            "not whitelisted"
    }),
    SERVER_FULL(new String[]{
            "multiplayer.disconnect.server_full"
    }, new String[]{
            "the server is full"
    }),
    SERVER_CLOSED(new String[]{
            "multiplayer.disconnect.server_shutdown",
            "multiplayer.disconnect.server_closed"
    }, new String[]{
            "server closed",
            // Hypixel Alpha 测试服关闭（截图确认原文 "The Hypixel Alpha server is currently closed!"）
            "currently closed"
    }),
    VERSION_INCOMPATIBLE(new String[]{
            "multiplayer.disconnect.outdated_client",
            "multiplayer.disconnect.outdated_server",
            "multiplayer.disconnect.incompatible"
    }, new String[]{
            "incompatible client",
            "incompatible server",
            "outdated client",
            "outdated server"
    }),
    DUPLICATE_LOGIN(new String[]{
            "multiplayer.disconnect.duplicate_login"
    }, new String[]{
            "logged in from another location"
    }),
    EXPIRED_PUBLIC_KEY(new String[]{
            "multiplayer.disconnect.expired_public_key",
            "multiplayer.disconnect.invalid_public_key_signature",
            "multiplayer.disconnect.invalid_public_key_signature.new",
            "chat.disabled.expiredProfileKey"
    }, new String[]{
            "expired profile public key",
            "invalid signature for profile public key",
            "check that your system time"
    }),
    TIMEOUT(new String[]{
            "disconnect.timeout"
    }, new String[]{
            "timed out",
            "connection timed out",
            "connect timed out"
    }),
    PROTOCOL_ERROR(new String[]{
            "disconnect.packetError",
            "disconnect.exceeded_packet_rate",
            "disconnect.overflow",
            "multiplayer.disconnect.invalid_packet"
    }, new String[]{
            "network protocol error",
            "packet overflow",
            "exceeded packet rate",
            "invalid packet"
    }),
    // 网络层自发异常（Connection.exceptionCaught → disconnect.genericReason + 异常文本，旧版本显示
    // "Internal Exception: ..."）。与 PROTOCOL_ERROR 不同：这类多为瞬断，重连通常能恢复，
    // 是否加入黑名单、设几次重试上限请按需选择。
    NETWORK_ERROR(new String[]{
            // 服务器未发断开包直接断流 / 通道静默关闭（无原因时的兜底 "Disconnected"）
            "disconnect.endOfStream",
            "multiplayer.disconnect.generic"
    }, new String[]{
            "internal exception",
            "connection reset",
            "connection refused",
            "forcibly closed",
            "socketexception"
    });

    private final String[] keys;
    private final String[] patterns;

    DisconnectReason(String[] keys, String[] patterns) {
        this.keys = keys;
        this.patterns = patterns;
    }

    /**
     * 判断断线原因是否命中该黑名单项。
     *
     * @param key        原因组件的翻译 key，非可翻译组件时为 null
     * @param plainLower 原因组件的解析明文（已小写）
     */
    public boolean matches(String key, String plainLower) {
        if (key != null) {
            for (String k : keys) {
                if (k.equals(key)) {
                    return true;
                }
            }
        }
        for (String p : patterns) {
            if (plainLower.contains(p)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 断线原因是否命中黑名单任意一项。黑名单为空或原因为 null 时恒为 false。
     */
    public static boolean matchesAny(List<DisconnectReason> blacklist, Component reason) {
        if (blacklist == null || blacklist.isEmpty() || reason == null) {
            return false;
        }
        String key = reason.getContents() instanceof TranslatableContents contents ? contents.getKey() : null;
        String plain = reason.getString().toLowerCase(Locale.ROOT);
        for (DisconnectReason blacklisted : blacklist) {
            if (blacklisted.matches(key, plain)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public String toString() {
        return ChatUtils.translate("config.babyzombieaddons.option.autoReconnectBlacklist." + name());
    }
}