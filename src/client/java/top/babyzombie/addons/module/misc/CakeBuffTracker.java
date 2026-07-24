package top.babyzombie.addons.module.misc;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLevelEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import top.babyzombie.addons.config.ModConfigManager;
import top.babyzombie.addons.config.hud.HudManager;
import top.babyzombie.addons.util.ChatUtils;
import top.babyzombie.addons.util.ItemUtils;
import top.babyzombie.addons.util.ServerTick;
import top.babyzombie.addons.util.render.BeamRenderer;
import top.babyzombie.addons.util.render.RenderPhaseRegister;
import top.babyzombie.addons.util.tracker.HypixelLocationTracker;

public final class CakeBuffTracker {

    private static final Map<String, Integer> CAKE_INDEX = new LinkedHashMap<>();
    private static final String[] NAMES;
    static {
        // PUA icons from Hypixel official Skyblock resource pack (assets/minecraft/font/default.json)
        // \uE010=health, \uE008=defense, \uE00D=strength, \uE022=speed,
        // \uE003=intelligence, \uE00B=ferocity, \uE028=vitality, \uE027=true defense,
        // \uE021=sea creature chance, \uE01A=magic find, \uE013=pet luck,
        // \uE006=cold resistance, \uE020=rift time,
        // \uE053=mining fortune, \uE051=farming fortune, \uE054=foraging fortune,
        // \uE025=treasure chance, \uE077=tracking, \uE023=sweep,
        // \uE05B=hunter fortune
        Object[][] data = {
                {"10\uE010 Health", "§c10\uE010 Health   "},
                {"3\uE008 Defense", "§a3\uE008 Defense   "},
                {"2\uE00D Strength", "§c2\uE00D Strength   "},
                {"10\uE022 Speed", "§f10\uE022 Speed   "},
                {"5\uE003 Intelligence", "§b5\uE003 Intelligence   "},
                {"2\uE00B Ferocity", "§c2\uE00B Ferocity   "},
                {"1\uE028 Vitality", "§41\uE028 Vitality   "},
                {"1\uE027 True Defense", "§f1\uE027 True Defense   "},
                {"1\uE021 Sea Creature Chance", "§31\uE021 Sea Creature Chance   "},
                {"1\uE01A Magic Find", "§b1\uE01A Magic Find   "},
                {"1\uE013 Pet Luck", "§d1\uE013 Pet Luck   "},
                {"1\uE006 Cold Resistance", "§b1\uE006 Cold Resistance   "},
                {"10\uE020 Rift Time", "§a10\uE020 Rift Time   "},
                {"5\uE053 Mining Fortune", "§65\uE053 Mining Fortune   "},
                {"5\uE051 Farming Fortune", "§65\uE051 Farming Fortune   "},
                {"5\uE054 Foraging Fortune", "§65\uE054 Foraging Fortune   "},
                {"1\uE025 Treasure Chance", "§61\uE025 Treasure Chance   "},
                {"1\uE077 Tracking", "§d1\uE077 Tracking   "},
                {"5\uE023 Sweep", "§25\uE023 Sweep   "},
                {"1\uE05B Hunter Fortune", "§d1\uE05B Hunter Fortune   "}
        };
        NAMES = new String[data.length];
        for (int i = 0; i < data.length; i++) {
            CAKE_INDEX.put((String) data[i][0], i);
            NAMES[i] = (String) data[i][1];
        }
    }

