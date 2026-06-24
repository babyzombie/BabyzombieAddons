package top.babyzombie.addons.compatibility.modmenu.updatechecker;

import com.terraformersmc.modmenu.api.UpdateChannel;
import com.terraformersmc.modmenu.api.UpdateChecker;
import com.terraformersmc.modmenu.api.UpdateInfo;
import net.fabricmc.loader.api.FabricLoader;
import top.babyzombie.addons.util.UpdateCheckUtil;

import java.util.regex.Pattern;

/** ModMenu update checker using the shared {@link UpdateCheckUtil}. */
public class GitHubUpdateChecker implements UpdateChecker {

    private static final Pattern PRERELEASE_PATTERN = Pattern.compile("[.\\-](?:alpha|beta|pre|rc)\\d*", Pattern.CASE_INSENSITIVE);

    private final String currentVersion;

    public GitHubUpdateChecker(String modId) {
        this.currentVersion = FabricLoader.getInstance()
                .getModContainer(modId)
                .map(c -> c.getMetadata().getVersion().getFriendlyString())
                .orElse(null);
    }

    @Override
    public UpdateInfo checkForUpdates() {
        if (currentVersion == null) return null;

        try {
            var release = UpdateCheckUtil.fetchLatest();
            if (release == null) return null;

            if (UpdateCheckUtil.isNewer(release.tag(), currentVersion)) {
                return new SimpleUpdateInfo(
                        true,
                        release.downloadUrl(),
                        PRERELEASE_PATTERN.matcher(release.tag()).find() ? UpdateChannel.BETA : UpdateChannel.RELEASE
                );
            }
        } catch (Exception ignored) {
        }
        return null;
    }
}
