package top.babyzombie.addons.module.kuudra;

import java.util.regex.Pattern;

/**
 * Kuudra 副本聊天触发消息匹配工具 — 集中管理 Elle 台词与系统行动播报。
 *
 * <p>Elle 台词在 stripColor 后格式为 {@code [NPC] Elle: <台词>}。玩家聊天消息永远
 * 以 {@code 玩家名: }（或 {@code [rank] 玩家名: }、{@code Party > 玩家名: }）开头，
 * 不会出现 {@code [NPC] Elle: } 前缀（玩家名不含空格与方括号），因此锚定该前缀
 * 即可防止玩家在聊天栏复制 Elle 台词造成误判。</p>
 */
public final class KuudraChatLines {
    private KuudraChatLines() {}

    // ── 完整消息已验证（此前用 equals 匹配在游戏中工作正常） ──

    /** 开场钓鱼：Elle 去钓 Kuudra（wiki 的 Kuudra's End 版本已过时，以游戏内 "and" 版本为准） */
    private static final Pattern FISH_UP_KUUDRA = Pattern.compile(
            "^\\[NPC] Elle: Okay adventurers, I will go and fish up Kuudra!$");

    /** P2 开始：补给收集完毕 */
    private static final Pattern SUPPLIES_COLLECTED = Pattern.compile(
            "^\\[NPC] Elle: OMG! Great work collecting my supplies!$");

    /** P2 结束：弩炮建成（"blow"/"blows" 两个变体） */
    private static final Pattern BALLISTA_READY = Pattern.compile(
            "^\\[NPC] Elle: Phew! The Ballista is finally ready! It should be strong enough to tank Kuudra's blows? now!$");

    /** P4 开始：最后一击 */
    private static final Pattern P4_START = Pattern.compile(
            "^\\[NPC] Elle: POW! SURELY THAT'S IT! I don't think he has any more in him!$");

    /** P4 稍后（击杀确认后） */
    private static final Pattern KNEW_YOU_COULD_DO_IT = Pattern.compile(
            "^\\[NPC] Elle: I knew you could do it!$");

    /** T5 真身巢穴（Infernal 难度） */
    private static final Pattern TRUE_LAIR = Pattern.compile(
            "^\\[NPC] Elle: What just happened\\? Is this Kuudra's real lair\\?$");

    // ── 完整句未逐一验证，锚定前缀 + 台词开头 ──

    /** 登台提示（完整句为 "Head over to the main platform, I will join you when I get a bite!"） */
    private static final Pattern HEAD_TO_PLATFORM = Pattern.compile(
            "^\\[NPC] Elle: Head over to the main platform\\b.*$");

    /** 没钓到补给 */
    private static final Pattern NOT_AGAIN = Pattern.compile(
            "^\\[NPC] Elle: Not again!$");

    /** 战斗结束（完整句为 "Good job everyone. A hard fought battle ..."） */
    private static final Pattern GOOD_JOB = Pattern.compile(
            "^\\[NPC] Elle: Good job everyone\\..*$");

    public static boolean isFishUpKuudra(String text) {
        return FISH_UP_KUUDRA.matcher(text).matches();
    }

    public static boolean isSuppliesCollected(String text) {
        return SUPPLIES_COLLECTED.matcher(text).matches();
    }

    public static boolean isBallistaReady(String text) {
        return BALLISTA_READY.matcher(text).matches();
    }

    public static boolean isP4Start(String text) {
        return P4_START.matcher(text).matches();
    }

    public static boolean isKnewYouCouldDoIt(String text) {
        return KNEW_YOU_COULD_DO_IT.matcher(text).matches();
    }

    public static boolean isTrueLair(String text) {
        return TRUE_LAIR.matcher(text).matches();
    }

    public static boolean isHeadToPlatform(String text) {
        return HEAD_TO_PLATFORM.matcher(text).matches();
    }

    public static boolean isNotAgain(String text) {
        return NOT_AGAIN.matcher(text).matches();
    }

    public static boolean isGoodJob(String text) {
        return GOOD_JOB.matcher(text).matches();
    }

    // ── 系统行动播报（非 Elle 台词，Kuudra 副本通用触发消息） ──

    /** 玩家被 Kuudra 吞掉（"<玩家名> has been eaten by Kuudra!"） */
    private static final Pattern EATEN_BY_KUUDRA = Pattern.compile(
            "([0-9a-zA-Z_]{2,24}) has been eaten by Kuudra!");

    /** 玩家打爆 Kuudra 的 pod（"<玩家名> destroyed one of Kuudra's pods"） */
    private static final Pattern DESTROYED_POD = Pattern.compile(
            "([0-9a-zA-Z_]{2,24}) destroyed one of Kuudra's pods");

    public static boolean isEatenByKuudra(String text) {
        return EATEN_BY_KUUDRA.matcher(text).find();
    }

    public static boolean isDestroyedPod(String text) {
        return DESTROYED_POD.matcher(text).find();
    }
}
