package me.yourname.burzhuy.menu;

import me.yourname.burzhuy.data.BuyerData;
import me.yourname.burzhuy.data.BuyerDataManager;
import me.yourname.burzhuy.economy.EconomyManager;
import me.yourname.burzhuy.items.ItemPrice;
import me.yourname.burzhuy.items.ItemPriceManager;
import me.yourname.burzhuy.scheduler.SchedulerAdapter;
import me.yourname.burzhuy.utils.MessageManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;

/**
 * Main buyer menu with auto-updating timer support for both Paper and Folia
 */
public class BuyerMenu implements Listener, CommandExecutor {

    private final BuyerDataManager dataManager;
    private final ItemPriceManager priceManager;
    private final EconomyManager economyManager;
    private final MessageManager messageManager;
    private final SchedulerAdapter scheduler;
    private final JavaPlugin plugin;
    private final FileConfiguration config;

    // Timer tasks for info slot updates
    private final Map<UUID, SchedulerAdapter.TaskWrapper> infoTimers = new HashMap<>();

    public BuyerMenu(JavaPlugin plugin, BuyerDataManager dataManager, ItemPriceManager priceManager, 
                     EconomyManager economyManager, MessageManager messageManager, SchedulerAdapter scheduler) {
        this.plugin = plugin;
        this.dataManager = dataManager;
        this.priceManager = priceManager;
        this.economyManager = economyManager;
        this.messageManager = messageManager;
        this.scheduler = scheduler;
        this.config = plugin.getConfig();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(messageManager.getMessage("messages.player_only"));
            return true;
        }
        
        Player player = (Player) sender;
        BuyerData data = dataManager.get(player);
        
        if (data.getLots() == null || data.getLots().isEmpty()) {
            data.refreshLots(priceManager.getPrices().keySet());
            dataManager.saveAll();
        }
        
