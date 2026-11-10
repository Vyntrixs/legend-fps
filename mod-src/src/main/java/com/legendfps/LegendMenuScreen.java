package com.legendfps;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.gui.screens.options.OptionsScreen;
import net.minecraft.network.chat.Component;

import java.util.List;

public class LegendMenuScreen extends RedScreen {
    private static final String[] TABS = {"HOME", "SOCIAL", "WARDROBE", "SKINS", "EMOTES", "STORE", "SETTINGS"};
    private final boolean pauseMode;
    private int tab = 0;
    private int categoryIndex = 0;
    private String selectedFriend;
    private String selectedCosmeticId;
    private EditBox nameBox;
    private EditBox codeBox;

    public LegendMenuScreen(Screen parent) { this(parent, false); }

    public LegendMenuScreen(Screen parent, boolean pauseMode) {
        super("LEGEND", parent);
        this.pauseMode = pauseMode;
        this.tab = pauseMode ? 0 : 0;
    }

    @Override
    protected void init() {
        clearWidgets();
        int navY = 38;
        int gap = 3;
        int navW = Math.max(38, Math.min(76, (width - 24 - gap * (TABS.length - 1)) / TABS.length));
        int total = navW * TABS.length + gap * (TABS.length - 1);
        int navX = (width - total) / 2;
        for (int i = 0; i < TABS.length; i++) {
            final int page = i;
            add((tab == i ? "• " : "") + TABS[i], navX + i * (navW + gap), navY, navW,
                    b -> { tab = page; status = ""; rebuildWidgets(); });
        }

        int left = Math.max(12, width / 2 - Math.min(260, width / 2 - 12));
        int panelW = Math.min(520, width - 24);
        int y = 72;
        int buttonW = Math.min(230, panelW - 24);
        int cx = width / 2;

        switch (tab) {
            case 0 -> {
                add("RESUME GAME", cx - buttonW / 2, y + 8, buttonW, b -> Minecraft.getInstance().setScreen(null));
                add("INVITE FRIEND", cx - buttonW / 2, y + 36, buttonW, b -> { tab = 1; rebuildWidgets(); });
                add("OPEN SINGLEPLAYER", cx - buttonW / 2, y + 64, buttonW, b ->
                        Minecraft.getInstance().setScreen(new net.minecraft.client.gui.screens.worldselection.SelectWorldScreen(this)));
                add("MULTIPLAYER", cx - buttonW / 2, y + 92, buttonW, b ->
                        Minecraft.getInstance().setScreen(new net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen(this)));
            }
            case 1 -> initSocial(left + 12, y, panelW - 24);
            case 2 -> initWardrobe(left + 12, y, panelW - 24);
            case 3 -> {
                add("OPEN OFFICIAL SKIN PROFILE", cx - buttonW / 2, y + 12, buttonW,
                        b -> { Minecraft.getInstance().keyboardHandler.setClipboard("https://www.minecraft.net/msaprofile/mygames/editskin"); status = "Official skin page link copied"; });
                add("SKIN PNG IMPORT IS NOT READY", cx - buttonW / 2, y + 42, buttonW, b -> status = "PNG importing is still being integrated");
            }
            case 4 -> {
                add("DANCE PACK 854123", cx - buttonW / 2, y + 10, buttonW, b -> status = Config.redeem("854123"));
                add("LEGENDARY PACK 739206", cx - buttonW / 2, y + 38, buttonW, b -> status = Config.redeem("739206"));
                add("SPECIAL PACK 916482", cx - buttonW / 2, y + 66, buttonW, b -> status = Config.redeem("916482"));
                add("EMOTE ANIMATIONS IN DEVELOPMENT", cx - buttonW / 2, y + 98, buttonW, b -> {});
            }
            case 5 -> initStore(cx, y, buttonW);
            case 6 -> {
                add("MINECRAFT SETTINGS", cx - buttonW / 2, y + 10, buttonW,
                        b -> Minecraft.getInstance().setScreen(new OptionsScreen(this, Minecraft.getInstance().options)));
                add("PERFORMANCE", cx - buttonW / 2, y + 38, buttonW,
                        b -> Minecraft.getInstance().setScreen(new PerformanceScreen(this)));
                add(pauseMode ? "DISCONNECT" : "QUIT GAME", cx - buttonW / 2, y + 66, buttonW, b -> {
                    if (pauseMode) Minecraft.getInstance().disconnect(new TitleScreen(), false);
                    else Minecraft.getInstance().stop();
                });
            }
        }
    }

