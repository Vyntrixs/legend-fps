package com.legendfps;

import net.minecraft.client.gui.screens.Screen;
import java.util.List;

public class CosmeticsScreen extends RedScreen {
    private int categoryIndex = 0;
    private int page = 0;
    public CosmeticsScreen(Screen parent) { super("LEGEND WARDROBE", parent); }

    @Override protected void init() {
        Cosmetic.Category[] cats = Cosmetic.Category.values();
        Cosmetic.Category cat = cats[categoryIndex];
        int cx = this.width / 2;
        add("◀", cx - 112, 34, 38, b -> { categoryIndex = (categoryIndex + cats.length - 1) % cats.length; page=0; rebuildWidgets(); });
        add(cat.label + " (" + (categoryIndex + 1) + "/" + cats.length + ")", cx - 70, 34, 140, b -> {});
        add("▶", cx + 74, 34, 38, b -> { categoryIndex = (categoryIndex + 1) % cats.length; page=0; rebuildWidgets(); });

        List<Cosmetic> items = Cosmetic.in(cat);
        int perPage = Math.max(3, Math.min(8, (this.height - 126) / 22));
        int pages = Math.max(1, (items.size() + perPage - 1) / perPage);
        page = Math.max(0, Math.min(page, pages - 1));
        int x = cx - Math.min(140, this.width / 2 - 12);
        int width = Math.min(280, this.width - 24);
        int start = page * perPage;
        for (int i = start; i < Math.min(items.size(), start + perPage); i++) {
            Cosmetic item = items.get(i);
            boolean owned = Config.owns(item.id());
            boolean equipped = item.id().equals(Config.data.equipped.get(cat.name()));
            boolean favorite = Config.data.favorites.contains(item.id());
            String label = (favorite ? "★ " : "") + item.name() + (equipped ? "  [ON]" : owned ? "  [OWNED]" : "  [" + price(item) + " Coins]");
            add(label, x, 62 + (i - start) * 22, width, b -> {
                if (!Config.owns(item.id())) {
                    if (Config.buy(item, price(item))) status = "Purchased " + item.name();
                    else status = "Not enough Coins";
                } else {
                    if (item.id().equals(Config.data.equipped.get(cat.name()))) Config.data.equipped.remove(cat.name());
                    else Config.data.equipped.put(cat.name(), item.id());
                    status = "Equipped " + item.name();
                    Config.save();
                }
                rebuildWidgets();
            });
        }
        int y = this.height - 28;
        add("★ Favorite", cx - 145, y, 88, b -> {
            if (items.isEmpty()) return;
            Cosmetic item = items.get(Math.min(items.size()-1, page * perPage));
            if (!Config.data.favorites.remove(item.id())) Config.data.favorites.add(item.id());
            Config.save(); rebuildWidgets();
        });
        add("Prev", cx - 50, y, 48, b -> { if (page > 0) { page--; rebuildWidgets(); } });
        add("Back", cx + 2, y, 48, b -> onClose());
        add("Next", cx + 54, y, 48, b -> { if (page < pages - 1) { page++; rebuildWidgets(); } });
    }

    private int price(Cosmetic c) {
        return switch (c.category()) {
            case HAT, HAIR, FACE, EARS, SHOES -> 200;
            case ARMS, SHOULDERS, BACK, TOPS, PANTS -> 250;
            case PETS, OUTERWEAR, CAPE -> 400;
            case WING, OUTFITS -> 500;
            default -> 350;
        };
    }
}
