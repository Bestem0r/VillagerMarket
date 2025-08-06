package net.bestemor.villagermarket.shop;

import net.bestemor.core.config.ConfigManager;
import org.bukkit.configuration.file.FileConfiguration;

import java.math.BigDecimal;
import java.util.List;

public class ShopStats {

    private final FileConfiguration config;

    private int itemsSold;
    private int itemsBought;

    private double moneyEarned;
    private double moneySpent;

    public ShopStats(FileConfiguration config) {
        this.config = config;

        this.itemsSold = config.getInt("stats.items_sold");
        this.itemsBought = config.getInt("stats.items_bought");
        this.moneyEarned = config.getInt("stats.money_earned");
        this.moneySpent = config.getInt("stats.money_spent");
    }

    public void addSold(int amount) {
        itemsSold += amount;
    }

    public void addBought(int amount) {
        itemsBought += amount;
    }

    public void addEarned(double amount) {
        moneyEarned += amount;
    }

    public void addSpent(double amount) {
        moneySpent += amount;
    }

    public void save() {
        config.set("stats.items_sold", itemsSold);
        config.set("stats.items_bought", itemsBought);
        config.set("stats.money_earned", moneyEarned);
        config.set("stats.money_spent", moneySpent);
    }

    public List<String> getStats() {
        return ConfigManager.getListBuilder("stats_message")
                .replace("%items_sold%", String.valueOf(itemsSold))
                .replace("%items_bought%", String.valueOf(itemsBought))
                .replaceCurrency("%money_earned%", BigDecimal.valueOf(moneyEarned))
                .replaceCurrency("%money_spent%", BigDecimal.valueOf(moneySpent))
                .build();
    }
}
