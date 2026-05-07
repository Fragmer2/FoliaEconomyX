package me.yourname.burzhuy.quest;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.EntityType;

import java.util.ArrayList;
import java.util.List;

/**
 * Pool of all available multiplier quests loaded from config
 */
public class MultiplierQuestPool {
    
    public static final List<MultiplierQuest> ALL_QUESTS = new ArrayList<>();

    /**
     * Load all quests from configuration
     * @param config Plugin configuration
     */
    public static void loadQuests(FileConfiguration config) {
        ALL_QUESTS.clear();
        
        ConfigurationSection section = config.getConfigurationSection("interface.quests");
        if (section == null) {
            Bukkit.getLogger().warning("[BurzhuyPlugin] No quests section found in config!");
            return;
        }
        
        int loaded = 0;
        for (String key : section.getKeys(false)) {
            try {
                String name = section.getString(key + ".name");
                String typeStr = section.getString(key + ".type");
                QuestType type = QuestType.valueOf(typeStr);
                int amount = section.getInt(key + ".amount");
                
                Object target = null;
                
                // Parse target based on quest type
                switch (type) {
                    case MOB_KILL:
                        target = EntityType.valueOf(section.getString(key + ".target"));
                        break;
                        
                    case ITEM_DELIVER:
                    case BLOCK_BREAK:
                    case ITEM_CRAFT:
                    case GET_ITEM:
                        target = Material.valueOf(section.getString(key + ".target"));
                        break;
                        
                    case TRAVEL:
                    case DRINK_POTION:
                        target = section.getString(key + ".target");
                        break;
                        
                    case WALK_DISTANCE:
                    case WALK_ON_WATER:
                    case VISIT_OCEAN:
                    case JUMP:
                    case USE_TOTEM:
                        // No target needed
                        break;
                        
                    default:
                        target = null;
                }
                
                MultiplierQuest quest = new MultiplierQuest(name, type, target, amount);
                ALL_QUESTS.add(quest);
                
                Bukkit.getLogger().info(String.format(
                    "[BurzhuyPlugin] Quest loaded: %s (%s), amount=%d, target=%s",
                    name, type, amount, target
                ));
                loaded++;
                
            } catch (Exception e) {
                Bukkit.getLogger().warning(String.format(
                    "[BurzhuyPlugin] Error loading quest '%s': %s",
                    key, e.getMessage()
                ));
                e.printStackTrace();
            }
        }
        
        Bukkit.getLogger().info(String.format(
            "[BurzhuyPlugin] Total multiplier quests loaded: %d",
            loaded
        ));
    }
}
