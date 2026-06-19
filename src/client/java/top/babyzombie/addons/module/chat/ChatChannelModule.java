package top.babyzombie.addons.module.chat;

import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.client.Minecraft;
import top.babyzombie.addons.config.ModConfigManager;
import top.babyzombie.addons.util.DataPersistence;
import top.babyzombie.addons.util.tracker.HypixelLocationTracker;

import java.util.Locale;
import java.util.regex.Pattern;

public final class ChatChannelModule {

    private static final Pattern CHANNEL_SWITCH_EN = Pattern.compile(
            "You (?:are|'re) now in the (GUILD|OFFICER|PARTY|SKYBLOCK CO-OP|ALL) channel!?",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern CHANNEL_SWITCH_CN = Pattern.compile(
            "你正处于(公会|公会管理频道|组队|空岛生存合作模式|所有)频道中[！!]?");

    private static final String DATA_FILE = "chat_channel.json";

    private static ChatChannel currentChannel;

    private ChatChannelModule() {}

    public static void init() {
        loadChannel();

        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            if (overlay) return;
            if (!HypixelLocationTracker.getInstance().isOnHypixel()) return;
            if (!ModConfigManager.get().chatChannel.chatChannelSwitcher) return;
            detectAndSetChannel(message.getString());
        });
    }

    private static void detectAndSetChannel(String text) {
        var m = CHANNEL_SWITCH_EN.matcher(text);
        if (m.find()) {
            setChannel(m.group(1));
            return;
        }
        m = CHANNEL_SWITCH_CN.matcher(text);
        if (m.find()) {
            setChannel(m.group(1));
        }
    }

    private static void setChannel(String name) {
        ChatChannel ch = switch (name.toUpperCase(Locale.ROOT)) {
            case "GUILD", "公会" -> ChatChannel.GC;
            case "OFFICER", "公会管理频道" -> ChatChannel.OC;
            case "PARTY", "组队" -> ChatChannel.PC;
            case "SKYBLOCK CO-OP", "空岛生存合作模式" -> ChatChannel.CC;
            case "ALL", "所有" -> ChatChannel.AC;
            default -> null;
        };
        if (ch != null && ch != currentChannel) {
            currentChannel = ch;
            saveChannel();
        }
    }

    private static void saveChannel() {
        if (currentChannel == null) return;
        var uuid = Minecraft.getInstance().getUser().getProfileId().toString();
        if (uuid == null || uuid.isEmpty()) return;
        DataPersistence.save(uuid, DATA_FILE, currentChannel);
    }

    private static void loadChannel() {
        var uuid = Minecraft.getInstance().getUser().getProfileId().toString();
        if (uuid == null || uuid.isEmpty()) return;
        currentChannel = DataPersistence.load(uuid, DATA_FILE, ChatChannel.class);
    }

    public static ChatChannel getCurrentChannel() {
        return currentChannel;
    }
}