    private enum skullTexture {
        HEALTH("ewogICJ0aW1lc3RhbXAiIDogMTcwODM1MTMzMjczMSwKICAicHJvZmlsZUlkIiA6ICJmOGJmNDBjOWExYzY0ZTllOTIyZTc4M2UwMzNiODBiMyIsCiAgInByb2ZpbGVOYW1lIiA6ICJUeGxvbjUiLAogICJzaWduYXR1cmVSZXF1aXJlZCIgOiB0cnVlLAogICJ0ZXh0dXJlcyIgOiB7CiAgICAiU0tJTiIgOiB7CiAgICAgICJ1cmwiIDogImh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMjExODU2NzY0MTY2NTNiZjcwNjAzYzcyM2ZmN2E0OGUwODBkYjY0OWE4Y2U1ZDY1YjQ2MWJmNjU0ODc0OTM1YSIsCiAgICAgICJtZXRhZGF0YSIgOiB7CiAgICAgICAgIm1vZGVsIiA6ICJzbGltIgogICAgICB9CiAgICB9CiAgfQp9"),
        DEFENSE("ewogICJ0aW1lc3RhbXAiIDogMTYyMjI3MzY1NzM1NCwKICAicHJvZmlsZUlkIiA6ICJjOTAzOGQzZjRiMTg0M2JiYjUwNTU5ZGE3MWFjMTk2MiIsCiAgInByb2ZpbGVOYW1lIiA6ICJUQk5SY29vbGNhdCIsCiAgInNpZ25hdHVyZVJlcXVpcmVkIiA6IHRydWUsCiAgInRleHR1cmVzIiA6IHsKICAgICJTS0lOIiA6IHsKICAgICAgInVybCIgOiAiaHR0cDovL3RleHR1cmVzLm1pbmVjcmFmdC5uZXQvdGV4dHVyZS9iYTdiZmE4MGE4NWU3MDdlZWRmOTQ1Yjg4OTA1OTQyZjVmNzc5NWVhNTI2NGQ2OTJhMjJlNzA3ZmM1NzdhODhiIiwKICAgICAgIm1ldGFkYXRhIiA6IHsKICAgICAgICAibW9kZWwiIDogInNsaW0iCiAgICAgIH0KICAgIH0KICB9Cn0="),
        STRENGTH("ewogICJ0aW1lc3RhbXAiIDogMTY2MDEwMzYwODk2NywKICAicHJvZmlsZUlkIiA6ICI3NTE0NDQ4MTkxZTY0NTQ2OGM5NzM5YTZlMzk1N2JlYiIsCiAgInByb2ZpbGVOYW1lIiA6ICJUaGFua3NNb2phbmciLAogICJzaWduYXR1cmVSZXF1aXJlZCIgOiB0cnVlLAogICJ0ZXh0dXJlcyIgOiB7CiAgICAiU0tJTiIgOiB7CiAgICAgICJ1cmwiIDogImh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNTBmMTQ0ZmQ0ZDdjODllMDNmMzVkN2VmZTQxZDM5NzgzOTY4MjM1OTE2YTAwOWU0ZDBmODgyZWMyYzhlNmViNyIKICAgIH0KICB9Cn0="),
        SPEED("ewogICJ0aW1lc3RhbXAiIDogMTYyMjMwMDI1NDI3MSwKICAicHJvZmlsZUlkIiA6ICIwYTUzMDU0MTM4YWI0YjIyOTVhMGNlZmJiMGU4MmFkYiIsCiAgInByb2ZpbGVOYW1lIiA6ICJQX0hpc2lybyIsCiAgInNpZ25hdHVyZVJlcXVpcmVkIiA6IHRydWUsCiAgInRleHR1cmVzIiA6IHsKICAgICJTS0lOIiA6IHsKICAgICAgInVybCIgOiAiaHR0cDovL3RleHR1cmVzLm1pbmVjcmFmdC5uZXQvdGV4dHVyZS9lMjU1ODg5OTliNjc0YjM5OWExOTgwYTI1ZDljYTMyMzYzOWQwMzNlM2Y4NTA0MGMxZGU3ZmZiNDI4YmNhMDk1IiwKICAgICAgIm1ldGFkYXRhIiA6IHsKICAgICAgICAibW9kZWwiIDogInNsaW0iCiAgICAgIH0KICAgIH0KICB9Cn0="),
        INTELLIGENCE("ewogICJ0aW1lc3RhbXAiIDogMTcxOTQ1NjgzMDYzNiwKICAicHJvZmlsZUlkIiA6ICJlZGUyYzdhMGFjNjM0MTNiYjA5ZDNmMGJlZTllYzhlYyIsCiAgInByb2ZpbGVOYW1lIiA6ICJ0aGVEZXZKYWRlIiwKICAic2lnbmF0dXJlUmVxdWlyZWQiIDogdHJ1ZSwKICAidGV4dHVyZXMiIDogewogICAgIlNLSU4iIDogewogICAgICAidXJsIiA6ICJodHRwOi8vdGV4dHVyZXMubWluZWNyYWZ0Lm5ldC90ZXh0dXJlLzVlZmYwOTA0ZDVlYzY5MWRkOGUzOGQxZjYzZDM4YmVmMDQ5MjAxM2VjZTkzZDMyYWY5MGRjMDMyMDExNmM1OTciLAogICAgICAibWV0YWRhdGEiIDogewogICAgICAgICJtb2RlbCIgOiAic2xpbSIKICAgICAgfQogICAgfQogIH0KfQ=="),
        FEROCITY("ewogICJ0aW1lc3RhbXAiIDogMTcxOTQ1NzE2MTczOSwKICAicHJvZmlsZUlkIiA6ICI4MDBmNmU2ZGNiMTk0Yzc2OGE1OWU1Y2Q2MzFlNjI2YyIsCiAgInByb2ZpbGVOYW1lIiA6ICJ5dXl1dXUxIiwKICAic2lnbmF0dXJlUmVxdWlyZWQiIDogdHJ1ZSwKICAidGV4dHVyZXMiIDogewogICAgIlNLSU4iIDogewogICAgICAidXJsIiA6ICJodHRwOi8vdGV4dHVyZXMubWluZWNyYWZ0Lm5ldC90ZXh0dXJlL2Y1OGNkMjc1MDg4NDQ3MWMwODM2MzQzMzQzZWM2ZTE4ODIyOWRhMDU0ZWYzNjcxMDEzYWU4ZDQzZDZiMDI1NDgiLAogICAgICAibWV0YWRhdGEiIDogewogICAgICAgICJtb2RlbCIgOiAic2xpbSIKICAgICAgfQogICAgfQogIH0KfQ=="),
        VITALITY("ewogICJ0aW1lc3RhbXAiIDogMTY5MjgwNDA3NTczNSwKICAicHJvZmlsZUlkIiA6ICIwYmM5ZDc3YmQ1YTA0NWMzOTY4MWUzYTRhNDIzODZlMyIsCiAgInByb2ZpbGVOYW1lIiA6ICJWaXRvcmlpaW5oYSIsCiAgInNpZ25hdHVyZVJlcXVpcmVkIiA6IHRydWUsCiAgInRleHR1cmVzIiA6IHsKICAgICJTS0lOIiA6IHsKICAgICAgInVybCIgOiAiaHR0cDovL3RleHR1cmVzLm1pbmVjcmFmdC5uZXQvdGV4dHVyZS9jMzgzMGQyMzVjOWRlNzAzOTQ2ODY0YzI4MWY0MmY5ZDQ1NjQ3NjkxZWUzNTNmYmJiMDcyMjcxNzcwNmI4YmRhIiwKICAgICAgIm1ldGFkYXRhIiA6IHsKICAgICAgICAibW9kZWwiIDogInNsaW0iCiAgICAgIH0KICAgIH0KICB9Cn0="),
        TRUE_DEFENSE("ewogICJ0aW1lc3RhbXAiIDogMTY5MjgwMzkwNTI0NSwKICAicHJvZmlsZUlkIiA6ICIzNTE2NjhhMTk5MmM0ZGZlOWRkNmY5NTUxNWFkNzVmNyIsCiAgInByb2ZpbGVOYW1lIiA6ICJCbHVlX1BrIiwKICAic2lnbmF0dXJlUmVxdWlyZWQiIDogdHJ1ZSwKICAidGV4dHVyZXMiIDogewogICAgIlNLSU4iIDogewogICAgICAidXJsIiA6ICJodHRwOi8vdGV4dHVyZXMubWluZWNyYWZ0Lm5ldC90ZXh0dXJlL2Q0MGY5Mjk5NTA4Mzg3NjE4Y2ZhZDZkYjM1YzlmNmQ4MDdjNDkzMzdkMzMzZDZlYzNiMTNkOWU4N2QwZDQ4OGYiLAogICAgICAibWV0YWRhdGEiIDogewogICAgICAgICJtb2RlbCIgOiAic2xpbSIKICAgICAgfQogICAgfQogIH0KfQ=="),
        SEA_CREATURE_CHANCE("ewogICJ0aW1lc3RhbXAiIDogMTcxOTQ1Njk0MzMwMiwKICAicHJvZmlsZUlkIiA6ICI3MzE4MWQxZDRjYWQ0ZmU0YTcxNWNjNmUxOGNjYzVkNyIsCiAgInByb2ZpbGVOYW1lIiA6ICJaZmVybjRuZGl0byIsCiAgInNpZ25hdHVyZVJlcXVpcmVkIiA6IHRydWUsCiAgInRleHR1cmVzIiA6IHsKICAgICJTS0lOIiA6IHsKICAgICAgInVybCIgOiAiaHR0cDovL3RleHR1cmVzLm1pbmVjcmFmdC5uZXQvdGV4dHVyZS9lMTJhMjhlNjlkYTVlNDNjZThmZGVjYzZhODIzYzkyZmZmODQ3MTllMDE0N2NhYWM3MWM1YzhkOTlkYTU3ODFjIiwKICAgICAgIm1ldGFkYXRhIiA6IHsKICAgICAgICAibW9kZWwiIDogInNsaW0iCiAgICAgIH0KICAgIH0KICB9Cn0="),
        MAGIC_FIND("ewogICJ0aW1lc3RhbXAiIDogMTcxOTQ1NjkxMjk0OCwKICAicHJvZmlsZUlkIiA6ICI1ZjU5NmViY2JlOTQ0NmQxYmI0M2JlNGYzZjRiOGJlNSIsCiAgInByb2ZpbGVOYW1lIiA6ICJUZWlsMHNzIiwKICAic2lnbmF0dXJlUmVxdWlyZWQiIDogdHJ1ZSwKICAidGV4dHVyZXMiIDogewogICAgIlNLSU4iIDogewogICAgICAidXJsIiA6ICJodHRwOi8vdGV4dHVyZXMubWluZWNyYWZ0Lm5ldC90ZXh0dXJlLzY0NTkyNzQ2Y2MzNTY3MmVlMjliYjNhMTUzZDc0ZjdhNWQwZTVmZTg2MTI3MjlhMzRmYWQyNTJmZTc5MDQyODMiLAogICAgICAibWV0YWRhdGEiIDogewogICAgICAgICJtb2RlbCIgOiAic2xpbSIKICAgICAgfQogICAgfQogIH0KfQ=="),
        PET_LUCK("ewogICJ0aW1lc3RhbXAiIDogMTYyMjI5OTg2NzkxMiwKICAicHJvZmlsZUlkIiA6ICJmMjU5MTFiOTZkZDU0MjJhYTcwNzNiOTBmOGI4MTUyMyIsCiAgInByb2ZpbGVOYW1lIiA6ICJmYXJsb3VjaDEwMCIsCiAgInNpZ25hdHVyZVJlcXVpcmVkIiA6IHRydWUsCiAgInRleHR1cmVzIiA6IHsKICAgICJTS0lOIiA6IHsKICAgICAgInVybCIgOiAiaHR0cDovL3RleHR1cmVzLm1pbmVjcmFmdC5uZXQvdGV4dHVyZS8yNWQ5MjQzYzYxZTJjM2UxZTBmOTkzMTg0M2NhZGJkMGRmZjRhNjQ2MjI1ZWY1NGM1NTc5NzljZjM5YmIyMDU3IiwKICAgICAgIm1ldGFkYXRhIiA6IHsKICAgICAgICAibW9kZWwiIDogInNsaW0iCiAgICAgIH0KICAgIH0KICB9Cn0="),
        COLD_RESISTANCE("ewogICJ0aW1lc3RhbXAiIDogMTcyOTYxOTM4NjE2NCwKICAicHJvZmlsZUlkIiA6ICIzZGE2ZDgxOTI5MTY0MTNlODhlNzg2MjQ3NzA4YjkzZSIsCiAgInByb2ZpbGVOYW1lIiA6ICJGZXJTdGlsZSIsCiAgInNpZ25hdHVyZVJlcXVpcmVkIiA6IHRydWUsCiAgInRleHR1cmVzIiA6IHsKICAgICJTS0lOIiA6IHsKICAgICAgInVybCIgOiAiaHR0cDovL3RleHR1cmVzLm1pbmVjcmFmdC5uZXQvdGV4dHVyZS80OThjODhkYWZlMTMzZTc5N2YyOTk0YWNjMDUzYjM5MjZlOTI5MWQwYjcwOTM1OThiMzlkMjcyYzdjOTRhMzY2IiwKICAgICAgIm1ldGFkYXRhIiA6IHsKICAgICAgICAibW9kZWwiIDogInNsaW0iCiAgICAgIH0KICAgIH0KICB9Cn0="),
        RIFT_TIME("ewogICJ0aW1lc3RhbXAiIDogMTcyOTYxOTQ4NTk3OCwKICAicHJvZmlsZUlkIiA6ICIyYjcyZWYyYWUzMmQ0Zjc1OGEyMThlMDI4MTViYmNjZSIsCiAgInByb2ZpbGVOYW1lIiA6ICJ2b2xrb2RhZl82MyIsCiAgInNpZ25hdHVyZVJlcXVpcmVkIiA6IHRydWUsCiAgInRleHR1cmVzIiA6IHsKICAgICJTS0lOIiA6IHsKICAgICAgInVybCIgOiAiaHR0cDovL3RleHR1cmVzLm1pbmVjcmFmdC5uZXQvdGV4dHVyZS9kMzdiMWZiNGQwOTgxOTAxYTI5MmM0MmM2NTM5NDZjYTBlMmRlYTE4YmU5Y2FmYTJmMmY5OTMzZmRhYmFhOTg0IiwKICAgICAgIm1ldGFkYXRhIiA6IHsKICAgICAgICAibW9kZWwiIDogInNsaW0iCiAgICAgIH0KICAgIH0KICB9Cn0="),
        MINING_FORTUNE("ewogICJ0aW1lc3RhbXAiIDogMTY0Nzk1NDY2NDEyNywKICAicHJvZmlsZUlkIiA6ICI1MTY4ZjZlMjIyM2E0Y2FjYjdiN2QyZjYyZWMxZGFhOSIsCiAgInByb2ZpbGVOYW1lIiA6ICJkZWZfbm90X2FzaCIsCiAgInNpZ25hdHVyZVJlcXVpcmVkIiA6IHRydWUsCiAgInRleHR1cmVzIiA6IHsKICAgICJTS0lOIiA6IHsKICAgICAgInVybCIgOiAiaHR0cDovL3RleHR1cmVzLm1pbmVjcmFmdC5uZXQvdGV4dHVyZS9mYjdjZmJiNmFmNzAwZTQ5YmZkYjYwMjM2OTIwYzUyMjA4NGJiODA4YWI2MTI0NzRmYjFmODNlYTQxMGQ1NDg0IgogICAgfQogIH0KfQ=="),
        FARMING_FORTUNE("ewogICJ0aW1lc3RhbXAiIDogMTY0Nzk1NDY4MzMwMCwKICAicHJvZmlsZUlkIiA6ICJlMmVkYTM1YjMzZGU0M2UxOTVhZmRkNDgxNzQ4ZDlhOSIsCiAgInByb2ZpbGVOYW1lIiA6ICJDaGFsa19SaWNlR0kiLAogICJzaWduYXR1cmVSZXF1aXJlZCIgOiB0cnVlLAogICJ0ZXh0dXJlcyIgOiB7CiAgICAiU0tJTiIgOiB7CiAgICAgICJ1cmwiIDogImh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYjIzN2NkZmQ3ZDNmMjBlMGY1YjY5N2EyMGM4NTMyNGZiYjdiY2Q0MDllNjUzMzM3Y2NkNWE5Y2U4Y2NhZmMxZCIKICAgIH0KICB9Cn0="),
        FORAGING_FORTUNE("ewogICJ0aW1lc3RhbXAiIDogMTY0Nzk1NDY0ODIwMCwKICAicHJvZmlsZUlkIiA6ICI4Zjk3NzhmNWVhMTY0MDVmOWEwMDM0YjU4YjljMWM2MCIsCiAgInByb2ZpbGVOYW1lIiA6ICJ1bm5hbWVkYXV0aG9yIiwKICAic2lnbmF0dXJlUmVxdWlyZWQiIDogdHJ1ZSwKICAidGV4dHVyZXMiIDogewogICAgIlNLSU4iIDogewogICAgICAidXJsIiA6ICJodHRwOi8vdGV4dHVyZXMubWluZWNyYWZ0Lm5ldC90ZXh0dXJlL2JhMGE1YjUxMGE4ZDgyNGZmNDkxMGNhNWIyNjk4YWEzZDAzMGY4Mzc4MTBlOGQ3ZjBiYmNhOGNmMDZjZTIwMjMiCiAgICB9CiAgfQp9"),
        TREASURE_CHANCE("ewogICJ0aW1lc3RhbXAiIDogMTc4MDkwNjExNTY3NSwKICAicHJvZmlsZUlkIiA6ICI0MDUxNzNiODY0OTM0NTUxOThlOGMzOGJmNmJmNjZiMSIsCiAgInByb2ZpbGVOYW1lIiA6ICJzcGFjZWNhZGV0MTgiLAogICJzaWduYXR1cmVSZXF1aXJlZCIgOiB0cnVlLAogICJ0ZXh0dXJlcyIgOiB7CiAgICAiU0tJTiIgOiB7CiAgICAgICJ1cmwiIDogImh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNjNkMjIwZjIzNWYyODRmY2M1ZmRjNDVkZGQ5Zjk3ZDY2NjA0ZGEyYTZhZDQxYWI4OTRhNjNmZGU4YjdlNDk5NSIsCiAgICAgICJtZXRhZGF0YSIgOiB7CiAgICAgICAgIm1vZGVsIiA6ICJzbGltIgogICAgICB9CiAgICB9CiAgfQp9"),
        TRACKING("ewogICJ0aW1lc3RhbXAiIDogMTc4MDkwNjI3Nzk5MywKICAicHJvZmlsZUlkIiA6ICI2NDEwZjRiZjMwNDU0OTdmODBjZDI4NWIyYmJiNTk5NSIsCiAgInByb2ZpbGVOYW1lIiA6ICJNaW5lU2tpbl8xNSIsCiAgInNpZ25hdHVyZVJlcXVpcmVkIiA6IHRydWUsCiAgInRleHR1cmVzIiA6IHsKICAgICJTS0lOIiA6IHsKICAgICAgInVybCIgOiAiaHR0cDovL3RleHR1cmVzLm1pbmVjcmFmdC5uZXQvdGV4dHVyZS8zYjhhM2UyMmQ5N2MwMzFhM2YyZjg3Yzg1ZjgzMWI0YWJmYTQ3MzU4ZDMyNDljYWMwNDNhYmU5OTlmNTQ5YWI2IiwKICAgICAgIm1ldGFkYXRhIiA6IHsKICAgICAgICAibW9kZWwiIDogInNsaW0iCiAgICAgIH0KICAgIH0KICB9Cn0="),
        SWEEP("ewogICJ0aW1lc3RhbXAiIDogMTc4MDkwNjM0Nzk2OSwKICAicHJvZmlsZUlkIiA6ICIwNTljODIxYzhhODU0NGJiOWJiODVhOGMxNjVhYTc5YiIsCiAgInByb2ZpbGVOYW1lIiA6ICJoZWxsc3RydWNrZWR6IiwKICAic2lnbmF0dXJlUmVxdWlyZWQiIDogdHJ1ZSwKICAidGV4dHVyZXMiIDogewogICAgIlNLSU4iIDogewogICAgICAidXJsIiA6ICJodHRwOi8vdGV4dHVyZXMubWluZWNyYWZ0Lm5ldC90ZXh0dXJlLzQ1NDVjNmJmZDc3MzAyNDUxMWUyMzVlMTJmNmEyYTJlZGUwNjBjYjhkYmJhOTVlMWZjMjQ1YzE2MDEwNjc2YWIiLAogICAgICAibWV0YWRhdGEiIDogewogICAgICAgICJtb2RlbCIgOiAic2xpbSIKICAgICAgfQogICAgfQogIH0KfQ=="),
        HUNTER_FORTUNE("ewogICJ0aW1lc3RhbXAiIDogMTc4MDkwNjE3MzM2MiwKICAicHJvZmlsZUlkIiA6ICI2ZjMzNmQxNzI4ODY0ZTJkOTYxNTFjOGY4YjkzMGFjOCIsCiAgInByb2ZpbGVOYW1lIiA6ICJTVVNfQUoiLAogICJzaWduYXR1cmVSZXF1aXJlZCIgOiB0cnVlLAogICJ0ZXh0dXJlcyIgOiB7CiAgICAiU0tJTiIgOiB7CiAgICAgICJ1cmwiIDogImh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZjA5MzljYzk4Y2VlMjBkODQ4ODljYTBkY2JjODNlMzVkZjcwYzNjOTgyNDViZjJlNmU2ZjZhM2FkMjg0Y2M3ZSIsCiAgICAgICJtZXRhZGF0YSIgOiB7CiAgICAgICAgIm1vZGVsIiA6ICJzbGltIgogICAgICB9CiAgICB9CiAgfQp9");

