package top.babyzombie.addons.util.gui.overlay;

import java.util.Collections;
import java.util.List;

public final class TopOrderData {
    private TopOrderData() {}

    public enum OrderType { BUY, SELL }

    public record TopOrderEntry(
            String priceRaw,       // 原始文本如 "180,019.6 coins "
            String priceNumberOnly, // 纯数字如 "180,019.6"
            int amount,             // x 后面的数量
            int orderCount,         // 订单数
            OrderType type
    ) {}

    public record ParsedBazzarGui(
            String itemName,
            List<TopOrderEntry> buyOrders,
            List<TopOrderEntry> sellOrders,
            boolean valid
    ) {
        public static final ParsedBazzarGui EMPTY =
                new ParsedBazzarGui("", Collections.emptyList(), Collections.emptyList(), false);
    }
}
