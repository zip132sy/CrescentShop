package io.bukkitcode.crescentshop.listener;

import io.bukkitcode.crescentshop.CrescentShop;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

/** 聊天监听器：捕获等待输入的消息。 */
public class ChatListener implements Listener {

    private final CrescentShop plugin;

    public ChatListener(CrescentShop plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        if (!plugin.getChatInputManager().isPending(player.getUniqueId())) {
            return;
        }
        event.setCancelled(true);
        String message = event.getMessage();
        plugin.getServer().getScheduler().runTask(plugin, () ->
                plugin.getChatInputManager().handleInput(player, message));
    }
}
