package top.babyzombie.addons.module.misc.bank;

import top.babyzombie.addons.config.ModConfigManager;
import top.babyzombie.addons.config.SkyblockConfig.Bank;
import top.babyzombie.addons.module.misc.sign.SignQuickButtons;

import java.util.List;

/** 银行存/取款金额告示牌的快捷金额按钮:共享引擎 {@link SignQuickButtons} 的 bank 适配器 */
public final class BankSignQuickAmountButtons {

    private BankSignQuickAmountButtons() {}

    public static void init() {
        SignQuickButtons.register(FEATURE);
    }

    private static Bank getCfg() {
        try { return ModConfigManager.get().skyblock.bank; } catch (Exception e) { return null; }
    }

    private static final SignQuickButtons.Feature FEATURE = new SignQuickButtons.Feature() {
        @Override
        public boolean enabled() {
            Bank cfg = getCfg();
            return cfg != null && cfg.signQuickAmountsEnabled;
        }

        @Override
        public List<SignQuickButtons.Spec> specs() {
            Bank cfg = getCfg();
            if (cfg == null || cfg.signQuickAmounts == null) return List.of();
            return cfg.signQuickAmounts.stream()
                    .map(a -> new SignQuickButtons.Spec(a.displayText(), String.valueOf(a.amount())))
                    .toList();
        }

        @Override
        public boolean matches(String[] messages) {
            return isBankSignIdentity(messages);
        }

        @Override
        public int maxRows() { return 3; } // 默认 18 个金额按钮 2 行放不下,需 3 行

        @Override
        public boolean autoCloseSign() {
            Bank cfg = getCfg();
            return cfg != null && cfg.signQuickAmountsAutoClose;
        }
    };

    /** 匹配银行金额输入告示牌的固定内容:第3行 Enter the amount / 第4行 to withdraw 或 to deposit(全字匹配),不管前两行 */
    private static boolean isBankSignIdentity(String[] messages) {
        if (messages.length < 4) return false;
        if (!"Enter the amount".equals(SignQuickButtons.plainLine(messages[2]))) return false;
        String last = SignQuickButtons.plainLine(messages[3]);
        return "to withdraw".equals(last) || "to deposit".equals(last);
    }
}
