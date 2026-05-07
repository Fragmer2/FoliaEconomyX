package me.yourname.burzhuy.utils;

import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * Manages all plugin messages with support for color codes and placeholders
 */
public class MessageManager {
    
    private final JavaPlugin plugin;
    private FileConfiguration messages;
    private final Map<String, String> messageCache = new HashMap<>();
    
    public MessageManager(JavaPlugin plugin) {
        this.plugin = plugin;
        loadMessages();
    }
    
    /**
     * Load messages from messages.yml
     */
    private void loadMessages() {
        File messagesFile = new File(plugin.getDataFolder(), "messages.yml");
        
        if (!messagesFile.exists()) {
            plugin.saveResource("messages.yml", false);
        }
        
        messages = YamlConfiguration.loadConfiguration(messagesFile);
        cacheMessages();
    }
    
    /**
     * Cache all messages for faster access
     */
    private void cacheMessages() {
        messageCache.clear();
        cacheSection("messages");
        cacheSection("buyer");
        cacheSection("slot_upgrade");
        cacheSection("quests");
        cacheSection("menu_titles");
        cacheSection("admin");
        cacheSection("errors");
        cacheSection("success");
    }
    
    private void cacheSection(String section) {
        if (messages.getConfigurationSection(section) != null) {
            for (String key : messages.getConfigurationSection(section).getKeys(true)) {
                String fullKey = section + "." + key;
                if (messages.isString(fullKey)) {
                    messageCache.put(fullKey, colorize(messages.getString(fullKey)));
                }
            }
        }
    }
    
    /**
     * Get a message by key with placeholder replacement
     * 
     * @param key Message key (e.g., "messages.buyer_refreshed")
     * @param placeholders Placeholder replacements (key, value, key, value...)
     * @return Formatted message with color codes
     */
    public String getMessage(String key, Object... placeholders) {
        String message = messageCache.getOrDefault(key, key);
        
        // Replace placeholders
        for (int i = 0; i < placeholders.length; i += 2) {
            if (i + 1 < placeholders.length) {
                String placeholder = "{" + placeholders[i] + "}";
                String value = String.valueOf(placeholders[i + 1]);
                message = message.replace(placeholder, value);
            }
        }
        
        return message;
    }
    
    /**
     * Get a raw message without placeholder replacement
     */
    public String getMessage(String key) {
        return messageCache.getOrDefault(key, colorize(messages.getString(key, key)));
    }
    
    /**
     * Convert color codes from & to §
     */
    private String colorize(String message) {
        if (message == null) return "";
        return ChatColor.translateAlternateColorCodes('&', message);
    }
    
    /**
     * Reload messages from file
     */
    public void reload() {
        loadMessages();
    }
}
