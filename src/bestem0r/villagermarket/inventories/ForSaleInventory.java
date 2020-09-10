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
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

public class ForSaleInventory {

    public static class Builder {

        private final VillagerShop villagerShop;
        private VillagerShop.VillagerType villagerType = VillagerShop.VillagerType.PLAYER;
        private boolean isEditor = false;
        private int size;
        private HashMap<Integer, ItemForSale> itemsForSale = new HashMap<>();

        public Builder(VillagerShop villagerShop) {
            this.villagerShop = villagerShop;
        }
        public Builder villagerType(VillagerShop.VillagerType villagerType) {
            this.villagerType = villagerType;
            return this;
        }
        public Builder isEditor(boolean isEditor) {
            this.isEditor = isEditor;
            return this;
        }
        public Builder size(int size) {
            this.size = size;
            return this;
        }
        public Builder itemsForSale(HashMap<Integer, ItemForSale> itemsForSale) {
            this.itemsForSale = itemsForSale;
            return this;
        }

        public Inventory build() {
            String title = (isEditor ? ColorBuilder.color("menus.edit_for_sale.title") : ColorBuilder.color("menus.buy_items.title"));
            Inventory inventory = Bukkit.createInventory(null, size, ChatColor.DARK_GRAY + title);

            ItemStack[] inventoryItems = new ItemStack[size];
            Arrays.fill(inventoryItems, null);

            for (Integer slot : itemsForSale.keySet()) {
                ItemForSale itemForSale = itemsForSale.get(slot);
                if (itemForSale == null) continue;
                ItemMeta itemMeta = itemForSale.getItemMeta();
                if (itemMeta == null) continue;

                ArrayList<String> itemLore;

                int itemAmount = itemForSale.getAmount();
                int storageAmount = villagerShop.getItemAmount(itemForSale.asItemStack());
                double price = itemForSale.getWorth();

                String storageAmount_s = String.valueOf(storageAmount);
                String itemAmount_s = String.valueOf(itemAmount);
                String price_s = String.valueOf(price);

                if (villagerType == VillagerShop.VillagerType.ADMIN) {
                    if (isEditor) {
                        itemLore = ColorBuilder.loreReplaceTwo("menus.edit_for_sale.item_lore_admin", "%amount%", itemAmount_s, "%price%", price_s);
                    } else {
                        itemLore = ColorBuilder.loreReplaceTwo("menus.buy_items.item_lore_admin", "%amount%", itemAmount_s, "%price%", price_s);
                    }
                } else {
                    if (isEditor) {
                        itemLore = ColorBuilder.loreReplaceThree("menus.edit_for_sale.item_lore", "%amount%", itemAmount_s, "%price%", price_s, "%stock%", storageAmount_s);
                    } else {
                        itemLore = ColorBuilder.loreReplaceThree("menus.buy_items.item_lore", "%amount%", itemAmount_s, "%price%", price_s, "%stock%", storageAmount_s);
                    }
                }

                itemMeta.setLore(itemLore);
                ItemStack inventoryStack = new ItemStack(itemForSale.getType(), itemAmount);
                inventoryStack.setItemMeta(itemMeta);
                inventoryItems[slot] = inventoryStack;
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
