package me.yourname.burzhuy.quest;

/**
 * Represents a single multiplier quest
 */
public class MultiplierQuest {
    private final String name;
    private final QuestType type;
    private final Object target; // Material, EntityType, String, or null
    private final int amount;

    public MultiplierQuest(String name, QuestType type, Object target, int amount) {
        this.name = name;
        this.type = type;
        this.target = target;
        this.amount = amount;
    }

    public String getName() {
        return name;
    }

    public QuestType getType() {
        return type;
    }

    public Object getTarget() {
        return target;
    }

    public int getAmount() {
        return amount;
    }
}
