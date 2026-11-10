package com.legendfps;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class LegendMenuScreen extends RedScreen {
    public LegendMenuScreen(Screen parent) { super("LEGEND FPS", parent); }
    @Override protected void init() {
        int panelWidth = Math.min(330, this.width - 24);
        int x = (this.width - panelWidth) / 2 + 10;
        int y = Math.max(40, this.height / 2 - 91);
        int w = panelWidth - 20;
        add("Friends", x, y, w, b -> minecraft.setScreen(new FriendsScreen(this))); y += 23;
        add("Invite to World", x, y, w, b -> minecraft.setScreen(new FriendsScreen(this, true))); y += 23;
        add("Wardrobe / Cosmetics", x, y, w, b -> minecraft.setScreen(new CosmeticsScreen(this))); y += 23;
        add("Coins & Redeem Codes", x, y, w, b -> minecraft.setScreen(new RedeemScreen(this))); y += 23;
        add("Performance", x, y, w, b -> minecraft.setScreen(new PerformanceScreen(this))); y += 23;
        add("Effects: " + (Config.data.effects ? "ON" : "OFF"), x, y, w, b -> {
            Config.data.effects = !Config.data.effects; Config.save();
            b.setMessage(Component.literal("Effects: " + (Config.data.effects ? "ON" : "OFF")));
        }); y += 25;
        add("Back", x, y, w, b -> onClose());
    }
    @Override public void renderBackground(GuiGraphics g, int mx, int my, float d) {
        super.renderBackground(g, mx, my, d);
        int w=Math.min(350,this.width-16), l=(this.width-w)/2, t=30, bot=this.height-20;
        g.fill(l,t,l+w,bot,0xF20A0A0D); g.fill(l,t,l+w,t+2,RED); g.fill(l,bot-2,l+w,bot,RED);
    }
    @Override public void render(GuiGraphics g,int mx,int my,float d) {
        super.render(g,mx,my,d);
        g.drawCenteredString(font,Component.literal("SOCIAL  •  WARDROBE  •  PERFORMANCE"),width/2,20,WHITE);
        g.drawCenteredString(font,Component.literal("Coins: "+Config.data.coins+"  |  Owned: "+Config.data.owned.size()),width/2,height-13,GRAY);
    }
}
