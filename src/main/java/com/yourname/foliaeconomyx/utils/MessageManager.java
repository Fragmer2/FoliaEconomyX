package com.yourname.foliaeconomyx.utils;

import com.yourname.foliaeconomyx.FoliaEconomyX;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class MessageManager {
    
    private final FoliaEconomyX plugin;
    private File messagesFile;
    private FileConfiguration messages;

    public MessageManager(FoliaEconomyX plugin) {
        this.plugin = plugin;
        loadMessages();
    }

    private void loadMessages() {
        messagesFile = new File(plugin.getDataFolder(), "messages.yml");
        
        if (!messagesFile.exists()) {
            plugin.saveResource("messages.yml", false);
        }
        
        messages = YamlConfiguration.loadConfiguration(messagesFile);
        
        // Load defaults
        InputStream defaultStream = plugin.getResource("messages.yml");
        if (defaultStream != null) {
            YamlConfiguration defaultConfig = YamlConfiguration.loadConfiguration(
                new InputStreamReader(defaultStream));
            messages.setDefaults(defaultConfig);
        }
    }

    public void reload() {
        messages = YamlConfiguration.loadConfiguration(messagesFile);
    }

    public String getMessage(String path) {
        return getMessage(path, "");
    }

    public String getMessage(String path, String defaultMessage) {
        String message = messages.getString(path, defaultMessage);
        String prefix = messages.getString("prefix", "");
        
        if (message.isEmpty()) {
            return "";
        }
        
        return ChatColor.translateAlternateColorCodes('&', prefix + message);
    }

    public String getMessageNoPrefix(String path) {
        String message = messages.getString(path, "");
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    public String formatMessage(String path, String... replacements) {
        String message = getMessage(path);
        
        if (replacements.length % 2 != 0) {
            plugin.getLogger().warning("Invalid replacements for message: " + path);
            return message;
        }
        
        for (int i = 0; i < replacements.length; i += 2) {
            String placeholder = replacements[i];
            String value = replacements[i + 1];
            message = message.replace(placeholder, value);
        }
        
        return message;
    }

    public void save() {
        try {
            messages.save(messagesFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save messages.yml: " + e.getMessage());
        }
    }

    // Utility methods for common placeholders
    public String formatBalance(String path, String currency, String currencySymbol, double amount) {
        return formatMessage(path,
            "%currency%", currency,
            "%currency_symbol%", currencySymbol,
            "%amount%", String.format("%.2f", amount),
            "%balance%", String.format("%.2f", amount)
        );
    }

    public String formatPlayer(String path, String playerName) {
        return formatMessage(path, "%player%", playerName);
    }

    public String formatCurrency(String path, String currency, String symbol) {
        return formatMessage(path,
            "%currency%", currency,
            "%currency_symbol%", symbol,
            "%currency_name%", currency
        );
    }

    public String formatTransfer(String path, String from, String to, String currency, 
                                 String currencySymbol, double amount) {
        return formatMessage(path,
            "%from%", from,
            "%to%", to,
            "%player%", to,
            "%currency%", currency,
            "%currency_symbol%", currencySymbol,
            "%amount%", String.format("%.2f", amount)
        );
    }

    public String formatConfirmation(int level, String player, String currency, 
                                     String currencySymbol, double amount) {
        String basePath = "pay.confirmation-" + level;
        
        String message = getMessage(basePath);
        message = message.replace("%player%", player);
        message = message.replace("%currency%", currency);
        message = message.replace("%currency_symbol%", currencySymbol);
        message = message.replace("%amount%", String.format("%.2f", amount));
        
        // Add details and action messages
        String details = getMessageNoPrefix(basePath + "-details");
        String action = getMessageNoPrefix(basePath + "-action");
        
        if (!details.isEmpty()) {
            details = details.replace("%player%", player)
                           .replace("%currency_symbol%", currencySymbol)
                           .replace("%amount%", String.format("%.2f", amount));
            message += "\n" + details;
        }
        
        if (level == 3) {
            String details2 = getMessageNoPrefix(basePath + "-details-2");
            if (!details2.isEmpty()) {
                details2 = details2.replace("%player%", player)
                                 .replace("%currency_symbol%", currencySymbol)
                                 .replace("%amount%", String.format("%.2f", amount));
                message += "\n" + details2;
            }
        }
        
        if (!action.isEmpty()) {
            message += "\n" + action;
        }
        
        return message;
    }

    public String formatError(String errorType, String... replacements) {
        return formatMessage("error." + errorType, replacements);
    }

    public String formatTime(long seconds) {
        if (seconds < 60) {
            return formatMessage("time.seconds", "%s", String.valueOf(seconds));
        } else if (seconds < 3600) {
            return formatMessage("time.minutes", "%s", String.valueOf(seconds / 60));
        } else if (seconds < 86400) {
            return formatMessage("time.hours", "%s", String.valueOf(seconds / 3600));
        } else {
            return formatMessage("time.days", "%s", String.valueOf(seconds / 86400));
        }
    }

    public String formatNumber(double number) {
        if (number >= 1_000_000_000_000L) {
            return String.format("%.1f%s", number / 1_000_000_000_000.0, 
                messages.getString("numbers.trillion", "T"));
        } else if (number >= 1_000_000_000) {
            return String.format("%.1f%s", number / 1_000_000_000.0, 
                messages.getString("numbers.billion", "B"));
        } else if (number >= 1_000_000) {
            return String.format("%.1f%s", number / 1_000_000.0, 
                messages.getString("numbers.million", "M"));
        } else if (number >= 1_000) {
            return String.format("%.1f%s", number / 1_000.0, 
                messages.getString("numbers.thousand", "K"));
        } else {
            return String.format("%.2f", number);
        }
    }
}
