package dev.kaden.halfcoldhot;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class HalfColdHalfHotMod implements ModInitializer {
    public static final String MOD_ID = "halfcoldhot";
    public static final Identifier MODE = id("mode");
    public static final Identifier ICE_WALL = id("ice_wall");
    public static final Identifier ICE_LANCE = id("ice_lance");
    public static final Identifier ICE_SLIDE = id("ice_slide");
    public static final Identifier FIRE_BURST = id("fire_burst");
    public static final Identifier JET_BURN = id("jet_burn");
    public static final Identifier FLASHFREEZE = id("flashfreeze");
    public static final Identifier STEAM_NOVA = id("steam_nova");
    public static final Identifier ICE_ARMOR = id("ice_armor");

    private static final Map<UUID, FighterState> STATES = new HashMap<>();
    private static Identifier id(String path) { return new Identifier(MOD_ID, path); }

    private static final class FighterState {
        double heat = 100.0;
        double cold = 100.0;
        int mode = 2; // 0 = ice, 1 = fire, 2 = balanced
        int tick = 0;
        final Map<String, Integer> cooldowns = new HashMap<>();
    }

    @Override
    public void onInitialize() {
        ServerPlayNetworking.registerGlobalReceiver(MODE, (server, player, handler, buf, sender) -> server.execute(() -> cycleMode(player)));
        ServerPlayNetworking.registerGlobalReceiver(ICE_WALL, (server, player, handler, buf, sender) -> server.execute(() -> iceWall(player)));
        ServerPlayNetworking.registerGlobalReceiver(ICE_LANCE, (server, player, handler, buf, sender) -> server.execute(() -> iceLance(player)));
        ServerPlayNetworking.registerGlobalReceiver(ICE_SLIDE, (server, player, handler, buf, sender) -> server.execute(() -> iceSlide(player)));
        ServerPlayNetworking.registerGlobalReceiver(FIRE_BURST, (server, player, handler, buf, sender) -> server.execute(() -> fireBurst(player)));
        ServerPlayNetworking.registerGlobalReceiver(JET_BURN, (server, player, handler, buf, sender) -> server.execute(() -> jetBurn(player)));
        ServerPlayNetworking.registerGlobalReceiver(FLASHFREEZE, (server, player, handler, buf, sender) -> server.execute(() -> flashfreeze(player)));
        ServerPlayNetworking.registerGlobalReceiver(STEAM_NOVA, (server, player, handler, buf, sender) -> server.execute(() -> steamNova(player)));
        ServerPlayNetworking.registerGlobalReceiver(ICE_ARMOR, (server, player, handler, buf, sender) -> server.execute(() -> iceArmor(player)));
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) tickPlayer(player);
        });
    }

    private static FighterState state(ServerPlayerEntity player) {
        return STATES.computeIfAbsent(player.getUuid(), uuid -> new FighterState());
    }

    private static void tickPlayer(ServerPlayerEntity player) {
        FighterState s = state(player);
        s.tick++;
        s.heat = Math.min(100.0, s.heat + (s.mode == 1 ? 1.25 : 0.85));
        s.cold = Math.min(100.0, s.cold + (s.mode == 0 ? 1.25 : 0.85));
        s.cooldowns.replaceAll((key, value) -> Math.max(0, value - 1));
        s.cooldowns.values().removeIf(value -> value <= 0);
        if (s.tick % 40 == 0) {
            String mode = s.mode == 0 ? "ICE" : s.mode == 1 ? "FIRE" : "BALANCED";
            String balance = Math.abs(s.heat - s.cold) <= 15 ? "  §bBALANCED BONUS" : "";
            player.sendMessage(Text.literal("§f[§bHalf-Cold §cHalf-Hot§f] §7" + mode +
                    "  §bCold " + (int)s.cold + "%  §cHeat " + (int)s.heat + "%" + balance), true);
        }
    }

    private static boolean cast(ServerPlayerEntity p, String key, double heat, double cold, int cooldown) {
        FighterState s = state(p);
        if (s.cooldowns.containsKey(key)) {
            p.sendMessage(Text.literal("§7Ability recharging: " + (s.cooldowns.get(key) / 20.0) + "s"), true);
            return false;
        }
        if (s.heat < heat || s.cold < cold) {
            p.sendMessage(Text.literal("§cNot enough temperature energy. Switch modes or wait to recover."), true);
            return false;
        }
        s.heat -= heat;
        s.cold -= cold;
        s.cooldowns.put(key, cooldown);
        return true;
    }

    private static void cycleMode(ServerPlayerEntity p) {
        FighterState s = state(p);
        s.mode = (s.mode + 1) % 3;
        String name = s.mode == 0 ? "ICE FOCUS" : s.mode == 1 ? "FIRE FOCUS" : "TEMPERATURE BALANCE";
        p.sendMessage(Text.literal("§fQuirk stance: §b" + name), true);
    }

    private static boolean iceAllowed(ServerPlayerEntity p) {
        return state(p).mode != 1;
    }
    private static boolean fireAllowed(ServerPlayerEntity p) {
        return state(p).mode != 0;
    }

    private static void iceWall(ServerPlayerEntity p) {
        if (!iceAllowed(p) || !cast(p, "ice_wall", 0, 28, 100)) return;
        ServerWorld w = p.getServerWorld();
        Vec3d dir = flatLook(p);
        Vec3d center = p.getPos().add(dir.multiply(3));
        Vec3d side = new Vec3d(-dir.z, 0, dir.x);
        for (int x=-3; x<=3; x++) for (int y=0; y<4; y++) {
            BlockPos pos = BlockPos.ofFloored(center.add(side.multiply(x)).add(0,y,0));
            if (w.getBlockState(pos).isAir() && w.getBlockState(pos.up()).isAir())
                w.setBlockState(pos, Blocks.PACKED_ICE.getDefaultState());
        }
        w.spawnParticles(ParticleTypes.SNOWFLAKE, center.x, center.y+1, center.z, 65, 3, 1.2, 0.6, 0.025);
    }

    private static void iceLance(ServerPlayerEntity p) {
        if (!iceAllowed(p) || !cast(p, "ice_lance", 0, 22, 55)) return;
        ServerWorld w = p.getServerWorld();
        Vec3d dir = flatLook(p);
        Vec3d start = p.getPos().add(dir.multiply(1.5));
        for (int i=0; i<7; i++) {
            Vec3d point = start.add(dir.multiply(i*1.15));
            BlockPos base = BlockPos.ofFloored(point);
            int height = 3 + (i % 3);
            for (int y=0; y<height; y++) {
                BlockPos pos = base.up(y);
                if (w.getBlockState(pos).isAir()) w.setBlockState(pos, Blocks.BLUE_ICE.getDefaultState());
            }
            w.spawnParticles(ParticleTypes.SNOWFLAKE, point.x, point.y+1, point.z, 12, .3, .5, .3, .02);
        }
        Box hit = p.getBoundingBox().stretch(dir.multiply(9)).expand(1.0);
        for (Entity e : w.getOtherEntities(p, hit, entity -> entity instanceof LivingEntity)) {
            LivingEntity target = (LivingEntity)e;
            if (inFront(p, target, dir, 0.25)) {
                target.damage(w.getDamageSources().playerAttack(p), 7.0F);
                target.setFrozenTicks(100);
                target.takeKnockback(.55, -dir.x, -dir.z);
            }
        }
    }

    private static void iceSlide(ServerPlayerEntity p) {
        if (!iceAllowed(p) || !cast(p, "ice_slide", 0, 12, 35)) return;
        Vec3d dir = flatLook(p);
        p.addVelocity(dir.x*1.65, .18, dir.z*1.65);
        p.velocityModified = true;
        ServerWorld w = p.getServerWorld();
        w.spawnParticles(ParticleTypes.SNOWFLAKE, p.getX(), p.getY()+.15, p.getZ(), 45, .65, .12, .65, .03);
    }

    private static void fireBurst(ServerPlayerEntity p) {
        if (!fireAllowed(p) || !cast(p, "fire_burst", 24, 0, 55)) return;
        ServerWorld w = p.getServerWorld();
        Vec3d dir = p.getRotationVec(1).normalize();
        Vec3d origin = p.getEyePos().add(dir);
        for (int i=1; i<=10; i++) {
            Vec3d point = origin.add(dir.multiply(i*.7));
            w.spawnParticles(ParticleTypes.FLAME, point.x, point.y, point.z, 9, .22, .22, .22, .015);
        }
        Box hit = p.getBoundingBox().stretch(dir.multiply(8)).expand(1.2);
        for (Entity e : w.getOtherEntities(p, hit, entity -> entity instanceof LivingEntity)) {
            LivingEntity target = (LivingEntity)e;
            if (inFront(p, target, dir, .35)) {
                target.damage(w.getDamageSources().playerAttack(p), 8.0F);
                target.setOnFireFor(4);
                target.takeKnockback(.7, -dir.x, -dir.z);
            }
        }
    }

    private static void jetBurn(ServerPlayerEntity p) {
        if (!fireAllowed(p) || !cast(p, "jet_burn", 32, 0, 90)) return;
        Vec3d dir = flatLook(p);
        p.addVelocity(dir.x*1.2, .78, dir.z*1.2);
        p.velocityModified = true;
        ServerWorld w = p.getServerWorld();
        w.spawnParticles(ParticleTypes.FLAME, p.getX(), p.getY()+.5, p.getZ(), 65, .7, .5, .7, .06);
        Box area = p.getBoundingBox().expand(2.3);
        for (Entity e : w.getOtherEntities(p, area, entity -> entity instanceof LivingEntity)) {
            LivingEntity target = (LivingEntity)e;
            target.damage(w.getDamageSources().playerAttack(p), 5.0F);
            target.setOnFireFor(3);
        }
    }

    private static void flashfreeze(ServerPlayerEntity p) {
        FighterState s = state(p);
        if (s.mode != 2 || !cast(p, "flashfreeze", 35, 35, 240)) {
            if (s.mode != 2) p.sendMessage(Text.literal("§bFlashfreeze Heatwave requires Temperature Balance stance."), true);
            return;
        }
        ServerWorld w = p.getServerWorld();
        Vec3d dir = flatLook(p);
        Vec3d center = p.getPos().add(dir.multiply(3.5));
        Box area = new Box(center.x-5, center.y-2, center.z-5, center.x+5, center.y+3, center.z+5);
        for (Entity e : w.getOtherEntities(p, area, entity -> entity instanceof LivingEntity)) {
            LivingEntity target = (LivingEntity)e;
            target.damage(w.getDamageSources().playerAttack(p), 12.0F);
            target.setFrozenTicks(160);
            target.setOnFireFor(3);
            target.takeKnockback(.9, p.getX()-target.getX(), p.getZ()-target.getZ());
        }
        w.spawnParticles(ParticleTypes.SNOWFLAKE, center.x, center.y+1, center.z, 140, 4, 1.5, 4, .04);
        w.spawnParticles(ParticleTypes.FLAME, center.x, center.y+1, center.z, 110, 4, 1.2, 4, .04);
        w.spawnParticles(ParticleTypes.CLOUD, center.x, center.y+1, center.z, 75, 3, 1, 3, .025);
    }

    private static void steamNova(ServerPlayerEntity p) {
        FighterState s = state(p);
        if (s.mode != 2 || !cast(p, "steam_nova", 20, 20, 140)) {
            if (s.mode != 2) p.sendMessage(Text.literal("§bSteam Nova requires Temperature Balance stance."), true);
            return;
        }
        ServerWorld w = p.getServerWorld();
        Box area = p.getBoundingBox().expand(5);
        for (Entity e : w.getOtherEntities(p, area, entity -> entity instanceof LivingEntity)) {
            LivingEntity target = (LivingEntity)e;
            target.damage(w.getDamageSources().playerAttack(p), 9.0F);
            target.takeKnockback(1.15, p.getX()-target.getX(), p.getZ()-target.getZ());
        }
        w.spawnParticles(ParticleTypes.CLOUD, p.getX(), p.getY()+1, p.getZ(), 180, 3.5, 1.8, 3.5, .06);
        w.spawnParticles(ParticleTypes.FLAME, p.getX(), p.getY()+1, p.getZ(), 45, 2.5, 1, 2.5, .025);
    }

    private static void iceArmor(ServerPlayerEntity p) {
        if (!iceAllowed(p) || !cast(p, "ice_armor", 0, 30, 300)) return;
        ServerWorld w = p.getServerWorld();
        p.setAbsorptionAmount(Math.min(8.0F, p.getAbsorptionAmount()+6.0F));
        w.spawnParticles(ParticleTypes.SNOWFLAKE, p.getX(), p.getY()+1, p.getZ(), 90, .8, 1, .8, .04);
        p.sendMessage(Text.literal("§bIce Armor: temporary frost shield granted."), true);
    }

    private static Vec3d flatLook(ServerPlayerEntity p) {
        Vec3d look = p.getRotationVec(1);
        Vec3d flat = new Vec3d(look.x, 0, look.z);
        return flat.lengthSquared() < .001 ? new Vec3d(0,0,1) : flat.normalize();
    }

    private static boolean inFront(ServerPlayerEntity p, LivingEntity target, Vec3d dir, double threshold) {
        Vec3d toward = target.getPos().subtract(p.getPos()).normalize();
        return dir.dotProduct(toward) > threshold;
    }
}
