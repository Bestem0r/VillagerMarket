package net.bestemor.villagermarket.menu;

import net.bestemor.villagermarket.ConfigManager;
import net.bestemor.villagermarket.VMPlugin;
import me.bestem0r.villagermarket.shop.*;
import net.bestemor.villagermarket.shop.*;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.*;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class Shopfront {

    public enum Type {
        EDITOR,
        CUSTOMER,
        DETAILED
    }

    private final ShopfrontHolder holder;

    private final Inventory editorInventory;
    private final Map<UUID, Inventory> customerInventories = new ConcurrentHashMap<>();
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

    private final Map<Integer, ShopItem> items = new ConcurrentHashMap<>();

    public Shopfront(VMPlugin plugin, ShopfrontHolder holder, VillagerShop shop, int page) {
        this.plugin = plugin;
        this.holder = holder;
        this.shop = shop;
        this.page = page;
        this.isInfinite = shop.getShopSize() == 0;
        this.size = (isInfinite ? 54 : shop.getShopSize());

        String editorTitle = ConfigManager.getString("menus.edit_shopfront.title");
        String detailedTitle =  ConfigManager.getString("menus.shopfront.title") + " " + ConfigManager.getString("menus.shopfront.detail_suffix");

        if (isInfinite) {
            editorTitle += " | " + (page + 1);
            detailedTitle += " | " + (page + 1);
        }

        this.editorInventory = Bukkit.createInventory(null, size, editorTitle);
        this.detailedInventory = Bukkit.createInventory(null, size, detailedTitle);

        loadItemsFromConfig();

        loadItems();
        update();
    }

    public void loadItemsFromConfig() {
        this.back = ConfigManager.getItem("items.back").build();
        this.details = ConfigManager.getItem("menus.shopfront.items.toggle_details").build();
        this.filler = ConfigManager.getItem("items.filler").build();
        this.next = ConfigManager.getItem("items.next").build();
        this.previous = ConfigManager.getItem("items.previous").build();
    }

    private void loadItems() {
        items.clear();
        if (isInfinite) {
            int start = page * 45;
            int end = start + 44;
            for (int slot : shop.getShopfrontHolder().getItemList().keySet()) {
                if (slot < start || slot > end) { continue; }
                items.put(slot - start, shop.getShopfrontHolder().getItemList().get(slot));
            }
        } else {
            items.putAll(shop.getShopfrontHolder().getItemList());
        }
        Collection<ShopItem> shopItems = Collections.synchronizedCollection(items.values());
        try {
            for (ShopItem shopItem : shopItems) {
                shopItem.reloadMeta(shop);
            }
        } catch (ConcurrentModificationException ignore) {}

    }

    public void update() {
        loadItems();

        for (UUID uuid : customerInventories.keySet()) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null && customerInventories.get(uuid) != null) {
                customerInventories.get(uuid).setContents(getCustomerInventory(p).getContents());
            }
        }
        updateDetailedInventory();
        updateEditorInventory();
    }

    private Inventory getCustomerInventory(Player player) {
        String customerTitle = ConfigManager.getString("menus.shopfront.title");
        if (isInfinite) {
            customerTitle += " | " + (page + 1);
        }

        Inventory customerInventory = Bukkit.createInventory(null, size, customerTitle);
        for (Integer slot : items.keySet()) {
            ShopItem item = items.get(slot);
            if (item == null) {
                continue;
            }
            customerInventory.setItem(slot, item.getCustomerItem(player));
        }
        buildBottom(customerInventory);
        customerInventory.setItem(size - 1, details);

        return customerInventory;
    }
    private void updateDetailedInventory() {
        detailedInventory.clear();
        for (Integer slot : items.keySet()) {
            ShopItem item = items.get(slot);
            if (item == null) {
                continue;
            }
            detailedInventory.setItem(slot, item.getRawItem());
        }
        buildBottom(detailedInventory);
        detailedInventory.setItem(size - 1, details);
    }
    private void updateEditorInventory() {
        editorInventory.clear();
        for (Integer slot : items.keySet()) {
            ShopItem item = items.get(slot);
            if (item == null) {
                continue;
            }
            editorInventory.setItem(slot, item.getEditorItem());
        }
        buildBottom(editorInventory);
        editorInventory.setItem(size - 1, back);
    }

    public void open(Player player, Type type) {
        switch (type) {
            case EDITOR:
                player.openInventory(editorInventory);
                break;
            case CUSTOMER:
                Inventory i = getCustomerInventory(player);
                player.openInventory(i);
                customerInventories.put(player.getUniqueId(), i);
                break;
            case DETAILED:
                player.openInventory(detailedInventory);
        }
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            Bukkit.getPluginManager().registerEvents(new ClickListener(player, type), plugin);
        }, 1L);
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

    public void close() {
        editorInventory.getViewers().forEach(HumanEntity::closeInventory);
        detailedInventory.getViewers().forEach(HumanEntity::closeInventory);
        for (Inventory i : customerInventories.values()) {
            i.getViewers().forEach(HumanEntity::closeInventory);
        }
    }

    private class ClickListener implements Listener {

        private Instant nextClick = Instant.now();
        private final Player player;
        private final Type type;

        public ClickListener(Player player, Type type) {
            this.player = player;
            this.type = type;
        }

        @EventHandler
        public void onClick(InventoryClickEvent event) {
            if (this.player != event.getWhoClicked()) { return; }
            if (event.getRawSlot() < 0) { return; }

            ItemStack current = event.getCurrentItem();

            if (isInfinite && event.getRawSlot() == 50 && page + 1 < shop.getShopfrontHolder().getSize()) {
                holder.open(player, type, page + 1);
                player.playSound(player.getLocation(), ConfigManager.getSound("sounds.menu_click"), 0.5f, 1);
                return;
            }
            if (isInfinite && event.getRawSlot() == 48 && page != 0) {
                holder.open(player, type, page - 1);
                player.playSound(player.getLocation(), ConfigManager.getSound("sounds.menu_click"), 0.5f, 1);
                return;
            }

            if (event.getRawSlot() == event.getView().getTopInventory().getSize() - 1) {
                event.setCancelled(true);
                boolean owner;
                if (shop instanceof PlayerShop){
                    PlayerShop playerShop = (PlayerShop) shop;
                    owner = playerShop.hasOwner() && playerShop.getOwnerUUID().equals(player.getUniqueId());
                } else {
                    owner = player.hasPermission("villagermarket.admin");
                }

                player.playSound(player.getLocation(), ConfigManager.getSound("sounds.menu_click"), 0.5f, 1);

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
            if (isInfinite && event.getRawSlot() > 44 && event.getRawSlot() < 54) {
                event.setCancelled(true);
                return;
            }

            if (event.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY) {
                event.setCancelled(true);
            }
            if (event.getRawSlot() < event.getView().getTopInventory().getSize()) {
                event.setCancelled(true);
                switch (type) {
                    case EDITOR:

                        int slot = event.getSlot() + page * 45;
                        ItemStack cursor = event.getCursor();
                        
                        if (current == null && cursor != null && cursor.getType() != Material.AIR) {
                            if (plugin.getShopManager().isBlackListed(cursor.getType())) {
                                player.sendMessage(ConfigManager.getMessage("messages.blacklisted"));

                            } else {
                                createShopItem(cursor, slot);
                            }
                            event.getView().close();
                        } else {
                            ShopItem shopItem = shop.getShopfrontHolder().getItemList().get(slot);
                            if (shopItem != null) {
                                player.playSound(player.getLocation(), ConfigManager.getSound("sounds.menu_click"), 0.5f, 1);
                                shopItem.openEditor(player, shop);
                            }
                        }
                        
                        break;
                    case CUSTOMER:
                        if (Instant.now().isBefore(nextClick)) {
                            player.sendMessage(ConfigManager.getMessage("messages.shopfront_cooldown"));
                            return;
                        }
                        shop.customerInteract(event, event.getSlot() + page * 45);
                        this.nextClick = Instant.now().plusMillis(plugin.getConfig().getInt("menus.shopfront.cooldown"));
                        break;
                    case DETAILED:
                        if (event.isCancelled() && event.getCurrentItem() != null) {
                            player.sendMessage(ConfigManager.getMessage("messages.must_be_menulore"));
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
            customerInventories.remove(player.getUniqueId());

            HandlerList.unregisterAll(this);
        }

        private void createShopItem(ItemStack i, int slot) {

            player.sendMessage(ConfigManager.getMessage("messages.type_amount"));
            ShopItem shopItem = new ShopItem(plugin, i.clone(), slot);
            shopItem.setAdmin(shop instanceof AdminShop);

            plugin.getChatListener().addDecimalListener(player, (amount) -> {
                if (amount.intValue() > 64 || amount.intValue() < 1) {
                    player.sendMessage(ConfigManager.getMessage("messages.not_valid_range"));
                    return;
                }
                shopItem.setAmount(amount.intValue());

                player.sendMessage(ConfigManager.getMessage("messages.amount_successful"));
                player.sendMessage(ConfigManager.getMessage("messages.type_price"));

                plugin.getChatListener().addDecimalListener(player, (price) -> {

                    BigDecimal maxPrice = BigDecimal.valueOf(ConfigManager.getDouble("max_item_price"));
                    if (!player.hasPermission("villagermarket.bypass_price") && maxPrice.doubleValue() != 0 && price.compareTo(maxPrice) > 0) {
                        player.sendMessage(ConfigManager.getMessage("messages.max_item_price"));
                        return;
                    }

                    shopItem.setPrice(price);

                    shop.getShopfrontHolder().getItemList().put(shopItem.getSlot(), shopItem);
                    Bukkit.getScheduler().runTaskAsynchronously(plugin, Shopfront.this::update);

                    player.sendMessage(ConfigManager.getMessage("messages.add_successful"));

                    open(player, Type.EDITOR);
                    player.playSound(player.getLocation(), ConfigManager.getSound("sounds.add_item"), 0.5f, 1);
                });
            });
        }
    }
}
