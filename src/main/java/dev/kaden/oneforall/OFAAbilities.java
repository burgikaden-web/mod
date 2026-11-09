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
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class OFAAbilities {
    private static final Map<UUID, Map<String, Long>> COOLDOWNS = new HashMap<>();

    private OFAAbilities() {}

    private static boolean startCooldown(ServerPlayerEntity player, String ability, long ticks) {
        long now = player.getServerWorld().getTime();
        Map<String, Long> playerCooldowns = COOLDOWNS.computeIfAbsent(player.getUuid(), ignored -> new HashMap<>());
        long readyAt = playerCooldowns.getOrDefault(ability, 0L);
        if (now < readyAt) {
            player.sendMessage(Text.literal(ability + " is recharging (" + ((readyAt - now + 19) / 20) + "s)."), true);
            return false;
        }
        playerCooldowns.put(ability, now + ticks);
        return true;
    }

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
        if (!startCooldown(player, "Blackwhip", 40)) return;
        if (!progress.spendEnergy(20)) {
            player.sendMessage(Text.literal("Not enough energy. Wait for it to recharge."), true);
            return;
        }

        ServerWorld world = player.getServerWorld();
        Vec3d look = player.getRotationVec(1.0F).normalize();
        Vec3d up = new Vec3d(0.0, 1.0, 0.0);
        Vec3d side = look.crossProduct(up);
        if (side.lengthSquared() < 0.0001) side = new Vec3d(1.0, 0.0, 0.0);
        else side = side.normalize();

        Vec3d shoulderLine = player.getPos().add(0.0, 1.25, 0.0);
        Vec3d leftWrist = shoulderLine.add(side.multiply(-0.34)).add(look.multiply(0.18));
        Vec3d rightWrist = shoulderLine.add(side.multiply(0.34)).add(look.multiply(0.18));

        // Prioritize enemies in front of the player, rather than magically pulling every nearby mob.
        List<HostileEntity> targets = world.getEntitiesByClass(HostileEntity.class,
                player.getBoundingBox().expand(20.0),
                mob -> mob.isAlive()
                        && mob.squaredDistanceTo(player) <= 20.0 * 20.0
                        && mob.getPos().subtract(player.getPos()).normalize().dotProduct(look) > 0.35);
        targets.sort(java.util.Comparator.comparingDouble(mob -> mob.squaredDistanceTo(player)));

        int targetCount = Math.min(2, targets.size());
        for (int i = 0; i < targetCount; i++) {
            HostileEntity mob = targets.get(i);
            Vec3d wrist = (i == 0 ? rightWrist : leftWrist);
            Vec3d torso = mob.getPos().add(0.0, mob.getHeight() * 0.58, 0.0);
            spawnBlackwhipStrand(world, wrist, torso, i * 17 + 3);

            mob.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 55, 1, true, true, true));
            Vec3d pull = player.getPos().subtract(mob.getPos()).normalize();
            mob.addVelocity(pull.x * 0.32, 0.12, pull.z * 0.32);
            mob.velocityModified = true;
        }

        HitResult hit = player.raycast(24.0, 1.0F, false);
        if (targetCount == 0 && hit.getType() == HitResult.Type.BLOCK
                && hit instanceof BlockHitResult blockHit
                && !blockHit.getBlockPos().equals(player.getBlockPos())) {
            Vec3d anchor = hit.getPos();
            spawnBlackwhipStrand(world, rightWrist, anchor, 31);
            spawnBlackwhipStrand(world, leftWrist, anchor, 47);
            Vec3d pull = anchor.subtract(player.getPos()).normalize().multiply(1.15).add(0.0, 0.38, 0.0);
            player.setVelocity(player.getVelocity().multiply(0.25).add(pull));
            player.velocityModified = true;
        } else if (targetCount == 0) {
            Vec3d reach = player.getPos().add(look.multiply(17.0)).add(0.0, 1.0, 0.0);
            spawnBlackwhipStrand(world, rightWrist, reach, 59);
            spawnBlackwhipStrand(world, leftWrist, reach.add(side.multiply(-0.8)), 71);
        }

        world.playSound(null, player.getBlockPos(), SoundEvents.BLOCK_CHAIN_HIT,
                SoundCategory.PLAYERS, 0.85F, 0.55F);
        OFAWorldState.get(player.getServer()).markProgressDirty();
        OFANetwork.sendSync(player, progress);
    }

    /**
     * Builds a curved, layered tendril from the player's wrist.
     * The dark core, violet energy, and sparse bright flecks create a much
     * closer Blackwhip silhouette than a single straight line of particles.
     */
    private static void spawnBlackwhipStrand(ServerWorld world, Vec3d from, Vec3d to, int seed) {
        Vec3d delta = to.subtract(from);
        Vec3d side = delta.crossProduct(new Vec3d(0.0, 1.0, 0.0));
        if (side.lengthSquared() < 0.0001) side = new Vec3d(1.0, 0.0, 0.0);
        else side = side.normalize();

        double distance = delta.length();
        double bend = Math.min(2.8, Math.max(0.45, distance * 0.16));
        double sway = ((seed % 2 == 0) ? 1.0 : -1.0) * bend;
        Vec3d control = from.add(to).multiply(0.5)
                .add(side.multiply(sway))
                .add(0.0, Math.min(1.4, distance * 0.06), 0.0);

        int steps = Math.max(24, Math.min(56, (int) (distance * 3.0)));
        for (int i = 0; i <= steps; i++) {
            double t = (double) i / steps;
            double oneMinus = 1.0 - t;
            Vec3d point = from.multiply(oneMinus * oneMinus)
                    .add(control.multiply(2.0 * oneMinus * t))
                    .add(to.multiply(t * t));

            // Three offset layers give the tendril a broad, irregular silhouette.
            double ripple = Math.sin(t * Math.PI * 7.0 + seed) * 0.055;
            Vec3d offset = side.multiply(ripple);
            Vec3d core = point.add(offset);
            world.spawnParticles(ParticleTypes.SQUID_INK,
                    core.x, core.y, core.z, 2, 0.035, 0.035, 0.035, 0.0);

            if (i % 2 == 0) {
                Vec3d violet = core.add(side.multiply(0.075));
                world.spawnParticles(ParticleTypes.PORTAL,
                        violet.x, violet.y, violet.z, 1, 0.018, 0.018, 0.018, 0.0);
                world.spawnParticles(ParticleTypes.WITCH,
                        core.x, core.y, core.z, 1, 0.025, 0.025, 0.025, 0.0);
            }
            if (i % 5 == 0) {
                Vec3d glint = core.add(side.multiply(-0.045));
                world.spawnParticles(ParticleTypes.END_ROD,
                        glint.x, glint.y, glint.z, 1, 0.008, 0.008, 0.008, 0.0);
            }
        }
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
        if (!startCooldown(player, "Detroit Smash", 60)) return;
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


    public static void useDelawareSmash(ServerPlayerEntity player) {
        OFAPlayerProgress progress = OFAWorldState.get(player.getServer()).getPlayer(player.getUuid());
        if (!progress.isActive()) {
            player.sendMessage(Text.literal("Activate One For All first."), true);
            return;
        }
        if (!progress.hasDelawareSmash()) {
            player.sendMessage(Text.literal("Delaware Smash unlocks at 2 vestiges."), true);
            return;
        }
        if (!startCooldown(player, "Delaware Smash", 25)) return;
        if (!progress.spendEnergy(15)) {
            player.sendMessage(Text.literal("Not enough energy. Wait for it to recharge."), true);
            return;
        }

        ServerWorld world = player.getServerWorld();
        Vec3d start = player.getCameraPosVec(1.0F);
        HitResult hit = player.raycast(22.0, 1.0F, false);
        Vec3d end = hit.getPos();
        Box impactArea = new Box(end.x - 2.0, end.y - 2.0, end.z - 2.0,
                end.x + 2.0, end.y + 2.0, end.z + 2.0);
        float damage = (float) (4.0 + progress.getPercentage() * 0.05);

        for (HostileEntity mob : world.getEntitiesByClass(HostileEntity.class, impactArea,
                entity -> entity.isAlive() && entity.squaredDistanceTo(player) <= 22.0 * 22.0)) {
            mob.damage(world.getDamageSources().playerAttack(player), damage);
            mob.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 30, 0, true, true, true));
        }

        Vec3d difference = end.subtract(start);
        int points = Math.max(8, Math.min(36, (int) (difference.length() * 2.0)));
        for (int i = 0; i <= points; i++) {
            Vec3d point = start.add(difference.multiply((double) i / points));
            world.spawnParticles(ParticleTypes.CRIT, point.x, point.y, point.z, 2, 0.035, 0.035, 0.035, 0.01);
            world.spawnParticles(ParticleTypes.CLOUD, point.x, point.y, point.z, 1, 0.02, 0.02, 0.02, 0.005);
        }
        world.playSound(null, player.getBlockPos(), SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP,
                SoundCategory.PLAYERS, 1.0F, 0.7F);
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
