package com.yourname.foliaeconomyx.api;

import com.yourname.foliaeconomyx.FoliaEconomyX;
import com.yourname.foliaeconomyx.currency.Currency;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.OfflinePlayer;

import java.util.List;

/**
 * Vault integration - uses DEFAULT currency only
 * For multi-currency support, other plugins should use FoliaEconomyX API directly
 */
public class VaultEconomyProvider implements Economy {
    
    private final FoliaEconomyX plugin;

    public VaultEconomyProvider(FoliaEconomyX plugin) {
        this.plugin = plugin;
    }

    private String getDefaultCurrency() {
        return plugin.getCurrencyManager().getDefaultCurrencyId();
    }

    private EconomyManager getManager() {
        return plugin.getEconomyManager();
    }

    @Override
    public boolean isEnabled() {
        return plugin.isEnabled();
    }

    @Override
    public String getName() {
        return "FoliaEconomyX";
    }

    @Override
    public boolean hasBankSupport() {
        return false;
    }

    @Override
    public int fractionalDigits() {
        Currency currency = plugin.getCurrencyManager().getDefaultCurrency();
        return currency != null ? currency.getDecimalPlaces() : 2;
    }

    @Override
    public String format(double amount) {
        Currency currency = plugin.getCurrencyManager().getDefaultCurrency();
        return currency != null ? currency.formatAmount(amount) : String.format("$%.2f", amount);
    }

    @Override
    public String currencyNamePlural() {
        Currency currency = plugin.getCurrencyManager().getDefaultCurrency();
        return currency != null ? currency.getNamePlural() : "Dollars";
    }

    @Override
    public String currencyNameSingular() {
        Currency currency = plugin.getCurrencyManager().getDefaultCurrency();
        return currency != null ? currency.getNameSingular() : "Dollar";
    }

    // OfflinePlayer methods
    @Override
    public boolean hasAccount(OfflinePlayer player) {
        return getManager().hasAccount(player.getUniqueId(), getDefaultCurrency());
    }

    @Override
    public boolean hasAccount(OfflinePlayer player, String worldName) {
        return hasAccount(player);
    }

    @Override
    public double getBalance(OfflinePlayer player) {
        return getManager().getBalance(player.getUniqueId(), getDefaultCurrency());
    }

    @Override
    public double getBalance(OfflinePlayer player, String world) {
        return getBalance(player);
    }

    @Override
    public boolean has(OfflinePlayer player, double amount) {
        return getManager().has(player.getUniqueId(), getDefaultCurrency(), amount);
    }

    @Override
    public boolean has(OfflinePlayer player, String worldName, double amount) {
        return has(player, amount);
    }

    @Override
    public EconomyResponse withdrawPlayer(OfflinePlayer player, double amount) {
        if (amount < 0) {
            return new EconomyResponse(0, 0, EconomyResponse.ResponseType.FAILURE, 
                "Cannot withdraw negative amount");
        }
        
        try {
            if (getManager().withdraw(player.getUniqueId(), getDefaultCurrency(), amount)) {
                double newBalance = getManager().getBalance(player.getUniqueId(), getDefaultCurrency());
                return new EconomyResponse(amount, newBalance, EconomyResponse.ResponseType.SUCCESS, null);
            } else {
                double balance = getManager().getBalance(player.getUniqueId(), getDefaultCurrency());
                return new EconomyResponse(0, balance, EconomyResponse.ResponseType.FAILURE, 
                    "Insufficient funds");
            }
        } catch (Exception e) {
            return new EconomyResponse(0, 0, EconomyResponse.ResponseType.FAILURE, e.getMessage());
        }
    }

    @Override
    public EconomyResponse withdrawPlayer(OfflinePlayer player, String worldName, double amount) {
        return withdrawPlayer(player, amount);
    }

    @Override
    public EconomyResponse depositPlayer(OfflinePlayer player, double amount) {
        if (amount < 0) {
            return new EconomyResponse(0, 0, EconomyResponse.ResponseType.FAILURE, 
                "Cannot deposit negative amount");
        }
        
        try {
            if (getManager().deposit(player.getUniqueId(), getDefaultCurrency(), amount)) {
                double newBalance = getManager().getBalance(player.getUniqueId(), getDefaultCurrency());
                return new EconomyResponse(amount, newBalance, EconomyResponse.ResponseType.SUCCESS, null);
            } else {
                double balance = getManager().getBalance(player.getUniqueId(), getDefaultCurrency());
                return new EconomyResponse(0, balance, EconomyResponse.ResponseType.FAILURE, 
                    "Maximum balance exceeded");
            }
        } catch (Exception e) {
            return new EconomyResponse(0, 0, EconomyResponse.ResponseType.FAILURE, e.getMessage());
        }
    }

