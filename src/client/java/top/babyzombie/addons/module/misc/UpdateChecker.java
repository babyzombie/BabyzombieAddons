package top.babyzombie.addons.module.misc;

import com.google.gson.JsonParser;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import top.babyzombie.addons.config.ModConfigManager;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public final class UpdateChecker {
    private static boolean checked;
    private static final String API_URL =
            "https://api.github.com/repos/babyzombie/BabyzombieAddons/releases/latest";
    private static final String RELEASES_GITHUB_URL =
            "https://github.com/babyzombie/BabyzombieAddons/releases/latest";
    private static final String RELEASES_GITEE_URL =
            "https://gitee.com/Bluesky-kk/BabyzombieAddons/releases/latest";

    private UpdateChecker() {}

    public static void init() {
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            if (checked) return;
            if (!ModConfigManager.get().general.updateChecker) return;
            checked = true;
            var version = FabricLoader.getInstance()
                    .getModContainer("babyzombieaddons")
                    .map(c -> c.getMetadata().getVersion().getFriendlyString())
                    .orElse("0.0.0");
            check(client, version);
        });
    }

    private static void check(Minecraft client, String currentVersion) {
        var thread = new Thread(() -> {
            try (var http = HttpClient.newHttpClient()) {
                var req = HttpRequest.newBuilder()
                        .uri(URI.create(API_URL))
                        .header("User-Agent", "BabyzombieAddons-UpdateChecker")
                        .timeout(Duration.ofSeconds(10))
                        .build();
                var res = http.send(req, HttpResponse.BodyHandlers.ofString());
                if (res.statusCode() != 200) return;
                var tag = JsonParser.parseString(res.body())
                        .getAsJsonObject().get("tag_name").getAsString();
                var latest = tag.startsWith("v") ? tag.substring(1) : tag;

                if (isNewer(latest, currentVersion)) {
                    var msg = Component.translatable(
                                    "babyzombieaddons.update.new_version", latest, currentVersion)
                            .append(" ")
                            .append(createDownloadLink(
                                    "babyzombieaddons.update.download.github", RELEASES_GITHUB_URL))
                            .append(" ")
                            .append(createDownloadLink(
                                    "babyzombieaddons.update.download.gitee", RELEASES_GITEE_URL));
                    client.execute(() -> {
                        var player = client.player;
                        if (player != null) player.displayClientMessage(msg, false);
                    });
                }
            } catch (Exception ignored) {
                // Network errors silently ignored
            }
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

    private static boolean isNewer(String latest, String current) {
        var a = splitVersion(latest);
        var b = splitVersion(current);
        int n = Math.min(a.length, b.length);
        for (int i = 0; i < n; i++) {
            int cmp = comparePart(a[i], b[i]);
            if (cmp != 0) return cmp > 0;
        }
        // Shorter is stable and therefore newer: "2.0.0" > "2.0.0-alpha.4"
        return a.length < b.length;
    }

    private static String[] splitVersion(String v) {
        // "2.0.0-alpha.4" → ["2","0","0","alpha","4"]
        return v.split("[.\\-]");
    }

    private static int comparePart(String a, String b) {
        Integer ia = tryInt(a), ib = tryInt(b);
        if (ia != null && ib != null) return ia.compareTo(ib);
        if (ia != null) return 1;  // numeric > string
        if (ib != null) return -1; // string < numeric
        return a.compareTo(b);
    }

    private static Integer tryInt(String s) {
        try { return Integer.parseInt(s); }
        catch (NumberFormatException e) { return null; }
    }
}
