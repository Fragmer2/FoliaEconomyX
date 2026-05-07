package me.yourname.burzhuy.metrics;

import me.yourname.burzhuy.BurzhuyPlugin;
import org.bstats.bukkit.Metrics;

public class MetricsManager {

    private final BurzhuyPlugin plugin;
    private Metrics metrics;

    public MetricsManager(BurzhuyPlugin plugin) {
        this.plugin = plugin;
    }

    public void start() {
        // Plugin ID from bstats.org
        int pluginId = 31144;

        try {
            metrics = new Metrics(plugin, pluginId);
            plugin.getLogger().info("bStats metrics enabled!");
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to initialize bStats: " + e.getMessage());
        }
    }
}
