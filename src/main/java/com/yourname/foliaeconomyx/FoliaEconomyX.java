package com.yourname.foliaeconomyx;

import com.yourname.foliaeconomyx.api.EconomyManager;
import com.yourname.foliaeconomyx.api.VaultEconomyProvider;
import com.yourname.foliaeconomyx.commands.BaltopCommand;
import com.yourname.foliaeconomyx.commands.EcoCommand;
import com.yourname.foliaeconomyx.commands.MoneyCommand;
import com.yourname.foliaeconomyx.commands.PayCommand;
import com.yourname.foliaeconomyx.confirmation.ConfirmationManager;
import com.yourname.foliaeconomyx.currency.CurrencyManager;
import com.yourname.foliaeconomyx.storage.BalanceStorage;
import com.yourname.foliaeconomyx.storage.mysql.MySQLStorage;
import com.yourname.foliaeconomyx.storage.yaml.YamlStorage;
import com.yourname.foliaeconomyx.utils.ConfigManager;
import com.yourname.foliaeconomyx.utils.MessageManager;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.concurrent.TimeUnit;

public class FoliaEconomyX extends JavaPlugin {
    
    private CurrencyManager currencyManager;
    private BalanceStorage storage;
    private EconomyManager economyManager;
    private ConfirmationManager confirmationManager;
    private ConfigManager configManager;
    private MessageManager messageManager;
    private VaultEconomyProvider vaultProvider;

    @Override
    public void onEnable() {
        try {
            // 1. Save default configs
            saveDefaultConfig();
            saveResource("messages.yml", false);
            
            // 2. Initialize managers
            configManager = new ConfigManager(this);
            messageManager = new MessageManager(this);
            
            // 3. Initialize currency manager
            currencyManager = new CurrencyManager(this);
            
            // 4. Initialize storage
            String storageType = getConfig().getString("storage.type", "yaml").toLowerCase();
            getLogger().info("Storage type: " + storageType);
            
            switch (storageType) {
                case "mysql":
                    try {
                        storage = new MySQLStorage(this);
                        storage.initialize();
                        if (!storage.isReady()) {
                            throw new RuntimeException("MySQL storage not ready");
                        }
                        getLogger().info("MySQL storage initialized successfully");
                    } catch (Exception e) {
                        getLogger().warning("Failed to initialize MySQL storage: " + e.getMessage());
                        getLogger().info("Falling back to YAML storage...");
                        storage = new YamlStorage(this);
                        storage.initialize();
                    }
                    break;
                case "postgresql":
                    try {
                        storage = new MySQLStorage(this);
                        storage.initialize();
                        if (!storage.isReady()) {
                            throw new RuntimeException("PostgreSQL storage not ready");
                        }
                        getLogger().info("PostgreSQL storage initialized successfully");
                    } catch (Exception e) {
                        getLogger().warning("Failed to initialize PostgreSQL storage: " + e.getMessage());
                        getLogger().info("Falling back to YAML storage...");
                        storage = new YamlStorage(this);
                        storage.initialize();
                    }
                    break;
                default:
                    storage = new YamlStorage(this);
                    storage.initialize();
                    getLogger().info("YAML storage initialized successfully");
                    break;
            }
            
            if (!storage.isReady()) {
                getLogger().severe("All storage methods failed! Disabling plugin...");
                getServer().getPluginManager().disablePlugin(this);
                return;
            }
            
            storage.load().join(); // Wait for load to complete
            
            // 5. Initialize economy manager
            economyManager = new EconomyManager(this, storage);
            
            // 6. Initialize confirmation manager
            confirmationManager = new ConfirmationManager(this);
            
            // 7. Setup Vault
            if (!setupVault()) {
                getLogger().warning("Vault not found! Some plugins may not work.");
            }
            
            // 7.5. Setup PlaceholderAPI
            if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
                new com.yourname.foliaeconomyx.placeholders.EconomyExpansion(this).register();
                getLogger().info("PlaceholderAPI hooked successfully!");
            }
            
            // 8. Register commands
            registerCommands();
            
            // 9. Register listeners
            // TODO: Add PlayerListener when implemented
            
            // 10. Start auto-save task
            startAutoSave();
            
            // 11. Initialize bStats
            if (configManager.isMetricsEnabled()) {
                new com.yourname.foliaeconomyx.metrics.MetricsManager(this).start();
            }
            
            getLogger().info("FoliaEconomyX v" + getDescription().getVersion() + " enabled!");
            getLogger().info("Storage: " + storage.getType());
            getLogger().info("Currencies: " + currencyManager.getTotalCurrencies());
            
        } catch (Exception e) {
            getLogger().severe("Failed to enable plugin: " + e.getMessage());
            e.printStackTrace();
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    @Override
    public void onDisable() {
        try {
            // Save all data
            if (storage != null) {
                getLogger().info("Saving economy data...");
                storage.save().join(); // Wait for save to complete
                storage.close();
            }
            
            // Unregister Vault
            if (vaultProvider != null) {
                getServer().getServicesManager().unregister(Economy.class, vaultProvider);
            }
            
            // Shutdown confirmation manager
            if (confirmationManager != null) {
                confirmationManager.shutdown();
            }
            
            getLogger().info("FoliaEconomyX disabled!");
            
        } catch (Exception e) {
            getLogger().severe("Error during shutdown: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private boolean setupVault() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        
        vaultProvider = new VaultEconomyProvider(this);
        getServer().getServicesManager().register(
            Economy.class,
            vaultProvider,
            this,
            ServicePriority.Highest
        );
        
        getLogger().info("Successfully hooked into Vault!");
        return true;
    }

    private void registerCommands() {
        getCommand("money").setExecutor(new MoneyCommand(this));
        getCommand("pay").setExecutor(new PayCommand(this));
        getCommand("eco").setExecutor(new EcoCommand(this));
        getCommand("baltop").setExecutor(new BaltopCommand(this));
    }

    private void startAutoSave() {
        long interval = getConfig().getLong("storage.auto-save-interval", 300);
        
        getServer().getAsyncScheduler().runAtFixedRate(
            this,
            task -> {
                if (storage != null && storage.isReady()) {
                    storage.save();
                }
            },
            interval,
            interval,
            TimeUnit.SECONDS
        );
        
        getLogger().info("Auto-save enabled (interval: " + interval + "s)");
    }

    // Getters
    public CurrencyManager getCurrencyManager() {
        return currencyManager;
    }

    public BalanceStorage getStorage() {
        return storage;
    }

    public EconomyManager getEconomyManager() {
        return economyManager;
    }

    public ConfirmationManager getConfirmationManager() {
        return confirmationManager;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public MessageManager getMessageManager() {
        return messageManager;
    }
}
