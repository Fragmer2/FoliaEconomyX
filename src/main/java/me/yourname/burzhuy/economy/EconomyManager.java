package me.yourname.burzhuy.economy;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Manages economy integration via Vault
 */
public class EconomyManager {
    
    private final JavaPlugin plugin;
    private Economy economy;

    public EconomyManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Setup economy provider
     * @return true if economy was successfully set up
     */
    public boolean setupEconomy() {
        if (plugin.getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        
        RegisteredServiceProvider<Economy> rsp = plugin.getServer()
                .getServicesManager()
                .getRegistration(Economy.class);
                
        if (rsp == null) {
            return false;
        }
        
        economy = rsp.getProvider();
        return economy != null;
    }

    /**
     * Get player's balance
     */
    public double getBalance(Player player) {
        return economy.getBalance(player);
    }

    /**
     * Deposit money to player's account
     */
    public void deposit(Player player, double amount) {
        economy.depositPlayer(player, amount);
    }

    /**
     * Withdraw money from player's account
     */
    public void withdraw(Player player, double amount) {
        economy.withdrawPlayer(player, amount);
    }
}
