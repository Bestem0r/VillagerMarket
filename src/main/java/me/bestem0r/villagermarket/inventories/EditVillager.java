package me.bestem0r.villagermarket.inventories;

import me.bestem0r.villagermarket.VMPlugin;
import me.bestem0r.villagermarket.utilities.ColorBuilder;
import me.bestem0r.villagermarket.utilities.Methods;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Villager;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.Locale;

public class EditVillager {

    public EditVillager() {}

    public static Inventory create(VMPlugin plugin) {
        Inventory inventory = Bukkit.createInventory(null, 18, new ColorBuilder(plugin).path("menus.edit_villager.title").build());

        Villager.Profession[] professions = Villager.Profession.values();
        for (int i = 0; i < professions.length; i++) {
            inventory.setItem(i, Methods.stackFromPath(plugin, "menus.edit_villager.items." + professions[i].name().toLowerCase(Locale.ROOT)));
        }
        if (plugin.isCitizensEnabled()) {
            inventory.setItem(16, Methods.stackFromPath(plugin, "menus.edit_villager.citizens_item"));
        }
        inventory.setItem(17, Methods.stackFromPath(plugin, "items.back"));
        return inventory;
    }
}
