package top.babyzombie.addons.util;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.Minecraft;

/**
 * Hypixel 服务器快捷连接。进入连接界面由 {@link ConnectScreen#startConnecting} 完成。
 */
public final class HypixelServer {

    public static final String ADDRESS = "hypixel.net";

    private HypixelServer() {}

    public static void join(Screen parent) {
        var mc = Minecraft.getInstance();
        ConnectScreen.startConnecting(parent, mc,
                ServerAddress.parseString(ADDRESS),
                new ServerData("Hypixel", ADDRESS, ServerData.Type.OTHER),
                false, null);
    }
}
