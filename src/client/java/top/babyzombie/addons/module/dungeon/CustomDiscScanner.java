package top.babyzombie.addons.module.dungeon;

import net.fabricmc.loader.api.FabricLoader;
import org.lwjgl.stb.STBVorbis;
import org.lwjgl.stb.STBVorbisInfo;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import top.babyzombie.addons.config.ModConfig;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.file.*;

/**
 * 启动时扫描 {@code config/babyzombieaddons/custom_discs/}，
 * 解析 {@code N_Name.ogg} 格式的自定义唱片文件，
 * 缓存每个槽位的显示名和时长供 {@link ModConfig.MusicDisc} 查询。
 */
public final class CustomDiscScanner {

    private static final Path CUSTOM_DISCS_DIR = FabricLoader.getInstance().getConfigDir()
            .resolve("babyzombieaddons").resolve("custom_discs");
    private static final int SLOT_COUNT = 9;
    private static final CustomDiscInfo[] SLOT_INFO = new CustomDiscInfo[SLOT_COUNT];

    private CustomDiscScanner() {}

    /** 扫描目录，填充槽位信息。启动时调用一次。 */
    public static void init() {
        for (int i = 0; i < SLOT_COUNT; i++) {
            SLOT_INFO[i] = null;
        }

        try {
            Files.createDirectories(CUSTOM_DISCS_DIR);
        } catch (IOException ignored) {
            return;
        }

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(CUSTOM_DISCS_DIR, "*.ogg")) {
            for (Path entry : stream) {
                String filename = entry.getFileName().toString();
                // 格式：{n}_{DisplayName}.ogg
                int underscore = filename.indexOf('_');
                if (underscore <= 0) continue;

                String slotStr = filename.substring(0, underscore);
                int slot;
                try {
                    slot = Integer.parseInt(slotStr);
                } catch (NumberFormatException ignored) {
                    continue;
                }
                if (slot < 1 || slot > SLOT_COUNT) continue;

                String rest = filename.substring(underscore + 1);
                if (!rest.toLowerCase().endsWith(".ogg") || rest.length() <= 4) continue;
                String displayName = rest.substring(0, rest.length() - 4).replace('_', ' ');

                int durationSeconds = probeDuration(entry);
                SLOT_INFO[slot - 1] = new CustomDiscInfo(displayName, durationSeconds);
            }
        } catch (IOException ignored) {
        }
    }

    /** 返回自定义唱片文件的存放目录。 */
    public static Path getCustomDiscsDir() {
        return CUSTOM_DISCS_DIR;
    }

    /** 返回指定自定义槽位对应的文件路径（如果存在），否则返回 null。 */
    public static Path getPath(ModConfig.MusicDisc disc) {
        int idx = disc.ordinal() - ModConfig.MusicDisc.CUSTOM_1.ordinal();
        if (idx < 0 || idx >= SLOT_COUNT) return null;
        if (SLOT_INFO[idx] == null) return null;
        return findFileForSlot(idx + 1);
    }

    /** 是否有任意一个自定义槽位存在文件。 */
    public static boolean hasAnyActive() {
        for (int i = 0; i < SLOT_COUNT; i++) {
            if (SLOT_INFO[i] != null) return true;
        }
        return false;
    }

    /** 返回指定枚举值的槽位信息，null = 未安装。 */
    public static CustomDiscInfo getInfo(ModConfig.MusicDisc disc) {
        int idx = disc.ordinal() - ModConfig.MusicDisc.CUSTOM_1.ordinal();
        if (idx < 0 || idx >= SLOT_COUNT) return null;
        return SLOT_INFO[idx];
    }

    /**
     * 在目录中查找指定槽位的 .ogg 文件。
     * 由 CustomDiscPackResources 用于把资源路径映射到真实文件。
     */
    static Path findFileForSlot(int slot) {
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(CUSTOM_DISCS_DIR,
                slot + "_*.ogg")) {
            var it = stream.iterator();
            if (it.hasNext()) return it.next();
        } catch (IOException ignored) {
        }
        return null;
    }

    /** 使用 STBVorbis 快速读取 Ogg 文件的总采样数，计算时长（秒）。 */
    private static int probeDuration(Path path) {
        try {
            byte[] data = Files.readAllBytes(path);
            ByteBuffer buf = MemoryUtil.memAlloc(data.length);
            try {
                buf.put(data);
                buf.flip();

                try (MemoryStack stack = MemoryStack.stackPush()) {
                    IntBuffer error = stack.callocInt(1);
                    long handle = STBVorbis.stb_vorbis_open_memory(buf, error, null);
                    if (handle == MemoryUtil.NULL) return 0;

                    try {
                        int samples = STBVorbis.stb_vorbis_stream_length_in_samples(handle);
                        STBVorbisInfo info = STBVorbisInfo.create();
                        STBVorbis.stb_vorbis_get_info(handle, info);
                        int sampleRate = info.sample_rate();
                        if (sampleRate > 0 && samples > 0) {
                            return samples / sampleRate;
                        }
                    } finally {
                        STBVorbis.stb_vorbis_close(handle);
                    }
                }
            } finally {
                MemoryUtil.memFree(buf);
            }
        } catch (IOException ignored) {
        }
        return 0;
    }

    /**
     * 自定义唱片槽位的已解析元数据。
     * @param displayName 用于 GUI 显示的友好名称
     * @param durationSeconds 音频时长（秒），无法解析时为 0
     */
    public record CustomDiscInfo(String displayName, int durationSeconds) {}
}
