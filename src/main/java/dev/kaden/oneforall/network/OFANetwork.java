package dev.kaden.oneforall.network;

import dev.kaden.oneforall.OFAAbilities;
import dev.kaden.oneforall.OFAProgression;
import dev.kaden.oneforall.data.OFAPlayerProgress;
import dev.kaden.oneforall.data.OFAWorldState;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;

public final class OFANetwork {
    private OFANetwork() {}

    public static void registerServerReceivers() {
        ServerPlayNetworking.registerGlobalReceiver(OFAIds.TOGGLE,
                (server, player, handler, buf, responseSender) -> server.execute(() -> {
                    OFAPlayerProgress progress = OFAWorldState.get(server).getPlayer(player.getUuid());
                    progress.setActive(!progress.isActive());
                    OFAWorldState.get(server).markProgressDirty();
                    OFAProgression.refreshAttributes(player, progress);
                    player.sendMessage(net.minecraft.text.Text.literal(progress.isActive()
                            ? "One For All activated" : "One For All deactivated"), true);
                    sendSync(player, progress);
                }));

        ServerPlayNetworking.registerGlobalReceiver(OFAIds.BLACKWHIP,
                (server, player, handler, buf, responseSender) -> server.execute(() -> OFAAbilities.useBlackwhip(player)));

        ServerPlayNetworking.registerGlobalReceiver(OFAIds.DANGER_SENSE,
                (server, player, handler, buf, responseSender) -> server.execute(() -> {
                    OFAPlayerProgress progress = OFAWorldState.get(server).getPlayer(player.getUuid());
                    if (!progress.isActive()) {
                        player.sendMessage(net.minecraft.text.Text.literal("Activate One For All first."), true);
                        return;
                    }
                    if (!progress.hasDangerSense()) {
                        player.sendMessage(net.minecraft.text.Text.literal("Danger Sense unlocks at 20 vestiges."), true);
                        return;
                    }
                    progress.toggleDangerSense();
                    OFAWorldState.get(server).markProgressDirty();
                    player.sendMessage(net.minecraft.text.Text.literal(progress.isDangerSenseActive()
                            ? "Danger Sense enabled" : "Danger Sense disabled"), true);
                    sendSync(player, progress);
                }));

        ServerPlayNetworking.registerGlobalReceiver(OFAIds.DETROIT_SMASH,
                (server, player, handler, buf, responseSender) -> server.execute(() -> OFAAbilities.useDetroitSmash(player)));
    }

    public static void sendSync(ServerPlayerEntity player, OFAPlayerProgress progress) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeInt(progress.getVestiges());
        buf.writeInt(progress.getPercentage());
        buf.writeInt(progress.getEnergy());
        buf.writeBoolean(progress.isActive());
        buf.writeBoolean(progress.isDangerSenseActive());
        buf.writeBoolean(progress.hasBlackwhip());
        buf.writeBoolean(progress.hasDangerSense());
        buf.writeBoolean(progress.hasDetroitSmash());
        ServerPlayNetworking.send(player, OFAIds.SYNC, buf);
    }
}
