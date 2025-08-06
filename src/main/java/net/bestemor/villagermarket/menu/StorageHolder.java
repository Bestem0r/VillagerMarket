package net.bestemor.villagermarket.menu;

import net.bestemor.villagermarket.VMPlugin;
import net.bestemor.villagermarket.shop.PlayerShop;
import net.bestemor.villagermarket.shop.ShopMenu;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class StorageHolder {

    private final VMPlugin plugin;
    private final PlayerShop shop;

    private final List<StorageMenu> storageMenus = new ArrayList<>();
    private Runnable clickEvent;

    private final int size;
    private final boolean isInfinite;
    private boolean addAllowed = true;

    public StorageHolder(VMPlugin plugin, PlayerShop shop) {
        this.plugin = plugin;
        this.shop = shop;
        this.size = shop.getStorageSize();
        this.isInfinite = this.size == 0;
    }

    public StorageHolder(VMPlugin plugin, int size) {
        this.plugin = plugin;
        this.shop = null;
        this.size = size;
        this.isInfinite = size == 0;
    }

    private void reload() {
        if (isInfinite) {
            if (!storageMenus.get(storageMenus.size() - 1).isEmpty()) {
                storageMenus.add(new StorageMenu(this, size, storageMenus.size()));
            }
        }
        storageMenus.forEach(StorageMenu::update);
    }

    public void loadItems(List<ItemStack> items) {
        storageMenus.clear();
        if (isInfinite) {
            int pages = (int) (Math.ceil(items.size() / 45d)) + 1;
            for (int page = 0; page < pages; page ++) {
                storageMenus.add(new StorageMenu(this, size, page));
            }
        } else {
            storageMenus.add(new StorageMenu(this, size, 0));
        }
        storageMenus.forEach(s -> s.setItems(items));
    }

    protected void back(Player player) {
        if (shop != null) {
            shop.openInventory(player, ShopMenu.EDIT_SHOP);
        } else {
            player.closeInventory();
        }
    }

    protected void onClick() {
        if (clickEvent != null) {
            clickEvent.run();
        }
    }

    public void setClickEvent(Runnable closeEvent) {
        this.clickEvent = closeEvent;
    }

    public void open(Player player) {
        reload();
        storageMenus.get(0).open(player);
    }

    public void open(Player player, int page) {
        storageMenus.get(page).open(player);
    }

    public int getAmount(ItemStack i) {
        return storageMenus.stream().mapToInt(s -> s.getAmount(i)).sum();
    }
    public PlayerShop getShop() {
        return shop;
    }

    public int getPages() {
        return storageMenus.size();
    }

    public List<ItemStack> getItems() {
        List<ItemStack> items = new ArrayList<>();
        for (StorageMenu menu : storageMenus) {
            items.addAll(menu.getItems());
        }
        return items;
    }

    public void clear() {
        loadItems(new ArrayList<>());
    }

    public int getAvailableSpace(ItemStack i) {
        return storageMenus.stream().mapToInt(s -> s.getAvailableSpace(i)).sum();
    }

    public void removeItem(ItemStack i, int amount) {
        int amountLeft = amount;
        for (StorageMenu menu : storageMenus) {
            amountLeft -= menu.removeItem(i, amountLeft);
            if (amountLeft <= 0) {
                break;
            }
        }
    }

    public void addItem(ItemStack i, int amount) {
        for (StorageMenu storage : storageMenus) {
            int available = storage.getAvailableSpace(i);
            if (available >= amount) {
                storage.addItem(i, amount);
                break;
            }
        }
    }

    public void setAddingAllowed(boolean b) {
        this.addAllowed = b;
    }

    public boolean isAddAllowed() {
        return addAllowed;
    }

    public void closeAll() {
        for (StorageMenu storageMenu : storageMenus) {
            storageMenu.close();
        }
    }
}
