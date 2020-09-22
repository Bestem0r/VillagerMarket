package bestem0r.villagermarket.menus;

import bestem0r.villagermarket.VMPlugin;
import bestem0r.villagermarket.items.ShopItem;
import bestem0r.villagermarket.items.MenuItem;
import bestem0r.villagermarket.shops.VillagerShop;
import bestem0r.villagermarket.utilities.Color;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.Arrays;
import java.util.HashMap;

public abstract class ShopfrontMenu {

    public static class Builder {

        private VillagerShop villagerShop;
        private boolean isEditor = false;
        private int size;
        private HashMap<Integer, ShopItem> itemList;
        private ShopItem.LoreType loreType;

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
        public Builder itemList(HashMap<Integer, ShopItem> itemsForSale) {
            this.itemList = itemsForSale;
            return this;
        }
        public Builder loreType(ShopItem.LoreType loreType) {
            this.loreType = loreType;
            return this;
        }

        public Inventory build() {
            String title = (isEditor ? new Color.Builder().path("menus.edit_shopfront.title").build() : new Color.Builder().path("menus.shopfront.title").build());
            String detailSuffix = new Color.Builder().path("menus.shopfront.detail_suffix").build();
            title = (!isEditor && loreType == ShopItem.LoreType.ITEM ? title + " " + detailSuffix : title);
            Inventory inventory = Bukkit.createInventory(null, size, ChatColor.DARK_GRAY + title);

            ItemStack[] inventoryItems = new ItemStack[size];
            Arrays.fill(inventoryItems, null);

            for (Integer slot : itemList.keySet()) {
                if (itemList.get(slot) != null) {
                    ShopItem shopItem = itemList.get(slot);
                    shopItem.toggleEditor(isEditor);
                    shopItem.refreshLore(villagerShop);
                    inventoryItems[slot] = shopItem.asItemStack(loreType);
                } else {
                    inventoryItems[slot] = null;
                }

            }
            inventory.setContents(inventoryItems);
            FileConfiguration mainConfig = VMPlugin.getInstance().getConfig();

            MenuItem back = new MenuItem.Builder(Material.valueOf(mainConfig.getString("items.back.material")))
                    .nameFromPath("items.back.name")
                    .build();

            MenuItem toggleDetails = new MenuItem.Builder(Material.valueOf(mainConfig.getString("items.toggle_details.material")))
                    .nameFromPath("items.toggle_details.name")
                    .lore(new Color.Builder().path("items.toggle_details.lore").buildLore())
                    .build();

            inventory.setItem(size - 1, (isEditor) ? back : toggleDetails);
            return inventory;
        }
    }
}
