package dev.kaden.halfcoldhot;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.particle.ParticleTypes;

public class HalfColdHalfHotMod implements ModInitializer {
    public static final String MOD_ID = "halfcoldhot";
    public static final Identifier ICE_WALL = id("ice_wall");
    public static final Identifier ICE_SPIKES = id("ice_spikes");
    public static final Identifier ICE_SLIDE = id("ice_slide");
    public static final Identifier FIRE_BLAST = id("fire_blast");
    public static final Identifier JET_BURN = id("jet_burn");
    public static final Identifier FLASHFREEZE_HEATWAVE = id("flashfreeze_heatwave");

    private static Identifier id(String path) { return new Identifier(MOD_ID, path); }

    @Override
    public void onInitialize() {
        ServerPlayNetworking.registerGlobalReceiver(ICE_WALL, (server, player, handler, buf, sender) -> server.execute(() -> createIceWall(player)));
        ServerPlayNetworking.registerGlobalReceiver(ICE_SPIKES, (server, player, handler, buf, sender) -> server.execute(() -> createIceSpikes(player)));
        ServerPlayNetworking.registerGlobalReceiver(ICE_SLIDE, (server, player, handler, buf, sender) -> server.execute(() -> iceSlide(player)));
        ServerPlayNetworking.registerGlobalReceiver(FIRE_BLAST, (server, player, handler, buf, sender) -> server.execute(() -> fireBlast(player)));
        ServerPlayNetworking.registerGlobalReceiver(JET_BURN, (server, player, handler, buf, sender) -> server.execute(() -> jetBurn(player)));
        ServerPlayNetworking.registerGlobalReceiver(FLASHFREEZE_HEATWAVE, (server, player, handler, buf, sender) -> server.execute(() -> flashfreezeHeatwave(player)));
    }

    private static void createIceWall(ServerPlayerEntity player) {
        ServerWorld world = player.getServerWorld();
        Vec3d forward = player.getRotationVec(1.0F).multiply(2.5);
        Vec3d center = player.getPos().add(forward);
        Vec3d side = new Vec3d(-forward.z, 0, forward.x).normalize();
        for (int x = -3; x <= 3; x++) for (int y = 0; y < 4; y++) {
            BlockPos pos = BlockPos.ofFloored(center.add(side.multiply(x)).add(0, y, 0));
            if (world.getBlockState(pos).isAir()) world.setBlockState(pos, Blocks.PACKED_ICE.getDefaultState());
        }
        world.spawnParticles(ParticleTypes.SNOWFLAKE, center.x, center.y + 1, center.z, 45, 2.2, 1.2, 0.8, 0.03);
    }

    private static void createIceSpikes(ServerPlayerEntity player) {
        ServerWorld world = player.getServerWorld();
        Vec3d direction = player.getRotationVec(1.0F).normalize();
        Vec3d side = new Vec3d(-direction.z, 0, direction.x);
        Vec3d origin = player.getPos().add(direction.multiply(2.0));
        for (int row = 0; row < 5; row++) {
            Vec3d rowCenter = origin.add(direction.multiply(row * 1.15));
            int width = row < 2 ? 1 : 2;
            for (int x = -width; x <= width; x++) {
                int height = Math.max(1, 4 - row / 2 - Math.abs(x));
                for (int y = 0; y < height; y++) {
                    BlockPos pos = BlockPos.ofFloored(rowCenter.add(side.multiply(x)).add(0, y, 0));
                    if (world.getBlockState(pos).isAir()) world.setBlockState(pos, Blocks.BLUE_ICE.getDefaultState());
                }
            }
        }
        Box hitBox = player.getBoundingBox().stretch(direction.multiply(7)).expand(1.3);
        for (Entity entity : world.getOtherEntities(player, hitBox, e -> e instanceof LivingEntity)) {
            LivingEntity target = (LivingEntity) entity;
            if (target.squaredDistanceTo(player) > 2.0) {
                target.damage(world.getDamageSources().playerAttack(player), 5.0F);
                target.setFrozenTicks(100);
            }
        }
        world.spawnParticles(ParticleTypes.SNOWFLAKE, origin.x, origin.y, origin.z, 70, 2.5, 0.8, 2.5, 0.04);
    }

