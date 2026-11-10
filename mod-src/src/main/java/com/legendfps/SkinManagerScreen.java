package com.legendfps;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;

/**
 * LEGEND skin hub. Shows the active Minecraft skin in a live 3D preview and
 * provides the official profile link. Custom PNG importing/applying is a
 * separate integration and is deliberately not presented as working yet.
 */
public final class SkinManagerScreen extends RedScreen {
    private static final String SKIN_PROFILE = "https://www.minecraft.net/msaprofile/mygames/editskin";

    public SkinManagerScreen(Screen parent) {
        super("SKIN MANAGER", parent);
    }

    @Override
    protected void init() {
        int center = width / 2;
        add("Copy official skin page link", center - 105, height - 76, 210, button -> {
            Minecraft.getInstance().keyboardHandler.setClipboard(SKIN_PROFILE);
            status = "Skin profile link copied";
        });
        add("Back", center - 55, height - 49, 110, button -> onClose());
    }

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float delta) {
        super.renderBackground(g, mouseX, mouseY, delta);
        int panelWidth = Math.min(500, width - 24);
        int left = (width - panelWidth) / 2;
        g.fill(left + 12, 48, left + panelWidth - 12, height - 94, 0xD9101017);
        g.fill(left + 12, 48, left + panelWidth - 12, 50, RED);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
        super.render(g, mouseX, mouseY, delta);
        Minecraft mc = Minecraft.getInstance();
        int center = width / 2;
        if (mc.player != null) {
            int size = Math.min(115, Math.max(72, height / 3));
            int x1 = center - size;
            int x2 = center + size;
            int y1 = Math.min(height - 112, Math.max(180, height / 2 + size / 2));
            InventoryScreen.renderEntityInInventoryFollowsMouse(
                    g, x1, y1, x2, y1 + 4, size, 0.0F,
                    center - mouseX, y1 - mouseY, mc.player);
            g.drawCenteredString(font, Component.literal("ACTIVE SKIN  •  " + mc.getUser().getName()),
                    center, 66, WHITE);
        } else {
            g.drawCenteredString(font, Component.literal("Launch a world to preview your player here"),
                    center, 110, GRAY);
        }
        g.drawCenteredString(font, Component.literal("Your current Minecraft account skin is shown above."),
                center, height - 112, WHITE);
        g.drawCenteredString(font, Component.literal("To change the account skin, use the official profile page."),
                center, height - 99, GRAY);
        g.drawCenteredString(font, Component.literal("PNG import and in-game skin applying are not implemented yet."),
                center, height - 86, 0xFFFF7777);
    }
}
