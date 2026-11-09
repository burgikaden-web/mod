package dev.kaden.oneforall;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import dev.kaden.oneforall.data.OFAPlayerProgress;
import dev.kaden.oneforall.data.OFAWorldState;
import dev.kaden.oneforall.network.OFANetwork;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public final class OFAVestigeCommands {
    private OFAVestigeCommands() {}

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                dispatcher.register(literal("vestiges")
                        .requires(source -> source.hasPermissionLevel(2))
                        .then(literal("get")
                                .executes(context -> {
                                    ServerPlayerEntity target = context.getSource().getPlayerOrThrow();
                                    OFAPlayerProgress progress = OFAWorldState.get(context.getSource().getServer())
                                            .getPlayer(target.getUuid());
                                    context.getSource().sendFeedback(() -> Text.literal(
                                            target.getName().getString() + " has " + progress.getVestiges()
                                                    + "/" + OFAPlayerProgress.MAX_VESTIGES + " vestiges."), false);
                                    return progress.getVestiges();
                                }))
                        .then(literal("set")
                                .then(argument("amount", IntegerArgumentType.integer(0, OFAPlayerProgress.MAX_VESTIGES))
                                        .executes(context -> {
                                            ServerPlayerEntity target = context.getSource().getPlayerOrThrow();
                                            int amount = IntegerArgumentType.getInteger(context, "amount");
                                            OFAPlayerProgress progress = OFAWorldState.get(context.getSource().getServer())
                                                    .getPlayer(target.getUuid());
                                            progress.setVestiges(amount);
                                            OFAWorldState.get(context.getSource().getServer()).markProgressDirty();
                                            OFAProgression.refreshAttributes(target, progress);
                                            OFANetwork.sendSync(target, progress);
                                            context.getSource().sendFeedback(() -> Text.literal(
                                                    "Set your vestiges to " + amount + "."), true);
                                            return amount;
                                        })))
                        .then(literal("add")
                                .then(argument("amount", IntegerArgumentType.integer(1, OFAPlayerProgress.MAX_VESTIGES))
                                        .executes(context -> {
                                            ServerPlayerEntity target = context.getSource().getPlayerOrThrow();
                                            int amount = IntegerArgumentType.getInteger(context, "amount");
                                            OFAPlayerProgress progress = OFAWorldState.get(context.getSource().getServer())
                                                    .getPlayer(target.getUuid());
                                            int before = progress.getVestiges();
                                            progress.addVestiges(amount);
                                            int added = progress.getVestiges() - before;
                                            OFAWorldState.get(context.getSource().getServer()).markProgressDirty();
                                            OFAProgression.refreshAttributes(target, progress);
                                            OFANetwork.sendSync(target, progress);
                                            context.getSource().sendFeedback(() -> Text.literal(
                                                    "Added " + added + " vestiges. Total: " + progress.getVestiges() + "."), true);
                                            return added;
                                        }))));
    }
}
