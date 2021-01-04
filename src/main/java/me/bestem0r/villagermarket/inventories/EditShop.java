package me.bestem0r.villagermarket.inventories;

import me.bestem0r.villagermarket.shops.AdminShop;
import me.bestem0r.villagermarket.shops.PlayerShop;
import me.bestem0r.villagermarket.shops.VillagerShop;
import me.bestem0r.villagermarket.utilities.ColorBuilder;
import me.bestem0r.villagermarket.utilities.Methods;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Date;


public class EditShop {

    private EditShop() {}

    public static Inventory create(JavaPlugin plugin, VillagerShop villagerShop) {
        Inventory inventory = Bukkit.createInventory(null, 9, new ColorBuilder(plugin).path("menus.edit_shop.title").build());

        FileConfiguration mainConfig = plugin.getConfig();

        ItemStack editShopfront = Methods.stackFromPath(plugin, "menus.edit_shop.items.edit_shopfront");
        ItemStack previewShop = Methods.stackFromPath(plugin, "menus.edit_shop.items.preview_shop");
        ItemStack storage = Methods.stackFromPath(plugin, "menus.edit_shop.items.edit_storage");
        ItemStack editVillager = Methods.stackFromPath(plugin, "menus.edit_shop.items.edit_villager");
        ItemStack changeName = Methods.stackFromPath(plugin, "menus.edit_shop.items.change_name");
        ItemStack sellShop = Methods.stackFromPath(plugin, "menus.edit_shop.items.sell_shop");

        Date date = new Date(villagerShop.getExpireDate().getTime());

        String time;
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
                break;
            default:
                time = "never";
        }

        ItemStack collectMoney = Methods.stackFromPath(plugin, "menus.edit_shop.items.collect_money");
        ItemMeta collectMeta = collectMoney.getItemMeta();
        collectMeta.setLore(new ColorBuilder(plugin)
                .path("menus.edit_shop.items.collect_money.lore")
                .replaceWithCurrency("%worth%", villagerShop.getCollectedMoney().stripTrailingZeros().toPlainString())
                .buildLore());
        collectMoney.setItemMeta(collectMeta);

        ItemStack increaseTime = Methods.stackFromPath(plugin, "menus.edit_shop.items.increase_time");
        ItemMeta increaseMeta = increaseTime.getItemMeta();
        increaseMeta.setLore(new ColorBuilder(plugin)
                .path("menus.edit_shop.items.increase_time.lore")
                .replace("%expire%", String.valueOf(date))
                .replace("%time%", time)
                .replaceWithCurrency("%price%", String.valueOf(villagerShop.getCost()))
                .buildLore());
        increaseTime.setItemMeta(increaseMeta);

        ItemStack back = Methods.stackFromPath(plugin, "items.back");

        ItemStack[] inventoryItems;
        if (villagerShop instanceof AdminShop) {
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
        if (villagerShop instanceof PlayerShop && !time.equals("never")) {
            inventoryItems[7] = increaseTime;
        }
        if (villagerShop instanceof PlayerShop && mainConfig.getBoolean("require_collect")) {
            inventoryItems[6] = collectMoney;
        }
        inventory.setContents(inventoryItems);
        return inventory;
    }
}
