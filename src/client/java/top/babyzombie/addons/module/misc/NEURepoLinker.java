package top.babyzombie.addons.module.misc;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import top.babyzombie.addons.util.ChatUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.concurrent.CompletableFuture;

/**
 * 一键创建目录链接，将 Firmament 和 NEU 的物品库指向 Skyblocker 的 item-repo，
 * 从而只需下载一份物品库数据。
 */
public final class NEURepoLinker {

    private NEURepoLinker() {}

    /// 要创建链接的路径（相对于 gameDir）
    private static final Path[] LINK_PATHS = {
            Path.of(".firmament", "repo-extracted"),
            Path.of("config", "notenoughupdates", "repo")
    };

    /// Skyblocker 物品库路径（相对于 gameDir）
    private static final Path SKYBLOCKER_REPO = Path.of("config", "skyblocker", "item-repo");

    private static final boolean IS_WINDOWS = System.getProperty("os.name").toLowerCase().contains("win");

    // region 翻译键
    private static final String TK_START            = "babyzombieaddons.neulink.start";
    private static final String TK_DELETING         = "babyzombieaddons.neulink.deleting";
    private static final String TK_CREATING         = "babyzombieaddons.neulink.creating";
    private static final String TK_SUCCESS          = "babyzombieaddons.neulink.success";
    private static final String TK_DISABLE_SH       = "babyzombieaddons.neulink.success_disable_sh";
    private static final String TK_DISABLE_FIRM     = "babyzombieaddons.neulink.success_disable_firm";
    private static final String TK_HINT             = "babyzombieaddons.neulink.success_hint";
    private static final String TK_ALREADY_LINKED   = "babyzombieaddons.neulink.already_linked";
    private static final String TK_NO_SKYBLOCKER    = "babyzombieaddons.neulink.no_skyblocker_repo";
    private static final String TK_FAILED           = "babyzombieaddons.neulink.failed";
    // endregion

    /**
     * 按钮回调：关闭配置界面后异步执行链接创建。
     */
    public static void startLinking() {
        Minecraft.getInstance().setScreen(null);
        CompletableFuture.runAsync(() -> {
            try {
                doLink();
            } catch (Exception e) {
                show(TK_FAILED);
                show(Component.literal("§c" + e.getMessage()));
            }
        });
    }

    // ---- 核心流程 --------------------------------------------------------

    private static void doLink() throws IOException {
        Path gameDir = FabricLoader.getInstance().getGameDir();
        Path skyblockerPath = gameDir.resolve(SKYBLOCKER_REPO);

        show(TK_START);

        if (!Files.isDirectory(skyblockerPath)) {
            show(TK_NO_SKYBLOCKER);
            // 创建空目录让 junction 有合法目标，等 Skyblocker 启动下载后内容会自动出现
            try { Files.createDirectories(skyblockerPath); } catch (IOException ignored) {}
        }

        // 检查是否已经全部链接
        if (allLinked(gameDir)) {
            show(TK_ALREADY_LINKED);
            return;
        }

        // 1. 删除旧文件夹
        show(TK_DELETING);
        for (Path p : LINK_PATHS) {
            deleteIfNeeded(gameDir.resolve(p));
        }

        // 2. 创建链接
        show(TK_CREATING);
        boolean ok;
        if (IS_WINDOWS) {
            ok = createWindowsJunctions(gameDir);
        } else {
            ok = createUnixSymlinks(gameDir);
        }

        if (!ok) {
            show(TK_FAILED);
            return;
        }

        // 3. 成功
        show(TK_SUCCESS);
        showDisableInstructions();
    }

    // ---- 删除 ------------------------------------------------------------

    /**
     * 删除指定路径（如果是真实目录则递归删除，如果已是链接则跳过，稍后由创建覆盖）。
     */
    private static void deleteIfNeeded(Path path) {
        if (!Files.exists(path)) return;
        if (isLink(path)) {
            try { Files.delete(path); } catch (IOException ignored) {}
            return;
        }
        if (Files.isDirectory(path)) {
            try (var walk = Files.walk(path)) {
                walk.sorted(Comparator.reverseOrder())
                        .forEach(p -> {
                            try { Files.delete(p); } catch (IOException ignored) {}
                        });
            } catch (IOException ignored) {}
        } else {
            try { Files.delete(path); } catch (IOException ignored) {}
        }
    }

    // ---- Unix/Linux/Mac --------------------------------------------------

    private static boolean createUnixSymlinks(Path gameDir) {
        Path target = gameDir.resolve(SKYBLOCKER_REPO).toAbsolutePath().normalize();
        boolean allOk = true;
        for (Path linkRel : LINK_PATHS) {
            Path link = gameDir.resolve(linkRel);
            try {
                Files.createDirectories(link.getParent());
                if (Files.exists(link)) {
                    Files.delete(link);
                }
                Files.createSymbolicLink(link, target);
            } catch (IOException e) {
                allOk = false;
                show(Component.literal("§c" + linkRel + ": " + e.getMessage()));
            }
        }
        return allOk;
    }