        final String texture;
        skullTexture(String texture) {this.texture = texture;}
    }

    private static final boolean[] found = new boolean[CAKE_INDEX.size()];
    private static long lastEatTime;
    private static String checklistText;

    // —— 光柱渲染 ——
    /// base64 纹理 → 蛋糕索引，一次构建 O(1) 查找
    private static final Map<String, Integer> TEXTURE_TO_INDEX = new HashMap<>(20);
    /// § 颜色码 → ARGB 颜色值
    private static final Map<Character, Integer> CODE_TO_COLOR = new HashMap<>(16);
    static {
        for (var tex : skullTexture.values()) {
            TEXTURE_TO_INDEX.put(tex.texture, tex.ordinal());
        }

        CODE_TO_COLOR.put('0', 0xFF000000);
        CODE_TO_COLOR.put('1', 0xFF0000AA);
        CODE_TO_COLOR.put('2', 0xFF00AA00);
        CODE_TO_COLOR.put('3', 0xFF00AAAA);
        CODE_TO_COLOR.put('4', 0xFFAA0000);
        CODE_TO_COLOR.put('5', 0xFFAA00AA);
        CODE_TO_COLOR.put('6', 0xFFFFAA00);
        CODE_TO_COLOR.put('7', 0xFFAAAAAA);
        CODE_TO_COLOR.put('8', 0xFF555555);
        CODE_TO_COLOR.put('9', 0xFF5555FF);
        CODE_TO_COLOR.put('a', 0xFF55FF55);
        CODE_TO_COLOR.put('b', 0xFF55FFFF);
        CODE_TO_COLOR.put('c', 0xFFFF5555);
        CODE_TO_COLOR.put('d', 0xFFFF55FF);
        CODE_TO_COLOR.put('e', 0xFFFFFF55);
        CODE_TO_COLOR.put('f', 0xFFFFFFFF);
    }

