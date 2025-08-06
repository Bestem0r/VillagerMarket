package net.bestemor.villagermarket.menu;

import net.bestemor.core.config.ConfigManager;
import net.bestemor.villagermarket.VMPlugin;
import net.bestemor.villagermarket.event.interact.CreateShopItemsEvent;
import net.bestemor.villagermarket.shop.*;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.PlayerDropItemEvent;
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

    private Inventory editorInventory;
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

        this.editorInventory = Bukkit.createInventory(null, size, getEditorTitle());
        this.detailedInventory = Bukkit.createInventory(null, size, getDetailedTitle());

        loadItemsFromConfig();

        loadItems();
    }

    private String getEditorTitle() {
        String editorTitle = ConfigManager.getString("menus.edit_shopfront.title").replace("%shop%", shop.getShopName());
        if (isInfinite) {
            editorTitle += " | " + (page + 1);
        }
        return editorTitle;
    }

    private String getDetailedTitle() {
        String detailedTitle = (ConfigManager.getString("menus.shopfront.title") + " " + ConfigManager.getString("menus.shopfront.detail_suffix"))
                .replace("%shop%", shop.getShopName());
        if (isInfinite) {
            detailedTitle += " | " + (page + 1);
        }
        return detailedTitle;
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
                if (slot < start || slot > end) {
                    continue;
                }
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
        } catch (ConcurrentModificationException ignore) {
        }

    }

    public void update() {
        loadItems();

        for (UUID uuid : customerInventories.keySet()) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null && customerInventories.get(uuid) != null) {
                Bukkit.getScheduler().runTask(plugin, () -> {
                    if (customerInventories.containsKey(uuid)) {
                        customerInventories.get(uuid).setContents(getCustomerInventory(p).getContents());
                    }
                });
            }
        }
        if (!ConfigManager.getBoolean("disable_lore_toggle")) {
            updateDetailedInventory();
        }
        updateEditorInventory();
    }

    private Inventory getCustomerInventory(Player player) {
        String customerTitle = ConfigManager.getString("menus.shopfront.title").replace("%shop%", shop.getShopName());
        if (isInfinite) {
            customerTitle += " | " + (page + 1);
        }

        Inventory customerInventory = Bukkit.createInventory(null, size, customerTitle);
        for (Integer slot : items.keySet()) {
            ShopItem item = items.get(slot);
            if (item == null) {
                continue;
            }
            customerInventory.setItem(slot, item.getCustomerItem(player, item.getAmount()));
        }
        buildBottom(customerInventory);
        if (!ConfigManager.getBoolean("disable_lore_toggle")) {
            customerInventory.setItem(size - 1, details);
        }

        return customerInventory;
    }

    private void updateDetailedInventory() {
        detailedInventory.clear();
        for (Integer slot : items.keySet()) {
            ShopItem item = items.get(slot);
            if (item == null) {
                continue;
            }
            if (detailedInventory.getSize() <= slot) {
                continue;
            }
            detailedInventory.setItem(slot, item.getRawItem());
        }
        buildBottom(detailedInventory);
        if (!ConfigManager.getBoolean("disable_lore_toggle")) {
            detailedInventory.setItem(size - 1, details);
        }
    }

    private void updateEditorInventory() {
        editorInventory = Bukkit.createInventory(null, size, getEditorTitle());
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
            for (int i = inventory.getSize() - 9; i < inventory.getSize(); i++) {
                inventory.setItem(i, filler);
            }
            if (page != 0) {
                inventory.setItem(48, previous);
            }
            if (page < holder.getSize() - 1) {
                inventory.setItem(50, next);
            }
        }
    }

    public void close() {
        List<HumanEntity> editorViewers = new ArrayList<>(editorInventory.getViewers());
        editorViewers.forEach(HumanEntity::closeInventory);

        List<HumanEntity> detailedViewers = new ArrayList<>(detailedInventory.getViewers());
        detailedViewers.forEach(HumanEntity::closeInventory);

        for (Inventory i : customerInventories.values()) {
            List<HumanEntity> customerViewers = new ArrayList<>(i.getViewers());
            customerViewers.forEach(HumanEntity::closeInventory);
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

        @SuppressWarnings("unused")
        @EventHandler
        public void onClick(InventoryClickEvent event) {
            if (this.player != event.getWhoClicked()) {
                return;
            }
            if (event.getRawSlot() < 0) {
                return;
            }

            if (Instant.now().isBefore(nextClick)) {
                player.sendMessage(ConfigManager.getMessage("messages.shopfront_cooldown"));
                return;
            }
            this.nextClick = Instant.now().plusMillis(plugin.getConfig().getInt("menus.shopfront.cooldown"));

            if (type == Type.DETAILED || type == Type.CUSTOMER) {
                event.setCancelled(true);
            }
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
                if (shop instanceof PlayerShop playerShop) {
                    owner = playerShop.hasOwner() && playerShop.getOwnerUUID().equals(player.getUniqueId());
                } else {
                    owner = player.hasPermission("villagermarket.admin");
                }

                player.playSound(player.getLocation(), ConfigManager.getSound("sounds.back"), 0.5f, 1);

                if (type == Type.EDITOR) {
                    shop.openInventory(player, ShopMenu.EDIT_SHOP);
                } else if (!ConfigManager.getBoolean("disable_lore_toggle")) {
                    if (event.getClick() == ClickType.RIGHT && owner) {
                        shop.openInventory(player, ShopMenu.EDIT_SHOP);
                    } else if (event.getClick() == ClickType.RIGHT) {
                        event.getView().close();
                    } else {
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

                int slot = event.getSlot() + page * 45;
                ShopItem shopItem = shop.getShopfrontHolder().getItemList().get(slot);

                switch (type) {
                    case EDITOR:

                        ItemStack cursor = event.getCursor();

                        if (cursor != null && cursor.getType() != Material.AIR) {
                            if (plugin.getShopManager().isBlackListed(cursor.getType())) {
                                player.sendMessage(ConfigManager.getMessage("messages.blacklisted"));

                            } else {
                                createShopItem(cursor, slot);
                            }
                            DropListener dropListener = new DropListener(player);
                            Bukkit.getPluginManager().registerEvents(dropListener, plugin);
                            Bukkit.getScheduler().runTaskLater(plugin, () -> HandlerList.unregisterAll(dropListener), 10L);

                            event.getView().close();
                        } else {
                            if (shopItem != null) {
                                player.playSound(player.getLocation(), ConfigManager.getSound("sounds.menu_click"), 0.5f, 1);
                                shopItem.openEditor(player, shop, page);
                            }
                        }
                        break;
                    case CUSTOMER:
                        ItemMode mode = shopItem.getMode();
                        if (mode == ItemMode.BUY_AND_SELL) {
                            mode = ItemMode.BUY;
                        }
                        BuyItemMenu buyItemMenu = new BuyItemMenu(shopItem, mode.inverted(), player, page);
                        buyItemMenu.open(player);
                        break;
                    case DETAILED:
                        if (event.isCancelled() && event.getCurrentItem() != null) {
                            player.sendMessage(ConfigManager.getMessage("messages.must_be_menulore"));
                        }
                }
            }
        }

        @SuppressWarnings("unused")
        @EventHandler
        public void onDrag(InventoryDragEvent event) {
            if (event.getWhoClicked() != this.player) {
                return;
            }
            event.setCancelled(true);
        }

        @SuppressWarnings("unused")
        @EventHandler
        public void onClose(InventoryCloseEvent event) {
            if (this.player != event.getPlayer()) {
                return;
            }
            customerInventories.remove(player.getUniqueId());

            HandlerList.unregisterAll(this);
        }

        private void createShopItem(ItemStack i, int slot) {

            player.sendMessage(ConfigManager.getMessage("messages.type_amount"));
            ShopItem shopItem = new ShopItem(plugin, shop, i.clone(), slot);
            shopItem.setAdmin(shop instanceof AdminShop);

            plugin.getChatListener().addDecimalListener(player, (amount) -> {
                if (amount.intValue() > ConfigManager.getInt("max_sell_amount") || amount.intValue() < 1) {
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

                    shopItem.setSellPrice(price);

                    shop.getShopfrontHolder().addItem(shopItem.getSlot(), shopItem);
                    update();

                    player.sendMessage(ConfigManager.getMessage("messages.add_successful"));

                    open(player, Type.EDITOR);
                    player.playSound(player.getLocation(), ConfigManager.getSound("sounds.add_item"), 0.5f, 1);
                    CreateShopItemsEvent createShopItemsEvent = new CreateShopItemsEvent(player, shop, shopItem);
                    Bukkit.getPluginManager().callEvent(createShopItemsEvent);

                });
            });
        }
    }

    private record DropListener(Player player) implements Listener {

        @SuppressWarnings("unused")
        @EventHandler
        public void onDrop(PlayerDropItemEvent event) {
            if (event.getPlayer() != player) {
                return;
            }
            event.setCancelled(true);
        }
    }
}
