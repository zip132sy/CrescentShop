package io.bukkitcode.crescentshop.data;

import io.bukkitcode.crescentshop.CrescentShop;
import io.bukkitcode.crescentshop.model.Shop;
import io.bukkitcode.crescentshop.model.ShopItem;
import io.bukkitcode.crescentshop.system.SystemShopManager;
import io.bukkitcode.crescentshop.util.TextUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** 聊天输入管理器。 */
public class ChatInputManager {

    public enum InputType {
        BUY_SHOP_ITEM, SYSTEM_SELL, SYSTEM_BUY, CREATE_SHOP, RENAME_SHOP,
        EDIT_PRICE, SELL_ITEM, SET_SIZE, CONFIRM_LARGE_MAP, ICON_SEARCH,
        TRANSFER_CONFIRM, LOAN_DAYS
    }

    public static class PendingInput {
        private final InputType type;
        private final Shop shop;
        private final ShopItem shopItem;
        private final SystemShopManager.SystemShopItem systemItem;
        private final int itemIndex;
        private final String extra;

        public PendingInput(InputType type, Shop shop, ShopItem shopItem,
                            SystemShopManager.SystemShopItem systemItem, int itemIndex, String extra) {
            this.type = type;
            this.shop = shop;
            this.shopItem = shopItem;
            this.systemItem = systemItem;
            this.itemIndex = itemIndex;
            this.extra = extra;
        }

        public InputType getType() {
            return type;
        }

        public Shop getShop() {
            return shop;
        }

        public ShopItem getShopItem() {
            return shopItem;
        }

        public SystemShopManager.SystemShopItem getSystemItem() {
            return systemItem;
        }

        public int getItemIndex() {
            return itemIndex;
        }

        public String getExtra() {
            return extra;
        }
    }

    private final CrescentShop plugin;
    private final Map<UUID, PendingInput> pending = new HashMap<>();

    public ChatInputManager(CrescentShop plugin) {
        this.plugin = plugin;
    }

    public void awaitQuantity(Player player, Shop shop, ShopItem shopItem) {
        put(player, new PendingInput(InputType.BUY_SHOP_ITEM, shop, shopItem, null, -1, null));
    }

    public void awaitSystemSell(Player player, SystemShopManager.SystemShopItem item) {
        put(player, new PendingInput(InputType.SYSTEM_SELL, null, null, item, -1, null));
    }

    public void awaitSystemBuy(Player player, SystemShopManager.SystemShopItem item) {
        put(player, new PendingInput(InputType.SYSTEM_BUY, null, null, item, -1, null));
    }

    public void awaitCreateShop(Player player) {
        put(player, new PendingInput(InputType.CREATE_SHOP, null, null, null, -1, null));
    }

    public void awaitRenameShop(Player player, Shop shop) {
        put(player, new PendingInput(InputType.RENAME_SHOP, shop, null, null, -1, null));
    }

    public void awaitEditPrice(Player player, Shop shop, int itemIndex) {
        put(player, new PendingInput(InputType.EDIT_PRICE, shop, null, null, itemIndex, null));
    }

    public void awaitSellItem(Player player, Shop shop) {
        put(player, new PendingInput(InputType.SELL_ITEM, shop, null, null, -1, null));
    }

    public void awaitSetSize(Player player, Shop shop) {
        put(player, new PendingInput(InputType.SET_SIZE, shop, null, null, -1, null));
    }

    public void awaitConfirmLargeMap(Player player, Shop shop) {
        put(player, new PendingInput(InputType.CONFIRM_LARGE_MAP, shop, null, null, -1, null));
    }

    public void awaitIconSearch(Player player, Shop shop) {
        put(player, new PendingInput(InputType.ICON_SEARCH, shop, null, null, -1, null));
    }

    public void awaitTransferConfirm(Player player, String data) {
        put(player, new PendingInput(InputType.TRANSFER_CONFIRM, null, null, null, -1, data));
    }

    public void awaitLoanDays(Player player, String data) {
        put(player, new PendingInput(InputType.LOAN_DAYS, null, null, null, -1, data));
    }

    private void put(Player player, PendingInput input) {
        pending.put(player.getUniqueId(), input);
    }

    public boolean isPending(UUID uuid) {
        return pending.containsKey(uuid);
    }

    public void cancel(UUID uuid) {
        pending.remove(uuid);
    }

