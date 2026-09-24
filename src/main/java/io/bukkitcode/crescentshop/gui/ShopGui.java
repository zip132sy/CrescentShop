package io.bukkitcode.crescentshop.gui;

import io.bukkitcode.crescentshop.CrescentShop;
import io.bukkitcode.crescentshop.data.EconomyDataManager.Income;
import io.bukkitcode.crescentshop.model.EconomyModels.Debt;
import io.bukkitcode.crescentshop.model.EconomyModels.Loan;
import io.bukkitcode.crescentshop.model.EconomyModels.Transfer;
import io.bukkitcode.crescentshop.model.Shop;
import io.bukkitcode.crescentshop.model.ShopItem;
import io.bukkitcode.crescentshop.util.GuiUtil;
import io.bukkitcode.crescentshop.util.ItemCategory;
import io.bukkitcode.crescentshop.util.TextUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

public class ShopGui {

    private static final int PAGE_SIZE = 45;
    private final CrescentShop plugin;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("MM-dd HH:mm");

    public ShopGui(CrescentShop plugin) {
        this.plugin = plugin;
    }

    public void openList(Player player) {
        List<Shop> shops = new ArrayList<>(plugin.getShopManager().getAllShops());
        int rows = Math.max(3, Math.min(6, (shops.size() + 8) / 9 + 1));
        int size = rows * 9;
        GuiHolder holder = new GuiHolder(GuiHolder.GuiType.SHOP_LIST);
        Inventory inv = Bukkit.createInventory(holder, size, TextUtil.color("&8商店合集 &7(" + shops.size() + ")"));
        holder.setInventory(inv);
        holder.setShopList(shops);
        ItemStack filler = GuiUtil.buildFiller();
        for (int i = size - 9; i < size; i++) {
            inv.setItem(i, filler);
        }
        int slot = 0;
        for (Shop shop : shops) {
            if (slot >= size - 9) {
                break;
            }
            inv.setItem(slot, buildShopIcon(shop));
            slot++;
        }
        int defaultRows = plugin.getConfigManager().getDefaultShopRows();
        double createFee = defaultRows * 9 * plugin.getConfigManager().getSlotCost();
        inv.setItem(size - 9, GuiUtil.buildItem(Material.CHEST, "&a创建商店",
                Arrays.asList("&7点击后在聊天栏输入商店名", "&7支持颜色代码，如 &f&a我的店",
                        "&7默认 " + defaultRows + " 行，创建费用: &e" + TextUtil.formatMoney(createFee)
                                + " " + plugin.getConfigManager().getCurrencyName())));
        inv.setItem(size - 5, GuiUtil.buildItem(Material.COMPASS, "&b搜索商店",
                Arrays.asList("&7使用 &f/shop search <关键词> &7搜索")));
        inv.setItem(size - 1, GuiUtil.buildItem(Material.ENDER_CHEST, "&6系统商店",
                Arrays.asList("&7点击打开系统回收/出售商店")));
        player.openInventory(inv);
    }

