package me.yourname.burzhuy.quest;

import me.yourname.burzhuy.data.BuyerData;
import me.yourname.burzhuy.data.BuyerDataManager;
import me.yourname.burzhuy.scheduler.SchedulerAdapter;
import me.yourname.burzhuy.utils.MessageManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityResurrectEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.*;

/**
 * Listener for multiplier quest progression events
 */
public class MultiplierQuestListener implements Listener {
    
    private final BuyerDataManager dataManager;
    private final MessageManager messageManager;
    private final SchedulerAdapter scheduler;
    private final int maxMultiplierLevel;

    // Tracking maps for movement-based quests
    private final Map<UUID, org.bukkit.Location> lastLocations = new HashMap<>();
    private final Map<UUID, Boolean> wasOnGround = new HashMap<>();
    private final Map<UUID, Double> walkRemainder = new HashMap<>();
    private final Map<UUID, Double> waterWalkRemainder = new HashMap<>();

    public MultiplierQuestListener(BuyerDataManager dataManager, FileConfiguration config, 
                                   MessageManager messageManager, SchedulerAdapter scheduler) {
        this.dataManager = dataManager;
        this.messageManager = messageManager;
        this.scheduler = scheduler;
        this.maxMultiplierLevel = config.getInt("multiplier.max_level", 5);
    }

    // === EFFECTS ===
    
    private void playQuestCompleteEffects(Player player) {
        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
        player.getWorld().spawnParticle(Particle.HEART, player.getLocation().add(0, 1, 0), 20, 0.4, 0.6, 0.4, 0.1);
    }
    
    private void playMultiplierUpgradeEffects(Player player) {
        player.getWorld().spawnParticle(Particle.FIREWORK, player.getLocation().add(0, 1, 0), 1);
    }

    // === EVENT HANDLERS ===

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        if (event.getEntity().getKiller() == null) return;
        
        Player player = event.getEntity().getKiller();
        BuyerData data = dataManager.get(player);
        List<Integer> indices = data.getActiveQuestIndices();
        List<Integer> progress = data.getQuestProgress();
        boolean changed = false;
        
        for (int i = 0; i < indices.size(); i++) {
            MultiplierQuest quest = MultiplierQuestPool.ALL_QUESTS.get(indices.get(i));
            
            if (quest.getType() == QuestType.MOB_KILL && event.getEntity().getType() == quest.getTarget()) {
                if (progress.get(i) < quest.getAmount()) {
                    int newValue = progress.get(i) + 1;
                    progress.set(i, newValue);
                    changed = true;
                    
                    if (newValue == quest.getAmount()) {
                        player.sendMessage(messageManager.getMessage("quests.completed", "name", quest.getName()));
                        playQuestCompleteEffects(player);
                    } else {
                        player.sendMessage(messageManager.getMessage("quests.progress",
                                "name", quest.getName(),
                                "current", newValue,
                                "total", quest.getAmount()));
                    }
                }
            }
        }
        
        if (changed) handleQuestUpdate(player, data, progress);
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        BuyerData data = dataManager.get(player);
        List<Integer> indices = data.getActiveQuestIndices();
        List<Integer> progress = data.getQuestProgress();
        boolean changed = false;
        
        for (int i = 0; i < indices.size(); i++) {
            MultiplierQuest quest = MultiplierQuestPool.ALL_QUESTS.get(indices.get(i));
            
            if (quest.getType() == QuestType.BLOCK_BREAK && event.getBlock().getType() == quest.getTarget()) {
                if (progress.get(i) < quest.getAmount()) {
                    int newValue = progress.get(i) + 1;
                    progress.set(i, newValue);
                    changed = true;
                    
                    if (newValue == quest.getAmount()) {
                        player.sendMessage(messageManager.getMessage("quests.completed", "name", quest.getName()));
                        playQuestCompleteEffects(player);
                    } else {
                        player.sendMessage(messageManager.getMessage("quests.progress",
                                "name", quest.getName(),
                                "current", newValue,
                                "total", quest.getAmount()));
                    }
                }
            }
        }
        
