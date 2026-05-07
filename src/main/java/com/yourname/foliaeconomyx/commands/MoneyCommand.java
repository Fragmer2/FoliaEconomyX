package com.yourname.foliaeconomyx.commands;

import com.yourname.foliaeconomyx.FoliaEconomyX;
import com.yourname.foliaeconomyx.currency.Currency;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class MoneyCommand implements CommandExecutor, TabCompleter {
    
    private final FoliaEconomyX plugin;

    public MoneyCommand(FoliaEconomyX plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // /money - show all balances for sender
        if (args.length == 0) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(plugin.getMessageManager().getMessage("error.player-only"));
                return true;
            }
            
            Player player = (Player) sender;
            showBalances(sender, player);
            return true;
        }
        
        // /money <currency> - show specific currency balance
        if (args.length == 1) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(plugin.getMessageManager().getMessage("error.player-only"));
                return true;
            }
            
            Player player = (Player) sender;
            String currencyId = args[0].toLowerCase();
            
            // Check if it's a player name (checking other player's balance)
            @SuppressWarnings("deprecation")
            OfflinePlayer target = Bukkit.getOfflinePlayer(args[0]);
            
            if (target.hasPlayedBefore() || target.isOnline()) {
                // It's a player
                if (!sender.hasPermission("foliaeconomyx.money.others")) {
                    sender.sendMessage(plugin.getMessageManager().getMessage("error.no-permission"));
                    return true;
                }
                showBalances(sender, target);
            } else {
                // It's a currency
                showCurrencyBalance(sender, player, currencyId);
            }
            return true;
        }
        
        // /money <currency> <player> - show specific currency for specific player
        if (args.length == 2) {
            if (!sender.hasPermission("foliaeconomyx.money.others")) {
                sender.sendMessage(plugin.getMessageManager().getMessage("error.no-permission"));
                return true;
            }
            
            String currencyId = args[0].toLowerCase();
            
            @SuppressWarnings("deprecation")
            OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
            
            if (!plugin.getEconomyManager().hasAccount(target.getUniqueId(), currencyId)) {
                sender.sendMessage(plugin.getMessageManager().getMessage("error.player-not-found"));
                return true;
            }
            
            showCurrencyBalance(sender, target, currencyId);
            return true;
        }
        
        sender.sendMessage("§cUsage: /money [currency] [player]");
        return true;
    }

    private void showBalances(CommandSender sender, OfflinePlayer target) {
        boolean isSelf = sender instanceof Player && ((Player) sender).getUniqueId().equals(target.getUniqueId());
        
        // Header - hardcoded to avoid messages.yml issues
        if (isSelf) {
            sender.sendMessage("§a§lYour balance:");
        } else {
            String name = target.getName() != null ? target.getName() : "Unknown";
            sender.sendMessage("§a§l" + name + "'s balance:");
        }
        
        // Get all balances
        Map<String, Double> balances = plugin.getEconomyManager().getAllBalances(target.getUniqueId());
        
        // Show each currency
        for (Currency currency : plugin.getCurrencyManager().getAllCurrencies()) {
            double balance = balances.getOrDefault(currency.getId(), currency.getStartingBalance());
            
            // Format: "  $1000.00 (Dollars)"
            String formatted = String.format("§a  %s §7(%s)", 
                currency.formatAmount(balance),
                currency.getNamePlural());
            
            sender.sendMessage(formatted);
        }
    }

    private void showCurrencyBalance(CommandSender sender, OfflinePlayer target, String currencyId) {
        Currency currency = plugin.getCurrencyManager().getCurrency(currencyId);
        
        if (currency == null) {
            sender.sendMessage("§cCurrency '" + currencyId + "' not found!");
            return;
        }
        
        if (!currency.isEnabled()) {
            sender.sendMessage("§cCurrency '" + currencyId + "' is disabled!");
            return;
        }
        
        double balance = plugin.getEconomyManager().getBalance(target.getUniqueId(), currencyId);
        
        boolean isSelf = sender instanceof Player && ((Player) sender).getUniqueId().equals(target.getUniqueId());
        
        // Header
        if (isSelf) {
            sender.sendMessage("§a§lYour balance:");
        } else {
            String name = target.getName() != null ? target.getName() : "Unknown";
            sender.sendMessage("§a§l" + name + "'s balance:");
        }
        
        // Balance
        String formatted = String.format("§a  %s §7(%s)", 
            currency.formatAmount(balance),
            currency.getNamePlural());
        sender.sendMessage(formatted);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            // Currency names
            completions.addAll(plugin.getCurrencyManager().getCurrencyIds());
            
            // Player names if has permission
            if (sender.hasPermission("foliaeconomyx.money.others")) {
                completions.addAll(Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .collect(Collectors.toList()));
            }
            
            return completions.stream()
                .filter(s -> s.toLowerCase().startsWith(args[0].toLowerCase()))
                .collect(Collectors.toList());
        }
        
        if (args.length == 2 && sender.hasPermission("foliaeconomyx.money.others")) {
            // Player names
            return Bukkit.getOnlinePlayers().stream()
                .map(Player::getName)
                .filter(name -> name.toLowerCase().startsWith(args[1].toLowerCase()))
                .collect(Collectors.toList());
        }
        
        return completions;
    }
}