    @Override
    public EconomyResponse depositPlayer(OfflinePlayer player, String worldName, double amount) {
        return depositPlayer(player, amount);
    }

    @Override
    public boolean createPlayerAccount(OfflinePlayer player) {
        if (hasAccount(player)) {
            return false;
        }
        getManager().createAccount(player.getUniqueId(), getDefaultCurrency());
        return true;
    }

    @Override
    public boolean createPlayerAccount(OfflinePlayer player, String worldName) {
        return createPlayerAccount(player);
    }

    // String-based methods (deprecated but required)
    @Override
    @SuppressWarnings("deprecation")
    public boolean hasAccount(String playerName) {
        return hasAccount(org.bukkit.Bukkit.getOfflinePlayer(playerName));
    }

    @Override
    @SuppressWarnings("deprecation")
    public boolean hasAccount(String playerName, String worldName) {
        return hasAccount(playerName);
    }

    @Override
    @SuppressWarnings("deprecation")
    public double getBalance(String playerName) {
        return getBalance(org.bukkit.Bukkit.getOfflinePlayer(playerName));
    }

    @Override
    @SuppressWarnings("deprecation")
    public double getBalance(String playerName, String world) {
        return getBalance(playerName);
    }

    @Override
    @SuppressWarnings("deprecation")
    public boolean has(String playerName, double amount) {
        return has(org.bukkit.Bukkit.getOfflinePlayer(playerName), amount);
    }

    @Override
    @SuppressWarnings("deprecation")
    public boolean has(String playerName, String worldName, double amount) {
        return has(playerName, amount);
    }

    @Override
    @SuppressWarnings("deprecation")
    public EconomyResponse withdrawPlayer(String playerName, double amount) {
        return withdrawPlayer(org.bukkit.Bukkit.getOfflinePlayer(playerName), amount);
    }

    @Override
    @SuppressWarnings("deprecation")
    public EconomyResponse withdrawPlayer(String playerName, String worldName, double amount) {
        return withdrawPlayer(playerName, amount);
    }

    @Override
    @SuppressWarnings("deprecation")
    public EconomyResponse depositPlayer(String playerName, double amount) {
        return depositPlayer(org.bukkit.Bukkit.getOfflinePlayer(playerName), amount);
    }

    @Override
    @SuppressWarnings("deprecation")
    public EconomyResponse depositPlayer(String playerName, String worldName, double amount) {
        return depositPlayer(playerName, amount);
    }

    @Override
    @SuppressWarnings("deprecation")
    public boolean createPlayerAccount(String playerName) {
        return createPlayerAccount(org.bukkit.Bukkit.getOfflinePlayer(playerName));
    }

    @Override
    @SuppressWarnings("deprecation")
    public boolean createPlayerAccount(String playerName, String worldName) {
        return createPlayerAccount(playerName);
    }

    // Bank methods (not supported)
    @Override
    public EconomyResponse createBank(String name, String player) {
        return new EconomyResponse(0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, 
            "Banks are not supported");
    }

    @Override
    public EconomyResponse createBank(String name, OfflinePlayer player) {
        return new EconomyResponse(0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, 
            "Banks are not supported");
    }

    @Override
    public EconomyResponse deleteBank(String name) {
        return new EconomyResponse(0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, 
            "Banks are not supported");
    }

    @Override
    public EconomyResponse bankBalance(String name) {
        return new EconomyResponse(0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, 
            "Banks are not supported");
    }

    @Override
    public EconomyResponse bankHas(String name, double amount) {
        return new EconomyResponse(0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, 
            "Banks are not supported");
    }

    @Override
    public EconomyResponse bankWithdraw(String name, double amount) {
        return new EconomyResponse(0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, 
            "Banks are not supported");
    }

    @Override
    public EconomyResponse bankDeposit(String name, double amount) {
        return new EconomyResponse(0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, 
            "Banks are not supported");
    }

    @Override
    public EconomyResponse isBankOwner(String name, String playerName) {
        return new EconomyResponse(0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, 
            "Banks are not supported");
    }

    @Override
    public EconomyResponse isBankOwner(String name, OfflinePlayer player) {
        return new EconomyResponse(0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, 
            "Banks are not supported");
    }

    @Override
    public EconomyResponse isBankMember(String name, String playerName) {
        return new EconomyResponse(0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, 
            "Banks are not supported");
    }

    @Override
    public EconomyResponse isBankMember(String name, OfflinePlayer player) {
        return new EconomyResponse(0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, 
            "Banks are not supported");
    }

    @Override
    public List<String> getBanks() {
        return List.of();
    }
}
