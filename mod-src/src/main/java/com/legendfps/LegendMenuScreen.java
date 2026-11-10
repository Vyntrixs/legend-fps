package com.legendfps;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.gui.screens.options.OptionsScreen;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.ArrayList;

public class LegendMenuScreen extends RedScreen {
    private static final String[] TABS = {"HOME", "SOCIAL", "WARDROBE", "SKINS", "EMOTES", "STORE", "SETTINGS"};
    private final boolean pauseMode;
    private int tab = 0;
    private int categoryIndex = 0;
    private String selectedFriend;
    private String selectedCosmeticId;
    private EditBox nameBox;
    private EditBox codeBox;
    private final List<Button> styledButtons = new ArrayList<>();
    private int emotePackIndex = 0;
    private static final String[][] EMOTE_PACKS = {
        {"Wave","Salute","Clap","Point","Cheer","Laugh","Bow","Shrug","Dance 01","Dance 02","Dance 03","Dance 04","Dance 05","Dance 06","Dance 07"},
        {"Legend Dance 01","Legend Dance 02","Legend Dance 03","Legend Dance 04","Legend Dance 05","Legend Dance 06","Legend Dance 07","Legend Dance 08","Legend Dance 09","Legend Dance 10","Legend Dance 11","Legend Dance 12","Legend Dance 13","Legend Dance 14","Legend Dance 15"},
        {"Special Dance 01","Special Dance 02","Special Dance 03","Special Dance 04","Special Dance 05","Special Dance 06","Special Dance 07","Special Dance 08","Special Dance 09","Special Dance 10","Special Dance 11","Special Dance 12","Special Dance 13","Special Dance 14","Special Dance 15"}
    };
    private static final String[] EMOTE_CODES = {"854123","739206","916482"};

    public LegendMenuScreen(Screen parent) { this(parent, false); }

    public LegendMenuScreen(Screen parent, boolean pauseMode) {
        super("LEGEND", parent);
        this.pauseMode = pauseMode;
        this.tab = pauseMode ? 0 : 0;
    }

    @Override
    protected Button add(String label, int x, int y, int w, Button.OnPress press) {
        Button button = super.add(label, x, y, w, press);
        styledButtons.add(button);
        return button;
    }

    @Override
    protected void init() {
        clearWidgets();
        styledButtons.clear();
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
            case 4 -> initEmotes(cx, y, buttonW);
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
            add("PREVIEW ON PLAYER", contentX, actionY - 25, contentW, b -> {
                Config.data.previewed.put(cat.name(), item.id());
                Config.save();
                status = "Previewing " + item.name() + " on your character";
                rebuildWidgets();
            });
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

    private void initEmotes(int cx, int y, int buttonW) {
        int packY = y + 2;
        int packW = Math.max(76, (buttonW - 8) / 3);
        String[] labels = {"DANCES 854123", "LEGENDARY 739206", "SPECIAL 916482"};
        for (int i = 0; i < 3; i++) {
            final int pack = i;
            add((emotePackIndex == i ? "• " : "") + labels[i],
                    cx - (packW * 3 + 8) / 2 + i * (packW + 4), packY, packW,
                    b -> { emotePackIndex = pack; rebuildWidgets(); });
        }
        int gridW = Math.min(500, width - 32);
        int left = (width - gridW) / 2;
        int gap = 5;
        int cols = 3;
        int itemW = (gridW - gap * (cols - 1)) / cols;
        int startY = packY + 31;
        boolean unlocked = Config.data.unlockedEmotePacks.contains(EMOTE_CODES[emotePackIndex]);
        for (int i = 0; i < 15; i++) {
            final String dance = EMOTE_PACKS[emotePackIndex][i];
            int col = i % cols, row = i / cols;
            int bx = left + col * (itemW + gap);
            int by = startY + row * 27;
            boolean selected = dance.equals(Config.data.selectedEmote);
            boolean inWheel = Config.data.emoteWheel.contains(dance);
            String label = (selected ? "◆ " : "") + dance + (inWheel ? "  ✓" : "");
            add(unlocked ? label : "LOCKED • " + dance, bx, by, itemW, b -> {
                if (!Config.data.unlockedEmotePacks.contains(EMOTE_CODES[emotePackIndex])) {
                    status = "Redeem pack code " + EMOTE_CODES[emotePackIndex] + " in STORE";
                    return;
                }
                Config.data.selectedEmote = dance;
                Config.save();
                status = "Selected " + dance + " • animation integration pending";
                rebuildWidgets();
            });
        }
        int actionY = height - 72;
        String chosen = Config.data.selectedEmote;
        add("ADD / REMOVE SELECTED FROM EMOTE WHEEL", cx - buttonW / 2, actionY, buttonW, b -> {
            if (chosen == null || chosen.isBlank()) { status = "Select a dance first"; return; }
            if (Config.data.emoteWheel.contains(chosen)) Config.data.emoteWheel.remove(chosen);
            else if (Config.data.emoteWheel.size() < 8) Config.data.emoteWheel.add(chosen);
            else { status = "Emote wheel is full (8 slots)"; return; }
            Config.save();
            status = "Emote wheel updated";
            rebuildWidgets();
        });
        gStatusHint();
    }

    private void gStatusHint() {
        // The selected dance is stored separately from its wheel assignment.
    }

    private static int cosmeticPrice(Cosmetic item) {
        return Math.min(500, 100 + (Math.max(1, item.tier()) - 1) * 125);
    }

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

    public void openEmotes() {
        this.tab = 4;
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
        // Re-skin every clickable menu button with the LEGEND black/red theme.
        for (Button button : styledButtons) {
            int bx = button.getX();
            int by = button.getY();
            int bw = button.getWidth();
            int bh = button.getHeight();
            boolean hovered = mouseX >= bx && mouseX < bx + bw && mouseY >= by && mouseY < by + bh;
            int edge = hovered ? 0xFFFF343D : 0xFF8F1721;
            int fill = hovered ? 0xFF3A0D13 : 0xFF111116;
            g.fill(bx, by, bx + bw, by + bh, edge);
            g.fill(bx + 1, by + 1, bx + bw - 1, by + bh - 1, fill);
            g.fill(bx + 2, by + 2, bx + bw - 2, by + 4, hovered ? 0xFFFF4A52 : 0xFFB51D2A);
            int textColor = button.active ? (hovered ? 0xFFFFFFFF : 0xFFF2E8E9) : 0xFF777777;
            g.drawCenteredString(font, button.getMessage(), bx + bw / 2, by + (bh - 8) / 2 + 1, textColor);
        }
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