    public void openView(Player player, Shop shop) {
        int rows = Math.max(plugin.getConfigManager().getMinShopRows(),
                Math.min(plugin.getConfigManager().getMaxShopRows(), shop.getRows()));
        int size = rows * 9;
        GuiHolder holder = new GuiHolder(GuiHolder.GuiType.SHOP_VIEW);
        holder.setShopName(shop.getName());
        Inventory inv = Bukkit.createInventory(holder, size, TextUtil.color("&8商店 &7- " + shop.getName()));
        holder.setInventory(inv);
        List<ShopItem> items = shop.getItems();
        int capacity = size - 9;
        for (int i = 0; i < items.size() && i < capacity; i++) {
            inv.setItem(i, buildItemIcon(items.get(i), false));
        }
        ItemStack filler = GuiUtil.buildFiller();
        for (int i = size - 9; i < size; i++) {
            inv.setItem(i, filler);
        }
        inv.setItem(size - 9, GuiUtil.buildItem(Material.ARROW, "&e返回商店列表", Arrays.asList("&7点击返回合集面板")));
        inv.setItem(size - 5, GuiUtil.buildItem(Material.PAPER, "&b商店信息",
                Arrays.asList("&7名称: " + shop.getName(),
                        "&7商品数: &f" + items.size() + " &7/ &f" + shop.getCapacity())));
        if (shop.getIconData() != null) {
            ItemStack icon = io.bukkitcode.crescentshop.util.ItemSerializer.deserialize(shop.getIconData());
            if (icon != null) {
                icon.setAmount(1);
                ItemMeta iconMeta = icon.getItemMeta();
                if (iconMeta != null) {
                    iconMeta.setDisplayName(TextUtil.color("&b商店图标"));
                    iconMeta.setLore(Arrays.asList(TextUtil.color("&7" + shop.getName())));
                    icon.setItemMeta(iconMeta);
                }
                inv.setItem(size - 6, icon);
            }
        }
        if (shop.getOwner().equals(player.getUniqueId())) {
            inv.setItem(size - 1, GuiUtil.buildItem(Material.ANVIL, "&a编辑商店",
                    Arrays.asList("&7点击进入编辑界面", "&7可改名、改大小、下架、改价")));
        }
        player.openInventory(inv);
    }

    public void openEdit(Player player, Shop shop) {
        int rows = Math.max(3, Math.min(6, shop.getRows()));
        int size = rows * 9;
        GuiHolder holder = new GuiHolder(GuiHolder.GuiType.SHOP_EDIT);
        holder.setShopName(shop.getName());
        Inventory inv = Bukkit.createInventory(holder, size, TextUtil.color("&8编辑商店 &7- " + shop.getName()));
        holder.setInventory(inv);
        inv.setItem(0, GuiUtil.buildItem(Material.NAME_TAG, "&b修改商店名",
                Arrays.asList("&7当前: " + shop.getName(), "&7支持颜色代码，如 &f&a我的店", "",
                        "&e点击后在聊天栏输入新名称")));
        inv.setItem(1, GuiUtil.buildItem(Material.ITEM_FRAME, "&b修改界面大小",
                Arrays.asList("&7当前: &f" + shop.getRows() + " 行",
                        "&7范围: &f" + plugin.getConfigManager().getMinShopRows() + " - "
                                + plugin.getConfigManager().getMaxShopRows() + " 行",
                        "&7按格子差额收费（每格 " + plugin.getConfigManager().getSlotCost() + "）", "",
                        "&e点击后在聊天栏输入行数")));
        inv.setItem(2, GuiUtil.buildItem(Material.HOPPER, "&a上架手持物品",
                Arrays.asList("&7手持物品后点击", "&7输入「价格 数量」上架",
                        "&7数量不能超过手持数量与 64", "", "&e点击后在聊天栏输入")));
        inv.setItem(3, GuiUtil.buildItem(Material.PAPER, "&b商店信息",
                Arrays.asList("&7名称: " + shop.getName(),
                        "&7商品数: &f" + shop.getItems().size() + " &7/ &f" + shop.getCapacity(),
                        "&7界面大小: &f" + shop.getRows() + " 行")));
        inv.setItem(4, GuiUtil.buildItem(Material.BOOK, "&b操作说明",
                Arrays.asList("&7商品区：", "&e左键 &7= 修改价格", "&e右键 &7= 下架商品")));
        inv.setItem(5, GuiUtil.buildItem(Material.ITEM_FRAME, "&b设置商店图标",
                Arrays.asList("&7手持物品后点击", "&7支持地图（显示地图画面）",
                        "&7默认图标为箱子", "", "&e点击后设置图标")));
        inv.setItem(8, GuiUtil.buildItem(Material.BARRIER, "&c返回商店列表", Arrays.asList("&7点击返回合集面板")));
        List<ShopItem> items = shop.getItems();
        int slot = 9;
        for (ShopItem item : items) {
            if (slot >= size) {
                break;
            }
            inv.setItem(slot, buildItemIcon(item, true));
            slot++;
        }
        player.openInventory(inv);
    }

