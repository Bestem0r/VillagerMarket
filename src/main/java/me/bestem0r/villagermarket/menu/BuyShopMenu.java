package me.bestem0r.villagermarket.menu;

import me.bestem0r.villagermarket.ConfigManager;
import me.bestem0r.villagermarket.VMPlugin;
import me.bestem0r.villagermarket.shop.PlayerShop;
import me.bestem0r.villagermarket.shop.ShopMenu;
import net.milkbowl.vault.economy.Economy;
import org.apache.commons.lang.WordUtils;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.math.BigDecimal;
import java.util.Date;

public class BuyShopMenu extends Menu {

    private final VMPlugin plugin;
    private final PlayerShop shop;

    public BuyShopMenu(VMPlugin plugin, PlayerShop shop) {
        super(plugin.getMenuListener(), 27, ConfigManager.getString("menus.buy_shop.title"));
        this.plugin = plugin;
        this.shop = shop;
    }

    @Override
    public void create(Inventory inventory) {

        String cost = String.valueOf(shop.getCost());

        int shopS = shop.getShopSize();
        int storageS = shop.getStorageSize();

        String infinite = WordUtils.capitalizeFully(ConfigManager.getString("quantity.infinite"));

        String shopAmount = (shopS == 0 ? infinite : String.valueOf(shopS - 1));
        String storageAmount = (storageS == 0 ? infinite : String.valueOf(storageS - 1));

        String shortTime = shop.getDuration();
        String unit = shortTime.substring(shortTime.length() - 1);
        String amount = shortTime.substring(0, shortTime.length() - 1);
        String time = shortTime.equals("infinite") ? ConfigManager.getUnit("never", false) : amount + " " + ConfigManager.getUnit(unit, Integer.parseInt(amount) > 1);

        ItemStack shopSize = ConfigManager.getItem("menus.buy_shop.items.shop_size").replace("%amount%", shopAmount).build();
        ItemStack storageSize = ConfigManager.getItem( "menus.buy_shop.items.storage_size").replace("%amount%", storageAmount).build();
        ItemStack buyShop = ConfigManager.getItem("menus.buy_shop.items.buy_shop").replaceCurrency("%price%", new BigDecimal(cost)).replace("%time%", time).build();

        fillEdges(ConfigManager.getItem("items.filler").build());
        inventory.setItem(12, shopSize);
        inventory.setItem(13, buyShop);
        inventory.setItem(14, storageSize);
    }


    @Override
    public void handleClick(InventoryClickEvent event) {
        if (event.getRawSlot() >= 27) { return; }

        event.setCancelled(true);

        if (event.getRawSlot() == 13) {
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

            economy.withdrawPlayer(player, shop.getCost());
            shop.setOwner(player);

            shop.updateMenu(ShopMenu.EDIT_SHOP);

            player.playSound(player.getLocation(), ConfigManager.getSound("sounds.buy_shop"), 1, 1);

            VMPlugin.log.add(new Date() + ": " + player.getName() + " bought shop for " + shop.getCost());
            shop.updateRedstone(false);
            shop.openInventory(player, ShopMenu.EDIT_SHOP);
        }
    }
}