    public boolean handleInput(Player player, String input) {
        PendingInput context = pending.remove(player.getUniqueId());
        if (context == null) {
            return false;
        }
        if (input.equalsIgnoreCase("cancel")) {
            player.sendMessage(TextUtil.color("&7已取消操作。"));
            return true;
        }
        switch (context.getType()) {
            case CREATE_SHOP:
                handleCreateShop(player, input);
                return true;
            case RENAME_SHOP:
                handleRenameShop(player, context.getShop(), input);
                return true;
            case EDIT_PRICE:
                handleEditPrice(player, context.getShop(), context.getItemIndex(), input);
                return true;
            case SELL_ITEM:
                handleSellItem(player, context.getShop(), input);
                return true;
            case SET_SIZE:
                handleSetSize(player, context.getShop(), input);
                return true;
            case CONFIRM_LARGE_MAP:
                handleConfirmLargeMap(player, context.getShop(), input);
                return true;
            case ICON_SEARCH:
                handleIconSearch(player, context.getShop(), input);
                return true;
            case TRANSFER_CONFIRM:
                handleTransferConfirm(player, context.getExtra(), input);
                return true;
            case LOAN_DAYS:
                handleLoanDays(player, context.getExtra(), input);
                return true;
            default:
                break;
        }
        int amount;
        try {
            amount = Integer.parseInt(input.trim());
        } catch (NumberFormatException e) {
            player.sendMessage(TextUtil.color("&c请输入一个有效的整数。"));
            return true;
        }
        if (amount <= 0) {
            player.sendMessage(TextUtil.color("&c数量必须大于 0。"));
            return true;
        }
        switch (context.getType()) {
            case BUY_SHOP_ITEM:
                buyShopItem(player, context.getShop(), context.getShopItem(), amount);
                break;
            case SYSTEM_SELL:
                sellToSystem(player, context.getSystemItem(), amount);
                break;
            case SYSTEM_BUY:
                buyFromSystem(player, context.getSystemItem(), amount);
                break;
            default:
                break;
        }
        return true;
    }

