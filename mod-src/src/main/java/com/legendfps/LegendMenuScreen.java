package com.legendfps;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.gui.screens.options.OptionsScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen;
import net.minecraft.network.chat.Component;

public class LegendMenuScreen extends RedScreen {
    private final boolean pauseMode;

    public LegendMenuScreen(Screen parent) {
        this(parent, false);
    }

    public LegendMenuScreen(Screen parent, boolean pauseMode) {
        super(pauseMode ? "LEGEND MENU" : "LEGEND FPS", parent);
        this.pauseMode = pauseMode;
    }

    @Override
    protected void init() {
        int columns = width >= 560 ? 2 : 1;
        int buttonWidth = columns == 2 ? Math.min(190, (width - 70) / 2) : Math.min(230, width - 44);
        int gap = 10;
        int totalWidth = columns * buttonWidth + (columns - 1) * gap;
        int startX = (width - totalWidth) / 2;
        int startY = pauseMode ? Math.max(78, height / 2 - 100) : Math.max(76, height / 2 - 118);
        int rowGap = 27;
        int index = 0;

        if (pauseMode) {
            addAt("RESUME", index++, columns, startX, startY, buttonWidth, rowGap,
                    b -> Minecraft.getInstance().setScreen(null));
        } else {
            addAt("SINGLEPLAYER", index++, columns, startX, startY, buttonWidth, rowGap,
                    b -> Minecraft.getInstance().setScreen(new SelectWorldScreen(this)));
            addAt("MULTIPLAYER", index++, columns, startX, startY, buttonWidth, rowGap,
                    b -> Minecraft.getInstance().setScreen(new JoinMultiplayerScreen(this)));
        }

        addAt("FRIENDS", index++, columns, startX, startY, buttonWidth, rowGap,
                b -> Minecraft.getInstance().setScreen(new FriendsScreen(this)));
        addAt("INVITE FRIEND", index++, columns, startX, startY, buttonWidth, rowGap,
                b -> Minecraft.getInstance().setScreen(new FriendsScreen(this, true)));
        addAt("WARDROBE", index++, columns, startX, startY, buttonWidth, rowGap,
                b -> Minecraft.getInstance().setScreen(new CosmeticsScreen(this)));
        addAt("SKIN MANAGER", index++, columns, startX, startY, buttonWidth, rowGap,
                b -> Minecraft.getInstance().setScreen(new SkinManagerScreen(this)));
        addAt("PERFORMANCE", index++, columns, startX, startY, buttonWidth, rowGap,
                b -> Minecraft.getInstance().setScreen(new PerformanceScreen(this)));
        addAt("COINS & REDEEM", index++, columns, startX, startY, buttonWidth, rowGap,
                b -> Minecraft.getInstance().setScreen(new RedeemScreen(this)));
        addAt("SETTINGS", index++, columns, startX, startY, buttonWidth, rowGap,
                b -> Minecraft.getInstance().setScreen(new OptionsScreen(this, Minecraft.getInstance().options)));

        if (pauseMode) {
            addAt("DISCONNECT", index++, columns, startX, startY, buttonWidth, rowGap,
                    b -> Minecraft.getInstance().disconnect(new TitleScreen(), false));
        } else {
            addAt("QUIT GAME", index++, columns, startX, startY, buttonWidth, rowGap,
                    b -> Minecraft.getInstance().stop());
        }
    }

    private void addAt(String label, int index, int columns, int startX, int startY,
                       int buttonWidth, int rowGap, net.minecraft.client.gui.components.Button.OnPress action) {
        int col = index % columns;
        int row = index / columns;
        add(label, startX + col * (buttonWidth + 10), startY + row * rowGap, buttonWidth, action);
    }

    @Override
    public void onClose() {
        if (pauseMode) {
            Minecraft.getInstance().setScreen(null);
        } else if (parent != null) {
            super.onClose();
        } else {
            Minecraft.getInstance().setScreen(new TitleScreen());
        }
    }

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float delta) {
        super.renderBackground(g, mouseX, mouseY, delta);
        int panelWidth = Math.min(560, width - 20);
        int left = (width - panelWidth) / 2;
        int top = 34;
        int bottom = height - 24;
        g.fill(left, top, left + panelWidth, bottom, 0xF20A0A0D);
        g.fill(left, top, left + panelWidth, top + 3, RED);
        g.fill(left, bottom - 3, left + panelWidth, bottom, RED);
        g.fill(left, top, left + 2, bottom, 0xFF5A1018);
        g.fill(left + panelWidth - 2, top, left + panelWidth, bottom, 0xFF5A1018);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
        super.render(g, mouseX, mouseY, delta);
        g.drawCenteredString(font, Component.literal("SOCIAL  •  WARDROBE  •  SKINS  •  PERFORMANCE"),
                width / 2, 19, WHITE);
        if (Minecraft.getInstance().player != null && width >= 560) {
            int centerX = width / 2 + Math.min(170, width / 4);
            int bottom = Math.min(height - 55, height / 2 + 110);
            InventoryScreen.renderEntityInInventoryFollowsMouse(
                    g, centerX - 70, bottom, centerX + 70, bottom + 4, 76,
                    0.0F, centerX - mouseX, bottom - mouseY, Minecraft.getInstance().player);
            g.drawCenteredString(font, Component.literal(Minecraft.getInstance().getUser().getName()),
                    centerX, bottom + 8, GRAY);
        }
        g.drawCenteredString(font,
                Component.literal("LEGEND COINS  " + Config.data.coins + "   •   OWNED " + Config.data.owned.size()),
                width / 2, height - 14, GRAY);
    }
}
