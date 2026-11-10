package com.legendfps;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

public class LegendFpsClient implements ClientModInitializer {
    private static KeyMapping openKey;

    @Override
    public void onInitializeClient() {
        Config.load();
        if (Config.data.relayUrl != null && !Config.data.relayUrl.isBlank()) SocialService.start();
        ClientReceiveMessageEvents.GAME.register((message, overlay) -> WorldInviteService.observeGameMessage(message));
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> WorldInviteService.reset());

        openKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.legendfps.open",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_RIGHT_SHIFT,
                KeyMapping.Category.MISC));

        // Add a LEGEND FPS shortcut to both the main menu and the pause menu.
        ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            if (screen instanceof TitleScreen || screen instanceof PauseScreen) {
                int buttonWidth = 112;
                int x = Math.max(6, screen.width - buttonWidth - 8);
                int y = screen instanceof TitleScreen ? 8 : Math.max(8, screen.height - 30);

                Button button = Button.builder(Component.literal("LEGEND FPS"), b ->
                                client.setScreen(new LegendMenuScreen(screen)))
                        .bounds(x, y, buttonWidth, 20)
                        .build();
                screen.addRenderableWidget(button);
            }
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openKey.consumeClick()) {
                if (client.screen == null) {
                    client.setScreen(new LegendMenuScreen(null));
                }
            }
            CosmeticEffects.tick(client);
            WorldInviteService.tick(client);
            SocialService.pumpInvites(client);
        });
    }
}
