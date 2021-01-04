package me.bestem0r.villagermarket.inventories;

import me.bestem0r.villagermarket.shops.VillagerShop;
import me.bestem0r.villagermarket.utilities.ColorBuilder;
import me.bestem0r.villagermarket.utilities.Methods;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

public class SellShop {

    private SellShop() {}

    public static Inventory create(JavaPlugin plugin, VillagerShop villagerShop) {
        Inventory inventory = Bukkit.createInventory(null, 9, new ColorBuilder(plugin).path("menus.sell_shop.title").build());

        FileConfiguration mainConfig = plugin.getConfig();
        String priceHalved = String.valueOf((double) villagerShop.getCost() * (mainConfig.getDouble("refund_percent") / 100) * villagerShop.getTimesRented());

        String configPath = (villagerShop.getCost() == -1 ? "yes_remove" : "yes_sell");

        ItemStack cancel = Methods.stackFromPath(plugin, "menus.sell_shop.items.no_cancel");

        ItemStack filler = Methods.stackFromPath(plugin, "items.filler");
        ItemStack confirm = Methods.stackFromPath(plugin, "menus.sell_shop.items." + configPath);

        ItemMeta confirmMeta = confirm.getItemMeta();
        confirmMeta.setLore(new ColorBuilder(plugin).path("menus.sell_shop.items." + configPath + ".lore")
                .replaceWithCurrency("%amount%", priceHalved)
                .buildLore());
        confirm.setItemMeta(confirmMeta);

        ItemStack[] sellItems = {
                filler,
                filler,
                filler,
                confirm,
                filler,
                cancel,
                filler,
                filler,
                filler
        };

        inventory.setContents(sellItems);
        return inventory;
    }
}
