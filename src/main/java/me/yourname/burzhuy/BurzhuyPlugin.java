package me.yourname.burzhuy;

import me.yourname.burzhuy.data.BuyerDataManager;
import me.yourname.burzhuy.economy.EconomyManager;
import me.yourname.burzhuy.items.ItemPriceManager;
import me.yourname.burzhuy.menu.BuyerMenu;
import me.yourname.burzhuy.menu.MultiplierQuestMenu;
import me.yourname.burzhuy.menu.SlotUpgradeMenu;
import me.yourname.burzhuy.metrics.MetricsManager;
import me.yourname.burzhuy.quest.MultiplierQuestListener;
import me.yourname.burzhuy.quest.MultiplierQuestPool;
import me.yourname.burzhuy.scheduler.SchedulerAdapter;
import me.yourname.burzhuy.utils.MessageManager;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Main plugin class for BurzhuyPlugin
 * Supports both Paper and Folia schedulers
 */
public class BurzhuyPlugin extends JavaPlugin {

    private EconomyManager economyManager;
    private BuyerDataManager buyerDataManager;
    private ItemPriceManager itemPriceManager;
    private MessageManager messageManager;
    private SchedulerAdapter scheduler;
    
    private BuyerMenu buyerMenu;
    private SchedulerAdapter.TaskWrapper refreshTask;

    @Override
    public void onEnable() {
        // Save default config
        saveDefaultConfig();
        
        // Initialize message manager
        messageManager = new MessageManager(this);
        
        // Initialize scheduler adapter
        scheduler = new SchedulerAdapter(this);

        // Setup economy
        economyManager = new EconomyManager(this);
        if (!economyManager.setupEconomy()) {
            getLogger().severe(messageManager.getMessage("errors.vault_not_found"));
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // Initialize managers
        buyerDataManager = new BuyerDataManager(this);
        itemPriceManager = new ItemPriceManager(this);

        // Load quests from config
        MultiplierQuestPool.loadQuests(getConfig());

        // Create and link menus
        buyerMenu = new BuyerMenu(this, buyerDataManager, itemPriceManager, economyManager, messageManager, scheduler);
        SlotUpgradeMenu slotUpgradeMenu = new SlotUpgradeMenu(this, buyerDataManager, economyManager, itemPriceManager, null, buyerMenu, messageManager, scheduler);
        MultiplierQuestMenu multiplierQuestMenu = new MultiplierQuestMenu(this, buyerDataManager, slotUpgradeMenu, messageManager, scheduler);
        slotUpgradeMenu.setMultiplierQuestMenu(multiplierQuestMenu);

        // Register event listeners
        getServer().getPluginManager().registerEvents(buyerMenu, this);
        getServer().getPluginManager().registerEvents(slotUpgradeMenu, this);
        getServer().getPluginManager().registerEvents(multiplierQuestMenu, this);
        getServer().getPluginManager().registerEvents(new MultiplierQuestListener(buyerDataManager, getConfig(), messageManager, scheduler), this);

        // Register commands
        registerCommands(buyerMenu, slotUpgradeMenu, multiplierQuestMenu);

        // Setup lot refresh scheduler
        setupRefreshScheduler();

        // Initialize bStats metrics
        new MetricsManager(this).start();

        getLogger().info(messageManager.getMessage("success.plugin_enabled"));
    }

    @Override
    public void onDisable() {
        // Cancel refresh task
        if (refreshTask != null) {
            refreshTask.cancel();
        }
        
        // Stop all buyer menu timers
        if (buyerMenu != null) {
            buyerMenu.stopAllTimers();
        }
        
        // Save all data
        buyerDataManager.saveAll();
        
        getLogger().info(messageManager.getMessage("success.plugin_disabled"));
    }

    /**
     * Register all plugin commands
     */
    private void registerCommands(BuyerMenu buyerMenu, SlotUpgradeMenu slotUpgradeMenu, MultiplierQuestMenu multiplierQuestMenu) {
        getCommand("buyer").setExecutor(buyerMenu);
        getCommand("upgradebuyer").setExecutor(slotUpgradeMenu);
        getCommand("slotupgrademenu").setExecutor(slotUpgradeMenu);
        getCommand("multiplierquest").setExecutor(multiplierQuestMenu);
        
        // Admin command
        getCommand("refreshlots").setExecutor((sender, command, label, args) -> {
            if (!sender.hasPermission("burzhuy.admin")) {
                sender.sendMessage(messageManager.getMessage("messages.no_permission"));
                return true;
            }
            
            if (args.length != 1) {
                sender.sendMessage(messageManager.getMessage("admin.refresh_usage"));
                return true;
            }
            
            Player target = getServer().getPlayer(args[0]);
            if (target == null) {
                sender.sendMessage(messageManager.getMessage("messages.player_not_found"));
                return true;
            }
            
            buyerDataManager.refreshLotsFor(target, itemPriceManager.getPrices().keySet());
            
            sender.sendMessage(messageManager.getMessage("admin.lots_refreshed", "player", target.getName()));
            target.sendMessage(messageManager.getMessage("admin.lots_refreshed_target"));
            
            return true;
        });
    }

    /**
     * Setup the lot refresh scheduler compatible with both Paper and Folia
     */
    private void setupRefreshScheduler() {
        long intervalSeconds = getConfig().getLong("buyer_reset.interval_seconds", 21600); // Default 6 hours
        long intervalTicks = intervalSeconds * 20;

        // Calculate time until next reset
        long now = System.currentTimeMillis();
        long nextReset = getConfig().getLong("buyer_reset.next_reset", 0L);

        if (nextReset < now) {
            nextReset = now + intervalSeconds * 1000L;
            getConfig().set("buyer_reset.next_reset", nextReset);
            saveConfig();
        }

        long delayTicks = (nextReset - now) / 50; // Convert milliseconds to ticks

        // Schedule the refresh task
        refreshTask = scheduler.runTaskTimer(() -> {
            // Refresh all lots
            buyerDataManager.refreshAllLots(itemPriceManager.getPrices().keySet());
            
            // Notify all online players
            String message = messageManager.getMessage("messages.buyer_refreshed");
            
            // Use scheduler to ensure we're on the correct thread for Folia
            for (Player player : Bukkit.getOnlinePlayers()) {
                final Player p = player;
                scheduler.runAtEntity(p, () -> {
                    p.sendMessage(message);
                    p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 0.8f, 1.5f);
                });
            }
            
            getLogger().info("Lots for all players have been automatically refreshed.");

            // Set next reset time
            long newNextReset = System.currentTimeMillis() + intervalSeconds * 1000L;
            getConfig().set("buyer_reset.next_reset", newNextReset);
            saveConfig();
            
        }, delayTicks, intervalTicks);
    }

    // Getters for other classes
    public MessageManager getMessageManager() {
        return messageManager;
    }

    public SchedulerAdapter getScheduler() {
        return scheduler;
    }
    
    public BuyerDataManager getBuyerDataManager() {
        return buyerDataManager;
    }
    
    public ItemPriceManager getItemPriceManager() {
        return itemPriceManager;
    }
    
    public EconomyManager getEconomyManager() {
        return economyManager;
    }
    
    public BuyerMenu getBuyerMenu() {
        return buyerMenu;
    }
}
