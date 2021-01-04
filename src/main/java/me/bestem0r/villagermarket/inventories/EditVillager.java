package me.bestem0r.villagermarket.inventories;

import me.bestem0r.villagermarket.utilities.ColorBuilder;
import me.bestem0r.villagermarket.utilities.Methods;
import org.bukkit.Bukkit;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

public class EditVillager {

    public EditVillager() {}

    public static Inventory create(JavaPlugin plugin) {
        Inventory inventory = Bukkit.createInventory(null, 9, new ColorBuilder(plugin).path("menus.edit_villager.title").build());

        ItemStack smith = Methods.stackFromPath(plugin, "menus.edit_villager.items.smith");
        ItemStack butcher = Methods.stackFromPath(plugin, "menus.edit_villager.items.butcher");
        ItemStack cartographer = Methods.stackFromPath(plugin, "menus.edit_villager.items.cartographer");
        ItemStack cleric = Methods.stackFromPath(plugin, "menus.edit_villager.items.cleric");
        ItemStack farmer = Methods.stackFromPath(plugin, "menus.edit_villager.items.farmer");
        ItemStack fisherman = Methods.stackFromPath(plugin, "menus.edit_villager.items.fisherman");
        ItemStack leatherWorker = Methods.stackFromPath(plugin, "menus.edit_villager.items.leatherworker");
        ItemStack librarian = Methods.stackFromPath(plugin, "menus.edit_villager.items.librarian");
        ItemStack back = Methods.stackFromPath(plugin, "items.back");
        
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