    // ---- Windows (Junction) ----------------------------------------------

    /**
     * 通过 Start-Process cmd -Verb RunAs 提权执行 mklink /D。
     * 弹一次 UAC 窗口，用户点击「是」后完成。
     */
    private static boolean createWindowsJunctions(Path gameDir) {
        Path target = gameDir.resolve(SKYBLOCKER_REPO).toAbsolutePath().normalize();
        Path batFile = null;

        try {
            // 先确保父目录存在（非提权即可）
            for (Path linkRel : LINK_PATHS) {
                Files.createDirectories(gameDir.resolve(linkRel).getParent());
            }

            // 构建 mklink 命令，用 && 串联
            StringBuilder mklinkCmds = new StringBuilder();
            for (Path linkRel : LINK_PATHS) {
                Path link = gameDir.resolve(linkRel).toAbsolutePath().normalize();
                if (!mklinkCmds.isEmpty()) mklinkCmds.append(" && ");
                mklinkCmds.append(String.format("mklink /D \"%s\" \"%s\"",
                        link.toString(), target.toString()));
            }

            // 写到 bat 临时文件
            batFile = Files.createTempFile("bza-neulink-", ".bat");
            Files.writeString(batFile, "@echo off\r\n" + mklinkCmds.toString() + "\r\n");

            // Start-Process cmd -Verb RunAs -Wait，直接跑 bat
            String psCmd = String.format(
                    "Start-Process cmd -Verb RunAs -Wait -WindowStyle Hidden " +
                            "-ArgumentList '/c \"%s\"'",
                    batFile.toString()
            );

            new ProcessBuilder("powershell", "-Command", psCmd)
                    .redirectErrorStream(true)
                    .start()
                    .waitFor();

        } catch (Exception e) {
            return false;
        } finally {
            if (batFile != null) {
                try { Files.deleteIfExists(batFile); } catch (IOException ignored) {}
            }
        }

        return isLink(gameDir.resolve(LINK_PATHS[0]))
                && isLink(gameDir.resolve(LINK_PATHS[1]));
    }

    // ---- 后续指引 --------------------------------------------------------

    private static void showDisableInstructions() {
        String lang = Minecraft.getInstance().getLanguageManager().getSelected();
        boolean isChinese = lang.toLowerCase().contains("zh");

        // SkyHanni + Firmament 各一行：翻译文本 + 可点击链接
        showLine(TK_DISABLE_SH, "/skyhanni NEU Repo Auto Update");
        showLine(TK_DISABLE_FIRM, "/firmament search " + (isChinese ? "每次启动时自动下载" : "download new items"));
        show(TK_HINT);
    }

    /** 发送一行：翻译文本 + 空格 + 可点击指令。 */
    private static void showLine(String key, String command) {
        Component clickable = Component.literal("[点击打开设置]")
                .withStyle(style -> style
                        .withColor(ChatFormatting.AQUA)
                        .withUnderlined(true)
                        .withClickEvent(new ClickEvent.RunCommand(command.startsWith("/") ? command : "/" + command))
                );
        Minecraft.getInstance().execute(() ->
                ChatUtils.showMessage(Component.translatable(key).append(" ").append(clickable))
        );
    }

    /** 在主线程发送翻译后的系统消息。 */
    private static void show(String translationKey) {
        Minecraft.getInstance().execute(() -> ChatUtils.showTranslatable(translationKey));
    }

    /** 在主线程发送 Component 消息。 */
    private static void show(Component msg) {
        Minecraft.getInstance().execute(() -> ChatUtils.showMessage(msg));
    }

    // ---- 工具方法 --------------------------------------------------------

    private static boolean allLinked(Path gameDir) {
        for (Path p : LINK_PATHS) {
            if (!isLink(gameDir.resolve(p))) {
                return false;
            }
        }
        return true;
    }

    /**
     * 检查路径是否是符号链接或目录联结（Windows junction）。
     * Java 的 Files.isSymbolicLink() 无法识别 mklink /J 创建的联结，
     * 且 Files.getAttribute() 默认会跟随链接读到目标的属性。
     * 必须加 NOFOLLOW_LINKS 才能读到联结自身的 FILE_ATTRIBUTE_REPARSE_POINT。
     */
    private static boolean isLink(Path path) {
        try {
            if (Files.isSymbolicLink(path)) return true;
            if (!IS_WINDOWS) return false;
            int attrs = (int) Files.getAttribute(path, "dos:attributes", LinkOption.NOFOLLOW_LINKS);
            return (attrs & 0x400) != 0;
        } catch (Exception e) {
            return false;
        }
    }
}
