package bestem0r.villagermarket.inventories;

import bestem0r.villagermarket.VMPlugin;
import bestem0r.villagermarket.items.MenuItem;
import bestem0r.villagermarket.utilities.ColorBuilder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class ProfessionInventory {

    public static Inventory create() {
        Inventory inventory = Bukkit.createInventory(null, 9, ColorBuilder.color("menus.edit_villager.title"));

        FileConfiguration mainConfig = VMPlugin.getInstance().getConfig();

        MenuItem smith = new MenuItem.Builder(Material.IRON_CHESTPLATE)
                .nameFromPath("menus.edit_villager.items.smith")
                .build();
        MenuItem butcher = new MenuItem.Builder(Material.PORKCHOP)
                .nameFromPath("menus.edit_villager.items.butcher")
                .build();
        MenuItem cartographer = new MenuItem.Builder(Material.MAP)
                .nameFromPath("menus.edit_villager.items.cartographer")
                .build();
        MenuItem cleric = new MenuItem.Builder(Material.POTION)
                .nameFromPath("menus.edit_villager.items.cleric")
                .build();
        MenuItem farmer = new MenuItem.Builder(Material.WHEAT)
                .nameFromPath("menus.edit_villager.items.farmer")
                .build();
        MenuItem fisherman = new MenuItem.Builder(Material.COD)
                .nameFromPath("menus.edit_villager.items.fisherman")
                .build();
        MenuItem leatherWorker = new MenuItem.Builder(Material.LEATHER)
                .nameFromPath("menus.edit_villager.items.leatherworker")
                .build();
        MenuItem librarian = new MenuItem.Builder(Material.BOOK)
                .nameFromPath("menus.edit_villager.items.librarian")
                .build();

        MenuItem back = new MenuItem.Builder(Material.valueOf(mainConfig.getString("items.back.material")))
                .nameFromPath("items.back.name")
                .build();
        
        ItemStack[] villagerItems = {
                smith,
                butcher,
                cartographer,
                cleric,
                farmer,
                fisherman,
                leatherWorker,
                librarian,
                back
        };
        inventory.setContents(villagerItems);
        return inventory;
    }
}
