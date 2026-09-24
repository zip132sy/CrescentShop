package io.bukkitcode.crescentshop.util;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** GUI 工具：快速构建带名称与 Lore 的物品。 */
public final class GuiUtil {

    private GuiUtil() {
    }

    public static ItemStack buildItem(Material material, String name, String... lore) {
        return buildItem(material, name, Arrays.asList(lore));
    }

    public static ItemStack buildItem(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(TextUtil.color(name));
            if (lore != null && !lore.isEmpty()) {
                List<String> list = new ArrayList<>();
                for (String line : lore) {
                    list.add(TextUtil.color(line));
                }
                meta.setLore(list);
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    public static ItemStack buildFiller() {
        return buildItem(Material.STAINED_GLASS_PANE, "&7", Arrays.asList(" "));
    }
}
