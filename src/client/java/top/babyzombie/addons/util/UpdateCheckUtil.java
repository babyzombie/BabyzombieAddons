package top.babyzombie.addons.util;

import com.google.gson.JsonParser;
import org.jetbrains.annotations.Nullable;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/** Shared fetch + version comparison logic used by both the in-game and ModMenu update checkers. */
public final class UpdateCheckUtil {
    private static final String GITHUB_API = "https://api.github.com/repos/babyzombie/BabyzombieAddons/releases/latest";
    private static final String GITEE_API = "https://gitee.com/api/v5/repos/Bluesky-kk/BabyzombieAddons/releases?per_page=1&page=1&direction=desc";
    private static final String GITHUB_DOWNLOAD = "https://github.com/babyzombie/BabyzombieAddons/releases/latest";
    private static final String GITEE_DOWNLOAD = "https://gitee.com/Bluesky-kk/BabyzombieAddons/releases/latest";

    private UpdateCheckUtil() {}

    /** Holds the latest release tag and where to download it. */
    public record ReleaseInfo(String tag, String downloadUrl) {}

    /**
     * Fetch the latest release info. Tries GitHub first, falls back to Gitee.
     *
     * @return tag (without leading 'v') and download URL, or null if both sources are unreachable
     */
    @Nullable
    public static ReleaseInfo fetchLatest() {
        try {
            return new ReleaseInfo(stripV(fetchFromUrl(GITHUB_API, false)), GITHUB_DOWNLOAD);
        } catch (Exception e) {
            try {
                return new ReleaseInfo(stripV(fetchFromUrl(GITEE_API, true)), GITEE_DOWNLOAD);
            } catch (Exception ex) {
                return null;
            }
        }
    }

    private static String stripV(String tag) {
        return tag.startsWith("v") ? tag.substring(1) : tag;
    }

    private static String fetchFromUrl(String url, boolean isArrayResponse) throws Exception {
        try (var http = HttpClient.newHttpClient()) {
            var req = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("User-Agent", "BabyzombieAddons-UpdateChecker")
                    .timeout(Duration.ofSeconds(15))
                    .build();
            var res = http.send(req, HttpResponse.BodyHandlers.ofString());
            if (res.statusCode() != 200)
                throw new RuntimeException("HTTP " + res.statusCode());
            var json = JsonParser.parseString(res.body());
            if (isArrayResponse) {
                var arr = json.getAsJsonArray();
                if (arr.isEmpty()) throw new RuntimeException("Empty release list");
                return arr.get(0).getAsJsonObject().get("tag_name").getAsString();
            }
            return json.getAsJsonObject().get("tag_name").getAsString();
        }
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
        // Shorter is stable and therefore newer: "2.0.0" > "2.0.0-alpha.4"
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
