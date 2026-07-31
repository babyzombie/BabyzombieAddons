package top.babyzombie.addons.module.misc.abiphone;

import com.google.gson.reflect.TypeToken;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ResolvableProfile;
import org.jetbrains.annotations.Nullable;
import top.babyzombie.addons.util.DataPersistence;
import top.babyzombie.addons.util.ScreenLoadWaiter;
import top.babyzombie.addons.util.tracker.HypixelLocationTracker;

import java.lang.reflect.Type;
import java.util.*;

import java.util.regex.Pattern;

public class AbiphoneTracker {

    private static final AbiphoneTracker INSTANCE = new AbiphoneTracker();

    private static final Pattern TITLE_PATTERN = Pattern.compile("^\\(\\d+/\\d+\\).* Abiphone.*");
    private static final Type ITEM_LIST_TYPE = new TypeToken<List<ItemEntry>>() {}.getType();

    private AbiphoneTracker() {}

    public static AbiphoneTracker getInstance() {
        return INSTANCE;
    }

    public void init() {
        ScreenLoadWaiter.whenScreenOpened(
            title -> TITLE_PATTERN.matcher(title).matches(),
            44, 0,
            containerScreen -> {
                var tracker = HypixelLocationTracker.getInstance();
                String uuid = tracker.getUuid();
                String profileId = tracker.getProfileId();
                if (uuid == null || profileId == null) return;
                if (tracker.isInAlpha()) return;

                var menu = containerScreen.getMenu();
                var collected = new LinkedHashSet<ItemEntry>();
                for (int i = 10; i < 44; i++) {
                    ItemStack stack = menu.slots.get(i).getItem();
                    if (stack.isEmpty()) continue;

                    String itemId = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
                    if (itemId.contains("glass_pane")) continue;

                    String name = stack.getHoverName().getString();
                    String nbt = null;
                    var profileComp = stack.get(DataComponents.PROFILE);
                    if (profileComp != null) {
                        nbt = ResolvableProfile.CODEC.encodeStart(NbtOps.INSTANCE, profileComp)
                            .result().map(Tag::toString).orElse(null);
                    }
                    String description = null;
                    var loreComp = stack.get(DataComponents.LORE);
                    if (loreComp != null) {
                        List<String> descLines = new ArrayList<>();
                        for (var line : loreComp.lines()) {
                            String text = line.getString();
                            if (text.isBlank()) break;
                            descLines.add(text);
                        }
                        if (!descLines.isEmpty()) {
                            description = String.join("\n", descLines);
                        }
                    }
                    collected.add(new ItemEntry(name, itemId, nbt, description));
                }
                saveItems(uuid, profileId, new ArrayList<>(collected));
            });
    }

    /** Per-profile data lives at data/&lt;uuid&gt;/&lt;profileId&gt;/abiphone.json. */
    private static String subDir(String uuid, String profileId) {
        return uuid + "/" + profileId;
    }

    public void saveOrderedItems(String uuid, String profileId, List<ItemEntry> items) {
        DataPersistence.save(subDir(uuid, profileId), "abiphone.json", items);
    }

    public List<ItemEntry> loadItems(String uuid, String profileId) {
        List<ItemEntry> list = DataPersistence.load(subDir(uuid, profileId), "abiphone.json", ITEM_LIST_TYPE);
        return list != null ? list : Collections.emptyList();
    }

    private void saveItems(String uuid, String profileId, List<ItemEntry> newItems) {
        List<ItemEntry> items = new ArrayList<>(loadItems(uuid, profileId));
        for (ItemEntry e : newItems) {
            if (!items.contains(e)) {
                items.add(e);
            }
        }
        DataPersistence.save(subDir(uuid, profileId), "abiphone.json", items);
    }

    public record ItemEntry(String name, String material, String nbt, @Nullable String description) {
        @Override
        public boolean equals(Object o) {
            if (!(o instanceof ItemEntry that)) return false;
            return Objects.equals(name, that.name)
                && Objects.equals(material, that.material);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name, material);
        }
    }
}
