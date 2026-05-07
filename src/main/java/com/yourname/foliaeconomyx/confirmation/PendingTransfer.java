package com.yourname.foliaeconomyx.confirmation;

public class PendingTransfer {
    
    private final String sender;
    private final String target;
    private final String currency;
    private final double amount;
    private final int maxLevel;
    private final long timestamp;
    private int currentLevel;

    public PendingTransfer(String sender, String target, String currency, double amount, int maxLevel) {
        this.sender = sender;
        this.target = target;
        this.currency = currency;
        this.amount = amount;
        this.maxLevel = maxLevel;
        this.timestamp = System.currentTimeMillis();
        this.currentLevel = 0;
    }

    public boolean isSameTransfer(String target, String currency, double amount) {
        return this.target.equalsIgnoreCase(target) &&
               this.currency.equalsIgnoreCase(currency) &&
               Math.abs(this.amount - amount) < 0.01;
    }

    public boolean isExpired() {
        return System.currentTimeMillis() - timestamp > 30000; // 30 seconds
    }

    public void incrementLevel() {
        currentLevel++;
    }

    public boolean isConfirmed() {
        return currentLevel >= maxLevel;
    }

    public int getLevel() {
        return currentLevel;
    }

    public int getMaxLevel() {
        return maxLevel;
    }

    public String getSender() {
        return sender;
    }

    public String getTarget() {
        return target;
    }

    public String getCurrency() {
        return currency;
    }

    public double getAmount() {
        return amount;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public int getRemainingConfirmations() {
        return maxLevel - currentLevel;
    }
}
