package com.yourname.foliaeconomyx.confirmation;

import com.yourname.foliaeconomyx.FoliaEconomyX;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ConfirmationManager {
    
    private final FoliaEconomyX plugin;
    private final Map<UUID, PendingTransfer> pendingTransfers;

    public ConfirmationManager(FoliaEconomyX plugin) {
        this.plugin = plugin;
        this.pendingTransfers = new ConcurrentHashMap<>();
        startCleanupTask();
    }

    public boolean needsConfirmation(Player player, String target, String currency, double amount) {
        PendingTransfer pending = pendingTransfers.get(player.getUniqueId());
        
        if (pending == null) {
            return true; // First attempt, needs confirmation
        }
        
        // Check if same transfer
        if (!pending.isSameTransfer(target, currency, amount)) {
            // Different transfer, start over
            pendingTransfers.remove(player.getUniqueId());
            return true;
        }
        
        // Check if expired
        if (pending.isExpired()) {
            pendingTransfers.remove(player.getUniqueId());
            return true;
        }
        
        return false;
    }

    public int getCurrentLevel(UUID playerUuid) {
        PendingTransfer pending = pendingTransfers.get(playerUuid);
        return pending != null ? pending.getLevel() : 0;
    }

    public void addConfirmation(Player player, String target, String currency, double amount, int maxLevel) {
        PendingTransfer pending = pendingTransfers.get(player.getUniqueId());
        
        if (pending == null || !pending.isSameTransfer(target, currency, amount)) {
            // New transfer
            pending = new PendingTransfer(player.getName(), target, currency, amount, maxLevel);
            pendingTransfers.put(player.getUniqueId(), pending);
        } else {
            // Increment level
            pending.incrementLevel();
        }
    }

    public boolean isConfirmed(UUID playerUuid) {
        PendingTransfer pending = pendingTransfers.get(playerUuid);
        if (pending == null) return false;
        
        boolean confirmed = pending.isConfirmed();
        if (confirmed) {
            pendingTransfers.remove(playerUuid);
        }
        return confirmed;
    }

    public void cancel(UUID playerUuid) {
        pendingTransfers.remove(playerUuid);
    }

    public PendingTransfer getPending(UUID playerUuid) {
        return pendingTransfers.get(playerUuid);
    }

    private void startCleanupTask() {
        plugin.getServer().getAsyncScheduler().runAtFixedRate(
            plugin,
            task -> {
                long now = System.currentTimeMillis();
                pendingTransfers.entrySet().removeIf(entry -> 
                    entry.getValue().getTimestamp() + 60000 < now // 1 minute timeout
                );
            },
            30,
            30,
            java.util.concurrent.TimeUnit.SECONDS
        );
    }

    public void shutdown() {
        pendingTransfers.clear();
    }
}
