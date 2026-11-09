package dev.kaden.oneforall;

import com.zigythebird.playeranimatorapi.API.PlayerAnimAPI;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

public final class OFAAnimationController {
    private OFAAnimationController() {}

    public static void playFullCowling(ServerPlayerEntity player) {
        play(player, "full_cowling");
    }

    public static void playBlackwhip(ServerPlayerEntity player) {
        play(player, "blackwhip_cast");
    }

    public static void playSmash(ServerPlayerEntity player) {
        play(player, "detroit_smash");
    }

    private static void play(ServerPlayerEntity player, String animationName) {
        if (player.getServerWorld() == null) return;
        PlayerAnimAPI.playPlayerAnim(player.getServerWorld(), player,
                new Identifier(OneForAllMod.MOD_ID, animationName));
    }
}