    private static void iceSlide(ServerPlayerEntity player) {
        Vec3d direction = player.getRotationVec(1.0F);
        Vec3d horizontal = new Vec3d(direction.x, 0, direction.z).normalize();
        player.addVelocity(horizontal.x * 1.35, 0.12, horizontal.z * 1.35);
        player.velocityModified = true;
        ServerWorld world = player.getServerWorld();
        world.spawnParticles(ParticleTypes.SNOWFLAKE, player.getX(), player.getY() + 0.2, player.getZ(), 35, 0.7, 0.15, 0.7, 0.03);
        BlockPos under = player.getBlockPos().down();
        if (world.getBlockState(under).isAir()) world.setBlockState(under, Blocks.ICE.getDefaultState());
    }

    private static void fireBlast(ServerPlayerEntity player) {
        ServerWorld world = player.getServerWorld();
        Vec3d direction = player.getRotationVec(1.0F).normalize();
        Vec3d start = player.getEyePos().add(direction);
        for (int i = 1; i <= 9; i++) {
            Vec3d point = start.add(direction.multiply(i * 0.65));
            world.spawnParticles(ParticleTypes.FLAME, point.x, point.y, point.z, 8, 0.22, 0.22, 0.22, 0.015);
        }
        Box box = player.getBoundingBox().stretch(direction.multiply(7)).expand(1.1);
        for (Entity entity : world.getOtherEntities(player, box, e -> e instanceof LivingEntity)) {
            Vec3d toTarget = entity.getPos().subtract(player.getPos()).normalize();
            if (direction.dotProduct(toTarget) > 0.55) {
                LivingEntity target = (LivingEntity) entity;
                target.damage(world.getDamageSources().playerAttack(player), 6.0F);
                target.setOnFireFor(4);
            }
        }
    }

    private static void jetBurn(ServerPlayerEntity player) {
        ServerWorld world = player.getServerWorld();
        Vec3d center = player.getPos().add(player.getRotationVec(1.0F).multiply(2.2));
        world.spawnParticles(ParticleTypes.FLAME, center.x, center.y + 1, center.z, 100, 1.5, 1.0, 1.5, 0.08);
        Box area = new Box(center.x - 2.5, center.y - 1, center.z - 2.5, center.x + 2.5, center.y + 2.5, center.z + 2.5);
        for (Entity entity : world.getOtherEntities(player, area, e -> e instanceof LivingEntity)) {
            LivingEntity target = (LivingEntity) entity;
            target.damage(world.getDamageSources().playerAttack(player), 7.0F);
            target.setOnFireFor(5);
        }
    }

    private static void flashfreezeHeatwave(ServerPlayerEntity player) {
        ServerWorld world = player.getServerWorld();
        Vec3d center = player.getPos().add(player.getRotationVec(1.0F).multiply(3.0));
        Box area = new Box(center.x - 5, center.y - 2, center.z - 5, center.x + 5, center.y + 3, center.z + 5);
        for (Entity entity : world.getOtherEntities(player, area, e -> e instanceof LivingEntity)) {
            LivingEntity target = (LivingEntity) entity;
            target.damage(world.getDamageSources().playerAttack(player), 9.0F);
            target.setFrozenTicks(140);
            target.setOnFireFor(3);
            Vec3d push = target.getPos().subtract(player.getPos()).normalize().multiply(0.65);
            target.addVelocity(push.x, 0.28, push.z);
            target.velocityModified = true;
        }
        world.spawnParticles(ParticleTypes.SNOWFLAKE, center.x, center.y + 1, center.z, 100, 4, 1.5, 4, 0.05);
        world.spawnParticles(ParticleTypes.FLAME, center.x, center.y + 1, center.z, 75, 3.5, 1.2, 3.5, 0.04);
        world.spawnParticles(ParticleTypes.CLOUD, center.x, center.y + 1, center.z, 45, 3, 1, 3, 0.02);
    }
}
