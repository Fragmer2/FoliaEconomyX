package com.yourname.foliaeconomyx.placeholders;

import com.yourname.foliaeconomyx.FoliaEconomyX;
import com.yourname.foliaeconomyx.currency.Currency;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class EconomyExpansion extends PlaceholderExpansion {
    
    private final FoliaEconomyX plugin;
    private final Map<String, LinkedHashMap<UUID, Double>> topCache;
    private long lastUpdate = 0;
    
    public EconomyExpansion(FoliaEconomyX plugin) {
        this.plugin = plugin;
        this.topCache = new ConcurrentHashMap<>();
        
        // Start real-time update task (every 5 seconds)
        startRealtimeUpdateTask();
    }
    
    @Override
    public @NotNull String getIdentifier() {
        return "foliaeconomyx";
    }
    
    @Override
    public @NotNull String getAuthor() {
        return plugin.getDescription().getAuthors().toString();
    }
    
    @Override
    public @NotNull String getVersion() {
        return plugin.getDescription().getVersion();
    }
    
    @Override
    public boolean persist() {
        return true;
    }
    
    @Override
    public boolean canRegister() {
        return true;
    }
    
    @Override
    public String onRequest(OfflinePlayer player, @NotNull String params) {
        // %foliaeconomyx_balance% - default currency
        if (params.equals("balance")) {
            Currency currency = plugin.getCurrencyManager().getDefaultCurrency();
            double balance = plugin.getEconomyManager().getBalance(player.getUniqueId(), currency.getId());
            return formatNumber(balance);
        }
        
        // %foliaeconomyx_balance_<currency>% - specific currency
        if (params.startsWith("balance_")) {
            String currencyId = params.substring(8);
            Currency currency = plugin.getCurrencyManager().getCurrency(currencyId);
            if (currency == null) return "Unknown";
            double balance = plugin.getEconomyManager().getBalance(player.getUniqueId(), currencyId);
            return formatNumber(balance);
        }
        
        // %foliaeconomyx_balance_formatted% - with symbol
        if (params.equals("balance_formatted")) {
            Currency currency = plugin.getCurrencyManager().getDefaultCurrency();
            double balance = plugin.getEconomyManager().getBalance(player.getUniqueId(), currency.getId());
            return currency.formatAmount(balance);
        }
        
        // %foliaeconomyx_balance_<currency>_formatted%
        if (params.startsWith("balance_") && params.endsWith("_formatted")) {
            String currencyId = params.substring(8, params.length() - 10);
            Currency currency = plugin.getCurrencyManager().getCurrency(currencyId);
            if (currency == null) return "Unknown";
            double balance = plugin.getEconomyManager().getBalance(player.getUniqueId(), currencyId);
            return currency.formatAmount(balance);
        }
        
        // %foliaeconomyx_top_<position>_name% - top player name
        // %foliaeconomyx_top_<position>_balance% - top player balance
        // %foliaeconomyx_top_<currency>_<position>_name%
        // %foliaeconomyx_top_<currency>_<position>_balance%
        if (params.startsWith("top_")) {
            return handleTopPlaceholder(params);
        }
        
        return null;
    }
    
    private String handleTopPlaceholder(String params) {
        String[] parts = params.split("_");
        
        // top_1_name or top_1_balance (default currency)
        if (parts.length == 3) {
            int position = Integer.parseInt(parts[1]);
            String type = parts[2]; // "name" or "balance"
            
            Currency defaultCurrency = plugin.getCurrencyManager().getDefaultCurrency();
            return getTopPlayer(defaultCurrency.getId(), position, type);
        }
        
        // top_dollars_1_name or top_dollars_1_balance
        if (parts.length == 4) {
            String currencyId = parts[1];
            int position = Integer.parseInt(parts[2]);
            String type = parts[3]; // "name" or "balance"
            
            return getTopPlayer(currencyId, position, type);
        }
        
        return "Invalid";
    }
    
    private String getTopPlayer(String currencyId, int position, String type) {
        // Update cache if needed
        updateTopCache(currencyId);
        
        LinkedHashMap<UUID, Double> top = topCache.get(currencyId);
        if (top == null || top.isEmpty()) {
            return type.equals("name") ? "Nobody" : "0";
        }
        
        List<Map.Entry<UUID, Double>> entries = new ArrayList<>(top.entrySet());
        
        if (position < 1 || position > entries.size()) {
            return type.equals("name") ? "Nobody" : "0";
        }
        
        Map.Entry<UUID, Double> entry = entries.get(position - 1);
        
        if (type.equals("name")) {
            OfflinePlayer player = Bukkit.getOfflinePlayer(entry.getKey());
            return player.getName() != null ? player.getName() : "Unknown";
        } else if (type.equals("balance")) {
            return formatNumber(entry.getValue());
        }
        
        return "Invalid";
    }
    
    private void updateTopCache(String currencyId) {
        // Force update every 5 seconds
        long now = System.currentTimeMillis();
        if (now - lastUpdate < 5000) {
            return;
        }
        
        Map<UUID, Double> balances = plugin.getEconomyManager().getStorage().getCurrencyBalances(currencyId);
        
        LinkedHashMap<UUID, Double> sorted = balances.entrySet().stream()
            .sorted(Map.Entry.<UUID, Double>comparingByValue().reversed())
            .limit(100)
            .collect(LinkedHashMap::new,
                (map, entry) -> map.put(entry.getKey(), entry.getValue()),
                LinkedHashMap::putAll);
        
        topCache.put(currencyId, sorted);
        lastUpdate = now;
    }
    
    private void startRealtimeUpdateTask() {
        plugin.getServer().getGlobalRegionScheduler().runAtFixedRate(
            plugin,
            task -> {
                // Update all currency tops every 5 seconds
                for (Currency currency : plugin.getCurrencyManager().getAllCurrencies()) {
                    if (currency.isEnabled()) {
                        updateTopCacheForce(currency.getId());
                    }
                }
            },
            100L, // 5 seconds delay
            100L  // 5 seconds interval
        );
    }
    
    private void updateTopCacheForce(String currencyId) {
        Map<UUID, Double> balances = plugin.getEconomyManager().getStorage().getCurrencyBalances(currencyId);
        
        LinkedHashMap<UUID, Double> sorted = balances.entrySet().stream()
            .sorted(Map.Entry.<UUID, Double>comparingByValue().reversed())
            .limit(100)
            .collect(LinkedHashMap::new,
                (map, entry) -> map.put(entry.getKey(), entry.getValue()),
                LinkedHashMap::putAll);
        
        topCache.put(currencyId, sorted);
    }
    
    private String formatNumber(double number) {
        if (number >= 1_000_000_000_000L) {
            return String.format("%.1fT", number / 1_000_000_000_000.0);
        } else if (number >= 1_000_000_000) {
            return String.format("%.1fB", number / 1_000_000_000.0);
        } else if (number >= 1_000_000) {
            return String.format("%.1fM", number / 1_000_000.0);
        } else if (number >= 1_000) {
            return String.format("%.1fK", number / 1_000.0);
        } else {
            return String.format("%.2f", number);
        }
    }
}
