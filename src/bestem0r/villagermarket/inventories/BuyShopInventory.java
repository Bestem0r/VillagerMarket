package bestem0r.villagermarket.inventories;

import bestem0r.villagermarket.VMPlugin;
import bestem0r.villagermarket.items.MenuItem;
import bestem0r.villagermarket.shops.VillagerShop;
import bestem0r.villagermarket.utilities.ColorBuilder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class BuyShopInventory {

    public static Inventory create(VillagerShop villagerShop) {
        Inventory inventory = Bukkit.createInventory(null, 9, ColorBuilder.color("menus.buy_shop.title"));

        FileConfiguration mainConfig = VMPlugin.getInstance().getConfig();

        String cost = String.valueOf(villagerShop.getCost());
        String forSaleAmount = String.valueOf(villagerShop.getSize() * 9 - 1);
        String storageAmount = String.valueOf(villagerShop.getSize() * 18 - 1);

        String forSaleSizeName = ColorBuilder.colorReplace("menus.buy_shop.items.for_sale_size.name", "%amount%", forSaleAmount);
        String storageSizeName = ColorBuilder.colorReplace("menus.buy_shop.items.storage_size.name", "%amount%", storageAmount);

        MenuItem forSaleSize = new MenuItem.Builder(Material.valueOf(mainConfig.getString("menus.buy_shop.items.for_sale_size.material")))
                .nameFromPath(forSaleSizeName)
                .build();

        MenuItem storageSize = new MenuItem.Builder(Material.valueOf(mainConfig.getString("menus.buy_shop.items.storage_size.material")))
                .nameFromPath(storageSizeName)
                .build();

        MenuItem fillerItem = new MenuItem.Builder(Material.BLUE_STAINED_GLASS_PANE)
                .name(" ")
                .build();

        MenuItem buyShop = new MenuItem.Builder(Material.valueOf(mainConfig.getString("menus.buy_shop.items.buy_shop.material")))
                .nameFromPath("menus.buy_shop.items.buy_shop.name")
                .lore(ColorBuilder.loreReplace("menus.buy_shop.items.buy_shop.lore", "%price%", cost))
                .build();

        ItemStack[] items = {
                fillerItem,
                fillerItem,
                fillerItem,
                forSaleSize,
                buyShop,
                storageSize,
                fillerItem,
                fillerItem,
                fillerItem};
        inventory.setContents(items);

        return inventory;
    }
}
