package me.bestem0r.villagermarket.inventories;

import me.bestem0r.villagermarket.utilities.ColorBuilder;
import me.bestem0r.villagermarket.utilities.Methods;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

public class StorageBuilder {

    private final List<ItemStack> items;
    private final JavaPlugin plugin;

    private final boolean isInfinite;
    private final int size;

    public StorageBuilder(JavaPlugin plugin, int size, List<ItemStack> items) {
        this.plugin = plugin;
        this.items = items;
        if (size == 0) {
            this.isInfinite = true;
            this.size = 54;
        } else {
            this.isInfinite = false;
            this.size = size;
        }
    }


    public List<Inventory> create() {
        List<Inventory> inventories = new ArrayList<>();

        NamespacedKey key = new NamespacedKey(plugin, "vm-gui-item");

        ItemStack back = Methods.stackFromPath(plugin, "items.back");
        setPersistentDataString(back, key, "back");

        String title = new ColorBuilder(plugin).path("menus.storage.title").build();

        if (isInfinite) {
            ItemStack filler = Methods.stackFromPath(plugin, "items.filler");
            setPersistentDataString(filler, key, "filler");
            ItemStack next = Methods.stackFromPath(plugin, "menus.storage.items.next");
            ItemStack previous = Methods.stackFromPath(plugin, "menus.storage.items.previous");

            Inventory first = Bukkit.createInventory(null, size, title + " | 1");
            first.setContents(items.subList(0, Math.min(items.size(), 44)).toArray(new ItemStack[0]));

            fillBottom(first, filler);
            first.setItem(size - 1, back);
            setPersistentDataInt(next, key, 1);
            first.setItem(50, next);

            inventories.add(first);

            int midPages = (int) Math.ceil((double) (items.size() - 45) / 45);
            if (midPages > 0) {
                for (int i = 1; i < midPages + 1; i++) {
                    Inventory midPage = Bukkit.createInventory(null, size, title + " | " + (i + 1));

                    midPage.setContents(items.subList(Math.min(i * 45, items.size()), Math.min(i * 45 + 45, items.size())).toArray(new ItemStack[0]));
                    fillBottom(midPage, filler);

                    setPersistentDataInt(previous, key, i - 1);
                    midPage.setItem(48, previous);
                    setPersistentDataInt(next, key, i + 1);
                    midPage.setItem(50, next);
                    midPage.setItem(53, back);

                    inventories.add(midPage);
                }
            }
            Inventory last = Bukkit.createInventory(null, size, title + " | " + (midPages > 0 ? midPages + 2 : 2));

            fillBottom(last, filler);
            last.setItem(size - 1, back);
            setPersistentDataInt(previous, key, (Math.max(midPages, 0)) );
            last.setItem(48, previous);

            inventories.add(last);

        } else {
            Inventory inventory = Bukkit.createInventory(null, size, title);
            inventory.setContents(items.toArray(new ItemStack[0]));
            inventory.setItem(size - 1, back);
            inventories.add(inventory);
        }

        return inventories;
    }

    private void fillBottom(Inventory inventory, ItemStack itemStack) {
        for (int i = inventory.getSize() - 9; i < inventory.getSize(); i ++) {
            inventory.setItem(i, itemStack);
        }
    }

    private void setPersistentDataString(ItemStack itemStack, NamespacedKey key, String value) {
        ItemMeta itemMeta = itemStack.getItemMeta();
        itemMeta.getPersistentDataContainer().set(key, PersistentDataType.STRING, value);
        itemStack.setItemMeta(itemMeta);
    }

    private void setPersistentDataInt(ItemStack itemStack, NamespacedKey key, int value) {
        ItemMeta itemMeta = itemStack.getItemMeta();
        itemMeta.getPersistentDataContainer().set(key, PersistentDataType.INTEGER, value);
        itemStack.setItemMeta(itemMeta);
    }

}
