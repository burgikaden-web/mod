package dev.kaden.oneforall;

import dev.kaden.oneforall.data.OFAPlayerProgress;
import dev.kaden.oneforall.data.OFAWorldState;
import dev.kaden.oneforall.network.OFANetwork;
import dev.kaden.oneforall.spiderman.SpiderManAbilities;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class OneForAllMod implements ModInitializer {
    public static final String MOD_ID = "oneforall";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        OFANetwork.registerServerReceivers();
        SpiderManAbilities.register();

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayerEntity player = handler.player;
            OFAPlayerProgress progress = OFAWorldState.get(server).getPlayer(player.getUuid());
            OFAProgression.refreshAttributes(player, progress);
            OFANetwork.sendSync(player, progress);
            player.sendMessage(Text.literal("One For All + Spider-Man abilities loaded."), true);
        });

        ServerTickEvents.END_SERVER_TICK.register(OneForAllMod::tickPlayers);
        ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
            if (!(source.getAttacker() instanceof ServerPlayerEntity player)) return;
            if (!(entity instanceof HostileEntity) && entity.getMaxHealth() < 100.0F) return;
            OFAProgression.grantVestiges(player, entity.getMaxHealth());
        });
        LOGGER.info("One For All + Spider-Man initialized for Minecraft 1.20.1");
    }

    private static void tickPlayers(MinecraftServer server) {
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            OFAPlayerProgress progress = OFAWorldState.get(server).getPlayer(player.getUuid());
            OFAProgression.tick(player, progress);
        }
    }
}
