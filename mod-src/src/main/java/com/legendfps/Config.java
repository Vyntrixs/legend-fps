package com.legendfps;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Config {
    public static class Data {
        public List<String> owned = new ArrayList<>();
        public List<String> redeemed = new ArrayList<>();
        public List<String> friends = new ArrayList<>();
        public Map<String, String> equipped = new HashMap<>();
        public boolean effects = true;
        public String relayUrl = "";
        public String relayToken = "";
        public String secret = java.util.UUID.randomUUID().toString();
        public String deviceId = java.util.UUID.randomUUID().toString();
    }
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path PATH = FabricLoader.getInstance().getConfigDir().resolve("legendfps.json");
    public static Data data = new Data();

    public static void load() {
        try {
            if (Files.exists(PATH)) {
                try (Reader r = Files.newBufferedReader(PATH)) {
                    Data d = GSON.fromJson(r, Data.class);
                    if (d != null) data = d;
                }
            }
        } catch (Exception ignored) {}
        if (data.owned == null) data.owned = new ArrayList<>();
        if (data.redeemed == null) data.redeemed = new ArrayList<>();
        if (data.friends == null) data.friends = new ArrayList<>();
        if (data.equipped == null) data.equipped = new HashMap<>();
        if (data.relayUrl == null) data.relayUrl = "";
        if (data.relayToken == null) data.relayToken = "";
        if (data.secret == null || data.secret.length() < 16) data.secret = java.util.UUID.randomUUID().toString();
        if (data.deviceId == null || data.deviceId.isBlank()) data.deviceId = java.util.UUID.randomUUID().toString();
        save();
    }
    public static void save() {
        try {
            Files.createDirectories(PATH.getParent());
            try (Writer w = Files.newBufferedWriter(PATH)) { GSON.toJson(data, w); }
        } catch (Exception ignored) {}
    }
    public static boolean owns(String id) { return data.owned.contains(id); }

    public static String redeem(String raw) {
        String code = raw == null ? "" : raw.trim();
        int tier;
        if (code.equals("202609")) tier = 1;
        else if (code.equals("201367")) tier = 2;
        else if (code.equals("200090")) tier = 3;
        else if (code.equals("200167")) tier = 4;
        else return "Invalid code";
        if (data.redeemed.contains(code)) return "Code already redeemed";
        int added = 0;
        for (Cosmetic c : Cosmetic.ALL) {
            if (c.tier() == tier && !data.owned.contains(c.id())) {
                data.owned.add(c.id());
                added++;
            }
        }
        data.redeemed.add(code);
        save();
        return "Unlocked " + added + " cosmetics";
    }
}
