package top.babyzombie.addons.module.events;


import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import com.mojang.authlib.properties.Property;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import top.babyzombie.addons.config.ModConfigManager;
import top.babyzombie.addons.util.ChatUtils;
import top.babyzombie.addons.util.HypixelLocationTracker;
import top.babyzombie.addons.util.WorldTextRenderer;


public final class FruitDiggingModule {

    private static final Map<String, String> FRUITS = Map.ofEntries(
            Map.entry("ewogICJ0aW1lc3RhbXAiIDogMTcxODIwMzE5MzY5OCwKICAicHJvZmlsZUlkIiA6ICIyMjAwZjYzOWI1YTU0YzM2YjA4ZThiNjZhNDNjNmJjNCIsCiAgInByb2ZpbGVOYW1lIiA6ICJCYXVvSmxlVCIsCiAgInNpZ25hdHVyZVJlcXVpcmVkIiA6IHRydWUsCiAgInRleHR1cmVzIiA6IHsKICAgICJTS0lOIiA6IHsKICAgICAgInVybCIgOiAiaHR0cDovL3RleHR1cmVzLm1pbmVjcmFmdC5uZXQvdGV4dHVyZS9jOTJiMDk5YTYyY2QyZmJmOGFkYTA5ZGVjMTQ1Yzc1ZDdmZGE0ZGM1N2I5NjhiZWEzYThmYTExZTM3YWE0OGIyIgogICAgfQogIH0KfQ==", "Cherry"),
            Map.entry("ewogICJ0aW1lc3RhbXAiIDogMTcxOTMyMDg4MTU1MCwKICAicHJvZmlsZUlkIiA6ICJiMTM1MDRmMjMxOGI0OWNjYWFkZDcyYWVhYmMyNTQ1MCIsCiAgInByb2ZpbGVOYW1lIiA6ICJUeXBrZW4iLAogICJzaWduYXR1cmVSZXF1aXJlZCIgOiB0cnVlLAogICJ0ZXh0dXJlcyIgOiB7CiAgICAiU0tJTiIgOiB7CiAgICAgICJ1cmwiIDogImh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMTdlYTI3OGQ2MjI1YzQ0N2M1OTQzZDY1Mjc5OGQwYmJiZDE0MTg0MzRjZThjNTRjNTRmZGFjNzk5OTRkZGQ2YyIKICAgIH0KICB9Cn0=", "Apple"),
            Map.entry("ewogICJ0aW1lc3RhbXAiIDogMTcxOTMyMDk0MjUyNSwKICAicHJvZmlsZUlkIiA6ICI1ZjU5NmViY2JlOTQ0NmQxYmI0M2JlNGYzZjRiOGJlNSIsCiAgInByb2ZpbGVOYW1lIiA6ICJUZWlsMHNzIiwKICAic2lnbmF0dXJlUmVxdWlyZWQiIDogdHJ1ZSwKICAidGV4dHVyZXMiIDogewogICAgIlNLSU4iIDogewogICAgICAidXJsIiA6ICJodHRwOi8vdGV4dHVyZXMubWluZWNyYWZ0Lm5ldC90ZXh0dXJlL2FjMjY4ZDM2YzJjNjA0N2ZmZWVjMDAxMjQwOTYzNzZiNTZkYmI0ZDc1NmE1NTMyOTM2M2ExYjI3ZmNkNjU5Y2QiCiAgICB9CiAgfQp9", "Durian"),
            Map.entry("ewogICJ0aW1lc3RhbXAiIDogMTcxODIwMzIxNTc3NiwKICAicHJvZmlsZUlkIiA6ICJmZmU5MzczY2YyMDM0OWFhYTJlN2NiYzJkZmY2M2I5MyIsCiAgInByb2ZpbGVOYW1lIiA6ICJNZWxvblR1bmExIiwKICAic2lnbmF0dXJlUmVxdWlyZWQiIDogdHJ1ZSwKICAidGV4dHVyZXMiIDogewogICAgIlNLSU4iIDogewogICAgICAidXJsIiA6ICJodHRwOi8vdGV4dHVyZXMubWluZWNyYWZ0Lm5ldC90ZXh0dXJlLzEwY2ViMTQ1NWI0NzFkMDE2YTlmMDZkMjVmNmU0NjhkZjlmY2YyMjNlMmMxZTQ3OTViMTZlODRmY2NhMjY0ZWUiCiAgICB9CiAgfQp9", "Coconut"),
            Map.entry("ewogICJ0aW1lc3RhbXAiIDogMTcxOTMyMDk5NTI0OCwKICAicHJvZmlsZUlkIiA6ICI4YWFlYTdlYjViOWM0ZWEwODUxNWU3MDhhZGIxODBkNyIsCiAgInByb2ZpbGVOYW1lIiA6ICJNYVBhODA3MTEiLAogICJzaWduYXR1cmVSZXF1aXJlZCIgOiB0cnVlLAogICJ0ZXh0dXJlcyIgOiB7CiAgICAiU0tJTiIgOiB7CiAgICAgICJ1cmwiIDogImh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZWZlNGVmODNiYWYxMDVlOGRlZTZjZjAzZGZlNzQwN2YxOTExYjNiOTk1MmM4OTFhZTM0MTM5NTYwZjI5MzFkNiIKICAgIH0KICB9Cn0=", "Watermelon"),
            Map.entry("ewogICJ0aW1lc3RhbXAiIDogMTcxOTMyMDk4MDg4NywKICAicHJvZmlsZUlkIiA6ICJiMmQ4MTA2YTJjM2Y0ZTY4ODA0ODkzOWU0NGM1NmUyMiIsCiAgInByb2ZpbGVOYW1lIiA6ICJSZWdoeGQiLAogICJzaWduYXR1cmVSZXF1aXJlZCIgOiB0cnVlLAogICJ0ZXh0dXJlcyIgOiB7CiAgICAiU0tJTiIgOiB7CiAgICAgICJ1cmwiIDogImh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNDA4MjRkMTgwNzkwNDJkNTc2OWYyNjRmNDQzOTRiOTViOWI5OWNlNjg5Njg4Y2MxMGM5ZWVjM2Y4ODJjY2MwOCIKICAgIH0KICB9Cn0=", "Pomegranate"),
            Map.entry("ewogICJ0aW1lc3RhbXAiIDogMTcxOTMyMDkyODM3MiwKICAicHJvZmlsZUlkIiA6ICJmNzg5OWI1ZGEzZGM0ZTY0YmFlM2QyMmYzMWFjMzBhZSIsCiAgInByb2ZpbGVOYW1lIiA6ICJwaXhlbGJsb2IxMjEyIiwKICAic2lnbmF0dXJlUmVxdWlyZWQiIDogdHJ1ZSwKICAidGV4dHVyZXMiIDogewogICAgIlNLSU4iIDogewogICAgICAidXJsIiA6ICJodHRwOi8vdGV4dHVyZXMubWluZWNyYWZ0Lm5ldC90ZXh0dXJlLzNjYzc2MWJjYjA1Nzk3NjNkOWI4YWI2YjdiOTZmYTc3ZWI2ZDk2MDVhODA0ZDgzOGZlYzM5ZTdiMjVmOTU1OTEiCiAgICB9CiAgfQp9", "Dragonfruit"),
            Map.entry("ewogICJ0aW1lc3RhbXAiIDogMTcxOTMyMDk2NDE1NSwKICAicHJvZmlsZUlkIiA6ICI4ZTFjZTM2ZGE2Mzk0ZjgwOTFmZjZjYTZiZTNhZTA5NSIsCiAgInByb2ZpbGVOYW1lIiA6ICJGdWxsY3JlbiIsCiAgInNpZ25hdHVyZVJlcXVpcmVkIiA6IHRydWUsCiAgInRleHR1cmVzIiA6IHsKICAgICJTS0lOIiA6IHsKICAgICAgInVybCIgOiAiaHR0cDovL3RleHR1cmVzLm1pbmVjcmFmdC5uZXQvdGV4dHVyZS9mMzYzYTYyMTI2YTM1NTM3ZjgxODkzNDNhMjI2NjBkZTc1ZTgxMGM2YWMwMDRhN2QzZGE2NWYxYzA0MGE4MzkiCiAgICB9CiAgfQp9", "Mango"),
            Map.entry("ewogICJ0aW1lc3RhbXAiIDogMTYyNDU0NjA4NjAxNiwKICAicHJvZmlsZUlkIiA6ICI0NWY3YTJlNjE3ODE0YjJjODAwODM5MmRmN2IzNWY0ZiIsCiAgInByb2ZpbGVOYW1lIiA6ICJfSnVzdERvSXQiLAogICJzaWduYXR1cmVSZXF1aXJlZCIgOiB0cnVlLAogICJ0ZXh0dXJlcyIgOiB7CiAgICAiU0tJTiIgOiB7CiAgICAgICJ1cmwiIDogImh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYTc2YTI4MTFkMWUxNzZhMDdiNmQwYTY1N2I5MTBmMTM0ODk2Y2UzMDg1MGY2ZTgwYzdjODM3MzJkODUzODFlYSIKICAgIH0KICB9Cn0=", "Bomb"),
            Map.entry("ewogICJ0aW1lc3RhbXAiIDogMTcxODIwMzIzMzA1NiwKICAicHJvZmlsZUlkIiA6ICIxODA1Y2E2MmM0ZDI0M2NiOWQxYmY4YmM5N2E1YjgyNCIsCiAgInByb2ZpbGVOYW1lIiA6ICJSdWxsZWQiLAogICJzaWduYXR1cmVSZXF1aXJlZCIgOiB0cnVlLAogICJ0ZXh0dXJlcyIgOiB7CiAgICAiU0tJTiIgOiB7CiAgICAgICJ1cmwiIDogImh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNDA3YjI3NWQyOGI5MjdiMWJmN2Y2ZGQ5ZjQ1ZmJkYWQyYWY4NTcxYzU0YzhmMDI3ZDFiZmY2OTU2ZmJmM2MxNiIKICAgIH0KICB9Cn0=", "Rum")
    );

