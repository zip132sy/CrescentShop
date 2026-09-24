package io.bukkitcode.crescentshop.command;

import io.bukkitcode.crescentshop.CrescentShop;
import io.bukkitcode.crescentshop.gui.GuiHolder;
import io.bukkitcode.crescentshop.model.EconomyModels.Loan;
import io.bukkitcode.crescentshop.model.EconomyModels.Transfer;
import io.bukkitcode.crescentshop.model.Shop;
import io.bukkitcode.crescentshop.model.ShopItem;
import io.bukkitcode.crescentshop.util.GuiUtil;
import io.bukkitcode.crescentshop.util.ItemSerializer;
import io.bukkitcode.crescentshop.util.MapIconUtil;
import io.bukkitcode.crescentshop.util.TextUtil;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.map.MapView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/** 商店主命令：处理 /shop 的全部子命令，并向 GUI / 聊天输入流程提供操作入口。 */
public class ShopCommand implements CommandExecutor, TabCompleter {

    private final CrescentShop plugin;

    public ShopCommand(CrescentShop plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            Player player = requirePlayer(sender);
            if (player == null) {
                return true;
            }
            if (!player.hasPermission("crescentshop.use")) {
                player.sendMessage(TextUtil.color("&c你没有权限使用商店。"));
                return true;
            }
            plugin.getShopGui().openList(player);
            return true;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "help":
            case "h":
                sendHelp(sender);
                return true;
            case "reload":
            case "r":
                return handleReload(sender);
            default:
                break;
        }

        Player player = requirePlayer(sender);
        if (player == null) {
            sender.sendMessage(TextUtil.color("&c控制台仅支持 /shop reload 与 /shop help。"));
            return true;
        }

