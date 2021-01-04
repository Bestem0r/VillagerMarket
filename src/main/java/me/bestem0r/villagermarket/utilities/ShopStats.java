package me.bestem0r.villagermarket.utilities;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;

public class ShopStats {

    private final JavaPlugin plugin;

    private int itemsSold;
    private int itemsBought;

    private double moneyEarned;
    private double moneySpent;

    public ShopStats(JavaPlugin plugin, FileConfiguration config) {
        this.plugin = plugin;
        this.itemsSold = config.getInt("stats.items_sold");
        this.itemsBought = config.getInt("stats.items_bought");
        this.moneyEarned = config.getInt("stats.money_earned");
        this.moneySpent = config.getInt("stats.money_spent");
    }
    public ShopStats(JavaPlugin plugin) {
        this.plugin = plugin;
        this.itemsBought = 0;
        this.itemsSold = 0;
        this.moneyEarned = 0;
        this.moneySpent = 0;
    }

    /** Adders */
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

    /** Getters */
    public int getItemsSold() {
        return itemsSold;
    }
    public int getItemsBought() {
        return itemsBought;
    }

    public double getMoneyEarned() {
        return moneyEarned;
    }
    public double getMoneySpent() {
        return moneySpent;
    }

    public ArrayList<String> getStats() {
        return new ColorBuilder(plugin)
                .path("stats_message")
                .replace("%items_sold%", String.valueOf(itemsSold))
                .replace("%items_bought%", String.valueOf(itemsBought))
                .replaceWithCurrency("%money_earned%", String.valueOf(moneyEarned))
                .replaceWithCurrency("%money_spent%", String.valueOf(moneySpent))
                .buildLore();
    }
}
