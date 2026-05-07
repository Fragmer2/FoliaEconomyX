package com.yourname.foliaeconomyx.utils;

import com.yourname.foliaeconomyx.FoliaEconomyX;
import org.bukkit.configuration.file.FileConfiguration;

public class ConfigManager {
    
    private final FoliaEconomyX plugin;
    private FileConfiguration config;

    public ConfigManager(FoliaEconomyX plugin) {
        this.plugin = plugin;
        this.config = plugin.getConfig();
    }

    public void reload() {
        plugin.reloadConfig();
        this.config = plugin.getConfig();
    }

    // Storage Settings
    public String getStorageType() {
        return config.getString("storage.type", "yaml");
    }

    public long getAutoSaveInterval() {
        return config.getLong("storage.auto-save-interval", 300);
    }

    public boolean isBackupsEnabled() {
        return config.getBoolean("storage.backups.enabled", true);
    }

    public int getMaxBackups() {
        return config.getInt("storage.backups.max-backups", 5);
    }

    // MySQL Settings
    public String getMySQLHost() {
        return config.getString("storage.mysql.host", "localhost");
    }

    public int getMySQLPort() {
        return config.getInt("storage.mysql.port", 3306);
    }

    public String getMySQLDatabase() {
        return config.getString("storage.mysql.database", "foliaeconomyx");
    }

    public String getMySQLUsername() {
        return config.getString("storage.mysql.username", "root");
    }

    public String getMySQLPassword() {
        return config.getString("storage.mysql.password", "password");
    }

    // Transaction Settings
    public boolean isTransactionLoggingEnabled() {
        return config.getBoolean("transactions.log-transactions", true);
    }

    public int getHistoryRetentionDays() {
        return config.getInt("transactions.history-retention-days", 90);
    }

    public boolean isRateLimitEnabled() {
        return config.getBoolean("transactions.rate-limit.enabled", true);
    }

    public int getMaxTransfersPerMinute() {
        return config.getInt("transactions.rate-limit.max-transfers-per-minute", 10);
    }

    public int getMaxTransfersPerHour() {
        return config.getInt("transactions.rate-limit.max-transfers-per-hour", 100);
    }

    // Baltop Settings
    public long getBaltopCacheDuration() {
        return config.getLong("baltop.cache-duration", 300);
    }

    public int getBaltopPlayersPerPage() {
        return config.getInt("baltop.players-per-page", 10);
    }

    public long getBaltopUpdateInterval() {
        return config.getLong("baltop.update-interval", 60);
    }

    // Performance Settings
    public boolean isAsyncOperationsEnabled() {
        return config.getBoolean("performance.async-operations", true);
    }

    public boolean isCacheEnabled() {
        return config.getBoolean("performance.enable-cache", true);
    }

    public int getCacheSize() {
        return config.getInt("performance.cache-size", 1000);
    }

    public int getNameCacheSize() {
        return config.getInt("performance.name-cache-size", 5000);
    }

    // Logging Settings
    public String getLogLevel() {
        return config.getString("logging.level", "INFO");
    }

    public boolean isDebugEnabled() {
        return config.getBoolean("logging.debug", false);
    }

    // Update Checker Settings
    public boolean isUpdateCheckerEnabled() {
        return config.getBoolean("update-checker.enabled", true);
    }

    public boolean shouldNotifyAdmins() {
        return config.getBoolean("update-checker.notify-admins", true);
    }

    public int getUpdateCheckInterval() {
        return config.getInt("update-checker.check-interval", 3600);
    }

    // Metrics Settings
    public boolean isMetricsEnabled() {
        return config.getBoolean("metrics.enabled", true);
    }

    // Donate Protection Settings
    public boolean isDonateLoggingEnabled() {
        return config.getBoolean("donate-protection.log-to-file", true);
    }

    public String getDonateLogFile() {
        return config.getString("donate-protection.log-file", "donate_transfers.log");
    }

    public boolean isDiscordWebhookEnabled() {
        return config.getBoolean("donate-protection.discord-webhook.enabled", false);
    }

    public String getDiscordWebhookUrl() {
        return config.getString("donate-protection.discord-webhook.url", "");
    }

    public boolean shouldNotifyOnTransfer() {
        return config.getBoolean("donate-protection.discord-webhook.notify-on-transfer", true);
    }

    public double getWebhookMinimumAmount() {
        return config.getDouble("donate-protection.discord-webhook.minimum-amount", 100);
    }

    // Confirmation Messages
    public String getConfirmationMessage(int level) {
        return config.getString("donate-protection.confirmation-messages.level-" + level, 
            "&cConfirmation required!");
    }

    // Group Limits
    public boolean isGroupLimitsEnabled() {
        return config.getBoolean("group-limits.enabled", false);
    }

    public double getGroupLimit(String group, String currency) {
        return config.getDouble("group-limits.limits." + group + "." + currency + ".max-balance", -1);
    }

    // Language
    public String getLanguage() {
        return config.getString("language", "en_US");
    }

    // PlaceholderAPI Settings
    public boolean shouldFormatLargeNumbers() {
        return config.getBoolean("placeholders.format-large-numbers", true);
    }

    public int getPlaceholderDecimalPlaces() {
        return config.getInt("placeholders.decimal-places", 2);
    }

    // Migration Settings
    public boolean shouldCreateMigrationBackup() {
        return config.getBoolean("migration.create-backup", true);
    }

    public boolean isEssentialsMigrationEnabled() {
        return config.getBoolean("migration.essentials.enabled", true);
    }

    public String getEssentialsDataFile() {
        return config.getString("migration.essentials.data-file", "plugins/Essentials/userdata");
    }

    public FileConfiguration getConfig() {
        return config;
    }
}
