package com.legendfps;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;

import java.util.ArrayList;
import java.util.List;

public record Cosmetic(String id, String name, Category category, ParticleOptions particle, int tier) {

    public enum Category {
        WING("Wings"), CAPE("Capes"), HALO("Halos"), AURA("Auras"), TRAIL("Trails");
        public final String label;
        Category(String label) { this.label = label; }
    }

    public static final int TIER1_COUNT = 26;
    public static final List<Cosmetic> ALL = new ArrayList<>();

    private static void add(Category c, String id, String name, ParticleOptions p) {
        ALL.add(new Cosmetic(id, name, c, p, ALL.size() < TIER1_COUNT ? 1 : 2));
    }

    private static void addCape(String id, String name, int tier) {
        ALL.add(new Cosmetic(id, name, Category.CAPE, ParticleTypes.CLOUD, tier));
    }

    static {
        // Existing wing/halo/aura/trail cosmetics and their original unlock tiers.
        add(Category.WING, "wing_ember", "Ember Wings", ParticleTypes.FLAME);
        add(Category.WING, "wing_frost", "Frost Wings", ParticleTypes.SNOWFLAKE);
        add(Category.WING, "wing_void", "Void Wings", ParticleTypes.PORTAL);
        add(Category.WING, "wing_angel", "Angel Wings", ParticleTypes.END_ROD);
        add(Category.WING, "wing_forest", "Forest Wings", ParticleTypes.HAPPY_VILLAGER);
        add(Category.WING, "wing_soul", "Soul Wings", ParticleTypes.SOUL_FIRE_FLAME);
        add(Category.HALO, "halo_gold", "Gold Halo", ParticleTypes.END_ROD);
        add(Category.HALO, "halo_flame", "Flame Halo", ParticleTypes.FLAME);
        add(Category.HALO, "halo_soul", "Soul Halo", ParticleTypes.SOUL_FIRE_FLAME);
        add(Category.HALO, "halo_frost", "Frost Halo", ParticleTypes.SNOWFLAKE);
        add(Category.HALO, "halo_heart", "Heart Halo", ParticleTypes.HEART);
        add(Category.AURA, "aura_ember", "Ember Aura", ParticleTypes.SMALL_FLAME);
        add(Category.AURA, "aura_soul", "Soul Aura", ParticleTypes.SOUL);
        add(Category.AURA, "aura_frost", "Frost Aura", ParticleTypes.SNOWFLAKE);
        add(Category.AURA, "aura_nature", "Nature Aura", ParticleTypes.HAPPY_VILLAGER);
        add(Category.AURA, "aura_arcane", "Arcane Aura", ParticleTypes.ENCHANT);
        add(Category.AURA, "aura_crit", "Critical Aura", ParticleTypes.CRIT);
        add(Category.TRAIL, "trail_flame", "Flame Trail", ParticleTypes.FLAME);
        add(Category.TRAIL, "trail_soul", "Soul Trail", ParticleTypes.SOUL_FIRE_FLAME);
        add(Category.TRAIL, "trail_snow", "Snow Trail", ParticleTypes.SNOWFLAKE);
        add(Category.TRAIL, "trail_cloud", "Cloud Trail", ParticleTypes.CLOUD);
        add(Category.TRAIL, "trail_heart", "Heart Trail", ParticleTypes.HEART);
        add(Category.TRAIL, "trail_note", "Note Trail", ParticleTypes.NOTE);
        add(Category.TRAIL, "trail_spark", "Spark Trail", ParticleTypes.ELECTRIC_SPARK);
        add(Category.TRAIL, "trail_magic", "Magic Trail", ParticleTypes.ENCHANT);
        add(Category.TRAIL, "trail_crit", "Critical Trail", ParticleTypes.CRIT);
        add(Category.WING, "wing_storm", "Storm Wings", ParticleTypes.ELECTRIC_SPARK);
        add(Category.WING, "wing_sakura", "Sakura Wings", ParticleTypes.CHERRY_LEAVES);
        add(Category.WING, "wing_arcane", "Arcane Wings", ParticleTypes.ENCHANT);
        add(Category.WING, "wing_shadow", "Shadow Wings", ParticleTypes.ASH);
        add(Category.HALO, "halo_spark", "Spark Halo", ParticleTypes.ELECTRIC_SPARK);
        add(Category.HALO, "halo_glow", "Glow Halo", ParticleTypes.GLOW);
        add(Category.HALO, "halo_witch", "Witch Halo", ParticleTypes.WITCH);
        add(Category.AURA, "aura_music", "Music Aura", ParticleTypes.NOTE);
        add(Category.AURA, "aura_cloud", "Cloud Aura", ParticleTypes.CLOUD);
        add(Category.AURA, "aura_portal", "Portal Aura", ParticleTypes.PORTAL);
        add(Category.AURA, "aura_love", "Love Aura", ParticleTypes.HEART);
        add(Category.TRAIL, "trail_petal", "Petal Trail", ParticleTypes.CHERRY_LEAVES);
        add(Category.TRAIL, "trail_star", "Star Trail", ParticleTypes.END_ROD);
        add(Category.TRAIL, "trail_green", "Green Trail", ParticleTypes.HAPPY_VILLAGER);

        // 30 cape designs: the first 15 use 200090, the next 15 use 200167.
        addCape("cape_crimson", "Crimson", 3);
        addCape("cape_obsidian", "Obsidian", 3);
        addCape("cape_royal", "Royal", 3);
        addCape("cape_dragon", "Dragon", 3);
        addCape("cape_nether", "Nether", 3);
        addCape("cape_end", "End", 3);
        addCape("cape_ice", "Glacier", 3);
        addCape("cape_flame", "Inferno", 3);
        addCape("cape_storm", "Storm", 3);
        addCape("cape_sakura", "Sakura", 3);
        addCape("cape_void", "Void", 3);
        addCape("cape_emerald", "Emerald", 3);
        addCape("cape_galaxy", "Galaxy", 3);
        addCape("cape_shadow", "Shadow", 3);
        addCape("cape_legend", "Legend", 3);
        addCape("cape_aurora", "Aurora", 4);
        addCape("cape_sunset", "Sunset", 4);
        addCape("cape_ocean", "Ocean", 4);
        addCape("cape_desert", "Desert", 4);
        addCape("cape_forest", "Forest", 4);
        addCape("cape_lightning", "Lightning", 4);
        addCape("cape_soul", "Soul", 4);
        addCape("cape_angel", "Angel", 4);
        addCape("cape_demon", "Demon", 4);
        addCape("cape_cyber", "Cyber", 4);
        addCape("cape_pixel", "Pixel", 4);
        addCape("cape_titan", "Titan", 4);
        addCape("cape_phantom", "Phantom", 4);
        addCape("cape_champion", "Champion", 4);
        addCape("cape_vyntrix", "Vyntrix", 4);
    }

    public static List<Cosmetic> in(Category cat) {
        List<Cosmetic> out = new ArrayList<>();
        for (Cosmetic c : ALL) if (c.category() == cat) out.add(c);
        return out;
    }

    public static Cosmetic equipped(Category cat) {
        String id = Config.data.equipped.get(cat.name());
        if (id == null) return null;
        for (Cosmetic c : ALL) if (c.id().equals(id) && Config.owns(id)) return c;
        return null;
    }
}
