package top.babyzombie.addons.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.Comparator;
import java.util.stream.Stream;

/**
 * Simple JSON-based persistent data storage.
 * Data is stored under config/babyzombieaddons/data/ with optional subdirectories.
 * Subdirectory keys use forward slashes to nest, e.g. "&lt;uuid&gt;/&lt;profileId&gt;" for per-profile data.
 */
public final class DataPersistence {

    private static final Logger LOGGER = LoggerFactory.getLogger("BabyzombieAddons/DataPersistence");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_ROOT = FabricLoader.getInstance().getConfigDir()
            .resolve("babyzombieaddons");
    private static final Path DATA_DIR = CONFIG_ROOT.resolve("data");

    private DataPersistence() {}

    // ---- save ----

    /** Save data to the root data directory. */
    public static <T> void save(String filename, T data) {
        save(null, filename, data);
    }

    /**
     * Save data under an optional subdirectory (e.g. "uuid/profileId" for per-profile data).
     * Written atomically: a temp file is written first, then renamed into place.
     */
    public static <T> void save(String subDir, String filename, T data) {
        try {
            Path dir = subDir != null ? DATA_DIR.resolve(subDir) : DATA_DIR;
            Files.createDirectories(dir);
            Path file = dir.resolve(filename);
            Path tmp = dir.resolve(filename + ".tmp");
            Files.writeString(tmp, GSON.toJson(data),
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            try {
                Files.move(tmp, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING); // fallback: FS without atomic move
            }
        } catch (IOException e) {
            LOGGER.error("Failed to save {}", filename, e);
        }
    }

    // ---- load ----

    /** Load data from the root data directory, or null if not found. */
    public static <T> T load(String filename, Class<T> clazz) {
        return load(null, filename, (Type) clazz);
    }

    /** Load data from the root data directory, or null if not found. Supports generic types via Type. */
    public static <T> T load(String filename, Type type) {
        return load(null, filename, type);
    }

    /** Load data from an optional subdirectory, or null if not found. */
    public static <T> T load(String subDir, String filename, Class<T> clazz) {
        return load(subDir, filename, (Type) clazz);
    }

    /** Load data from an optional subdirectory, or null if not found. Supports generic types via Type. */
    public static <T> T load(String subDir, String filename, Type type) {
        Path dir = subDir != null ? DATA_DIR.resolve(subDir) : DATA_DIR;
        Path file = dir.resolve(filename);
        if (!Files.exists(file)) return null;
        try {
            return GSON.fromJson(Files.readString(file), type);
        } catch (Exception e) {
            LOGGER.error("Failed to load {}", file, e);
            return null;
        }
    }

    // ---- migration helpers ----

    /** Move a file within the data root. Old file is only removed after the new one exists. */
    public static boolean move(String oldSubDir, String oldFile, String newSubDir, String newFile) {
        return moveTo(DATA_DIR, oldSubDir, oldFile, DATA_DIR, newSubDir, newFile);
    }

    /**
     * Move a file from the config root (config/babyzombieaddons/) into the data root.
     * Used by one-time migrations of legacy flat files.
     */
    public static boolean moveFromConfigRoot(String oldFile, String newSubDir, String newFile) {
        return moveTo(CONFIG_ROOT, null, oldFile, DATA_DIR, newSubDir, newFile);
    }

    private static boolean moveTo(Path oldRoot, String oldSubDir, String oldFile,
                                  Path newRoot, String newSubDir, String newFile) {
        try {
            Path oldPath = (oldSubDir != null ? oldRoot.resolve(oldSubDir) : oldRoot).resolve(oldFile);
            if (!Files.exists(oldPath)) return true; // nothing to migrate
            Path newDir = newSubDir != null ? newRoot.resolve(newSubDir) : newRoot;
            Files.createDirectories(newDir);
            Files.move(oldPath, newDir.resolve(newFile), StandardCopyOption.REPLACE_EXISTING);
            LOGGER.info("Migrated {} → {}", oldPath, newDir.resolve(newFile));
            return true;
        } catch (IOException e) {
            LOGGER.error("Failed to move {} → {}", oldFile, newFile, e);
            return false;
        }
    }

    /**
     * Move all files under an old data subdirectory into a new one (recursively).
     * The old directory is removed once empty. Safe even if the target already exists.
     */
    public static boolean moveDirectory(String oldSubDir, String newSubDir) {
        Path oldDir = DATA_DIR.resolve(oldSubDir);
        if (!Files.isDirectory(oldDir)) return true;
        Path newDir = DATA_DIR.resolve(newSubDir);
        try {
            Files.createDirectories(newDir);
            try (Stream<Path> walk = Files.walk(oldDir)) {
                for (Path p : walk.filter(Files::isRegularFile).toList()) {
                    Path rel = oldDir.relativize(p);
                    Path target = newDir.resolve(rel);
                    Files.createDirectories(target.getParent());
                    Files.move(p, target, StandardCopyOption.REPLACE_EXISTING);
                }
            }
            // remove the now-empty old directory tree (deepest first)
            try (Stream<Path> walk = Files.walk(oldDir)) {
                for (Path p : walk.sorted(Comparator.comparingInt(Path::getNameCount).reversed()).toList()) {
                    Files.deleteIfExists(p);
                }
            }
            LOGGER.info("Migrated directory {} → {}", oldSubDir, newSubDir);
            return true;
        } catch (IOException e) {
            LOGGER.error("Failed to move directory {} → {}", oldSubDir, newSubDir, e);
            return false;
        }
    }

    // ---- misc ----

    /** Check whether a file exists in the root data directory. */
    public static boolean exists(String filename) {
        return exists(null, filename);
    }

    /** Check whether a file exists under an optional subdirectory. */
    public static boolean exists(String subDir, String filename) {
        Path dir = subDir != null ? DATA_DIR.resolve(subDir) : DATA_DIR;
        return Files.exists(dir.resolve(filename));
    }

    /** Delete a file from the root data directory. */
    public static void delete(String filename) {
        delete(null, filename);
    }

    /** Delete a file from an optional subdirectory. */
    public static void delete(String subDir, String filename) {
        try {
            Path dir = subDir != null ? DATA_DIR.resolve(subDir) : DATA_DIR;
            Files.deleteIfExists(dir.resolve(filename));
        } catch (IOException e) {
            LOGGER.error("Failed to delete {}", filename, e);
        }
    }
}
