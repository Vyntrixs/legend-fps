package com.legendfps;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Accept / Decline window shown when a friend invites you to their world. */
public class InviteDialogScreen extends RedScreen {
    private final SocialService.Invite invite;
    private boolean answered;

    public InviteDialogScreen(Screen parent, SocialService.Invite invite) {
        super("World Invite", parent);
        this.invite = invite;
    }

    @Override
    protected void init() {
        int cx = this.width / 2;
        int y = this.height / 2 + 14;
        add("Accept", cx - 105, y, 100, b -> {
            answered = true;
            SocialService.respondToInvite(invite.id(), true);
            WorldInviteService.join(invite.address());
        });
        add("Decline", cx + 5, y, 100, b -> decline());
    }

    private void decline() {
        if (!answered) {
            answered = true;
            SocialService.respondToInvite(invite.id(), false);
        }
        Minecraft.getInstance().setScreen(parent);
    }

    @Override
    public void onClose() { decline(); }

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float delta) {
        g.fill(0, 0, this.width, this.height, 0xB0000000);
        int w = Math.min(280, this.width - 20);
        int left = (this.width - w) / 2;
        int top = this.height / 2 - 52;
        int bottom = this.height / 2 + 44;
        g.fill(left, top, left + w, bottom, 0xF20A0A0D);
        g.fill(left, top, left + w, top + 2, RED);
        g.fill(left, bottom - 2, left + w, bottom, RED);
        g.fill(left, top, left + 2, bottom, 0xFF5A1018);
        g.fill(left + w - 2, top, left + w, bottom, 0xFF5A1018);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
        super.render(g, mouseX, mouseY, delta);
        int cx = this.width / 2;
        g.drawCenteredString(this.font, Component.literal(invite.from() + " invited you to their world"), cx, this.height / 2 - 30, WHITE);
        g.drawCenteredString(this.font, Component.literal(invite.address()), cx, this.height / 2 - 14, GRAY);
    }
}
