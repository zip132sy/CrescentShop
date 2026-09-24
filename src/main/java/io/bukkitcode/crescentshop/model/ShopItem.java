package io.bukkitcode.crescentshop.model;

import io.bukkitcode.crescentshop.util.ItemSerializer;
import org.bukkit.inventory.ItemStack;

/** 商品对象。 */
public class ShopItem {

    private ItemStack item;
    private double price;
    private int amount;

    public ShopItem(ItemStack item, double price, int amount) {
        this.item = item;
        this.price = price;
        this.amount = amount;
    }

    public ItemStack getItem() {
        return item;
    }

    public void setItem(ItemStack item) {
        this.item = item;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public int getAmount() {
        return amount;
    }

    public void setAmount(int amount) {
        this.amount = amount;
    }

    public String serializeItem() {
        return ItemSerializer.serialize(item);
    }

    public void deserializeItem(String data) {
        this.item = ItemSerializer.deserialize(data);
    }
}
