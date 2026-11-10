package com.legendfps;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public abstract class RedScreen extends Screen {
    protected static final int RED = 0xFFE01B1B;
    protected static final int WHITE = 0xFFFFFFFF;
    protected static final int GRAY = 0xFFAAAAAA;

    protected final Screen parent;
    protected String status = "";

    protected RedScreen(String title, Screen parent) {
        super(Component.literal(title));
        this.parent = parent;
    }

    protected Button add(String label, int x, int y, int w, Button.OnPress press) {
        return addRenderableWidget(Button.builder(Component.literal(label), press)
                .bounds(x, y, w, 20).build());
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(parent);
    }

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float delta) {
        g.fill(0, 0, this.width, this.height, 0xF008080B);
        g.fill(0, 0, this.width, 3, RED);
        g.fill(0, this.height - 3, this.width, this.height, RED);
        int panelWidth = Math.min(520, this.width - 20);
        int left = (this.width - panelWidth) / 2;
        g.fill(left, 34, left + panelWidth, this.height - 28, 0xB80D0D12);
        g.fill(left, 34, left + panelWidth, 36, 0xFF7D101A);
        g.fill(left, this.height - 30, left + panelWidth, this.height - 28, 0xFF7D101A);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
        super.render(g, mouseX, mouseY, delta);
        g.drawCenteredString(this.font,
                Component.literal(this.title.getString()).withStyle(ChatFormatting.BOLD),
                this.width / 2, 12, RED);
        if (!status.isEmpty()) {
            g.drawCenteredString(this.font, Component.literal(status), this.width / 2, this.height - 42, WHITE);
        }
    }
}
