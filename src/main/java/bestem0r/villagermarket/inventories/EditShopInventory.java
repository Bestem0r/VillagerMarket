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


public class EditShopInventory {

    public static Inventory create(VillagerShop.VillagerType villagerType) {
        Inventory inventory = Bukkit.createInventory(null, 9, ColorBuilder.color("menus.edit_shop.title"));

        FileConfiguration mainConfig = VMPlugin.getInstance().getConfig();

        MenuItem editForSale = new MenuItem.Builder(Material.WRITABLE_BOOK)
                .nameFromPath("menus.edit_shop.items.edit_for_sale.name")
                .lore(ColorBuilder.lore("menus.edit_shop.items.edit_for_sale.lore"))
                .build();

        MenuItem storage = new MenuItem.Builder(Material.CHEST)
                .nameFromPath("menus.edit_shop.items.edit_storage.name")
                .lore(ColorBuilder.lore("menus.edit_shop.items.edit_storage.lore"))
                .build();

        MenuItem editVillager = new MenuItem.Builder(Material.VILLAGER_SPAWN_EGG)
                .nameFromPath("menus.edit_shop.items.edit_villager.name")
                .lore(ColorBuilder.lore("menus.edit_shop.items.edit_villager.lore"))
                .build();

        MenuItem sellShop = new MenuItem.Builder(Material.EMERALD)
                .nameFromPath("menus.edit_shop.items.sell_shop.name")
                .lore(ColorBuilder.lore("menus.edit_shop.items.sell_shop.lore"))
                .build();

        MenuItem back = new MenuItem.Builder(Material.valueOf(mainConfig.getString("items.back.material")))
                .nameFromPath("items.back.name")
                .build();

        ItemStack[] inventoryItems;
        if (villagerType == VillagerShop.VillagerType.ADMIN) {
            inventoryItems = new ItemStack[] {
                    editForSale,
                    editVillager,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    back
            };
        } else {
            inventoryItems = new ItemStack[] {
                    editForSale,
                    editVillager,
                    storage,
                    sellShop,
                    null,
                    null,
                    null,
                    null,
                    back
            };
        }
        inventory.setContents(inventoryItems);
        return inventory;
    }
}
