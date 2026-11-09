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
    private static KeyBinding mode, iceWall, iceLance, iceSlide, fireBurst, jetBurn, flashfreeze, steamNova, iceArmor;

    @Override
    public void onInitializeClient() {
        mode = key("mode", GLFW.GLFW_KEY_C);
        iceWall = key("ice_wall", GLFW.GLFW_KEY_R);
        iceLance = key("ice_lance", GLFW.GLFW_KEY_G);
        iceSlide = key("ice_slide", GLFW.GLFW_KEY_H);
        fireBurst = key("fire_burst", GLFW.GLFW_KEY_V);
        jetBurn = key("jet_burn", GLFW.GLFW_KEY_B);
        flashfreeze = key("flashfreeze", GLFW.GLFW_KEY_N);
        steamNova = key("steam_nova", GLFW.GLFW_KEY_M);
        iceArmor = key("ice_armor", GLFW.GLFW_KEY_J);
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null) return;
            send(mode, HalfColdHalfHotMod.MODE);
            send(iceWall, HalfColdHalfHotMod.ICE_WALL);
            send(iceLance, HalfColdHalfHotMod.ICE_LANCE);
            send(iceSlide, HalfColdHalfHotMod.ICE_SLIDE);
            send(fireBurst, HalfColdHalfHotMod.FIRE_BURST);
            send(jetBurn, HalfColdHalfHotMod.JET_BURN);
            send(flashfreeze, HalfColdHalfHotMod.FLASHFREEZE);
            send(steamNova, HalfColdHalfHotMod.STEAM_NOVA);
            send(iceArmor, HalfColdHalfHotMod.ICE_ARMOR);
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
