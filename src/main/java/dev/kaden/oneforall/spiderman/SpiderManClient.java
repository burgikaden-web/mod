package dev.kaden.oneforall.spiderman;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.List;

public final class SpiderManClient {
    private static int senseCooldown = 0;
    private static KeyBinding swing, zip, shot, crawl, suit, sense;

    public void onInitializeClient() {
        swing = key("key.oneforall.web_swing", GLFW.GLFW_KEY_Z);
        zip = key("key.oneforall.web_zip", GLFW.GLFW_KEY_X);
        shot = key("key.oneforall.web_shot", GLFW.GLFW_KEY_C);
        crawl = key("key.oneforall.wall_crawl", GLFW.GLFW_KEY_H);
        suit = key("key.oneforall.next_suit", GLFW.GLFW_KEY_B);
        sense = key("key.oneforall.spider_sense", GLFW.GLFW_KEY_J);

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.getNetworkHandler() == null || client.player == null || client.world == null) return;
            while (swing.wasPressed()) ClientPlayNetworking.send(SpiderManIds.WEB_SWING, PacketByteBufs.empty());
            while (zip.wasPressed()) ClientPlayNetworking.send(SpiderManIds.WEB_ZIP, PacketByteBufs.empty());
            while (shot.wasPressed()) ClientPlayNetworking.send(SpiderManIds.WEB_SHOT, PacketByteBufs.empty());
            while (crawl.wasPressed()) ClientPlayNetworking.send(SpiderManIds.TOGGLE_CRAWL, PacketByteBufs.empty());
            while (suit.wasPressed()) ClientPlayNetworking.send(SpiderManIds.NEXT_SUIT, PacketByteBufs.empty());
            while (sense.wasPressed()) {
                client.player.sendMessage(Text.literal("Spider-Sense is active"), true);
                senseCooldown = 1;
            }
            if (senseCooldown > 0) senseCooldown--;
            List<HostileEntity> threats = client.world.getEntitiesByClass(HostileEntity.class,
                client.player.getBoundingBox().expand(12.0), e -> e.isAlive() && !e.isRemoved());
            if (!threats.isEmpty() && senseCooldown == 0) {
                senseCooldown = 70;
                client.player.sendMessage(Text.literal("⚠ SPIDER-SENSE — threat nearby!"), true);
                client.player.playSound(SoundEvents.BLOCK_NOTE_BLOCK_PLING, 0.8F, 1.8F);
            }
        });
    }

    private static KeyBinding key(String translationKey, int code) {
        return KeyBindingHelper.registerKeyBinding(new KeyBinding(translationKey,
            InputUtil.Type.KEYSYM, code, "key.categories.oneforall"));
    }
}