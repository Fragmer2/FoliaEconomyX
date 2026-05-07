package me.yourname.burzhuy.menu;

import me.yourname.burzhuy.data.BuyerData;
import me.yourname.burzhuy.data.BuyerDataManager;
import me.yourname.burzhuy.economy.EconomyManager;
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
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;

/**
 * Menu for upgrading buyer slots
 */
public class SlotUpgradeMenu implements Listener, CommandExecutor {

    private final BuyerDataManager dataManager;
    private final EconomyManager economyManager;
    private final ItemPriceManager priceManager;
    private final MessageManager messageManager;
    private final SchedulerAdapter scheduler;
    private MultiplierQuestMenu multiplierQuestMenu;
    private final BuyerMenu buyerMenu;
    private final JavaPlugin plugin;
    private final FileConfiguration config;

    public SlotUpgradeMenu(JavaPlugin plugin, BuyerDataManager dataManager, EconomyManager economyManager, 
                          ItemPriceManager priceManager, MultiplierQuestMenu multiplierQuestMenu, 
                          BuyerMenu buyerMenu, MessageManager messageManager, SchedulerAdapter scheduler) {
        this.plugin = plugin;
        this.dataManager = dataManager;
        this.economyManager = economyManager;
        this.priceManager = priceManager;
        this.multiplierQuestMenu = multiplierQuestMenu;
        this.buyerMenu = buyerMenu;
        this.messageManager = messageManager;
        this.scheduler = scheduler;
        this.config = plugin.getConfig();
    }

