package me.yourname.burzhuy.menu;

import me.yourname.burzhuy.data.BuyerData;
import me.yourname.burzhuy.data.BuyerDataManager;
import me.yourname.burzhuy.quest.MultiplierQuest;
import me.yourname.burzhuy.quest.MultiplierQuestPool;
import me.yourname.burzhuy.quest.MultiplierQuestUtil;
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
 * Menu for multiplier quests
 */
public class MultiplierQuestMenu implements Listener, CommandExecutor {
    
    private final BuyerDataManager dataManager;
    private final MessageManager messageManager;
    private final SchedulerAdapter scheduler;
    private final JavaPlugin plugin;
    private final FileConfiguration config;
    private SlotUpgradeMenu slotUpgradeMenu;

    public MultiplierQuestMenu(JavaPlugin plugin, BuyerDataManager dataManager, 
                               SlotUpgradeMenu slotUpgradeMenu, MessageManager messageManager,
                               SchedulerAdapter scheduler) {
        this.plugin = plugin;
        this.dataManager = dataManager;
        this.slotUpgradeMenu = slotUpgradeMenu;
        this.messageManager = messageManager;
        this.scheduler = scheduler;
        this.config = plugin.getConfig();
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    public void setSlotUpgradeMenu(SlotUpgradeMenu menu) {
        this.slotUpgradeMenu = menu;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player) {
            open((Player) sender);
        } else {
            sender.sendMessage(messageManager.getMessage("messages.player_only"));
        }
        return true;
    }

    public void open(Player player) {
        BuyerData data = dataManager.get(player);
        List<Integer> indices = data.getActiveQuestIndices();
        List<Integer> progress = data.getQuestProgress();

        String title = config.getString("menu.multiplier_quest.title", "&6&l☀ Multiplier Quests ☀").replace("&", "§");
        int size = config.getInt("menu.multiplier_quest.size", 27);
        Inventory inv = Bukkit.createInventory(null, size, title);

        // Glass panes
        Material glassMat = Material.valueOf(config.getString("menu.multiplier_quest.glass.material", "ORANGE_STAINED_GLASS_PANE"));
        String glassName = config.getString("menu.multiplier_quest.glass.name", "&e ");
        ItemStack glass = createItem(glassMat, glassName, Collections.emptyList());
        for (int i = 0; i < size; i++) {
            inv.setItem(i, glass);
        }

        // Quest items
        for (int i = 0; i < indices.size(); i++) {
            MultiplierQuest quest = MultiplierQuestPool.ALL_QUESTS.get(indices.get(i));
            int prog = progress.get(i);
            
            String status = prog >= quest.getAmount()
                    ? config.getString("menu.multiplier_quest.completed_status", "&6✔ &eCompleted!")
                    : config.getString("menu.multiplier_quest.inprogress_status", "&6⏳ &eIn progress");
                    
            Material icon = Material.valueOf(config.getString("menu.multiplier_quest.quest_icon.material", "PAPER"));
            String name = config.getString("menu.multiplier_quest.quest_icon.name", "&6{name}")
                    .replace("{name}", quest.getName());
                    
            List<String> lore = new ArrayList<>();
            for (String line : config.getStringList("menu.multiplier_quest.quest_icon.lore")) {
                line = line.replace("{name}", quest.getName())
                           .replace("{progress}", String.valueOf(prog))
                           .replace("{amount}", String.valueOf(quest.getAmount()))
                           .replace("{status}", status);
                lore.add(line);
            }
            
            if (quest.getType().name().equals("ITEM_DELIVER") && prog < quest.getAmount()) {
                lore.add("&7&oClick to deliver items for this quest!");
            }
            
            inv.setItem(10 + i, createItem(icon, name, lore));
        }

        // Completed button
        int maxLevel = config.getInt("multiplier.max_level", 5);
        if (MultiplierQuestUtil.allQuestsCompleted(data) && data.getMultiplierLevel() >= maxLevel) {
            int completedSlot = config.getInt("menu.multiplier_quest.completed_button.slot", 16);
            Material completedMat = Material.valueOf(config.getString("menu.multiplier_quest.completed_button.material", "EXPERIENCE_BOTTLE"));
            String completedName = config.getString("menu.multiplier_quest.completed_button.name", "&6All quests completed!");
            List<String> completedLore = config.getStringList("menu.multiplier_quest.completed_button.lore");
            inv.setItem(completedSlot, createItem(completedMat, completedName, completedLore));
        }

        // Back button
        int backSlot = config.getInt("menu.multiplier_quest.back_button.slot", 22);
        Material backMat = Material.valueOf(config.getString("menu.multiplier_quest.back_button.material", "ARROW"));
        String backName = config.getString("menu.multiplier_quest.back_button.name", "&cBack");
        List<String> backLore = config.getStringList("menu.multiplier_quest.back_button.lore");
        inv.setItem(backSlot, createItem(backMat, backName, backLore));

        player.openInventory(inv);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        
        Player player = (Player) event.getWhoClicked();
        String title = event.getView().getTitle();
        
        if (!title.equals(config.getString("menu.multiplier_quest.title", "&6&l☀ Multiplier Quests ☀").replace("&", "§"))) return;
        
        event.setCancelled(true);

        int backSlot = config.getInt("menu.multiplier_quest.back_button.slot", 22);
        if (event.getRawSlot() == backSlot) {
            if (slotUpgradeMenu != null) {
                slotUpgradeMenu.open(player);
            }
            return;
        }

        int slot = event.getRawSlot();
        int questIndex = slot - 10;
        if (questIndex < 0 || questIndex > 2) return;

        BuyerData data = dataManager.get(player);
        List<Integer> indices = data.getActiveQuestIndices();
        List<Integer> progress = data.getQuestProgress();
        
        if (questIndex >= indices.size()) return;

        MultiplierQuest quest = MultiplierQuestPool.ALL_QUESTS.get(indices.get(questIndex));
        int prog = progress.get(questIndex);

        // Handle ITEM_DELIVER quest
        if (quest.getType().name().equals("ITEM_DELIVER") && prog < quest.getAmount()) {
            Material mat = (Material) quest.getTarget();
            int need = quest.getAmount() - prog;
            int have = countItems(player, mat);
            
            if (have == 0) {
                player.sendMessage(messageManager.getMessage("quests.no_items_to_deliver"));
                return;
            }
            
            int toRemove = Math.min(have, need);
            removeItems(player, mat, toRemove);
            progress.set(questIndex, prog + toRemove);
            data.setQuestProgress(progress);
            dataManager.saveAll();
            
            player.sendMessage(messageManager.getMessage("quests.delivered_items",
                    "amount", toRemove,
                    "item", mat.name()));

            // Effects
            player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
            player.getWorld().spawnParticle(Particle.HEART, player.getLocation().add(0, 1, 0), 20, 0.4, 0.6, 0.4, 0.1);

            open(player);
        }
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
}
