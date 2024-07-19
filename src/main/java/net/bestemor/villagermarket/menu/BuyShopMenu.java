package net.bestemor.villagermarket.menu;

import net.bestemor.core.config.ConfigManager;
import net.bestemor.core.menu.Clickable;
import net.bestemor.core.menu.Menu;
import net.bestemor.core.menu.MenuConfig;
import net.bestemor.core.menu.MenuContent;
import net.bestemor.villagermarket.VMPlugin;
import net.bestemor.villagermarket.event.BuyShopEvent;
import net.bestemor.villagermarket.shop.PlayerShop;
import net.bestemor.villagermarket.shop.ShopMenu;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.math.BigDecimal;
import java.util.Date;

public class BuyShopMenu extends Menu {

    private final VMPlugin plugin;
    private final PlayerShop shop;

    public BuyShopMenu(VMPlugin plugin, PlayerShop shop) {
        super(MenuConfig.fromConfig("menus.buy_shop"));
        this.plugin = plugin;
        this.shop = shop;
    }

    @Override
    public void onCreate(MenuContent content) {

        String cost = String.valueOf(shop.getCost());

        int shopS = shop.getShopSize();
        int storageS = shop.getStorageSize();

        String infinite = ConfigManager.getString("quantity.infinite");

        String shopAmount = (shopS == 0 ? infinite : String.valueOf(shopS - 1));
        String storageAmount = (storageS == 0 ? infinite : String.valueOf(storageS - 1));

        String shortTime = shop.getDuration();
        String unit = shortTime.substring(shortTime.length() - 1);
        String amount = shortTime.substring(0, shortTime.length() - 1);
        String time = shortTime.equals("infinite") ? ConfigManager.getUnit("never", false) : amount + " " + ConfigManager.getUnit(unit, Integer.parseInt(amount) > 1);

        ItemStack shopSize = ConfigManager.getItem("menus.buy_shop.items.shop_size").replace("%amount%", shopAmount).build();
        ItemStack storageSize = ConfigManager.getItem( "menus.buy_shop.items.storage_size").replace("%amount%", storageAmount).build();
        ItemStack buyShop = ConfigManager.getItem("menus.buy_shop.items.buy_shop").replaceCurrency("%price%", new BigDecimal(cost)).replace("%time%", time).build();

        int[] fillerSlots = ConfigManager.getIntArray("menus.buy_shop.filler_slots");
        content.fillSlots(ConfigManager.getItem("items.filler").build(), fillerSlots);

        int shopSizeSlot = ConfigManager.getInt("menus.buy_shop.items.shop_size.slot");
        int storageSizeSlot = ConfigManager.getInt("menus.buy_shop.items.storage_size.slot");
        int buyShopSlot = ConfigManager.getInt("menus.buy_shop.items.buy_shop.slot");

        content.setClickable(shopSizeSlot, Clickable.empty(shopSize));
        content.setClickable(buyShopSlot, Clickable.of(buyShop, (event) -> {
            Player player = (Player) event.getWhoClicked();

            Economy economy = plugin.getEconomy();
            if (ConfigManager.getBoolean("buy_shop_permission") && !player.hasPermission("villagermarket.buy_shop")) {
                player.sendMessage(ConfigManager.getMessage("messages.no_permission_buy_shop"));
                return;
            }
            int max = plugin.getShopManager().getMaxShops(player);
            int owned = plugin.getShopManager().getOwnedShops(player).size();
            if (max != -1 && owned >= max) {
                player.sendMessage(ConfigManager.getMessage("messages.max_shops")
                        .replace("%current%", String.valueOf(owned))
                        .replace("%max%", String.valueOf(max)));

                player.playSound(player.getLocation(), ConfigManager.getSound("sounds.max_shops"), 1, 1);
                return;
            }
            if (economy.getBalance(player) < shop.getCost()) {
                player.sendMessage(ConfigManager.getMessage("messages.not_enough_money"));
                return;
            }

            BuyShopEvent buyShopEvent = new BuyShopEvent(player, shop);
            Bukkit.getPluginManager().callEvent(buyShopEvent);
            if (buyShopEvent.isCancelled()) {
                return;
            }

            economy.withdrawPlayer(player, shop.getCost());
            shop.setOwner(player);

            shop.updateMenu(ShopMenu.EDIT_SHOP);

            player.playSound(player.getLocation(), ConfigManager.getSound("sounds.buy_shop"), 1, 1);

            VMPlugin.log.add(new Date() + ": " + player.getName() + " bought shop for " + shop.getCost());
            shop.updateRedstone(false);
            shop.openInventory(player, ShopMenu.EDIT_SHOP);
        }));
        content.setClickable(storageSizeSlot, Clickable.empty(storageSize));
    }
}
