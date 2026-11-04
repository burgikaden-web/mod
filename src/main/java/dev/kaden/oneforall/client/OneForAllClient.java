package dev.kaden.oneforall.client;

import dev.kaden.oneforall.network.OFAIds;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class OneForAllClient implements ClientModInitializer {
    private static KeyBinding toggleKey;
    private static KeyBinding blackwhipKey;
    private static KeyBinding dangerSenseKey;
    private static KeyBinding smashKey;
    private static final Set<HostileEntity> markedEntities = new HashSet<>();

    @Override
    public void onInitializeClient() {
        toggleKey = register("key.oneforall.toggle", GLFW.GLFW_KEY_O);
        blackwhipKey = register("key.oneforall.blackwhip", GLFW.GLFW_KEY_R);
        dangerSenseKey = register("key.oneforall.danger_sense", GLFW.GLFW_KEY_G);
        smashKey = register("key.oneforall.detroit_smash", GLFW.GLFW_KEY_V);

        ClientPlayNetworking.registerGlobalReceiver(OFAIds.SYNC, (client, handler, buf, responseSender) -> {
            int vestiges = buf.readInt();
            int percentage = buf.readInt();
            int energy = buf.readInt();
            boolean active = buf.readBoolean();
            boolean dangerSense = buf.readBoolean();
            boolean blackwhip = buf.readBoolean();
            boolean dangerUnlocked = buf.readBoolean();
            boolean smash = buf.readBoolean();
            client.execute(() -> OFAClientData.update(vestiges, percentage, energy, active,
                    dangerSense, blackwhip, dangerUnlocked, smash));
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.getNetworkHandler() != null && client.player != null) {
                while (toggleKey.wasPressed()) ClientPlayNetworking.send(OFAIds.TOGGLE, PacketByteBufs.empty());
                while (blackwhipKey.wasPressed()) ClientPlayNetworking.send(OFAIds.BLACKWHIP, PacketByteBufs.empty());
                while (dangerSenseKey.wasPressed()) ClientPlayNetworking.send(OFAIds.DANGER_SENSE, PacketByteBufs.empty());
                while (smashKey.wasPressed()) ClientPlayNetworking.send(OFAIds.DETROIT_SMASH, PacketByteBufs.empty());
            }
            updateDangerHighlights(client);
        });

        HudRenderCallback.EVENT.register((drawContext, tickDelta) -> renderHud(MinecraftClient.getInstance(), drawContext));
    }

    private static KeyBinding register(String translationKey, int keyCode) {
        return KeyBindingHelper.registerKeyBinding(new KeyBinding(
                translationKey, InputUtil.Type.KEYSYM, keyCode, "key.categories.oneforall"));
    }

    private static void updateDangerHighlights(MinecraftClient client) {
        if (client.world == null || client.player == null || !OFAClientData.dangerSense) {
            clearHighlights();
            return;
        }

        List<HostileEntity> nearby = client.world.getEntitiesByClass(HostileEntity.class,
                client.player.getBoundingBox().expand(18.0), entity -> entity.isAlive());
        Set<HostileEntity> visibleNow = new HashSet<>(nearby);
        for (HostileEntity entity : nearby) {
            if (!entity.isGlowing()) {
                entity.setGlowing(true);
                markedEntities.add(entity);
            }
        }
        for (HostileEntity previous : new ArrayList<>(markedEntities)) {
            if (!visibleNow.contains(previous)) {
                previous.setGlowing(false);
                markedEntities.remove(previous);
            }
        }
    }

    private static void clearHighlights() {
        for (HostileEntity entity : markedEntities) entity.setGlowing(false);
        markedEntities.clear();
    }

    private static void renderHud(MinecraftClient client, net.minecraft.client.gui.DrawContext context) {
        if (client.player == null || client.options.hudHidden || !OFAClientData.receivedSync) return;
        int x = 10;
        int y = context.getScaledWindowHeight() - 56;
        int barWidth = 124;
        context.fill(x - 5, y - 6, x + barWidth + 5, y + 49, 0x99081018);
        context.drawTextWithShadow(client.textRenderer,
                Text.literal("ONE FOR ALL  " + OFAClientData.percentage + "%"), x, y, 0xFFFFFFFF);
        context.fill(x, y + 13, x + barWidth, y + 20, 0xFF30343B);
        context.fill(x, y + 13, x + (int) (barWidth * OFAClientData.percentage / 100.0), y + 20, 0xFF28C8A8);
        context.drawTextWithShadow(client.textRenderer,
                Text.literal("Vestiges: " + OFAClientData.vestiges + "   Energy: " + OFAClientData.energy + "%"),
                x, y + 25, 0xFFE4EAF0);
        String status = OFAClientData.active ? "ACTIVE" : "OFF";
        if (OFAClientData.dangerSense) status += "  |  DANGER SENSE";
        context.drawTextWithShadow(client.textRenderer, Text.literal(status), x, y + 37,
                OFAClientData.active ? 0xFF61E6B4 : 0xFFFF7777);
    }
}
