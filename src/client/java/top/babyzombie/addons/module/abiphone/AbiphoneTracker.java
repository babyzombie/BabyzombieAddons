package top.babyzombie.addons.module.abiphone;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ResolvableProfile;
import org.jetbrains.annotations.Nullable;
import top.babyzombie.addons.util.tracker.HypixelLocationTracker;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;

public class AbiphoneTracker {

    private static final AbiphoneTracker INSTANCE = new AbiphoneTracker();

    private static final Pattern TITLE_PATTERN = Pattern.compile("^\\(\\d+/\\d+\\).* Abiphone.*");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Type ITEM_LIST_TYPE = new TypeToken<List<ItemEntry>>() {}.getType();

    private final Path configDir;

    private AbiphoneTracker() {
        this.configDir = FabricLoader.getInstance().getConfigDir().resolve("babyzombieaddons");
    }

    public static AbiphoneTracker getInstance() {
        return INSTANCE;
    }

    public void init() {
        ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            if (!HypixelLocationTracker.getInstance().isInSkyblock()) return;
            if (!(screen instanceof AbstractContainerScreen<?> containerScreen)) return;

            String title = screen.getTitle().getString();
            if (!TITLE_PATTERN.matcher(title).matches()) return;

            var menu = containerScreen.getMenu();
            if (menu.slots.size() < 90) return;

            var tracker = HypixelLocationTracker.getInstance();
            String uuid = tracker.getUuid();
            String profileId = tracker.getProfileId();
            if (uuid == null || profileId == null) return;

            // Container contents arrive after screen init — wait 2 ticks
            AtomicBoolean done = new AtomicBoolean(false);
            ClientTickEvents.END_CLIENT_TICK.register(tickClient -> {
                if (done.getAndSet(true)) return;
                if (tickClient.screen != containerScreen) return;

                List<ItemEntry> newItems = new ArrayList<>();
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
                    newItems.add(new ItemEntry(name, itemId, nbt, description));
                }

                if (newItems.isEmpty()) return;

                saveItems(uuid, profileId, newItems);
            });
        });
    }

    private Path getFile(String uuid, String profileId) {
        return configDir.resolve("abiphone").resolve(uuid + "_" + profileId + ".json");
    }

    public void saveOrderedItems(String uuid, String profileId, List<ItemEntry> items) {
        try {
            Files.createDirectories(configDir);
            Files.writeString(getFile(uuid, profileId), GSON.toJson(items),
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public List<ItemEntry> loadItems(String uuid, String profileId) {
        Path file = getFile(uuid, profileId);
        if (!Files.exists(file)) return Collections.emptyList();

        try {
            String raw = Files.readString(file);
            List<ItemEntry> list = GSON.fromJson(raw, ITEM_LIST_TYPE);
            return list != null ? list : Collections.emptyList();
        } catch (IOException e) {
            e.printStackTrace();
            return Collections.emptyList();
        }
    }

    private void saveItems(String uuid, String profileId, List<ItemEntry> newItems) {
        try {
            Files.createDirectories(configDir);
            Path file = getFile(uuid, profileId);

            List<ItemEntry> items = new ArrayList<>();
            if (Files.exists(file)) {
                String raw = Files.readString(file);
                List<ItemEntry> list = GSON.fromJson(raw, ITEM_LIST_TYPE);
                if (list != null) items.addAll(list);
            }

            for (ItemEntry e : newItems) {
                if (!items.contains(e)) {
                    items.add(e);
                }
            }

            Files.writeString(file, GSON.toJson(items), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            e.printStackTrace();
        }
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
