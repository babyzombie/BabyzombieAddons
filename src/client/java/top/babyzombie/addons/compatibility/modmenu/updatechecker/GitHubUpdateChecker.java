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
    private final String mcVersion;

    public GitHubUpdateChecker(String modId) {
        this.currentVersion = FabricLoader.getInstance()
                .getModContainer(modId)
                .map(c -> c.getMetadata().getVersion().getFriendlyString())
                .orElse(null);
        this.mcVersion = FabricLoader.getInstance()
                .getModContainer("minecraft")
                .map(c -> c.getMetadata().getVersion().getFriendlyString())
                .orElse("");
    }

    @Override
    public UpdateInfo checkForUpdates() {
        if (currentVersion == null) return null;

        try {
            var release = UpdateCheckUtil.fetchLatest(mcVersion);
            if (release == null) return null;

            if (UpdateCheckUtil.isNewer(release.tag(), currentVersion)) {
                String updateMessage = release.tag() + "-mc" + mcVersion + " from " + release.source();
                return new SimpleUpdateInfo(
                        true,
                        release.downloadUrl(),
                        PRERELEASE_PATTERN.matcher(release.tag()).find() ? UpdateChannel.BETA : UpdateChannel.RELEASE,
                        updateMessage
                );
            }
        } catch (Exception ignored) {
        }
        return null;
    }
}
