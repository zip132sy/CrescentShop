package io.bukkitcode.crescentshop.data;

import io.bukkitcode.crescentshop.CrescentShop;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/** 默认价格表：规则自动定价 + OP 手动覆盖。 */
public class PriceTable {

    private final CrescentShop plugin;
    private final File file;
    private final Map<String, Double> overrides = new HashMap<>();

    public PriceTable(CrescentShop plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "data/prices.yml");
    }

    public void load() {
        overrides.clear();
        if (!file.exists()) {
            return;
        }
        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        if (config.getConfigurationSection("prices") == null) {
            return;
        }
        for (String key : config.getConfigurationSection("prices").getKeys(false)) {
            overrides.put(key, config.getDouble("prices." + key));
        }
    }

    public void save() {
        FileConfiguration config = new YamlConfiguration();
        for (Map.Entry<String, Double> entry : overrides.entrySet()) {
            config.set("prices." + entry.getKey(), entry.getValue());
        }
        try {
            File parent = file.getParentFile();
            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("保存价格表失败: " + e.getMessage());
        }
    }

    public void setPrice(Material material, double price) {
        overrides.put(material.name(), price);
    }

    public boolean hasOverride(Material material) {
        return overrides.containsKey(material.name());
    }

    public double getPrice(Material material) {
        Double override = overrides.get(material.name());
        return override != null ? override : estimatePrice(material);
    }

    private double estimatePrice(Material material) {
        String name = material.name();
        if (name.contains("DIAMOND")) {
            return 50.0;
        }
        if (name.contains("EMERALD")) {
            return 30.0;
        }
        if (name.contains("NETHER_STAR") || name.contains("DRAGON_EGG")) {
            return 200.0;
        }
        if (name.contains("GOLD") || name.contains("GOLDEN")) {
            return 12.0;
        }
        if (name.contains("IRON")) {
            return 8.0;
        }
        if (name.contains("LAPIS") || name.contains("INK_SACK")) {
            return 5.0;
        }
        if (name.contains("REDSTONE")) {
            return 4.0;
        }
        if (name.contains("COAL")) {
            return 3.0;
        }
        if (name.contains("QUARTZ")) {
            return 6.0;
        }
        if (name.contains("GLOWSTONE")) {
            return 7.0;
        }
        if (name.contains("OBSIDIAN")) {
            return 10.0;
        }
        if (name.contains("ENDER")) {
            return 18.0;
        }
        if (name.contains("BLAZE")) {
            return 15.0;
        }
        if (name.contains("GHAST")) {
            return 25.0;
        }
        if (name.contains("SLIME")) {
            return 9.0;
        }
        if (name.contains("NETHER")) {
            return 8.0;
        }
        if (name.contains("GOLDEN_APPLE")) {
            return name.contains("ENCHANTED") ? 100.0 : 40.0;
        }
        if (name.contains("CAKE") || name.contains("COOKIE") || name.contains("PIE")) {
            return 6.0;
        }
        if (name.contains("COOKED") || name.contains("STEAK") || name.contains("PORKCHOP")) {
            return 5.0;
        }
        if (name.contains("BREAD") || name.contains("APPLE") || name.contains("CARROT")
                || name.contains("POTATO") || name.contains("BEETROOT")) {
            return 2.0;
        }
        if (name.contains("WHEAT") || name.contains("SEEDS") || name.contains("SUGAR")
                || name.contains("MELON") || name.contains("PUMPKIN")) {
            return 1.5;
        }
        if (name.contains("FISH") || name.contains("CHICKEN") || name.contains("BEEF")
                || name.contains("MUTTON") || name.contains("RABBIT")) {
            return 3.0;
        }
        if (name.endsWith("_SWORD") || name.endsWith("_PICKAXE") || name.endsWith("_AXE")
                || name.endsWith("_SHOVEL") || name.endsWith("_HOE")
                || name.endsWith("_HELMET") || name.endsWith("_CHESTPLATE")
                || name.endsWith("_LEGGINGS") || name.endsWith("_BOOTS")) {
            if (name.startsWith("DIAMOND")) {
                return 45.0;
            }
            if (name.startsWith("GOLD")) {
                return 20.0;
            }
            if (name.startsWith("IRON")) {
                return 15.0;
            }
            if (name.startsWith("CHAINMAIL")) {
                return 12.0;
            }
            if (name.startsWith("LEATHER")) {
                return 6.0;
            }
            if (name.startsWith("STONE") || name.startsWith("WOOD")) {
                return 3.0;
            }
            return 10.0;
        }
        if (name.contains("DIRT") || name.contains("GRASS") || name.contains("SAND")
                || name.contains("GRAVEL") || name.contains("CLAY")) {
            return 0.5;
        }
        if (name.contains("COBBLESTONE") || name.contains("STONE") || name.contains("LOG")
                || name.contains("PLANK") || name.contains("WOOD")) {
            return 1.0;
        }
        if (name.contains("GLASS") || name.contains("BRICK") || name.contains("TERRACOTTA")) {
            return 2.0;
        }
        if (name.contains("WOOL") || name.contains("CARPET")) {
            return 2.5;
        }
        if (name.contains("SHULKER")) {
            return 30.0;
        }
        if (name.contains("BONE") || name.contains("STRING") || name.contains("FEATHER")
                || name.contains("LEATHER") || name.contains("FLINT")) {
            return 2.0;
        }
        if (name.contains("PAPER") || name.contains("BOOK") || name.contains("SUGAR_CANE")) {
            return 1.5;
        }
        if (name.contains("POTION") || name.contains("BREWING")) {
            return 15.0;
        }
        if (name.contains("ENCHANT") || name.contains("ANVIL")) {
            return 25.0;
        }
        return 1.0;
    }
}
