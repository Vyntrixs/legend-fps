package com.legendfps;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.world.level.GameType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.ServerSocket;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Hosting side of world invites. Publishes the integrated server (which triggers e4mc),
 * captures the real e4mc hostname that e4mc announces in chat, and sends it to friends.
 * e4mc exposes no public API, so the announcement message is the supported address source.
 */
public final class WorldInviteService {
    private static final Logger LOGGER = LoggerFactory.getLogger("LEGEND FPS");
    private static final Pattern E4MC_HOST = Pattern.compile(
            "(?i)\\b([a-z0-9-]+(?:\\.[a-z0-9-]+)*\\.e4mc\\.link(?::\\d{1,5})?)\\b");
    private static final long WAIT_LIMIT_MS = 30_000;

    private static volatile String publicAddress = "";
    private static volatile String status = "";
    private static final Set<String> QUEUED = ConcurrentHashMap.newKeySet();
    private static volatile long waitingSince;

    private WorldInviteService() {}

    public static boolean e4mcPresent() {
        return FabricLoader.getInstance().isModLoaded("e4mc");
    }

    /** Host pressed "Invite to World" for this friend. Never opens the Open-to-LAN screen. */
    public static void inviteFriend(String friend) {
        Minecraft mc = Minecraft.getInstance();
        IntegratedServer server = mc.getSingleplayerServer();
        if (server == null || mc.player == null) { status = "Open your singleplayer world first"; return; }
        if (!e4mcPresent()) { status = "e4mc is not loaded - cannot create a public address"; return; }
        if (!SocialService.isConnected()) { status = "Not connected to the relay"; return; }
        if (!SocialService.isFriend(friend)) { status = friend + " is not your friend yet"; return; }

        if (!server.isPublished()) {
            if (!publish(mc, server)) return;
        }
        if (!publicAddress.isBlank()) {
            SocialService.sendInvite(friend, publicAddress);
            status = SocialService.status();
        } else {
            QUEUED.add(friend);
            waitingSince = System.currentTimeMillis();
            status = "Waiting for the e4mc address…";
        }
    }

    private static boolean publish(Minecraft mc, IntegratedServer server) {
        try {
            int port;
            try (ServerSocket probe = new ServerSocket(0)) { port = probe.getLocalPort(); }
            GameType mode = server.getDefaultGameType();
            boolean ok = server.publishServer(mode, false, port);
            if (!ok) status = "Minecraft could not open the world to other players";
            return ok;
        } catch (Exception e) {
            status = "Could not open the world: " + e.getClass().getSimpleName();
            LOGGER.error("Could not publish the singleplayer world for e4mc", e);
            return false;
        }
    }

    /** Fabric GAME message hook: looks for the hostname e4mc prints when the domain is assigned. */
    public static void observeGameMessage(Component message) {
        if (message == null) return;
        String found = null;
        Matcher m = E4MC_HOST.matcher(message.getString());
        if (m.find()) found = m.group(1);
        if (found == null) {
            // e4mc "hide domain in chat" mode: the real domain is in the click-to-copy event.
            final String[] box = {null};
            message.visit((style, text) -> {
                ClickEvent ce = style.getClickEvent();
                if (ce instanceof ClickEvent.CopyToClipboard copy) {
                    Matcher c = E4MC_HOST.matcher(copy.value());
                    if (c.find()) box[0] = c.group(1);
                }
                return Optional.empty();
            }, Style.EMPTY);
            found = box[0];
        }
        if (found != null) onAddress(found);
    }

    private static void onAddress(String address) {
        publicAddress = address;
        LOGGER.info("Captured e4mc public address for world invite");
        for (String friend : QUEUED) {
            SocialService.sendInvite(friend, address);
        }
        if (!QUEUED.isEmpty()) status = "Invite sent";
        QUEUED.clear();
    }

    /**
     * Read e4mc's own session state when its address does not arrive.
     * Reflection is intentionally diagnostic-only; no e4mc internals are modified.
     */
    private static String e4mcSessionDiagnostics() {
        try {
            Class<?> e4mc = Class.forName("link.e4mc.E4mcClient");
            Object session = e4mc.getField("session").get(null);
            if (session == null) return "e4mc session was not created";

            Object state = session.getClass().getField("state").get(session);
            Object failure = session.getClass().getField("failureCause").get(session);
            if (failure instanceof Throwable cause) {
                LOGGER.error("e4mc tunnel entered a failed state", cause);
                return "e4mc " + state + " (" + cause.getClass().getSimpleName() + ": " + describeFailure(cause) + ")";
            }
            return "e4mc state: " + state;
        } catch (ClassNotFoundException e) {
            return "e4mc diagnostic class unavailable";
        } catch (ReflectiveOperationException | LinkageError e) {
            LOGGER.warn("Could not read e4mc session diagnostics", e);
            return "e4mc diagnostics unavailable";
        }
    }

    /** Keeps the actionable native-loader cause visible in the in-game status and latest.log. */
    private static String describeFailure(Throwable failure) {
        Throwable root = failure;
        for (int i = 0; i < 5 && root.getCause() != null && root.getCause() != root; i++) {
            root = root.getCause();
        }
        String message = root.getMessage();
        if (message == null || message.isBlank()) message = failure.getMessage();
        if (message == null || message.isBlank()) return "no native-loader detail; inspect latest.log";
        message = message.replaceAll("[\\r\\n\\t]+", " ").trim();
        return message.length() > 180 ? message.substring(0, 177) + "..." : message;
    }

    /** Client tick: reports when e4mc never produced an address. */
    public static void tick(Minecraft mc) {
        if (!QUEUED.isEmpty() && System.currentTimeMillis() - waitingSince > WAIT_LIMIT_MS) {
            String diagnostics = e4mcSessionDiagnostics();
            LOGGER.warn("No e4mc public address received within 30 seconds: {}", diagnostics);
            QUEUED.clear();
            status = "e4mc timeout: " + diagnostics + " - automatic diagnostics saved to latest.log";
        }
    }

    /** World closed: the old e4mc address is no longer valid. */
    public static void reset() {
        publicAddress = "";
        QUEUED.clear();
        waitingSince = 0L;
    }

    /** Guest accepted an invite: leave any current world, then connect to the host's e4mc address. */
    public static void join(String address) {
        Minecraft mc = Minecraft.getInstance();
        mc.execute(() -> {
            try {
                McCompat.leaveCurrentWorld(mc);
                ServerAddress target = ServerAddress.parseString(address);
                ServerData data = new ServerData("LEGEND FPS Invite", address, ServerData.Type.OTHER);
                ConnectScreen.startConnecting(new TitleScreen(), mc, target, data, false, null);
                status = "Joining " + address + "…";
            } catch (Exception e) {
                status = "Auto-join failed for " + address + " (" + e.getClass().getSimpleName() + ")";
            }
        });
    }

    public static String status() { return status; }
    public static String publicAddress() { return publicAddress; }
}