    public void openIconMain(Player player, Shop shop) {
        GuiHolder holder = new GuiHolder(GuiHolder.GuiType.ICON_SELECT);
        holder.setShopName(shop.getName());
        Inventory inv = Bukkit.createInventory(holder, 27, TextUtil.color("&8设置商店图标"));
        holder.setInventory(inv);
        ItemStack filler = GuiUtil.buildFiller();
        for (int i = 0; i < 27; i++) {
            inv.setItem(i, filler);
        }
        inv.setItem(11, GuiUtil.buildItem(Material.STICK, "&a使用手持物品",
                Arrays.asList("&7把当前手持的物品设为图标", "&7支持地图（显示地图画面）", "",
                        "&e点击使用手持物品")));
        inv.setItem(15, GuiUtil.buildItem(Material.CHEST, "&b从列表选择",
                Arrays.asList("&7浏览分类或搜索物品", "&7从列表中选择一个作为图标", "",
                        "&e点击打开物品列表")));
        player.openInventory(inv);
    }

    public void openIconCategory(Player player, Shop shop) {
        GuiHolder holder = new GuiHolder(GuiHolder.GuiType.ICON_CATEGORY);
        holder.setShopName(shop.getName());
        Inventory inv = Bukkit.createInventory(holder, 27, TextUtil.color("&8选择物品分类"));
        holder.setInventory(inv);
        ItemStack filler = GuiUtil.buildFiller();
        for (int i = 0; i < 27; i++) {
            inv.setItem(i, filler);
        }
        List<String> categories = ItemCategory.CATEGORIES;
        Material[] icons = {Material.DIAMOND, Material.APPLE, Material.DIAMOND_SWORD,
                Material.BRICK, Material.REDSTONE, Material.PAPER};
        int slot = 10;
        for (int i = 0; i < categories.size(); i++) {
            inv.setItem(slot, GuiUtil.buildItem(icons[i], "&b" + categories.get(i),
                    Arrays.asList("&7点击浏览该分类的物品")));
            slot++;
        }
        inv.setItem(22, GuiUtil.buildItem(Material.COMPASS, "&e搜索物品",
                Arrays.asList("&7点击后在聊天栏输入关键词")));
        inv.setItem(18, GuiUtil.buildItem(Material.ARROW, "&e返回", Arrays.asList("&7返回图标选择界面")));
        player.openInventory(inv);
    }

    public void openIconList(Player player, Shop shop, List<Material> materials,
                             int page, int categoryIndex, String keyword) {
        int totalPages = Math.max(1, (materials.size() + PAGE_SIZE - 1) / PAGE_SIZE);
        if (page < 0) {
            page = 0;
        }
        if (page >= totalPages) {
            page = totalPages - 1;
        }
        String title = keyword != null ? "&8搜索: &f" + keyword : "&8" + ItemCategory.CATEGORIES.get(categoryIndex);
        GuiHolder holder = new GuiHolder(GuiHolder.GuiType.ICON_LIST);
        holder.setShopName(shop.getName());
        holder.setCategoryIndex(categoryIndex);
        holder.setPage(page);
        holder.setMaterialList(materials);
        holder.setSearchKeyword(keyword);
        Inventory inv = Bukkit.createInventory(holder, 54, TextUtil.color(title));
        holder.setInventory(inv);
        int start = page * PAGE_SIZE;
        for (int i = 0; i < PAGE_SIZE; i++) {
            int index = start + i;
            if (index >= materials.size()) {
                break;
            }
            Material material = materials.get(index);
            inv.setItem(i, GuiUtil.buildItem(material, "&f" + material.name(),
                    Arrays.asList("&7点击设为商店图标")));
        }
        ItemStack filler = GuiUtil.buildFiller();
        for (int i = 45; i < 54; i++) {
            inv.setItem(i, filler);
        }
        if (page > 0) {
            inv.setItem(45, GuiUtil.buildItem(Material.ARROW, "&e上一页",
                    Arrays.asList("&7第 " + page + " / " + totalPages + " 页")));
        }
        inv.setItem(49, GuiUtil.buildItem(Material.PAPER, "&b第 " + (page + 1) + " / " + totalPages + " 页",
                Arrays.asList("&7共 " + materials.size() + " 个物品")));
        if (page < totalPages - 1) {
            inv.setItem(53, GuiUtil.buildItem(Material.ARROW, "&e下一页",
                    Arrays.asList("&7第 " + (page + 2) + " / " + totalPages + " 页")));
        }
        inv.setItem(48, GuiUtil.buildItem(Material.COMPASS, "&e搜索物品",
                Arrays.asList("&7点击后在聊天栏输入关键词")));
        inv.setItem(50, GuiUtil.buildItem(Material.BARRIER, "&c返回分类", Arrays.asList("&7返回分类选择界面")));
        player.openInventory(inv);
    }

