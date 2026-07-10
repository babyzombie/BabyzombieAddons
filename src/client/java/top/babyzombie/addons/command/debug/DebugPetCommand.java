package top.babyzombie.addons.command.debug;

import com.mojang.brigadier.builder.ArgumentBuilder;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.network.chat.Component;
import top.babyzombie.addons.util.pet.*;
import top.babyzombie.addons.util.pet.state.PlayerPetState;

import java.util.List;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommands.literal;

/**
 * /bza debug pet — shows detailed pet XP calculation info.
 */
public final class DebugPetCommand {
    private DebugPetCommand() {}

    private static final String PFX = "babyzombieaddons.debug.pet.";

    public static void register(ArgumentBuilder<FabricClientCommandSource, ?> parent) {
        parent.then(literal("pet").executes(ctx -> {
            dumpPetInfo(ctx.getSource());
            return 1;
        }));
    }

    private static void dumpPetInfo(FabricClientCommandSource src) {
        PetManager pm = PetManager.getInstance();
        PlayerPetState state = pm.getPetState();
        PetConstants.getInstance().ensureLoaded();

        // ---- Summoned Pet ----
        src.sendFeedback(Component.translatable(PFX + "summoned_header",
            String.valueOf(pm.getPets().size())));

        PetData current = pm.getCurrentPet();
        if (current != null) {
            SkillType primary = current.getPrimarySkill();
            src.sendFeedback(Component.translatable(PFX + "type",
                current.type(), current.getLevel()));
            src.sendFeedback(Component.translatable(PFX + "primary_skill",
                primary != null ? primary.name() : t(PFX + "unknown")));
            src.sendFeedback(Component.translatable(PFX + "exp",
                fmtNum(current.exp())));
            src.sendFeedback(Component.translatable(PFX + "tier",
                PetData.TIER_NAMES.get(current.tier())));
            src.sendFeedback(Component.translatable(PFX + "held_item",
                current.heldItem() != null ? current.heldItem() : t(PFX + "held_item_none"),
                PetExperienceTracker.resolvePetItemMultiplier(current.heldItem(), null)));
            src.sendFeedback(Component.translatable(PFX + "candy", current.candyUsed()));
        } else {
            src.sendFeedback(Component.translatable(PFX + "none"));
        }

        // ---- Exp Sharing ----
        src.sendFeedback(Component.empty());
        src.sendFeedback(Component.translatable(PFX + "shared_header"));

        List<PetData> sharedPets = pm.getSharedPets();
        if (sharedPets.isEmpty()) {
            src.sendFeedback(Component.translatable(PFX + "none"));
        } else {
            int slotNum = 1;
            for (PetData p : sharedPets) {
                SkillType primary = p.getPrimarySkill();
                boolean isExpShare = "PET_ITEM_EXP_SHARE".equals(p.heldItem());
                src.sendFeedback(Component.translatable(PFX + "shared_slot",
                    slotNum++, p.type(), p.getLevel(),
                    primary != null ? primary.name() : "?",
                    p.heldItem() != null ? p.heldItem() : t(PFX + "shared_no_item"),
                    isExpShare ? t(PFX + "shared_exp_share_tag") : ""));
            }
        }

        // ---- State ----
        src.sendFeedback(Component.empty());
        src.sendFeedback(Component.translatable(PFX + "state_header"));

        src.sendFeedback(Component.translatable(PFX + "state_taming",
            state.getTamingLevel(), state.getTamingMultiplier(), state.getTamingSharePercent()));
        src.sendFeedback(Component.translatable(PFX + "state_beastmaster",
            String.format("%.1f", (state.beastmasterMult - 1.0) * 100),
            String.format("%.3f", state.beastmasterMult)));
        src.sendFeedback(Component.translatable(PFX + "state_battle_exp",
            state.battleExperienceLevel > 0
                ? toRoman(state.battleExperienceLevel)
                : t(PFX + "state_battle_exp_none"),
            state.getBattleExperienceMultiplier()));
        src.sendFeedback(Component.translatable(PFX + "state_why_not_more",
            state.whyNotMoreLevel > 0
                ? toRoman(state.whyNotMoreLevel)
                : t(PFX + "state_why_not_more_none"),
            state.whyNotMoreLevel));
        src.sendFeedback(Component.translatable(PFX + "state_diana_mayor",
            (state.dianaMayor ? "§a" : "§c") + t(state.dianaMayor ? PFX + "yes" : PFX + "no")));
        src.sendFeedback(Component.translatable(PFX + "state_pet_xp_buff",
            (state.dianaPetXpBuff ? "§a" : "§c") + t(state.dianaPetXpBuff ? PFX + "yes" : PFX + "no")));
        src.sendFeedback(Component.translatable(PFX + "state_sharing_is_caring",
            (state.dianaSharingIsCaring ? "§a" : "§c") + t(state.dianaSharingIsCaring ? PFX + "yes" : PFX + "no")));

        // ---- Last XP ----
        PetExperienceTracker.LastCalculation last = PetExperienceTracker.getInstance().getLastCalculation();
        if (last != null) {
            src.sendFeedback(Component.empty());
            src.sendFeedback(Component.translatable(PFX + "last_xp_header"));
            src.sendFeedback(Component.translatable(PFX + "last_xp_summoned",
                last.skillName, last.skillXp, last.summonedXp));
            for (int i = 0; i < last.sharedResults.size(); i++) {
                var r = last.sharedResults.get(i);
                src.sendFeedback(Component.translatable(PFX + "last_xp_shared",
                    i + 1, r.petType(), r.xpGained()));
            }
        }
    }

    private static String fmtNum(double n) {
        return String.format("%,.2f", n);
    }

    private static String t(String key) {
        return Component.translatable(key).getString();
    }

    private static String toRoman(int num) {
        return switch (num) {
            case 1 -> "I";
            case 2 -> "II";
            case 3 -> "III";
            case 4 -> "IV";
            case 5 -> "V";
            case 6 -> "VI";
            case 7 -> "VII";
            case 8 -> "VIII";
            case 9 -> "IX";
            case 10 -> "X";
            default -> String.valueOf(num);
        };
    }
}
