package com.yourname.foliaeconomyx.metrics;

import com.yourname.foliaeconomyx.FoliaEconomyX;
import org.bstats.bukkit.Metrics;
import org.bstats.charts.SimplePie;
import org.bstats.charts.SingleLineChart;

public class MetricsManager {
    
    private final FoliaEconomyX plugin;
    private Metrics metrics;
    
    public MetricsManager(FoliaEconomyX plugin) {
        this.plugin = plugin;
    }
    
    public void start() {
        // Plugin ID from bstats.org (you need to register your plugin there)
        // For now using a placeholder ID - replace with your actual ID
        int pluginId = 31143; // ← ИЗМЕНИ НА СВОЙ ID с bstats.org
        
        metrics = new Metrics(plugin, pluginId);
        
        // Custom charts
        addCustomCharts();
        
        plugin.getLogger().info("bStats metrics enabled!");
    }
    
    private void addCustomCharts() {
        // Storage type chart
        metrics.addCustomChart(new SimplePie("storage_type", 
            () -> plugin.getEconomyManager().getStorage().getType()
        ));
        
        // Number of currencies
        metrics.addCustomChart(new SingleLineChart("currencies_count", 
            () -> plugin.getCurrencyManager().getTotalCurrencies()
        ));
        
        // Total accounts
        metrics.addCustomChart(new SingleLineChart("total_accounts", 
            () -> plugin.getEconomyManager().getStorage().getAllAccounts().size()
        ));
    }
}
