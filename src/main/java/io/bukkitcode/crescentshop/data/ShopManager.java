package io.bukkitcode.crescentshop.data;

import io.bukkitcode.crescentshop.CrescentShop;
import io.bukkitcode.crescentshop.model.Shop;
import io.bukkitcode.crescentshop.model.ShopItem;
import io.bukkitcode.crescentshop.util.TextUtil;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** 商店管理器：加载、保存、查找与增删。商店名可含颜色代码，查重用去色名。 */
public class ShopManager {

    private final CrescentShop plugin;
    private final File file;
    private final Map<String, Shop> shops = new LinkedHashMap<>();

    public ShopManager(CrescentShop plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "data/shops.yml");
    }

    public void load() {
        shops.clear();
        if (!file.exists()) {
            return;
        }
        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection root = config.getConfigurationSection("shops");
        if (root == null) {
            return;
        }
        for (String key : root.getKeys(false)) {
            ConfigurationSection section = root.getConfigurationSection(key);
            if (section == null) {
                continue;
            }
            try {
                String name = section.getString("name");
                if (name == null || name.isEmpty()) {
                    continue;
                }
                UUID owner = UUID.fromString(section.getString("owner", ""));
                int rows = section.getInt("rows", plugin.getConfigManager().getDefaultShopRows());
                Shop shop = new Shop(name, owner, rows);
                shop.setIconData(section.getString("icon", null));

                ConfigurationSection itemsSection = section.getConfigurationSection("items");
                if (itemsSection != null) {
                    for (String itemKey : itemsSection.getKeys(false)) {
                        ConfigurationSection itemSec = itemsSection.getConfigurationSection(itemKey);
                        if (itemSec == null) {
                            continue;
                        }
                        ShopItem shopItem = new ShopItem(null,
                                itemSec.getDouble("price", 0.0),
                                itemSec.getInt("amount", 0));
                        shopItem.deserializeItem(itemSec.getString("data", null));
                        if (shopItem.getItem() != null) {
                            shop.getItems().add(shopItem);
                        }
                    }
                }
                shops.put(normalizeKey(name), shop);
            } catch (Exception e) {
                plugin.getLogger().warning("加载商店 [" + key + "] 失败: " + e.getMessage());
            }
        }
    }

    public void save() {
        FileConfiguration config = new YamlConfiguration();
        int index = 0;
        for (Shop shop : shops.values()) {
            String base = "shops." + index + ".";
            config.set(base + "name", shop.getName());
            config.set(base + "owner", shop.getOwner().toString());
            config.set(base + "rows", shop.getRows());
            if (shop.getIconData() != null) {
                config.set(base + "icon", shop.getIconData());
            }
            List<ShopItem> items = shop.getItems();
            for (int i = 0; i < items.size(); i++) {
                ShopItem item = items.get(i);
                String itemBase = base + "items." + i + ".";
                config.set(itemBase + "price", item.getPrice());
                config.set(itemBase + "amount", item.getAmount());
                config.set(itemBase + "data", item.serializeItem());
            }
            index++;
        }
        try {
            File parent = file.getParentFile();
            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("保存商店数据失败: " + e.getMessage());
        }
    }

    private String normalizeKey(String name) {
        return TextUtil.stripColor(name).toLowerCase();
    }

    public boolean isNameTaken(String name) {
        return shops.containsKey(normalizeKey(name));
    }

    public Shop createShop(String name, UUID owner, int rows) {
        Shop shop = new Shop(name, owner, rows);
        shops.put(normalizeKey(name), shop);
        return shop;
    }

    public boolean renameShop(Shop shop, String newName) {
        String newKey = normalizeKey(newName);
        String oldKey = normalizeKey(shop.getName());
        if (!newKey.equals(oldKey) && shops.containsKey(newKey)) {
            return false;
        }
        shops.remove(oldKey);
        shop.setName(newName);
        shops.put(newKey, shop);
        return true;
    }

    public Shop getShop(String name) {
        return shops.get(normalizeKey(name));
    }

    public Shop getShopByIndex(int index) {
        if (index < 0 || index >= shops.size()) {
            return null;
        }
        int i = 0;
        for (Shop shop : shops.values()) {
            if (i == index) {
                return shop;
            }
            i++;
        }
        return null;
    }

    public Shop getShopByOwner(UUID owner) {
        for (Shop shop : shops.values()) {
            if (shop.getOwner().equals(owner)) {
                return shop;
            }
        }
        return null;
    }

    public boolean deleteShop(String name) {
        return shops.remove(normalizeKey(name)) != null;
    }

    public Collection<Shop> getAllShops() {
        return shops.values();
    }

    public List<Shop> search(String keyword) {
        List<Shop> result = new ArrayList<>();
        String lower = TextUtil.stripColor(keyword).toLowerCase();
        for (Shop shop : shops.values()) {
            if (TextUtil.stripColor(shop.getName()).toLowerCase().contains(lower)) {
                result.add(shop);
                continue;
            }
            for (ShopItem item : shop.getItems()) {
                if (item.getItem() != null && item.getItem().getType().name().toLowerCase().contains(lower)) {
                    result.add(shop);
                    break;
                }
            }
        }
        return result;
    }
}
