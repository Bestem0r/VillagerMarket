package bestem0r.villagermarket.menus;

import bestem0r.villagermarket.VMPlugin;
import bestem0r.villagermarket.items.MenuItem;
import bestem0r.villagermarket.shops.VillagerShop;
import bestem0r.villagermarket.utilities.Color;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public abstract class BuyShopMenu {

    public static Inventory create(VillagerShop villagerShop) {
        Inventory inventory = Bukkit.createInventory(null, 9, new Color.Builder().path("menus.buy_shop.title").build());

        FileConfiguration mainConfig = VMPlugin.getInstance().getConfig();

        String cost = String.valueOf(villagerShop.getCost());
        String shopAmount = String.valueOf(villagerShop.getShopfrontSize() - 1);
        String storageAmount = String.valueOf(villagerShop.getStorageSize() - 1);

        String time = mainConfig.getString("time.indefinitely");
        String time_short = villagerShop.getDuration();
        String unit = time_short.substring(time_short.length() - 1);
        String amount = time_short.substring(0, time_short.length() - 1);
        switch (unit) {
            case "s":
                time = amount + " " + mainConfig.getString("time.seconds");
                break;
            case "m":
                time = amount + " " + mainConfig.getString("time.minutes");
                break;
            case "h":
                time = amount + " " + mainConfig.getString("time.hours");
                break;
            case "d":
                time = amount + " " + mainConfig.getString("time.days");
        }

        String shopName = new Color.Builder()
                .path("menus.buy_shop.items.shop_size.name")
                .replace("%amount%", shopAmount)
                .build();

        String storageName = new Color.Builder()
                .path("menus.buy_shop.items.storage_size.name")
                .replace("%amount%", storageAmount)
                .build();

        MenuItem shopSize = new MenuItem.Builder(Material.valueOf(mainConfig.getString("menus.buy_shop.items.shop_size.material")))
                .nameFromPath(shopName)
                .build();

        MenuItem storageSize = new MenuItem.Builder(Material.valueOf(mainConfig.getString("menus.buy_shop.items.storage_size.material")))
                .nameFromPath(storageName)
                .build();

        MenuItem fillerItem = new MenuItem.Builder(Material.BLUE_STAINED_GLASS_PANE)
                .name(" ")
                .build();

        MenuItem buyShop = new MenuItem.Builder(Material.valueOf(mainConfig.getString("menus.buy_shop.items.buy_shop.material")))
                .nameFromPath("menus.buy_shop.items.buy_shop.name")
                .lore(new Color.Builder()
                        .path("menus.buy_shop.items.buy_shop.lore")
                        .replace("%price%", cost + VMPlugin.getCurrency())
                        .replace("%time%", time)
                        .buildLore())
                .build();

        ItemStack[] items = {
                fillerItem,
                fillerItem,
                fillerItem,
                shopSize,
                buyShop,
                storageSize,
                fillerItem,
                fillerItem,
                fillerItem};
        inventory.setContents(items);

        return inventory;
    }
}
