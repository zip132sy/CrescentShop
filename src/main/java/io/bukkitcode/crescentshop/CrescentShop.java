package io.bukkitcode.crescentshop;

import io.bukkitcode.crescentshop.command.ShopCommand;
import io.bukkitcode.crescentshop.config.ConfigManager;
import io.bukkitcode.crescentshop.data.ChatInputManager;
import io.bukkitcode.crescentshop.data.EconomyDataManager;
import io.bukkitcode.crescentshop.data.MarketManager;
import io.bukkitcode.crescentshop.data.PlayerDataManager;
import io.bukkitcode.crescentshop.data.PriceTable;
import io.bukkitcode.crescentshop.data.ShopManager;
import io.bukkitcode.crescentshop.economy.EconomyManager;
import io.bukkitcode.crescentshop.gui.ShopGui;
import io.bukkitcode.crescentshop.listener.ChatListener;
import io.bukkitcode.crescentshop.listener.GuiListener;
import io.bukkitcode.crescentshop.listener.PlayerListener;
import io.bukkitcode.crescentshop.system.SystemShopManager;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

/** CrescentShop 主类。 */
public class CrescentShop extends JavaPlugin {

    private ConfigManager configManager;
    private EconomyManager economyManager;
    private PlayerDataManager playerDataManager;
    private ShopManager shopManager;
    private SystemShopManager systemShopManager;
    private ChatInputManager chatInputManager;
    private MarketManager marketManager;
    private PriceTable priceTable;
    private EconomyDataManager economyDataManager;
    private ShopCommand shopCommand;

    private ShopGui shopGui;

    @Override
    public void onEnable() {
        this.configManager = new ConfigManager(this);
        configManager.load();

        this.playerDataManager = new PlayerDataManager(this);
        playerDataManager.load();

        this.shopManager = new ShopManager(this);
        shopManager.load();

        this.priceTable = new PriceTable(this);
        priceTable.load();

        this.marketManager = new MarketManager(this);
        marketManager.load();

        this.systemShopManager = new SystemShopManager(this);
        systemShopManager.load();

        this.economyDataManager = new EconomyDataManager(this);
        economyDataManager.load();

        this.chatInputManager = new ChatInputManager(this);

        this.economyManager = new EconomyManager(this);
        economyManager.setup();

        this.shopGui = new ShopGui(this);

        PluginCommand pluginCommand = getCommand("shop");
        if (pluginCommand != null) {
            this.shopCommand = new ShopCommand(this);
            pluginCommand.setExecutor(shopCommand);
            pluginCommand.setTabCompleter(shopCommand);
        } else {
            getLogger().severe("无法注册命令 /shop。");
        }

        getServer().getPluginManager().registerEvents(new GuiListener(this), this);
        getServer().getPluginManager().registerEvents(new ChatListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerListener(this), this);

        getServer().getScheduler().runTaskTimer(this, () -> {
            String today = java.time.LocalDate.now().toString();
            if (!today.equals(systemShopManager.getCurrentDay())) {
                systemShopManager.refresh(today);
            }
            economyDataManager.processExpiredTransfers();
            economyDataManager.processDueLoans();
            economyDataManager.processLoanReminders();
        }, 20L * 60, 20L * 60);

        getLogger().info("CrescentShop 已启用，经济来源: " + economyManager.getProvider().getName());
    }

    @Override
    public void onDisable() {
        if (playerDataManager != null) {
            playerDataManager.save();
        }
        if (shopManager != null) {
            shopManager.save();
        }
        if (systemShopManager != null) {
            systemShopManager.save();
        }
        if (marketManager != null) {
            marketManager.save();
        }
        if (priceTable != null) {
            priceTable.save();
        }
        if (economyDataManager != null) {
            economyDataManager.save();
        }
        getLogger().info("CrescentShop 已禁用，数据已保存。");
    }

    public void reloadAll() {
        configManager.load();
        playerDataManager.load();
        shopManager.load();
        priceTable.load();
        marketManager.load();
        systemShopManager.load();
        economyDataManager.load();
        economyManager.setup();
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public EconomyManager getEconomyManager() {
        return economyManager;
    }

    public PlayerDataManager getPlayerDataManager() {
        return playerDataManager;
    }

    public ShopManager getShopManager() {
        return shopManager;
    }

    public SystemShopManager getSystemShopManager() {
        return systemShopManager;
    }

    public ChatInputManager getChatInputManager() {
        return chatInputManager;
    }

    public MarketManager getMarketManager() {
        return marketManager;
    }

    public PriceTable getPriceTable() {
        return priceTable;
    }

    public EconomyDataManager getEconomyDataManager() {
        return economyDataManager;
    }

    public ShopCommand getShopCommand() {
        return shopCommand;
    }

    public ShopGui getShopGui() {
        return shopGui;
    }

}
