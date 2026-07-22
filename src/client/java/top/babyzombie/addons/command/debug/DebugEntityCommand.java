package top.babyzombie.addons.command.debug;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.serialization.JsonOps;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.arguments.IdentifierArgument;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.world.entity.*;
import top.babyzombie.addons.util.ChatUtils;
import top.babyzombie.addons.util.PlayerUtils;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import top.babyzombie.addons.config.ModConfigManager;

import java.util.ArrayList;
import java.util.List;

import org.joml.Vector3fc;
import top.babyzombie.addons.mixin.entity.DisplayAccessor;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommands.argument;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommands.literal;

public final class DebugEntityCommand {
    private DebugEntityCommand() {}

    public static void register(ArgumentBuilder<FabricClientCommandSource, ?> parent) {
        parent.then(literal("getentity")
                .executes(ctx -> list(ctx.getSource(), 5.0, null))
                .then(argument("distance", DoubleArgumentType.doubleArg(1, 128))
                        .executes(ctx -> list(ctx.getSource(),
                                DoubleArgumentType.getDouble(ctx, "distance"), null))
                        .then(argument("type", IdentifierArgument.id())
                                .suggests((ctx, builder) -> {
                                    String remaining = builder.getRemaining().toLowerCase();
                                    BuiltInRegistries.ENTITY_TYPE.keySet().stream()
                                            .map(Identifier::toString)
                                            .filter(id -> id.contains(remaining))
                                            .forEach(builder::suggest);
                                    return builder.buildFuture();
                                })
                                .executes(ctx -> list(ctx.getSource(),
                                        DoubleArgumentType.getDouble(ctx, "distance"),
                                        ctx.getArgument("type", Identifier.class).toString())))));
    }

    private static int list(FabricClientCommandSource src, double distance, String typeFilter) {
        var mc = Minecraft.getInstance();
        var player = mc.player;
        if (player == null) return 0;

        var aabb = player.getBoundingBox().inflate(distance);
        List<Entity> entities = player.level().getEntities(player, aabb, e -> !e.is(player));

        if (typeFilter != null) {
            String lower = typeFilter.toLowerCase();
            entities = entities.stream()
                    .filter(e -> {
                        String name = e.getName().getString().toLowerCase();
                        String typeKey = EntityType.getKey(e.getType()).toString().toLowerCase();
                        String className = e.getClass().getSimpleName().toLowerCase();
                        return name.contains(lower) || typeKey.contains(lower) || className.contains(lower);
                    })
                    .toList();
        }

        String filterSuffix = typeFilter != null
                ? Component.translatable("babyzombieaddons.debug.entities.none_filter", typeFilter).getString()
                : "";

        if (entities.isEmpty()) {
            src.sendFeedback(Component.translatable("babyzombieaddons.debug.entities.none",
                    String.valueOf(distance), filterSuffix));
            return 1;
        }

        src.sendFeedback(Component.translatable("babyzombieaddons.debug.entities.header",
                entities.size(), distance));

        int max = ModConfigManager.get().misc.maxDebugEntities;
        int count = 0;
        for (var entity : entities) {
            if (count >= max) {
                src.sendFeedback(Component.translatable("babyzombieaddons.debug.entities.truncated",
                        entities.size() - max));
                break;
            }
            try {
                dumpEntity(src, entity);
            } catch (Exception e) {
                src.sendFeedback(Component.nullToEmpty(e.getMessage()));
            }
            count++;
        }

        return 1;
    }

