package com.legendfps;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.regex.Pattern;

/**
 * Friends, friend requests, relay settings and world invites.
 * Everything shown comes from the relay (SocialService); the screen never opens the vanilla LAN screen.
 */
public class FriendsScreen extends RedScreen {
    private static final Pattern USERNAME = Pattern.compile("[A-Za-z0-9_]{3,16}");
    private static final int TAB_FRIENDS = 0, TAB_PLAYERS = 1, TAB_REQUESTS = 2, TAB_RELAY = 3;

    private int tab = TAB_FRIENDS;
    private int scroll;
    private int lastVersion = -1;
    private String selectedFriend;
    private EditBox usernameBox;
    private EditBox relayBox;
    private final boolean inviteMode;

    public FriendsScreen(Screen parent) { this(parent, false); }

    public FriendsScreen(Screen parent, boolean inviteMode) {
        super(inviteMode ? "Invite to World" : "Friends", parent);
        this.inviteMode = inviteMode;
    }

    @Override
    protected void init() {
        lastVersion = SocialService.version();
        int center = this.width / 2;
        int panel = Math.min(300, this.width - 24);
        int x = center - panel / 2;

        // tabs
        int tw = (panel - 12) / 4;
        int reqCount = SocialService.incoming().size();
        add((tab == TAB_FRIENDS ? "» " : "") + "Friends", x, 40, tw, b -> switchTab(TAB_FRIENDS));
        add((tab == TAB_PLAYERS ? "» " : "") + "Players", x + tw + 4, 40, tw, b -> {
            SocialService.refreshPublicPlayers();
            switchTab(TAB_PLAYERS);
        });
        add((tab == TAB_REQUESTS ? "» " : "") + "Requests" + (reqCount > 0 ? " (" + reqCount + ")" : ""),
                x + 2 * (tw + 4), 40, tw, b -> switchTab(TAB_REQUESTS));
        add((tab == TAB_RELAY ? "» " : "") + "Relay", x + 3 * (tw + 4), 40, tw, b -> switchTab(TAB_RELAY));

        switch (tab) {
            case TAB_FRIENDS -> initFriends(x, panel);
            case TAB_PLAYERS -> initPlayers(x, panel);
            case TAB_REQUESTS -> initRequests(x, panel);
            default -> initRelay(x, panel);
        }
        add("Back", center - 50, this.height - 54, 100, b -> onClose());
    }

    private void switchTab(int t) { tab = t; scroll = 0; rebuildWidgets(); }

    private int maxRows() { return Math.max(1, (this.height - 190) / 22); }

    private void initFriends(int x, int panel) {
        int y = 66;
        usernameBox = new EditBox(this.font, x, y, panel - 78, 20, Component.literal("LEGEND FPS username"));
        usernameBox.setMaxLength(16);
        usernameBox.setHint(Component.literal("Account username"));
        addRenderableWidget(usernameBox);
        add("Add", x + panel - 74, y, 74, b -> {
            String name = usernameBox.getValue().trim();
            if (!USERNAME.matcher(name).matches()) { status = "Enter a valid LEGEND FPS username"; return; }
            if (SocialService.sendFriendRequest(name)) status = "Sending request…";
            else status = SocialService.status();
            usernameBox.setValue("");
        });

        List<SocialService.Friend> friends = SocialService.friends();
        int rows = maxRows();
        scroll = Math.max(0, Math.min(scroll, Math.max(0, friends.size() - rows)));
        int rowY = y + 26;
        for (int i = scroll; i < Math.min(friends.size(), scroll + rows); i++) {
            SocialService.Friend f = friends.get(i);
            Component label = Component.literal(f.online() ? "● " : "○ ")
                    .withStyle(f.online() ? ChatFormatting.GREEN : ChatFormatting.GRAY)
                    .append(Component.literal((f.name().equals(selectedFriend) ? "» " : "") + f.name())
                            .withStyle(ChatFormatting.WHITE));
            addRenderableWidget(net.minecraft.client.gui.components.Button.builder(label, b -> {
                selectedFriend = f.name();
                status = "Selected: " + f.name();
                rebuildWidgets();
            }).bounds(x + 20, rowY, panel - 40, 20).build());
            rowY += 22;
        }
        if (friends.size() > rows) {
            add("▲", x, y + 26, 18, b -> { scroll = Math.max(0, scroll - 1); rebuildWidgets(); });
            add("▼", x, y + 26 + 22, 18, b -> { scroll++; rebuildWidgets(); });
        }
        int actionY = this.height - 78;
        add(inviteMode ? "Send World Invite" : "Invite to World", x, actionY, panel / 2 + 20, b -> inviteSelected());
        add("Remove", x + panel / 2 + 24, actionY, panel / 2 - 24, b -> {
            if (selectedFriend == null) { status = "Select a friend first"; return; }
            SocialService.removeFriend(selectedFriend);
            status = "Removed " + selectedFriend;
            selectedFriend = null;
        });
    }

