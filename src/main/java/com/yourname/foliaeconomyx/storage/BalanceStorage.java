package com.yourname.foliaeconomyx.storage;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface BalanceStorage {
    
    /**
     * Initialize storage connection
     */
    void initialize();
    
    /**
     * Close storage connection
     */
    void close();
    
    /**
     * Load all balances from storage
     */
    CompletableFuture<Void> load();
    
    /**
     * Save all balances to storage
     */
    CompletableFuture<Void> save();
    
    /**
     * Get balance for a player in specific currency
     */
    double getBalance(UUID uuid, String currency);
    
    /**
     * Set balance for a player in specific currency
     */
    void setBalance(UUID uuid, String currency, double amount);
    
    /**
     * Check if player has account in any currency
     */
    boolean hasAccount(UUID uuid);
    
    /**
     * Check if player has account in specific currency
     */
    boolean hasAccount(UUID uuid, String currency);
    
    /**
     * Create account for player in specific currency
     */
    void createAccount(UUID uuid, String currency, double startingBalance);
    
    /**
     * Delete player account (all currencies)
     */
    void deleteAccount(UUID uuid);
    
    /**
     * Get all balances for a player (all currencies)
     */
    Map<String, Double> getAllBalances(UUID uuid);
    
    /**
     * Get all players with balance in specific currency
     */
    Map<UUID, Double> getCurrencyBalances(String currency);
    
    /**
     * Get all UUIDs with accounts
     */
    Set<UUID> getAllAccounts();
    
    /**
     * Get top balances for specific currency
     */
    Map<UUID, Double> getTopBalances(String currency, int limit);
    
    /**
     * Create backup (if supported)
     */
    void createBackup();
    
    /**
     * Check if storage is ready
     */
    boolean isReady();
    
    /**
     * Get storage type name
     */
    String getType();
}
