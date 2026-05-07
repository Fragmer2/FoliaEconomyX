package com.yourname.foliaeconomyx.storage.yaml;

import com.yourname.foliaeconomyx.FoliaEconomyX;
import com.yourname.foliaeconomyx.storage.BalanceStorage;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class YamlStorage implements BalanceStorage {
    
    private final FoliaEconomyX plugin;
    private final File dataFile;
    private final Map<UUID, Map<String, Double>> balanceCache;
    private YamlConfiguration config;
    private boolean ready = false;

    public YamlStorage(FoliaEconomyX plugin) {
        this.plugin = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "balances.yml");
        this.balanceCache = new ConcurrentHashMap<>();
    }

    @Override
    public void initialize() {
        if (!dataFile.exists()) {
            try {
                dataFile.getParentFile().mkdirs();
                dataFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Failed to create balances.yml: " + e.getMessage());
                return;
            }
        }
        ready = true;
        plugin.getLogger().info("YAML storage initialized successfully");
    }

    @Override
    public CompletableFuture<Void> load() {
        return CompletableFuture.runAsync(() -> {
            config = YamlConfiguration.loadConfiguration(dataFile);
            balanceCache.clear();
            
            ConfigurationSection balancesSection = config.getConfigurationSection("balances");
            if (balancesSection == null) {
                plugin.getLogger().info("No balances found in balances.yml (new file)");
                return;
            }
            
            int count = 0;
            for (String uuidStr : balancesSection.getKeys(false)) {
                try {
                    UUID uuid = UUID.fromString(uuidStr);
                    ConfigurationSection playerSection = balancesSection.getConfigurationSection(uuidStr);
                    
                    if (playerSection != null) {
                        Map<String, Double> currencies = new ConcurrentHashMap<>();
                        for (String currency : playerSection.getKeys(false)) {
                            double balance = playerSection.getDouble(currency);
                            currencies.put(currency, balance);
                            count++;
                        }
                        balanceCache.put(uuid, currencies);
                    }
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("Invalid UUID in balances.yml: " + uuidStr);
                }
            }
            
            plugin.getLogger().info("Loaded " + count + " balance entries from YAML");
        });
    }

    @Override
    public CompletableFuture<Void> save() {
        return CompletableFuture.runAsync(() -> {
            config = new YamlConfiguration();
            
            for (Map.Entry<UUID, Map<String, Double>> entry : balanceCache.entrySet()) {
                String uuidStr = entry.getKey().toString();
                for (Map.Entry<String, Double> currencyEntry : entry.getValue().entrySet()) {
                    config.set("balances." + uuidStr + "." + currencyEntry.getKey(), 
                        currencyEntry.getValue());
                }
            }
            
            try {
                config.save(dataFile);
                
                if (plugin.getConfig().getBoolean("storage.backups.enabled", true)) {
                    createBackup();
                }
                
                plugin.getLogger().info("Saved balances to YAML");
            } catch (IOException e) {
                plugin.getLogger().severe("Failed to save balances.yml: " + e.getMessage());
            }
        });
    }

    @Override
    public double getBalance(UUID uuid, String currency) {
        return balanceCache.getOrDefault(uuid, Collections.emptyMap())
            .getOrDefault(currency, 0.0);
    }

    @Override
    public void setBalance(UUID uuid, String currency, double amount) {
        balanceCache.computeIfAbsent(uuid, k -> new ConcurrentHashMap<>())
            .put(currency, amount);
    }

    @Override
    public boolean hasAccount(UUID uuid) {
        return balanceCache.containsKey(uuid);
    }

    @Override
    public boolean hasAccount(UUID uuid, String currency) {
        return balanceCache.getOrDefault(uuid, Collections.emptyMap())
            .containsKey(currency);
    }

    @Override
    public void createAccount(UUID uuid, String currency, double startingBalance) {
        setBalance(uuid, currency, startingBalance);
    }

    @Override
    public void deleteAccount(UUID uuid) {
        balanceCache.remove(uuid);
    }

    @Override
    public Map<String, Double> getAllBalances(UUID uuid) {
        return new HashMap<>(balanceCache.getOrDefault(uuid, Collections.emptyMap()));
    }

    @Override
    public Map<UUID, Double> getCurrencyBalances(String currency) {
        Map<UUID, Double> result = new HashMap<>();
        for (Map.Entry<UUID, Map<String, Double>> entry : balanceCache.entrySet()) {
            Double balance = entry.getValue().get(currency);
            if (balance != null) {
                result.put(entry.getKey(), balance);
            }
        }
        return result;
    }

    @Override
    public Set<UUID> getAllAccounts() {
        return new HashSet<>(balanceCache.keySet());
    }

    @Override
    public Map<UUID, Double> getTopBalances(String currency, int limit) {
        return getCurrencyBalances(currency).entrySet().stream()
            .sorted(Map.Entry.<UUID, Double>comparingByValue().reversed())
            .limit(limit)
            .collect(LinkedHashMap::new,
                (map, entry) -> map.put(entry.getKey(), entry.getValue()),
                LinkedHashMap::putAll);
    }

    @Override
    public void createBackup() {
        File backupDir = new File(plugin.getDataFolder(), "backups");
        if (!backupDir.exists()) {
            backupDir.mkdirs();
        }
        
        String timestamp = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new Date());
        File backupFile = new File(backupDir, "balances_" + timestamp + ".yml");
        
        try {
            Files.copy(dataFile.toPath(), backupFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            cleanOldBackups(backupDir);
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to create backup: " + e.getMessage());
        }
    }

    private void cleanOldBackups(File backupDir) {
        File[] backups = backupDir.listFiles((dir, name) -> 
            name.startsWith("balances_") && name.endsWith(".yml"));
        
        if (backups == null) return;
        
        int maxBackups = plugin.getConfig().getInt("storage.backups.max-backups", 5);
        if (backups.length > maxBackups) {
            Arrays.sort(backups, Comparator.comparingLong(File::lastModified));
            
            int toDelete = backups.length - maxBackups;
            for (int i = 0; i < toDelete; i++) {
                backups[i].delete();
            }
        }
    }

    @Override
    public void close() {
        save().join(); // Wait for final save
        plugin.getLogger().info("YAML storage closed");
    }

    @Override
    public boolean isReady() {
        return ready;
    }

    @Override
    public String getType() {
        return "YAML";
    }
}
