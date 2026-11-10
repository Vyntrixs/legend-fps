package com.legendfps;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class Config {
    public static class Data {
        public List<String> owned = new ArrayList<>();
        public List<String> redeemed = new ArrayList<>();
        public List<String> friends = new ArrayList<>();
        public List<String> favorites = new ArrayList<>();
        public List<String> outfitNames = new ArrayList<>();
        public List<Map<String, String>> outfits = new ArrayList<>();
        public Map<String, String> equipped = new HashMap<>();
        public int coins = 0;
        public boolean effects = true;
        public String relayUrl = "";
        public String relayToken = "";
        public String secret = java.util.UUID.randomUUID().toString();
        public String deviceId = java.util.UUID.randomUUID().toString();
    }
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path PATH = FabricLoader.getInstance().getConfigDir().resolve("legendfps.json");
    public static Data data = new Data();

    private static final Map<String, Integer> COIN_CODES = Map.of(
        "EDFRTG", 1000, "LGC2026", 2000, "REDVIP", 3000, "VYN777", 4000, "DRAGONX", 5000
    );
    private static final Map<String, Cosmetic.Category> CATEGORY_CODES = Map.ofEntries(
        Map.entry("H7K-X92-PQ4", Cosmetic.Category.HAT),
        Map.entry("HR8-M3Z-K21", Cosmetic.Category.HAIR),
        Map.entry("FC9-W2L-7XD", Cosmetic.Category.FACE),
        Map.entry("ER4-T8Q-5MN", Cosmetic.Category.EARS),
        Map.entry("SH6-P9V-3KA", Cosmetic.Category.SHOULDERS),
        Map.entry("GL2-X7R-8JW", Cosmetic.Category.ARMS),
        Map.entry("BK5-N4Y-9TZ", Cosmetic.Category.BACK),
        Map.entry("TP8-Q3M-6VX", Cosmetic.Category.TOPS),
        Map.entry("PT4-K9W-2RA", Cosmetic.Category.PANTS),
        Map.entry("OT7-Z5H-3NP", Cosmetic.Category.OUTFITS),
        Map.entry("SH9-L2X-8KD", Cosmetic.Category.SHOES),
        Map.entry("OW3-V6J-4QM", Cosmetic.Category.OUTERWEAR),
        Map.entry("WN8-R4K-2YP", Cosmetic.Category.WING),
        Map.entry("CP6-X9M-5QJ", Cosmetic.Category.CAPE),
        Map.entry("PT7-H3V-8LA", Cosmetic.Category.PETS)
    );

    public static void load() {
        try {
            if (Files.exists(PATH)) try (Reader r = Files.newBufferedReader(PATH)) {
                Data d = GSON.fromJson(r, Data.class);
                if (d != null) data = d;
            }
        } catch (Exception ignored) {}
        if (data.owned == null) data.owned = new ArrayList<>();
        if (data.redeemed == null) data.redeemed = new ArrayList<>();
        if (data.friends == null) data.friends = new ArrayList<>();
        if (data.favorites == null) data.favorites = new ArrayList<>();
        if (data.outfitNames == null) data.outfitNames = new ArrayList<>();
        if (data.outfits == null) data.outfits = new ArrayList<>();
        if (data.equipped == null) data.equipped = new HashMap<>();
        if (data.coins < 0) data.coins = 0;
        if (data.relayUrl == null) data.relayUrl = "";
        if (data.relayToken == null) data.relayToken = "";
        if (data.secret == null || data.secret.length() < 16) data.secret = UUID.randomUUID().toString();
        if (data.deviceId == null || data.deviceId.isBlank()) data.deviceId = UUID.randomUUID().toString();
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
        String code = raw == null ? "" : raw.trim().toUpperCase(Locale.ROOT);
        if (data.redeemed.contains(code)) return "Code already redeemed";
        int tier = switch (code) {
            case "202609" -> 1;
            case "201367" -> 2;
            case "200090" -> 3;
            case "200167" -> 4;
            default -> -1;
        };
        if (tier >= 0) {
            int added = 0;
            for (Cosmetic c : Cosmetic.ALL) if (c.tier() == tier && !data.owned.contains(c.id())) {
                data.owned.add(c.id()); added++;
            }
            data.redeemed.add(code); save();
            return "Unlocked " + added + " cosmetics";
        }
        if (code.equals("FX2-N6W-9ZR")) {
            int added = 0;
            for (Cosmetic.Category category : new Cosmetic.Category[]{
                    Cosmetic.Category.AURA, Cosmetic.Category.TRAIL, Cosmetic.Category.HAND_TRAIL}) {
                for (Cosmetic c : Cosmetic.in(category)) if (!data.owned.contains(c.id())) {
                    data.owned.add(c.id()); added++;
                }
            }
            data.redeemed.add(code); save();
            return "Unlocked " + added + " particle and hand-trail cosmetics";
        }
        Cosmetic.Category category = CATEGORY_CODES.get(code);
        if (category != null) {
            int added = 0;
            for (Cosmetic c : Cosmetic.in(category)) if (!data.owned.contains(c.id())) {
                data.owned.add(c.id()); added++;
            }
            data.redeemed.add(code); save();
            return "Unlocked " + added + " " + category.label;
        }
        if (code.equals("854123") || code.equals("739206") || code.equals("916482")) {
            return "Emote pack code reserved - emote animations are not implemented yet";
        }
        if (code.equals("X9Q-V7K-2MZ-84R")) {
            return "Owner code is locked until secure owner-account verification is configured";
        }
        return "Invalid code";
    }

    public static String redeemCoins(String raw) {
        String code = raw == null ? "" : raw.trim().toUpperCase(Locale.ROOT);
        Integer amount = COIN_CODES.get(code);
        if (amount == null) return "Invalid coin code";
        if (data.redeemed.contains(code)) return "Code already redeemed";
        data.coins += amount;
        data.redeemed.add(code);
        save();
        return "+" + amount + " Coins  |  Balance: " + data.coins;
    }

    public static boolean buy(Cosmetic item, int price) {
        if (item == null || owns(item.id()) || price < 0 || data.coins < price) return false;
        data.coins -= price;
        data.owned.add(item.id());
        save();
        return true;
    }
}
