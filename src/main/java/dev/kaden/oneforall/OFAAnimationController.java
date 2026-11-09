package dev.kaden.oneforall;

import com.zigythebird.playeranimatorapi.API.PlayerAnimAPI;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

public final class OFAAnimationController {
    private OFAAnimationController() {}

    public static void playFullCowling(ServerPlayerEntity player) { play(player, "full_cowling"); }
    public static void playBlackwhip(ServerPlayerEntity player) { play(player, "blackwhip_cast"); }
    public static void playSmash(ServerPlayerEntity player) { play(player, "detroit_smash"); }
    public static void playDelawareSmash(ServerPlayerEntity player) { play(player, "delaware_smash"); }

    public static void stopFullCowling(ServerPlayerEntity player) {
        PlayerAnimAPI.stopPlayerAnim(player.getServerWorld(), player,
                new Identifier(OneForAllMod.MOD_ID, "full_cowling"));
    }

    private static void play(ServerPlayerEntity player, String animationName) {
        PlayerAnimAPI.playPlayerAnim(player.getServerWorld(), player,
                new Identifier(OneForAllMod.MOD_ID, animationName));
    }
}
