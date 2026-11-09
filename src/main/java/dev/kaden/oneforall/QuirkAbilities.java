package dev.kaden.oneforall;

import dev.kaden.oneforall.data.OFAPlayerProgress;
import dev.kaden.oneforall.data.OFAWorldState;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.particle.BlockStateParticleEffect;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.util.List;

public final class QuirkAbilities {
    private QuirkAbilities() {}

    public static void use(ServerPlayerEntity player, int slot) {
        OFAPlayerProgress progress = OFAWorldState.get(player.getServer()).getPlayer(player.getUuid());
        String quirk = progress.getQuirkId();
        if (!progress.isActive()) {
            player.sendMessage(Text.literal("Activate your quirk first (O)."), true);
            return;
        }
        switch (quirk) {
            case "one_for_all" -> useOneForAll(player, progress, slot);
            case "explosion" -> useExplosion(player, progress, slot);
            case "half_cold_half_hot" -> useHalfColdHalfHot(player, progress, slot);
            case "engine" -> useEngine(player, progress, slot);
            case "hardening" -> useHardening(player, progress, slot);
            default -> player.sendMessage(Text.literal("Unknown quirk. Use /quirk set to choose one."), true);
        }
    }

    private static boolean pay(ServerPlayerEntity player, OFAPlayerProgress progress, int cost) {
        if (!progress.spendEnergy(cost)) {
            player.sendMessage(Text.literal("Not enough energy. Current energy: " + progress.getEnergy()), true);
            return false;
        }
        OFAWorldState.get(player.getServer()).markProgressDirty();
        return true;
    }

    private static List<HostileEntity> targets(ServerPlayerEntity player, double range) {
        return player.getServerWorld().getEntitiesByClass(HostileEntity.class,
                player.getBoundingBox().expand(range),
                entity -> entity.isAlive() && entity.squaredDistanceTo(player) <= range * range);
    }

