package dev.kaden.oneforall.spiderman;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.Entity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class SpiderManAbilities {
    private static final Map<UUID, Long> COOLDOWNS = new ConcurrentHashMap<>();
    private SpiderManAbilities() {}

    public static void register() {
        ServerPlayNetworking.registerGlobalReceiver(SpiderManIds.WEB_SWING,
                (server, player, handler, buf, responseSender) -> server.execute(() -> swing(player)));
        ServerPlayNetworking.registerGlobalReceiver(SpiderManIds.WEB_ZIP,
                (server, player, handler, buf, responseSender) -> server.execute(() -> zip(player)));
        ServerPlayNetworking.registerGlobalReceiver(SpiderManIds.WEB_SHOT,
                (server, player, handler, buf, responseSender) -> server.execute(() -> webShot(player)));
    }

    private static boolean ready(ServerPlayerEntity player, long delay) {
        long now = player.getWorld().getTime();
        long last = COOLDOWNS.getOrDefault(player.getUuid(), -1000L);
        if (now - last < delay) return false;
        COOLDOWNS.put(player.getUuid(), now);
        return true;
    }

    private static HitResult raycast(ServerPlayerEntity player, double distance) {
        Vec3d start = player.getCameraPosVec(1.0F);
        Vec3d end = start.add(player.getRotationVec(1.0F).multiply(distance));
        return player.getWorld().raycast(new RaycastContext(start, end,
                RaycastContext.ShapeType.OUTLINE, RaycastContext.FluidHandling.NONE, player));
    }

    private static void swing(ServerPlayerEntity player) {
        if (!ready(player, 8)) return;
        HitResult hit = raycast(player, 38.0);
        if (!(hit instanceof BlockHitResult blockHit) || hit.getType() != HitResult.Type.BLOCK) {
            player.sendMessage(Text.literal("No solid web anchor in range."), true);
            return;
        }
        Vec3d anchor = blockHit.getPos();
        Vec3d delta = anchor.subtract(player.getPos());
        double distance = Math.max(1.0, delta.length());
        Vec3d pull = delta.normalize().multiply(Math.min(1.65, 0.48 + distance * 0.025));
        Vec3d velocity = player.getVelocity().multiply(0.72).add(pull).add(0, 0.20, 0);
        player.setVelocity(velocity);
        player.velocityModified = true;
        player.fallDistance = 0;
        player.getWorld().spawnParticles(ParticleTypes.CLOUD, player.getX(), player.getY() + 1,
                player.getZ(), 8, 0.25, 0.35, 0.25, 0.04);
        player.sendMessage(Text.literal("Web swing!"), true);
    }

    private static void zip(ServerPlayerEntity player) {
        if (!ready(player, 12)) return;
        HitResult hit = raycast(player, 32.0);
        if (hit.getType() == HitResult.Type.MISS) {
            player.sendMessage(Text.literal("No web-zip target in range."), true);
            return;
        }
        Vec3d target = hit.getPos();
        Vec3d direction = target.subtract(player.getPos()).normalize();
        player.setVelocity(direction.multiply(2.25).add(0, 0.35, 0));
        player.velocityModified = true;
        player.fallDistance = 0;
        player.getWorld().spawnParticles(ParticleTypes.CRIT, player.getX(), player.getY() + 1,
                player.getZ(), 12, 0.25, 0.3, 0.25, 0.12);
    }

    private static void webShot(ServerPlayerEntity player) {
        if (!ready(player, 5)) return;
        HitResult hit = raycast(player, 24.0);
        if (hit.getType() == HitResult.Type.MISS) return;
        if (hit instanceof EntityHitResult entityHit) {
            Entity target = entityHit.getEntity();
            if (target == player) return;
            target.addVelocity(0, 0.28, 0);
            target.velocityModified = true;
            player.getWorld().spawnParticles(ParticleTypes.SNOWFLAKE,
                    target.getX(), target.getBodyY(0.5), target.getZ(), 18, 0.35, 0.45, 0.35, 0.02);
            player.sendMessage(Text.literal("Web shot connected!"), true);
        } else {
            Vec3d point = hit.getPos();
            player.getWorld().spawnParticles(ParticleTypes.SNOWFLAKE, point.x, point.y, point.z,
                    16, 0.2, 0.2, 0.2, 0.02);
        }
    }
}
