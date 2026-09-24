package io.bukkitcode.crescentshop.model;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** 商店对象。 */
public class Shop {

    private String name;
    private final UUID owner;
    private int rows;
    private final List<ShopItem> items = new ArrayList<>();
    private String iconData;

    public Shop(String name, UUID owner, int rows) {
        this.name = name;
        this.owner = owner;
        this.rows = rows;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public UUID getOwner() {
        return owner;
    }

    public int getRows() {
        return rows;
    }

    public void setRows(int rows) {
        this.rows = rows;
    }

    public List<ShopItem> getItems() {
        return items;
    }

    public String getIconData() {
        return iconData;
    }

    public void setIconData(String iconData) {
        this.iconData = iconData;
    }

    public int getCapacity() {
        return rows * 9 - 9;
    }

    public boolean isFull() {
        return items.size() >= getCapacity();
    }
}
