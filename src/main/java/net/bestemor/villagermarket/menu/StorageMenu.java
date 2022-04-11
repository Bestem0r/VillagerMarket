package net.bestemor.villagermarket.menu;

import net.bestemor.core.config.ConfigManager;
import net.bestemor.core.menu.Clickable;
import net.bestemor.core.menu.Menu;
import net.bestemor.core.menu.MenuContent;
import net.bestemor.core.menu.MenuListener;
import net.bestemor.villagermarket.utils.VMUtils;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
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
    protected void onCreate(MenuContent content) {
        getInventory().setContents(items.toArray(new ItemStack[0]));
        update();
    }


    @Override
    protected void onUpdate(MenuContent content) {
        if (isInfinite) {
            content.fillBottom(ConfigManager.getItem("items.filler").build());

            if (page != 0) {
                content.setClickable(48, Clickable.of(ConfigManager.getItem("items.previous").build(), event -> {
                    Player player = (Player) event.getWhoClicked();
                    player.playSound(player.getLocation(), ConfigManager.getSound("sounds.menu_click"), 0.5f, 1);
                    didChangePage = true;
                    holder.open(player, page - 1);
                }));
            }
            if (page != holder.getPages() - 1) {
                content.setClickable(50, Clickable.of(ConfigManager.getItem("items.next").build(), event -> {
                    Player player = (Player) event.getWhoClicked();
                    player.playSound(player.getLocation(), ConfigManager.getSound("sounds.menu_click"), 0.5f, 1);
                    didChangePage = true;
                    holder.open(player, page + 1);
                }));
            }
        }
        content.setClickable(inventorySize - 1, Clickable.of(ConfigManager.getItem("items.back").build(), event -> {
            Player player = (Player) event.getWhoClicked();
            this.holder.back(player);
            player.playSound(player.getLocation(), ConfigManager.getSound("sounds.menu_click"), 0.5f, 1);
        }));
    }

    @Override
    protected void onClose(InventoryCloseEvent event) {
        if (!didChangePage) {
            holder.close();
        }
        didChangePage = false;
        holder.getShop().getShopfrontHolder().update();
    }

    @Override
    protected void onClick(InventoryClickEvent event) {
        int size = event.getView().getTopInventory().getSize();
        if (isInfinite ? (event.getRawSlot() < size - 9 || event.getRawSlot() > size) : (event.getRawSlot() != size - 1)) {
            event.setCancelled(false);
        }
    }

    public void setItems(List<ItemStack> items) {
        int start = Math.min(items.size(), page * itemsSize);
        int end = Math.min(items.size(), (page + 1) * itemsSize);
        this.items = items.subList(start, end);
        create();
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
