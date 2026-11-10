package com.legendfps;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import java.util.ArrayList;
import java.util.List;

public record Cosmetic(String id, String name, Category category, ParticleOptions particle, int tier) {
    public enum Category {
        WING("Wings"), CAPE("Capes"), HALO("Halos"), AURA("Auras"), TRAIL("Trails"), HAND_TRAIL("Hand Trails"),
        HAT("Hats"), HAIR("Hair"), FACE("Face"), EARS("Ears"), SHOULDERS("Shoulders"),
        ARMS("Arms & Gloves"), BACK("Back"), TOPS("Tops"), PANTS("Pants"),
        OUTFITS("Full Outfits"), SHOES("Shoes"), OUTERWEAR("Outerwear"), PETS("Pets");
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
    private static void addSet(Category c, String prefix, String... names) {
        for (int i = 0; i < names.length; i++)
            add(c, prefix + "_" + (i + 1), names[i], ParticleTypes.END_ROD);
    }

    static {
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

        add(Category.HAND_TRAIL, "hand_flame", "Flame Hand Trail", ParticleTypes.FLAME);
        add(Category.HAND_TRAIL, "hand_soul", "Soul Hand Trail", ParticleTypes.SOUL_FIRE_FLAME);
        add(Category.HAND_TRAIL, "hand_lightning", "Lightning Hand Trail", ParticleTypes.ELECTRIC_SPARK);
        add(Category.HAND_TRAIL, "hand_magic", "Arcane Hand Trail", ParticleTypes.ENCHANT);
        add(Category.HAND_TRAIL, "hand_heart", "Heart Hand Trail", ParticleTypes.HEART);
        add(Category.HAND_TRAIL, "hand_frost", "Frost Hand Trail", ParticleTypes.SNOWFLAKE);
        add(Category.HAND_TRAIL, "hand_void", "Void Hand Trail", ParticleTypes.PORTAL);
        add(Category.HAND_TRAIL, "hand_star", "Star Hand Trail", ParticleTypes.END_ROD);

        String[] first = {"Crimson","Obsidian","Royal","Dragon","Nether","End","Glacier","Inferno","Storm","Sakura","Void","Emerald","Galaxy","Shadow","Legend"};
        String[] second = {"Aurora","Sunset","Ocean","Desert","Forest","Lightning","Soul","Angel","Demon","Cyber","Pixel","Titan","Phantom","Champion","Vyntrix"};
        for (int i=0;i<first.length;i++) addCape("cape_"+first[i].toLowerCase(), first[i], 3);
        for (int i=0;i<second.length;i++) addCape("cape_"+second[i].toLowerCase(), second[i], 4);

        addSet(Category.HAT,"hat","Crimson Crown","Shadow Hood","Frost Beanie","Dragon Helm","Royal Cap");
        addSet(Category.HAIR,"hair","Spiky Black","Silver Flow","Crimson Fade","Midnight Fringe","Neon Hair");
        addSet(Category.FACE,"face","Red Visor","Phantom Mask","Pixel Shades","Cyber Mask","Star Glasses");
        addSet(Category.EARS,"ears","Fox Ears","Cat Ears","Dragon Horns","Neon Headset");
        addSet(Category.SHOULDERS,"shoulder","Mini Dragon","Shoulder Flame","Raven","Frost Spirit");
        addSet(Category.ARMS,"arms","Crimson Gloves","Cyber Bracers","Shadow Wraps","Golden Gauntlets");
        addSet(Category.BACK,"back","Explorer Pack","Dragon Pack","Jet Pack","Void Backpack");
        addSet(Category.TOPS,"top","Legend Hoodie","Crimson Jacket","Cyber Shirt","Royal Tunic");
        addSet(Category.PANTS,"pants","Shadow Pants","Crimson Trousers","Cyber Pants","Royal Pants");
        addSet(Category.OUTFITS,"outfit","Vyntrix Suit","Dragon Knight","Cyber Legend","Shadow Assassin");
        addSet(Category.SHOES,"shoes","Flame Boots","Neon Sneakers","Royal Boots","Frost Shoes");
        addSet(Category.OUTERWEAR,"outer","Long Crimson Coat","Phantom Cloak","Frost Jacket","Dragon Mantle");
        addSet(Category.PETS,"pet","Mini Dragon","Floating Slime","Baby Phantom","Tiny Bee","Pixel Fox");
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
