package top.babyzombie.addons.util;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import org.jetbrains.annotations.Nullable;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/** Shared fetch + version comparison logic used by both the in-game and ModMenu update checkers. */
public final class UpdateCheckUtil {

    // Match the MC version in an asset filename, e.g. babyzombieaddons-2.7.0+1.21.4.jar
    private static final String GITHUB_API = "https://api.github.com/repos/babyzombie/BabyzombieAddons/releases?per_page=5";
    private static final String GITEE_API = "https://gitee.com/api/v5/repos/Bluesky-kk/BabyzombieAddons/releases?per_page=5&page=1&direction=desc";

    private UpdateCheckUtil() {}

    /** Holds the latest release tag and where to download it. */
    public record ReleaseInfo(String tag, String downloadUrl) {}

    /**
     * Fetch the latest release that has an asset matching the given MC version.
     * An asset matches if its filename contains the MC version (e.g. {@code mc26.1.2}).
     * Tries GitHub first, falls back to Gitee.
     *
     * @param mcVersion the current MC version string from FabricLoader, e.g. "26.1.2"
     * @return tag (without leading 'v') and the matching asset's download URL, or null
     */
    @Nullable
    public static ReleaseInfo fetchLatest(String mcVersion) {
        try {
            return fetchFromGitHub(mcVersion);
        } catch (Exception e) {
            try {
                return fetchFromGitee(mcVersion);
            } catch (Exception ex) {
                return null;
            }
        }
    }

    private static ReleaseInfo fetchFromGitHub(String mcVersion) throws Exception {
        try (var http = HttpClient.newHttpClient()) {
            var req = HttpRequest.newBuilder()
                    .uri(URI.create(GITHUB_API))
                    .header("User-Agent", "BabyzombieAddons-UpdateChecker")
                    .header("Accept", "application/vnd.github+json")
                    .timeout(Duration.ofSeconds(15))
                    .build();
            var res = http.send(req, HttpResponse.BodyHandlers.ofString());
            if (res.statusCode() != 200) throw new RuntimeException("HTTP " + res.statusCode());

            for (JsonElement el : JsonParser.parseString(res.body()).getAsJsonArray()) {
                var release = el.getAsJsonObject();
                var found = findMatchingAsset(release.getAsJsonArray("assets"), mcVersion);
                if (found != null) {
                    String tag = release.get("tag_name").getAsString();
                    return new ReleaseInfo(stripV(tag), found);
                }
            }
            throw new RuntimeException("No release with asset matching MC " + mcVersion);
        }
    }

    private static ReleaseInfo fetchFromGitee(String mcVersion) throws Exception {
        try (var http = HttpClient.newHttpClient()) {
            var req = HttpRequest.newBuilder()
                    .uri(URI.create(GITEE_API))
                    .header("User-Agent", "BabyzombieAddons-UpdateChecker")
                    .timeout(Duration.ofSeconds(15))
                    .build();
            var res = http.send(req, HttpResponse.BodyHandlers.ofString());
            if (res.statusCode() != 200) throw new RuntimeException("HTTP " + res.statusCode());

            var arr = JsonParser.parseString(res.body()).getAsJsonArray();
            if (arr.isEmpty()) throw new RuntimeException("Empty release list");

            for (JsonElement el : arr) {
                var release = el.getAsJsonObject();
                var found = findMatchingAsset(release.getAsJsonArray("assets"), mcVersion);
                if (found != null) {
                    String tag = release.get("tag_name").getAsString();
                    return new ReleaseInfo(stripV(tag), found);
                }
            }
            throw new RuntimeException("No release with asset matching MC " + mcVersion);
        }
    }

    /** Find a non-sources asset whose filename contains the MC version. Returns its download URL or null. */
    @Nullable
    private static String findMatchingAsset(JsonElement assetsEl, String mcVersion) {
        if (assetsEl == null) return null;
        for (JsonElement a : assetsEl.getAsJsonArray()) {
            var asset = a.getAsJsonObject();
            String name = asset.get("name").getAsString();
            if (!name.endsWith(".jar") || name.endsWith("-sources.jar")) continue;
            if (name.contains(mcVersion)) {
                return asset.get("browser_download_url").getAsString();
            }
        }
        return null;
    }

    private static String stripV(String tag) {
        return tag.startsWith("v") ? tag.substring(1) : tag;
    }

    /** Compare two versions, e.g. "2.0.0" &gt; "2.0.0-alpha.4". */
    public static boolean isNewer(String latest, String current) {
        var a = splitVersion(latest);
        var b = splitVersion(current);
        int n = Math.min(a.length, b.length);
        for (int i = 0; i < n; i++) {
            int cmp = comparePart(a[i], b[i]);
            if (cmp != 0) return cmp > 0;
        }
        return a.length < b.length;
    }

    private static String[] splitVersion(String v) {
        return v.split("[.\\-]");
    }

    private static int comparePart(String a, String b) {
        Integer ia = tryInt(a), ib = tryInt(b);
        if (ia != null && ib != null) return ia.compareTo(ib);
        if (ia != null) return 1;
        if (ib != null) return -1;
        return a.compareTo(b);
    }

    private static Integer tryInt(String s) {
        try { return Integer.parseInt(s); }
        catch (NumberFormatException e) { return null; }
    }
}