    private static final int AREA_X_MIN = -112, AREA_X_MAX = -106;
    private static final int AREA_Z_MIN = 19, AREA_Z_MAX = 25;
    private static final int AREA_Y_MIN = 72, AREA_Y_MAX = 75;

    private static final List<Marker> fruits = new ArrayList<>();
    private static final List<Marker> treasures = new ArrayList<>();
    private static final List<Marker> bombs = new ArrayList<>();
    private static int digX, digZ;
    private static boolean hasDigLoc;
    private static long lastNpcDialogTime;
    private static String acceptCommand;

    private FruitDiggingModule() {}

    public static void init() {
        AttackBlockCallback.EVENT.register((player, world, hand, pos, direction) -> {
            if (!ModConfigManager.get().events.fruitDiggingHelper) return InteractionResult.PASS;
            if (!isInCarnival()) return InteractionResult.PASS;
            if (!isInDigArea(pos.getX(), pos.getY(), pos.getZ())) return InteractionResult.PASS;
            digX = pos.getX();
            digZ = pos.getZ();
            hasDigLoc = true;
            return InteractionResult.PASS;
        });

        net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            if (overlay) return;
            if (!ModConfigManager.get().events.fruitDiggingHelper) return;
            if (!isInCarnival() || !hasDigLoc) return;
            String text = ChatUtils.stripColor(message.getString());

            if (text.matches("TREASURE! There is a[n]? [a-zA-Z]+ nearby\\.")) {
                treasures.add(new Marker(digX, digZ, tr("babyzombieaddons.fruitdigging.treasure_nearby")));
                hasDigLoc = false;
            } else if (text.matches("(ANCHOR|TREASURE)! There are no fruits nearby!")) {
                treasures.add(new Marker(digX, digZ, tr("babyzombieaddons.fruitdigging.no_fruit")));
                hasDigLoc = false;
            } else if (text.matches("MINES! There are [0-9]+ bomb[s]? hidden nearby\\.")) {
                bombs.add(new Marker(digX, digZ, tr("babyzombieaddons.fruitdigging.bombs_nearby")));
                hasDigLoc = false;
            }
        });

        net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            if (overlay) return;
            if (!ModConfigManager.get().events.fruitDiggingHelper) return;
            if (!HypixelLocationTracker.getInstance().isInSkyblock()) return;
            String text = ChatUtils.stripColor(message.getString());

            if (text.matches("\\s+Fruit Digging")
                    || text.equals("[NPC] Carnival Pirateman: Here's yer shovel, then.")) {
                fruits.clear();
                treasures.clear();
                bombs.clear();
                hasDigLoc = false;
                acceptCommand = null;
            }
        });

        // Auto-accept: capture accept command from options message
        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            if (overlay) return;
            if (!ModConfigManager.get().events.fruitDiggingAutoAccept) return;
            if (!HypixelLocationTracker.getInstance().isInSkyblock()) return;
            String text = ChatUtils.stripColor(message.getString());

            if (text.contains("[NPC] Carnival Pirateman: Would ye like to do some Fruit Digging?")) {
                lastNpcDialogTime = System.currentTimeMillis();
                acceptCommand = null;
                return;
            }

            if (text.contains("Select an option:") && text.contains("[Aye sure do!]")) {
                acceptCommand = findClickCommand(message, "Aye sure do!");
            }
        });

        // Right-click on NPC within 2s → cancel and auto-accept
        UseEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (!ModConfigManager.get().events.fruitDiggingAutoAccept) return InteractionResult.PASS;
            if (acceptCommand == null) return InteractionResult.PASS;
            if (System.currentTimeMillis() - lastNpcDialogTime > 2000) {
                acceptCommand = null;
                return InteractionResult.PASS;
            }
            if (entity instanceof net.minecraft.world.entity.player.Player) {
                ChatUtils.sendCommand(acceptCommand);
                acceptCommand = null;
                return InteractionResult.FAIL;
            }
            return InteractionResult.PASS;
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (!ModConfigManager.get().events.fruitDiggingHelper) return;
            if (!isInCarnival()) return;
            if (client.player == null || client.player.tickCount % 10 != 0) return;

            var items = client.player.level().getEntitiesOfClass(ItemEntity.class,
                    new AABB(AREA_X_MIN, AREA_Y_MIN, AREA_Z_MIN, AREA_X_MAX, AREA_Y_MAX, AREA_Z_MAX));

            for (var item : items) {
                var stack = item.getItem();
                if (stack.getItem() != Items.PLAYER_HEAD) continue;

                int bx = (int) Math.floor(item.getX());
                int bz = (int) Math.floor(item.getZ());
                boolean dug = client.player.level()
                        .getBlockState(new net.minecraft.core.BlockPos(bx, 72, bz)).getBlock() != Blocks.SAND;

                String textureValue = getSkullTexture(stack);
                if (textureValue == null) continue;
                String fruitName = FRUITS.get(textureValue);
                if (fruitName == null) continue;

                String label = (dug ? "§a" : "§e") + fruitName;
                int fx = (int) Math.floor(item.getX());
                int fz = (int) Math.floor(item.getZ());
                fruits.removeIf(m -> m.x == fx && m.z == fz);
                fruits.add(new Marker(fx, fz, label));
            }
        });

        WorldRenderEvents.BEFORE_ENTITIES.register(ctx -> {
            if (!ModConfigManager.get().events.fruitDiggingHelper) return;
            if (!isInCarnival()) return;
            int total = fruits.size() + treasures.size() + bombs.size();
            if (total == 0) return;

            for (var m : bombs)
                WorldTextRenderer.renderString(ctx, m.label, m.x + 0.5, 74.7, m.z + 0.5, 0xFF5555, 0.025f, true);
            for (var m : treasures)
                WorldTextRenderer.renderString(ctx, m.label, m.x + 0.5, 74.5, m.z + 0.5, 0xFFFF55, 0.025f, true);
            for (var m : fruits)
                WorldTextRenderer.renderString(ctx, m.label, m.x - 0.5, 74.3, m.z - 0.5, 0xFFFFFF, 0.025f, true);
        });
    }

    private static String getSkullTexture(net.minecraft.world.item.ItemStack stack) {
        if (!stack.is(Items.PLAYER_HEAD)) return null;
        ResolvableProfile profile = stack.get(DataComponents.PROFILE);
        if (profile == null) return null;
        var gameProfile = profile.partialProfile();
        if (gameProfile == null) return null;
        var textures = gameProfile.properties().get("textures");
        if (textures == null) return null;
        return textures.stream()
                .filter(Objects::nonNull)
                .map(Property::value)
                .findFirst()
                .orElse(null);
    }

    private static String tr(String key) {
        return Component.translatable(key).getString();
    }

    private static boolean isInCarnival() {
        var tracker = HypixelLocationTracker.getInstance();
        return tracker.isInSkyblock() && "Carnival".equals(tracker.getLocation());
    }

    private static boolean isInDigArea(double x, double y, double z) {
        return x >= AREA_X_MIN && x <= AREA_X_MAX
                && z >= AREA_Z_MIN && z <= AREA_Z_MAX
                && y >= AREA_Y_MIN && y <= AREA_Y_MAX;
    }

    private static String findClickCommand(Component component, String targetText) {
        var clickEvent = component.getStyle().getClickEvent();
        if (clickEvent != null && component.getString().contains(targetText)) {
            if (clickEvent instanceof ClickEvent.RunCommand runCommand) {
                return runCommand.command();
            }
        }
        for (var sibling : component.getSiblings()) {
            String result = findClickCommand(sibling, targetText);
            if (result != null) return result;
        }
        return null;
    }

    private record Marker(int x, int z, String label) {}
}
