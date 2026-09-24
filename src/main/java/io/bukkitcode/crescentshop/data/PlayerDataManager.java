package io.bukkitcode.crescentshop.data;

import io.bukkitcode.crescentshop.CrescentShop;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** 玩家数据管理器：余额与每日回收量。 */
public class PlayerDataManager {

    private final CrescentShop plugin;
    private final File file;
    private final Map<UUID, Double> balances = new HashMap<>();
    private final Map<UUID, Integer> recycledToday = new HashMap<>();
    private String recycleDay = "";

    public PlayerDataManager(CrescentShop plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "data/players.yml");
    }

    public void load() {
        balances.clear();
        recycledToday.clear();
        if (!file.exists()) {
            return;
        }
        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection balanceSection = config.getConfigurationSection("balances");
        if (balanceSection != null) {
            for (String key : balanceSection.getKeys(false)) {
                try {
                    balances.put(UUID.fromString(key), balanceSection.getDouble(key));
                } catch (IllegalArgumentException ignored) {
                }
            }
        }
        this.recycleDay = config.getString("recycle-day", "");
        ConfigurationSection recycleSection = config.getConfigurationSection("recycled-today");
        if (recycleSection != null) {
            for (String key : recycleSection.getKeys(false)) {
                try {
                    recycledToday.put(UUID.fromString(key), recycleSection.getInt(key));
                } catch (IllegalArgumentException ignored) {
                }
            }
        }
    }

    public void save() {
        FileConfiguration config = new YamlConfiguration();
        for (Map.Entry<UUID, Double> entry : balances.entrySet()) {
            config.set("balances." + entry.getKey().toString(), entry.getValue());
        }
        config.set("recycle-day", recycleDay);
        for (Map.Entry<UUID, Integer> entry : recycledToday.entrySet()) {
            config.set("recycled-today." + entry.getKey().toString(), entry.getValue());
        }
        try {
            File parent = file.getParentFile();
            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("保存玩家数据失败: " + e.getMessage());
        }
    }

    public double getBalance(UUID uuid) {
        Double value = balances.get(uuid);
        return value == null ? plugin.getConfigManager().getStartBalance() : value;
    }

    public void setBalance(UUID uuid, double amount) {
        balances.put(uuid, amount);
    }

    public boolean hasAccount(UUID uuid) {
        return balances.containsKey(uuid);
    }

    public void createAccount(UUID uuid) {
        if (!balances.containsKey(uuid)) {
            balances.put(uuid, plugin.getConfigManager().getStartBalance());
        }
    }

    public int getRecycledToday(UUID uuid) {
        checkRecycleDay();
        Integer value = recycledToday.get(uuid);
        return value == null ? 0 : value;
    }

    public void addRecycledToday(UUID uuid, int amount) {
        checkRecycleDay();
        recycledToday.put(uuid, getRecycledToday(uuid) + amount);
    }

    private void checkRecycleDay() {
        String today = java.time.LocalDate.now().toString();
        if (!today.equals(recycleDay)) {
            recycleDay = today;
            recycledToday.clear();
        }
    }
}
