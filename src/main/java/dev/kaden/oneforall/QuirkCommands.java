package dev.kaden.oneforall;

import com.mojang.brigadier.arguments.StringArgumentType;
import dev.kaden.oneforall.data.OFAPlayerProgress;
import dev.kaden.oneforall.data.OFAWorldState;
import dev.kaden.oneforall.network.OFANetwork;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.Map;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public final class QuirkCommands {
    private static final Map<String, String> QUIRKS = Map.of(
            "one_for_all", "One For All",
            "explosion", "Explosion",
            "half_cold_half_hot", "Half-Cold Half-Hot",
            "engine", "Engine",
            "hardening", "Hardening"
    );

    private QuirkCommands() {}

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(literal("quirk")
                    .then(literal("list").executes(context -> {
                        context.getSource().sendFeedback(() -> Text.literal(
                                "Available quirks: one_for_all, explosion, half_cold_half_hot, engine, hardening"), false);
                        return QUIRKS.size();
                    }))
                    .then(literal("get").executes(context -> {
                        ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
                        OFAPlayerProgress progress = OFAWorldState.get(context.getSource().getServer()).getPlayer(player.getUuid());
                        String name = QUIRKS.getOrDefault(progress.getQuirkId(), progress.getQuirkId());
                        context.getSource().sendFeedback(() -> Text.literal("Current quirk: " + name), false);
                        return 1;
                    }))
                    .then(literal("set").then(argument("quirk", StringArgumentType.word()).executes(context -> {
                        ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
                        String id = StringArgumentType.getString(context, "quirk").toLowerCase();
                        if (!QUIRKS.containsKey(id)) {
                            context.getSource().sendError(Text.literal("Unknown quirk. Use /quirk list."));
                            return 0;
                        }
                        OFAPlayerProgress progress = OFAWorldState.get(context.getSource().getServer()).getPlayer(player.getUuid());
                        progress.setQuirkId(id);
                        OFAWorldState.get(context.getSource().getServer()).markProgressDirty();
                        OFANetwork.sendSync(player, progress);
                        context.getSource().sendFeedback(() -> Text.literal("Quirk selected: " + QUIRKS.get(id)), true);
                        return 1;
                    }))));
        });
    }
}