    static void dumpEntity(FabricClientCommandSource src, Entity entity) {
        var mc = Minecraft.getInstance();
        var name = ChatUtils.toLegacyString(entity.getName());

        // If TextDisplay, override name with the actual displayed text
        if (entity instanceof net.minecraft.world.entity.Display.TextDisplay td) {
            var displayText = td.getText();
            if (!displayText.getString().isEmpty()) {
                name = ChatUtils.toLegacyString(displayText);
            }
        }

        var customNameStr = entity.getCustomName() != null
                ? ChatUtils.toLegacyString(entity.getCustomName()) : null;
        var type = entity.getType();
        var typeKey = EntityType.getKey(type).toString();
        var className = entity.getClass().getSimpleName();
        var vel = entity.getDeltaMovement();
        var bbox = entity.getBoundingBox();

        var lines = new ArrayList<Component>();

        // —— line 1: name + tags, hover: type / UUID / fire / on_ground ——
        var nameLine = new StringBuilder();
        nameLine.append("§b").append(name);
        if (customNameStr != null && !customNameStr.equals(name)) {
            nameLine.append(Tstr("debug.entity.custom", customNameStr));
        }
        if (entity.isInvisible()) nameLine.append(Tstr("debug.entity.invisible"));
        if (entity.isCurrentlyGlowing()) nameLine.append(Tstr("debug.entity.glowing"));

        nameLine.append(" §7(").append(className).append(")");

        var nameHover = typeKey + "\n"
                + Tstr("debug.entity.uuid", entity.getUUID()) + "\n"
                + Tstr("debug.entity.fire", entity.getRemainingFireTicks()) + "\n"
                + Tstr("debug.entity.on_ground", entity.onGround());

        // Add scale to hover
        var scaleStr = getEntityScale(entity);
        if (scaleStr != null) {
            nameHover += "\n" + Tstr("debug.entity.scale", scaleStr);
        }

        // For player entities, append skin texture URL at the bottom of hover
        if (entity.getType() == EntityTypes.PLAYER) {
            String skinUrl = PlayerUtils.getSkinTextureUrl(PlayerUtils.getPlayerProfile(entity));
            if (skinUrl != null) {
                nameHover += "\n" + Tstr("debug.entity.skin", skinUrl);
            }
        }

        lines.add(hover(nameLine.toString(), nameHover));

        // —— line 2: exact coords [+ health], hover: rotation / bbox / velocity / eye_height [/ pose / armor / absorption] ——
        var posText = new StringBuilder();
        posText.append(Tstr("debug.entity.exact",
                String.format("%.2f", entity.getX()),
                String.format("%.2f", entity.getY()),
                String.format("%.2f", entity.getZ())));

        var posHover = new StringBuilder();
        posHover.append(Tstr("debug.entity.rotation",
                String.format("%.1f", entity.getYRot()), String.format("%.1f", entity.getXRot()))).append("\n");
        posHover.append(Tstr("debug.entity.bbox",
                String.format("%.1f %.1f %.1f", bbox.minX, bbox.minY, bbox.minZ),
                String.format("%.1f %.1f %.1f", bbox.maxX, bbox.maxY, bbox.maxZ))).append("\n");
        posHover.append(Tstr("debug.entity.velocity",
                String.format("%.3f", vel.x), String.format("%.3f", vel.y), String.format("%.3f", vel.z))).append("\n");
        posHover.append(Tstr("debug.entity.eye_height", String.format("%.2f", entity.getEyeHeight())));

        if (entity instanceof LivingEntity living) {
            posHover.append("\n").append(Tstr("debug.entity.pose", living.getPose().name()));
            posHover.append(Tstr("debug.entity.armor", living.getArmorValue()));
            float abs = living.getAbsorptionAmount();
            if (abs > 0) posHover.append(Tstr("debug.entity.absorption", String.format("%.1f", abs)));

            // health on the line
            posText.append(Tstr("debug.entity.health",
                    String.format("%.1f", living.getHealth()),
                    String.format("%.1f", living.getMaxHealth())));
        }

        lines.add(hover(posText.toString(), posHover.toString()));

        if (entity instanceof LivingEntity living) {
            // —— effects (brief line, hover details) ——
            var effects = living.getActiveEffects();
            if (!effects.isEmpty()) {
                var brief = new StringBuilder();
                var detail = new StringBuilder();
                boolean first = true;
                for (var inst : effects) {
                    if (!first) { brief.append(", "); detail.append("\n"); }
                    String effName = inst.getEffect().getRegisteredName().toString();
                    brief.append("§d").append(effName).append(" §e").append(inst.getAmplifier() + 1);
                    detail.append("§d").append(effName)
                            .append(" §eLv.").append(inst.getAmplifier() + 1)
                            .append(" §7").append(String.format("%.1fs", inst.getDuration() / 20f));
                    first = false;
                }
                lines.add(hover(Tstr("debug.entity.effects", brief.toString()),
                        detail.toString()));
            }

            // —— equipment (hover + click on name to copy formatted item data) ——
            var equipBrief = new StringBuilder();
            var equipLines = new ArrayList<Component>();
            for (var slot : EquipmentSlot.values()) {
                ItemStack item = living.getItemBySlot(slot);
                if (item.isEmpty()) continue;

                var itemName = ChatUtils.toLegacyString(item.getDisplayName());
                var slotName = "§d" + slot.getName() + ": §f" + itemName;
                if (!equipBrief.isEmpty()) equipBrief.append(", ");
                equipBrief.append(slotName);

                var hoverSb = new StringBuilder();
                hoverSb.append(slotName);
                hoverSb.append("\n§7").append(EntityType.getKey(type));

                var cmd = item.get(DataComponents.CUSTOM_MODEL_DATA);
                if (cmd != null) {
                    hoverSb.append("\n\n§6§l--- CUSTOM_MODEL_DATA ---");
                    hoverSb.append("\n§7").append(cmd);
                }

                var itemModel = item.get(DataComponents.ITEM_MODEL);
                if (itemModel != null) {
                    hoverSb.append("\n\n§6§l--- ITEM_MODEL ---");
                    hoverSb.append("\n§7").append(itemModel);
                }

                var tooltip = item.getTooltipLines(
                        net.minecraft.world.item.Item.TooltipContext.of(mc.level),
                        mc.player, TooltipFlag.Default.NORMAL);
                for (var tip : tooltip) {
                    hoverSb.append("\n").append(ChatUtils.toLegacyString(tip));
                }

                var customData = item.get(DataComponents.CUSTOM_DATA);
                if (customData != null && !customData.isEmpty()) {
                    Tag tag = customData.copyTag();
                    JsonElement je = com.mojang.serialization.Dynamic.convert(NbtOps.INSTANCE, JsonOps.INSTANCE, tag);
                    String json = new GsonBuilder().setPrettyPrinting().create().toJson(je);
                    hoverSb.append("\n\n§6§l--- CUSTOM_DATA ---");
                    hoverSb.append("\n§7").append(json);
                }

                String copyText = top.babyzombie.addons.util.ItemUtils.formatItemCopyText(item);
                equipLines.add(Component.literal(slotName)
                        .withStyle(style -> style
                                .withHoverEvent(new HoverEvent.ShowText(Component.literal(hoverSb.toString())))
                                .withClickEvent(new ClickEvent.CopyToClipboard(copyText))));
            }
            if (!equipBrief.isEmpty()) {
                var line = Component.empty();
                for (int i = 0; i < equipLines.size(); i++) {
                    if (i > 0) line.append(Component.literal(", "));
                    line.append(equipLines.get(i));
                }
                lines.add(Component.literal(Tstr("debug.entity.equip", "")).append(line));
            }
        }

        // —— item entity / item display ——
        ItemStack heldItem = null;
        if (entity instanceof ItemEntity itemEnt) {
            heldItem = itemEnt.getItem();
        } else if (entity instanceof Display.ItemDisplay itemDisp) {
            heldItem = itemDisp.getItemStack();
        } else if (entity instanceof Display.BlockDisplay blockDisp) {
            BlockState blockState = blockDisp.getBlockState();
            if (!blockState.isAir()) {
                Block block = blockState.getBlock();
                String blockKey = BuiltInRegistries.BLOCK.getKey(block).toString();
                ItemStack blockItem = new ItemStack(block.asItem());

                // Build hover with block state properties
                StringBuilder hoverSb = new StringBuilder();
                hoverSb.append("§f").append(blockKey);

                var properties = blockState.getValues().toList();
                if (!properties.isEmpty()) {
                    hoverSb.append("\n\n§6§l--- Properties ---");
                    for (var pv : properties) {
                        hoverSb.append("\n§7")
                                .append(pv.property().getName())
                                .append(" = §a")
                                .append(pv.value().toString());
                    }
                }

                hoverSb.append("\n\n§7Item: ")
                        .append(BuiltInRegistries.ITEM.getKey(block.asItem()).toString());

                String copyText = top.babyzombie.addons.util.ItemUtils.formatItemCopyText(blockItem);
                lines.add(Component.literal("§dBlockState: §f" + blockKey)
                        .withStyle(style -> style
                                .withHoverEvent(new HoverEvent.ShowText(Component.literal(hoverSb.toString())))
                                .withClickEvent(new ClickEvent.CopyToClipboard(copyText))));
            }
        } else if (entity instanceof ArmorStand as) {
            // armor stand: grab mainhand + head for display
            var mainHand = as.getItemBySlot(EquipmentSlot.MAINHAND);
            var head = as.getItemBySlot(EquipmentSlot.HEAD);
            if (!mainHand.isEmpty()) heldItem = mainHand;
            if (!head.isEmpty()) heldItem = head; // head overrides
        }
        if (heldItem != null && !heldItem.isEmpty()) {
            var itemName = ChatUtils.toLegacyString(heldItem.getDisplayName());
            var hoverSb = new StringBuilder();
            hoverSb.append("§f").append(itemName);
            hoverSb.append("\n§7").append(BuiltInRegistries.ITEM.getKey(heldItem.getItem()));

            var tooltip = heldItem.getTooltipLines(
                    net.minecraft.world.item.Item.TooltipContext.of(mc.level),
                    mc.player, TooltipFlag.Default.NORMAL);
            for (var tip : tooltip) {
                hoverSb.append("\n").append(ChatUtils.toLegacyString(tip));
            }

            var customData = heldItem.get(DataComponents.CUSTOM_DATA);
            if (customData != null && !customData.isEmpty()) {
                Tag tag = customData.copyTag();
                JsonElement je = com.mojang.serialization.Dynamic.convert(NbtOps.INSTANCE, JsonOps.INSTANCE, tag);
                String json = new GsonBuilder().setPrettyPrinting().create().toJson(je);
                hoverSb.append("\n\n§6§l--- CUSTOM_DATA ---");
                hoverSb.append("\n§7").append(json);
            }

            var cmd = heldItem.get(DataComponents.CUSTOM_MODEL_DATA);
            if (cmd != null) {
                hoverSb.append("\n\n§6§l--- CUSTOM_MODEL_DATA ---");
                hoverSb.append("\n§7").append(cmd);
            }

            String copyText = top.babyzombie.addons.util.ItemUtils.formatItemCopyText(heldItem);
            lines.add(Component.literal("§dHolding: §f" + itemName)
                    .withStyle(style -> style
                            .withHoverEvent(new HoverEvent.ShowText(Component.literal(hoverSb.toString())))
                            .withClickEvent(new ClickEvent.CopyToClipboard(copyText))));
        }

        // —— riding (hover shows entity info) ——
        if (entity.getVehicle() != null) {
            lines.add(hover(Tstr("debug.entity.riding",
                    ChatUtils.toLegacyString(entity.getVehicle().getName())),
                    entitySummary(entity.getVehicle())));
        }

        // —— passengers (hover shows entity info) ——
        if (!entity.getPassengers().isEmpty()) {
            var brief = entity.getPassengers().stream()
                    .map(e -> ChatUtils.toLegacyString(e.getName()))
                    .toList();
            var detail = entity.getPassengers().stream()
                    .map(DebugEntityCommand::entitySummary)
                    .reduce((a, b) -> a + "\n" + b).orElse("");
            lines.add(hover(Tstr("debug.entity.passengers", String.join(", ", brief)),
                    detail));
        }

        var result = Component.empty();
        for (int i = 0; i < lines.size(); i++) {
            if (i > 0) result.append(Component.literal("\n"));
            result.append(lines.get(i));
        }
        src.sendFeedback(result);
    }

