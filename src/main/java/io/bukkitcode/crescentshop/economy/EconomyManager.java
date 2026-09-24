package io.bukkitcode.crescentshop.economy;

import io.bukkitcode.crescentshop.CrescentShop;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.lang.reflect.Method;

/** 经济管理器：优先 Vault（反射），否则内置。 */
public class EconomyManager {

    /** 经济提供者接口 */
    public interface Provider {
        double getBalance(OfflinePlayer player);

        boolean has(OfflinePlayer player, double amount);

        boolean withdraw(OfflinePlayer player, double amount);

        boolean deposit(OfflinePlayer player, double amount);

        boolean set(OfflinePlayer player, double amount);

        String getName();
    }

    private final CrescentShop plugin;
    private Provider provider;

    public EconomyManager(CrescentShop plugin) {
        this.plugin = plugin;
    }

    public void setup() {
        if (Bukkit.getPluginManager().getPlugin("Vault") != null) {
            try {
                Class<?> economyClass = Class.forName("net.milkbowl.vault.economy.Economy");
                RegisteredServiceProvider<?> registration =
                        Bukkit.getServicesManager().getRegistration(economyClass);
                if (registration != null && registration.getProvider() != null) {
                    this.provider = new VaultProvider(registration.getProvider());
                    plugin.getLogger().info("已接入 Vault 经济系统。");
                    return;
                }
            } catch (Throwable throwable) {
                plugin.getLogger().warning("Vault 接入失败，回退到内置经济: " + throwable.getMessage());
            }
        }
        this.provider = new InternalProvider(plugin);
        plugin.getLogger().info("使用内置经济系统。");
    }

    public Provider getProvider() {
        return provider;
    }

    /** 内置经济实现 */
    private static class InternalProvider implements Provider {
        private final CrescentShop plugin;

        private InternalProvider(CrescentShop plugin) {
            this.plugin = plugin;
        }

        @Override
        public double getBalance(OfflinePlayer player) {
            return plugin.getPlayerDataManager().getBalance(player.getUniqueId());
        }

        @Override
        public boolean has(OfflinePlayer player, double amount) {
            return getBalance(player) >= amount;
        }

        @Override
        public boolean withdraw(OfflinePlayer player, double amount) {
            double balance = getBalance(player);
            if (balance < amount) {
                return false;
            }
            plugin.getPlayerDataManager().setBalance(player.getUniqueId(), balance - amount);
            return true;
        }

        @Override
        public boolean deposit(OfflinePlayer player, double amount) {
            plugin.getPlayerDataManager().setBalance(player.getUniqueId(), getBalance(player) + amount);
            return true;
        }

        @Override
        public boolean set(OfflinePlayer player, double amount) {
            plugin.getPlayerDataManager().setBalance(player.getUniqueId(), amount);
            return true;
        }

        @Override
        public String getName() {
            return "内置经济";
        }
    }

    /** Vault 经济实现（反射） */
    private static class VaultProvider implements Provider {
        private final Object economy;
        private final Method getBalanceMethod;
        private final Method hasMethod;
        private final Method withdrawMethod;
        private final Method depositMethod;
        private final Method transactionSuccessMethod;

        private VaultProvider(Object economy) throws ReflectiveOperationException {
            this.economy = economy;
            Class<?> economyClass = economy.getClass();
            this.getBalanceMethod = economyClass.getMethod("getBalance", OfflinePlayer.class);
            this.hasMethod = economyClass.getMethod("has", OfflinePlayer.class, double.class);
            this.withdrawMethod = economyClass.getMethod("withdrawPlayer", OfflinePlayer.class, double.class);
            this.depositMethod = economyClass.getMethod("depositPlayer", OfflinePlayer.class, double.class);
            Class<?> responseClass = Class.forName("net.milkbowl.vault.economy.EconomyResponse");
            this.transactionSuccessMethod = responseClass.getMethod("transactionSuccess");
        }

        @Override
        public double getBalance(OfflinePlayer player) {
            try {
                Object result = getBalanceMethod.invoke(economy, player);
                return result instanceof Number ? ((Number) result).doubleValue() : 0.0;
            } catch (ReflectiveOperationException e) {
                return 0.0;
            }
        }

        @Override
        public boolean has(OfflinePlayer player, double amount) {
            try {
                Object result = hasMethod.invoke(economy, player, amount);
                return result instanceof Boolean && (Boolean) result;
            } catch (ReflectiveOperationException e) {
                return false;
            }
        }

        @Override
        public boolean withdraw(OfflinePlayer player, double amount) {
            try {
                return isSuccess(withdrawMethod.invoke(economy, player, amount));
            } catch (ReflectiveOperationException e) {
                return false;
            }
        }

        @Override
        public boolean deposit(OfflinePlayer player, double amount) {
            try {
                return isSuccess(depositMethod.invoke(economy, player, amount));
            } catch (ReflectiveOperationException e) {
                return false;
            }
        }

        @Override
        public boolean set(OfflinePlayer player, double amount) {
            double balance = getBalance(player);
            if (balance > amount) {
                return withdraw(player, balance - amount);
            } else if (balance < amount) {
                return deposit(player, amount - balance);
            }
            return true;
        }

        private boolean isSuccess(Object response) {
            if (response == null) {
                return false;
            }
            try {
                Object result = transactionSuccessMethod.invoke(response);
                return result instanceof Boolean && (Boolean) result;
            } catch (ReflectiveOperationException e) {
                return false;
            }
        }

        @Override
        public String getName() {
            return "Vault";
        }
    }
}