    private static final float BEAM_HALF_WIDTH = 0.15f;
    private static final double BEAM_HEIGHT = 2048;
    private static final double SCAN_RANGE = 24;

    private CakeBuffTracker() {}

    public static void init() {
        // Cancel the original Hypixel cake message
        ClientReceiveMessageEvents.ALLOW_GAME.register((message, overlay) -> {
            if (overlay) return true;
            if (!ModConfigManager.get().skyblock.cakeBuffTracker) return true;
            var tracker = HypixelLocationTracker.getInstance();
            if (!tracker.isIn("Private Island")) return true;

            String text = ChatUtils.stripColor(message.getString());
            if (text.startsWith("Yum! You gain +") || text.startsWith("Big Yum! You refresh +")) {
                if (text.endsWith(" for 48 hours!")) {
                    onCakeEaten(text);
                    return false; // Cancel original message
                }
            }
            return true;
        });

        // Reset timer on world change
        ClientLevelEvents.AFTER_CLIENT_LEVEL_CHANGE.register((client, world) -> {
            lastEatTime = 0;
        });

        // HUD rendering — shows for 60 seconds after eating a cake
        HudElementRegistry.attachElementAfter(VanillaHudElements.OVERLAY_MESSAGE,
                Identifier.fromNamespaceAndPath("babyzombieaddons", "cake_buff_tracker"),
                (context, tickCounter) -> {
            if (!ModConfigManager.get().skyblock.cakeBuffTracker) return;
            if (checklistText == null) return;
            long elapsed = ServerTick.getTime() - lastEatTime;
            if (elapsed > 30_000) {
                checklistText = null;
                return;
            }

            var font = Minecraft.getInstance().font;
            int x = HudManager.x("CakeBuffTracker");
            int y = HudManager.y("CakeBuffTracker");
            float s = HudManager.scale("CakeBuffTracker");
            HudManager.drawScaled(context, font, checklistText, x, y, s);
        });

        // 给还没吃的蛋糕盔甲架画信标光柱
        RenderPhaseRegister.register(ctx -> {
            if (!ModConfigManager.get().skyblock.cakeBuffTracker) return;
            if (checklistText == null) return;
            if (!HypixelLocationTracker.getInstance().isIn("Private Island")) return;

            var player = Minecraft.getInstance().player;
            if (player == null) return;

            var stands = player.level().getEntitiesOfClass(
                    ArmorStand.class,
                    new AABB(player.blockPosition()).inflate(SCAN_RANGE, 128, SCAN_RANGE),
                    e -> !e.isDeadOrDying() && e instanceof ArmorStand as && as.isSmall()
            );

            for (var stand : stands) {
                ItemStack head = stand.getItemBySlot(EquipmentSlot.HEAD);
                if (head.isEmpty()) continue;
                String tex = ItemUtils.getSkullTexture(head);
                if (tex == null) continue;
                Integer idx = TEXTURE_TO_INDEX.get(tex);
                if (idx == null || found[idx]) continue;
                BeamRenderer.drawBeam(ctx,
                        stand.getX(), stand.getY() + 0.3, stand.getZ(),
                        BEAM_HEIGHT, BEAM_HALF_WIDTH, getBeamColor(idx));
            }
        });
    }

