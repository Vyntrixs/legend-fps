package com.legendfps;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class AccountScreen extends RedScreen {
    private EditBox username;
    private EditBox password;
    private boolean registerMode;

    public AccountScreen(Screen parent) { super("LEGEND FPS ACCOUNT", parent); }

    @Override
    protected void init() {
        int panel = Math.min(280, this.width - 24);
        int x = (this.width - panel) / 2;
        int y = Math.max(58, this.height / 2 - 72);
        username = new EditBox(font, x, y, panel, 20, Component.literal("Username"));
        username.setMaxLength(16);
        username.setHint(Component.literal("Username (3-16 characters)"));
        addRenderableWidget(username);
        y += 27;
        password = new EditBox(font, x, y, panel, 20, Component.literal("Password"));
        password.setMaxLength(128);
        password.setHint(Component.literal("Password (minimum 6 characters)"));
        password.setMaxLength(128);
        addRenderableWidget(password);
        y += 30;
        add(registerMode ? "Create account" : "Login", x, y, panel, b -> submit());
        y += 25;
        add(registerMode ? "I already have an account" : "Create a new account", x, y, panel, b -> {
            registerMode = !registerMode;
            rebuildWidgets();
        });
        y += 25;
        add("Saved accounts", x, y, panel, b -> minecraft.setScreen(new AccountManagerScreen(this)));
        y += 25;
        add("Back", x, y, panel, b -> onClose());
    }

    private void submit() {
        String name = username.getValue().trim();
        String pass = password.getValue();
        if (!AccountService.canAdd(name)) { status = "Remove one saved account before adding another"; return; }
        if (pass.length() < 6) { status = "Password must contain at least 6 characters"; return; }
        status = registerMode ? "Creating account…" : "Signing in…";
        AccountService.login(name, pass, registerMode, result -> {
            status = result.message();
            if (result.ok()) {
                password.setValue("");
                minecraft.setScreen(new AccountManagerScreen(parent));
            }
        });
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
        super.render(g, mouseX, mouseY, delta);
        g.drawCenteredString(font, "Up to 3 saved accounts on this device", width / 2, height - 16, GRAY);
    }
}
