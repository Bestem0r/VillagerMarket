package net.bestemor.villagermarket.menu;

import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public abstract class Menu {

    private final MenuListener listener;
    private final Inventory inventory;

    private final int size;
    private boolean isCreated = false;

    protected Menu(MenuListener listener, int size, String name) {
        this.size = size;
        this.listener = listener;

        this.inventory = Bukkit.createInventory(null, size, name);
    }

    protected abstract void create(Inventory inventory);
    public abstract void handleClick(InventoryClickEvent event);

    protected void onClose(InventoryCloseEvent event) {}

    public void update() {
        update(inventory);
    }

    protected void update(Inventory inventory) {
        create(inventory);
    };

    public void open(Player player) {
        if (!isCreated) {
            create(inventory);
            isCreated = true;
        }
        player.openInventory(inventory);
        this.listener.registerMenu(this);
    }

    public boolean hasPlayer(HumanEntity entity) {
        return inventory.getViewers().stream()
                .map(Entity::getUniqueId)
                .collect(Collectors.toList())
                .contains(entity.getUniqueId());
    }

    public List<HumanEntity> getViewers() {
        return inventory == null ? new ArrayList<>() : inventory.getViewers();
    }

    protected void fillSlots(ItemStack fill, int... slots) {
        if (inventory != null) {
            for (int s : slots) {
                inventory.setItem(s, fill);
            }
        }
    }

    public void close() {
        List<HumanEntity> viewers = new ArrayList<>(getViewers());
        viewers.forEach(HumanEntity::closeInventory);
    }

    protected void fillEdges(ItemStack fill) {
        if (inventory != null) {
            for (int s = 0; s < size; s++) {
                if (s < 9 || s > size - 9 || (s % 9 == 0) || ((s + 1) % 9 == 0)) {
                    inventory.setItem(s, fill);
                }
            }
        }
    }

    protected void fillBottom(ItemStack fill) {
        if (inventory != null) {
            for (int s = size - 9; s < size; s++) {
                inventory.setItem(s, fill);
            }
        }
    }

    public Inventory getInventory() {
        return inventory;
    }
}
