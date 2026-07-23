package top.babyzombie.addons.module.chat.popup;

import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.FishingRodItem;
import top.babyzombie.addons.config.ModConfigManager;
import top.babyzombie.addons.config.hud.HudManager;
import top.babyzombie.addons.event.SendCommandEvents;
import top.babyzombie.addons.util.ChatUtils;
import top.babyzombie.addons.util.ItemUtils;
import top.babyzombie.addons.util.PlaySoundHelper;
import top.babyzombie.addons.util.tracker.HypixelLocationTracker;
import top.babyzombie.addons.util.KeyBindingUtil;
import top.babyzombie.addons.util.ServerTick;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

public final class PopupEventsModule {

    private static final Pattern PARTY_INVITE = Pattern.compile(
            "(?:\\[[\\w+\\+-]+] )?([0-9a-zA-Z_]{2,24})( has invited you to join | has invited all members of .+ to |邀请你加入|已邀请.+中的所有成员加入)(.+?)( party!|组队！)");
    private static final Pattern FRIEND_REQUEST = Pattern.compile(
            "(Friend request from |好友请求：)(?:\\[[\\w+\\+-]+] )?([0-9a-zA-Z_]{2,24})");
    private static final Pattern SKYBLOCK_TRADE = Pattern.compile(
            "([0-9a-zA-Z_]{2,24}) has sent you a trade request\\. Click here to accept!");
    private static final Pattern DUELS_REQUEST = Pattern.compile(
            "(?:\\[[\\w+\\+-]+] )?([0-9a-zA-Z_]{2,24})( has invited you to |邀请你参与)(.+?)[!！]");
    private static final Pattern DUNGEON_RESTART = Pattern.compile(
            "(?:组队|組隊|Party) > (?:\\[[\\w+\\+-]+] )?([0-9a-zA-Z_]+)(?: [♲Ⓑ☀⚒ቾ]+)?: rs",
            Pattern.CASE_INSENSITIVE);

    public enum PopupSound {
        BELL("bell", SoundEvents.BELL_BLOCK),
        NOTE_BLOCK("note_block", SoundEvents.NOTE_BLOCK_PLING.value()),
        EXPERIENCE("experience", SoundEvents.EXPERIENCE_ORB_PICKUP),
        LEVEL_UP("level_up", SoundEvents.PLAYER_LEVELUP),
        DRAGON("dragon", SoundEvents.ENDER_DRAGON_GROWL),
        ANVIL("anvil", SoundEvents.ANVIL_LAND),
        GOAT_HORN("goat_horn", SoundEvents.GOAT_HORN_SOUND_VARIANTS.get(2).value()),
        LAVA_CHICKEN("lava_chicken", SoundEvents.MUSIC_DISC_LAVA_CHICKEN.value());

        public final String key;
        public final SoundEvent sound;

        PopupSound(String key, SoundEvent sound) {
            this.key = key;
            this.sound = sound;
        }
    }

    private static final List<Float> lllllava = Arrays.asList(0.9f, 2.45f, 40.7f, 42.2f);

    private enum EventType {
        PARTY("party_invite"), GUILD_PARTY("guild_party_invite"),
        FRIEND("friend_request"), TRADE("trade_request"),
        POSITION_SWAP("position_swap"), DUEL("duel_request"), RESTART("restart_request"), BAIT("bait_low");

        final String key;
        EventType(String k) { this.key = k; }
    }

    private static Component title = Component.empty();
    private static Component body = Component.empty();
    private static String command = "";
    private static long expireTime;
    private static long totalTime;
    private static EventType eventType;
    public static KeyMapping keyYes;
    public static KeyMapping keyNo;

    private PopupEventsModule() {}

