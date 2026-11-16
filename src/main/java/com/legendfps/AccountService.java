package com.legendfps;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.function.Consumer;

public final class AccountService {
    public record Result(boolean ok, String message) {}
    private static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(12)).build();
    private static volatile boolean authenticated;
    private static volatile boolean startupChecked;
    private static volatile boolean needsLogin;
    private static volatile String status = "Not signed in";

    private AccountService() {}

    public static boolean isAuthenticated() { return authenticated; }
    public static String username() { return Config.data.accountUsername == null ? "" : Config.data.accountUsername; }
    public static String status() { return status; }
    public static int savedCount() { return Config.data.savedAccounts == null ? 0 : Config.data.savedAccounts.size(); }

    public static void tick(Minecraft mc) {
        if (!startupChecked) {
            startupChecked = true;
            if (Config.data.sessionToken == null || Config.data.sessionToken.isBlank()) {
                needsLogin = true;
                status = "Sign in to use LEGEND FPS social features";
            } else {
                validateCurrentSession(ok -> {
                    if (!ok) {
                        needsLogin = true;
                        if (mc.screen instanceof TitleScreen || mc.screen == null) mc.setScreen(new AccountScreen(null));
                    } else {
                        needsLogin = false;
                        SocialService.start();
                    }
                });
            }
        }
        if (needsLogin && (mc.screen instanceof TitleScreen || mc.screen == null)) {
            needsLogin = false;
            mc.setScreen(new AccountScreen(null));
        }
    }

    private static String apiBase() {
        String raw = Config.data.relayUrl == null ? "" : Config.data.relayUrl.trim();
        if (raw.isEmpty()) raw = "wss://legend-fps-relay.onrender.com/ws";
        raw = raw.replaceFirst("/ws/?$", "");
        if (raw.startsWith("wss://")) return "https://" + raw.substring(6);
        if (raw.startsWith("https://")) return raw;
        return "";
    }

    private static void request(String path, String method, JsonObject body, String token, Consumer<Result> done) {
        try {
            HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(apiBase() + path))
                    .timeout(Duration.ofSeconds(20))
                    .header("Content-Type", "application/json");
            if (token != null && !token.isBlank()) builder.header("Authorization", "Bearer " + token);
            if ("GET".equals(method)) builder.GET();
            else builder.POST(HttpRequest.BodyPublishers.ofString(body == null ? "{}" : body.toString(), StandardCharsets.UTF_8));
            HTTP.sendAsync(builder.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8))
                    .whenComplete((response, error) -> {
                        Result result;
                        if (error != null) result = new Result(false, "Cannot reach account service");
                        else {
                            try {
                                JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
                                if (response.statusCode() >= 200 && response.statusCode() < 300) {
                                    String username = json.has("username") ? json.get("username").getAsString() : "";
                                    String session = json.has("token") ? json.get("token").getAsString() : "";
                                    if (!username.isBlank() && !session.isBlank()) {
                                        if (!storeSession(username, session)) result = new Result(false, "This device already has 3 saved accounts. Remove one first.");
                                        else {
                                            authenticated = true;
                                            needsLogin = false;
                                            status = "Signed in as " + username;
                                            result = new Result(true, status);
                                        }
                                    } else result = new Result(true, username);
                                } else result = new Result(false, json.has("error") ? json.get("error").getAsString() : "Account request failed");
                            } catch (Exception e) { result = new Result(false, "Invalid response from account service"); }
                        }
                        Result finalResult = result;
                        Minecraft.getInstance().execute(() -> {
                            if (finalResult.ok() && !Config.data.sessionToken.isBlank()) SocialService.start();
                            if (!finalResult.ok()) status = finalResult.message();
                            if (done != null) done.accept(finalResult);
                        });
                    });
        } catch (Exception e) {
            if (done != null) Minecraft.getInstance().execute(() -> done.accept(new Result(false, "Invalid account service URL")));
        }
    }

    private static boolean storeSession(String username, String token) {
        var list = Config.data.savedAccounts;
        int found = -1;
        for (int i = 0; i < list.size(); i++) if (list.get(i).username.equalsIgnoreCase(username)) { found = i; break; }
        if (found < 0 && list.size() >= 3) return false;
        if (found >= 0) list.set(found, new Config.SavedAccount(username, token));
        else list.add(new Config.SavedAccount(username, token));
        Config.data.accountUsername = username;
        Config.data.sessionToken = token;
        Config.save();
        return true;
    }

    public static boolean canAdd(String username) {
        return Config.data.savedAccounts.stream().anyMatch(a -> a.username.equalsIgnoreCase(username))
                || Config.data.savedAccounts.size() < 3;
    }

    public static void login(String username, String password, boolean register, Consumer<Result> done) {
        JsonObject body = new JsonObject();
        body.addProperty("username", username);
        body.addProperty("password", password);
        request(register ? "/api/register" : "/api/login", "POST", body, null, done);
    }

    public static void validateCurrentSession(Consumer<Boolean> done) {
        String token = Config.data.sessionToken == null ? "" : Config.data.sessionToken;
        if (token.isBlank()) { authenticated = false; done.accept(false); return; }
        request("/api/me", "GET", null, token, result -> {
            authenticated = result.ok();
            if (!authenticated) { Config.data.sessionToken = ""; Config.data.accountUsername = ""; Config.save(); }
            done.accept(authenticated);
        });
    }

    public static void switchAccount(String username, Consumer<Result> done) {
        for (Config.SavedAccount account : Config.data.savedAccounts) {
            if (account.username.equalsIgnoreCase(username)) {
                Config.data.accountUsername = account.username;
                Config.data.sessionToken = account.sessionToken;
                Config.save();
                authenticated = false;
                validateCurrentSession(ok -> {
                    if (ok) { status = "Signed in as " + username; SocialService.start(); done.accept(new Result(true, status)); }
                    else done.accept(new Result(false, "Saved session expired. Sign in again."));
                });
                return;
            }
        }
        done.accept(new Result(false, "Saved account not found"));
    }

    private static void revokeSession(String token) {
        if (token == null || token.isBlank()) return;
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(apiBase() + "/api/logout"))
                    .timeout(Duration.ofSeconds(10))
                    .header("Authorization", "Bearer " + token)
                    .POST(HttpRequest.BodyPublishers.ofString("{}"))
                    .build();
            HTTP.sendAsync(request, HttpResponse.BodyHandlers.discarding());
        } catch (Exception ignored) { }
    }

    public static void removeAccount(String username) {
        for (Config.SavedAccount account : Config.data.savedAccounts) {
            if (account.username.equalsIgnoreCase(username)) { revokeSession(account.sessionToken); break; }
        }
        Config.data.savedAccounts.removeIf(a -> a.username.equalsIgnoreCase(username));
        if (Config.data.accountUsername.equalsIgnoreCase(username)) {
            Config.data.accountUsername = "";
            Config.data.sessionToken = "";
            authenticated = false;
            SocialService.disconnect();
        }
        Config.save();
    }

    public static void signOut() {
        Config.data.accountUsername = "";
        Config.data.sessionToken = "";
        authenticated = false;
        SocialService.disconnect();
        Config.save();
    }
}
