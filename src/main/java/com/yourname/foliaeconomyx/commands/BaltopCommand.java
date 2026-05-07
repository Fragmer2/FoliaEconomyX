package com.yourname.foliaeconomyx.commands;

import com.yourname.foliaeconomyx.FoliaEconomyX;
import com.yourname.foliaeconomyx.currency.Currency;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class BaltopCommand implements CommandExecutor, TabCompleter {
    
    private final FoliaEconomyX plugin;
    private final Map<String, CachedBaltop> cache;

    public BaltopCommand(FoliaEconomyX plugin) {
        this.plugin = plugin;
        this.cache = new ConcurrentHashMap<>();
        startCacheUpdateTask();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // /baltop [currency] [page]
        String currencyId = plugin.getCurrencyManager().getDefaultCurrencyId();
        int page = 1;
        
        if (args.length >= 1) {
            // Check if first arg is currency or page number
            try {
                page = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                // It's a currency
                currencyId = args[0].toLowerCase();
            }
        }
        
        if (args.length >= 2) {
            currencyId = args[0].toLowerCase();
            try {
                page = Integer.parseInt(args[1]);
            } catch (NumberFormatException e) {
                sender.sendMessage("§cInvalid page number!");
                return true;
            }
        }
        
        Currency currency = plugin.getCurrencyManager().getCurrency(currencyId);
        
        if (currency == null) {
            sender.sendMessage(plugin.getMessageManager().formatMessage("error.currency-not-found",
                "%currency%", currencyId));
            return true;
        }
        
        if (!currency.isEnabled()) {
            sender.sendMessage(plugin.getMessageManager().formatMessage("error.currency-disabled",
                "%currency%", currencyId));
            return true;
        }
        
        showBaltop(sender, currency, page);
        return true;
    }

    private void showBaltop(CommandSender sender, Currency currency, int page) {
        CachedBaltop cached = cache.get(currency.getId());
        
        if (cached == null || cached.isExpired()) {
            sender.sendMessage("§eUpdating baltop...");
            updateBaltop(currency.getId());
            
            // Wait a bit for update
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                // Ignore
            }
            
            cached = cache.get(currency.getId());
            
            if (cached == null) {
                sender.sendMessage("§cFailed to load baltop! Try again in a moment.");
                return;
            }
        }
        
        Map<UUID, Double> topBalances = cached.getBalances();
        
        if (topBalances.isEmpty()) {
            sender.sendMessage("§cNo players found with this currency!");
            return;
        }
        
        int playersPerPage = plugin.getConfigManager().getBaltopPlayersPerPage();
        int totalPages = (int) Math.ceil((double) topBalances.size() / playersPerPage);
        
        if (page < 1 || page > totalPages) {
            page = 1;
        }
        
        int startIndex = (page - 1) * playersPerPage;
        int endIndex = Math.min(startIndex + playersPerPage, topBalances.size());
        
        // Header
        sender.sendMessage("§6§l━━━━━━ Top Balances (" + currency.getNamePlural() + ") ━━━━━━");
        
        // Entries
        List<Map.Entry<UUID, Double>> entries = new ArrayList<>(topBalances.entrySet());
        
        for (int i = startIndex; i < endIndex; i++) {
            Map.Entry<UUID, Double> entry = entries.get(i);
            int rank = i + 1;
            
            @SuppressWarnings("deprecation")
            OfflinePlayer player = Bukkit.getOfflinePlayer(entry.getKey());
            String playerName = player.getName() != null ? player.getName() : "Unknown";
            
            String formatted = String.format("§e#%d §f%s §7- §a%s", 
                rank, 
                playerName, 
                currency.formatAmount(entry.getValue()));
            
            sender.sendMessage(formatted);
        }
        
        // Footer
        sender.sendMessage("§6§l━━━━━━━━━━ Page " + page + "/" + totalPages + " ━━━━━━━━━━");
    }

    private void updateBaltop(String currencyId) {
        Bukkit.getAsyncScheduler().runNow(plugin, task -> {
            Map<UUID, Double> topBalances = plugin.getEconomyManager().getTopBalances(currencyId, 100);
            
            // Sort by balance descending
            Map<UUID, Double> sorted = topBalances.entrySet().stream()
                .sorted(Map.Entry.<UUID, Double>comparingByValue().reversed())
                .collect(LinkedHashMap::new, 
                    (map, entry) -> map.put(entry.getKey(), entry.getValue()), 
                    LinkedHashMap::putAll);
            
            long cacheDuration = plugin.getConfigManager().getBaltopCacheDuration();
            cache.put(currencyId, new CachedBaltop(sorted, System.currentTimeMillis() + (cacheDuration * 1000)));
        });
    }

    private void startCacheUpdateTask() {
        long updateInterval = plugin.getConfigManager().getBaltopUpdateInterval();
        
        plugin.getServer().getAsyncScheduler().runAtFixedRate(
            plugin,
            task -> {
                // Update all currency baltops
                for (Currency currency : plugin.getCurrencyManager().getAllCurrencies()) {
                    if (currency.isEnabled()) {
                        updateBaltop(currency.getId());
                    }
                }
            },
            updateInterval,
            updateInterval,
            java.util.concurrent.TimeUnit.SECONDS
        );
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> completions = new ArrayList<>(plugin.getCurrencyManager().getCurrencyIds());
            completions.addAll(List.of("1", "2", "3", "4", "5"));
            
            return completions.stream()
                .filter(s -> s.toLowerCase().startsWith(args[0].toLowerCase()))
                .collect(Collectors.toList());
        }
        
        if (args.length == 2) {
            return List.of("1", "2", "3", "4", "5");
        }
        
        return new ArrayList<>();
    }

    // Inner class for caching
    private static class CachedBaltop {
        private final Map<UUID, Double> balances;
        private final long expiresAt;

        public CachedBaltop(Map<UUID, Double> balances, long expiresAt) {
            this.balances = balances;
            this.expiresAt = expiresAt;
        }

        public Map<UUID, Double> getBalances() {
            return balances;
        }

        public boolean isExpired() {
            return System.currentTimeMillis() > expiresAt;
        }
    }
}