    private void initSocial(int x, int y, int w) {
        nameBox = new EditBox(font, x, y, Math.max(90, w - 72), 20, Component.literal("Minecraft username"));
        nameBox.setMaxLength(16);
        nameBox.setHint(Component.literal("Player name"));
        addRenderableWidget(nameBox);
        add("ADD", x + w - 66, y, 66, b -> {
            String name = nameBox.getValue().trim();
            if (SocialService.sendFriendRequest(name)) status = "Friend request sent";
            else status = SocialService.status();
            nameBox.setValue("");
        });
        List<SocialService.Friend> friends = SocialService.friends();
        int rowY = y + 27;
        int max = Math.max(1, Math.min(6, (height - rowY - 92) / 22));
        for (int i = 0; i < Math.min(max, friends.size()); i++) {
            SocialService.Friend f = friends.get(i);
            String label = (f.online() ? "● " : "○ ") + f.name() + (f.name().equals(selectedFriend) ? "  ✓" : "");
            add(label, x, rowY, w, b -> { selectedFriend = f.name(); status = "Selected " + f.name(); rebuildWidgets(); });
            rowY += 22;
        }
        if (friends.isEmpty()) {
            add("NO FRIENDS - CONFIGURE RELAY IN SETTINGS", x, rowY + 8, w, b -> status = SocialService.status());
        }
        add("INVITE SELECTED FRIEND TO WORLD", x, height - 78, w, b -> {
            if (selectedFriend == null) { status = "Select a friend first"; return; }
            WorldInviteService.inviteFriend(selectedFriend);
            status = WorldInviteService.status();
        });
        add("FRIEND REQUESTS", x, height - 52, w, b -> status = "Incoming: " + SocialService.incoming().size() + "  Outgoing: " + SocialService.outgoing().size());
    }

    private void initWardrobe(int x, int y, int w) {
        Cosmetic.Category[] categories = Cosmetic.Category.values();
        Cosmetic.Category cat = categories[categoryIndex % categories.length];
        int sideW = Math.max(78, Math.min(112, w / 4));
        int gap = 7;
        int contentX = x + sideW + gap;
        int contentW = Math.max(80, w - sideW - gap);
        int rowH = 22;

        // Category rail, similar to the Essential wardrobe.
        int visibleCategories = Math.min(categories.length, Math.max(5, (height - y - 112) / rowH));
        int firstCategory = Math.max(0, Math.min(categoryIndex - visibleCategories / 2,
                categories.length - visibleCategories));
        for (int i = 0; i < visibleCategories; i++) {
            int index = firstCategory + i;
            Cosmetic.Category c = categories[index];
            add((index == categoryIndex ? "› " : "") + c.label, x, y + i * rowH,
                    sideW, b -> { categoryIndex = index; selectedCosmeticId = null; rebuildWidgets(); });
        }

        List<Cosmetic> items = Cosmetic.in(cat);
        int cols = contentW >= 330 ? 3 : contentW >= 210 ? 2 : 1;
        int cardGap = 4;
        int cardW = Math.max(64, (contentW - cardGap * (cols - 1)) / cols);
        int cardY = y + 2;
        int cardH = 43;
        int maxRows = Math.max(1, Math.min(5, (height - cardY - 100) / (cardH + cardGap)));
        int maxItems = cols * maxRows;
        for (int i = 0; i < Math.min(items.size(), maxItems); i++) {
            Cosmetic item = items.get(i);
            int col = i % cols;
            int row = i / cols;
            int bx = contentX + col * (cardW + cardGap);
            int by = cardY + row * (cardH + cardGap);
            boolean owned = Config.owns(item.id());
            boolean equipped = item.id().equals(Config.data.equipped.get(cat.name()));
            String prefix = selectedCosmeticId != null && selectedCosmeticId.equals(item.id()) ? "◆ " : "";
            String suffix = equipped ? " ✓" : owned ? " •" : "";
            add(prefix + item.name() + suffix, bx, by, cardW,
                    b -> { selectedCosmeticId = item.id(); status = item.name() + (owned ? " • OWNED" : " • " + item.tier() + " TIER"); rebuildWidgets(); });
        }

        int actionY = height - 92;
        Cosmetic selected = null;
        if (selectedCosmeticId != null) {
            for (Cosmetic item : items) if (item.id().equals(selectedCosmeticId)) selected = item;
        }
        if (selected != null) {
            final Cosmetic item = selected;
            boolean owned = Config.owns(item.id());
            int price = cosmeticPrice(item);
            add(owned ? "EQUIP / UNEQUIP" : "BUY • " + price, contentX, actionY,
                    Math.max(80, (contentW - 8) / 2), b -> {
                        if (!Config.owns(item.id())) {
                            status = Config.buy(item, price) ? "Purchased " + item.name() : "Not enough coins";
                        } else {
                            if (item.id().equals(Config.data.equipped.get(cat.name()))) Config.data.equipped.remove(cat.name());
                            else Config.data.equipped.put(cat.name(), item.id());
                            Config.save();
                            status = "Outfit updated";
                        }
                        rebuildWidgets();
                    });
            add(Config.data.favorites.contains(item.id()) ? "♥ FAVORITE" : "♡ FAVORITE",
                    contentX + Math.max(80, (contentW - 8) / 2) + 8, actionY,
                    Math.max(70, (contentW - 8) / 2), b -> {
                        if (Config.data.favorites.contains(item.id())) Config.data.favorites.remove(item.id());
                        else Config.data.favorites.add(item.id());
                        Config.save();
                        rebuildWidgets();
                    });
        } else {
            gNoop();
        }

        int outfitY = height - 64;
        add("SAVE OUTFIT", contentX, outfitY, Math.max(70, (contentW - 5) / 2), b -> {
            if (Config.data.outfits.size() >= 128) { status = "All 128 outfit slots are full"; return; }
            Config.data.outfits.add(new java.util.HashMap<>(Config.data.equipped));
            Config.data.outfitNames.add("Outfit " + (Config.data.outfits.size()));
            Config.save();
            status = "Outfit saved (" + Config.data.outfits.size() + "/128)";
        });
        add("LOAD OUTFIT 1", contentX + Math.max(70, (contentW - 5) / 2) + 5, outfitY,
                Math.max(70, (contentW - 5) / 2), b -> {
                    if (Config.data.outfits.isEmpty()) { status = "No saved outfits yet"; return; }
                    Config.data.equipped.clear();
                    Config.data.equipped.putAll(Config.data.outfits.get(0));
                    Config.save();
                    status = "Outfit 1 loaded";
                    rebuildWidgets();
                });
    }

