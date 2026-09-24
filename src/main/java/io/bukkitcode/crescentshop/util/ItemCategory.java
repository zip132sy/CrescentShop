package io.bukkitcode.crescentshop.util;

import org.bukkit.Material;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** 物品分类工具。 */
public final class ItemCategory {

    public static final List<String> CATEGORIES = Arrays.asList(
            "矿物材料", "食物", "工具装备", "建筑方块", "红石杂项", "其他");

    private ItemCategory() {
    }

    public static boolean isTechnical(Material material) {
        String name = material.name();
        return name.contains("PISTON_HEAD") || name.contains("PISTON_EXTENSION")
                || name.contains("COMMAND") || name.contains("STRUCTURE")
                || name.contains("BARRIER") || name.contains("LIGHT_BLOCK")
                || name.contains("END_GATEWAY") || name.contains("END_PORTAL")
                || name.contains("PORTAL") || name.equals("FIRE")
                || name.equals("WATER") || name.equals("STATIONARY_WATER")
                || name.equals("LAVA") || name.equals("STATIONARY_LAVA")
                || name.contains("BED_BLOCK") || name.contains("DOUBLE_")
                || name.contains("_MOVING") || name.contains("BURNING_FURNACE")
                || name.contains("STANDING_BANNER") || name.contains("WALL_BANNER")
                || name.contains("SIGN_POST") || name.contains("WALL_SIGN")
                || name.contains("WOODEN_DOOR") || name.contains("IRON_DOOR_BLOCK")
                || name.contains("CAULDRON") || name.contains("BREWING_STAND")
                || name.contains("FLOWER_POT") || name.contains("SKULL")
                || name.contains("REDSTONE_COMPARATOR") || name.contains("DIODE")
                || name.contains("POWERED_") || name.contains("DETECTOR_RAIL")
                || name.contains("ACTIVATOR_RAIL") || name.contains("TRIPWIRE")
                || name.contains("COCOA") || name.contains("NETHER_WARTS")
                || name.contains("CROPS") || name.equals("CARROT")
                || name.equals("POTATO") || name.contains("BEETROOT_BLOCK")
                || name.contains("SUGAR_CANE_BLOCK") || name.contains("PUMPKIN_STEM")
                || name.contains("MELON_STEM") || name.contains("STEM")
                || name.contains("VINE") || name.contains("LILY_PAD")
                || name.contains("NETHER_STALK") || name.contains("CHORUS")
                || name.contains("END_ROD") || name.contains("PURPUR")
                || name.equals("AIR");
    }

    public static int getCategoryIndex(Material material) {
        String name = material.name();
        if (name.contains("DIAMOND") || name.contains("EMERALD") || name.contains("GOLD")
                || name.contains("IRON") || name.contains("COAL") || name.contains("REDSTONE")
                || name.contains("LAPIS") || name.contains("INK_SACK") || name.contains("QUARTZ")
                || name.contains("GLOWSTONE") || name.contains("OBSIDIAN") || name.contains("ENDER")
                || name.contains("BLAZE") || name.contains("GHAST") || name.contains("SLIME")
                || name.contains("NETHER") || name.contains("PRISMARINE") || name.contains("SHULKER")
                || name.contains("INGOT") || name.contains("NUGGET") || name.contains("GEM")) {
            return 0;
        }
        if (material.isEdible() || name.contains("APPLE") || name.contains("BREAD")
                || name.contains("COOKED") || name.contains("RAW") || name.contains("FISH")
                || name.contains("PORK") || name.contains("BEEF") || name.contains("CHICKEN")
                || name.contains("MUTTON") || name.contains("RABBIT") || name.contains("POTATO")
                || name.contains("CARROT") || name.contains("BEETROOT") || name.contains("MELON")
                || name.contains("PUMPKIN_PIE") || name.contains("COOKIE") || name.contains("CAKE")
                || name.contains("MUSHROOM") || name.contains("SEEDS")) {
            return 1;
        }
        if (name.endsWith("_SWORD") || name.endsWith("_PICKAXE") || name.endsWith("_AXE")
                || name.endsWith("_SHOVEL") || name.endsWith("_HOE")
                || name.endsWith("_HELMET") || name.endsWith("_CHESTPLATE")
                || name.endsWith("_LEGGINGS") || name.endsWith("_BOOTS")
                || name.contains("BOW") || name.contains("ARROW") || name.contains("SHIELD")
                || name.contains("FISHING_ROD") || name.contains("FLINT_AND_STEEL")
                || name.contains("SHEARS") || name.contains("ELYTRA") || name.contains("TOTEM")) {
            return 2;
        }
        if (material.isBlock() && !isTechnical(material)) {
            return 3;
        }
        if (name.contains("REDSTONE") || name.contains("REPEATER") || name.contains("COMPARATOR")
                || name.contains("PISTON") || name.contains("DISPENSER") || name.contains("DROPPER")
                || name.contains("HOPPER") || name.contains("OBSERVER") || name.contains("RAIL")
                || name.contains("MINECART") || name.contains("TNT") || name.contains("LEVER")
                || name.contains("BUTTON") || name.contains("PRESSURE_PLATE") || name.contains("TRIPWIRE")
                || name.contains("DAYLIGHT") || name.contains("NOTE_BLOCK") || name.contains("JUKEBOX")
                || name.contains("COMMAND") || name.contains("STRUCTURE")) {
            return 4;
        }
        return 5;
    }

    public static List<Material> getItems(int categoryIndex, boolean filterTech) {
        List<Material> result = new ArrayList<>();
        for (Material material : Material.values()) {
            if (material == Material.AIR) {
                continue;
            }
            if (filterTech && isTechnical(material)) {
                continue;
            }
            if (getCategoryIndex(material) == categoryIndex) {
                result.add(material);
            }
        }
        return result;
    }

    public static List<Material> search(String keyword, boolean filterTech) {
        List<Material> result = new ArrayList<>();
        String lower = keyword.toLowerCase();
        for (Material material : Material.values()) {
            if (material == Material.AIR) {
                continue;
            }
            if (filterTech && isTechnical(material)) {
                continue;
            }
            if (material.name().toLowerCase().contains(lower)) {
                result.add(material);
            }
        }
        return result;
    }
}
