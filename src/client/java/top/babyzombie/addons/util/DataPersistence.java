package top.babyzombie.addons.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

/**
 * Simple JSON-based persistent data storage.
 * Data is stored under config/babyzombieaddons/data/ with optional subdirectories.
 */
public final class DataPersistence {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path DATA_DIR = FabricLoader.getInstance().getConfigDir()
            .resolve("babyzombieaddons").resolve("data");

    private DataPersistence() {}

    /** Save data to the root data directory. */
    public static <T> void save(String filename, T data) {
        save(null, filename, data);
    }

    /**
     * Save data under an optional subdirectory (e.g. player UUID or profile ID).
     */
    public static <T> void save(String subDir, String filename, T data) {
        try {
            Path dir = subDir != null ? DATA_DIR.resolve(subDir) : DATA_DIR;
            Files.createDirectories(dir);
            Files.writeString(dir.resolve(filename), GSON.toJson(data),
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /** Load data from the root data directory, or null if not found. */
    public static <T> T load(String filename, Class<T> clazz) {
        return load(null, filename, clazz);
    }

    /**
     * Load data from an optional subdirectory.
     */
    public static <T> T load(String subDir, String filename, Class<T> clazz) {
        Path dir = subDir != null ? DATA_DIR.resolve(subDir) : DATA_DIR;
        Path file = dir.resolve(filename);
        if (!Files.exists(file)) return null;
        try {
            return GSON.fromJson(Files.readString(file), clazz);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

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
            e.printStackTrace();
        }
    }
}