    private void initPlayers(int x, int panel) {
        int y = 68;
        List<String> players = SocialService.publicPlayers();
        int rows = maxRows();
        scroll = Math.max(0, Math.min(scroll, Math.max(0, players.size() - rows)));
        int shown = 0;
        for (int i = scroll; i < Math.min(players.size(), scroll + rows); i++) {
            String name = players.get(i);
            if (name.equalsIgnoreCase(AccountService.username())) continue;
            final String playerName = name;
            add(playerName, x, y, panel - 78, b -> { selectedFriend = playerName; status = "Selected: " + playerName; });
            add("Add", x + panel - 74, y, 74, b -> {
                if (SocialService.sendFriendRequest(playerName)) status = "Request sent to " + playerName;
                else status = SocialService.status();
            });
            y += 22;
            shown++;
        }
        if (players.size() > rows) {
            add("▲", x, 68 + rows * 22, 28, b -> { scroll = Math.max(0, scroll - 1); rebuildWidgets(); });
            add("▼", x + 32, 68 + rows * 22, 28, b -> { scroll++; rebuildWidgets(); });
        }
        add("Refresh", x, this.height - 78, panel, b -> {
            SocialService.refreshPublicPlayers();
            status = "Refreshing player list…";
        });
    }

    private void initRequests(int x, int panel) {
        int y = 68;
        int rows = maxRows();
        int shown = 0;
        for (String from : SocialService.incoming()) {
            if (shown++ >= rows) break;
            final String name = from;
            add("Accept: " + name, x, y, panel - 108, b -> { SocialService.acceptRequest(name); status = "Accepting " + name + "…"; });
            add("Decline", x + panel - 104, y, 104, b -> { SocialService.declineRequest(name); status = "Declined " + name; });
            y += 22;
        }
        for (String to : SocialService.outgoing()) {
            if (shown++ >= rows) break;
            final String name = to;
            add("Pending: " + name + "  (cancel)", x, y, panel, b -> { SocialService.cancelRequest(name); status = "Cancelled request to " + name; });
            y += 22;
        }
    }

    private void initRelay(int x, int panel) {
        int y = 74;
        relayBox = new EditBox(this.font, x, y, panel, 20, Component.literal("Relay URL"));
        relayBox.setMaxLength(200);
        relayBox.setHint(Component.literal("wss://legend-fps-relay.onrender.com/ws"));
        relayBox.setValue(Config.data.relayUrl == null ? "" : Config.data.relayUrl);
        addRenderableWidget(relayBox);
        y += 30;
        add("Save & Connect", x, y, panel, b -> {
            String value = relayBox.getValue().trim();
            if (!value.startsWith("wss://") && !value.startsWith("ws://")) {
                status = "Relay URL must start with wss://";
                return;
            }
            Config.data.relayUrl = value;
            Config.save();
            SocialService.start();
            status = "Connecting…";
        });
        y += 25;
        add("Account Manager", x, y, panel, b -> minecraft.setScreen(new AccountManagerScreen(this)));
        y += 25;
        add("Sign in / Register", x, y, panel, b -> minecraft.setScreen(new AccountScreen(this)));
    }

    private void inviteSelected() {
        if (selectedFriend == null) { status = "Select a friend first"; return; }
        Minecraft client = Minecraft.getInstance();

        // Friend identities are LEGEND FPS account names, not necessarily Minecraft usernames.
        if (client.getSingleplayerServer() != null) {
            WorldInviteService.inviteFriend(selectedFriend);
            status = WorldInviteService.status();
            return;
        }
        status = "Open a singleplayer world to invite a friend";
    }

    @Override
    public void tick() {
        super.tick();
        if (SocialService.version() != lastVersion && !(getFocused() instanceof EditBox)) {
            rebuildWidgets();
        }
        String ws = WorldInviteService.status();
        if (!ws.isEmpty() && !ws.equals(status) && tab == TAB_FRIENDS) status = ws;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
        if (tab == TAB_FRIENDS && SocialService.friends().size() > maxRows()) {
            scroll = Math.max(0, scroll - (int) Math.signum(vertical));
            rebuildWidgets();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontal, vertical);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
        super.render(g, mouseX, mouseY, delta);
        String relay = "Relay: " + SocialService.status();
        g.drawCenteredString(this.font, Component.literal(relay), this.width / 2, 27, SocialService.isConnected() ? 0xFF55FF55 : GRAY);
        if (tab == TAB_FRIENDS && SocialService.friends().isEmpty()) {
            g.drawCenteredString(this.font, Component.literal(SocialService.isConnected()
                    ? "No friends yet - add a player by name" : "Set up the Relay tab to use friends"),
                    this.width / 2, 106, GRAY);
        }
        if (tab == TAB_PLAYERS && SocialService.publicPlayers().isEmpty()) {
            g.drawCenteredString(this.font, Component.literal(SocialService.isConnected()
                    ? "No other LEGEND FPS players online" : "Connect to Relay to see public players"),
                    this.width / 2, 88, GRAY);
        }
        if (tab == TAB_REQUESTS && SocialService.incoming().isEmpty() && SocialService.outgoing().isEmpty()) {
            g.drawCenteredString(this.font, Component.literal("No pending requests"), this.width / 2, 80, GRAY);
        }
        if (!WorldInviteService.publicAddress().isBlank()) {
            g.drawCenteredString(this.font, Component.literal("World: " + WorldInviteService.publicAddress()),
                    this.width / 2, this.height - 66, GRAY);
        }
    }
}
