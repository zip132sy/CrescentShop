package io.bukkitcode.crescentshop.listener;

import io.bukkitcode.crescentshop.CrescentShop;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

/** 玩家监听器：登录时初始化账户与旧配置提示。 */
public class PlayerListener implements Listener {

    private final CrescentShop plugin;

    public PlayerListener(CrescentShop plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        plugin.getPlayerDataManager().createAccount(player.getUniqueId());
        if (player.isOp() && plugin.getConfigManager().isConfigOutdated()) {
            player.sendMessage(plugin.getConfigManager().buildOutdatedMessage());
        }
    }
}