        open(player);
        return true;
    }

    /**
     * Open the buyer menu for a player
     */
    public void open(Player player) {
        BuyerData data = dataManager.get(player);

        String title = config.getString("menu.buyer.title", "&6&l☀ Buyer ☀").replace("&", "§");
        int size = config.getInt("menu.buyer.size", 54);
        Inventory inv = Bukkit.createInventory(null, size, title);

        // Fill with glass panes
        Material glassMat = Material.valueOf(config.getString("menu.buyer.glass.material", "ORANGE_STAINED_GLASS_PANE"));
        String glassName = config.getString("menu.buyer.glass.name", "&e ");
        ItemStack glass = createItem(glassMat, glassName, Collections.emptyList());
        
        for (int i = 0; i < size; i++) {
            if (i < 9 || i > 44 || i % 9 == 0 || i % 9 == 8) {
                inv.setItem(i, glass);
            }
        }

        // Info slot with timer
        updateInfoSlot(player, inv);

        // Upgrade button
        int upgradeSlot = config.getInt("menu.buyer.upgrade.slot", 8);
        Material upgradeMat = Material.valueOf(config.getString("menu.buyer.upgrade.material", "CHEST"));
        String upgradeName = config.getString("menu.buyer.upgrade.name", "&6&lUpgrade Slots");
        List<String> upgradeLore = config.getStringList("menu.buyer.upgrade.lore");
        inv.setItem(upgradeSlot, createItem(upgradeMat, upgradeName, upgradeLore));

        // Sell slots
        List<Integer> sellSlots = config.getIntegerList("menu.buyer.sell_slots");
        int maxSlots = getMaxSlots(player);
        
        if (sellSlots.size() < maxSlots) {
            player.sendMessage(String.format(
                "&c[Error] Your slot limit (%d) exceeds the number of sell_slots (%d) in config! Contact administration.",
                maxSlots, sellSlots.size()
            ));
        }
        
        List<Material> lots = data.getLots();
        int maxItems = Math.min(data.getSlots(), sellSlots.size());
        
        // Calculate multiplier
        double baseMultiplier = config.getDouble("multiplier.base", 1.0);
        double perLevel = config.getDouble("multiplier.per_level", 0.4);
        double multiplier = baseMultiplier + data.getMultiplierLevel() * perLevel;

        for (int i = 0; i < Math.min(lots.size(), maxItems); i++) {
            Material material = lots.get(i);
            ItemPrice price = priceManager.getPrice(material);
            if (price == null) continue;
            
            int lotAmount = priceManager.getLotAmount(material);
            int slot = sellSlots.get(i);
            
            String name = config.getString("menu.buyer.sell_item.name", "&e{name}")
                    .replace("{name}", price.getName());
            
            List<String> lore = new ArrayList<>();
            double displayPrice = price.getPrice() * multiplier;
            
            for (String line : config.getStringList("menu.buyer.sell_item.lore")) {
                line = line.replace("{price}", String.format("%.2f", displayPrice))
                           .replace("{amount}", String.valueOf(lotAmount))
                           .replace("{multiplier}", String.format("%.2f", multiplier));
                lore.add(line);
            }
            
            lore.add("&eLMB: Sell 1 item");
            lore.add("&eRMB: Sell 64");
            lore.add("&eShift+RMB: Sell all");
            
            inv.setItem(slot, createItem(material, name, lore));
        }
        
        player.openInventory(inv);
        startInfoTimer(player);
    }

    /**
     * Update the info slot with current timer
     */
    public void updateInfoSlot(Player player, Inventory inv) {
        try {
            BuyerData data = dataManager.get(player);
            if (data == null) {
                plugin.getLogger().warning("BuyerData is null for player " + player.getName());
                return;
            }

            // Calculate timer
            long nextReset = config.getLong("buyer_reset.next_reset", 0L);
            long now = System.currentTimeMillis();
            long millisLeft = Math.max(0, nextReset - now);
            long totalSeconds = millisLeft / 1000;
            long hours = totalSeconds / 3600;
            long minutes = (totalSeconds % 3600) / 60;
            long seconds = totalSeconds % 60;
            String timerString = String.format("%02d:%02d:%02d", hours, minutes, seconds);

            int infoSlot = config.getInt("menu.buyer.info.slot", 4);
            
            if (infoSlot >= inv.getSize() || infoSlot < 0) {
                plugin.getLogger().warning("Invalid info slot: " + infoSlot);
                return;
            }
            
            Material infoMat = Material.valueOf(config.getString("menu.buyer.info.material", "GOLD_INGOT"));
            String infoName = config.getString("menu.buyer.info.name", "&6&lInformation");
            List<String> infoLore = new ArrayList<>();
            
            double baseMultiplier = config.getDouble("multiplier.base", 1.0);
            double perLevel = config.getDouble("multiplier.per_level", 0.4);
            double multiplier = baseMultiplier + data.getMultiplierLevel() * perLevel;
            
            for (String line : config.getStringList("menu.buyer.info.lore")) {
                line = line.replace("{slots}", String.valueOf(data.getSlots()))
                           .replace("{max_slots}", String.valueOf(getMaxSlots(player)))
                           .replace("{multiplier}", String.format("%.2f", multiplier))
                           .replace("{reset_timer}", timerString);
                infoLore.add(line);
            }
            
            inv.setItem(infoSlot, createItem(infoMat, infoName, infoLore));
            
        } catch (Exception e) {
            plugin.getLogger().severe("Critical error in updateInfoSlot for player " + player.getName() + ": " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    /**
     * Start timer for auto-updating info slot (Folia-compatible)
     */
    public void startInfoTimer(Player player) {
        stopInfoTimer(player);

        SchedulerAdapter.TaskWrapper task = scheduler.runTaskTimer(() -> {
            if (!player.isOnline()) {
                stopInfoTimer(player);
                return;
            }
            
            Inventory openInv = player.getOpenInventory().getTopInventory();
            if (openInv == null || openInv.getSize() != config.getInt("menu.buyer.size", 54)) {
                stopInfoTimer(player);
                return;
            }
            
            String currentTitle = player.getOpenInventory().getTitle();
            String expectedTitle = config.getString("menu.buyer.title", "&6&l☀ Buyer ☀");
            if (currentTitle == null || !currentTitle.equals(expectedTitle)) {
                stopInfoTimer(player);
                return;
            }
            
            try {
                updateInfoSlot(player, openInv);
            } catch (Exception e) {
                plugin.getLogger().warning("Error updating timer for player " + player.getName() + ": " + e.getMessage());
                stopInfoTimer(player);
            }
        }, 20L, 20L); // Once per second

        if (task != null) {
            infoTimers.put(player.getUniqueId(), task);
        } else {
            plugin.getLogger().warning("Failed to start timer for player " + player.getName());
        }
    }

    /**
     * Stop timer for a player
     */
    public void stopInfoTimer(Player player) {
        UUID playerId = player.getUniqueId();
        SchedulerAdapter.TaskWrapper task = infoTimers.get(playerId);
        if (task != null) {
            try {
                task.cancel();
            } catch (Exception e) {
                plugin.getLogger().warning("Error stopping timer for player " + player.getName() + ": " + e.getMessage());
            } finally {
                infoTimers.remove(playerId);
            }
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        stopInfoTimer(event.getPlayer());
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getPlayer() instanceof Player) {
            Player player = (Player) event.getPlayer();
            String title = event.getView().getTitle();
            String expectedTitle = config.getString("menu.buyer.title", "&6&l☀ Buyer ☀");
            if (title != null && title.equals(expectedTitle)) {
                stopInfoTimer(player);
            }
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        
        Player player = (Player) event.getWhoClicked();
        String title = event.getView().getTitle();
        
        if (!title.equals(config.getString("menu.buyer.title", "&6&l☀ Buyer ☀").replace("&", "§"))) return;
        
        event.setCancelled(true);

        int upgradeSlot = config.getInt("menu.buyer.upgrade.slot", 8);
        if (event.getRawSlot() == upgradeSlot) {
            // Use entity scheduler for player commands on Folia
            scheduler.runAtEntity(player, () -> Bukkit.dispatchCommand(player, "upgradebuyer"));
            return;
        }

        List<Integer> sellSlots = config.getIntegerList("menu.buyer.sell_slots");
        if (sellSlots.contains(event.getRawSlot())) {
            ItemStack clicked = event.getCurrentItem();
            if (clicked == null || !clicked.hasItemMeta()) return;
            
            Material mat = clicked.getType();
            BuyerData data = dataManager.get(player);
            if (!data.getLots().contains(mat)) return;

            if (event.isLeftClick() && !event.isShiftClick()) {
                sellAmount(player, mat, 1);
                return;
            }
            if (event.isRightClick() && event.isShiftClick()) {
                int total = countItems(player, mat);
                if (total > 0) sellAmount(player, mat, total);
                return;
            }
            if (event.isRightClick() && !event.isShiftClick()) {
                int lotAmount = priceManager.getLotAmount(mat);
                sellAmount(player, mat, lotAmount);
                return;
            }
        }
    }

    private void sellAmount(Player player, Material material, int amount) {
        int totalInInv = countItems(player, material);
        if (totalInInv < amount) {
            player.sendMessage(messageManager.getMessage("buyer.insufficient_items"));
            return;
        }
        
        removeItems(player, material, amount);
        ItemPrice price = priceManager.getPrice(material);
        BuyerData data = dataManager.get(player);
        
        double baseMultiplier = config.getDouble("multiplier.base", 1.0);
        double perLevel = config.getDouble("multiplier.per_level", 0.4);
        double multiplier = baseMultiplier + data.getMultiplierLevel() * perLevel;
        
        double total = price.getPrice() * amount * multiplier;
        economyManager.deposit(player, total);
        
        player.sendMessage(messageManager.getMessage("buyer.sold_items",
                "amount", amount,
                "name", price.getName(),
                "price", String.format("%.2f", total),
                "multiplier", String.format("%.2f", multiplier)));
        
        dataManager.saveAll();

        // Effects
        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.8f, 1.2f);
        player.getWorld().spawnParticle(Particle.HEART, player.getLocation().add(0, 1, 0), 15, 0.5, 0.5, 0.5, 0.1);

        open(player);
    }

    private int countItems(Player player, Material mat) {
        int count = 0;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() == mat) {
                count += item.getAmount();
            }
        }
        return count;
    }

    private void removeItems(Player player, Material mat, int amount) {
        int left = amount;
        ItemStack[] contents = player.getInventory().getContents();
        
        for (int i = 0; i < contents.length; i++) {
            ItemStack item = contents[i];
            if (item != null && item.getType() == mat) {
                int take = Math.min(left, item.getAmount());
                item.setAmount(item.getAmount() - take);
                if (item.getAmount() <= 0) {
                    contents[i] = null;
                }
                left -= take;
                if (left <= 0) break;
            }
        }
        
        player.getInventory().setContents(contents);
    }
    
    private ItemStack createItem(Material mat, String name, List<String> lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name.replace("&", "§"));
        
        List<String> coloredLore = new ArrayList<>();
        for (String line : lore) {
            coloredLore.add(line.replace("&", "§"));
        }
        meta.setLore(coloredLore);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS);
        item.setItemMeta(meta);
        return item;
    }

    private int getMaxSlots(Player player) {
        if (player.hasPermission("burzhuy.maxslots.22")) return 22;
        if (player.hasPermission("burzhuy.maxslots.20")) return 20;
        if (player.hasPermission("burzhuy.maxslots.18")) return 18;
        if (player.hasPermission("burzhuy.maxslots.16")) return 16;
        if (player.hasPermission("burzhuy.maxslots.14")) return 14;
        return 12; // Default
    }

    /**
     * Stop all timers when plugin disables
     */
    public void stopAllTimers() {
        for (Map.Entry<UUID, SchedulerAdapter.TaskWrapper> entry : new HashMap<>(infoTimers).entrySet()) {
            try {
                if (entry.getValue() != null) {
                    entry.getValue().cancel();
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Error stopping timer: " + e.getMessage());
            }
        }
        infoTimers.clear();
    }
}
