package dev.kaden.oneforall;

import dev.kaden.oneforall.data.OFAPlayerProgress;
import dev.kaden.oneforall.data.OFAWorldState;
import dev.kaden.oneforall.network.OFANetwork;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.util.List;

public final class OFAAbilities {
    private OFAAbilities() {}

    public static void useBlackwhip(ServerPlayerEntity player) {
        OFAPlayerProgress progress = OFAWorldState.get(player.getServer()).getPlayer(player.getUuid());
        if (!progress.isActive()) {
            player.sendMessage(Text.literal("Activate One For All first."), true);
            return;
        }
        if (!progress.hasBlackwhip()) {
            player.sendMessage(Text.literal("Blackwhip unlocks at 10 vestiges."), true);
            return;
        }
        if (!progress.spendEnergy(20)) {
            player.sendMessage(Text.literal("Not enough energy. Wait for it to recharge."), true);
            return;
        }

        ServerWorld world = player.getServerWorld();
        HitResult hit = player.raycast(24.0, 1.0F, false);
        Vec3d start = player.getCameraPosVec(1.0F);
        Vec3d end = hit.getPos();

        if (hit.getType() == HitResult.Type.BLOCK && hit instanceof BlockHitResult blockHit && !blockHit.getBlockPos().equals(player.getBlockPos())) {
            Vec3d pull = end.subtract(player.getPos()).normalize().multiply(1.25).add(0.0, 0.42, 0.0);
            player.setVelocity(player.getVelocity().multiply(0.25).add(pull));
            player.velocityModified = true;
        }

        Box area = player.getBoundingBox().expand(8.0);
        List<HostileEntity> nearby = world.getEntitiesByClass(HostileEntity.class, area,
                mob -> mob.isAlive() && mob.squaredDistanceTo(player) <= 64.0);
        for (HostileEntity mob : nearby) {
            mob.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 55, 1, true, true, true));
            Vec3d towardPlayer = player.getPos().subtract(mob.getPos()).normalize();
            mob.addVelocity(towardPlayer.x * 0.42, 0.16, towardPlayer.z * 0.42);
            mob.velocityModified = true;
            spawnTendril(world, mob.getCameraPosVec(1.0F), start);
        }

        if (nearby.isEmpty()) spawnTendril(world, start, end);
        world.playSound(null, player.getBlockPos(), SoundEvents.BLOCK_CHAIN_HIT, SoundCategory.PLAYERS, 0.9F, 0.8F);
        OFAWorldState.get(player.getServer()).markProgressDirty();
        OFANetwork.sendSync(player, progress);
    }

    public static void useDetroitSmash(ServerPlayerEntity player) {
        OFAPlayerProgress progress = OFAWorldState.get(player.getServer()).getPlayer(player.getUuid());
        if (!progress.isActive()) {
            player.sendMessage(Text.literal("Activate One For All first."), true);
            return;
        }
        if (!progress.hasDetroitSmash()) {
            player.sendMessage(Text.literal("Detroit Smash unlocks at 5 vestiges."), true);
            return;
        }
        if (!progress.spendEnergy(40)) {
            player.sendMessage(Text.literal("Not enough energy. Wait for it to recharge."), true);
            return;
        }

        ServerWorld world = player.getServerWorld();
        int percentage = progress.getPercentage();
        double radius = 6.5;
        double damage = 5.0 + percentage * 0.10;
        Box area = player.getBoundingBox().expand(radius);
        List<HostileEntity> targets = world.getEntitiesByClass(HostileEntity.class, area,
                mob -> mob.isAlive() && mob.squaredDistanceTo(player) <= radius * radius);

        for (HostileEntity mob : targets) {
            Vec3d outward = mob.getPos().subtract(player.getPos()).normalize();
            mob.damage(world.getDamageSources().playerAttack(player), (float) damage);
            mob.addVelocity(outward.x * 1.05, 0.55, outward.z * 1.05);
            mob.velocityModified = true;
        }

        // A particle shockwave only: it does not create a vanilla explosion or damage blocks.
        double px = player.getX();
        double py = player.getY() + 0.15;
        double pz = player.getZ();
        for (int i = 0; i < 48; i++) {
            double angle = (Math.PI * 2.0 * i) / 48.0;
            double x = px + Math.cos(angle) * 3.0;
            double z = pz + Math.sin(angle) * 3.0;
            world.spawnParticles(ParticleTypes.CLOUD, x, py, z, 2, 0.12, 0.03, 0.12, 0.01);
            world.spawnParticles(ParticleTypes.CRIT, x, py + 0.2, z, 1, 0.04, 0.04, 0.04, 0.01);
        }
        world.spawnParticles(ParticleTypes.EXPLOSION_EMITTER, px, py, pz, 1, 0.0, 0.0, 0.0, 0.0);
        world.playSound(null, player.getBlockPos(), SoundEvents.ENTITY_GENERIC_EXPLODE, SoundCategory.PLAYERS, 1.3F, 0.75F);
        OFAWorldState.get(player.getServer()).markProgressDirty();
        OFANetwork.sendSync(player, progress);
    }

    private static void spawnTendril(ServerWorld world, Vec3d from, Vec3d to) {
        Vec3d difference = to.subtract(from);
        double length = difference.length();
        int points = Math.max(8, Math.min(32, (int) (length * 2.0)));
        for (int i = 0; i <= points; i++) {
            Vec3d point = from.add(difference.multiply((double) i / points));
            world.spawnParticles(ParticleTypes.WITCH, point.x, point.y, point.z, 1, 0.025, 0.025, 0.025, 0.0);
        }
    }
}
