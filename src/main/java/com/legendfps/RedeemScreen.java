package com.legendfps;

import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class RedeemScreen extends RedScreen {
    private EditBox box;

    public RedeemScreen(Screen parent) {
        super("Redeem Code", parent);
    }

    @Override
    protected void init() {
        int x = this.width / 2 - 100;
        int y = Math.max(50, this.height / 2 - 40);

        box = new EditBox(this.font, x, y, 200, 20, Component.literal("Code"));
        box.setMaxLength(16);
        addRenderableWidget(box);

        add("Redeem", x, y + 28, 200, b -> status = Config.redeem(box.getValue()));
        add("Back", x, y + 56, 200, b -> onClose());
    }
}
