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
import java.util.stream.Collectors;

public class EcoCommand implements CommandExecutor, TabCompleter {
    
    private final FoliaEconomyX plugin;

    public EcoCommand(FoliaEconomyX plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("foliaeconomyx.admin")) {
            sender.sendMessage(plugin.getMessageManager().getMessage("error.no-permission"));
            return true;
        }
        
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }
        
        String subCommand = args[0].toLowerCase();
        
        switch (subCommand) {
            case "give":
                return handleGive(sender, args);
            case "take":
                return handleTake(sender, args);
            case "set":
                return handleSet(sender, args);
            case "reset":
                return handleReset(sender, args);
            case "reload":
                return handleReload(sender);
            default:
                sendHelp(sender);
                return true;
        }
    }

    private boolean handleGive(CommandSender sender, String[] args) {
        // /eco give <player> <amount> [currency]
        if (args.length < 3) {
            sender.sendMessage("§cUsage: /eco give <player> <amount> [currency]");
            return true;
        }
        
        @SuppressWarnings("deprecation")
        OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
        
        double amount;
        try {
            amount = Double.parseDouble(args[2]);
        } catch (NumberFormatException e) {
            sender.sendMessage(plugin.getMessageManager().getMessage("error.invalid-amount"));
            return true;
        }
        
        if (amount <= 0) {
            sender.sendMessage(plugin.getMessageManager().getMessage("error.invalid-amount"));
            return true;
        }
        
        String currencyId = args.length == 4 ? args[3].toLowerCase() : 
            plugin.getCurrencyManager().getDefaultCurrencyId();
        
        Currency currency = plugin.getCurrencyManager().getCurrency(currencyId);
        
        if (currency == null) {
            sender.sendMessage(plugin.getMessageManager().formatMessage("error.currency-not-found",
                "%currency%", currencyId));
            return true;
        }
        
        if (plugin.getEconomyManager().deposit(target.getUniqueId(), currencyId, amount)) {
            double newBalance = plugin.getEconomyManager().getBalance(target.getUniqueId(), currencyId);
            
            sender.sendMessage(plugin.getMessageManager().formatMessage("eco.give",
                "%currency_symbol%", currency.getSymbol(),
                "%amount%", String.format("%." + currency.getDecimalPlaces() + "f", amount),
                "%player%", target.getName() != null ? target.getName() : args[1],
                "%balance%", String.format("%." + currency.getDecimalPlaces() + "f", newBalance)));
        } else {
            sender.sendMessage("§cFailed to give money! Maximum balance may be exceeded.");
        }
        
        return true;
    }

    private boolean handleTake(CommandSender sender, String[] args) {
        // /eco take <player> <amount> [currency]
        if (args.length < 3) {
            sender.sendMessage("§cUsage: /eco take <player> <amount> [currency]");
            return true;
        }
        
        @SuppressWarnings("deprecation")
        OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
        
        double amount;
        try {
            amount = Double.parseDouble(args[2]);
        } catch (NumberFormatException e) {
            sender.sendMessage(plugin.getMessageManager().getMessage("error.invalid-amount"));
            return true;
        }
        
        if (amount <= 0) {
            sender.sendMessage(plugin.getMessageManager().getMessage("error.invalid-amount"));
            return true;
        }
        
        String currencyId = args.length == 4 ? args[3].toLowerCase() : 
            plugin.getCurrencyManager().getDefaultCurrencyId();
        
        Currency currency = plugin.getCurrencyManager().getCurrency(currencyId);
        
        if (currency == null) {
            sender.sendMessage(plugin.getMessageManager().formatMessage("error.currency-not-found",
                "%currency%", currencyId));
            return true;
        }
        
        if (!plugin.getEconomyManager().hasAccount(target.getUniqueId(), currencyId)) {
            sender.sendMessage(plugin.getMessageManager().getMessage("error.player-not-found"));
            return true;
        }
        
        if (plugin.getEconomyManager().withdraw(target.getUniqueId(), currencyId, amount)) {
            double newBalance = plugin.getEconomyManager().getBalance(target.getUniqueId(), currencyId);
            
            sender.sendMessage(plugin.getMessageManager().formatMessage("eco.take",
                "%currency_symbol%", currency.getSymbol(),
                "%amount%", String.format("%." + currency.getDecimalPlaces() + "f", amount),
                "%player%", target.getName() != null ? target.getName() : args[1],
                "%balance%", String.format("%." + currency.getDecimalPlaces() + "f", newBalance)));
        } else {
            sender.sendMessage("§cFailed to take money! Player may not have enough funds.");
        }
        
        return true;
    }

    private boolean handleSet(CommandSender sender, String[] args) {
        // /eco set <player> <amount> [currency]
        if (args.length < 3) {
            sender.sendMessage("§cUsage: /eco set <player> <amount> [currency]");
            return true;
        }
        
        @SuppressWarnings("deprecation")
        OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
        
        double amount;
        try {
            amount = Double.parseDouble(args[2]);
        } catch (NumberFormatException e) {
            sender.sendMessage(plugin.getMessageManager().getMessage("error.invalid-amount"));
            return true;
        }
        
        String currencyId = args.length == 4 ? args[3].toLowerCase() : 
            plugin.getCurrencyManager().getDefaultCurrencyId();
        
        Currency currency = plugin.getCurrencyManager().getCurrency(currencyId);
        
        if (currency == null) {
            sender.sendMessage(plugin.getMessageManager().formatMessage("error.currency-not-found",
                "%currency%", currencyId));
            return true;
        }
        
        if (amount < 0 && !currency.isAllowNegative()) {
            sender.sendMessage(plugin.getMessageManager().getMessage("error.negative-balance"));
            return true;
        }
        
        try {
            plugin.getEconomyManager().setBalance(target.getUniqueId(), currencyId, amount);
            
            sender.sendMessage(plugin.getMessageManager().formatMessage("eco.set",
                "%player%", target.getName() != null ? target.getName() : args[1],
                "%balance%", String.format("%." + currency.getDecimalPlaces() + "f", amount),
                "%currency_symbol%", currency.getSymbol()));
        } catch (IllegalArgumentException e) {
            sender.sendMessage("§c" + e.getMessage());
        }
        
        return true;
    }

    private boolean handleReset(CommandSender sender, String[] args) {
        // /eco reset <player> [currency]
        if (args.length < 2) {
            sender.sendMessage("§cUsage: /eco reset <player> [currency]");
            return true;
        }
        
        @SuppressWarnings("deprecation")
        OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
        
        String currencyId = args.length == 3 ? args[2].toLowerCase() : 
            plugin.getCurrencyManager().getDefaultCurrencyId();
        
        Currency currency = plugin.getCurrencyManager().getCurrency(currencyId);
        
        if (currency == null) {
            sender.sendMessage(plugin.getMessageManager().formatMessage("error.currency-not-found",
                "%currency%", currencyId));
            return true;
        }
        
        double startingBalance = currency.getStartingBalance();
        plugin.getEconomyManager().setBalance(target.getUniqueId(), currencyId, startingBalance);
        
        sender.sendMessage(plugin.getMessageManager().formatMessage("eco.reset",
            "%player%", target.getName() != null ? target.getName() : args[1],
            "%balance%", String.format("%." + currency.getDecimalPlaces() + "f", startingBalance),
            "%currency_symbol%", currency.getSymbol()));
        
        return true;
    }

    private boolean handleReload(CommandSender sender) {
        plugin.reloadConfig();
        plugin.getConfigManager().reload();
        plugin.getMessageManager().reload();
        plugin.getCurrencyManager().reload();
        
        sender.sendMessage(plugin.getMessageManager().getMessage("system.reload"));
        return true;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage("§6§l=== FoliaEconomyX Admin Commands ===");
        sender.sendMessage("§e/eco give <player> <amount> [currency] §7- Give money");
        sender.sendMessage("§e/eco take <player> <amount> [currency] §7- Take money");
        sender.sendMessage("§e/eco set <player> <amount> [currency] §7- Set balance");
        sender.sendMessage("§e/eco reset <player> [currency] §7- Reset to starting balance");
        sender.sendMessage("§e/eco reload §7- Reload configuration");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return List.of("give", "take", "set", "reset", "reload").stream()
                .filter(sub -> sub.startsWith(args[0].toLowerCase()))
                .collect(Collectors.toList());
        }
        
        if (args.length == 2 && !args[0].equalsIgnoreCase("reload")) {
            return Bukkit.getOnlinePlayers().stream()
                .map(Player::getName)
                .filter(name -> name.toLowerCase().startsWith(args[1].toLowerCase()))
                .collect(Collectors.toList());
        }
        
        if (args.length == 3 && !args[0].equalsIgnoreCase("reset") && !args[0].equalsIgnoreCase("reload")) {
            return List.of("100", "500", "1000", "10000");
        }
        
        if (args.length == 4 || (args.length == 3 && args[0].equalsIgnoreCase("reset"))) {
            return new ArrayList<>(plugin.getCurrencyManager().getCurrencyIds());
        }
        
        return new ArrayList<>();
    }
}
