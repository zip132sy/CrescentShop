package io.bukkitcode.crescentshop.config;

import io.bukkitcode.crescentshop.CrescentShop;
import io.bukkitcode.crescentshop.util.TextUtil;
import org.bukkit.configuration.file.FileConfiguration;

/** 配置管理器：读取 config.yml 并做版本校验。 */
public class ConfigManager {

    public static final int CONFIG_VERSION = 6;

    private final CrescentShop plugin;

    private String currencyName;
    private double startBalance;
    private double slotCost;
    private double sellTaxRate;
    private int defaultShopRows;
    private int maxShopRows;
    private int minShopRows;
    private int maxShopNameLength;
    private boolean allowLargeMapIcon;
    private int maxMapIcons;
    private boolean filterTechnicalItems;

    private boolean systemShopEnabled;
    private int systemShopDailyItems;
    private double systemShopBuyMultiplier;
    private double systemShopPriceFluctuation;
    private boolean recycleLimitEnabled;
    private String recycleLimitMode;
    private int recycleLimitFixed;
    private int recycleLimitRandomMin;
    private int recycleLimitRandomMax;

    private double marketSellRatio;
    private int marketMaxRecords;
    private double marketPriceCapMultiplier;

    private int transferExpireDays;
    private int maxLoansPerPlayer;
    private int maxIncomeRecords;
    private double overduePenaltyPerDay;

    private boolean configOutdated;

    public ConfigManager(CrescentShop plugin) {
        this.plugin = plugin;
    }

    public void load() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        FileConfiguration config = plugin.getConfig();
        this.configOutdated = config.getInt("config-version", 0) != CONFIG_VERSION;

        this.currencyName = config.getString("economy.currency-name", "积分");
        this.startBalance = config.getDouble("economy.start-balance", 100.0);

        this.slotCost = config.getDouble("shop.slot-cost", 2.5);
        this.sellTaxRate = config.getDouble("shop.sell-tax-rate", 0.05);
        this.defaultShopRows = config.getInt("shop.default-rows", 4);
        this.maxShopRows = config.getInt("shop.max-rows", 6);
        this.minShopRows = config.getInt("shop.min-rows", 1);
        this.maxShopNameLength = config.getInt("shop.max-name-length", 24);
        this.allowLargeMapIcon = config.getBoolean("shop.allow-large-map-icon", true);
        this.maxMapIcons = config.getInt("shop.max-map-icons", 50);
        this.filterTechnicalItems = config.getBoolean("shop.filter-technical-items", true);

        this.systemShopEnabled = config.getBoolean("system-shop.enabled", true);
        this.systemShopDailyItems = config.getInt("system-shop.daily-items", 10);
        this.systemShopBuyMultiplier = config.getDouble("system-shop.buy-multiplier", 2.0);
        this.systemShopPriceFluctuation = config.getDouble("system-shop.price-fluctuation", 0.3);
        this.recycleLimitEnabled = config.getBoolean("system-shop.recycle-limit.enabled", false);
        this.recycleLimitMode = config.getString("system-shop.recycle-limit.mode", "fixed");
        this.recycleLimitFixed = config.getInt("system-shop.recycle-limit.fixed-amount", 64);
        this.recycleLimitRandomMin = config.getInt("system-shop.recycle-limit.random-min", 16);
        this.recycleLimitRandomMax = config.getInt("system-shop.recycle-limit.random-max", 128);

        this.marketSellRatio = config.getDouble("market.sell-ratio", 0.95);
        this.marketMaxRecords = config.getInt("market.max-records", 50);
        this.marketPriceCapMultiplier = config.getDouble("market.price-cap-multiplier", 10.0);

        this.transferExpireDays = config.getInt("economy.transfer-expire-days", 7);
        this.maxLoansPerPlayer = config.getInt("economy.max-loans-per-player", 5);
        this.maxIncomeRecords = config.getInt("economy.max-income-records", 50);
        this.overduePenaltyPerDay = config.getDouble("economy.overdue-penalty-per-day", 3.0);
    }

    public boolean isConfigOutdated() {
        return configOutdated;
    }

    public String getCurrencyName() {
        return currencyName;
    }

    public double getStartBalance() {
        return startBalance;
    }

    public double getSlotCost() {
        return slotCost;
    }

    public double getSellTaxRate() {
        return sellTaxRate;
    }

    public int getDefaultShopRows() {
        return defaultShopRows;
    }

    public int getMaxShopRows() {
        return maxShopRows;
    }

    public int getMinShopRows() {
        return minShopRows;
    }

    public int getMaxShopNameLength() {
        return maxShopNameLength;
    }

    public boolean isAllowLargeMapIcon() {
        return allowLargeMapIcon;
    }

    public int getMaxMapIcons() {
        return maxMapIcons;
    }

    public boolean isFilterTechnicalItems() {
        return filterTechnicalItems;
    }

    public boolean isSystemShopEnabled() {
        return systemShopEnabled;
    }

    public int getSystemShopDailyItems() {
        return systemShopDailyItems;
    }

    public double getSystemShopBuyMultiplier() {
        return systemShopBuyMultiplier;
    }

    public double getSystemShopPriceFluctuation() {
        return systemShopPriceFluctuation;
    }

    public boolean isRecycleLimitEnabled() {
        return recycleLimitEnabled;
    }

    public String getRecycleLimitMode() {
        return recycleLimitMode;
    }

    public int getRecycleLimitFixed() {
        return recycleLimitFixed;
    }

    public int getRecycleLimitRandomMin() {
        return recycleLimitRandomMin;
    }

    public int getRecycleLimitRandomMax() {
        return recycleLimitRandomMax;
    }

    public double getMarketSellRatio() {
        return marketSellRatio;
    }

    public int getMarketMaxRecords() {
        return marketMaxRecords;
    }

    public double getMarketPriceCapMultiplier() {
        return marketPriceCapMultiplier;
    }

    public int getTransferExpireDays() {
        return transferExpireDays;
    }

    public int getMaxLoansPerPlayer() {
        return maxLoansPerPlayer;
    }

    public int getMaxIncomeRecords() {
        return maxIncomeRecords;
    }

    public double getOverduePenaltyPerDay() {
        return overduePenaltyPerDay;
    }

    public String buildOutdatedMessage() {
        return TextUtil.color("&e[CrescentShop] &c检测到旧版配置文件，部分新功能可能无法生效。")
                + "\n" + TextUtil.color("&7请备份后删除 &fplugins/CrescentShop/config.yml &7并重启。");
    }
}
