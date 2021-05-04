package me.bestem0r.villagermarket.inventories;

import me.bestem0r.villagermarket.VMPlugin;
import me.bestem0r.villagermarket.items.ShopItem;
import me.bestem0r.villagermarket.shops.AdminShop;
import me.bestem0r.villagermarket.shops.ShopMenu;
import me.bestem0r.villagermarket.shops.VillagerShop;
import me.bestem0r.villagermarket.utilities.ColorBuilder;
import me.bestem0r.villagermarket.utilities.Methods;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.*;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.HashMap;

public class Shopfront {

    public enum Type {
        EDITOR,
        CUSTOMER,
        DETAILED
    }

    private final ShopfrontHolder holder;

    private final Inventory editorInventory;
    private final Inventory customerInventory;
    private final Inventory detailedInventory;

    private ItemStack back;
    private ItemStack filler;
    private ItemStack next;
    private ItemStack previous;
    private ItemStack details;

    private final VMPlugin plugin;
    private final VillagerShop shop;
    private final int page;
    private final boolean isInfinite;
    private final int size;

    private final NamespacedKey guiKey;

    private final HashMap<Integer, ShopItem> items = new HashMap<>();

    public Shopfront(VMPlugin plugin, ShopfrontHolder holder, VillagerShop shop, int page) {
        this.plugin = plugin;
        this.holder = holder;
        this.shop = shop;
        this.page = page;
        this.isInfinite = shop.getShopSize() == 0;
        this.size = (isInfinite ? 54 : shop.getShopSize());

        guiKey = new NamespacedKey(plugin, "vm-gui-item");

        String editorTitle = new ColorBuilder(plugin).path("menus.edit_shopfront.title").build();
        String customerTitle = new ColorBuilder(plugin).path("menus.shopfront.title").build();
        String detailedTitle = customerTitle + " " + new ColorBuilder(plugin).path("menus.shopfront.detail_suffix").build();

        if (isInfinite) {
            editorTitle += " | " + (page + 1);
            customerTitle += " | " + (page + 1);
            detailedTitle += " | " + (page + 1);
        }

        this.editorInventory = Bukkit.createInventory(null, size, editorTitle);
        this.customerInventory = Bukkit.createInventory(null, size, customerTitle);
        this.detailedInventory = Bukkit.createInventory(null, size, detailedTitle);

        loadItemsFromConfig();
        loadItems();
        update();
    }

    public void loadItemsFromConfig() {
        this.back = Methods.stackFromPath(plugin, "items.back");
        this.details = Methods.stackFromPath(plugin, "items.toggle_details");
        this.filler = Methods.stackFromPath(plugin, "items.filler");
        setPersistentDataString(filler, "filler");
        this.next = Methods.stackFromPath(plugin, "menus.storage.items.next");
        setPersistentDataInt(next, page + 1);
        this.previous = Methods.stackFromPath(plugin, "menus.storage.items.previous");
        setPersistentDataInt(previous, page - 1);
    }

    private void loadItems() {
        items.clear();
        if (isInfinite) {
            int start = page * 45;
            int end = start + 44;
            for (int slot : shop.getItemList().keySet()) {
                if (slot < start || slot > end) { continue; }
                items.put(slot - start, shop.getItemList().get(slot));
            }
        } else {
            items.putAll(shop.getItemList());
        }
    }

    public void update() {
        loadItems();
        updateCustomerInventory();
        updateDetailedInventory();
        updateEditorInventory();
    }

    private void updateCustomerInventory() {
        customerInventory.clear();
        for (Integer slot : items.keySet()) {
            ShopItem item = (ShopItem) items.get(slot).clone();
            item.refreshLore(shop);
            customerInventory.setItem(slot, item.asItemStack(ShopItem.LoreType.MENU));
        }
        buildBottom(customerInventory);
        customerInventory.setItem(size - 1, details);
    }
    private void updateDetailedInventory() {
        detailedInventory.clear();
        for (Integer slot : items.keySet()) {
            ShopItem item = (ShopItem) items.get(slot).clone();
            detailedInventory.setItem(slot, item.asItemStack(ShopItem.LoreType.ITEM));
        }
        buildBottom(detailedInventory);
        detailedInventory.setItem(size - 1, details);
    }
    private void updateEditorInventory() {
        editorInventory.clear();
        for (Integer slot : items.keySet()) {
            ShopItem item = (ShopItem) items.get(slot).clone();
            item.toggleEditor(true);
            item.refreshLore(shop);
            editorInventory.setItem(slot, item.asItemStack(ShopItem.LoreType.MENU));
        }
        buildBottom(editorInventory);
        editorInventory.setItem(size - 1, back);
    }

