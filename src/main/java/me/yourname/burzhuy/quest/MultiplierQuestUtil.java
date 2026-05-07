package me.yourname.burzhuy.quest;

import me.yourname.burzhuy.data.BuyerData;
import org.bukkit.Bukkit;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Utility class for managing multiplier quests
 */
public class MultiplierQuestUtil {
    
    /**
     * Assign new random quests to player
     * @param data Player's buyer data
     */
    public static void assignNewQuests(BuyerData data) {
        Bukkit.getLogger().info(String.format(
            "[BurzhuyPlugin][DEBUG] assignNewQuests called. Total quests: %d",
            MultiplierQuestPool.ALL_QUESTS.size()
        ));
        
        List<Integer> indices = new ArrayList<>();
        for (int i = 0; i < MultiplierQuestPool.ALL_QUESTS.size(); i++) {
            indices.add(i);
        }
        Collections.shuffle(indices);
        
        if (indices.size() < 3) {
            Bukkit.getLogger().warning(String.format(
                "[BurzhuyPlugin][DEBUG] Not enough quests to assign! Need at least 3, current: %d",
                indices.size()
            ));
        }
        
        data.setActiveQuestIndices(indices.subList(0, Math.min(3, indices.size())));
        data.setQuestProgress(Arrays.asList(0, 0, 0));
    }

    /**
     * Check if all quests are completed
     * @param data Player's buyer data
     * @return true if all quests are completed
     */
    public static boolean allQuestsCompleted(BuyerData data) {
        List<Integer> indices = data.getActiveQuestIndices();
        List<Integer> progress = data.getQuestProgress();
        
        for (int i = 0; i < indices.size(); i++) {
            MultiplierQuest quest = MultiplierQuestPool.ALL_QUESTS.get(indices.get(i));
            if (progress.get(i) < quest.getAmount()) {
                return false;
            }
        }
        
        return true;
    }
}
