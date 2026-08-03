package top.babyzombie.addons.util;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.jetbrains.annotations.Nullable;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * 更新检查：查询 Modrinth API 获取当前 MC 版本下的最新正式版。
 * ModMenu 内置的 Modrinth 检查与游戏内聊天提示共用同一数据源。
 */
public final class UpdateCheckUtil {

    /** Modrinth 项目 slug，必须与 Modrinth 项目 URL 一致（ModMenu 内置检查也依赖它）。 */
    public static final String MODRINTH_SLUG = "babyzombieaddons";
    private static final String MODRINTH_API =
            "https://api.modrinth.com/v2/project/" + MODRINTH_SLUG + "/version";
    private static final String MODRINTH_PAGE = "https://modrinth.com/mod/" + MODRINTH_SLUG;

    private UpdateCheckUtil() {}

    /** 最新版本信息：完整版本号（如 3.4.1-mc26.1.2）、下载页面、changelog。 */
    public record ReleaseInfo(String versionNumber, String downloadUrl, String body) {
        /** "3.4.1-mc26.1.2" -> "3.4.1"；没有 mc 后缀时原样返回。 */
        public String baseVersion() {
            return versionNumber.replaceFirst("-mc\\d[\\d.]*(-rc\\.\\d+)?$", "");
        }
    }

    /**
     * 查询 Modrinth 上当前 MC 版本下最新的 listed release 版本。
     *
     * @param mcVersion 当前 MC 版本，如 "26.1.2"
     * @return 最新版本信息，查询失败或没有匹配版本时返回 null
     */
    @Nullable
    public static ReleaseInfo fetchLatest(String mcVersion) {
        try {
            String query = "?game_versions="
                    + URLEncoder.encode("[\"" + mcVersion + "\"]", StandardCharsets.UTF_8);
            try (var http = HttpClient.newHttpClient()) {
                var req = HttpRequest.newBuilder()
                        .uri(URI.create(MODRINTH_API + query))
                        .header("User-Agent", "BabyzombieAddons-UpdateChecker")
                        .header("Accept", "application/json")
                        .timeout(Duration.ofSeconds(15))
                        .build();
                var res = http.send(req, HttpResponse.BodyHandlers.ofString());
                if (res.statusCode() != 200) return null;

                JsonElement best = null;
                String bestDate = "";
                for (JsonElement el : JsonParser.parseString(res.body()).getAsJsonArray()) {
                    var version = el.getAsJsonObject();
                    if (!"listed".equals(getString(version, "status"))) continue;
                    if (!"release".equals(getString(version, "version_type"))) continue;
                    String date = getString(version, "date_published");
                    if (date == null) continue;
                    if (best == null || date.compareTo(bestDate) > 0) {
                        best = el;
                        bestDate = date;
                    }
                }
                if (best == null) return null;

                var obj = best.getAsJsonObject();
                String versionNumber = getString(obj, "version_number");
                if (versionNumber == null) return null;
                return new ReleaseInfo(
                        versionNumber,
                        MODRINTH_PAGE + "/version/" + versionNumber,
                        getChangelog(obj));
            }
        } catch (Exception e) {
            return null;
        }
    }

    @Nullable
    private static String getString(JsonObject obj, String key) {
        var el = obj.get(key);
        return (el == null || el.isJsonNull()) ? null : el.getAsString();
    }

    @Nullable
    private static String getChangelog(JsonObject version) {
        var el = version.get("changelog");
        if (el == null || el.isJsonNull()) return null;
        String raw = el.getAsString();
        if (raw.isBlank()) return null;
        return stripMarkdown(raw);
    }

    /** Convert markdown into plain text suitable for in-game hover display. */
    private static String stripMarkdown(String md) {
        var sb = new StringBuilder();
        for (String line : md.split("\n", -1)) {
            // Remove leading markdown headers (##, ###, etc.)
            line = line.replaceAll("^#{1,6}\\s*", "");
            // Remove bold/italic markers
            line = line.replaceAll("\\*\\*?(.+?)\\*\\*?", "$1");
            line = line.replaceAll("__(.+?)__", "$1");
            // Remove inline code
            line = line.replaceAll("`([^`]+)`", "$1");
            // Remove links, keep text [text](url)
            line = line.replaceAll("\\[([^]]+)]\\([^)]+\\)", "$1");
            // Remove images ![alt](url)
            line = line.replaceAll("!\\[([^]]*)]\\([^)]+\\)", "$1");
            sb.append(line).append('\n');
        }
        // Strip leading/trailing whitespace and collapse excess blank lines
        return sb.toString()
                .replaceAll("\n{4,}", "\n\n\n")
                .trim();
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