    private static int cosmeticPrice(Cosmetic item) {\n        return Math.min(500, 100 + (Math.max(1, item.tier()) - 1) * 125);\n    }\n\n    private void gNoop() { }

    private void initStore(int cx, int y, int buttonW) {
        codeBox = new EditBox(font, cx - buttonW / 2, y + 8, buttonW, 20, Component.literal("Redeem code"));
        codeBox.setMaxLength(32);
        codeBox.setHint(Component.literal("Enter code"));
        addRenderableWidget(codeBox);
        add("REDEEM COINS", cx - buttonW / 2, y + 34, buttonW, b -> {
            status = Config.redeemCoins(codeBox.getValue());
            codeBox.setValue("");
        });
        add("REDEEM UNLOCK CODE", cx - buttonW / 2, y + 62, buttonW, b -> {
            status = Config.redeem(codeBox.getValue());
            codeBox.setValue("");
        });
    }

    @Override
    public void onClose() {
        if (pauseMode) Minecraft.getInstance().setScreen(null);
        else if (parent != null) super.onClose();
        else Minecraft.getInstance().setScreen(new TitleScreen());
    }

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float delta) {
        g.fill(0, 0, width, height, 0xE9000000);
        int panelW = Math.min(560, width - 16);
        int left = (width - panelW) / 2;
        g.fill(left, 25, left + panelW, height - 18, 0xF20A0A0D);
        g.fill(left, 25, left + panelW, 28, RED);
        g.fill(left, height - 21, left + panelW, height - 18, RED);
        g.fill(left, 25, left + 2, height - 18, 0xFF641018);
        g.fill(left + panelW - 2, 25, left + panelW, height - 18, 0xFF641018);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
        super.render(g, mouseX, mouseY, delta);
        g.drawCenteredString(font, Component.literal("LEGEND FPS  •  " + Minecraft.getInstance().getUser().getName()), width / 2, 10, WHITE);
        if (Minecraft.getInstance().player != null && width >= 560 && tab != 1 && tab != 2) {
            int c = width / 2 + Math.min(170, width / 4);
            int bottom = Math.min(height - 58, height / 2 + 100);
            InventoryScreen.renderEntityInInventoryFollowsMouse(g, c - 62, bottom, c + 62, bottom + 4, 68,
                    0.0F, c - mouseX, bottom - mouseY, Minecraft.getInstance().player);
        }
        g.drawCenteredString(font, Component.literal("COINS " + Config.data.coins + "  •  OWNED " + Config.data.owned.size()),
                width / 2, height - 12, GRAY);
    }
}
