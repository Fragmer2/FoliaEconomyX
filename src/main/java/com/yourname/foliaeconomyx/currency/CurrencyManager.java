package com.yourname.foliaeconomyx.currency;

import com.yourname.foliaeconomyx.FoliaEconomyX;
import org.bukkit.configuration.ConfigurationSection;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class CurrencyManager {
    
    private final FoliaEconomyX plugin;
    private final Map<String, Currency> currencies;
    private String defaultCurrency;

    public CurrencyManager(FoliaEconomyX plugin) {
        this.plugin = plugin;
        this.currencies = new ConcurrentHashMap<>();
        loadCurrencies();
    }

    public void loadCurrencies() {
        currencies.clear();
        
        ConfigurationSection currenciesSection = plugin.getConfig().getConfigurationSection("currencies");
        if (currenciesSection == null) {
            plugin.getLogger().severe("No currencies defined in config.yml!");
            return;
        }
        
        // Load default currency
        defaultCurrency = currenciesSection.getString("default", "dollars");
        
        // Load regular currencies
        loadCurrencyType(currenciesSection, "regular", CurrencyType.REGULAR);
        
        // Load donate currencies
        loadCurrencyType(currenciesSection, "donate", CurrencyType.DONATE);
        
        plugin.getLogger().info("Loaded " + currencies.size() + " currencies (" +
                getRegularCurrencies().size() + " regular, " +
                getDonateCurrencies().size() + " donate)");
    }

    private void loadCurrencyType(ConfigurationSection parent, String section, CurrencyType type) {
        ConfigurationSection typeSection = parent.getConfigurationSection(section);
        if (typeSection == null) return;
        
        for (String currencyId : typeSection.getKeys(false)) {
            ConfigurationSection currencySection = typeSection.getConfigurationSection(currencyId);
            if (currencySection == null) continue;
            
            boolean enabled = currencySection.getBoolean("enabled", true);
            if (!enabled) continue;
            
            Currency currency = new Currency(
                currencyId,
                type,
                currencySection.getString("symbol", "$"),
                currencySection.getString("name-singular", "Dollar"),
                currencySection.getString("name-plural", "Dollars"),
                currencySection.getString("format", "$%amount%"),
                currencySection.getInt("decimal-places", 2),
                currencySection.getDouble("starting-balance", 0.0),
                currencySection.getDouble("max-balance", -1),
                currencySection.getBoolean("allow-negative", false),
                currencySection.getBoolean("allow-transfer", true),
                currencySection.getInt("require-confirmation", 0),
                currencySection.getInt("confirmation-timeout", 30),
                currencySection.getDouble("confirmation-threshold", 0.0),
                currencySection.getInt("transfer-cooldown", 0),
                enabled
            );
            
            currencies.put(currencyId, currency);
        }
    }

    public Currency getCurrency(String id) {
        return currencies.get(id.toLowerCase());
    }

    public Currency getDefaultCurrency() {
        return getCurrency(defaultCurrency);
    }

    public String getDefaultCurrencyId() {
        return defaultCurrency;
    }

    public Collection<Currency> getAllCurrencies() {
        return currencies.values();
    }

    public List<Currency> getRegularCurrencies() {
        return currencies.values().stream()
            .filter(c -> c.getType() == CurrencyType.REGULAR)
            .toList();
    }

    public List<Currency> getDonateCurrencies() {
        return currencies.values().stream()
            .filter(c -> c.getType() == CurrencyType.DONATE)
            .toList();
    }

    public boolean exists(String id) {
        return currencies.containsKey(id.toLowerCase());
    }

    public boolean isEnabled(String id) {
        Currency currency = getCurrency(id);
        return currency != null && currency.isEnabled();
    }

    public Set<String> getCurrencyIds() {
        return currencies.keySet();
    }

    public int getTotalCurrencies() {
        return currencies.size();
    }

    public void reload() {
        loadCurrencies();
    }
}
