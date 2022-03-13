package net.bestemor.villagermarket.menu;

import net.bestemor.villagermarket.ConfigManager;
import net.bestemor.villagermarket.utils.VMUtils;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class StorageMenu extends Menu {

    private final StorageHolder holder;

    private final boolean isInfinite;
    private final int inventorySize;
    private final int itemsSize;
    private final int page;

    private boolean didChangePage = false;

    private List<ItemStack> items = new ArrayList<>();

    public StorageMenu(MenuListener listener, StorageHolder holder, int size, int page) {
        super(listener, size == 0 ? 54 : size, ConfigManager.getString("menus.storage.title") + (size == 0 ? " | " + (page + 1) : ""));
        this.holder = holder;

        this.page = page;
        this.isInfinite = size == 0;
        this.inventorySize = size == 0 ? 54 : size;
        this.itemsSize = isInfinite ? 45 : inventorySize - 1;
    }

    @Override
    protected void create(Inventory inventory) {
        inventory.setContents(items.toArray(new ItemStack[0]));
        createBottom(inventory);
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();

        ItemStack current = event.getCurrentItem();

        if (holder.getShop() != null) {
            holder.getShop().getShopfrontHolder().update();
        }

        if (current == null) { return; }

        if (isInfinite && event.getRawSlot() < 54 && event.getRawSlot() > 44) {
            event.setCancelled(true);
        }

        if (event.getRawSlot() == inventorySize - 1) {
            this.holder.back(player);
            player.playSound(player.getLocation(), ConfigManager.getSound("sounds.menu_click"), 0.5f, 1);
            event.setCancelled(true);
            return;
        }

        if (isInfinite && event.getRawSlot() == 48 && page != 0) {
            player.playSound(player.getLocation(), ConfigManager.getSound("sounds.menu_click"), 0.5f, 1);
            didChangePage = true;
            holder.open(player, page - 1);
            return;
        }
        if (isInfinite && event.getRawSlot() == 50 && page != holder.getPages() - 1) {
            player.playSound(player.getLocation(), ConfigManager.getSound("sounds.menu_click"), 0.5f, 1);
            didChangePage = true;
            holder.open(player, page + 1);
        }


    }

    @Override
    protected void update(Inventory inventory) {
        createBottom(inventory);
    }

    @Override
    protected void onClose(InventoryCloseEvent event) {
        if (!didChangePage) {
            holder.close();
        }
        didChangePage = false;
    }

    private void createBottom(Inventory inventory) {
        if (isInfinite) {
            fillBottom(ConfigManager.getItem("items.filler").build());

            if (page != 0) {
                inventory.setItem(48, ConfigManager.getItem("items.previous").build());
            }
            if (page != holder.getPages() - 1) {
                inventory.setItem(50, ConfigManager.getItem("items.next").build());
            }
        }
        inventory.setItem(inventorySize - 1, ConfigManager.getItem("items.back").build());
    }

    public void setItems(List<ItemStack> items) {
        int start = Math.min(items.size(), page * itemsSize);
        int end = Math.min(items.size(), (page + 1) * itemsSize);
        this.items = items.subList(start, end);
        create(getInventory());
    }

    public int getAmount(ItemStack i) {
        int sum = 0;
        for (int slot = 0; slot < itemsSize && slot < getInventory().getSize(); slot ++) {
            ItemStack storageItem = getInventory().getItem(slot);
            if (VMUtils.compareItems(storageItem, i)) {
                sum += storageItem.getAmount();
            }
        }
        return sum;
    }

    public int getAvailableSpace(ItemStack i) {
        int inStorage = getAmount(i);
        int availableSlots = 0;
        if (getInventory() == null) {
            return 0;
        }
        for (int slot = 0; slot < itemsSize && slot < getInventory().getSize(); slot ++) {
            ItemStack storageItem = getInventory().getItem(slot);
            if (storageItem == null || VMUtils.compareItems(storageItem, i)) {
                availableSlots ++;
            }
        }

        return availableSlots * i.getType().getMaxStackSize() - inStorage;
    }

    public boolean isEmpty() {
        return isInfinite && Arrays.stream(getInventory().getContents()).filter(Objects::isNull).count() > 44;
    }

    public List<ItemStack> getItems() {
        List<ItemStack> items = new ArrayList<>();
        for (int slot = 0; slot < itemsSize && slot < getInventory().getSize(); slot ++) {
            items.add(getInventory().getItem(slot));
        }
        items.removeIf(Objects::isNull);
        return items;
    }

    public void addItem(ItemStack i) {
        getInventory().addItem(i);
    }
}