    public void openEcoMain(Player player) {
        GuiHolder holder = new GuiHolder(GuiHolder.GuiType.ECO_MAIN);
        Inventory inv = Bukkit.createInventory(holder, 27, TextUtil.color("&8经济面板"));
        holder.setInventory(inv);
        ItemStack filler = GuiUtil.buildFiller();
        for (int i = 0; i < 27; i++) {
            inv.setItem(i, filler);
        }
        double balance = plugin.getEconomyManager().getProvider().getBalance(player);
        List<Loan> activeLoans = plugin.getEconomyDataManager().getActiveLoansOf(player.getUniqueId());
        List<Debt> debts = plugin.getEconomyDataManager().getDebtsOf(player.getUniqueId());
        List<Transfer> pending = plugin.getEconomyDataManager().getPendingTransfers(player.getUniqueId());
        List<String> balanceLore = new ArrayList<>();
        balanceLore.add("&7余额: &e" + TextUtil.formatMoney(balance)
                + " " + plugin.getConfigManager().getCurrencyName());
        if (!activeLoans.isEmpty()) {
            balanceLore.add("");
            balanceLore.add("&7借款:");
            int count = 0;
            for (Loan loan : activeLoans) {
                if (count >= plugin.getConfigManager().getMaxLoansPerPlayer()) {
                    break;
                }
                String name = Bukkit.getOfflinePlayer(loan.getLender()).getName();
                balanceLore.add("&7 - 欠 &f" + (name == null ? "未知" : name)
                        + " &e" + TextUtil.formatMoney(loan.getRemaining()));
                count++;
            }
        }
        if (!debts.isEmpty()) {
            balanceLore.add("");
            balanceLore.add("&c欠款:");
            for (Debt debt : debts) {
                String name = Bukkit.getOfflinePlayer(debt.getLender()).getName();
                balanceLore.add("&7 - 欠 &f" + (name == null ? "未知" : name)
                        + " &c" + TextUtil.formatMoney(debt.getTotalOwed(
                        plugin.getConfigManager().getOverduePenaltyPerDay())));
            }
        }
        inv.setItem(10, GuiUtil.buildItem(Material.GOLD_INGOT, "&e我的余额", balanceLore));
        inv.setItem(12, GuiUtil.buildItem(Material.PAPER, "&b转账",
                Arrays.asList("&7使用 &f/shop pay <玩家> <金额> [备注]", "&7转账后对方需领取，过期自动退回")));
        inv.setItem(14, GuiUtil.buildItem(Material.EMERALD, "&a收款记录",
                Arrays.asList("&7待领取: &f" + pending.size() + " &7笔", "&7点击查看收款记录")));
        inv.setItem(16, GuiUtil.buildItem(Material.BOOK, "&6借款",
                Arrays.asList("&7使用 &f/shop loan <玩家> <金额> [备注] &7发起",
                        "&7使用 &f/shop loanrepay <玩家> [金额] &7还款")));
        player.openInventory(inv);
    }

