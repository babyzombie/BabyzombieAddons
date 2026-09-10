package top.babyzombie.addons.module.misc;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import top.babyzombie.addons.util.ChatUtils;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * 断线原因黑名单枚举。
 * <p>
 * 匹配依据（两者命中其一即可）：
 * <ol>
 *   <li>翻译 key —— 递归收集整棵原因组件树的 key，不只看顶层：原版会把真实原因塞进 translatable
 *       参数里，例如登录失败是
 *       {@code translatable("disconnect.loginFailedInfo", translatable("disconnect.loginFailedInfo.invalidSession"))}，
 *       只读顶层 key（disconnect.loginFailedInfo）永远匹配不到 INVALID_SESSION。</li>
 *   <li>明文包含匹配 —— 覆盖服务端自定义文字原因（如 Hypixel 的原始踢出消息）。因为客户端渲染出的是
 *       <b>本地化</b>文案（中文环境下 invalidSession 渲染为"登录失败：无效会话…"，不含英文 pattern），
 *       所以额外把每一层 key 规范化（点/下划线/驼峰→空格）后一起参与匹配，
 *       例如 disconnect.loginFailedInfo.invalidSession → "disconnect login failed info invalid session"。</li>
 * </ol>
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

    /** 组件树递归深度上限：正常断线原因只有 1~2 层，仅防御服务端发来的畸形深层嵌套。 */
    private static final int MAX_COMPONENT_DEPTH = 32;

    /**
     * 判断断线原因是否命中该黑名单项。
     *
     * @param keys        原因组件树里收集到的全部翻译 key（含 translatable 参数里的嵌套 key）
     * @param searchLower 可搜索文本（已小写）：渲染明文 + 各层 key 的规范化形式
     */
    public boolean matches(Set<String> keys, String searchLower) {
        if (keys != null) {
            for (String k : this.keys) {
                if (keys.contains(k)) {
                    return true;
                }
            }
        }
        for (String p : this.patterns) {
            if (searchLower.contains(p)) {
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
        Set<String> keys = new HashSet<>();
        StringBuilder search = new StringBuilder();
        collect(reason, keys, search, 0);
        String searchLower = search.toString().toLowerCase(Locale.ROOT);
        for (DisconnectReason blacklisted : blacklist) {
            if (blacklisted.matches(keys, searchLower)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 递归收集组件树里的翻译 key 与可搜索文本（translatable 参数 + sibling 全都要走）。
     * <p>
     * 参数既可能是 Component（原版 loginFailedInfo 这种嵌套原因），也可能是 String/Number
     * （如 disconnect.genericReason 直接塞异常文本），两类都收集，否则网络层异常会被漏掉。
     */
    private static void collect(Component component, Set<String> keys, StringBuilder search, int depth) {
        if (component == null || depth > MAX_COMPONENT_DEPTH) {
            return;
        }
        // 本节点自身的渲染明文；翻译缺失时 getString() 返回 key 原文，也一并可搜
        search.append(component.getString()).append('\n');
        if (component.getContents() instanceof TranslatableContents contents) {
            keys.add(contents.getKey());
            search.append(normalizeKey(contents.getKey())).append('\n');
            if (contents.getFallback() != null) {
                search.append(contents.getFallback()).append('\n');
            }
            for (Object arg : contents.getArgs()) {
                if (arg instanceof Component argComponent) {
                    collect(argComponent, keys, search, depth + 1);
                } else {
                    // 纯文本参数（如 disconnect.genericReason 直接塞的异常信息）
                    search.append(arg).append('\n');
                }
            }
        }
        for (Component sibling : component.getSiblings()) {
            collect(sibling, keys, search, depth + 1);
        }
    }

    /**
     * 把翻译 key 拆成可读单词："disconnect.loginFailedInfo.invalidSession"
     * → "disconnect login failed info invalid session"，让英文 pattern 在任意客户端语言下也能命中 key 语义。
     */
    private static String normalizeKey(String key) {
        StringBuilder sb = new StringBuilder(key.length() + 16);
        for (int i = 0; i < key.length(); i++) {
            char c = key.charAt(i);
            if (c == '.' || c == '_' || c == '-' || c == '/') {
                sb.append(' ');
            } else if (Character.isUpperCase(c) && i > 0) {
                sb.append(' ').append(c);
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    @Override
    public String toString() {
        return ChatUtils.translate("config.babyzombieaddons.option.autoReconnectBlacklist." + name());
    }
}