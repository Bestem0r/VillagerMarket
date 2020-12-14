package bestem0r.villagermarket.menus;

import bestem0r.villagermarket.VMPlugin;
import bestem0r.villagermarket.items.MenuItem;
import bestem0r.villagermarket.shops.AdminShop;
import bestem0r.villagermarket.shops.PlayerShop;
import bestem0r.villagermarket.shops.VillagerShop;
import bestem0r.villagermarket.utilities.Color;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.Date;


public abstract class EditShopMenu {

    public static Inventory create(VillagerShop villagerShop) {
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

        MenuItem sellShop = new MenuItem.Builder(Material.FEATHER)
                .nameFromPath("menus.edit_shop.items.sell_shop.name")
                .lore(new Color.Builder().path("menus.edit_shop.items.sell_shop.lore").buildLore())
                .build();

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

        MenuItem collectMoney = new MenuItem.Builder(Material.GOLD_INGOT)
                .nameFromPath("menus.edit_shop.items.collect_money.name")
                .lore(new Color.Builder()
                        .path("menus.edit_shop.items.collect_money.lore")
                        .replaceWithCurrency("%worth%", villagerShop.getCollectedMoney().stripTrailingZeros().toPlainString())
                        .buildLore())
                .build();

        MenuItem increaseTime = new MenuItem.Builder(Material.EMERALD)
                .nameFromPath("menus.edit_shop.items.increase_time.name")
                .lore(new Color.Builder()
                        .path("menus.edit_shop.items.increase_time.lore")
                        .replace("%expire%", String.valueOf(date))
                        .replace("%time%", time)
                        .replaceWithCurrency("%price%", String.valueOf(villagerShop.getCost()))
                        .buildLore())
                .build();

        MenuItem back = new MenuItem.Builder(Material.valueOf(mainConfig.getString("items.back.material")))
                .nameFromPath("items.back.name")
                .build();

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
