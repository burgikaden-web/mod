package dev.kaden.oneforall.spiderman;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class SpiderManSystems {
    public enum Suit {
        CLASSIC("Classic", 0), STEALTH("Stealth", 1), IRON("Iron Spider", 2),
        SCARLET("Scarlet", 3), FUTURE("Future Foundation", 4);
        public final String label;
        public final int id;
        Suit(String label, int id) { this.label = label; this.id = id; }
        public Suit next() { return values()[(id + 1) % values().length]; }
    }

    private static final Map<UUID, Suit> SUITS = new ConcurrentHashMap<>();
    private static final Map<UUID, Boolean> CRAWLING = new ConcurrentHashMap<>();
    private static final Map<UUID, Vec3d> ANCHORS = new ConcurrentHashMap<>();
    private static final Map<UUID, Double> ROPE_LENGTHS = new ConcurrentHashMap<>();

    private SpiderManSystems() {}

    public static void register() {
        ServerPlayNetworking.registerGlobalReceiver(SpiderManIds.TOGGLE_CRAWL,
            (server, player, handler, buf, sender) -> server.execute(() -> {
                boolean enabled = !CRAWLING.getOrDefault(player.getUuid(), false);
                CRAWLING.put(player.getUuid(), enabled);
                player.sendMessage(Text.literal("Wall crawl: " + (enabled ? "ON" : "OFF")), true);
                if (!enabled) player.setNoGravity(false);
            }));
        ServerPlayNetworking.registerGlobalReceiver(SpiderManIds.NEXT_SUIT,
            (server, player, handler, buf, sender) -> server.execute(() -> {
                Suit next = SUITS.getOrDefault(player.getUuid(), Suit.CLASSIC).next();
                SUITS.put(player.getUuid(), next);
                player.sendMessage(Text.literal("Suit selected: " + next.label), true);
                applySuit(player, next);
            }));
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) tick(player);
        });
    }

    public static void setSwingAnchor(ServerPlayerEntity player, Vec3d anchor) {
        double length = Math.max(4.0, Math.min(32.0, anchor.distanceTo(player.getPos())));
        ANCHORS.put(player.getUuid(), anchor);
        ROPE_LENGTHS.put(player.getUuid(), length);
        player.sendMessage(Text.literal("Web attached"), true);
    }

    public static void release(ServerPlayerEntity player) {
        ANCHORS.remove(player.getUuid());
        ROPE_LENGTHS.remove(player.getUuid());
    }

    private static void tick(ServerPlayerEntity player) {
        UUID id = player.getUuid();
        Suit suit = SUITS.getOrDefault(id, Suit.CLASSIC);
        if (player.age % 35 == 0) applySuit(player, suit);

        Vec3d anchor = ANCHORS.get(id);
        if (anchor != null) {
            Vec3d fromAnchor = player.getPos().subtract(anchor);
            double distance = fromAnchor.length();
            double rope = ROPE_LENGTHS.getOrDefault(id, 12.0);
            if (distance > rope) {
                Vec3d radial = fromAnchor.normalize();
                Vec3d velocity = player.getVelocity();
                double outward = velocity.dotProduct(radial);
                if (outward > 0) velocity = velocity.subtract(radial.multiply(outward * 1.08));
                Vec3d tangent = velocity.add(0, 0.035, 0);
                player.setVelocity(tangent);
                player.velocityModified = true;
            }
            if (player.age % 3 == 0)
                player.getServerWorld().spawnParticles(ParticleTypes.CRIT, anchor.x, anchor.y, anchor.z, 1, 0.03, 0.03, 0.03, 0);
        }

        boolean crawl = CRAWLING.getOrDefault(id, false);
        if (crawl && player.horizontalCollision && !player.isFallFlying()) {
            player.setNoGravity(true);
            Vec3d v = player.getVelocity();
            double climb = player.isSneaking() ? -0.12 : 0.12;
            player.setVelocity(v.x * 0.55, climb, v.z * 0.55);
            player.velocityModified = true;
            player.fallDistance = 0;
        } else if (player.hasNoGravity() && crawl) {
            player.setNoGravity(false);
        }
    }

    private static void applySuit(ServerPlayerEntity player, Suit suit) {
        switch (suit) {
            case CLASSIC -> {
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, 45, 0, true, false, false));
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.JUMP_BOOST, 45, 0, true, false, false));
            }
            case STEALTH -> player.addStatusEffect(new StatusEffectInstance(StatusEffects.INVISIBILITY, 45, 0, true, false, false));
            case IRON -> player.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 45, 0, true, false, false));
            case SCARLET -> {
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, 45, 1, true, false, false));
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH, 45, 0, true, false, false));
            }
            case FUTURE -> {
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, 45, 0, true, false, false));
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.NIGHT_VISION, 45, 0, true, false, false));
            }
        }
    }
}