    private static String entitySummary(Entity e) {
        var pos = e.blockPosition();
        return "§b" + ChatUtils.toLegacyString(e.getName())
                + " §8[" + EntityType.getKey(e.getType()) + " / " + e.getClass().getSimpleName() + "]"
                + "\n§7Pos: §a" + pos.getX() + " " + pos.getY() + " " + pos.getZ()
                + "\n§7UUID: " + e.getUUID()
                + (e instanceof LivingEntity l ? "\n§7HP: §c" + String.format("%.1f", l.getHealth())
                    + "/" + String.format("%.1f", l.getMaxHealth()) : "");
    }

    private static Component hover(String text, String hoverText) {
        return Component.literal(text).withStyle(style ->
                style.withHoverEvent(new HoverEvent.ShowText(Component.literal(hoverText))));
    }

    private static String Tstr(String key, Object... args) {
        return Component.translatable("babyzombieaddons." + key, args).getString();
    }

    /// Returns a formatted scale string, or null if not applicable
    private static String getEntityScale(Entity entity) {
        // Display entities have 3D scale from transformation
        if (entity instanceof net.minecraft.world.entity.Display d) {
            Vector3fc scale = getDisplayScale(d);
            if (scale != null) {
                return String.format("%.2f %.2f %.2f", scale.x(), scale.y(), scale.z());
            }
        }
        // LivingEntity has a global scale attribute
        if (entity instanceof LivingEntity living) {
            return String.format("%.2f", living.getScale());
        }
        return null;
    }

    private static Vector3fc getDisplayScale(net.minecraft.world.entity.Display entity) {
        return entity.getEntityData().get(DisplayAccessor.getDATA_SCALE_ID());
    }
}