    public void addNext() {
        customerInventory.setItem(50, next);
        detailedInventory.setItem(50, next);
        editorInventory.setItem(50, next);
    }
    public void removeNext() {
        customerInventory.setItem(50, filler);
        detailedInventory.setItem(50, filler);
        editorInventory.setItem(50, filler);
    }

    public void open(Player player, Type type) {
        switch (type) {
            case EDITOR:
                player.openInventory(editorInventory);
                break;
            case CUSTOMER:
                player.openInventory(customerInventory);
                break;
            case DETAILED:
                player.openInventory(detailedInventory);
        }
        Bukkit.getPluginManager().registerEvents(new ClickListener(player, type), plugin);
    }

    private void buildBottom(Inventory inventory) {
        if (isInfinite) {
            for (int i = inventory.getSize() - 9; i < inventory.getSize(); i ++) {
                inventory.setItem(i, filler);
            }
            if (page != 0) {
                inventory.setItem(48, previous);
            }
            if (page != holder.getSize() - 1) {
                inventory.setItem(50, next);
            }
        }
    }

    private void setPersistentDataString(ItemStack itemStack, String value) {
        ItemMeta itemMeta = itemStack.getItemMeta();
        itemMeta.getPersistentDataContainer().set(guiKey, PersistentDataType.STRING, value);
        itemStack.setItemMeta(itemMeta);
    }

    private void setPersistentDataInt(ItemStack itemStack, int value) {
        ItemMeta itemMeta = itemStack.getItemMeta();
        itemMeta.getPersistentDataContainer().set(guiKey, PersistentDataType.INTEGER, value);
        itemStack.setItemMeta(itemMeta);
    }

    private class ClickListener implements Listener {

        private final Player player;
        private final Type type;

        public ClickListener(Player player, Type type) {
            this.player = player;
            this.type = type;
        }

        @EventHandler
        public void onClick(InventoryClickEvent event) {
            if (this.player != event.getWhoClicked()) { return; }
            if (event.getRawSlot() == -1) { return; }

            ItemStack current = event.getCurrentItem();

            if (current != null && current.getItemMeta().getPersistentDataContainer().has(guiKey, PersistentDataType.INTEGER)) {

                Integer page = current.getItemMeta().getPersistentDataContainer().get(guiKey, PersistentDataType.INTEGER);
                holder.open(player, type, page);
                player.playSound(player.getLocation(), Sound.valueOf(plugin.getConfig().getString("sounds.menu_click")), 1, 1);

                event.setCancelled(true);
                return;
            }
            if (current != null && current.getItemMeta().getPersistentDataContainer().has(guiKey, PersistentDataType.STRING)) {
                event.setCancelled(true);
                return;
            }

            if (event.getRawSlot() == event.getView().getTopInventory().getSize() - 1) {
                event.setCancelled(true);
                boolean owner = shop.getOwnerUUID().equals(player.getUniqueId().toString()) || (shop instanceof AdminShop && player.hasPermission("villagermarket.admin"));

                player.playSound(player.getLocation(), Sound.valueOf(plugin.getConfig().getString("sounds.menu_click")), 0.5f, 1);

                if (type == Type.EDITOR) {
                    shop.openInventory(player, ShopMenu.EDIT_SHOP);
                } else {
                    if (event.getClick() == ClickType.RIGHT && owner) {
                        shop.openInventory(player, ShopMenu.EDIT_SHOP);
                    } else if (event.getClick() == ClickType.RIGHT) {
                        event.getView().close();
                    }
                    else {
                        open(player, (type == Type.CUSTOMER ? Type.DETAILED : Type.CUSTOMER));
                    }
                }
                return;
            }
            if (event.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY) {
                event.setCancelled(true);
            }
            if (event.getRawSlot() > -1 && event.getRawSlot() < event.getView().getTopInventory().getSize()) {
                event.setCancelled(true);
                switch (type) {
                    case EDITOR:
                        shop.editorInteract(event, event.getSlot() + page * 45);
                        break;
                    case CUSTOMER:
                        shop.customerInteract(event, event.getSlot() + page * 45);
                        break;
                    case DETAILED:
                        if (event.isCancelled() && event.getCurrentItem() != null) {
                            player.sendMessage(new ColorBuilder(plugin).path("messages.must_be_menulore").addPrefix().build());
                        }
                }
            }
        }

        @EventHandler
        public void onDrag(InventoryDragEvent event) {
            if ((Player) event.getWhoClicked() != this.player) { return; }
            event.setCancelled(true);
        }

        @EventHandler
        public void onClose(InventoryCloseEvent event) {
            if (this.player != event.getPlayer()) { return; }
            HandlerList.unregisterAll(this);
        }
    }
}
