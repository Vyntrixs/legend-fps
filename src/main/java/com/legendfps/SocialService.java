package com.legendfps;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.TitleScreen;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

/**
 * Connection to the LEGEND relay: friends, friend requests, presence and world invites.
 * All state here comes from the relay; nothing is simulated.
 */
public final class SocialService {
    public record Friend(String name, boolean online) {}
    public record Invite(String id, String from, String address) {}

    private static final Pattern NAME = Pattern.compile("[A-Za-z0-9_]{1,16}");
    private static final Pattern E4MC_ADDR = Pattern.compile("[A-Za-z0-9-]+(?:\\.[A-Za-z0-9-]+)*\\.e4mc\\.link(?::[0-9]{1,5})?");
    private static final HttpClient HTTP = HttpClient.newHttpClient();
    private static final ScheduledExecutorService TIMER = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "legendfps-relay");
        t.setDaemon(true);
        return t;
    });
    private static final ConcurrentLinkedQueue<Invite> PENDING = new ConcurrentLinkedQueue<>();

    private static volatile WebSocket socket;
    private static volatile boolean registered;
    private static volatile boolean connecting;
    private static volatile boolean started;
    private static volatile String status = "Relay not configured";
    private static volatile int version;

    private static volatile List<Friend> friends = List.of();
    private static volatile List<String> incoming = List.of();
    private static volatile List<String> outgoing = List.of();

    private SocialService() {}

    // ---- lifecycle -------------------------------------------------------------------------

    /** Starts (or restarts) the auto-reconnecting relay connection using the saved config. */
    public static synchronized void start() {
        if (!started) {
            started = true;
            TIMER.scheduleWithFixedDelay(SocialService::tick, 5, 10, TimeUnit.SECONDS);
            TIMER.scheduleWithFixedDelay(SocialService::ping, 25, 25, TimeUnit.SECONDS);
        }
        disconnect();
        connect();
    }

    public static synchronized void disconnect() {
        WebSocket ws = socket;
        socket = null;
        registered = false;
        if (ws != null) {
            try { ws.sendClose(WebSocket.NORMAL_CLOSURE, "bye"); } catch (Exception ignored) { }
        }
    }

    private static void tick() {
        try {
            if (socket == null && !connecting) connect();
        } catch (Exception ignored) { }
    }

    private static void ping() {
        WebSocket ws = socket;
        if (ws != null && registered) sendRaw("{\"type\":\"ping\"}");
    }

    private static synchronized void connect() {
        String raw = Config.data.relayUrl == null ? "" : Config.data.relayUrl.trim();
        if (raw.isEmpty()) { setStatus("Relay not configured"); return; }
        if (socket != null || connecting) return;
        final URI uri;
        try {
            uri = URI.create(raw);
            if (uri.getScheme() == null || !(uri.getScheme().equals("ws") || uri.getScheme().equals("wss"))) {
                setStatus("Relay URL must start with wss:// (or ws://)");
                return;
            }
        } catch (Exception e) {
            setStatus("Invalid relay URL");
            return;
        }
        connecting = true;
        setStatus("Connecting…");
        HTTP.newWebSocketBuilder().buildAsync(uri, new WebSocket.Listener() {
            private final StringBuilder fragments = new StringBuilder();

            @Override public void onOpen(WebSocket ws) {
                socket = ws;
                connecting = false;
                JsonObject hello = new JsonObject();
                hello.addProperty("type", "register");
                hello.addProperty("name", Minecraft.getInstance().getUser().getName());
                hello.addProperty("secret", Config.data.secret);
                hello.addProperty("token", Config.data.relayToken == null ? "" : Config.data.relayToken);
                JsonArray claimed = new JsonArray();
                for (String n : Config.data.friends) claimed.add(n);
                hello.add("friends", claimed);
                ws.sendText(hello.toString(), true);
                ws.request(1);
            }

            @Override public CompletionStage<?> onText(WebSocket ws, CharSequence data, boolean last) {
                fragments.append(data);
                if (last) {
                    String packet = fragments.toString();
                    fragments.setLength(0);
                    handle(packet);
                }
                ws.request(1);
                return null;
            }

            @Override public void onError(WebSocket ws, Throwable error) {
                dropped(ws, "Connection error: " + error.getClass().getSimpleName());
            }

            @Override public CompletionStage<?> onClose(WebSocket ws, int code, String reason) {
                dropped(ws, reason == null || reason.isEmpty() ? "Disconnected" : "Disconnected: " + reason);
                return null;
            }
        }).whenComplete((ws, err) -> {
            if (err != null) {
                connecting = false;
                socket = null;
                setStatus("Cannot reach relay (" + (err.getCause() != null ? err.getCause().getClass().getSimpleName() : err.getClass().getSimpleName()) + ")");
            }
        });
    }

    private static void dropped(WebSocket ws, String why) {
        if (socket == ws || socket == null) {
            socket = null;
            registered = false;
            connecting = false;
            friends = List.of();
            incoming = List.of();
            outgoing = List.of();
            setStatus(why);
        }
    }

    // ---- inbound ---------------------------------------------------------------------------

    private static void handle(String raw) {
        try {
            JsonObject p = JsonParser.parseString(raw).getAsJsonObject();
            String type = p.has("type") ? p.get("type").getAsString() : "";
            switch (type) {
                case "registered" -> { registered = true; setStatus("Connected as " + p.get("name").getAsString()); }
                case "state" -> {
                    List<Friend> f = new ArrayList<>();
                    for (JsonElement e : p.getAsJsonArray("friends")) {
                        JsonObject o = e.getAsJsonObject();
                        f.add(new Friend(o.get("name").getAsString(), o.get("online").getAsBoolean()));
                    }
                    friends = List.copyOf(f);
                    boolean changed = false;
                    for (Friend fr : f) {
                        if (Config.data.friends.stream().noneMatch(n -> n.equalsIgnoreCase(fr.name()))) { Config.data.friends.add(fr.name()); changed = true; }
                    }
                    if (changed) Config.save();
                    incoming = names(p.getAsJsonArray("incoming"));
                    outgoing = names(p.getAsJsonArray("outgoing"));
                    version++;
                }
                case "info" -> setStatus(p.get("message").getAsString());
                case "error" -> setStatus(p.has("message") ? p.get("message").getAsString() : "Relay error");
                case "invite_sent" -> setStatus("Invite sent to " + p.get("to").getAsString());
                case "invite_result" -> setStatus(p.get("from").getAsString() + (p.get("accepted").getAsBoolean() ? " accepted your invite" : " declined your invite"));
                case "invite" -> {
                    String address = p.get("address").getAsString().trim();
                    String from = p.get("from").getAsString();
                    String id = p.get("id").getAsString();
                    if (NAME.matcher(from).matches() && E4MC_ADDR.matcher(address).matches() && address.length() <= 255) {
                        PENDING.add(new Invite(id, from, address));
                    }
                }
                default -> { }
            }
        } catch (Exception ignored) { }
    }

    private static List<String> names(JsonArray arr) {
        List<String> out = new ArrayList<>();
        for (JsonElement e : arr) out.add(e.getAsString());
        return List.copyOf(out);
    }

    /** Called on the client thread each tick: shows the Accept/Decline window for the next invite. */
    public static void pumpInvites(Minecraft mc) {
        if (PENDING.isEmpty()) return;
        var screen = mc.screen;
        boolean safe = screen == null || screen instanceof TitleScreen || screen instanceof PauseScreen || screen instanceof RedScreen;
        if (!safe || screen instanceof InviteDialogScreen) return;
        Invite next = PENDING.poll();
        if (next != null) mc.setScreen(new InviteDialogScreen(screen, next));
    }

    // ---- outbound --------------------------------------------------------------------------

    private static boolean sendRaw(String json) {
        WebSocket ws = socket;
        if (ws == null || !registered) { setStatus("Not connected to the relay"); return false; }
        ws.sendText(json, true);
        return true;
    }

    private static boolean sendWithName(String type, String field, String name) {
        if (!NAME.matcher(name == null ? "" : name).matches()) { setStatus("Invalid player name"); return false; }
        JsonObject o = new JsonObject();
        o.addProperty("type", type);
        o.addProperty(field, name);
        return sendRaw(o.toString());
    }

    public static boolean sendFriendRequest(String name) { return sendWithName("friend_request", "to", name); }
    public static boolean acceptRequest(String name) { return sendWithName("friend_accept", "from", name); }
    public static boolean declineRequest(String name) { return sendWithName("friend_decline", "from", name); }
    public static boolean cancelRequest(String name) { return sendWithName("friend_cancel", "to", name); }
    public static boolean removeFriend(String name) {
        Config.data.friends.removeIf(n -> n.equalsIgnoreCase(name));
        Config.save();
        return sendWithName("friend_remove", "name", name);
    }

    public static boolean sendInvite(String friend, String address) {
        if (!NAME.matcher(friend == null ? "" : friend).matches()) { setStatus("Invalid player name"); return false; }
        JsonObject o = new JsonObject();
        o.addProperty("type", "invite");
        o.addProperty("to", friend);
        o.addProperty("address", address);
        return sendRaw(o.toString());
    }

    public static void respondToInvite(String id, boolean accepted) {
        JsonObject o = new JsonObject();
        o.addProperty("type", "invite_response");
        o.addProperty("id", id);
        o.addProperty("accepted", accepted);
        sendRaw(o.toString());
    }

    // ---- accessors -------------------------------------------------------------------------

    public static boolean isConnected() { return socket != null && registered; }
    public static String status() { return status; }
    public static int version() { return version; }
    public static List<Friend> friends() { return friends; }
    public static List<String> incoming() { return incoming; }
    public static List<String> outgoing() { return outgoing; }
    public static boolean isFriend(String name) {
        return friends.stream().anyMatch(f -> f.name().toLowerCase(Locale.ROOT).equals(name.toLowerCase(Locale.ROOT)));
    }

    private static void setStatus(String s) { status = s; version++; }
}
