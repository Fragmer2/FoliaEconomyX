package me.yourname.burzhuy.data;

import org.bukkit.Material;

import java.util.*;

/**
 * Stores player's buyer data including slots, lots, and quest progress
 */
public class BuyerData {
    
    private int slots;
    private List<Material> lots = new ArrayList<>();

    // Multiplier quest data
    private int multiplierLevel = 0;
    private List<Integer> activeQuestIndices = new ArrayList<>(Arrays.asList(0, 1, 2));
    private List<Integer> questProgress = new ArrayList<>(Arrays.asList(0, 0, 0));

    public BuyerData() {
        this.slots = 9;
        fixQuestLists();
    }

    public BuyerData(int slots) {
        this.slots = slots;
        fixQuestLists();
    }

    // Slot management
    public int getSlots() {
        return slots;
    }

    public void setSlots(int slots) {
        this.slots = slots;
    }

    public void upgradeSlots() {
        slots++;
    }

    // Lot management
    public List<Material> getLots() {
        return lots;
    }

    public void setLots(List<Material> lots) {
        this.lots = lots;
    }

    /**
     * Refresh all lots with random materials
     */
    public void refreshLots(Set<Material> allMaterials) {
        List<Material> list = new ArrayList<>(allMaterials);
        Collections.shuffle(list);
        this.lots = list.subList(0, Math.min(getSlots(), list.size()));
    }

    /**
     * Add new lots when slots are upgraded
     */
    public void addNewLots(Set<Material> allMaterials) {
        List<Material> available = new ArrayList<>(allMaterials);
        available.removeAll(lots);
        Collections.shuffle(available);
        
        int need = getSlots() - lots.size();
        for (int i = 0; i < need && i < available.size(); i++) {
            lots.add(available.get(i));
        }
    }

    // Multiplier and quest management
    public int getMultiplierLevel() {
        return multiplierLevel;
    }

    public void setMultiplierLevel(int level) {
        this.multiplierLevel = level;
    }

    public void upgradeMultiplier() {
        this.multiplierLevel++;
    }

    public List<Integer> getActiveQuestIndices() {
        fixQuestLists();
        return activeQuestIndices;
    }

    public void setActiveQuestIndices(List<Integer> indices) {
        this.activeQuestIndices = indices;
        fixQuestLists();
    }

    public List<Integer> getQuestProgress() {
        fixQuestLists();
        return questProgress;
    }

    public void setQuestProgress(List<Integer> progress) {
        this.questProgress = progress;
        fixQuestLists();
    }

    /**
     * Ensure quest lists are always valid (3 elements)
     */
    private void fixQuestLists() {
        if (activeQuestIndices == null) {
            activeQuestIndices = new ArrayList<>();
        }
        if (questProgress == null) {
            questProgress = new ArrayList<>();
        }
        
        while (activeQuestIndices.size() < 3) {
            activeQuestIndices.add(0);
        }
        while (questProgress.size() < 3) {
            questProgress.add(0);
        }
        
        if (activeQuestIndices.size() > 3) {
            activeQuestIndices = activeQuestIndices.subList(0, 3);
        }
        if (questProgress.size() > 3) {
            questProgress = questProgress.subList(0, 3);
        }
    }
}
