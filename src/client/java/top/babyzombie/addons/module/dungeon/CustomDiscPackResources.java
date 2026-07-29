package top.babyzombie.addons.module.dungeon;

import com.mojang.logging.LogUtils;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.AbstractPackResources;
import net.minecraft.server.packs.PackLocationInfo;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.IoSupplier;
import org.slf4j.Logger;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

/**
 * 虚拟资源包：将 {@code config/babyzombieaddons/custom_discs/} 下的
 * 自定义 .ogg 文件暴露为 MC 资源系统的一部分，
 * 使 {@code SoundManager} 能够正常加载和播放它们。
 */
public final class CustomDiscPackResources extends AbstractPackResources {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String NAMESPACE = "babyzombieaddons";

    public CustomDiscPackResources(PackLocationInfo location) {
        super(location);
    }

    private byte[] packIconBytes;

    @Override
    public IoSupplier<InputStream> getRootResource(String... path) {
        String joined = String.join("/", path);
        if ("pack.mcmeta".equals(joined)) {
            String json = "{\"pack\":{"
                    + "\"description\":{\"translate\":\"resourcepack.babyzombieaddons.custom_discs.description\"},"
                    + "\"min_format\":[84],"
                    + "\"max_format\":[84]}}";
            return () -> new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));
        }
        if ("pack.png".equals(joined)) {
            if (packIconBytes == null) {
                packIconBytes = composeIcon();
            }
            if (packIconBytes != null) {
                byte[] icon = packIconBytes;
                return () -> new ByteArrayInputStream(icon);
            }
        }
        return null;
    }

    /**
     * 合成资源包图标：原版唱片纹理为底，右下角叠加模组图标。
     * 如果原版纹理不可用，则仅使用模组图标。
     */
    private static byte[] composeIcon() {
        try {
            // 有自定义唱片 → 唱片纹理；空 → 唱片机纹理
            boolean hasAnyDisc = CustomDiscScanner.hasAnyActive();
            String baseTexture = hasAnyDisc
                    ? "/assets/minecraft/textures/item/music_disc_5.png"
                    : "/assets/minecraft/textures/block/jukebox_top.png";
            BufferedImage disc = loadClasspathImage(baseTexture);
            // jukebox_top 也加载不到就用唱片纹理兜底
            if (disc == null && !hasAnyDisc) {
                disc = loadClasspathImage("/assets/minecraft/textures/item/music_disc_5.png");
            }
            BufferedImage badge = loadClasspathImage("/assets/babyzombieaddons/icon.png");
            if (badge == null) return null;

            int size = 128;
            BufferedImage result = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = result.createGraphics();

            if (disc != null) {
                // 像素纹理用最近邻保持锐利
                g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
                g.drawImage(disc, 0, 0, size, size, null);
            } else {
                // 回退：简单深色圆角底
                g.setColor(new Color(35, 35, 42));
                g.fillRoundRect(4, 4, size - 8, size - 8, 20, 20);
            }

            // 右下角模组图标（平滑缩放）
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            int badgeSize = (int) (size * 0.38);
            int margin = size / 14;
            int bx = size - badgeSize - margin;
            int by = size - badgeSize - margin;
            g.drawImage(badge, bx, by, badgeSize, badgeSize, null);

            g.dispose();

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ImageIO.write(result, "PNG", out);
            return out.toByteArray();
        } catch (Exception e) {
            LOGGER.warn("[CustomDisc] Failed to compose pack icon: {}", e.toString());
        }
        return null;
    }

    private static BufferedImage loadClasspathImage(String classpath) {
        try (InputStream in = CustomDiscPackResources.class.getResourceAsStream(classpath)) {
            if (in != null) return ImageIO.read(in);
        } catch (IOException ignored) {
        }
        return null;
    }

    @Override
    public IoSupplier<InputStream> getResource(PackType type, Identifier location) {
        if (type != PackType.CLIENT_RESOURCES) return null;
        if (!NAMESPACE.equals(location.getNamespace())) return null;

        // 路径格式：sounds/custom_disc_N.ogg
        String path = location.getPath();
        if (!path.endsWith(".ogg")) return null;
        String filename = path.substring(path.lastIndexOf('/') + 1);
        if (!filename.startsWith("custom_disc_")) return null;

        String numPart = filename.substring("custom_disc_".length(),
                filename.length() - ".ogg".length());
        int slot;
        try {
            slot = Integer.parseInt(numPart);
        } catch (NumberFormatException e) {
            return null;
        }
        if (slot < 1 || slot > 9) return null;

        Path file = CustomDiscScanner.findFileForSlot(slot);
        if (file != null && Files.exists(file)) {
            return IoSupplier.create(file);
        }
        return null;
    }

    @Override
    public void listResources(PackType type, String namespace, String directory,
                              ResourceOutput output) {
        if (type != PackType.CLIENT_RESOURCES) return;
        if (!NAMESPACE.equals(namespace)) return;
        if (!"sounds".equals(directory)) return;

        for (int slot = 1; slot <= 9; slot++) {
            Path file = CustomDiscScanner.findFileForSlot(slot);
            if (file != null && Files.exists(file)) {
                Identifier id = Identifier.tryBuild(NAMESPACE,
                        "sounds/custom_disc_" + slot + ".ogg");
                if (id != null) {
                    output.accept(id, IoSupplier.create(file));
                }
            }
        }
    }

    @Override
    public Set<String> getNamespaces(PackType type) {
        return type == PackType.CLIENT_RESOURCES
                ? Set.of(NAMESPACE) : Set.of();
    }

    @Override
    public void close() {
        // nothing to clean up
    }
}