    private void handleCreateShop(Player player, String name) {
        if (!player.hasPermission("crescentshop.create")) {
            player.sendMessage(TextUtil.color("&c你没有权限创建商店。"));
            return;
        }
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
            player.sendMessage(TextUtil.color("&c创建商店需要 &e" + TextUtil.formatMoney(fee) + "&c，余额不足。"));
            return;
        }
        plugin.getEconomyManager().getProvider().withdraw(player, fee);
        Shop shop = plugin.getShopManager().createShop(name, player.getUniqueId(), rows);
        plugin.getShopManager().save();
        player.sendMessage(TextUtil.color("&a商店 " + name + " &a创建成功！已扣除 &e" + TextUtil.formatMoney(fee)));
        plugin.getShopGui().openView(player, shop);
    }

    private void handleRenameShop(Player player, Shop shop, String newName) {
        if (shop == null) {
            return;
        }
        String error = validateShopName(newName);
        if (error != null) {
            player.sendMessage(TextUtil.color(error));
            return;
        }
        if (!plugin.getShopManager().renameShop(shop, newName)) {
            player.sendMessage(TextUtil.color("&c商店名已被占用。"));
            return;
        }
        plugin.getShopManager().save();
        player.sendMessage(TextUtil.color("&a商店已重命名为 " + newName));
        plugin.getShopGui().openEdit(player, shop);
    }

    private void handleEditPrice(Player player, Shop shop, int itemIndex, String input) {
        if (shop == null || itemIndex < 0 || itemIndex >= shop.getItems().size()) {
            player.sendMessage(TextUtil.color("&c商品不存在。"));
            return;
        }
        double price;
        try {
            price = Double.parseDouble(input.trim());
        } catch (NumberFormatException e) {
            player.sendMessage(TextUtil.color("&c价格必须是一个数字。"));
            return;
        }
        if (price <= 0) {
            player.sendMessage(TextUtil.color("&c价格必须大于 0。"));
            return;
        }
        shop.getItems().get(itemIndex).setPrice(price);
        plugin.getShopManager().save();
        player.sendMessage(TextUtil.color("&a价格已修改为 &e" + TextUtil.formatMoney(price)));
        plugin.getShopGui().openEdit(player, shop);
    }

    private void handleSellItem(Player player, Shop shop, String input) {
        if (shop == null) {
            return;
        }
        String[] parts = input.trim().split("\\s+");
        if (parts.length < 2) {
            player.sendMessage(TextUtil.color("&c请输入「价格 数量」，例如: 10 2"));
            return;
        }
        double price;
        int amount;
        try {
            price = Double.parseDouble(parts[0]);
            amount = Integer.parseInt(parts[1]);
        } catch (NumberFormatException e) {
            player.sendMessage(TextUtil.color("&c价格或数量格式错误。"));
            return;
        }
        plugin.getShopCommand().performSell(player, shop, price, amount);
        plugin.getShopGui().openEdit(player, shop);
    }

    private void handleSetSize(Player player, Shop shop, String input) {
        if (shop == null) {
            return;
        }
        int rows;
        try {
            rows = Integer.parseInt(input.trim());
        } catch (NumberFormatException e) {
            player.sendMessage(TextUtil.color("&c行数必须是一个整数。"));
            return;
        }
        plugin.getShopCommand().applySetSize(player, shop, rows);
        plugin.getShopGui().openEdit(player, shop);
    }

    private void handleConfirmLargeMap(Player player, Shop shop, String input) {
        if (shop == null) {
            return;
        }
        if (input.equalsIgnoreCase("confirm")) {
            plugin.getShopCommand().applyIcon(player, shop, true);
        } else {
            player.sendMessage(TextUtil.color("&7已取消设置图标。"));
        }
        plugin.getShopGui().openEdit(player, shop);
    }

    private void handleIconSearch(Player player, Shop shop, String keyword) {
        if (shop == null) {
            return;
        }
        java.util.List<Material> results = io.bukkitcode.crescentshop.util.ItemCategory
                .search(keyword, plugin.getConfigManager().isFilterTechnicalItems());
        if (results.isEmpty()) {
            player.sendMessage(TextUtil.color("&c没有找到匹配的物品。"));
            plugin.getShopGui().openIconCategory(player, shop);
            return;
        }
        plugin.getShopGui().openIconList(player, shop, results, 0, -1, keyword);
    }

    private void handleTransferConfirm(Player player, String data, String input) {
        if (!input.equalsIgnoreCase("confirm")) {
            player.sendMessage(TextUtil.color("&7已取消转账。"));
            return;
        }
        String[] parts = data.split("\\|", -1);
        if (parts.length < 3) {
            return;
        }
        try {
            UUID receiver = UUID.fromString(parts[0]);
            double amount = Double.parseDouble(parts[1]);
            String note = parts[2];
            plugin.getEconomyManager().getProvider().withdraw(player, amount);
            plugin.getEconomyDataManager().createTransfer(player.getUniqueId(), receiver, amount, note);
            player.sendMessage(TextUtil.color("&a转账成功！&f" + TextUtil.formatMoney(amount)
                    + " &a已进入待领取状态。"));
            Player target = Bukkit.getPlayer(receiver);
            if (target != null && target.isOnline()) {
                target.sendMessage(TextUtil.color("&e你收到一笔转账 &f"
                        + TextUtil.formatMoney(amount) + " &e，使用 &f/shop claim &e领取。"));
            }
        } catch (Exception e) {
            player.sendMessage(TextUtil.color("&c转账失败。"));
        }
    }

    private void handleLoanDays(Player player, String data, String input) {
        String[] parts = data.split("\\|", -1);
        if (parts.length < 4) {
            return;
        }
        int days;
        try {
            days = Integer.parseInt(input.trim());
        } catch (NumberFormatException e) {
            player.sendMessage(TextUtil.color("&c天数必须是一个整数。"));
            return;
        }
        if (days <= 0 || days > 365) {
            player.sendMessage(TextUtil.color("&c天数必须在 1 - 365 之间。"));
            return;
        }
        try {
            UUID borrower = UUID.fromString(parts[0]);
            double amount = Double.parseDouble(parts[1]);
            double rate = Double.parseDouble(parts[2]);
            io.bukkitcode.crescentshop.model.EconomyModels.Loan loan =
                    plugin.getEconomyDataManager().findPendingLoan(borrower, player.getUniqueId());
            if (loan == null) {
                player.sendMessage(TextUtil.color("&c该借款申请已不存在。"));
                return;
            }
            if (!plugin.getEconomyDataManager().acceptLoan(loan, days, rate)) {
                player.sendMessage(TextUtil.color("&c放款失败，你的余额不足。"));
                return;
            }
            player.sendMessage(TextUtil.color("&a已放款 &f" + TextUtil.formatMoney(amount)
                    + " &a给借款方，期限 &f" + days + " &a天。"));
            Player target = Bukkit.getPlayer(borrower);
            if (target != null && target.isOnline()) {
                target.sendMessage(TextUtil.color("&a你的借款申请已通过，收到 &f"
                        + TextUtil.formatMoney(amount) + " &a，期限 &f" + days + " &a天。"));
            }
        } catch (Exception e) {
            player.sendMessage(TextUtil.color("&c放款失败。"));
        }
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

    private void buyShopItem(Player player, Shop shop, ShopItem shopItem, int amount) {
        if (shop == null || shopItem == null) {
            return;
        }
        if (shop.getOwner().equals(player.getUniqueId())) {
            player.sendMessage(TextUtil.color("&c你不能购买自己商店的商品。"));
            return;
        }
        if (amount > shopItem.getAmount()) {
            player.sendMessage(TextUtil.color("&c库存不足，当前库存: &f" + shopItem.getAmount()));
            return;
        }
        double total = shopItem.getPrice() * amount;
        if (!plugin.getEconomyManager().getProvider().has(player, total)) {
            player.sendMessage(TextUtil.color("&c余额不足，需要 &e" + TextUtil.formatMoney(total)));
            return;
        }
        if (!hasInventorySpace(player, shopItem.getItem(), amount)) {
            player.sendMessage(TextUtil.color("&c你的背包空间不足。"));
            return;
        }
        plugin.getEconomyManager().getProvider().withdraw(player, total);
        plugin.getEconomyManager().getProvider().deposit(Bukkit.getOfflinePlayer(shop.getOwner()), total);
        plugin.getEconomyDataManager().recordIncome(shop.getOwner(), "商店售出", total, player.getName());
        ItemStack give = shopItem.getItem().clone();
        give.setAmount(amount);
        player.getInventory().addItem(give);
        shopItem.setAmount(shopItem.getAmount() - amount);
        if (shopItem.getAmount() <= 0) {
            shop.getItems().remove(shopItem);
        }
        plugin.getShopManager().save();
        player.sendMessage(TextUtil.color("&a购买成功！花费 &e" + TextUtil.formatMoney(total)));
        plugin.getShopGui().openView(player, shop);
    }

    private void sellToSystem(Player player, SystemShopManager.SystemShopItem item, int amount) {
        if (item == null) {
            return;
        }
        int limit = plugin.getSystemShopManager().getDailyRecycleLimit();
        if (limit >= 0) {
            int used = plugin.getPlayerDataManager().getRecycledToday(player.getUniqueId());
            int remaining = limit - used;
            if (remaining <= 0) {
                player.sendMessage(TextUtil.color("&c你今日的回收额度已用完。"));
                return;
            }
            if (amount > remaining) {
                player.sendMessage(TextUtil.color("&c今日回收额度不足，剩余: &f" + remaining));
                return;
            }
        }
        if (!hasItem(player, item.getMaterial(), amount)) {
            player.sendMessage(TextUtil.color("&c你身上没有足够的该物品。"));
            return;
        }
        removeItem(player, item.getMaterial(), amount);
        double total = item.getPrice() * amount;
        plugin.getEconomyManager().getProvider().deposit(player, total);
        plugin.getEconomyDataManager().recordIncome(player.getUniqueId(), "系统回收", total, "系统商店");
        plugin.getPlayerDataManager().addRecycledToday(player.getUniqueId(), amount);
        player.sendMessage(TextUtil.color("&a回收成功！获得 &e" + TextUtil.formatMoney(total)));
        plugin.getShopGui().openSystemShop(player);
    }

    private void buyFromSystem(Player player, SystemShopManager.SystemShopItem item, int amount) {
        if (item == null) {
            return;
        }
        double unitPrice = item.getPrice() * plugin.getConfigManager().getSystemShopBuyMultiplier();
        double total = unitPrice * amount;
        if (!plugin.getEconomyManager().getProvider().has(player, total)) {
            player.sendMessage(TextUtil.color("&c余额不足，需要 &e" + TextUtil.formatMoney(total)));
            return;
        }
        if (!hasInventorySpace(player, item.buildSellItem(1), amount)) {
            player.sendMessage(TextUtil.color("&c你的背包空间不足。"));
            return;
        }
        plugin.getEconomyManager().getProvider().withdraw(player, total);
        player.getInventory().addItem(item.buildSellItem(amount));
        player.sendMessage(TextUtil.color("&a购买成功！花费 &e" + TextUtil.formatMoney(total)));
        plugin.getShopGui().openSystemShop(player);
    }

    private boolean hasInventorySpace(Player player, ItemStack item, int amount) {
        int remaining = amount;
        int maxStack = item.getMaxStackSize();
        for (ItemStack content : player.getInventory().getStorageContents()) {
            if (content == null || content.getType() == Material.AIR) {
                remaining -= maxStack;
            } else if (content.isSimilar(item)) {
                remaining -= (maxStack - content.getAmount());
            }
            if (remaining <= 0) {
                return true;
            }
        }
        return remaining <= 0;
    }

    private boolean hasItem(Player player, Material material, int amount) {
        int count = 0;
        for (ItemStack content : player.getInventory().getStorageContents()) {
            if (content != null && content.getType() == material) {
                count += content.getAmount();
            }
        }
        return count >= amount;
    }

    private void removeItem(Player player, Material material, int amount) {
        int remaining = amount;
        ItemStack[] contents = player.getInventory().getStorageContents();
        for (int i = 0; i < contents.length && remaining > 0; i++) {
            ItemStack content = contents[i];
            if (content != null && content.getType() == material) {
                int take = Math.min(content.getAmount(), remaining);
                content.setAmount(content.getAmount() - take);
                remaining -= take;
                if (content.getAmount() <= 0) {
                    contents[i] = null;
                }
            }
        }
        player.getInventory().setStorageContents(contents);
    }
}
