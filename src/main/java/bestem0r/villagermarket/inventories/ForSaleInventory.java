package bestem0r.villagermarket.inventories;

import bestem0r.villagermarket.VMPlugin;
import bestem0r.villagermarket.items.ItemForSale;
import bestem0r.villagermarket.items.MenuItem;
import bestem0r.villagermarket.shops.VillagerShop;
import bestem0r.villagermarket.utilities.ColorBuilder;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.Arrays;
import java.util.HashMap;

public class ForSaleInventory {

    public static class Builder {

        private VillagerShop villagerShop;
        private boolean isEditor = false;
        private int size;
        private HashMap<Integer, ItemForSale> itemList;

        public Builder(VillagerShop villagerShop) {
            this.villagerShop = villagerShop;
        }
        public Builder isEditor(boolean isEditor) {
            this.isEditor = isEditor;
            return this;
        }
        public Builder size(int size) {
            this.size = size;
            return this;
        }
        public Builder itemList(HashMap<Integer, ItemForSale> itemsForSale) {
            this.itemList = itemsForSale;
            return this;
        }

        public Inventory build() {
            String title = (isEditor ? ColorBuilder.color("menus.edit_for_sale.title") : ColorBuilder.color("menus.buy_items.title"));
            Inventory inventory = Bukkit.createInventory(null, size, ChatColor.DARK_GRAY + title);

            ItemStack[] inventoryItems = new ItemStack[size];
            Arrays.fill(inventoryItems, null);

            for (Integer slot : itemList.keySet()) {
                if (itemList.get(slot) != null) {
                    ItemForSale itemForSale = itemList.get(slot);
                    itemForSale.toggleEditor(isEditor);
                    itemForSale.updateStorage(villagerShop);
                    inventoryItems[slot] = itemForSale.asItemStack();
                } else {
                    inventoryItems[slot] = null;
                }

            }
            inventory.setContents(inventoryItems);
            FileConfiguration mainConfig = VMPlugin.getInstance().getConfig();

            MenuItem back = new MenuItem.Builder(Material.valueOf(mainConfig.getString("items.back.material")))
                    .nameFromPath("items.back.name")
                    .build();

            if (isEditor) inventory.setItem(size - 1, back);
            return inventory;
        }
    }
}
