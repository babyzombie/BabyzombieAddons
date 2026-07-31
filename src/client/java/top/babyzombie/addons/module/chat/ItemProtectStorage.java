package top.babyzombie.addons.module.chat;

import com.google.gson.reflect.TypeToken;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ItemStack;
import top.babyzombie.addons.util.DataPersistence;
import top.babyzombie.addons.util.ItemUtils;

import java.util.HashSet;
import java.util.Set;

/**
 * 自维护的物品保护存储。仅在 Skyblocker 未安装时使用。
 * 文件位置：config/babyzombieaddons/data/protected_items.json
 */
public final class ItemProtectStorage {

    private static final boolean DISABLED = FabricLoader.getInstance().isModLoaded("skyblocker");

    private static final Set<String> uuids = new HashSet<>();

    private ItemProtectStorage() {}

    public static void load() {
        if (DISABLED) return;
        Set<String> loaded = DataPersistence.load("protected_items.json",
                new TypeToken<Set<String>>(){}.getType());
        if (loaded != null) uuids.addAll(loaded);
    }

    public static void save() {
        if (DISABLED) return;
        DataPersistence.save("protected_items.json", uuids);
    }

    public static boolean contains(ItemStack stack) {
        if (DISABLED) return false;
        String uuid = ItemUtils.getItemUuid(stack);
        return uuid != null && uuids.contains(uuid);
    }

    public static void toggle(ItemStack stack) {
        if (DISABLED) return;
        String uuid = ItemUtils.getItemUuid(stack);
        if (uuid == null) return;
        boolean added;
        if (uuids.contains(uuid)) { uuids.remove(uuid); added = false; }
        else { uuids.add(uuid); added = true; }
        save();

        // 叮声反馈
        var player = Minecraft.getInstance().player;
        if (player != null) {
            player.playSound(SoundEvents.NOTE_BLOCK_PLING.value(), 0.3f, added ? 1.5f : 1.0f);
        }
    }

    public static boolean containsUuid(String uuid) {
        if (DISABLED) return false;
        return uuid != null && uuids.contains(uuid);
    }

    static { load(); }
}
