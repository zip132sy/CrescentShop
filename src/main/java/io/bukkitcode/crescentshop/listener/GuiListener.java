package io.bukkitcode.crescentshop.listener;

import io.bukkitcode.crescentshop.CrescentShop;
import io.bukkitcode.crescentshop.gui.GuiHolder;
import io.bukkitcode.crescentshop.model.Shop;
import io.bukkitcode.crescentshop.model.ShopItem;
import io.bukkitcode.crescentshop.system.SystemShopManager;
import io.bukkitcode.crescentshop.util.TextUtil;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.util.List;

/** GUI 点击监听器。 */
public class GuiListener implements Listener {

    private final CrescentShop plugin;

    public GuiListener(CrescentShop plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        InventoryHolder holder = event.getInventory().getHolder();
        if (!(holder instanceof GuiHolder)) {
            return;
        }
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }
        Player player = (Player) event.getWhoClicked();
        GuiHolder guiHolder = (GuiHolder) holder;
        int slot = event.getRawSlot();
        Inventory inv = event.getInventory();
        if (slot < 0 || slot >= inv.getSize()) {
            return;
        }
        switch (guiHolder.getType()) {
            case SHOP_LIST:
                handleShopList(player, guiHolder, inv, slot);
                break;
            case SHOP_VIEW:
                handleShopView(player, guiHolder, inv, slot, event.isRightClick());
                break;
            case SEARCH_RESULT:
                handleSearch(player, guiHolder, inv, slot);
                break;
            case SYSTEM_SHOP:
                handleSystemShop(player, inv, slot, event.isRightClick());
                break;
            case SHOP_EDIT:
                handleShopEdit(player, guiHolder, inv, slot, event.isRightClick());
                break;
            case ICON_SELECT:
                handleIconSelect(player, guiHolder, slot);
                break;
            case ICON_CATEGORY:
                handleIconCategory(player, guiHolder, slot);
                break;
            case ICON_LIST:
                handleIconList(player, guiHolder, slot);
                break;
            case ECO_MAIN:
                handleEcoMain(player, slot);
                break;
            case INCOME_LIST:
                handleIncomeList(player, guiHolder, slot);
                break;
            default:
                break;
        }
    }

    private void handleShopList(Player player, GuiHolder holder, Inventory inv, int slot) {
        int size = inv.getSize();
        if (slot == size - 1) {
            plugin.getShopGui().openSystemShop(player);
            return;
        }
        if (slot == size - 9) {
            player.closeInventory();
            plugin.getChatInputManager().awaitCreateShop(player);
            player.sendMessage(TextUtil.color("&e请在聊天栏输入商店名，或输入 &fcancel &e取消。"));
            return;
        }
        if (slot >= size - 9) {
            return;
        }
        List<Shop> shopList = holder.getShopList();
        if (shopList != null && slot < shopList.size()) {
            plugin.getShopGui().openView(player, shopList.get(slot));
        }
    }

    private void handleShopView(Player player, GuiHolder holder, Inventory inv, int slot, boolean rightClick) {
        int size = inv.getSize();
        if (slot == size - 9) {
            plugin.getShopGui().openList(player);
            return;
        }
        if (slot == size - 1) {
            Shop editShop = plugin.getShopManager().getShop(holder.getShopName());
            if (editShop != null && editShop.getOwner().equals(player.getUniqueId())) {
                plugin.getShopGui().openEdit(player, editShop);
            }
            return;
        }
        if (slot >= size - 9) {
            return;
        }
        Shop shop = plugin.getShopManager().getShop(holder.getShopName());
        if (shop == null) {
            player.closeInventory();
            player.sendMessage(TextUtil.color("&c该商店已不存在。"));
            return;
        }
        List<ShopItem> items = shop.getItems();
        if (slot >= items.size()) {
            return;
        }
        ShopItem shopItem = items.get(slot);
        if (rightClick && shop.getOwner().equals(player.getUniqueId())) {
            items.remove(slot);
            plugin.getShopManager().save();
            player.sendMessage(TextUtil.color("&a已下架该商品。"));
            plugin.getShopGui().openView(player, shop);
            return;
        }
        plugin.getChatInputManager().awaitQuantity(player, shop, shopItem);
        player.sendMessage(TextUtil.color("&e请输入购买数量（1 - " + shopItem.getAmount() + "），或 &fcancel &e取消。"));
    }

    private void handleSearch(Player player, GuiHolder holder, Inventory inv, int slot) {
        int size = inv.getSize();
        if (slot == size - 9) {
            plugin.getShopGui().openList(player);
            return;
        }
        if (slot >= size - 9) {
            return;
        }
        List<Shop> shopList = holder.getShopList();
        if (shopList != null && slot < shopList.size()) {
            plugin.getShopGui().openView(player, shopList.get(slot));
        }
    }

    private void handleSystemShop(Player player, Inventory inv, int slot, boolean rightClick) {
        int size = inv.getSize();
        if (slot == size - 1) {
            plugin.getShopGui().openList(player);
            return;
        }
        if (slot >= size - 9) {
            return;
        }
        ItemStack clicked = inv.getItem(slot);
        if (clicked == null || clicked.getType() == Material.AIR) {
            return;
        }
        SystemShopManager.SystemShopItem sysItem =
                plugin.getSystemShopManager().findByMaterial(clicked.getType());
        if (sysItem == null) {
            return;
        }
        if (rightClick) {
            plugin.getChatInputManager().awaitSystemBuy(player, sysItem);
            player.sendMessage(TextUtil.color("&e请输入购买数量，或 &fcancel &e取消。"));
        } else {
            plugin.getChatInputManager().awaitSystemSell(player, sysItem);
            player.sendMessage(TextUtil.color("&e请输入出售数量，或 &fcancel &e取消。"));
        }
    }

    private void handleShopEdit(Player player, GuiHolder holder, Inventory inv, int slot, boolean rightClick) {
        Shop shop = plugin.getShopManager().getShop(holder.getShopName());
        if (shop == null) {
            player.closeInventory();
            player.sendMessage(TextUtil.color("&c该商店已不存在。"));
            return;
        }
        if (slot < 9) {
            switch (slot) {
                case 0:
                    player.closeInventory();
                    plugin.getChatInputManager().awaitRenameShop(player, shop);
                    player.sendMessage(TextUtil.color("&e请输入新商店名，或 &fcancel &e取消。"));
                    break;
                case 1:
                    player.closeInventory();
                    plugin.getChatInputManager().awaitSetSize(player, shop);
                    player.sendMessage(TextUtil.color("&e请输入新行数（"
                            + plugin.getConfigManager().getMinShopRows() + " - "
                            + plugin.getConfigManager().getMaxShopRows() + "），或 &fcancel &e取消。"));
                    break;
                case 2:
                    player.closeInventory();
                    ItemStack hand = player.getInventory().getItemInMainHand();
                    if (hand == null || hand.getType() == Material.AIR) {
                        player.sendMessage(TextUtil.color("&c请先手持要上架的物品。"));
                        return;
                    }
                    plugin.getChatInputManager().awaitSellItem(player, shop);
                    player.sendMessage(TextUtil.color("&7手持数量: &f" + hand.getAmount()));
                    player.sendMessage(TextUtil.color("&e请输入「价格 数量」，例如: &f10 2"));
                    break;
                case 5:
                    plugin.getShopGui().openIconMain(player, shop);
                    break;
                case 8:
                    plugin.getShopGui().openList(player);
                    break;
                default:
                    break;
            }
            return;
        }
        int index = slot - 9;
        List<ShopItem> items = shop.getItems();
        if (index < 0 || index >= items.size()) {
            return;
        }
        if (rightClick) {
            items.remove(index);
            plugin.getShopManager().save();
            player.sendMessage(TextUtil.color("&a已下架该商品。"));
            plugin.getShopGui().openEdit(player, shop);
        } else {
            player.closeInventory();
            plugin.getChatInputManager().awaitEditPrice(player, shop, index);
            player.sendMessage(TextUtil.color("&e请输入新价格，或 &fcancel &e取消。"));
        }
    }

    private void handleIconSelect(Player player, GuiHolder holder, int slot) {
        Shop shop = plugin.getShopManager().getShop(holder.getShopName());
        if (shop == null) {
            player.closeInventory();
            return;
        }
        if (slot == 11) {
            ItemStack hand = player.getInventory().getItemInMainHand();
            if (hand == null || hand.getType() == Material.AIR) {
                player.sendMessage(TextUtil.color("&c请先手持要作为图标的物品。"));
                return;
            }
            player.closeInventory();
            plugin.getShopCommand().applyIcon(player, shop, false);
        } else if (slot == 15) {
            plugin.getShopGui().openIconCategory(player, shop);
        }
    }

    private void handleIconCategory(Player player, GuiHolder holder, int slot) {
        Shop shop = plugin.getShopManager().getShop(holder.getShopName());
        if (shop == null) {
            player.closeInventory();
            return;
        }
        if (slot >= 10 && slot <= 15) {
            int categoryIndex = slot - 10;
            List<Material> materials = io.bukkitcode.crescentshop.util.ItemCategory
                    .getItems(categoryIndex, plugin.getConfigManager().isFilterTechnicalItems());
            plugin.getShopGui().openIconList(player, shop, materials, 0, categoryIndex, null);
            return;
        }
        if (slot == 22) {
            player.closeInventory();
            plugin.getChatInputManager().awaitIconSearch(player, shop);
            player.sendMessage(TextUtil.color("&e请输入搜索关键词，或 &fcancel &e取消。"));
            return;
        }
        if (slot == 18) {
            plugin.getShopGui().openIconMain(player, shop);
        }
    }

    private void handleIconList(Player player, GuiHolder holder, int slot) {
        Shop shop = plugin.getShopManager().getShop(holder.getShopName());
        if (shop == null) {
            player.closeInventory();
            return;
        }
        List<Material> materials = holder.getMaterialList();
        if (materials == null) {
            return;
        }
        if (slot >= 45) {
            if (slot == 45) {
                plugin.getShopGui().openIconList(player, shop, materials,
                        holder.getPage() - 1, holder.getCategoryIndex(), holder.getSearchKeyword());
            } else if (slot == 53) {
                plugin.getShopGui().openIconList(player, shop, materials,
                        holder.getPage() + 1, holder.getCategoryIndex(), holder.getSearchKeyword());
            } else if (slot == 48) {
                player.closeInventory();
                plugin.getChatInputManager().awaitIconSearch(player, shop);
                player.sendMessage(TextUtil.color("&e请输入搜索关键词，或 &fcancel &e取消。"));
            } else if (slot == 50) {
                plugin.getShopGui().openIconCategory(player, shop);
            }
            return;
        }
        int index = holder.getPage() * io.bukkitcode.crescentshop.gui.ShopGui.getPageSize() + slot;
        if (index < 0 || index >= materials.size()) {
            return;
        }
        player.closeInventory();
        plugin.getShopCommand().applyIconFromMaterial(player, shop, materials.get(index));
    }

    private void handleEcoMain(Player player, int slot) {
        if (slot == 14) {
            plugin.getShopGui().openIncome(player, 0);
        }
    }

    private void handleIncomeList(Player player, GuiHolder holder, int slot) {
        if (slot == 45) {
            plugin.getShopGui().openIncome(player, holder.getPage() - 1);
        } else if (slot == 53) {
            plugin.getShopGui().openIncome(player, holder.getPage() + 1);
        } else if (slot == 49) {
            plugin.getShopGui().openEcoMain(player);
        }
    }
}
