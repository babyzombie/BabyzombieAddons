package top.babyzombie.addons.module.misc.bazaar;

import top.babyzombie.addons.config.ModConfigManager;
import top.babyzombie.addons.config.SkyblockConfig.BazzarTopOrders;
import top.babyzombie.addons.module.misc.sign.SignQuickButtons;

import java.util.List;

/** Bazaar 数量输入告示牌的快捷数量按钮:共享引擎 {@link SignQuickButtons} 的 bazaar 适配器 */
public final class BazaarSignQuickAmountButtons {

    /** 告示牌箭头行(全字匹配) */
    private static final String SIGN_CARET_LINE = "^^^^^^^^^^^^^^^";

    private BazaarSignQuickAmountButtons() {}

    public static void init() {
        SignQuickButtons.register(FEATURE);
    }

    private static BazzarTopOrders getCfg() {
        try { return ModConfigManager.get().skyblock.bazzarTopOrders; } catch (Exception e) { return null; }
    }

    private static final SignQuickButtons.Feature FEATURE = new SignQuickButtons.Feature() {
        @Override
        public boolean enabled() {
            BazzarTopOrders cfg = getCfg();
            return cfg != null && cfg.signQuickAmountsEnabled;
        }

        @Override
        public List<SignQuickButtons.Spec> specs() {
            BazzarTopOrders cfg = getCfg();
            if (cfg == null || cfg.signQuickAmounts == null) return List.of();
            return cfg.signQuickAmounts.stream()
                    .map(a -> new SignQuickButtons.Spec(a.displayText(), String.valueOf(a.amount())))
                    .toList();
        }

        @Override
        public boolean matches(String[] messages) {
            return isAmountSignIdentity(messages);
        }

        @Override
        public int maxRows() { return 2; }

        @Override
        public boolean autoCloseSign() {
            BazzarTopOrders cfg = getCfg();
            return cfg != null && cfg.signQuickAmountsAutoClose;
        }
    };

    /** 匹配 Bazaar 输入数量告示牌的固定内容:^^^ 箭头行 / Enter amount / to order 或 to sell(全字匹配),不管第一行 */
    private static boolean isAmountSignIdentity(String[] messages) {
        if (messages.length < 4) return false;
        if (!SIGN_CARET_LINE.equals(SignQuickButtons.plainLine(messages[1]))) return false;
        if (!"Enter amount".equals(SignQuickButtons.plainLine(messages[2]))) return false;
        String last = SignQuickButtons.plainLine(messages[3]);
        return "to order".equals(last) || "to sell".equals(last);
    }
}