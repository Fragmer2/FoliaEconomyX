package com.yourname.foliaeconomyx.commands;

import com.yourname.foliaeconomyx.FoliaEconomyX;
import com.yourname.foliaeconomyx.confirmation.PendingTransfer;
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

public class PayCommand implements CommandExecutor, TabCompleter {
    
    private final FoliaEconomyX plugin;

    public PayCommand(FoliaEconomyX plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(plugin.getMessageManager().getMessage("error.player-only"));
            return true;
        }
        
        Player player = (Player) sender;
        
        // /pay <player> <amount> [currency]
        if (args.length < 2 || args.length > 3) {
            sender.sendMessage("§cUsage: /pay <player> <amount> [currency]");
            return true;
        }
        
        // Parse target
        @SuppressWarnings("deprecation")
        OfflinePlayer target = Bukkit.getOfflinePlayer(args[0]);
        
        if (!target.hasPlayedBefore() && !target.isOnline()) {
            sender.sendMessage(plugin.getMessageManager().getMessage("error.player-not-found"));
            return true;
        }
        
        // Check self-pay
        if (player.getUniqueId().equals(target.getUniqueId())) {
            sender.sendMessage(plugin.getMessageManager().getMessage("pay.self-pay"));
            return true;
        }
        
        // Parse amount
        double amount;
        try {
            amount = Double.parseDouble(args[1]);
        } catch (NumberFormatException e) {
            sender.sendMessage(plugin.getMessageManager().getMessage("error.invalid-amount"));
            return true;
        }
        
        if (amount <= 0) {
            sender.sendMessage(plugin.getMessageManager().getMessage("error.invalid-amount"));
            return true;
        }
        
        // Parse currency
        String currencyId = args.length == 3 ? args[2].toLowerCase() : 
            plugin.getCurrencyManager().getDefaultCurrencyId();
        
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
        
        if (!currency.isAllowTransfer()) {
            sender.sendMessage(plugin.getMessageManager().getMessage("error.transfer-not-allowed"));
            return true;
        }
        
        // Check if sender has enough
        if (!plugin.getEconomyManager().has(player.getUniqueId(), currencyId, amount)) {
            double balance = plugin.getEconomyManager().getBalance(player.getUniqueId(), currencyId);
            sender.sendMessage(plugin.getMessageManager().formatMessage("error.insufficient-funds",
                "%currency_symbol%", currency.getSymbol(),
                "%amount%", currency.formatAmount(amount),
                "%balance%", currency.formatAmount(balance)));
            return true;
        }
        
        // Handle confirmation system for donate currencies
        if (currency.needsConfirmation(amount)) {
            if (!player.hasPermission("foliaeconomyx.pay.bypass")) {
                return handleConfirmation(player, target, currency, amount);
            }
        }
        
        // Execute transfer
        executeTransfer(player, target, currency, amount);
        return true;
    }

    private boolean handleConfirmation(Player player, OfflinePlayer target, Currency currency, double amount) {
        String targetName = target.getName() != null ? target.getName() : target.getUniqueId().toString();
        
        // Check if needs confirmation
        if (plugin.getConfirmationManager().needsConfirmation(player, targetName, 
                currency.getId(), amount)) {
            // First attempt - add to pending
            plugin.getConfirmationManager().addConfirmation(player, targetName, 
                currency.getId(), amount, currency.getRequireConfirmation());
            
            // Send first confirmation message
            sendConfirmationMessage(player, 1, currency, amount, targetName);
            return true;
        }
        
        // Get current confirmation level
        PendingTransfer pending = plugin.getConfirmationManager().getPending(player.getUniqueId());
        
        if (pending == null) {
            // Start new confirmation
            plugin.getConfirmationManager().addConfirmation(player, targetName, 
                currency.getId(), amount, currency.getRequireConfirmation());
            sendConfirmationMessage(player, 1, currency, amount, targetName);
            return true;
        }
        
        // Check if expired
        if (pending.isExpired()) {
            plugin.getConfirmationManager().cancel(player.getUniqueId());
            player.sendMessage(plugin.getMessageManager().getMessage("pay.confirmation-expired"));
            return true;
        }
        
        // Increment confirmation level
        plugin.getConfirmationManager().addConfirmation(player, targetName, 
            currency.getId(), amount, currency.getRequireConfirmation());
        
        int currentLevel = plugin.getConfirmationManager().getCurrentLevel(player.getUniqueId());
        
        // Check if all confirmations complete
        if (plugin.getConfirmationManager().isConfirmed(player.getUniqueId())) {
            // Execute transfer
            executeTransfer(player, target, currency, amount);
            return true;
        }
        
        // Send next confirmation message
        sendConfirmationMessage(player, currentLevel + 1, currency, amount, targetName);
        return true;
    }

    private void sendConfirmationMessage(Player player, int level, Currency currency, 
                                        double amount, String targetName) {
        String message = plugin.getMessageManager().formatConfirmation(level, targetName, 
            currency.getId(), currency.getSymbol(), amount);
        player.sendMessage(message);
    }

    private void executeTransfer(Player sender, OfflinePlayer target, Currency currency, double amount) {
        // Double-check balance
        if (!plugin.getEconomyManager().has(sender.getUniqueId(), currency.getId(), amount)) {
            double balance = plugin.getEconomyManager().getBalance(sender.getUniqueId(), currency.getId());
            sender.sendMessage(plugin.getMessageManager().formatMessage("error.insufficient-funds",
                "%currency_symbol%", currency.getSymbol(),
                "%amount%", currency.formatAmount(amount),
                "%balance%", currency.formatAmount(balance)));
            return;
        }
        
        // Execute transfer
        if (plugin.getEconomyManager().transfer(sender.getUniqueId(), target.getUniqueId(), 
                currency.getId(), amount)) {
            
            // Notify sender
            String targetName = target.getName() != null ? target.getName() : target.getUniqueId().toString();
            sender.sendMessage(plugin.getMessageManager().formatTransfer("pay.success-sender",
                sender.getName(), targetName, currency.getId(), currency.getSymbol(), amount));
            
            // Notify receiver if online
            Player targetPlayer = target.getPlayer();
            if (targetPlayer != null && targetPlayer.isOnline()) {
                targetPlayer.sendMessage(plugin.getMessageManager().formatTransfer("pay.success-receiver",
                    sender.getName(), targetName, currency.getId(), currency.getSymbol(), amount));
            }
            
            // Log transaction if enabled
            if (plugin.getConfigManager().isTransactionLoggingEnabled()) {
                plugin.getLogger().info(String.format("[TRANSFER] %s -> %s: %s %s",
                    sender.getName(), targetName, currency.formatAmount(amount), currency.getId()));
            }
            
            // TODO: Send Discord webhook for donate currency transfers
            
        } else {
            sender.sendMessage("§cTransfer failed! Please contact an administrator.");
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            // Player names
            return Bukkit.getOnlinePlayers().stream()
                .map(Player::getName)
                .filter(name -> !name.equals(sender.getName()))
                .filter(name -> name.toLowerCase().startsWith(args[0].toLowerCase()))
                .collect(Collectors.toList());
        }
        
        if (args.length == 2) {
            // Amount suggestions
            return List.of("100", "500", "1000", "5000");
        }
        
        if (args.length == 3) {
            // Currency names
            return plugin.getCurrencyManager().getCurrencyIds().stream()
                .filter(c -> c.toLowerCase().startsWith(args[2].toLowerCase()))
                .collect(Collectors.toList());
        }
        
        return new ArrayList<>();
    }
}
