package me.yourname.burzhuy.items;

import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;

/**
 * Manages item prices loaded from configuration
 */
public class ItemPriceManager {
    
    private final Map<Material, ItemPrice> prices = new HashMap<>();
    private final Map<Material, Integer> lotAmounts = new HashMap<>();
    private final JavaPlugin plugin;

    public ItemPriceManager(JavaPlugin plugin) {
        this.plugin = plugin;
        FileConfiguration config = plugin.getConfig();
        
        // Load prices from all sections
        loadPrices(config, "item_prices.blocks");
        loadPrices(config, "item_prices.wood");
        loadPrices(config, "item_prices.ores");
        loadPrices(config, "item_prices.food");
        loadPrices(config, "item_prices.mob_drops");
        loadPrices(config, "item_prices.plants");
        loadPrices(config, "item_prices.special");
        loadPrices(config, "item_prices.potions");
        loadPrices(config, "item_prices.new_items");
        
        loadAmounts(config);
    }

    /**
     * Load prices from a config section
     */
    private void loadPrices(FileConfiguration config, String section) {
        if (config.getConfigurationSection(section) != null) {
            for (String key : config.getConfigurationSection(section).getKeys(false)) {
                try {
                    Material material = Material.valueOf(key.toUpperCase());
                    double price = config.getDouble(section + "." + key);
                    prices.put(material, new ItemPrice(price, formatMaterialName(material)));
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning(String.format(
                        "Unknown material in config (%s): %s", section, key
                    ));
                }
            }
        }
    }

    /**
     * Load custom lot amounts
     */
    private void loadAmounts(FileConfiguration config) {
        if (config.getConfigurationSection("lot_amounts") != null) {
            for (String key : config.getConfigurationSection("lot_amounts").getKeys(false)) {
                try {
                    Material material = Material.valueOf(key.toUpperCase());
                    int amount = config.getInt("lot_amounts." + key);
                    lotAmounts.put(material, amount);
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning(String.format(
                        "Unknown material in lot_amounts: %s", key
                    ));
                }
            }
        }
    }

    /**
     * Format material name for display
     */
    private String formatMaterialName(Material material) {
        String name = material.name().toLowerCase().replace("_", " ");
        StringBuilder result = new StringBuilder();
        
        for (String word : name.split(" ")) {
            if (result.length() > 0) {
                result.append(" ");
            }
            result.append(Character.toUpperCase(word.charAt(0)))
                  .append(word.substring(1));
        }
        
        return result.toString();
    }

    public Map<Material, ItemPrice> getPrices() {
        return prices;
    }

    public ItemPrice getPrice(Material material) {
        return prices.get(material);
    }

    public int getLotAmount(Material material) {
        return lotAmounts.getOrDefault(material, 64);
    }
}
