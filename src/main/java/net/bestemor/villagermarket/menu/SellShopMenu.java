package net.bestemor.villagermarket.menu;

import net.bestemor.core.config.ConfigManager;
import net.bestemor.core.menu.Clickable;
import net.bestemor.core.menu.Menu;
import net.bestemor.core.menu.MenuContent;
import net.bestemor.villagermarket.VMPlugin;
import net.bestemor.villagermarket.shop.PlayerShop;
import net.bestemor.villagermarket.shop.ShopMenu;
import net.bestemor.villagermarket.shop.VillagerShop;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.math.BigDecimal;

public class SellShopMenu extends Menu {

    private final VMPlugin plugin;
    private final VillagerShop shop;

    public SellShopMenu(VMPlugin plugin, VillagerShop shop) {
        super(plugin.getMenuListener(), 27, ConfigManager.getString("menus.sell_shop.title"));
        this.plugin = plugin;
        this.shop = shop;
    }

    @Override
    public void onCreate(MenuContent content) {
        String priceHalved = String.valueOf((double) shop.getCost() * (ConfigManager.getDouble("refund_percent") / 100) * shop.getTimesRented());

        String configPath = (shop.getCost() == -1 ? "yes_remove" : "yes_sell");

        ItemStack fillItem = ConfigManager.getItem("items.filler").build();
        ItemStack cancel = ConfigManager.getItem("menus.sell_shop.items.no_cancel").build();
        ItemStack confirmItem = ConfigManager.getItem("menus.sell_shop.items." + configPath)
                .replaceCurrency("%amount%", new BigDecimal(priceHalved)).build();

        content.setClickable(12, Clickable.of(confirmItem, event -> {
            PlayerShop playerShop = (PlayerShop) shop;

            Player player = (Player) event.getWhoClicked();
            player.playSound(player.getLocation(), ConfigManager.getSound("sounds.sell_shop"), 0.5f, 1);
            player.playSound(player.getLocation(), ConfigManager.getSound("sounds.menu_click"), 0.5f, 1);

            playerShop.abandon();
            if (playerShop.getCost() != -1) {
                player.sendMessage(ConfigManager.getMessage("messages.sold_shop"));
            } else if (plugin.getConfig().getBoolean("drop_spawn_egg")) {
                Location location = Bukkit.getEntity(playerShop.getEntityUUID()).getLocation();
                location.getWorld().dropItemNaturally(location, plugin.getShopManager().getShopItem(plugin, shop.getShopSize() / 9, shop.getStorageSize() / 9, 1));
                plugin.getShopManager().removeShop(shop.getEntityUUID());
            }
            event.getView().close();
        }));

        content.setClickable(14, Clickable.of(cancel, event -> {
            shop.openInventory((Player) event.getWhoClicked(), ShopMenu.EDIT_SHOP);
        }));

        content.fillEdges(fillItem);
    }
}
