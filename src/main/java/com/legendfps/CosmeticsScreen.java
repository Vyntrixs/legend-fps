package com.legendfps;

import net.minecraft.client.gui.screens.Screen;

import java.util.List;

public class CosmeticsScreen extends RedScreen {
    private Cosmetic.Category cat = Cosmetic.Category.WING;
    private int page = 0;

    public CosmeticsScreen(Screen parent) {
        super("Cosmetics", parent);
    }

    @Override
    protected void init() {
        int tabW = Math.min(90, (this.width - 20) / 4);
        int tx = this.width / 2 - tabW * 2;
        Cosmetic.Category[] cats = Cosmetic.Category.values();
        for (int i = 0; i < cats.length; i++) {
            final Cosmetic.Category c = cats[i];
            String label = (c == cat ? "> " : "") + c.label;
            add(label, tx + i * tabW, 30, tabW - 2, b -> {
                cat = c;
                page = 0;
                status = "";
                rebuildWidgets();
            });
        }

        List<Cosmetic> items = Cosmetic.in(cat);
        int perPage = Math.max(3, Math.min(8, (this.height - 120) / 22));
        int pages = Math.max(1, (items.size() + perPage - 1) / perPage);
        page = Math.max(0, Math.min(page, pages - 1));

        int x = this.width / 2 - 100;
        int start = page * perPage;
        int end = Math.min(items.size(), start + perPage);
        for (int i = start; i < end; i++) {
            final Cosmetic item = items.get(i);
            boolean owned = Config.owns(item.id());
            boolean on = item.id().equals(Config.data.equipped.get(cat.name()));
            String label = item.name() + (!owned ? "  [LOCKED]" : on ? "  [ON]" : "");
            add(label, x, 56 + (i - start) * 22, 200, b -> {
                if (!Config.owns(item.id())) {
                    status = "Locked - redeem a code first";
                    return;
                }
                if (item.id().equals(Config.data.equipped.get(cat.name()))) {
                    Config.data.equipped.remove(cat.name());
                } else {
                    Config.data.equipped.put(cat.name(), item.id());
                }
                Config.save();
                status = "";
                rebuildWidgets();
            });
        }

        int cx = this.width / 2;
        int by = this.height - 28;
        final int lastPage = pages - 1;
        add("Prev", cx - 100, by, 60, b -> {
            if (page > 0) {
                page--;
                rebuildWidgets();
            }
        });
        add("Back", cx - 30, by, 60, b -> onClose());
        add("Next", cx + 40, by, 60, b -> {
            if (page < lastPage) {
                page++;
                rebuildWidgets();
            }
        });
    }
}
