package io.bukkitcode.crescentshop.gui;

import io.bukkitcode.crescentshop.model.Shop;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.List;

/** GUI 持有者：标记本插件界面并携带上下文。 */
public class GuiHolder implements InventoryHolder {

    public enum GuiType {
        SHOP_LIST, SHOP_VIEW, SEARCH_RESULT, SYSTEM_SHOP, SHOP_EDIT,
        ICON_SELECT, ICON_CATEGORY, ICON_LIST, ECO_MAIN, INCOME_LIST
    }

    private final GuiType type;
    private Inventory inventory;
    private String shopName;
    private int itemIndex = -1;
    private String systemMaterial;
    private List<Shop> shopList;
    private int categoryIndex = -1;
    private int page;
    private List<Material> materialList;
    private String searchKeyword;

    public GuiHolder(GuiType type) {
        this.type = type;
    }

    public GuiType getType() {
        return type;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    public String getShopName() {
        return shopName;
    }

    public void setShopName(String shopName) {
        this.shopName = shopName;
    }

    public int getItemIndex() {
        return itemIndex;
    }

    public void setItemIndex(int itemIndex) {
        this.itemIndex = itemIndex;
    }

    public String getSystemMaterial() {
        return systemMaterial;
    }

    public void setSystemMaterial(String systemMaterial) {
        this.systemMaterial = systemMaterial;
    }

    public List<Shop> getShopList() {
        return shopList;
    }

    public void setShopList(List<Shop> shopList) {
        this.shopList = shopList;
    }

    public int getCategoryIndex() {
        return categoryIndex;
    }

    public void setCategoryIndex(int categoryIndex) {
        this.categoryIndex = categoryIndex;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public List<Material> getMaterialList() {
        return materialList;
    }

    public void setMaterialList(List<Material> materialList) {
        this.materialList = materialList;
    }

    public String getSearchKeyword() {
        return searchKeyword;
    }

    public void setSearchKeyword(String searchKeyword) {
        this.searchKeyword = searchKeyword;
    }
}
