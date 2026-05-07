package me.yourname.burzhuy.data;

import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Manages player data persistence
 */
public class BuyerDataManager {
    
    private final JavaPlugin plugin;
    private final Map<UUID, BuyerData> dataMap = new HashMap<>();
    private File dataFile;
    private FileConfiguration config;

    public BuyerDataManager(JavaPlugin plugin) {
        this.plugin = plugin;
        load();
    }

    /**
     * Get or create buyer data for a player
     */
    public BuyerData get(Player player) {
        return dataMap.computeIfAbsent(player.getUniqueId(), k -> new BuyerData());
    }

    /**
     * Save all player data to file
     */
    public void saveAll() {
        for (Map.Entry<UUID, BuyerData> entry : dataMap.entrySet()) {
            String path = entry.getKey().toString();
            BuyerData data = entry.getValue();
            
            // Save basic data
            config.set(path + ".slots", data.getSlots());
            
            // Save lots
            List<String> lotsString = new ArrayList<>();
            for (Material mat : data.getLots()) {
                lotsString.add(mat.name());
            }
            config.set(path + ".lots", lotsString);

            // Save multiplier quest data
            config.set(path + ".multiplierLevel", data.getMultiplierLevel());
            config.set(path + ".activeQuestIndices", data.getActiveQuestIndices());
            config.set(path + ".questProgress", data.getQuestProgress());
        }
        
        try {
            config.save(dataFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Refresh lots for all players
     */
    public void refreshAllLots(Set<Material> allMaterials) {
        for (BuyerData data : dataMap.values()) {
            data.refreshLots(allMaterials);
        }
        saveAll();
    }

    /**
     * Refresh lots for a specific player
     */
    public void refreshLotsFor(Player player, Set<Material> allMaterials) {
        BuyerData data = get(player);
        data.refreshLots(allMaterials);
        saveAll();
    }

    /**
     * Load player data from file
     */
    private void load() {
        dataFile = new File(plugin.getDataFolder(), "playerdata.yml");
        
        if (!dataFile.exists()) {
            dataFile.getParentFile().mkdirs();
            try {
                dataFile.createNewFile();
            } catch (IOException ignored) {
            }
        }
        
        config = YamlConfiguration.loadConfiguration(dataFile);
        
        for (String uuidStr : config.getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(uuidStr);
                int slots = config.getInt(uuidStr + ".slots", 9);
                
                BuyerData data = new BuyerData();
                data.setSlots(slots);

                // Load lots
                List<String> lotsString = config.getStringList(uuidStr + ".lots");
                List<Material> lots = new ArrayList<>();
                for (String matName : lotsString) {
                    try {
                        lots.add(Material.valueOf(matName));
                    } catch (IllegalArgumentException ignored) {
                    }
                }
                data.setLots(lots);

                // Load multiplier quest data
                data.setMultiplierLevel(config.getInt(uuidStr + ".multiplierLevel", 0));
                
                List<Integer> questIndices = config.getIntegerList(uuidStr + ".activeQuestIndices");
                List<Integer> questProgress = config.getIntegerList(uuidStr + ".questProgress");
                
                // Ensure lists have 3 elements
                while (questIndices.size() < 3) questIndices.add(0);
                while (questProgress.size() < 3) questProgress.add(0);
                if (questIndices.size() > 3) questIndices = questIndices.subList(0, 3);
                if (questProgress.size() > 3) questProgress = questProgress.subList(0, 3);
                
                data.setActiveQuestIndices(questIndices);
                data.setQuestProgress(questProgress);

                dataMap.put(uuid, data);
            } catch (Exception e) {
                plugin.getLogger().warning(String.format(
                    "Failed to load data for UUID %s: %s", uuidStr, e.getMessage()
                ));
            }
        }
    }
}
