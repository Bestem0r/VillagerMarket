package net.bestemor.villagermarket.shop;

import net.bestemor.core.config.ConfigManager;
import net.bestemor.core.config.CurrencyBuilder;
import net.bestemor.villagermarket.VMPlugin;
import net.bestemor.villagermarket.event.interact.BuyShopItemsEvent;
import net.bestemor.villagermarket.event.interact.SellShopItemsEvent;
import net.bestemor.villagermarket.event.interact.TradeShopItemsEvent;
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

    @Override
    public void buyItem(ShopItem item, int amount, Player player) {
        Economy economy = VMPlugin.getEconomy();

        BigDecimal price = item.getSellPrice(amount, true);

        if (!item.verifyPurchase(player, ItemMode.SELL, amount)) {
            return;
        }
        CurrencyBuilder message = ConfigManager.getCurrencyBuilder("messages.bought_item_as_customer")
                .replace("%amount%", String.valueOf(amount))
                .replace("%item%", item.getItemName())
                .replace("%shop%", getShopName())
                .addPrefix();

        if (item.isItemTrade()) {
            message.replace("%price%", item.getItemTradeAmount() + "x " + item.getItemTradeName());
        } else {
            message.replaceCurrency("%price%", price);
        }
        player.sendMessage(message.build());

        if (item.isItemTrade()) {
            TradeShopItemsEvent tradeShopItemsEvent = new TradeShopItemsEvent(player, this, item);
            Bukkit.getPluginManager().callEvent(tradeShopItemsEvent);
            if (tradeShopItemsEvent.isCancelled()) {
                return;
            }
            removeItems(player.getInventory(), item.getItemTrade(), item.getItemTradeAmount());
        } else {
            economy.withdrawPlayer(player, price.doubleValue());
            BigDecimal left = BigDecimal.valueOf(economy.getBalance(player));
            player.sendMessage(ConfigManager.getCurrencyBuilder("messages.money_left").replaceCurrency("%amount%", left).addPrefix().build());
            shopStats.addEarned(price.doubleValue());
        }

        shopStats.addSold(amount);
        giveShopItem(player, item, amount);
        item.incrementPlayerTrades(player);
        item.incrementServerTrades();

        BuyShopItemsEvent buyShopItemsEvent = new BuyShopItemsEvent(player, this, item, amount);
        Bukkit.getPluginManager().callEvent(buyShopItemsEvent);

        player.playSound(player.getLocation(), ConfigManager.getSound("sounds.buy_item"), 1, 1);

        VMPlugin.log.add(new Date() + ": " + player.getName() + " bought " + amount + "x " + item.getType() + " from Admin Shop " + "(" + price.toPlainString() + ")");

    }

    @Override
    public void sellItem(ShopItem item, int amount, Player player) {
        Economy economy = VMPlugin.getEconomy();

        BigDecimal price = item.getBuyPrice(amount, true);

        if (!item.verifyPurchase(player, ItemMode.BUY, amount)) {
            return;
        }

        player.sendMessage(ConfigManager.getCurrencyBuilder("messages.sold_item_as_customer")
                .replace("%amount%", String.valueOf(amount))
                .replaceCurrency("%price%", price)
                .replace("%item%", item.getItemName())
                .replace("%shop%", getShopName()).build());

        economy.depositPlayer(player, price.doubleValue());
        removeItems(player.getInventory(), item.getRawItem(), amount);
        item.incrementPlayerTrades(player);
        item.incrementServerTrades();
        shopStats.addBought(amount);
        shopStats.addSpent(price.doubleValue());

        SellShopItemsEvent sellShopItemsEvent = new SellShopItemsEvent(player, this, item, amount);
        Bukkit.getPluginManager().callEvent(sellShopItemsEvent);

        player.playSound(player.getLocation(), ConfigManager.getSound("sounds.sell_item"), 0.5f, 1);

        String currency = config.getString("currency");
        String valueCurrency = (config.getBoolean("currency_before") ? currency + price : price + currency);
        VMPlugin.log.add(new Date() + ": " + player.getName() + " sold " + amount + "x " + item.getType() + " to: " + entityUUID + " (" + valueCurrency + ")");
    }

    @Override
    public String getModeCycle(String mode, boolean isItemTrade) {
        return ConfigManager.getString("menus.edit_item.mode_cycle.admin_shop." + (!isItemTrade ? mode : "item_trade"));
    }


    @Override
    public int getAvailable(ShopItem shopItem) {
        return Integer.MAX_VALUE;
    }

    /**
     * Runs when a Player wants to buy a command
     */
    public void buyCommand(Player player, ShopItem shopItem) {
        Economy economy = VMPlugin.getEconomy();

        BigDecimal price = shopItem.getSellPrice();
        if (economy.getBalance(player) < price.doubleValue()) {
            player.sendMessage(ConfigManager.getMessage("messages.not_enough_money"));
            return;
        }
        boolean bypass = player.hasPermission("villagermarket.bypass_limit");
        int limit = shopItem.getLimit();
        int serverTrades = shopItem.getServerTrades();
        int playerTrades = shopItem.getPlayerLimit(player);
        ShopItem.LimitMode limitMode = shopItem.getLimitMode();
        if (!bypass && limit > 0 && ((limitMode == ShopItem.LimitMode.SERVER && serverTrades >= limit) || (limitMode == ShopItem.LimitMode.PLAYER && playerTrades >= limit))) {
            player.sendMessage(ConfigManager.getMessage("messages.reached_command_limit"));
            return;
        }
        economy.withdrawPlayer(player, price.doubleValue());

        if (shopItem.getCommands() != null && !shopItem.getCommands().isEmpty()) {
            ConsoleCommandSender sender = Bukkit.getConsoleSender();
            for (String command : shopItem.getCommands()) {
                Bukkit.dispatchCommand(sender, command.replaceAll("%player%", player.getName()));
            }
        }

        shopItem.incrementPlayerTrades(player);
        shopItem.incrementServerTrades();
    }
}
