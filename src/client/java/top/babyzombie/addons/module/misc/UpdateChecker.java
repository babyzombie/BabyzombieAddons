package top.babyzombie.addons.module.misc;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import top.babyzombie.addons.util.UpdateCheckUtil;
import top.babyzombie.addons.config.ModConfigManager;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLevelEvents;

import java.net.URI;

public final class UpdateChecker {
    private static boolean checked;
    private static final String RELEASES_GITHUB_URL =
            "https://github.com/babyzombie/BabyzombieAddons/releases/latest";
    private static final String RELEASES_GITEE_URL =
            "https://gitee.com/Bluesky-kk/BabyzombieAddons/releases/latest";

    private UpdateChecker() {}

    public static void init() {
        ClientLevelEvents.AFTER_CLIENT_LEVEL_CHANGE.register((client, level) -> {
            if (checked) return;
            if (!ModConfigManager.get().general.updateChecker) return;
            checked = true;
            var version = FabricLoader.getInstance()
                    .getModContainer("babyzombieaddons")
                    .map(c -> c.getMetadata().getVersion().getFriendlyString())
                    .orElse("0.0.0");
            var mcVersion = FabricLoader.getInstance()
                    .getModContainer("minecraft")
                    .map(c -> c.getMetadata().getVersion().getFriendlyString())
                    .orElse("");
            check(client, version, mcVersion);
        });
    }

    private static void check(Minecraft client, String currentVersion, String mcVersion) {
        var thread = new Thread(() -> {
            var release = UpdateCheckUtil.fetchLatest(mcVersion);
            if (release == null) return;
            if (!UpdateCheckUtil.isNewer(release.tag(), currentVersion)) return;

            var msg = Component.translatable(
                            "babyzombieaddons.update.new_version", release.tag(), currentVersion);
            if (release.body() != null && !release.body().isBlank()) {
                msg = msg.withStyle(style -> style
                        .withHoverEvent(new HoverEvent.ShowText(
                                Component.literal(release.body()))));
            }
            var finalMsg = msg.append(" ")
                    .append(createDownloadLink(
                            "babyzombieaddons.update.download.github", RELEASES_GITHUB_URL))
                    .append(" ")
                    .append(createDownloadLink(
                            "babyzombieaddons.update.download.gitee", RELEASES_GITEE_URL));
            client.execute(() -> {
                var player = client.player;
                if (player != null) player.sendSystemMessage(finalMsg);
            });
        }, "BZA-UpdateCheck");
        thread.setDaemon(true);
        thread.start();
    }

    private static Component createDownloadLink(String translationKey, String url) {
        return Component.translatable(translationKey)
                .withStyle(style -> style
                        .withClickEvent(new ClickEvent.OpenUrl(URI.create(url)))
                        .withHoverEvent(new HoverEvent.ShowText(
                                Component.translatable("babyzombieaddons.update.open_url", url))));
    }
}
