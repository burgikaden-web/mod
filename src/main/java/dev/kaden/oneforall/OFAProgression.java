package dev.kaden.oneforall;

import dev.kaden.oneforall.data.OFAPlayerProgress;
import dev.kaden.oneforall.data.OFAWorldState;
import dev.kaden.oneforall.network.OFANetwork;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.LivingEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.UUID;

public final class OFAProgression {
    private static final UUID STRENGTH_ID = UUID.fromString("b1f6f16e-0ad5-43c1-9d11-8e4fb03a1001");
    private static final UUID SPEED_ID = UUID.fromString("b1f6f16e-0ad5-43c1-9d11-8e4fb03a1002");

    private OFAProgression() {}

    public static void tick(ServerPlayerEntity player, OFAPlayerProgress progress) {
        if (player.age % 10 == 0 && progress.regenerateEnergy()) {
            OFAWorldState.get(player.getServer()).markProgressDirty();
        }

        if (progress.isActive() && player.age % 35 == 0) {
            int percentage = progress.getPercentage();
            int amplifier = percentage >= 80 ? 3 : percentage >= 60 ? 2 : percentage >= 35 ? 1 : 0;
            StatusEffectInstance current = player.getStatusEffect(StatusEffects.JUMP_BOOST);
            if (current == null || current.getAmplifier() < amplifier) {
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.JUMP_BOOST, 50, amplifier, true, false, false));
            }
        }

        if (player.age % 10 == 0) {
            OFANetwork.sendSync(player, progress);
        }
    }

    public static void grantVestiges(ServerPlayerEntity player, float defeatedMaxHealth) {
        OFAPlayerProgress progress = OFAWorldState.get(player.getServer()).getPlayer(player.getUuid());
        int before = progress.getVestiges();
        int reward = defeatedMaxHealth >= 200.0F ? 6 : defeatedMaxHealth >= 80.0F ? 3 : 1;
        if (!progress.addVestiges(reward)) return;

        OFAWorldState.get(player.getServer()).markProgressDirty();
        refreshAttributes(player, progress);

        if (before < 5 && progress.getVestiges() >= 5) {
            player.sendMessage(Text.literal("Unlocked: Detroit Smash"), false);
        }
        if (before < 10 && progress.getVestiges() >= 10) {
            player.sendMessage(Text.literal("Unlocked: Blackwhip"), false);
        }
        if (before < 20 && progress.getVestiges() >= 20) {
            player.sendMessage(Text.literal("Unlocked: Danger Sense"), false);
        }

        player.sendMessage(Text.literal("Vestiges +" + (progress.getVestiges() - before) + "  |  One For All: " + progress.getPercentage() + "%"), true);
        OFANetwork.sendSync(player, progress);
    }

    public static void refreshAttributes(ServerPlayerEntity player, OFAPlayerProgress progress) {
        applyModifier(player.getAttributeInstance(EntityAttributes.GENERIC_ATTACK_DAMAGE), STRENGTH_ID,
                "One For All strength", progress.isActive() ? 0.5 + progress.getPercentage() * 0.075 : 0.0);
        applyModifier(player.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED), SPEED_ID,
                "One For All speed", progress.isActive() ? 0.005 + progress.getPercentage() * 0.00055 : 0.0);
    }

    private static void applyModifier(EntityAttributeInstance attribute, UUID id, String name, double value) {
        if (attribute == null) return;
        attribute.removeModifier(id);
        if (value > 0.0) {
            attribute.addPersistentModifier(new EntityAttributeModifier(id, name, value, EntityAttributeModifier.Operation.ADDITION));
        }
    }
}