    public void openIncome(Player player, int page) {
        List<Income> records = plugin.getEconomyDataManager().getIncomes(player.getUniqueId());
        int totalPages = Math.max(1, (records.size() + PAGE_SIZE - 1) / PAGE_SIZE);
        if (page < 0) {
            page = 0;
        }
        if (page >= totalPages) {
            page = totalPages - 1;
        }
        GuiHolder holder = new GuiHolder(GuiHolder.GuiType.INCOME_LIST);
        holder.setPage(page);
        Inventory inv = Bukkit.createInventory(holder, 54, TextUtil.color("&8收款记录"));
        holder.setInventory(inv);
        int start = page * PAGE_SIZE;
        for (int i = 0; i < PAGE_SIZE; i++) {
            int index = start + i;
            if (index >= records.size()) {
                break;
            }
            Income income = records.get(index);
            List<String> lore = new ArrayList<>();
            lore.add("&7来源: &f" + income.getSource());
            lore.add("&7金额: &e" + TextUtil.formatMoney(income.getAmount())
                    + " " + plugin.getConfigManager().getCurrencyName());
            if (income.getFrom() != null && !income.getFrom().isEmpty()) {
                lore.add("&7来自: &f" + income.getFrom());
            }
            lore.add("&7时间: &f" + dateFormat.format(new Date(income.getTime())));
            inv.setItem(i, GuiUtil.buildItem(Material.PAPER, "&a+" + TextUtil.formatMoney(income.getAmount()), lore));
        }
        ItemStack filler = GuiUtil.buildFiller();
        for (int i = 45; i < 54; i++) {
            inv.setItem(i, filler);
        }
        if (page > 0) {
            inv.setItem(45, GuiUtil.buildItem(Material.ARROW, "&e上一页",
                    Arrays.asList("&7第 " + page + " / " + totalPages + " 页")));
        }
        inv.setItem(49, GuiUtil.buildItem(Material.PAPER, "&b第 " + (page + 1) + " / " + totalPages + " 页",
                Arrays.asList("&7共 " + records.size() + " 条记录", "&7点击返回经济面板")));
        if (page < totalPages - 1) {
            inv.setItem(53, GuiUtil.buildItem(Material.ARROW, "&e下一页",
                    Arrays.asList("&7第 " + (page + 2) + " / " + totalPages + " 页")));
        }
        player.openInventory(inv);
    }

    public void openSystemShop(Player player) {
        List<io.bukkitcode.crescentshop.system.SystemShopManager.SystemShopItem> items =
                plugin.getSystemShopManager().getDailyItems();
        int rows = Math.max(3, Math.min(6, (items.size() + 8) / 9 + 1));
        int size = rows * 9;
        GuiHolder holder = new GuiHolder(GuiHolder.GuiType.SYSTEM_SHOP);
        Inventory inv = Bukkit.createInventory(holder, size, TextUtil.color("&8系统商店 &7- &6每日刷新"));
        holder.setInventory(inv);
        ItemStack filler = GuiUtil.buildFiller();
        for (int i = size - 9; i < size; i++) {
            inv.setItem(i, filler);
        }
        int slot = 0;
        for (io.bukkitcode.crescentshop.system.SystemShopManager.SystemShopItem item : items) {
            if (slot >= size - 9) {
                break;
            }
            inv.setItem(slot, buildSystemIcon(item));
            slot++;
        }
        if (items.isEmpty()) {
            inv.setItem(13, GuiUtil.buildItem(Material.BARRIER, "&c今日暂无回收物品",
                    Arrays.asList("&7请检查配置或等待次日刷新")));
        }
        int limit = plugin.getSystemShopManager().getDailyRecycleLimit();
        String limitText;
        if (limit < 0) {
            limitText = "无上限";
        } else {
            int used = plugin.getPlayerDataManager().getRecycledToday(player.getUniqueId());
            limitText = Math.max(0, limit - used) + " / " + limit;
        }
        inv.setItem(size - 9, GuiUtil.buildItem(Material.PAPER, "&b今日回收上限",
                Arrays.asList("&7剩余额度: &f" + limitText, "&7每日 0 点自动刷新")));
        inv.setItem(size - 1, GuiUtil.buildItem(Material.ARROW, "&e返回商店列表", Arrays.asList("&7点击返回合集面板")));
        player.openInventory(inv);
    }

