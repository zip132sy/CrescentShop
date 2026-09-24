package io.bukkitcode.crescentshop.util;

import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.ChatColor;

import java.util.ArrayList;
import java.util.List;

/** 文本工具：颜色转换、金额格式化、可点击文本。 */
public final class TextUtil {

    private TextUtil() {
    }

    public static String color(String text) {
        return text == null ? "" : ChatColor.translateAlternateColorCodes('&', text);
    }

    public static List<String> color(List<String> list) {
        List<String> result = new ArrayList<>();
        if (list != null) {
            for (String line : list) {
                result.add(color(line));
            }
        }
        return result;
    }

    public static String stripColor(String text) {
        return text == null ? "" : ChatColor.stripColor(color(text));
    }

    public static String formatMoney(double value) {
        if (value == Math.floor(value) && !Double.isInfinite(value)) {
            return String.valueOf((long) value);
        }
        return String.format("%.2f", value);
    }

    /** 构建可点击文本：点击后填充命令到聊天栏 */
    public static TextComponent buildClickable(String text, String command, String hoverText) {
        TextComponent component = new TextComponent(color(text));
        component.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, command));
        if (hoverText != null && !hoverText.isEmpty()) {
            component.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                    new ComponentBuilder(color(hoverText)).create()));
        }
        return component;
    }
}
