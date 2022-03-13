package net.bestemor.villagermarket.shop;

import net.bestemor.villagermarket.ConfigManager;
import net.bestemor.villagermarket.VMPlugin;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;

import java.io.File;
import java.math.BigDecimal;
import java.util.Date;

public class AdminShop extends VillagerShop {

    public AdminShop(VMPlugin plugin, File file) {
        super(plugin, file);
        shopfrontHolder.load();
    }

    /** Buys item/command from the admin shop */
    @Override
    protected void buyItem(int slot, Player player) {
        ShopItem shopItem = shopfrontHolder.getItemList().get(slot);
        Economy economy = plugin.getEconomy();

        BigDecimal price = shopItem.getPrice();

        if (!shopItem.verifyPurchase(player)) {
            return;
        }
        if (shopItem.isItemTrade()) {
            removeItems(player.getInventory(), shopItem.getItemTrade());
        } else {
            economy.withdrawPlayer(player, price.doubleValue());
            BigDecimal left = BigDecimal.valueOf(economy.getBalance(player));
            player.sendMessage(ConfigManager.getCurrencyBuilder("messages.money_left").replaceCurrency("%amount%", left).addPrefix().build());
            shopStats.addEarned(price.doubleValue());
        }

        shopStats.addSold(shopItem.getAmount());
        giveShopItem(player, shopItem);
        shopItem.incrementPlayerTrades(player);
        shopItem.incrementServerTrades();

        player.playSound(player.getLocation(), ConfigManager.getSound("sounds.buy_item"), 1, 1);

        VMPlugin.log.add(new Date() + ": " + player.getName() + " bought " + shopItem.getAmount() + "x " + shopItem.getType() + " from Admin Shop " + "(" + price.toPlainString() + ")");

    }

    /** Sells item to the admin shop */
    @Override
    protected void sellItem(int slot, Player player) {
        ShopItem shopItem = shopfrontHolder.getItemList().get(slot);
        Economy economy = plugin.getEconomy();

        int amount = shopItem.getAmount();
        BigDecimal price = shopItem.getPrice();

        if (!shopItem.verifyPurchase(player)) {
            return;
        }

        economy.depositPlayer(player, price.doubleValue());
        removeItems(player.getInventory(), shopItem.getRawItem());
        shopItem.incrementPlayerTrades(player);
        shopItem.incrementServerTrades();
        shopStats.addBought(amount);
        shopStats.addSpent(price.doubleValue());


        player.playSound(player.getLocation(), ConfigManager.getSound("sounds.sell_item"), 0.5f, 1);

        String currency = config.getString("currency");
        String valueCurrency = (config.getBoolean("currency_before") ? currency + price : price + currency);
        VMPlugin.log.add(new Date() + ": " + player.getName() + " sold " + amount + "x " + shopItem.getType() + " to: " + entityUUID + " (" + valueCurrency + ")");
    }

    @Override
    public String getModeCycle(String mode, boolean isItemTrade) {
        return ConfigManager.getString("menus.edit_item.mode_cycle.admin_shop." + (!isItemTrade ? mode : "item_trade"));
    }


    @Override
    public int getAvailable(ShopItem shopItem) {
        return -1;
    }

    /** Runs when a Player wants to buy a command */
    public void buyCommand(Player player, ShopItem shopItem) {
        Economy economy = plugin.getEconomy();

        BigDecimal price = shopItem.getPrice();
        if (economy.getBalance(player) < price.doubleValue()) {
            player.sendMessage(ConfigManager.getMessage("messages.not_enough_money"));
            return;
        }
        if (shopItem.getPlayerLimit(player) >= shopItem.getLimit() && shopItem.getLimit() != 0) {
            player.sendMessage(ConfigManager.getMessage("messages.reached_command_limit"));
            return;
        }
        economy.withdrawPlayer(player, price.doubleValue());

        if (shopItem.getCommand() != null && !shopItem.getCommand().equals("")) {
            ConsoleCommandSender sender = Bukkit.getConsoleSender();
            Bukkit.dispatchCommand(sender, shopItem.getCommand().replaceAll("%player%", player.getName()));
        }

        shopItem.incrementPlayerTrades(player);
    }
}
