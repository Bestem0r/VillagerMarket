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


public abstract class EditShopMenu {

    public static Inventory create(VillagerShop.VillagerType villagerType) {
        Inventory inventory = Bukkit.createInventory(null, 9, new Color.Builder().path("menus.edit_shop.title").build());

        FileConfiguration mainConfig = VMPlugin.getInstance().getConfig();

        MenuItem editShopfront = new MenuItem.Builder(Material.WRITABLE_BOOK)
                .nameFromPath("menus.edit_shop.items.edit_shopfront.name")
                .lore(new Color.Builder().path("menus.edit_shop.items.edit_shopfront.lore").buildLore())
                .build();

        MenuItem previewShop = new MenuItem.Builder(Material.BOOK)
                .nameFromPath("menus.edit_shop.items.preview_shop.name")
                .lore(new Color.Builder().path("menus.edit_shop.items.preview_shop.lore").buildLore())
                .build();

        MenuItem storage = new MenuItem.Builder(Material.CHEST)
                .nameFromPath("menus.edit_shop.items.edit_storage.name")
                .lore(new Color.Builder().path("menus.edit_shop.items.edit_storage.lore").buildLore())
                .build();

        MenuItem editVillager = new MenuItem.Builder(Material.VILLAGER_SPAWN_EGG)
                .nameFromPath("menus.edit_shop.items.edit_villager.name")
                .lore(new Color.Builder().path("menus.edit_shop.items.edit_villager.lore").buildLore())
                .build();

        MenuItem changeName = new MenuItem.Builder(Material.NAME_TAG)
                .nameFromPath("menus.edit_shop.items.change_name.name")
                .lore(new Color.Builder().path("menus.edit_shop.items.change_name.lore").buildLore())
                .build();

        MenuItem sellShop = new MenuItem.Builder(Material.EMERALD)
                .nameFromPath("menus.edit_shop.items.sell_shop.name")
                .lore(new Color.Builder().path("menus.edit_shop.items.sell_shop.lore").buildLore())
                .build();

        MenuItem back = new MenuItem.Builder(Material.valueOf(mainConfig.getString("items.back.material")))
                .nameFromPath("items.back.name")
                .build();

        ItemStack[] inventoryItems;
        if (villagerType == VillagerShop.VillagerType.ADMIN) {
            inventoryItems = new ItemStack[] {
                    editShopfront,
                    previewShop,
                    editVillager,
                    changeName,
                    null,
                    null,
                    null,
                    null,
                    back
            };
        } else {
            inventoryItems = new ItemStack[] {
                    editShopfront,
                    previewShop,
                    storage,
                    editVillager,
                    changeName,
                    sellShop,
                    null,
                    null,
                    back
            };
        }
        inventory.setContents(inventoryItems);
        return inventory;
    }
}
