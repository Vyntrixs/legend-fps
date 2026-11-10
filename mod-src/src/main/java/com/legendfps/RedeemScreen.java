package com.legendfps;

import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class RedeemScreen extends RedScreen {
    private EditBox box;
    private boolean coinsMode = false;
    public RedeemScreen(Screen parent) { super("Redeem", parent); }

    @Override protected void init() {
        int x = this.width / 2 - 100;
        int y = Math.max(46, this.height / 2 - 52);
        add(coinsMode ? "COINS CODES" : "COSMETIC CODES", x, y - 28, 200, b -> {
            coinsMode = !coinsMode; status = ""; rebuildWidgets();
        });
        box = new EditBox(this.font, x, y, 200, 20, Component.literal("Enter code"));
        box.setMaxLength(24);
        box.setHint(Component.literal(coinsMode ? "Coin code" : "Cosmetic code"));
        addRenderableWidget(box);
        add("Redeem", x, y + 28, 200, b -> status = coinsMode ? Config.redeemCoins(box.getValue()) : Config.redeem(box.getValue()));
        add("Balance: " + Config.data.coins + " Coins", x, y + 54, 200, b -> {});
        add("Back", x, y + 82, 200, b -> onClose());
    }
}
