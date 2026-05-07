package com.yourname.foliaeconomyx.storage.mysql;

import com.yourname.foliaeconomyx.FoliaEconomyX;
import com.yourname.foliaeconomyx.storage.BalanceStorage;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class MySQLStorage implements BalanceStorage {
    
    private final FoliaEconomyX plugin;
    private final Map<UUID, Map<String, Double>> balanceCache;
    private HikariDataSource dataSource;
    private boolean ready = false;

    public MySQLStorage(FoliaEconomyX plugin) {
        this.plugin = plugin;
        this.balanceCache = new ConcurrentHashMap<>();
    }

    @Override
    public void initialize() {
        try {
            HikariConfig config = new HikariConfig();
            
            String host = plugin.getConfig().getString("storage.mysql.host", "localhost");
            int port = plugin.getConfig().getInt("storage.mysql.port", 3306);
            String database = plugin.getConfig().getString("storage.mysql.database", "foliaeconomyx");
            String username = plugin.getConfig().getString("storage.mysql.username", "root");
            String password = plugin.getConfig().getString("storage.mysql.password", "password");
            
            config.setJdbcUrl("jdbc:mysql://" + host + ":" + port + "/" + database + 
                "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC");
            config.setUsername(username);
            config.setPassword(password);
            
            config.setMaximumPoolSize(plugin.getConfig().getInt("storage.mysql.pool.maximum-pool-size", 10));
            config.setMinimumIdle(plugin.getConfig().getInt("storage.mysql.pool.minimum-idle", 2));
            config.setConnectionTimeout(plugin.getConfig().getLong("storage.mysql.pool.connection-timeout", 30000));
            config.setIdleTimeout(plugin.getConfig().getLong("storage.mysql.pool.idle-timeout", 600000));
            config.setMaxLifetime(plugin.getConfig().getLong("storage.mysql.pool.max-lifetime", 1800000));
            
            config.addDataSourceProperty("cachePrepStmts", "true");
            config.addDataSourceProperty("prepStmtCacheSize", "250");
            config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
            
            dataSource = new HikariDataSource(config);
            
            createTables();
            ready = true;
            
            plugin.getLogger().info("MySQL storage initialized successfully");
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to initialize MySQL storage: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void createTables() {
        String balancesTable = "CREATE TABLE IF NOT EXISTS balances (" +
            "uuid VARCHAR(36) NOT NULL, " +
            "currency VARCHAR(32) NOT NULL, " +
            "balance DOUBLE NOT NULL DEFAULT 0, " +
            "PRIMARY KEY (uuid, currency), " +
            "INDEX idx_currency (currency), " +
            "INDEX idx_balance (balance)" +
            ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4";
        
        String transactionsTable = "CREATE TABLE IF NOT EXISTS transactions (" +
            "id BIGINT AUTO_INCREMENT PRIMARY KEY, " +
            "timestamp BIGINT NOT NULL, " +
            "type VARCHAR(32) NOT NULL, " +
            "from_uuid VARCHAR(36), " +
            "to_uuid VARCHAR(36), " +
            "currency VARCHAR(32) NOT NULL, " +
            "amount DOUBLE NOT NULL, " +
            "balance_after DOUBLE, " +
            "description TEXT, " +
            "INDEX idx_from (from_uuid), " +
            "INDEX idx_to (to_uuid), " +
            "INDEX idx_currency (currency), " +
            "INDEX idx_timestamp (timestamp)" +
            ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4";
        
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(balancesTable);
            stmt.execute(transactionsTable);
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to create tables: " + e.getMessage());
        }
    }

    @Override
    public CompletableFuture<Void> load() {
        return CompletableFuture.runAsync(() -> {
            String query = "SELECT uuid, currency, balance FROM balances";
            
            try (Connection conn = dataSource.getConnection();
                 Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(query)) {
                
                int count = 0;
                while (rs.next()) {
                    UUID uuid = UUID.fromString(rs.getString("uuid"));
                    String currency = rs.getString("currency");
                    double balance = rs.getDouble("balance");
                    
                    balanceCache.computeIfAbsent(uuid, k -> new ConcurrentHashMap<>())
                        .put(currency, balance);
                    count++;
                }
                
                plugin.getLogger().info("Loaded " + count + " balance entries from MySQL");
            } catch (SQLException e) {
                plugin.getLogger().severe("Failed to load balances: " + e.getMessage());
            }
        });
    }

    @Override
    public CompletableFuture<Void> save() {
        return CompletableFuture.runAsync(() -> {
            if (dataSource == null || dataSource.isClosed()) {
                plugin.getLogger().warning("Cannot save: MySQL connection not available");
                return;
            }
            
            String query = "INSERT INTO balances (uuid, currency, balance) VALUES (?, ?, ?) " +
                "ON DUPLICATE KEY UPDATE balance = VALUES(balance)";
            
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(query)) {
                
                int count = 0;
                for (Map.Entry<UUID, Map<String, Double>> entry : balanceCache.entrySet()) {
                    UUID uuid = entry.getKey();
                    for (Map.Entry<String, Double> currencyEntry : entry.getValue().entrySet()) {
                        stmt.setString(1, uuid.toString());
                        stmt.setString(2, currencyEntry.getKey());
                        stmt.setDouble(3, currencyEntry.getValue());
                        stmt.addBatch();
                        count++;
                    }
                }
                
                stmt.executeBatch();
                plugin.getLogger().info("Saved " + count + " balance entries to MySQL");
            } catch (SQLException e) {
                plugin.getLogger().severe("Failed to save balances: " + e.getMessage());
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
        
        CompletableFuture.runAsync(() -> {
            String query = "DELETE FROM balances WHERE uuid = ?";
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(query)) {
                stmt.setString(1, uuid.toString());
                stmt.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().severe("Failed to delete account: " + e.getMessage());
            }
        });
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
        // MySQL backups should be done via mysqldump externally
        plugin.getLogger().info("MySQL backups should be configured externally (mysqldump)");
    }

    @Override
    public void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            save().join(); // Wait for final save
            dataSource.close();
            plugin.getLogger().info("MySQL connection closed");
        }
    }

    @Override
    public boolean isReady() {
        return ready && dataSource != null && !dataSource.isClosed();
    }

    @Override
    public String getType() {
        return "MySQL";
    }
    
    public HikariDataSource getDataSource() {
        return dataSource;
    }
}
