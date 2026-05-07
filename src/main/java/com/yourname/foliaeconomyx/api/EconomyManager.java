package com.yourname.foliaeconomyx.api;

import com.yourname.foliaeconomyx.FoliaEconomyX;
import com.yourname.foliaeconomyx.currency.Currency;
import com.yourname.foliaeconomyx.storage.BalanceStorage;

import java.util.*;

public class EconomyManager {
    
    private final FoliaEconomyX plugin;
    private final BalanceStorage storage;

    public EconomyManager(FoliaEconomyX plugin, BalanceStorage storage) {
        this.plugin = plugin;
        this.storage = storage;
    }
    
    public BalanceStorage getStorage() {
        return storage;
    }

    // Account Management
    public void createAccount(UUID uuid, String currency) {
        Currency curr = plugin.getCurrencyManager().getCurrency(currency);
        if (curr != null) {
            storage.createAccount(uuid, currency, curr.getStartingBalance());
        }
    }

    public boolean hasAccount(UUID uuid, String currency) {
        return storage.hasAccount(uuid, currency);
    }

    public void deleteAccount(UUID uuid) {
        storage.deleteAccount(uuid);
    }

    // Balance Operations
    public double getBalance(UUID uuid, String currency) {
        if (!hasAccount(uuid, currency)) {
            createAccount(uuid, currency);
        }
        return storage.getBalance(uuid, currency);
    }

    public void setBalance(UUID uuid, String currency, double amount) {
        Currency curr = plugin.getCurrencyManager().getCurrency(currency);
        if (curr == null) return;
        
        if (!curr.isAllowNegative() && amount < 0) {
            throw new IllegalArgumentException("Negative balance not allowed");
        }
        
        if (curr.getMaxBalance() > 0 && amount > curr.getMaxBalance()) {
            throw new IllegalArgumentException("Exceeds max balance");
        }
        
        storage.setBalance(uuid, currency, amount);
    }

    public boolean deposit(UUID uuid, String currency, double amount) {
        if (amount <= 0) return false;
        
        double current = getBalance(uuid, currency);
        double newBalance = current + amount;
        
        Currency curr = plugin.getCurrencyManager().getCurrency(currency);
        if (curr != null && curr.getMaxBalance() > 0 && newBalance > curr.getMaxBalance()) {
            return false;
        }
        
        setBalance(uuid, currency, newBalance);
        return true;
    }

    public boolean withdraw(UUID uuid, String currency, double amount) {
        if (amount <= 0) return false;
        
        double current = getBalance(uuid, currency);
        double newBalance = current - amount;
        
        Currency curr = plugin.getCurrencyManager().getCurrency(currency);
        if (curr != null && !curr.isAllowNegative() && newBalance < 0) {
            return false;
        }
        
        setBalance(uuid, currency, newBalance);
        return true;
    }

    public boolean has(UUID uuid, String currency, double amount) {
        return getBalance(uuid, currency) >= amount;
    }

    public boolean transfer(UUID from, UUID to, String currency, double amount) {
        if (!has(from, currency, amount)) return false;
        
        if (withdraw(from, currency, amount)) {
            if (deposit(to, currency, amount)) {
                return true;
            } else {
                deposit(from, currency, amount); // Rollback
            }
        }
        return false;
    }

    // Multi-currency operations
    public Map<String, Double> getAllBalances(UUID uuid) {
        return storage.getAllBalances(uuid);
    }

    public Map<UUID, Double> getTopBalances(String currency, int limit) {
        return storage.getTopBalances(currency, limit);
    }
}
