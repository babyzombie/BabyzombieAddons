package top.babyzombie.addons.module.chat;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.world.item.ItemStack;
import top.babyzombie.addons.util.ItemUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.UUID;

/**
 * 物品保护桥接。优先级：Skyblocker → Firmament → 自身配置。
 * 所有反射 Method/Field 在静态初始化时缓存，避免每次调用都查找。
 */
public final class ItemProtectBridge {

    private static final boolean SKYBLOCKER = FabricLoader.getInstance().isModLoaded("skyblocker");
    private static final boolean FIRMAMENT = FabricLoader.getInstance().isModLoaded("firmament");

    private ItemProtectBridge() {}

    public static boolean needsOwnProtection() {
        return !SKYBLOCKER && !FIRMAMENT;
    }

    public static boolean isProtected(ItemStack stack) {
        String uuid = ItemUtils.getItemUuid(stack);
        if (uuid == null || uuid.isEmpty()) return false;

        if (SKYBLOCKER) { Boolean r = SkyblockerReflect.contains(uuid); if (r != null) return r; }
        if (FIRMAMENT) { Boolean r = FirmamentReflect.contains(uuid); if (r != null) return r; }
        return ItemProtectStorage.containsUuid(uuid);
    }

    public static void toggle(ItemStack stack) {
        String uuid = ItemUtils.getItemUuid(stack);
        if (uuid == null || uuid.isEmpty()) return;

        if (SKYBLOCKER && SkyblockerReflect.ready()) { SkyblockerReflect.toggle(stack); return; }
        if (FIRMAMENT && FirmamentReflect.ready()) { FirmamentReflect.toggle(uuid); return; }

        // 反射不可用或 mod 未装 → 自维护
        ItemProtectStorage.toggle(stack);
    }

    // ========== Skyblocker 反射（缓存 Method/Field）==========

    private static final class SkyblockerReflect {
        private static final Class<?> CONFIG_MANAGER;
        private static final Method GET_CONFIG;
        private static final Method HANDLE_KEY_PRESSED; // ItemProtection.handleKeyPressed(ItemStack)
        private static final Field GENERAL_FIELD;
        private static final Field PROTECTED_ITEMS_FIELD;
        private static final Method CONTAINS;

        static {
            Class<?> cm = null, itemProtection = null;
            Method get = null, handleKey = null, contains = null;
            Field genF = null, itemsF = null;
            try {
                cm = Class.forName("de.hysky.skyblocker.config.SkyblockerConfigManager");
                get = cm.getMethod("get");
                Class<?> cfg = get.getReturnType();
                genF = cfg.getField("general");
                Class<?> genCfg = genF.getType();
                itemsF = genCfg.getField("protectedItems");
                contains = itemsF.getType().getMethod("contains", Object.class);
                // ItemProtection.handleKeyPressed(ItemStack) —— 走 Skyblocker 自己的完整切换流程
                itemProtection = Class.forName("de.hysky.skyblocker.skyblock.item.ItemProtection");
                handleKey = itemProtection.getMethod("handleKeyPressed", net.minecraft.world.item.ItemStack.class);
            } catch (Exception ignored) {}
            CONFIG_MANAGER = cm;
            GET_CONFIG = get;
            HANDLE_KEY_PRESSED = handleKey;
            GENERAL_FIELD = genF;
            PROTECTED_ITEMS_FIELD = itemsF;
            CONTAINS = contains;
        }

        static boolean ready() { return GET_CONFIG != null && CONTAINS != null; }

        static Boolean contains(String uuid) {
            if (!ready()) return null;
            try {
                Object config = GET_CONFIG.invoke(null);
                Object general = GENERAL_FIELD.get(config);
                Object items = PROTECTED_ITEMS_FIELD.get(general);
                return (boolean) CONTAINS.invoke(items, uuid);
            } catch (Exception e) { return null; }
        }

        // 调 Skyblocker 自己的 ItemProtection.handleKeyPressed，有完整的消息和音效反馈
        static void toggle(ItemStack stack) {
            if (HANDLE_KEY_PRESSED == null) return;
            try {
                HANDLE_KEY_PRESSED.invoke(null, stack);
            } catch (Exception ignored) {}
        }
    }

    // ========== Firmament 反射（缓存 Method/Field）==========

    private static final class FirmamentReflect {
        private static final Object SLOT_LOCKING;
        private static final Method GET_LOCKED_UUIDS;
        private static final Method CONTAINS;
        private static final Method ADD;
        private static final Method REMOVE;
        private static final Method MARK_DIRTY;
        private static final Method PLAY_SUCCESS;  // CommonSoundEffects.playSuccess()

        static {
            Object slotLocking = null;
            Method getLockedUUIDs = null, contains = null, add = null, remove = null, markDirty = null;
            Method playSuccess = null;
            try {
                Class<?> c = Class.forName("moe.nea.firmament.features.inventory.SlotLocking");
                slotLocking = c.getField("INSTANCE").get(null);
                getLockedUUIDs = c.getMethod("getLockedUUIDs");
                Class<?> setClass = getLockedUUIDs.getReturnType();
                contains = setClass.getMethod("contains", Object.class);
                add = setClass.getMethod("add", Object.class);
                remove = setClass.getMethod("remove", Object.class);
                Field dConfigField = c.getField("DConfig");
                Object dConfig = dConfigField.get(slotLocking);
                markDirty = dConfig.getClass().getMethod("markDirty");
                // Firmament 音效
                Class<?> soundCls = Class.forName("moe.nea.firmament.util.CommonSoundEffects");
                playSuccess = soundCls.getMethod("playSuccess");
            } catch (Exception ignored) {}
            SLOT_LOCKING = slotLocking;
            GET_LOCKED_UUIDS = getLockedUUIDs;
            CONTAINS = contains;
            ADD = add;
            REMOVE = remove;
            MARK_DIRTY = markDirty;
            PLAY_SUCCESS = playSuccess;
        }

        static boolean ready() { return SLOT_LOCKING != null && GET_LOCKED_UUIDS != null && CONTAINS != null; }

        static Boolean contains(String uuidStr) {
            if (!ready()) return null;
            try {
                UUID uuid = UUID.fromString(uuidStr);
                Object uuids = GET_LOCKED_UUIDS.invoke(SLOT_LOCKING);
                if (uuids == null) return null;
                return (boolean) CONTAINS.invoke(uuids, uuid);
            } catch (Exception e) { return null; }
        }

        static void toggle(String uuidStr) {
            if (!ready()) return;
            try {
                UUID uuid = UUID.fromString(uuidStr);
                Object uuids = GET_LOCKED_UUIDS.invoke(SLOT_LOCKING);
                if (uuids == null) return;
                if ((boolean) CONTAINS.invoke(uuids, uuid)) {
                    REMOVE.invoke(uuids, uuid);
                } else {
                    ADD.invoke(uuids, uuid);
                }
                MARK_DIRTY.invoke(SLOT_LOCKING.getClass().getField("DConfig").get(SLOT_LOCKING));
                if (PLAY_SUCCESS != null) PLAY_SUCCESS.invoke(null);
            } catch (Exception ignored) {}
        }
    }
}
