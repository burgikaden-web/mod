package dev.kaden.oneforall.spiderman;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

public final class SpiderManClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        KeyBinding swing = key("key.oneforall.web_swing", GLFW.GLFW_KEY_Z);
        KeyBinding zip = key("key.oneforall.web_zip", GLFW.GLFW_KEY_X);
        KeyBinding shot = key("key.oneforall.web_shot", GLFW.GLFW_KEY_C);

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.getNetworkHandler() == null || client.player == null) return;
            while (swing.wasPressed()) ClientPlayNetworking.send(SpiderManIds.WEB_SWING, PacketByteBufs.empty());
            while (zip.wasPressed()) ClientPlayNetworking.send(SpiderManIds.WEB_ZIP, PacketByteBufs.empty());
            while (shot.wasPressed()) ClientPlayNetworking.send(SpiderManIds.WEB_SHOT, PacketByteBufs.empty());
        });
    }

    private static KeyBinding key(String translationKey, int code) {
        return KeyBindingHelper.registerKeyBinding(new KeyBinding(translationKey,
                InputUtil.Type.KEYSYM, code, "key.categories.oneforall"));
    }
}