        if (changed) handleQuestUpdate(player, data, progress);
    }

    @EventHandler
    public void onCraft(CraftItemEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        
        Player player = (Player) event.getWhoClicked();
        BuyerData data = dataManager.get(player);
        List<Integer> indices = data.getActiveQuestIndices();
        List<Integer> progress = data.getQuestProgress();
        boolean changed = false;
        
        for (int i = 0; i < indices.size(); i++) {
            MultiplierQuest quest = MultiplierQuestPool.ALL_QUESTS.get(indices.get(i));
            
            if (quest.getType() == QuestType.ITEM_CRAFT && event.getRecipe().getResult().getType() == quest.getTarget()) {
                if (progress.get(i) < quest.getAmount()) {
                    int newValue = progress.get(i) + 1;
                    progress.set(i, newValue);
                    changed = true;
                    
                    if (newValue == quest.getAmount()) {
                        player.sendMessage(messageManager.getMessage("quests.completed", "name", quest.getName()));
                        playQuestCompleteEffects(player);
                    } else {
                        player.sendMessage(messageManager.getMessage("quests.progress",
                                "name", quest.getName(),
                                "current", newValue,
                                "total", quest.getAmount()));
                    }
                }
            }
        }
        
        if (changed) handleQuestUpdate(player, data, progress);
    }

    @EventHandler
    @SuppressWarnings("deprecation") // For PlayerPickupItemEvent
    public void onPickup(PlayerPickupItemEvent event) {
        Player player = event.getPlayer();
        checkGetItemQuests(player);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        checkGetItemQuests(player);
    }

    private void checkGetItemQuests(Player player) {
        BuyerData data = dataManager.get(player);
        List<Integer> indices = data.getActiveQuestIndices();
        List<Integer> progress = data.getQuestProgress();
        boolean changed = false;
        
        for (int i = 0; i < indices.size(); i++) {
            MultiplierQuest quest = MultiplierQuestPool.ALL_QUESTS.get(indices.get(i));
            
            if (quest.getType() == QuestType.GET_ITEM && quest.getTarget() instanceof Material) {
                Material mat = (Material) quest.getTarget();
                int old = progress.get(i);
                int have = countItems(player, mat);
                int toGive = Math.min(have, quest.getAmount() - old);
                
                if (toGive > 0 && old < quest.getAmount()) {
                    int newValue = Math.min(old + toGive, quest.getAmount());
                    progress.set(i, newValue);
                    changed = true;
                    
                    if (newValue == quest.getAmount()) {
                        player.sendMessage(messageManager.getMessage("quests.completed", "name", quest.getName()));
                        playQuestCompleteEffects(player);
                    } else {
                        player.sendMessage(messageManager.getMessage("quests.progress",
                                "name", quest.getName(),
                                "current", newValue,
                                "total", quest.getAmount()));
                    }
                }
            }
        }
        
        if (changed) handleQuestUpdate(player, data, progress);
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

    @EventHandler
    public void onPortal(PlayerPortalEvent event) {
        Player player = event.getPlayer();
        BuyerData data = dataManager.get(player);
        List<Integer> indices = data.getActiveQuestIndices();
        List<Integer> progress = data.getQuestProgress();
        boolean changed = false;
        
        for (int i = 0; i < indices.size(); i++) {
            MultiplierQuest quest = MultiplierQuestPool.ALL_QUESTS.get(indices.get(i));
            
            if (quest.getType() == QuestType.TRAVEL) {
                String target = (String) quest.getTarget();
                
                if ("NETHER".equals(target) && event.getTo().getWorld().getEnvironment() == World.Environment.NETHER) {
                    if (progress.get(i) < quest.getAmount()) {
                        progress.set(i, quest.getAmount());
                        changed = true;
                        player.sendMessage(messageManager.getMessage("quests.completed", "name", quest.getName()));
                        playQuestCompleteEffects(player);
                    }
                }
                
                if ("THE_END".equals(target) && event.getTo().getWorld().getEnvironment() == World.Environment.THE_END) {
                    if (progress.get(i) < quest.getAmount()) {
                        progress.set(i, quest.getAmount());
                        changed = true;
                        player.sendMessage(messageManager.getMessage("quests.completed", "name", quest.getName()));
                        playQuestCompleteEffects(player);
                    }
                }
            }
        }
        
        if (changed) handleQuestUpdate(player, data, progress);
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        BuyerData data = dataManager.get(player);
        List<Integer> indices = data.getActiveQuestIndices();
        List<Integer> progress = data.getQuestProgress();
        boolean changed = false;

        org.bukkit.Location last = lastLocations.get(player.getUniqueId());
        org.bukkit.Location current = player.getLocation();
        if (last == null) last = current.clone();

        for (int i = 0; i < indices.size(); i++) {
            MultiplierQuest quest = MultiplierQuestPool.ALL_QUESTS.get(indices.get(i));
            
            // WALK_DISTANCE with fractional remainder accumulation
            if (quest.getType() == QuestType.WALK_DISTANCE) {
                if (last.getWorld().equals(current.getWorld())) {
                    double dx = current.getX() - last.getX();
                    double dz = current.getZ() - last.getZ();
                    double dist = Math.sqrt(dx * dx + dz * dz);

                    double totalDist = walkRemainder.getOrDefault(player.getUniqueId(), 0.0) + dist;
                    int steps = (int) totalDist;
                    double remainder = totalDist - steps;

                    if (steps > 0 && progress.get(i) < quest.getAmount()) {
                        int old = progress.get(i);
                        int newValue = Math.min(quest.getAmount(), old + steps);
                        progress.set(i, newValue);
                        changed = true;
                        lastLocations.put(player.getUniqueId(), current.clone());

                        for (int j = old + 1; j <= newValue; j++) {
                            if (j % 100 == 0 || j == quest.getAmount()) {
                                player.sendMessage(messageManager.getMessage("quests.progress",
                                        "name", quest.getName(),
                                        "current", j,
                                        "total", quest.getAmount()));
                            }
                        }
                        
                        if (newValue == quest.getAmount()) {
                            player.sendMessage(messageManager.getMessage("quests.completed", "name", quest.getName()));
                            playQuestCompleteEffects(player);
                        }
                    }
                    
                    walkRemainder.put(player.getUniqueId(), remainder);
                }
            }
            
            // JUMP with notifications every 10 jumps
            if (quest.getType() == QuestType.JUMP) {
                boolean onGround = player.isOnGround();
                boolean prevOnGround = wasOnGround.getOrDefault(player.getUniqueId(), true);
                int old = progress.get(i);
                
                if (prevOnGround && !onGround && old < quest.getAmount()) {
                    int newValue = old + 1;
                    progress.set(i, newValue);
                    changed = true;
                    
                    if (newValue % 10 == 0 || newValue == quest.getAmount()) {
                        player.sendMessage(messageManager.getMessage("quests.progress",
                                "name", quest.getName(),
                                "current", newValue,
                                "total", quest.getAmount()));
                    }
                    
                    if (newValue == quest.getAmount()) {
                        player.sendMessage(messageManager.getMessage("quests.completed", "name", quest.getName()));
                        playQuestCompleteEffects(player);
                    }
                }
                
                wasOnGround.put(player.getUniqueId(), onGround);
            }
            
            // WALK_ON_WATER with fractional remainder
            if (quest.getType() == QuestType.WALK_ON_WATER) {
                if (current.getBlock().getType() == Material.WATER && progress.get(i) < quest.getAmount()) {
                    double dx = current.getX() - last.getX();
                    double dz = current.getZ() - last.getZ();
                    double dist = Math.sqrt(dx * dx + dz * dz);

                    double totalDist = waterWalkRemainder.getOrDefault(player.getUniqueId(), 0.0) + dist;
                    int steps = (int) totalDist;
                    double remainder = totalDist - steps;

                    if (steps > 0) {
                        int old = progress.get(i);
                        int newValue = Math.min(quest.getAmount(), old + steps);
                        progress.set(i, newValue);
                        changed = true;
                        lastLocations.put(player.getUniqueId(), current.clone());
                        
                        if (newValue % 10 == 0 || newValue == quest.getAmount()) {
                            player.sendMessage(messageManager.getMessage("quests.progress",
                                    "name", quest.getName(),
                                    "current", newValue,
                                    "total", quest.getAmount()));
                        }
                        
                        if (newValue == quest.getAmount()) {
                            player.sendMessage(messageManager.getMessage("quests.completed", "name", quest.getName()));
                            playQuestCompleteEffects(player);
                        }
                    }
                    
                    waterWalkRemainder.put(player.getUniqueId(), remainder);
                }
            }
            
            // VISIT_OCEAN
            if (quest.getType() == QuestType.VISIT_OCEAN) {
                Biome biome = player.getLocation().getBlock().getBiome();
                if (biome.toString().contains("OCEAN") && progress.get(i) < quest.getAmount()) {
                    progress.set(i, quest.getAmount());
                    changed = true;
                    player.sendMessage(messageManager.getMessage("quests.completed", "name", quest.getName()));
                    playQuestCompleteEffects(player);
                }
            }
        }
        
        lastLocations.put(player.getUniqueId(), current.clone());

        if (changed) handleQuestUpdate(player, data, progress);
    }

    @EventHandler
    public void onTotemUse(EntityResurrectEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        
        Player player = (Player) event.getEntity();
        BuyerData data = dataManager.get(player);
        List<Integer> indices = data.getActiveQuestIndices();
        List<Integer> progress = data.getQuestProgress();
        boolean changed = false;
        
        for (int i = 0; i < indices.size(); i++) {
            MultiplierQuest quest = MultiplierQuestPool.ALL_QUESTS.get(indices.get(i));
            
            if (quest.getType() == QuestType.USE_TOTEM && progress.get(i) < quest.getAmount()) {
                progress.set(i, quest.getAmount());
                changed = true;
                player.sendMessage(messageManager.getMessage("quests.completed", "name", quest.getName()));
                playQuestCompleteEffects(player);
            }
        }
        
        if (changed) handleQuestUpdate(player, data, progress);
    }

    @EventHandler
    public void onDrinkPotion(PlayerItemConsumeEvent event) {
        Player player = event.getPlayer();
        BuyerData data = dataManager.get(player);
        List<Integer> indices = data.getActiveQuestIndices();
        List<Integer> progress = data.getQuestProgress();
        boolean changed = false;
        
        for (int i = 0; i < indices.size(); i++) {
            MultiplierQuest quest = MultiplierQuestPool.ALL_QUESTS.get(indices.get(i));
            
            if (quest.getType() == QuestType.DRINK_POTION) {
                if ("STRENGTH".equals(quest.getTarget())) {
                    PotionEffectType type = PotionEffectType.getByName("INCREASE_DAMAGE");
                    if (type != null) {
                        for (PotionEffect effect : player.getActivePotionEffects()) {
                            if (effect.getType().equals(type) && progress.get(i) < quest.getAmount()) {
                                int newValue = progress.get(i) + 1;
                                progress.set(i, newValue);
                                changed = true;
                                
                                if (newValue == quest.getAmount()) {
                                    player.sendMessage(messageManager.getMessage("quests.completed", "name", quest.getName()));
                                    playQuestCompleteEffects(player);
                                } else {
                                    player.sendMessage(messageManager.getMessage("quests.progress",
                                            "name", quest.getName(),
                                            "current", newValue,
                                            "total", quest.getAmount()));
                                }
                                break;
                            }
                        }
                    }
                }
            }
        }
        
        if (changed) handleQuestUpdate(player, data, progress);
    }

    private void handleQuestUpdate(Player player, BuyerData data, List<Integer> progress) {
        data.setQuestProgress(progress);

        if (MultiplierQuestUtil.allQuestsCompleted(data)) {
            if (data.getMultiplierLevel() < maxMultiplierLevel) {
                data.upgradeMultiplier();
                
                try {
                    MultiplierQuestUtil.assignNewQuests(data);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                
                player.sendMessage(messageManager.getMessage("quests.all_completed_level_up",
                        "level", data.getMultiplierLevel()));
                playMultiplierUpgradeEffects(player);
            } else {
                player.sendMessage(messageManager.getMessage("quests.all_completed_max_level"));
            }
        }
        
        dataManager.saveAll();
    }
}
