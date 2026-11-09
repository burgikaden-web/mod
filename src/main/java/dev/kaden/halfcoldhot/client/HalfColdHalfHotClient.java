package dev.kaden.halfcoldhot.client;

import dev.kaden.halfcoldhot.HalfColdHalfHotMod;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

public class HalfColdHalfHotClient implements ClientModInitializer {
    private static KeyBinding iceWall, iceSpikes, iceSlide, fireBlast, jetBurn, flashfreeze;

    @Override
    public void onInitializeClient() {
        iceWall = key("ice_wall", GLFW.GLFW_KEY_R);
        iceSpikes = key("ice_spikes", GLFW.GLFW_KEY_G);
        iceSlide = key("ice_slide", GLFW.GLFW_KEY_H);
        fireBlast = key("fire_blast", GLFW.GLFW_KEY_V);
        jetBurn = key("jet_burn", GLFW.GLFW_KEY_B);
        flashfreeze = key("flashfreeze_heatwave", GLFW.GLFW_KEY_N);
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null) return;
            send(iceWall, HalfColdHalfHotMod.ICE_WALL);
            send(iceSpikes, HalfColdHalfHotMod.ICE_SPIKES);
            send(iceSlide, HalfColdHalfHotMod.ICE_SLIDE);
            send(fireBlast, HalfColdHalfHotMod.FIRE_BLAST);
            send(jetBurn, HalfColdHalfHotMod.JET_BURN);
            send(flashfreeze, HalfColdHalfHotMod.FLASHFREEZE_HEATWAVE);
        });
    }

    private static KeyBinding key(String name, int code) {
        return KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.halfcoldhot." + name, InputUtil.Type.KEYSYM, code, "category.halfcoldhot.abilities"));
    }

    private static void send(KeyBinding key, Identifier channel) {
        while (key.wasPressed()) ClientPlayNetworking.send(channel, PacketByteBufs.create());
    }
}
