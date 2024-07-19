package net.bestemor.villagermarket.menu;

import net.bestemor.core.config.ConfigManager;
import net.bestemor.core.menu.*;
import net.bestemor.villagermarket.VMPlugin;
import net.bestemor.villagermarket.shop.PlayerShop;
import net.bestemor.villagermarket.shop.ShopMenu;
import net.bestemor.villagermarket.shop.VillagerShop;
import net.bestemor.villagermarket.utils.VMUtils;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.math.BigDecimal;

public class SellShopMenu extends Menu {

    private final VMPlugin plugin;
    private final VillagerShop shop;

    public SellShopMenu(VMPlugin plugin, VillagerShop shop) {
        super(MenuConfig.fromConfig("menus.sell_shop"));
        this.plugin = plugin;
        this.shop = shop;
    }

    @Override
    public void onCreate(MenuContent content) {
        String priceHalved = String.valueOf((double) shop.getCost() * (ConfigManager.getDouble("refund_percent") / 100) * shop.getTimesRented());

        String configPath = (shop.getCost() == -1 ? "yes_remove" : "yes_sell");

        ItemStack confirmItem = ConfigManager.getItem("menus.sell_shop.items." + configPath)
                .replaceCurrency("%amount%", new BigDecimal(priceHalved)).build();

        int slot = ConfigManager.getInt("menus.sell_shop.items." + configPath + ".slot");
        content.setClickable(slot, Clickable.of(confirmItem, event -> {
            PlayerShop playerShop = (PlayerShop) shop;

            Player player = (Player) event.getWhoClicked();
            player.playSound(player.getLocation(), ConfigManager.getSound("sounds.sell_shop"), 0.5f, 1);
            player.playSound(player.getLocation(), ConfigManager.getSound("sounds.menu_click"), 0.5f, 1);

            playerShop.abandon();
            if (playerShop.getCost() != -1) {
                player.sendMessage(ConfigManager.getMessage("messages.sold_shop"));
            } else if (plugin.getConfig().getBoolean("drop_spawn_egg")) {
                Location location = VMUtils.getEntity(playerShop.getEntityUUID()).getLocation();
                location.getWorld().dropItemNaturally(location, plugin.getShopManager().getShopItem(plugin, shop.getShopSize() / 9, shop.getStorageSize() / 9, 1));
                plugin.getShopManager().removeShop(shop.getEntityUUID());
            }
            event.getView().close();
        }));

        content.setPlaced(PlacedClickable.fromConfig("menus.sell_shop.items.no_cancel", event -> {
            shop.openInventory((Player) event.getWhoClicked(), ShopMenu.EDIT_SHOP);
        }));


        int[] fillerSlots = ConfigManager.getIntArray("menus.sell_shop.filler_slots");
        content.fillSlots(ConfigManager.getItem("items.filler").build(), fillerSlots);
    }
}
