package top.babyzombie.addons.util;

import net.fabricmc.loader.api.FabricLoader;

/** 通用 HTTP 工具 */
public final class HttpUtils {
    private HttpUtils() {}

    /** 统一 UA：带 mod 名和版本，便于 Hypixel 识别流量来源（官方文档推荐） */
    public static final String USER_AGENT = "BabyzombieAddons/" +
            FabricLoader.getInstance().getModContainer("babyzombieaddons")
                    .map(c -> c.getMetadata().getVersion().getFriendlyString())
                    .orElse("unknown");
}