    /// 从 NAMES[idx] 的 § 颜色码提取 ARGB 光柱颜色
    private static int getBeamColor(int idx) {
        String name = NAMES[idx];
        if (name.length() > 1 && name.charAt(0) == '§') {
            Integer color = CODE_TO_COLOR.get(Character.toLowerCase(name.charAt(1)));
            if (color != null) return color;
        }
        return 0xFFFFAA00; // fallback: 金色
    }

    private static void onCakeEaten(String text) {
        String cakeName = text
            .replace("Yum! You gain +", "")
            .replace("Big Yum! You refresh +", "")
            .replace(" for 48 hours!", "");

        if (cakeName.equals(text)) return;
        Integer idx = CAKE_INDEX.get(cakeName);
        if (idx == null) return;

        found[idx] = true;
        lastEatTime = ServerTick.getTime();
        checklistText = buildChecklist();

        if (!checklistText.contains("✘")) {
            ChatUtils.showMessage(
                    Component.translatable("babyzombieaddons.cake.all_eaten").getString());
        }
    }

    private static String buildChecklist() {
        var sb = new StringBuilder(512);
        for (int i = 0; i < NAMES.length; i++) {
            if (i > 0) sb.append('\n');
            sb.append(NAMES[i]);
            sb.append(found[i] ? "§a✔" : "§c✘");
        }
        return sb.toString();
    }
}