    private ItemStack buildShopIcon(Shop shop) {
        String ownerName = Bukkit.getOfflinePlayer(shop.getOwner()).getName();
        if (ownerName == null) {
            ownerName = "未知";
        }
        List<String> lore = new ArrayList<>();
        lore.add("&7店主: &f" + ownerName);
        lore.add("&7商品数: &f" + shop.getItems().size() + " &7/ &f" + shop.getCapacity());
        lore.add("&7界面大小: &f" + shop.getRows() + " 行");
        lore.add("");
        lore.add("&e点击进入商店");
        ItemStack base = null;
        if (shop.getIconData() != null) {
            base = io.bukkitcode.crescentshop.util.ItemSerializer.deserialize(shop.getIconData());
        }
        if (base == null) {
            base = new ItemStack(Material.CHEST);
        }
        base.setAmount(1);
        ItemMeta meta = base.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(TextUtil.color(shop.getName()));
            meta.setLore(TextUtil.color(lore));
            base.setItemMeta(meta);
        }
        return base;
    }

    private ItemStack buildItemIcon(ShopItem shopItem, boolean edit) {
        ItemStack display = shopItem.getItem().clone();
        ItemMeta meta = display.getItemMeta();
        if (meta != null) {
            List<String> lore = meta.hasLore() ? new ArrayList<>(meta.getLore()) : new ArrayList<String>();
            lore.add("");
            lore.add(TextUtil.color("&7单价: &e" + TextUtil.formatMoney(shopItem.getPrice())
                    + " " + plugin.getConfigManager().getCurrencyName()));
            lore.add(TextUtil.color("&7剩余数量: &f" + shopItem.getAmount()));
            lore.add("");
            if (edit) {
                lore.add(TextUtil.color("&e左键 &7= 修改价格"));
                lore.add(TextUtil.color("&e右键 &7= 下架商品"));
            } else {
                lore.add(TextUtil.color("&e点击购买"));
            }
            meta.setLore(lore);
            display.setItemMeta(meta);
        }
        return display;
    }

    private ItemStack buildSystemIcon(io.bukkitcode.crescentshop.system.SystemShopManager.SystemShopItem item) {
        ItemStack display = item.buildDisplayItem();
        ItemMeta meta = display.getItemMeta();
        if (meta != null) {
            double buyPrice = item.getPrice() * plugin.getConfigManager().getSystemShopBuyMultiplier();
            List<String> lore = new ArrayList<>();
            lore.add(TextUtil.color("&7回收价: &a" + TextUtil.formatMoney(item.getPrice())
                    + " " + plugin.getConfigManager().getCurrencyName()));
            lore.add(TextUtil.color("&7出售价: &c" + TextUtil.formatMoney(buyPrice)
                    + " " + plugin.getConfigManager().getCurrencyName()));
            lore.add("");
            lore.add(TextUtil.color("&e左键 &7= 卖给系统（回收）"));
            lore.add(TextUtil.color("&e右键 &7= 从系统购买"));
            meta.setLore(lore);
            display.setItemMeta(meta);
        }
        return display;
    }

    public static int getPageSize() {
        return PAGE_SIZE;
    }
}
