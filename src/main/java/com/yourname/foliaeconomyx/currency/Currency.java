package com.yourname.foliaeconomyx.currency;

public class Currency {
    
    private final String id;
    private final CurrencyType type;
    private final String symbol;
    private final String nameSingular;
    private final String namePlural;
    private final String format;
    private final int decimalPlaces;
    private final double startingBalance;
    private final double maxBalance;
    private final boolean allowNegative;
    private final boolean allowTransfer;
    private final int requireConfirmation;
    private final int confirmationTimeout;
    private final double confirmationThreshold;
    private final int transferCooldown;
    private final boolean enabled;

    public Currency(String id, CurrencyType type, String symbol, String nameSingular, 
                    String namePlural, String format, int decimalPlaces, 
                    double startingBalance, double maxBalance, boolean allowNegative,
                    boolean allowTransfer, int requireConfirmation, int confirmationTimeout,
                    double confirmationThreshold, int transferCooldown, boolean enabled) {
        this.id = id;
        this.type = type;
        this.symbol = symbol;
        this.nameSingular = nameSingular;
        this.namePlural = namePlural;
        this.format = format;
        this.decimalPlaces = decimalPlaces;
        this.startingBalance = startingBalance;
        this.maxBalance = maxBalance;
        this.allowNegative = allowNegative;
        this.allowTransfer = allowTransfer;
        this.requireConfirmation = requireConfirmation;
        this.confirmationTimeout = confirmationTimeout;
        this.confirmationThreshold = confirmationThreshold;
        this.transferCooldown = transferCooldown;
        this.enabled = enabled;
    }

    public String getId() {
        return id;
    }

    public CurrencyType getType() {
        return type;
    }

    public String getSymbol() {
        return symbol;
    }

    public String getNameSingular() {
        return nameSingular;
    }

    public String getNamePlural() {
        return namePlural;
    }

    public String getFormat() {
        return format;
    }

    public int getDecimalPlaces() {
        return decimalPlaces;
    }

    public double getStartingBalance() {
        return startingBalance;
    }

    public double getMaxBalance() {
        return maxBalance;
    }

    public boolean isAllowNegative() {
        return allowNegative;
    }

    public boolean isAllowTransfer() {
        return allowTransfer;
    }

    public int getRequireConfirmation() {
        return requireConfirmation;
    }

    public int getConfirmationTimeout() {
        return confirmationTimeout;
    }

    public double getConfirmationThreshold() {
        return confirmationThreshold;
    }

    public int getTransferCooldown() {
        return transferCooldown;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public boolean isDonate() {
        return type == CurrencyType.DONATE;
    }

    public boolean needsConfirmation(double amount) {
        return isDonate() && requireConfirmation > 0 && amount >= confirmationThreshold;
    }

    public String formatAmount(double amount) {
        String formatted = String.format("%." + decimalPlaces + "f", amount);
        return format.replace("%amount%", formatted);
    }

    @Override
    public String toString() {
        return "Currency{id='" + id + "', type=" + type + ", symbol='" + symbol + "'}";
    }
}