        switch (sub) {
            case "create":
            case "c":
            case "new":
                return handleCreate(player, args);
            case "sell":
            case "s":
            case "add":
                return handleSell(player, args);
            case "edit":
            case "e":
            case "mod":
                return handleEdit(player);
            case "seticon":
            case "si":
                return handleSetIcon(player);
            case "setsize":
            case "sz":
            case "size":
                return handleSetSize(player, args);
            case "search":
            case "se":
            case "find":
                return handleSearch(player, args);
            case "sys":
                return handleSystemShop(player);
            case "balance":
            case "b":
            case "bal":
                return handleBalance(player);
            case "eco":
                return handleEco(player);
            case "income":
                return handleIncome(player);
            case "pay":
            case "transfer":
                return handlePay(player, args);
            case "claim":
                return handleClaim(player);
            case "loan":
                return handleLoan(player, args);
            case "loanaccept":
            case "la":
                return handleLoanAccept(player, args);
            case "loanreject":
            case "lr":
                return handleLoanReject(player, args);
            case "loanrepay":
                return handleLoanRepay(player, args);
            case "setprice":
            case "sp":
                return handleSetPrice(player, args);
            default:
                return handleOpenShop(player, args[0]);
        }
    }

    // ==================== 商店 ====================

    private boolean handleCreate(Player player, String[] args) {
        if (!player.hasPermission("crescentshop.create")) {
            player.sendMessage(TextUtil.color("&c你没有权限创建商店。"));
            return true;
        }
        if (args.length < 2) {
            player.sendMessage(TextUtil.color("&e用法: /shop create <商店名>"));
            return true;
        }
        createShop(player, args[1]);
        return true;
    }

    private void createShop(Player player, String name) {
        if (plugin.getShopManager().getShopByOwner(player.getUniqueId()) != null) {
            player.sendMessage(TextUtil.color("&c你已经拥有一个商店了。"));
            return;
        }
        String error = validateShopName(name);
        if (error != null) {
            player.sendMessage(TextUtil.color(error));
            return;
        }
        if (plugin.getShopManager().isNameTaken(name)) {
            player.sendMessage(TextUtil.color("&c商店名已被占用。"));
            return;
        }
        int rows = plugin.getConfigManager().getDefaultShopRows();
        double fee = rows * 9 * plugin.getConfigManager().getSlotCost();
        if (!plugin.getEconomyManager().getProvider().has(player, fee)) {
            player.sendMessage(TextUtil.color("&c创建商店需要 &e" + TextUtil.formatMoney(fee)
                    + " " + plugin.getConfigManager().getCurrencyName() + "&c，余额不足。"));
            return;
        }
        plugin.getEconomyManager().getProvider().withdraw(player, fee);
        Shop shop = plugin.getShopManager().createShop(name, player.getUniqueId(), rows);
        plugin.getShopManager().save();
        player.sendMessage(TextUtil.color("&a商店 " + name + " &a创建成功！已扣除 &e"
                + TextUtil.formatMoney(fee)));
        plugin.getShopGui().openView(player, shop);
    }

    private boolean handleSell(Player player, String[] args) {
        if (!player.hasPermission("crescentshop.sell")) {
            player.sendMessage(TextUtil.color("&c你没有权限上架商品。"));
            return true;
        }
        Shop shop = plugin.getShopManager().getShopByOwner(player.getUniqueId());
        if (shop == null) {
            player.sendMessage(TextUtil.color("&c你还没有商店，先使用 /shop create <名称> 创建。"));
            return true;
        }
        if (args.length < 3) {
            player.sendMessage(TextUtil.color("&e用法: /shop sell <价格> <数量> &7（价格为 0 或 auto 时自动定价）"));
            return true;
        }
        Double price = parsePrice(args[1]);
        if (price == null) {
            player.sendMessage(TextUtil.color("&c价格必须是一个数字（0 或 auto 表示按市场价自动定价）。"));
            return true;
        }
        Integer amount = parsePositiveInt(args[2]);
        if (amount == null) {
            player.sendMessage(TextUtil.color("&c数量必须是一个大于 0 的整数。"));
            return true;
        }
        performSell(player, shop, price, amount);
        return true;
    }

    /**
     * 上架手持物品。price <= 0 时按市场价自动定价。
     * 同时供命令与 GUI 聊天输入流程调用。
     */
    public void performSell(Player player, Shop shop, double price, int amount) {
        if (shop == null || !shop.getOwner().equals(player.getUniqueId())) {
            player.sendMessage(TextUtil.color("&c只能上架到自己拥有的商店。"));
            return;
        }
        ItemStack hand = player.getInventory().getItemInMainHand();
        if (hand == null || hand.getType() == Material.AIR) {
            player.sendMessage(TextUtil.color("&c请先手持要上架的物品。"));
            return;
        }
        if (amount <= 0) {
            player.sendMessage(TextUtil.color("&c数量必须大于 0。"));
            return;
        }
        if (amount > 64) {
            player.sendMessage(TextUtil.color("&c单次上架数量不能超过 64。"));
            return;
        }
        if (amount > hand.getAmount()) {
            player.sendMessage(TextUtil.color("&c数量超过手持数量，当前手持: &f" + hand.getAmount()));
            return;
        }
        boolean autoPrice = price <= 0;
        if (autoPrice) {
            if (plugin.getMarketManager().isExcluded(hand.getType())) {
                player.sendMessage(TextUtil.color("&c潜影盒不参与自动定价，请手动输入价格。"));
                return;
            }
            double market = plugin.getMarketManager().getMarketPrice(hand.getType());
            if (market <= 0) {
                player.sendMessage(TextUtil.color("&c该物品暂无市场价，无法自动定价，请手动输入价格。"));
                return;
            }
            price = Math.max(0.01, Math.round(market
                    * plugin.getConfigManager().getMarketSellRatio() * 100.0) / 100.0);
        }
        if (shop.isFull()) {
            player.sendMessage(TextUtil.color("&c商店已满，无法再上架商品。"));
            return;
        }
        double fee = plugin.getConfigManager().getSlotCost()
                + price * amount * plugin.getConfigManager().getSellTaxRate();
        if (!plugin.getEconomyManager().getProvider().has(player, fee)) {
            player.sendMessage(TextUtil.color("&c上架费用不足，需要 &e" + TextUtil.formatMoney(fee)
                    + " " + plugin.getConfigManager().getCurrencyName()));
            return;
        }
        plugin.getEconomyManager().getProvider().withdraw(player, fee);

        ItemStack template = hand.clone();
        template.setAmount(amount);
        int remain = hand.getAmount() - amount;
        if (remain > 0) {
            hand.setAmount(remain);
            player.getInventory().setItemInMainHand(hand);
        } else {
            player.getInventory().setItemInMainHand(new ItemStack(Material.AIR));
        }

        shop.getItems().add(new ShopItem(template, price, amount));
        plugin.getMarketManager().recordPrice(template.getType(), price);
        plugin.getMarketManager().save();
        plugin.getShopManager().save();
        player.sendMessage(TextUtil.color("&a上架成功！&f" + template.getType().name()
                + " &7x" + amount + " &7单价: &e" + TextUtil.formatMoney(price)
                + (autoPrice ? " &7(自动定价)" : "")
                + " &7上架费用: &e" + TextUtil.formatMoney(fee)));
    }

    private boolean handleEdit(Player player) {
        if (!player.hasPermission("crescentshop.sell")) {
            player.sendMessage(TextUtil.color("&c你没有权限编辑商店。"));
            return true;
        }
        Shop shop = plugin.getShopManager().getShopByOwner(player.getUniqueId());
        if (shop == null) {
            player.sendMessage(TextUtil.color("&c你还没有商店，先使用 /shop create <名称> 创建。"));
            return true;
        }
        plugin.getShopGui().openEdit(player, shop);
        return true;
    }

    private boolean handleSetIcon(Player player) {
        if (!player.hasPermission("crescentshop.sell")) {
            player.sendMessage(TextUtil.color("&c你没有权限设置商店图标。"));
            return true;
        }
        Shop shop = plugin.getShopManager().getShopByOwner(player.getUniqueId());
        if (shop == null) {
            player.sendMessage(TextUtil.color("&c你还没有商店。"));
            return true;
        }
        ItemStack hand = player.getInventory().getItemInMainHand();
        if (hand == null || hand.getType() == Material.AIR) {
            player.sendMessage(TextUtil.color("&c请先手持要作为图标的物品。"));
            return true;
        }
        applyIcon(player, shop, false);
        return true;
    }

    private boolean handleSetSize(Player player, String[] args) {
        if (!player.hasPermission("crescentshop.setsize")) {
            player.sendMessage(TextUtil.color("&c你没有权限设置商店界面大小。"));
            return true;
        }
        Shop shop = plugin.getShopManager().getShopByOwner(player.getUniqueId());
        if (shop == null) {
            player.sendMessage(TextUtil.color("&c你还没有商店。"));
            return true;
        }
        if (args.length < 2) {
            player.sendMessage(TextUtil.color("&e用法: /shop setsize <行数> &7("
                    + plugin.getConfigManager().getMinShopRows() + " - "
                    + plugin.getConfigManager().getMaxShopRows() + ")"));
            return true;
        }
        Integer rows = parsePositiveInt(args[1]);
        if (rows == null) {
            player.sendMessage(TextUtil.color("&c行数必须是一个整数。"));
            return true;
        }
        applySetSize(player, shop, rows);
        return true;
    }

    /** 调整商店界面行数，扩容按格子差额收费。供命令与 GUI 聊天输入流程调用。 */
    public void applySetSize(Player player, Shop shop, int rows) {
        if (shop == null || !shop.getOwner().equals(player.getUniqueId())) {
            player.sendMessage(TextUtil.color("&c只能修改自己拥有的商店。"));
            return;
        }
        int min = plugin.getConfigManager().getMinShopRows();
        int max = plugin.getConfigManager().getMaxShopRows();
        if (rows < min || rows > max) {
            player.sendMessage(TextUtil.color("&c行数必须在 &f" + min + " - " + max + " &c之间。"));
            return;
        }
        if (rows == shop.getRows()) {
            player.sendMessage(TextUtil.color("&e商店已经是 &f" + rows + " &e行，无需修改。"));
            return;
        }
        int newCapacity = rows * 9 - 9;
        if (shop.getItems().size() > newCapacity) {
            player.sendMessage(TextUtil.color("&c当前商品数量（&f" + shop.getItems().size()
                    + "&c）超过新界面容量（&f" + newCapacity + "&c），请先下架部分商品。"));
            return;
        }
        int diffSlots = (rows - shop.getRows()) * 9;
        if (diffSlots > 0) {
            double cost = diffSlots * plugin.getConfigManager().getSlotCost();
            if (!plugin.getEconomyManager().getProvider().has(player, cost)) {
                player.sendMessage(TextUtil.color("&c扩容需要 &e" + TextUtil.formatMoney(cost)
                        + " " + plugin.getConfigManager().getCurrencyName() + "&c，余额不足。"));
                return;
            }
            plugin.getEconomyManager().getProvider().withdraw(player, cost);
            player.sendMessage(TextUtil.color("&7已扣除扩容费用 &e" + TextUtil.formatMoney(cost)));
        }
        shop.setRows(rows);
        plugin.getShopManager().save();
        player.sendMessage(TextUtil.color("&a商店界面已调整为 &f" + rows + " &a行。"));
    }

    /**
     * 用手持物品设置商店图标。地图会复制画面到新地图；大型地图画需 confirmed=true。
     * 供命令、图标选择界面与确认输入流程调用。
     */
    public void applyIcon(Player player, Shop shop, boolean confirmed) {
        if (shop == null || !shop.getOwner().equals(player.getUniqueId())) {
            player.sendMessage(TextUtil.color("&c只能修改自己拥有的商店。"));
            return;
        }
        ItemStack hand = player.getInventory().getItemInMainHand();
        if (hand == null || hand.getType() == Material.AIR) {
            player.sendMessage(TextUtil.color("&c请先手持要作为图标的物品。"));
            return;
        }
        ItemStack icon;
        if (MapIconUtil.isFilledMap(hand)) {
            MapView view = MapIconUtil.getMapView(hand);
            if (view == null) {
                player.sendMessage(TextUtil.color("&c无法读取该地图的画面。"));
                return;
            }
            if (MapIconUtil.isFullMap(view)) {
                if (!plugin.getConfigManager().isAllowLargeMapIcon()) {
                    player.sendMessage(TextUtil.color("&c该地图可能是大型地图画的一部分，服务器已禁止作为图标。"));
                    return;
                }
                if (!confirmed) {
                    plugin.getChatInputManager().awaitConfirmLargeMap(player, shop);
                    player.sendMessage(TextUtil.color("&e该地图可能是大型地图画的一部分，占用服务器资源较大。"));
                    player.sendMessage(TextUtil.color("&e输入 &fconfirm &e确认上传，或 &fcancel &e取消。"));
                    return;
                }
            }
            if (!mapIconSlotsAvailable(shop)) {
                player.sendMessage(TextUtil.color("&c全服地图图标数量已达上限（"
                        + plugin.getConfigManager().getMaxMapIcons() + "）。"));
                return;
            }
            icon = MapIconUtil.copyToNewMap(view, player.getWorld());
            if (icon == null) {
                player.sendMessage(TextUtil.color("&c复制地图画面失败。"));
                return;
            }
        } else {
            icon = hand.clone();
            icon.setAmount(1);
        }
        shop.setIconData(ItemSerializer.serialize(icon));
        plugin.getShopManager().save();
        player.sendMessage(TextUtil.color("&a商店图标已更新。"));
        plugin.getShopGui().openEdit(player, shop);
    }

    /** 用指定材质设置商店图标。供图标列表界面调用。 */
    public void applyIconFromMaterial(Player player, Shop shop, Material material) {
        if (shop == null || !shop.getOwner().equals(player.getUniqueId())) {
            player.sendMessage(TextUtil.color("&c只能修改自己拥有的商店。"));
            return;
        }
        if (material == null || material == Material.AIR) {
            player.sendMessage(TextUtil.color("&c无效的物品。"));
            return;
        }
        shop.setIconData(ItemSerializer.serialize(new ItemStack(material)));
        plugin.getShopManager().save();
        player.sendMessage(TextUtil.color("&a商店图标已更新为 &f" + material.name()));
        plugin.getShopGui().openEdit(player, shop);
    }

    private boolean mapIconSlotsAvailable(Shop target) {
        int limit = plugin.getConfigManager().getMaxMapIcons();
        if (limit <= 0) {
            return true;
        }
        int count = 0;
        for (Shop shop : plugin.getShopManager().getAllShops()) {
            if (shop.getIconData() == null) {
                continue;
            }
            ItemStack icon = ItemSerializer.deserialize(shop.getIconData());
            if (icon != null && icon.getType() == Material.MAP && shop != target) {
                count++;
            }
        }
        return count < limit;
    }

    private boolean handleSearch(Player player, String[] args) {
        if (!player.hasPermission("crescentshop.use")) {
            player.sendMessage(TextUtil.color("&c你没有权限使用商店。"));
            return true;
        }
        if (args.length < 2) {
            player.sendMessage(TextUtil.color("&e用法: /shop search <关键词>"));
            return true;
        }
        String keyword = joinArgs(args, 1);
        List<Shop> results = plugin.getShopManager().search(keyword);
        if (results.isEmpty()) {
            player.sendMessage(TextUtil.color("&c没有找到匹配「&f" + keyword + "&c」的商店或物品。"));
            return true;
        }
        openSearchResult(player, results, keyword);
        return true;
    }

    /** 搜索结果面板：展示匹配关键词的商店（SEARCH_RESULT 类型由 GuiListener 处理点击）。 */
    private void openSearchResult(Player player, List<Shop> shops, String keyword) {
        int rows = Math.max(2, Math.min(6, (shops.size() + 8) / 9 + 1));
        int size = rows * 9;
        GuiHolder holder = new GuiHolder(GuiHolder.GuiType.SEARCH_RESULT);
        holder.setShopList(shops);
        Inventory inv = Bukkit.createInventory(holder, size,
                TextUtil.color("&8搜索: &7") + keyword + TextUtil.color(" &8(" + shops.size() + ")"));
        holder.setInventory(inv);
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
        inv.setItem(size - 9, GuiUtil.buildItem(Material.ARROW, "&e返回商店列表",
                Arrays.asList("&7点击返回合集面板")));
        player.openInventory(inv);
    }

    /** 构建商店图标（与合集面板一致的展示格式）。 */
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
            base = ItemSerializer.deserialize(shop.getIconData());
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

    private boolean handleSystemShop(Player player) {
        if (!player.hasPermission("crescentshop.use")) {
            player.sendMessage(TextUtil.color("&c你没有权限使用系统商店。"));
            return true;
        }
        if (!plugin.getConfigManager().isSystemShopEnabled()) {
            player.sendMessage(TextUtil.color("&c系统商店已关闭。"));
            return true;
        }
        plugin.getShopGui().openSystemShop(player);
        return true;
    }

    private boolean handleOpenShop(Player player, String name) {
        if (!player.hasPermission("crescentshop.use")) {
            player.sendMessage(TextUtil.color("&c你没有权限使用商店。"));
            return true;
        }
        Shop shop = plugin.getShopManager().getShop(name);
        if (shop == null) {
            player.sendMessage(TextUtil.color("&c商店「&f" + name
                    + "&c」不存在，可使用 /shop search " + name + " 搜索。"));
            return true;
        }
        plugin.getShopGui().openView(player, shop);
        return true;
    }

    // ==================== 经济 ====================

    private boolean handleBalance(Player player) {
        if (!player.hasPermission("crescentshop.use")) {
            player.sendMessage(TextUtil.color("&c你没有权限查询余额。"));
            return true;
        }
        double balance = plugin.getEconomyManager().getProvider().getBalance(player);
        player.sendMessage(TextUtil.color("&a余额: &e" + TextUtil.formatMoney(balance)
                + " " + plugin.getConfigManager().getCurrencyName()));
        return true;
    }

    private boolean handleEco(Player player) {
        if (!player.hasPermission("crescentshop.use")) {
            player.sendMessage(TextUtil.color("&c你没有权限使用经济面板。"));
            return true;
        }
        plugin.getShopGui().openEcoMain(player);
        return true;
    }

    private boolean handleIncome(Player player) {
        if (!player.hasPermission("crescentshop.use")) {
            player.sendMessage(TextUtil.color("&c你没有权限查看收款记录。"));
            return true;
        }
        plugin.getShopGui().openIncome(player, 0);
        return true;
    }

    private boolean handlePay(Player player, String[] args) {
        if (!player.hasPermission("crescentshop.use")) {
            player.sendMessage(TextUtil.color("&c你没有权限转账。"));
            return true;
        }
        if (args.length < 3) {
            player.sendMessage(TextUtil.color("&e用法: /shop pay <玩家> <金额> [备注]"));
            return true;
        }
        OfflinePlayer target = resolvePlayer(args[1]);
        if (target == null) {
            player.sendMessage(TextUtil.color("&c找不到玩家「&f" + args[1] + "&c」。"));
            return true;
        }
        if (target.getUniqueId().equals(player.getUniqueId())) {
            player.sendMessage(TextUtil.color("&c不能给自己转账。"));
            return true;
        }
        Double amount = parsePositiveDouble(args[2]);
        if (amount == null) {
            player.sendMessage(TextUtil.color("&c金额必须是一个大于 0 的数字。"));
            return true;
        }
        if (!plugin.getEconomyManager().getProvider().has(player, amount)) {
            player.sendMessage(TextUtil.color("&c余额不足。"));
            return true;
        }
        String note = args.length > 3 ? joinArgs(args, 3) : "";
        String data = target.getUniqueId() + "|" + amount + "|" + note;
        plugin.getChatInputManager().awaitTransferConfirm(player, data);
        player.sendMessage(TextUtil.color("&e即将向 &f" + target.getName() + " &e转账 &f"
                + TextUtil.formatMoney(amount) + " " + plugin.getConfigManager().getCurrencyName()
                + (note.isEmpty() ? "" : "&7（备注: " + note + "）")));
        player.sendMessage(TextUtil.color("&e输入 &fconfirm &e确认转账，或 &fcancel &e取消。"));
        return true;
    }

    private boolean handleClaim(Player player) {
        if (!player.hasPermission("crescentshop.use")) {
            player.sendMessage(TextUtil.color("&c你没有权限领取转账。"));
            return true;
        }
        List<Transfer> pending = plugin.getEconomyDataManager().getPendingTransfers(player.getUniqueId());
        if (pending.isEmpty()) {
            player.sendMessage(TextUtil.color("&e你没有待领取的转账。"));
            return true;
        }
        double total = 0;
        int count = 0;
        for (Transfer transfer : new ArrayList<>(pending)) {
            if (plugin.getEconomyDataManager().claimTransfer(transfer)) {
                total += transfer.getAmount();
                count++;
            }
        }
        player.sendMessage(TextUtil.color("&a已领取 &f" + count + " &a笔转账，共计 &e"
                + TextUtil.formatMoney(total) + " " + plugin.getConfigManager().getCurrencyName()));
        plugin.getEconomyDataManager().checkDebt(player.getUniqueId());
        return true;
    }

    private boolean handleLoan(Player player, String[] args) {
        if (!player.hasPermission("crescentshop.use")) {
            player.sendMessage(TextUtil.color("&c你没有权限发起借款。"));
            return true;
        }
        if (args.length < 3) {
            player.sendMessage(TextUtil.color("&e用法: /shop loan <玩家> <金额> [备注]"));
            return true;
        }
        OfflinePlayer target = resolvePlayer(args[1]);
        if (target == null) {
            player.sendMessage(TextUtil.color("&c找不到玩家「&f" + args[1] + "&c」。"));
            return true;
        }
        if (target.getUniqueId().equals(player.getUniqueId())) {
            player.sendMessage(TextUtil.color("&c不能向自己借款。"));
            return true;
        }
        Double amount = parsePositiveDouble(args[2]);
        if (amount == null) {
            player.sendMessage(TextUtil.color("&c金额必须是一个大于 0 的数字。"));
            return true;
        }
        int active = plugin.getEconomyDataManager().getActiveLoansOf(player.getUniqueId()).size();
        if (active >= plugin.getConfigManager().getMaxLoansPerPlayer()) {
            player.sendMessage(TextUtil.color("&c你当前的借款数量已达上限（"
                    + plugin.getConfigManager().getMaxLoansPerPlayer() + "）。"));
            return true;
        }
        if (plugin.getEconomyDataManager().findPendingLoan(
                player.getUniqueId(), target.getUniqueId()) != null) {
            player.sendMessage(TextUtil.color("&c你已向对方发起过借款申请，请等待处理。"));
            return true;
        }
        String note = args.length > 3 ? joinArgs(args, 3) : "";
        plugin.getEconomyDataManager().createLoan(player.getUniqueId(), target.getUniqueId(), amount, note);
        player.sendMessage(TextUtil.color("&a已向 &f" + target.getName() + " &a发起借款申请: &e"
                + TextUtil.formatMoney(amount) + " " + plugin.getConfigManager().getCurrencyName()
                + (note.isEmpty() ? "" : "&7（备注: " + note + "）")));
        Player lender = target.getPlayer();
        if (lender != null && lender.isOnline()) {
            lender.sendMessage(TextUtil.color("&e玩家 &f" + player.getName() + " &e向你发起借款申请: &f"
                    + TextUtil.formatMoney(amount) + " " + plugin.getConfigManager().getCurrencyName()
                    + (note.isEmpty() ? "" : "&7（备注: " + note + "）")));
            lender.sendMessage(TextUtil.color("&e同意: &f/shop loanaccept " + player.getName()
                    + " <天数> <利率> &7| &e拒绝: &f/shop loanreject " + player.getName()));
        }
        return true;
    }

    private boolean handleLoanAccept(Player player, String[] args) {
        if (!player.hasPermission("crescentshop.use")) {
            player.sendMessage(TextUtil.color("&c你没有权限处理借款。"));
            return true;
        }
        if (args.length < 4) {
            player.sendMessage(TextUtil.color("&e用法: /shop loanaccept <玩家> <天数> <利率>"));
            return true;
        }
        OfflinePlayer borrower = resolvePlayer(args[1]);
        if (borrower == null) {
            player.sendMessage(TextUtil.color("&c找不到玩家「&f" + args[1] + "&c」。"));
            return true;
        }
        Integer days = parsePositiveInt(args[2]);
        if (days == null || days > 365) {
            player.sendMessage(TextUtil.color("&c天数必须是 1 - 365 之间的整数。"));
            return true;
        }
        Double rate = parseNonNegativeDouble(args[3]);
        if (rate == null) {
            player.sendMessage(TextUtil.color("&c利率必须是一个不小于 0 的数字（如 0.05 表示 5%）。"));
            return true;
        }
        Loan loan = plugin.getEconomyDataManager().findPendingLoan(
                borrower.getUniqueId(), player.getUniqueId());
        if (loan == null) {
            player.sendMessage(TextUtil.color("&c该玩家没有向你发起借款申请。"));
            return true;
        }
        if (!plugin.getEconomyDataManager().acceptLoan(loan, days, rate)) {
            player.sendMessage(TextUtil.color("&c放款失败，你的余额不足。"));
            return true;
        }
        player.sendMessage(TextUtil.color("&a已放款 &f" + TextUtil.formatMoney(loan.getAmount())
                + " &a给 &f" + borrower.getName() + "&a，期限 &f" + days + " &a天，利率 &f" + rate));
        Player target = borrower.getPlayer();
        if (target != null && target.isOnline()) {
            target.sendMessage(TextUtil.color("&a你的借款申请已通过，收到 &f"
                    + TextUtil.formatMoney(loan.getAmount()) + " &a，期限 &f" + days
                    + " &a天，到期应还 &f" + TextUtil.formatMoney(loan.getTotalDue())));
        }
        return true;
    }

    private boolean handleLoanReject(Player player, String[] args) {
        if (!player.hasPermission("crescentshop.use")) {
            player.sendMessage(TextUtil.color("&c你没有权限处理借款。"));
            return true;
        }
        if (args.length < 2) {
            player.sendMessage(TextUtil.color("&e用法: /shop loanreject <玩家>"));
            return true;
        }
        OfflinePlayer borrower = resolvePlayer(args[1]);
        if (borrower == null) {
            player.sendMessage(TextUtil.color("&c找不到玩家「&f" + args[1] + "&c」。"));
            return true;
        }
        Loan loan = plugin.getEconomyDataManager().findPendingLoan(
                borrower.getUniqueId(), player.getUniqueId());
        if (loan == null) {
            player.sendMessage(TextUtil.color("&c该玩家没有向你发起借款申请。"));
            return true;
        }
        plugin.getEconomyDataManager().rejectLoan(loan);
        player.sendMessage(TextUtil.color("&a已拒绝 &f" + borrower.getName() + " &a的借款申请。"));
        Player target = borrower.getPlayer();
        if (target != null && target.isOnline()) {
            target.sendMessage(TextUtil.color("&c你的借款申请被 &f" + player.getName() + " &c拒绝。"));
        }
        return true;
    }

    private boolean handleLoanRepay(Player player, String[] args) {
        if (!player.hasPermission("crescentshop.use")) {
            player.sendMessage(TextUtil.color("&c你没有权限还款。"));
            return true;
        }
        if (args.length < 2) {
            player.sendMessage(TextUtil.color("&e用法: /shop loanrepay <玩家> [金额]"));
            return true;
        }
        OfflinePlayer lender = resolvePlayer(args[1]);
        if (lender == null) {
            player.sendMessage(TextUtil.color("&c找不到玩家「&f" + args[1] + "&c」。"));
            return true;
        }
        Loan loan = findActiveLoan(player.getUniqueId(), lender.getUniqueId());
        if (loan == null) {
            player.sendMessage(TextUtil.color("&c你没有欠该玩家的借款。"));
            return true;
        }
        double amount;
        if (args.length >= 3) {
            Double parsed = parsePositiveDouble(args[2]);
            if (parsed == null) {
                player.sendMessage(TextUtil.color("&c金额必须是一个大于 0 的数字。"));
                return true;
            }
            amount = parsed;
        } else {
            amount = loan.getRemaining();
        }
        double pay = Math.min(amount, loan.getRemaining());
        if (!plugin.getEconomyDataManager().repayLoan(loan, amount)) {
            player.sendMessage(TextUtil.color("&c还款失败，你的余额不足（需要 &e"
                    + TextUtil.formatMoney(pay) + "&c）。"));
            return true;
        }
        player.sendMessage(TextUtil.color("&a已还款 &f" + TextUtil.formatMoney(pay)
                + " &a给 &f" + lender.getName()
                + (loan.getRemaining() <= 0.001 ? "&a，该笔借款已结清。"
                        : "&a，剩余应还 &f" + TextUtil.formatMoney(loan.getRemaining()))));
        Player target = lender.getPlayer();
        if (target != null && target.isOnline()) {
            target.sendMessage(TextUtil.color("&a收到 &f" + player.getName() + " &a的还款 &f"
                    + TextUtil.formatMoney(pay)));
        }
        plugin.getEconomyDataManager().checkDebt(player.getUniqueId());
        return true;
    }

    private Loan findActiveLoan(UUID borrower, UUID lender) {
        for (Loan loan : plugin.getEconomyDataManager().getActiveLoansOf(borrower)) {
            if (loan.getLender().equals(lender)) {
                return loan;
            }
        }
        return null;
    }

    // ==================== 管理 ====================

    private boolean handleSetPrice(Player player, String[] args) {
        if (!player.hasPermission("crescentshop.admin")) {
            player.sendMessage(TextUtil.color("&c你没有权限设置默认价格。"));
            return true;
        }
        if (args.length < 2) {
            player.sendMessage(TextUtil.color("&e用法: /shop setprice <价格> &7（手持物品）"));
            return true;
        }
        Double price = parsePositiveDouble(args[1]);
        if (price == null) {
            player.sendMessage(TextUtil.color("&c价格必须是一个大于 0 的数字。"));
            return true;
        }
        ItemStack hand = player.getInventory().getItemInMainHand();
        if (hand == null || hand.getType() == Material.AIR) {
            player.sendMessage(TextUtil.color("&c请先手持要设置默认价的物品。"));
            return true;
        }
        plugin.getPriceTable().setPrice(hand.getType(), price);
        plugin.getPriceTable().save();
        player.sendMessage(TextUtil.color("&a已将 &f" + hand.getType().name()
                + " &a的默认价格设置为 &e" + TextUtil.formatMoney(price)));
        return true;
    }

    private boolean handleReload(CommandSender sender) {
        if (!sender.hasPermission("crescentshop.admin")) {
            sender.sendMessage(TextUtil.color("&c你没有权限重载插件。"));
            return true;
        }
        plugin.reloadAll();
        sender.sendMessage(TextUtil.color("&aCrescentShop 配置与数据已重载。"));
        return true;
    }

    // ==================== 帮助 ====================

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(TextUtil.color("&6========== &eCrescentShop 帮助 &6=========="));
        if (!(sender instanceof Player)) {
            sender.sendMessage(TextUtil.color("&e/shop reload &7- 重载配置与数据"));
            return;
        }
        Player player = (Player) sender;
        sendHelpLine(player, "/shop", "打开商店合集面板");
        sendHelpLine(player, "/shop <商店名>", "直接进入指定商店");
        sendHelpLine(player, "/shop create <名称>", "创建商店");
        sendHelpLine(player, "/shop sell <价格> <数量>", "上架手持物品（价格 0/auto 自动定价）");
        sendHelpLine(player, "/shop edit", "编辑自己的商店");
        sendHelpLine(player, "/shop seticon", "设置商店图标（手持物品）");
        sendHelpLine(player, "/shop setsize <行数>", "设置商店界面大小");
        sendHelpLine(player, "/shop search <关键词>", "搜索商店或物品");
        sendHelpLine(player, "/shop sys", "打开系统商店");
        sendHelpLine(player, "/shop balance", "查询余额");
        sendHelpLine(player, "/shop eco", "打开经济面板");
        sendHelpLine(player, "/shop income", "查看收款记录");
        sendHelpLine(player, "/shop pay <玩家> <金额> [备注]", "转账给玩家");
        sendHelpLine(player, "/shop claim", "领取待收款");
        sendHelpLine(player, "/shop loan <玩家> <金额> [备注]", "发起借款申请");
        sendHelpLine(player, "/shop loanaccept <玩家> <天数> <利率>", "同意借款");
        sendHelpLine(player, "/shop loanreject <玩家>", "拒绝借款");
        sendHelpLine(player, "/shop loanrepay <玩家> [金额]", "提前还款");
        if (player.hasPermission("crescentshop.admin")) {
            sendHelpLine(player, "/shop setprice <价格>", "设置手持物品默认价");
            sendHelpLine(player, "/shop reload", "重载配置与数据");
        }
        player.sendMessage(TextUtil.color("&7点击命令可自动填充到聊天栏"));
    }

    private void sendHelpLine(Player player, String command, String description) {
        TextComponent line = new TextComponent(TextUtil.color("&e" + command + " "));
        line.addExtra(TextUtil.buildClickable("&7" + description, command + " ",
                "&7点击填充: " + command));
        player.spigot().sendMessage(line);
    }

    // ==================== Tab 补全 ====================

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> result = new ArrayList<>();
        if (args.length == 1) {
            List<String> candidates = new ArrayList<>(Arrays.asList(
                    "help", "create", "sell", "edit", "seticon", "setsize", "search", "sys",
                    "balance", "eco", "income", "pay", "claim", "loan", "loanaccept",
                    "loanreject", "loanrepay"));
            if (sender.hasPermission("crescentshop.admin")) {
                candidates.add("setprice");
                candidates.add("reload");
            }
            if (sender instanceof Player) {
                for (Shop shop : plugin.getShopManager().getAllShops()) {
                    candidates.add(TextUtil.stripColor(shop.getName()));
                }
            }
            String prefix = args[0].toLowerCase(Locale.ROOT);
            for (String candidate : candidates) {
                if (candidate.toLowerCase(Locale.ROOT).startsWith(prefix)) {
                    result.add(candidate);
                }
            }
            return result;
        }
        if (args.length == 2) {
            String sub = args[0].toLowerCase(Locale.ROOT);
            if (sub.equals("pay") || sub.equals("transfer") || sub.equals("loan")
                    || sub.equals("loanaccept") || sub.equals("la")
                    || sub.equals("loanreject") || sub.equals("lr") || sub.equals("loanrepay")) {
                String prefix = args[1].toLowerCase(Locale.ROOT);
                for (Player online : Bukkit.getOnlinePlayers()) {
                    if (online.getName().toLowerCase(Locale.ROOT).startsWith(prefix)) {
                        result.add(online.getName());
                    }
                }
            }
            return result;
        }
        return result;
    }

    // ==================== 工具方法 ====================

    private Player requirePlayer(CommandSender sender) {
        if (sender instanceof Player) {
            return (Player) sender;
        }
        return null;
    }

    private String validateShopName(String name) {
        if (name == null || name.trim().isEmpty()) {
            return "&c商店名不能为空。";
        }
        if (TextUtil.stripColor(name).length() > plugin.getConfigManager().getMaxShopNameLength()) {
            return "&c商店名过长。";
        }
        if (name.contains(" ") || name.contains(".") || name.contains("/") || name.contains("\\")) {
            return "&c商店名不能包含空格、点号或斜杠。";
        }
        return null;
    }

    @SuppressWarnings("deprecation")
    private OfflinePlayer resolvePlayer(String name) {
        Player online = Bukkit.getPlayerExact(name);
        if (online != null) {
            return online;
        }
        OfflinePlayer offline = Bukkit.getOfflinePlayer(name);
        return offline.hasPlayedBefore() ? offline : null;
    }

    private Double parsePrice(String input) {
        if (input.equalsIgnoreCase("auto")) {
            return 0.0;
        }
        try {
            return Double.parseDouble(input);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Double parsePositiveDouble(String input) {
        try {
            double value = Double.parseDouble(input);
            return value > 0 ? value : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Double parseNonNegativeDouble(String input) {
        try {
            double value = Double.parseDouble(input);
            return value >= 0 ? value : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Integer parsePositiveInt(String input) {
        try {
            int value = Integer.parseInt(input);
            return value > 0 ? value : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String joinArgs(String[] args, int start) {
        StringBuilder builder = new StringBuilder();
        for (int i = start; i < args.length; i++) {
            if (builder.length() > 0) {
                builder.append(' ');
            }
            builder.append(args[i]);
        }
        return builder.toString();
    }
}
