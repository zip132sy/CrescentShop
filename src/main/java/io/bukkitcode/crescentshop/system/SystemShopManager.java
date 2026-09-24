package io.bukkitcode.crescentshop.system;

import io.bukkitcode.crescentshop.CrescentShop;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;

/** 系统商店管理器：每日随机物品、价格浮动、回收上限。 */
public class SystemShopManager {

    private final CrescentShop plugin;
    private final File file;
    private final Random random = new Random();
    private final List<SystemShopItem> dailyItems = new ArrayList<>();
    private String currentDay = "";
    private int dailyRecycleLimit = -1;

    public SystemShopManager(CrescentShop plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "data/system-shop.yml");
    }

    public void load() {
        dailyItems.clear();
        String today = LocalDate.now().toString();
        if (file.exists()) {
            FileConfiguration config = YamlConfiguration.loadConfiguration(file);
            currentDay = config.getString("day", "");
            dailyRecycleLimit = config.getInt("recycle-limit", -1);
            ConfigurationSection itemsSection = config.getConfigurationSection("items");
            if (itemsSection != null) {
                for (String key : itemsSection.getKeys(false)) {
                    ConfigurationSection sec = itemsSection.getConfigurationSection(key);
                    if (sec == null) {
                        continue;
                    }
                    Material material = Material.matchMaterial(sec.getString("material", ""));
                    if (material == null) {
                        continue;
                    }
                    dailyItems.add(new SystemShopItem(material,
                            sec.getDouble("base-price", 1.0),
                            sec.getDouble("price", 1.0),
                            sec.getInt("limit", -1)));
                }
            }
        }
        if (!today.equals(currentDay) || dailyItems.isEmpty()) {
            refresh(today);
        }
    }

    public void refresh(String day) {
        dailyItems.clear();
        currentDay = day;
        List<PoolEntry> pool = readPool();
        if (pool.isEmpty()) {
            plugin.getLogger().warning("系统商店物品池为空，请检查 config.yml。");
            save();
            return;
        }
        Collections.shuffle(pool, random);
        int count = Math.min(plugin.getConfigManager().getSystemShopDailyItems(), pool.size());
        double fluctuation = plugin.getConfigManager().getSystemShopPriceFluctuation();
        for (int i = 0; i < count; i++) {
            PoolEntry entry = pool.get(i);
            double market = plugin.getMarketManager().getMarketPrice(entry.material);
            double anchor = market > 0 ? (entry.basePrice + market) / 2.0 : entry.basePrice;
            double factor = 1.0 + (random.nextDouble() * 2 - 1) * fluctuation;
            dailyItems.add(new SystemShopItem(entry.material, entry.basePrice,
                    Math.max(0.01, anchor * factor), -1));
        }
        if (plugin.getConfigManager().isRecycleLimitEnabled()) {
            if ("random".equalsIgnoreCase(plugin.getConfigManager().getRecycleLimitMode())) {
                int min = plugin.getConfigManager().getRecycleLimitRandomMin();
                int max = Math.max(min, plugin.getConfigManager().getRecycleLimitRandomMax());
                dailyRecycleLimit = min + random.nextInt(max - min + 1);
            } else {
                dailyRecycleLimit = plugin.getConfigManager().getRecycleLimitFixed();
            }
        } else {
            dailyRecycleLimit = -1;
        }
        save();
    }

    private List<PoolEntry> readPool() {
        List<PoolEntry> pool = new ArrayList<>();
        for (Map<?, ?> map : plugin.getConfig().getMapList("system-shop.pool")) {
            Object materialObj = map.get("material");
            if (materialObj == null) {
                continue;
            }
            Material material = Material.matchMaterial(String.valueOf(materialObj));
            if (material == null) {
                continue;
            }
            double basePrice = 1.0;
            Object priceObj = map.get("base-price");
            if (priceObj instanceof Number) {
                basePrice = ((Number) priceObj).doubleValue();
            }
            pool.add(new PoolEntry(material, basePrice));
        }
        return pool;
    }

    public void save() {
        FileConfiguration config = new YamlConfiguration();
        config.set("day", currentDay);
        config.set("recycle-limit", dailyRecycleLimit);
        for (int i = 0; i < dailyItems.size(); i++) {
            SystemShopItem item = dailyItems.get(i);
            String base = "items." + i + ".";
            config.set(base + "material", item.getMaterial().name());
            config.set(base + "base-price", item.getBasePrice());
            config.set(base + "price", item.getPrice());
            config.set(base + "limit", item.getLimit());
        }
        try {
            File parent = file.getParentFile();
            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("保存系统商店数据失败: " + e.getMessage());
        }
    }

    public List<SystemShopItem> getDailyItems() {
        return dailyItems;
    }

    public int getDailyRecycleLimit() {
        return dailyRecycleLimit;
    }

    public String getCurrentDay() {
        return currentDay;
    }

    public SystemShopItem findByMaterial(Material material) {
        for (SystemShopItem item : dailyItems) {
            if (item.getMaterial() == material) {
                return item;
            }
        }
        return null;
    }

    private static class PoolEntry {
        private final Material material;
        private final double basePrice;

        private PoolEntry(Material material, double basePrice) {
            this.material = material;
            this.basePrice = basePrice;
        }
    }

    /** 系统商店物品条目 */
    public static class SystemShopItem {
        private final Material material;
        private final double basePrice;
        private final double price;
        private final int limit;

        public SystemShopItem(Material material, double basePrice, double price, int limit) {
            this.material = material;
            this.basePrice = basePrice;
            this.price = price;
            this.limit = limit;
        }

        public Material getMaterial() {
            return material;
        }

        public double getBasePrice() {
            return basePrice;
        }

        public double getPrice() {
            return price;
        }

        public int getLimit() {
            return limit;
        }

        public ItemStack buildDisplayItem() {
            return new ItemStack(material, 1);
        }

        public ItemStack buildSellItem(int amount) {
            return new ItemStack(material, amount);
        }
    }
}
