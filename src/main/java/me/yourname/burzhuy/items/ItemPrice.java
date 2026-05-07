package me.yourname.burzhuy.items;

/**
 * Represents the price information for an item
 */
public class ItemPrice {
    private final double price;
    private final String name;

    public ItemPrice(double price, String name) {
        this.price = price;
        this.name = name;
    }

    public double getPrice() {
        return price;
    }

    public String getName() {
        return name;
    }
}