    private static void useExplosion(ServerPlayerEntity player, OFAPlayerProgress progress, int slot) {
        ServerWorld world = player.getServerWorld();
        Vec3d look = player.getRotationVec(1.0F).normalize();
        if (slot == 1) {
            if (!pay(player, progress, 18)) return;
            Vec3d blast = player.getPos().add(look.multiply(3.2)).add(0, 1.0, 0);
            world.spawnParticles(ParticleTypes.EXPLOSION, blast.x, blast.y, blast.z, 18, .55, .45, .55, .12);
            world.spawnParticles(ParticleTypes.FLAME, blast.x, blast.y, blast.z, 35, .65, .5, .65, .08);
            for (HostileEntity mob : targets(player, 6.5)) {
                Vec3d away = mob.getPos().subtract(player.getPos()).normalize();
                mob.damage(world.getDamageSources().playerAttack(player), 5.0F);
                mob.addVelocity(away.x * .9, .45, away.z * .9);
                mob.velocityModified = true;
            }
            world.playSound(null, player.getBlockPos(), SoundEvents.ENTITY_GENERIC_EXPLODE, SoundCategory.PLAYERS, 1F, 1.25F);
        } else if (slot == 2) {
            if (!pay(player, progress, 22)) return;
            player.addVelocity(look.x * 1.35, Math.max(.45, look.y * 1.1 + .35), look.z * 1.35);
            player.velocityModified = true;
            world.spawnParticles(ParticleTypes.FLAME, player.getX(), player.getY() + .7, player.getZ(), 28, .45, .35, .45, .08);
            world.playSound(null, player.getBlockPos(), SoundEvents.ENTITY_FIREWORK_ROCKET_LAUNCH, SoundCategory.PLAYERS, .8F, 1.4F);
        } else {
            if (!pay(player, progress, 30)) return;
            for (HostileEntity mob : targets(player, 9.0)) {
                if (mob.getPos().subtract(player.getPos()).normalize().dotProduct(look) > .35) {
                    mob.damage(world.getDamageSources().playerAttack(player), 8.0F);
                    mob.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, 70, 0));
                }
            }
            world.spawnParticles(ParticleTypes.FLAME, player.getX(), player.getY() + 1, player.getZ(), 65, 1.2, .6, 1.2, .12);
            world.playSound(null, player.getBlockPos(), SoundEvents.ENTITY_GENERIC_EXPLODE, SoundCategory.PLAYERS, 1.2F, .8F);
        }
    }

    private static void useHalfColdHalfHot(ServerPlayerEntity player, OFAPlayerProgress progress, int slot) {
        ServerWorld world = player.getServerWorld();
        Vec3d look = player.getRotationVec(1.0F).normalize();
        if (slot == 1) {
            if (!pay(player, progress, 20)) return;
            Vec3d end = player.raycast(18, 1, false).getPos();
            Box area = new Box(end.x - 3, end.y - 2, end.z - 3, end.x + 3, end.y + 2, end.z + 3);
            for (HostileEntity mob : world.getEntitiesByClass(HostileEntity.class, area, LivingEntity::isAlive)) {
                mob.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 100, 4));
                mob.addStatusEffect(new StatusEffectInstance(StatusEffects.MINING_FATIGUE, 100, 1));
            }
            world.spawnParticles(ParticleTypes.SNOWFLAKE, end.x, end.y, end.z, 85, 2.5, 1.2, 2.5, .04);
            world.spawnParticles(ParticleTypes.CLOUD, end.x, end.y, end.z, 35, 1.8, .5, 1.8, .03);
            world.playSound(null, player.getBlockPos(), SoundEvents.BLOCK_GLASS_BREAK, SoundCategory.PLAYERS, 1F, .65F);
        } else if (slot == 2) {
            if (!pay(player, progress, 20)) return;
            Vec3d origin = player.getCameraPosVec(1.0F);
            for (int i = 1; i <= 18; i++) {
                Vec3d p = origin.add(look.multiply(i));
                world.spawnParticles(ParticleTypes.FLAME, p.x, p.y, p.z, 5, .12, .12, .12, .02);
                world.spawnParticles(ParticleTypes.SMOKE, p.x, p.y, p.z, 2, .08, .08, .08, .01);
            }
            Vec3d end = origin.add(look.multiply(18));
            for (HostileEntity mob : world.getEntitiesByClass(HostileEntity.class,
                    new Box(end.x - 2, end.y - 2, end.z - 2, end.x + 2, end.y + 2, end.z + 2), LivingEntity::isAlive)) {
                mob.damage(world.getDamageSources().playerAttack(player), 6.0F);
                mob.setOnFireFor(4);
            }
            world.playSound(null, player.getBlockPos(), SoundEvents.ITEM_FIRECHARGE_USE, SoundCategory.PLAYERS, 1F, .9F);
        } else {
            if (!pay(player, progress, 32)) return;
            Vec3d end = player.raycast(15, 1, false).getPos();
            Box area = new Box(end.x - 4, end.y - 2, end.z - 4, end.x + 4, end.y + 2, end.z + 4);
            for (HostileEntity mob : world.getEntitiesByClass(HostileEntity.class, area, LivingEntity::isAlive)) {
                mob.damage(world.getDamageSources().playerAttack(player), 7.0F);
                mob.setOnFireFor(5);
                mob.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 60, 1));
            }
            world.spawnParticles(ParticleTypes.FLAME, end.x, end.y, end.z, 90, 2.5, 1.3, 2.5, .08);
            world.spawnParticles(ParticleTypes.SNOWFLAKE, end.x, end.y, end.z, 70, 2.2, 1.0, 2.2, .03);
            world.playSound(null, player.getBlockPos(), SoundEvents.ENTITY_GENERIC_EXPLODE, SoundCategory.PLAYERS, 1.1F, 1.15F);
        }
    }

    private static void useEngine(ServerPlayerEntity player, OFAPlayerProgress progress, int slot) {
        ServerWorld world = player.getServerWorld();
        Vec3d look = player.getRotationVec(1.0F).normalize();
        if (slot == 1) {
            if (!pay(player, progress, 16)) return;
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, 160, 2, true, false, true));
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.JUMP_BOOST, 100, 1, true, false, true));
            world.spawnParticles(ParticleTypes.CLOUD, player.getX(), player.getY() + .2, player.getZ(), 24, .3, .12, .3, .02);
        } else if (slot == 2) {
            if (!pay(player, progress, 20)) return;
            player.setVelocity(player.getVelocity().multiply(.15).add(look.multiply(1.65)).add(0, .18, 0));
            player.velocityModified = true;
            world.spawnParticles(ParticleTypes.CLOUD, player.getX(), player.getY() + .5, player.getZ(), 36, .45, .25, .45, .04);
            world.playSound(null, player.getBlockPos(), SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP, SoundCategory.PLAYERS, .8F, 1.8F);
        } else {
            if (!pay(player, progress, 28)) return;
            for (HostileEntity mob : targets(player, 7.0)) {
                if (mob.getPos().subtract(player.getPos()).normalize().dotProduct(look) > .2) {
                    mob.damage(world.getDamageSources().playerAttack(player), 9.0F);
                    mob.addVelocity(look.x * 1.0, .3, look.z * 1.0);
                    mob.velocityModified = true;
                }
            }
            world.spawnParticles(ParticleTypes.SWEEP_ATTACK, player.getX() + look.x * 2, player.getY() + 1, player.getZ() + look.z * 2, 8, .6, .5, .6, 0);
            world.playSound(null, player.getBlockPos(), SoundEvents.ENTITY_PLAYER_ATTACK_KNOCKBACK, SoundCategory.PLAYERS, 1F, 1.25F);
        }
    }

    private static void useHardening(ServerPlayerEntity player, OFAPlayerProgress progress, int slot) {
        ServerWorld world = player.getServerWorld();
        if (slot == 1) {
            if (!pay(player, progress, 22)) return;
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 240, 1, true, true, true));
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 240, 0, true, false, true));
            world.spawnParticles(ParticleTypes.BLOCK, player.getX(), player.getY() + .8, player.getZ(), 36, .45, .6, .45, .15,
                    net.minecraft.block.Blocks.REDSTONE_BLOCK.getDefaultState());
            world.playSound(null, player.getBlockPos(), SoundEvents.BLOCK_STONE_PLACE, SoundCategory.PLAYERS, 1F, .65F);
        } else if (slot == 2) {
            if (!pay(player, progress, 18)) return;
            Vec3d look = player.getRotationVec(1.0F).normalize();
            player.addVelocity(look.x * .75, .25, look.z * .75);
            player.velocityModified = true;
            for (HostileEntity mob : targets(player, 4.5)) {
                mob.damage(world.getDamageSources().playerAttack(player), 8.0F);
                mob.addVelocity(look.x * .9, .35, look.z * .9);
                mob.velocityModified = true;
            }
            world.spawnParticles(ParticleTypes.BLOCK, player.getX(), player.getY() + 1, player.getZ(), 45, .5, .5, .5, .12,
                    net.minecraft.block.Blocks.DEEPSLATE.getDefaultState());
        } else {
            if (!pay(player, progress, 30)) return;
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 100, 3, true, true, true));
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH, 100, 1, true, true, true));
            world.spawnParticles(ParticleTypes.BLOCK, player.getX(), player.getY() + .8, player.getZ(), 80, .7, .8, .7, .18,
                    net.minecraft.block.Blocks.DEEPSLATE.getDefaultState());
            world.playSound(null, player.getBlockPos(), SoundEvents.BLOCK_ANVIL_LAND, SoundCategory.PLAYERS, .8F, .8F);
        }
    }

    private static void useOneForAll(ServerPlayerEntity player, OFAPlayerProgress progress, int slot) {
        if (slot == 1) useDetroitPlaceholder(player, progress);
        else if (slot == 2) useBlackwhipPlaceholder(player, progress);
        else useDelawarePlaceholder(player, progress);
    }

    private static void useDetroitPlaceholder(ServerPlayerEntity player, OFAPlayerProgress progress) {
        OFAAbilities.useDetroitSmash(player);
    }
    private static void useBlackwhipPlaceholder(ServerPlayerEntity player, OFAPlayerProgress progress) {
        OFAAbilities.useBlackwhip(player);
    }
    private static void useDelawarePlaceholder(ServerPlayerEntity player, OFAPlayerProgress progress) {
        OFAAbilities.useDelawareSmash(player);
    }
}