    public static void init() {
        var modCfg = ModConfigManager.get();
        keyYes = KeyBindingUtil.register("key.babyzombieaddons.popup_yes", modCfg.popup.popupYes);
        keyNo  = KeyBindingUtil.register("key.babyzombieaddons.popup_no",  modCfg.popup.popupNo);

        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            if (overlay) return;
            if (!HypixelLocationTracker.getInstance().isOnHypixel()) return;
            var cfg = ModConfigManager.get().popup;
            String text = message.getString();

            boolean isSysMsg = !text.matches("^(公会|Guild|组队|Party|Officer|Co-op) > .+|From .+|(?:\\[[^]]+\\] )?\\w{2,16}: .+");

            if (isSysMsg) {
                if (cfg.popupPartyInvite || cfg.popupGuildPartyInvite) {
                    var m = PARTY_INVITE.matcher(text);
                    if (m.find()) {
                        String player = m.group(1);
                        String what = m.group(3);
                        boolean guild = m.group(2) != null && (m.group(2).contains("all members") || m.group(2).contains("所有成员"));
                        if (guild && cfg.popupGuildPartyInvite)
                            notify(EventType.GUILD_PARTY, player, what);
                        else if (!guild && cfg.popupPartyInvite)
                            notify(EventType.PARTY, player, what);
                        return;
                    }
                }

                if (cfg.popupFriendRequest) {
                    var m = FRIEND_REQUEST.matcher(text);
                    if (m.find()) { notify(EventType.FRIEND, m.group(2), null); return; }
                }

                if (cfg.popupSkyblockTrade && HypixelLocationTracker.getInstance().isInSkyblock()) {
                    var m = SKYBLOCK_TRADE.matcher(ChatUtils.stripColor(text));
                    if (m.find()) {
                        var loc = HypixelLocationTracker.getInstance();
                        EventType t = loc.isInRift()
                                ? EventType.POSITION_SWAP : EventType.TRADE;
                        notify(t, m.group(1), null);
                        return;
                    }
                }

                if (cfg.popupDuelsRequest) {
                    var m = DUELS_REQUEST.matcher(text);
                    if (m.find()) { notify(EventType.DUEL, m.group(1), m.group(3)); return; }
                }
            }

            if (cfg.popupDungeonRestart && HypixelLocationTracker.getInstance().isInDungeon()) {
                var m = DUNGEON_RESTART.matcher(text);
                if (m.find()) { notify(EventType.RESTART, m.group(1), null); return; }
            }
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (expireTime == 0) return;
            while (keyYes.consumeClick()) accept();
            while (keyNo.consumeClick()) close();
        });

        HudElementRegistry.attachElementAfter(VanillaHudElements.OVERLAY_MESSAGE,
                Identifier.fromNamespaceAndPath("babyzombieaddons", "popup_events"),
                (context, tickCounter) -> {
            if (expireTime <= ServerTick.getTime()) { close(); return; }
            renderHUD(context);
        });

        SendCommandEvents.BEFORE_SEND.register(command -> {
            if (expireTime == 0 || expireTime <= ServerTick.getTime()) return false;
            if (PopupEventsModule.command.isEmpty()) return false;
            if (command.replace("/","").equals(PopupEventsModule.command)) {
                close();
            }
            return false;
        });

        UseItemCallback.EVENT.register((player, world, hand) -> {
            var cfg = ModConfigManager.get().popup;
            if (ModConfigManager.get().fishing.popupBaitLow <= 0) return InteractionResult.PASS;
            if (!HypixelLocationTracker.getInstance().isInSkyblock()) return InteractionResult.PASS;
            var held = player.getItemInHand(hand);
            if (!(held.getItem() instanceof FishingRodItem)) return InteractionResult.PASS;
            var baitStack = player.getInventory().getItem(8);
            if (baitStack.isEmpty()) return InteractionResult.PASS;
            String id = ItemUtils.getSkyblockId(baitStack);
            if (id == null || !id.endsWith("_BAIT")) return InteractionResult.PASS;
            int quantity = baitStack.getCount();
            if (quantity >= ModConfigManager.get().fishing.popupBaitLow) return InteractionResult.PASS;
            String baitName = ChatUtils.stripColor(baitStack.getHoverName().getString());
            notify(EventType.BAIT, baitName, String.valueOf(ModConfigManager.get().fishing.popupBaitLow));
            return InteractionResult.PASS;
        });
    }

    private static void notify(EventType type, String player, String extra) {
        eventType = type;
        String pre = "babyzombieaddons.popup.";
        title = Component.translatable(pre + "title." + type.key);
        if (extra != null) {
            if ("their".equals(extra))
                body = Component.translatable(pre + "body." + type.key + "_their", "§6" + player + "§f");
            else
                body = Component.translatable(pre + "body." + type.key, "§6" + player + "§f", "§6" + extra + "§f");
        } else
            body = Component.translatable(pre + "body." + type.key, "§6" + player + "§f");
        command = type == EventType.PARTY || type == EventType.GUILD_PARTY ? "party accept " + player
                : type == EventType.FRIEND ? "friend accept " + player
                : type == EventType.TRADE || type == EventType.POSITION_SWAP ? "trade " + player
                : type == EventType.DUEL ? "duel accept " + player
                : type == EventType.BAIT ? "bz " + player
                : "";
        totalTime = 10000;
        expireTime = ServerTick.getTime() + totalTime;
        playSound();
    }

    private static void playSound() {
        var player = Minecraft.getInstance().player;
        if (player == null) return;
        var popupSound = ModConfigManager.get().popup.popupSound;
        if (popupSound == PopupSound.LAVA_CHICKEN) {
            var instance = new SimpleSoundInstance(
                    popupSound.sound.location(), SoundSource.MASTER,
                    1f, 1f,
                    SoundInstance.createUnseededRandom(),
                    false, 0,
                    SoundInstance.Attenuation.NONE,
                    0, 0, 0, true
            );
            PlaySoundHelper.playSeeked(instance, lllllava.get((int) (Math.random() * lllllava.size())), 1.5f);
        } else {
            player.level().playSound(player, player.blockPosition(),
                    popupSound.sound, SoundSource.MASTER, 1f, 1f);
        }
    }

    private static void accept() {
        if (!command.isEmpty() && expireTime > ServerTick.getTime())
            ChatUtils.sendCommand(command);
        close();
    }

    private static void close() {
        title = Component.empty(); body = Component.empty();
        command = ""; expireTime = 0; totalTime = 0;
        eventType = null;
    }

    private static int titleColor() {
        if (eventType == null) return 0xFFFFFFFF;
        return switch (eventType) {
            case PARTY, GUILD_PARTY -> 0xFFFFAA00;
            case FRIEND -> 0xFF55FF55;
            case TRADE, POSITION_SWAP -> 0xFF55FFFF;
            case DUEL -> 0xFFFF5555;
            case RESTART -> 0xFFFFFF55;
            case BAIT -> 0xFF55FF55;
        };
    }

    private static void renderHUD(GuiGraphicsExtractor gui) {
        long remaining = expireTime - ServerTick.getTime();
        if (remaining <= 0) { close(); return; }
        var font = Minecraft.getInstance().font;
        int x = HudManager.x("Popup"), y = HudManager.y("Popup");
        float s = HudManager.scale("Popup");
        if (s != 1f) {
            var ps = gui.pose();
            ps.pushMatrix();
            ps.translate((float) x, (float) y);
            ps.scale(s, s);
            x = 0; y = 0;
        }

        var bodyLines = font.split(body, 148);
        int lh = font.lineHeight;
        float titleScale = 1.5f;
        int titleH = (int) Math.ceil(lh * titleScale) + 2;
        int bodyY = y + titleH + 4;
        int boxH = titleH + 6 + bodyLines.size() * lh + 4 + lh + 6;

        gui.fill(x, y, x + 152, y + boxH, 0x96000000);

        var ps = gui.pose();
        ps.pushMatrix();
        ps.translate(x + 76, y + titleH / 2f);
        ps.scale(titleScale, titleScale);
        gui.centeredText(font, title.getString(), 0, -lh / 2, titleColor());
        ps.popMatrix();

        for (int i = 0; i < bodyLines.size(); i++) {
            gui.text(font, bodyLines.get(i), x + 1, bodyY + i * lh, 0xFFFFFFFF, false);
        }

        int bottomY = y + boxH - 6;
        float progress = 1f - (float) remaining / totalTime;
        gui.fill(x, bottomY, x + (int)(152 * progress), y + boxH, 0x46FFFFFF);

        String kbYes = keyYes.getTranslatedKeyMessage().getString();
        String kbNo  = keyNo.getTranslatedKeyMessage().getString();
        Component accept = Component.translatable("babyzombieaddons.popup.accept", kbYes);
        Component ignore = Component.translatable("babyzombieaddons.popup.ignore", kbNo);
        String hint = "§a" + accept.getString() + "   §e" + ignore.getString();
        int hintY = bottomY - lh + 1;
        gui.text(font, "§7" + (remaining / 1000 + 1) + "s", x + 1, hintY, 0xFFFFFFFF, false);
        gui.text(font, hint, x + 150 - font.width(hint), hintY, 0xFFFFFFFF, false);

        if (s != 1f) gui.pose().popMatrix();
    }
}
