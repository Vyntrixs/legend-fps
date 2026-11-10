package com.legendfps;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class LegendMenuScreen extends RedScreen {
    public LegendMenuScreen(Screen parent) {
        super("LEGEND FPS", parent);
    }

    @Override
    protected void init() {
        int panelWidth = Math.min(330, this.width - 24);
        int x = (this.width - panelWidth) / 2 + 10;
        int y = Math.max(48, this.height / 2 - 78);
        int buttonWidth = panelWidth - 20;

        int pending = SocialService.incoming().size();
        add("Friends" + (pending > 0 ? " (" + pending + " requests)" : ""), x, y, buttonWidth, b -> minecraft.setScreen(new FriendsScreen(this)));
        y += 25;
        add("Invite to World", x, y, buttonWidth, b -> minecraft.setScreen(new FriendsScreen(this, true)));
        y += 25;
        add("Cosmetics", x, y, buttonWidth, b -> minecraft.setScreen(new CosmeticsScreen(this)));
        y += 25;
        add("Performance", x, y, buttonWidth, b -> minecraft.setScreen(new PerformanceScreen(this)));
        y += 25;
        add("Redeem Code", x, y, buttonWidth, b -> minecraft.setScreen(new RedeemScreen(this)));
        y += 30;
        add("Cosmetic effects: " + (Config.data.effects ? "ON" : "OFF"), x, y, buttonWidth, b -> {
            Config.data.effects = !Config.data.effects;
            Config.save();
            b.setMessage(Component.literal("Cosmetic effects: " + (Config.data.effects ? "ON" : "OFF")));
        });
        y += 28;
        add("Back", x, y, buttonWidth, b -> onClose());
    }

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float delta) {
        super.renderBackground(g, mouseX, mouseY, delta);
        int w = Math.min(350, this.width - 16);
        int left = (this.width - w) / 2;
        int top = 34;
        int bottom = this.height - 24;
        g.fill(left, top, left + w, bottom, 0xF20A0A0D);
        g.fill(left, top, left + w, top + 2, RED);
        g.fill(left, bottom - 2, left + w, bottom, RED);
        g.fill(left, top, left + 2, bottom, 0xFF5A1018);
        g.fill(left + w - 2, top, left + w, bottom, 0xFF5A1018);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
        super.render(g, mouseX, mouseY, delta);
        g.drawCenteredString(this.font,
                Component.literal("SOCIAL  •  COSMETICS  •  PERFORMANCE"),
                this.width / 2, 25, WHITE);
        g.drawCenteredString(this.font,
                Component.literal("Unlocked " + Config.data.owned.size() + " / " + Cosmetic.ALL.size()),
                this.width / 2, this.height - 15, GRAY);
    }
}