    public void setMultiplierQuestMenu(MultiplierQuestMenu menu) {
        this.multiplierQuestMenu = menu;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(messageManager.getMessage("messages.player_only"));
            return true;
        }
        open((Player) sender);
        return true;
    }

    public void open(Player player) {
        try {
            BuyerData data = dataManager.get(player);
            if (data == null) {
                plugin.getLogger().warning("BuyerData is null for player " + player.getName());
                player.sendMessage(messageManager.getMessage("messages.error_loading_data"));
                return;
            }

            String title = config.getString("menu.slot_upgrade.title", "&6&l☀ Upgrades ☀").replace("&", "§");
            int size = config.getInt("menu.slot_upgrade.size", 27);

            if (size % 9 != 0 || size < 9 || size > 54) {
                plugin.getLogger().warning("Invalid inventory size in config: " + size + ". Using default 27.");
                size = 27;
            }

            Inventory inv = Bukkit.createInventory(null, size, title);

            // Glass panes
            Material glassMat;
            try {
                glassMat = Material.valueOf(config.getString("menu.slot_upgrade.glass.material", "ORANGE_STAINED_GLASS_PANE"));
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Invalid glass material in config: " + config.getString("menu.slot_upgrade.glass.material"));
                glassMat = Material.ORANGE_STAINED_GLASS_PANE;
            }
            
            String glassName = config.getString("menu.slot_upgrade.glass.name", "&e ");
            ItemStack glass = createItem(glassMat, glassName, Collections.emptyList());
            for (int i = 0; i < size; i++) {
                inv.setItem(i, glass);
            }

            // Slot upgrade button
            int slot = config.getInt("menu.slot_upgrade.slot_button.slot", 12);
            
            if (slot < 0 || slot >= size) {
                plugin.getLogger().warning("Invalid slot upgrade button slot: " + slot + ". Using default 12.");
                slot = 12;
                if (slot >= size) slot = size / 2;
            }

            int base = config.getInt("upgrade_slot_cost.base", 80000);
            int perSlot = config.getInt("upgrade_slot_cost.per_slot", 10000);
            int maxSlots = getMaxSlots(player);
            int slotCost = base + data.getSlots() * perSlot;
            double playerBalance = economyManager.getBalance(player);
            boolean canUpgradeSlots = data.getSlots() < maxSlots && playerBalance >= slotCost;

            Material btnMat;
            try {
                btnMat = Material.valueOf(config.getString(
                        canUpgradeSlots ? "menu.slot_upgrade.slot_button.material" : "menu.slot_upgrade.slot_button.material_locked",
                        canUpgradeSlots ? "CHEST" : "BARRIER"
                ));
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Invalid slot button material in config");
                btnMat = canUpgradeSlots ? Material.CHEST : Material.BARRIER;
            }

            String btnName = config.getString("menu.slot_upgrade.slot_button.name", "&6&lAdd Slot");
            List<String> btnLore = new ArrayList<>();
            for (String line : config.getStringList("menu.slot_upgrade.slot_button.lore")) {
                line = line.replace("{slots}", String.valueOf(data.getSlots()))
                           .replace("{max_slots}", String.valueOf(maxSlots))
                           .replace("{cost}", String.valueOf(slotCost))
                           .replace("{balance}", String.format("%.2f", playerBalance))
                           .replace("{availability}", canUpgradeSlots
                                   ? config.getString("menu.slot_upgrade.slot_button.available", "&a✓ Available for purchase!")
                                   : config.getString("menu.slot_upgrade.slot_button.not_available", "&c✗ Insufficient funds"));
                btnLore.add(line);
            }
            inv.setItem(slot, createItem(btnMat, btnName, btnLore));

            // Quest button
            int questSlot = config.getInt("menu.slot_upgrade.quest_button.slot", 14);
            if (questSlot < 0 || questSlot >= size) {
                plugin.getLogger().warning("Invalid quest button slot: " + questSlot);
                questSlot = 14;
                if (questSlot >= size) questSlot = size - 3;
            }

            Material questMat;
            try {
                questMat = Material.valueOf(config.getString("menu.slot_upgrade.quest_button.material", "EXPERIENCE_BOTTLE"));
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Invalid quest button material in config");
                questMat = Material.EXPERIENCE_BOTTLE;
            }

            String questName = config.getString("menu.slot_upgrade.quest_button.name", "&6&lMultiplier Quests");
            List<String> questLore = config.getStringList("menu.slot_upgrade.quest_button.lore");
            inv.setItem(questSlot, createItem(questMat, questName, questLore));

            // Back button
            int backSlot = config.getInt("menu.slot_upgrade.back_button.slot", 26);
            if (backSlot < 0 || backSlot >= size) {
                plugin.getLogger().warning("Invalid back button slot: " + backSlot);
                backSlot = size - 1;
            }

            Material backMat;
            try {
                backMat = Material.valueOf(config.getString("menu.slot_upgrade.back_button.material", "ARROW"));
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Invalid back button material in config");
                backMat = Material.ARROW;
            }

            String backName = config.getString("menu.slot_upgrade.back_button.name", "&cBack");
            List<String> backLore = config.getStringList("menu.slot_upgrade.back_button.lore");
            inv.setItem(backSlot, createItem(backMat, backName, backLore));

            player.openInventory(inv);
            
        } catch (Exception e) {
            plugin.getLogger().severe("Critical error opening upgrade menu for player " + player.getName() + ": " + e.getMessage());
            e.printStackTrace();
            player.sendMessage(messageManager.getMessage("messages.error_opening_menu"));
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        
        String expectedTitle = config.getString("menu.slot_upgrade.title", "&6&l☀ Upgrades ☀").replace("&", "§");
        if (!event.getView().getTitle().equals(expectedTitle)) return;
        
        if (event.getClickedInventory() != event.getView().getTopInventory()) return;
        
        event.setCancelled(true);
        Player player = (Player) event.getWhoClicked();
        int raw = event.getRawSlot();

        if (raw < 0 || raw >= event.getInventory().getSize()) {
            return;
        }

        int backSlot = config.getInt("menu.slot_upgrade.back_button.slot", 26);
        if (raw == backSlot) {
            try {
                buyerMenu.open(player);
            } catch (Exception e) {
                plugin.getLogger().warning("Error opening buyer menu: " + e.getMessage());
                player.closeInventory();
            }
            return;
        }

        int slotBtn = config.getInt("menu.slot_upgrade.slot_button.slot", 12);
        int questBtn = config.getInt("menu.slot_upgrade.quest_button.slot", 14);

        if (raw == slotBtn && event.isLeftClick()) {
            scheduler.runTaskLater(() -> tryUpgradeSlots(player), 1L);
        }
        if (raw == questBtn && event.isLeftClick()) {
            scheduler.runTaskLater(() -> {
                if (multiplierQuestMenu != null) {
                    try {
                        multiplierQuestMenu.open(player);
                    } catch (Exception e) {
                        plugin.getLogger().warning("Error opening multiplier quest menu: " + e.getMessage());
                        player.sendMessage(messageManager.getMessage("messages.error_opening_menu"));
                    }
                } else {
                    player.sendMessage("&cQuest menu temporarily unavailable!");
                }
            }, 1L);
        }
    }

    private void tryUpgradeSlots(Player player) {
        try {
            BuyerData data = dataManager.get(player);
            if (data == null) {
                player.sendMessage(messageManager.getMessage("messages.error_loading_data"));
                return;
            }

            int base = config.getInt("upgrade_slot_cost.base", 80000);
            int perSlot = config.getInt("upgrade_slot_cost.per_slot", 10000);
            int maxSlots = getMaxSlots(player);
            int cost = base + data.getSlots() * perSlot;
            double playerBalance = economyManager.getBalance(player);

            if (data.getSlots() >= maxSlots) {
                player.sendMessage(messageManager.getMessage("slot_upgrade.max_slots_reached"));
                return;
            }

            if (playerBalance < cost) {
                player.sendMessage(messageManager.getMessage("slot_upgrade.insufficient_funds",
                        "cost", cost,
                        "balance", String.format("%.2f", playerBalance)));
                return;
            }

            if (economyManager.getBalance(player) >= cost) {
                try {
                    economyManager.withdraw(player, cost);
                    data.upgradeSlots();
                } catch (Exception e) {
                    plugin.getLogger().warning("Error withdrawing funds from player " + player.getName() + ": " + e.getMessage());
                    player.sendMessage(messageManager.getMessage("slot_upgrade.error_withdrawing"));
                    return;
                }
                
                if (priceManager != null && priceManager.getPrices() != null) {
                    data.addNewLots(priceManager.getPrices().keySet());
                } else {
                    plugin.getLogger().warning("PriceManager or its data unavailable during slot upgrade for player " + player.getName());
                }
                
                dataManager.saveAll();
                
                double newBalance = economyManager.getBalance(player);
                player.sendMessage(messageManager.getMessage("slot_upgrade.success",
                        "cost", cost,
                        "balance", String.format("%.2f", newBalance)));

                // Effects
                try {
                    player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
                    player.getWorld().spawnParticle(Particle.FIREWORK, player.getLocation().add(0, 1, 0), 3);
                } catch (Exception e) {
                    plugin.getLogger().warning("Error playing effects: " + e.getMessage());
                }

                scheduler.runTaskLater(() -> open(player), 2L);
            } else {
                player.sendMessage(messageManager.getMessage("slot_upgrade.insufficient_funds",
                        "cost", cost,
                        "balance", String.format("%.2f", playerBalance)));
            }
            
        } catch (Exception e) {
            plugin.getLogger().severe("Critical error upgrading slots for player " + player.getName() + ": " + e.getMessage());
            e.printStackTrace();
            player.sendMessage(messageManager.getMessage("slot_upgrade.error_upgrading"));
        }
    }

    private ItemStack createItem(Material mat, String name, List<String> lore) {
        try {
            ItemStack item = new ItemStack(mat);
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(name.replace("&", "§"));
                List<String> coloredLore = new ArrayList<>();
                for (String line : lore) {
                    coloredLore.add(line.replace("&", "§"));
                }
                meta.setLore(coloredLore);
                meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS);
                item.setItemMeta(meta);
            }
            return item;
        } catch (Exception e) {
            plugin.getLogger().warning("Error creating item with material " + mat + ": " + e.getMessage());
            ItemStack fallback = new ItemStack(Material.STONE);
            ItemMeta meta = fallback.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(name.replace("&", "§"));
                List<String> coloredLore = new ArrayList<>();
                for (String line : lore) {
                    coloredLore.add(line.replace("&", "§"));
                }
                meta.setLore(coloredLore);
                fallback.setItemMeta(meta);
            }
            return fallback;
        }
    }

    private int getMaxSlots(Player player) {
        try {
            if (player.hasPermission("burzhuy.maxslots.22")) return 22;
            if (player.hasPermission("burzhuy.maxslots.20")) return 20;
            if (player.hasPermission("burzhuy.maxslots.18")) return 18;
            if (player.hasPermission("burzhuy.maxslots.16")) return 16;
            if (player.hasPermission("burzhuy.maxslots.14")) return 14;
            return 12;
        } catch (Exception e) {
            plugin.getLogger().warning("Error getting max slots for player " + player.getName() + ": " + e.getMessage());
            return 12;
        }
    }
}
