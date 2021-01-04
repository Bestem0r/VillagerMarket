package me.bestem0r.villagermarket.inventories;

import me.bestem0r.villagermarket.shops.VillagerShop;
import me.bestem0r.villagermarket.utilities.ColorBuilder;
import me.bestem0r.villagermarket.utilities.Methods;
import org.apache.commons.lang.WordUtils;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

public class BuyShop {

    private BuyShop() {}

    public static Inventory create(JavaPlugin plugin, VillagerShop villagerShop) {
        Inventory inventory = Bukkit.createInventory(null, 9, new ColorBuilder(plugin).path("menus.buy_shop.title").build());

        FileConfiguration mainConfig = plugin.getConfig();

        String cost = String.valueOf(villagerShop.getCost());

        int shopS = villagerShop.getShopSize();
        int storageS = villagerShop.getStorageSize();

        String infinite = WordUtils.capitalizeFully(plugin.getConfig().getString("quantity.infinite"));

        String shopAmount = (shopS == 0 ? infinite : String.valueOf(shopS - 1));
        String storageAmount = (storageS == 0 ? infinite : String.valueOf(storageS - 1));

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

        String shopName = new ColorBuilder(plugin)
                .path("menus.buy_shop.items.shop_size.name")
                .replace("%amount%", shopAmount)
                .build();

        String storageName = new ColorBuilder(plugin)
                .path("menus.buy_shop.items.storage_size.name")
                .replace("%amount%", storageAmount)
                .build();

        ItemStack shopSize = Methods.stackFromPath(plugin, "menus.buy_shop.items.shop_size");
        ItemMeta shopMeta = shopSize.getItemMeta();
        shopMeta.setDisplayName(shopName);
        shopSize.setItemMeta(shopMeta);

        ItemStack storageSize = Methods.stackFromPath(plugin, "menus.buy_shop.items.storage_size");
        ItemMeta storageMeta = storageSize.getItemMeta();
        storageMeta.setDisplayName(storageName);
        storageSize.setItemMeta(storageMeta);

        ItemStack fillerItem = Methods.stackFromPath(plugin, "items.filler");

        ItemStack buyShop = Methods.stackFromPath(plugin, "menus.buy_shop.items.buy_shop");

        ItemMeta buyShopMeta = buyShop.getItemMeta();
        buyShopMeta.setLore(new ColorBuilder(plugin)
                .path("menus.buy_shop.items.buy_shop.lore")
                .replaceWithCurrency("%price%", cost)
                .replace("%time%", time)
                .buildLore());
        buyShop.setItemMeta(buyShopMeta);


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
