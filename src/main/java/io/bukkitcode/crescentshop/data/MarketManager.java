package io.bukkitcode.crescentshop.data;

import io.bukkitcode.crescentshop.CrescentShop;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 市场价管理器：记录玩家上架价，结合系统基准价与默认表计算市场价。
 * 玩家价离散度过大时取最高/最低中间值，并向默认表靠拢。
 */
public class MarketManager {

    private static final double DISPERSION_THRESHOLD = 3.0;
    private static final double CORRECTION_FACTOR = 0.2;

    private final CrescentShop plugin;
    private final File file;
    private final Map<String, List<Double>> playerRecords = new HashMap<>();

    public MarketManager(CrescentShop plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "data/market.yml");
    }

    public void load() {
        playerRecords.clear();
        if (!file.exists()) {
            return;
        }
        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection root = config.getConfigurationSection("records");
        if (root == null) {
            return;
        }
        for (String materialKey : root.getKeys(false)) {
            List<Double> list = new ArrayList<>();
            for (Object obj : root.getList(materialKey, new ArrayList<>())) {
                if (obj instanceof Number) {
                    list.add(((Number) obj).doubleValue());
                }
            }
            if (!list.isEmpty()) {
                playerRecords.put(materialKey, list);
            }
        }
    }

    public void save() {
        FileConfiguration config = new YamlConfiguration();
        for (Map.Entry<String, List<Double>> entry : playerRecords.entrySet()) {
            config.set("records." + entry.getKey(), entry.getValue());
        }
        try {
            File parent = file.getParentFile();
            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("保存市场数据失败: " + e.getMessage());
        }
    }

    public void recordPrice(Material material, double price) {
        if (isExcluded(material) || price <= 0) {
            return;
        }
        double basePrice = plugin.getPriceTable().getPrice(material);
        if (basePrice > 0) {
            double cap = basePrice * plugin.getConfigManager().getMarketPriceCapMultiplier();
            if (price > cap) {
                return;
            }
        }
        List<Double> list = playerRecords.computeIfAbsent(material.name(), k -> new ArrayList<>());
        list.add(price);
        int max = plugin.getConfigManager().getMarketMaxRecords();
        while (list.size() > max) {
            list.remove(0);
        }
    }

    public double getMarketPrice(Material material) {
        if (isExcluded(material)) {
            return -1;
        }
        double playerAvg = getPlayerAverage(material);
        double systemPrice = getSystemBasePrice(material);
        double defaultPrice = plugin.getPriceTable().getPrice(material);

        double effectivePlayerAvg = playerAvg;
        if (playerAvg > 0) {
            double[] range = getPlayerRange(material);
            if (range != null && range[0] > 0 && range[1] / range[0] > DISPERSION_THRESHOLD) {
                double mid = (range[0] + range[1]) / 2.0;
                if (defaultPrice > 0) {
                    double diff = Math.abs(mid - defaultPrice) / defaultPrice;
                    if (diff > 0.5) {
                        mid = mid + (defaultPrice - mid) * CORRECTION_FACTOR;
                    }
                }
                effectivePlayerAvg = mid;
            }
        }

        double sum = 0;
        int count = 0;
        if (effectivePlayerAvg > 0) {
            sum += effectivePlayerAvg;
            count++;
        }
        if (systemPrice > 0) {
            sum += systemPrice;
            count++;
        }
        if (defaultPrice > 0) {
            sum += defaultPrice;
            count++;
        }
        return count == 0 ? -1 : sum / count;
    }

    public double getPlayerAverage(Material material) {
        List<Double> list = playerRecords.get(material.name());
        if (list == null || list.isEmpty()) {
            return -1;
        }
        double sum = 0;
        for (double value : list) {
            sum += value;
        }
        return sum / list.size();
    }

    public double[] getPlayerRange(Material material) {
        List<Double> list = playerRecords.get(material.name());
        if (list == null || list.isEmpty()) {
            return null;
        }
        double min = Double.MAX_VALUE;
        double max = Double.MIN_VALUE;
        for (double value : list) {
            min = Math.min(min, value);
            max = Math.max(max, value);
        }
        return new double[]{min, max};
    }

    public double getSystemBasePrice(Material material) {
        List<Map<?, ?>> pool = plugin.getConfig().getMapList("system-shop.pool");
        for (Map<?, ?> entry : pool) {
            Object materialObj = entry.get("material");
            if (materialObj == null) {
                continue;
            }
            if (material.name().equalsIgnoreCase(String.valueOf(materialObj))) {
                Object priceObj = entry.get("base-price");
                if (priceObj instanceof Number) {
                    return ((Number) priceObj).doubleValue();
                }
            }
        }
        return -1;
    }

    public boolean isExcluded(Material material) {
        String name = material.name();
        return name.equals("SHULKER_BOX") || name.endsWith("_SHULKER_BOX");
    }
}
