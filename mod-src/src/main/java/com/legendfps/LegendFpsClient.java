package com.legendfps;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import org.lwjgl.glfw.GLFW;

public class LegendFpsClient implements ClientModInitializer {
    private static KeyMapping openKey;

    @Override
    public void onInitializeClient() {
        Config.load();
        CosmeticModelRenderer.register();
        if (Config.data.relayUrl != null && !Config.data.relayUrl.isBlank()) SocialService.start();
        ClientReceiveMessageEvents.GAME.register((message, overlay) -> WorldInviteService.observeGameMessage(message));
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> WorldInviteService.reset());

        // Replace the vanilla title and pause screens with LEGEND's own navigation.
        ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            if (client.screen != screen) return;
            if (screen instanceof TitleScreen) {
                client.setScreen(new LegendMenuScreen(null, false));
            } else if (screen instanceof PauseScreen) {
                client.setScreen(new LegendMenuScreen(null, true));
            }
        });

        openKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.legendfps.open",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_RIGHT_SHIFT,
                KeyMapping.Category.MISC));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openKey.consumeClick()) {
                if (client.screen == null) {
                    client.setScreen(new LegendMenuScreen(null, true));
                }
            }
            CosmeticEffects.tick(client);
            WorldInviteService.tick(client);
            SocialService.pumpInvites(client);
        });
    }
}
