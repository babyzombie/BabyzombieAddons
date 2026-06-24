package top.babyzombie.addons.compatibility.modmenu.updatechecker;

import com.terraformersmc.modmenu.api.UpdateChannel;
import com.terraformersmc.modmenu.api.UpdateInfo;

record SimpleUpdateInfo(
        boolean isUpdateAvailable,
        String downloadLink,
        UpdateChannel updateChannel
) implements UpdateInfo {
    @Override
    public String getDownloadLink() {
        return downloadLink;
    }

    @Override
    public UpdateChannel getUpdateChannel() {
        return updateChannel;
    }
}
