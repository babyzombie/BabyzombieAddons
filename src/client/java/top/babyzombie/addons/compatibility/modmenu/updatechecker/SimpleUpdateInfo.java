package top.babyzombie.addons.compatibility.modmenu.updatechecker;

import com.terraformersmc.modmenu.api.UpdateChannel;
import com.terraformersmc.modmenu.api.UpdateInfo;
import net.minecraft.network.chat.Component;

record SimpleUpdateInfo(
        boolean isUpdateAvailable,
        String downloadLink,
        UpdateChannel updateChannel,
        String updateMessage
) implements UpdateInfo {
    @Override
    public String getDownloadLink() {
        return downloadLink;
    }

    @Override
    public UpdateChannel getUpdateChannel() {
        return updateChannel;
    }

    @Override
    public Component getUpdateMessage() {
        return updateMessage != null ? Component.literal(updateMessage) : null;
    }
}
