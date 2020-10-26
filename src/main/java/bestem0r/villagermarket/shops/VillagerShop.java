package bestem0r.villagermarket.shops;

import bestem0r.villagermarket.events.chat.SetLimit;
import bestem0r.villagermarket.utilities.ShopStats;
import bestem0r.villagermarket.VMPlugin;
import bestem0r.villagermarket.events.ItemDrop;
import bestem0r.villagermarket.events.chat.SetAmount;
import bestem0r.villagermarket.items.MenuItem;
import bestem0r.villagermarket.items.ShopItem;
import bestem0r.villagermarket.menus.BuyShopMenu;
import bestem0r.villagermarket.menus.ProfessionMenu;
import bestem0r.villagermarket.menus.SellShopMenu;
import bestem0r.villagermarket.utilities.Color;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public abstract class VillagerShop {

    protected String ownerUUID;
    protected String ownerName;
    protected String entityUUID;

    protected String duration;
    protected int seconds;
    protected int timesRented;
    protected Timestamp expireDate;

    protected int cost;

    protected HashMap<Integer, ShopItem> itemList = new HashMap<>();

    protected int shopfrontSize;
    protected int storageSize;

    protected File file;
    protected FileConfiguration config;
    protected FileConfiguration mainConfig;

    protected Inventory buyShopMenu;
    protected Inventory editShopMenu;
    protected Inventory storageMenu;
    protected Inventory shopfrontMenu;
    protected Inventory shopfrontDetailedMenu;
    protected Inventory editShopfrontMenu;
    protected Inventory editVillagerMenu;
    protected Inventory sellShopMenu;

    protected ShopStats shopStats;

    public enum VillagerType {
        ADMIN,
        PLAYER
    }

    public VillagerShop(File file) {
        this.mainConfig = VMPlugin.getInstance().getConfig();
        this.file = file;
        this.config = YamlConfiguration.loadConfiguration(file);

        this.ownerUUID = config.getString("ownerUUID");
        this.ownerName = config.getString("ownerName");
        this.entityUUID = file.getName().substring(0, file.getName().indexOf('.'));

        this.shopfrontSize = config.getInt("shopfrontSize") * 9;
        this.storageSize = config.getInt("storageSize") * 9;
        this.expireDate = new Timestamp(config.getLong("expire"));

        int size = config.getInt("size");
        if (size != 0) {
            this.shopfrontSize = size * 9;
            this.storageSize = size * 18;
        }
        this.duration = config.getString("duration");
        this.duration = (duration == null ? "infinite" : duration);
        this.timesRented = config.getInt("times_rented");
        this.timesRented = (timesRented == 0 ? 1 : timesRented);
        this.seconds = secondsFromString(duration);

        this.cost = config.getInt("cost");

        this.shopStats = new ShopStats(config);

        buildItemList();

        this.buyShopMenu = newBuyShopInventory();
        this.editShopMenu = newEditShopInventory();
        this.storageMenu = newStorageInventory();
        this.editVillagerMenu = newEditVillagerInventory();
        this.sellShopMenu = newSellShopInventory();
    }

    /** Convert duration string to seconds */
    private int secondsFromString(String string) {
        if (string.equalsIgnoreCase("infinite")) return 0;

        String unit = string.substring(string.length() - 1);
        int size = Integer.parseInt(string.substring(0, string.length() - 1));
        switch (unit) {
            case "s":
                return size;
            case "m":
                return size * 60;
            case "h":
                return size * 3600;
            case "d":
                return size * 86400;
            default:
                Bukkit.getLogger().severe("Could not convert unit: " + unit);
                return 0;
        }
    }

    abstract void buildItemList();

    /** Inventory methods */

    public Inventory getInventory(ShopMenu shopMenu) {
        switch (shopMenu) {
            case STORAGE:
                return storageMenu;
            case BUY_SHOP:
                return buyShopMenu;
            case SHOPFRONT:
                return shopfrontMenu;
            case SHOPFRONT_DETAILED:
                return shopfrontDetailedMenu;
            case EDIT_SHOP:
                return editShopMenu;
            case SELL_SHOP:
                return sellShopMenu;
            case EDIT_SHOPFRONT:
                return editShopfrontMenu;
            case EDIT_VILLAGER:
                return editVillagerMenu;
        }
        return shopfrontMenu;
    }

    /** Buy items and sell items */
    public Boolean customerInteract(int slot, Player player) {
        ShopItem shopItem = itemList.get(slot);
        if (shopItem == null) return false;
        switch (shopItem.getMode()) {
            case BUY:
                sellItem(slot, player);
                break;
            case SELL:
                buyItem(slot, player);
                break;
        }
        updateShopInventories();
        return true;
    }
    protected abstract Boolean buyItem(int slot, Player player);
    protected abstract Boolean sellItem(int slot, Player player);

    /** Interact with the edit shop menu */
    public abstract Boolean editShopInteract(Player player, InventoryClickEvent event);

    /** Change items for sale */
    public Boolean itemsInteract(Player player, InventoryClickEvent event) {
        int slot = event.getRawSlot();
        String cancel = mainConfig.getString("cancel");
        ItemStack currentItem = event.getCurrentItem();
        ItemStack cursorItem = event.getCursor();
        if (slot < shopfrontSize && currentItem == null && cursorItem.getType() != Material.AIR) {
            //Add item
            if (slot == -1) return false;
            if (isBlackListed(cursorItem.getType())) {
                player.sendMessage(new Color.Builder().path("messages.blacklisted").addPrefix().build());
            } else {
                player.sendMessage(new Color.Builder().path("messages.type_amount").addPrefix().build());
                player.sendMessage(new Color.Builder().path("messages.type_cancel").replace("%cancel%", cancel).addPrefix().build());

                ShopItem.Builder builder = new ShopItem.Builder(cursorItem)
                        .entityUUID(entityUUID)
                        .villagerType(getType())
                        .slot(slot);

                Bukkit.getServer().getPluginManager().registerEvents(new ItemDrop(player), VMPlugin.getInstance());
                Bukkit.getServer().getPluginManager().registerEvents(new SetAmount(player, builder), VMPlugin.getInstance());
            }
            event.getView().close();
            return true;
        } else if (slot < shopfrontSize) {
            event.setCancelled(true);
            //Back
            if (slot == shopfrontSize - 1) {
                player.playSound(player.getLocation(), Sound.valueOf(VMPlugin.getInstance().getConfig().getString("sounds.menu_click")), 0.5f, 1);
                player.openInventory(getInventory(ShopMenu.EDIT_SHOP));
                return true;
            }
            //Quick add or change buy limit
            if (event.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY && this instanceof PlayerShop) {
                switch (itemList.get(slot).getMode()) {
                    case SELL:
                        quickAdd(player.getInventory(), event.getRawSlot());
                        break;
                    case BUY:
                        player.sendMessage(new Color.Builder().path("messages.type_max").addPrefix().build());
                        player.sendMessage(new Color.Builder().path("messages.type_cancel").replace("%cancel%", cancel).addPrefix().build());
                        Bukkit.getPluginManager().registerEvents(new SetLimit(player, this, slot), VMPlugin.getInstance());
                        Bukkit.getScheduler().runTaskLater(VMPlugin.getInstance(), () -> { event.getView().close(); }, 1L);
                }
                return true;
            }
            //Edit
            if (event.getClick() == ClickType.MIDDLE) {
                player.sendMessage(new Color.Builder().path("messages.type_amount").addPrefix().build());
                player.sendMessage(new Color.Builder().path("messages.type_cancel").replace("%cancel%", cancel).addPrefix().build());

                ShopItem.Builder builder = new ShopItem.Builder(itemList.get(slot).asItemStack(ShopItem.LoreType.ITEM))
                        .entityUUID(entityUUID)
                        .villagerType(getType())
                        .slot(slot);
                Bukkit.getServer().getPluginManager().registerEvents(new SetAmount(player, builder), VMPlugin.getInstance());
                event.getView().close();
                return true;
            }
            //Delete item
            if (event.getClick() == ClickType.RIGHT && currentItem != null) {
                player.playSound(player.getLocation(), Sound.valueOf(VMPlugin.getInstance().getConfig().getString("sounds.remove_item")), 0.5f, 1);
                itemList.remove(slot);
                updateShopInventories();
                return true;
            }
            //Change mode
            if (event.getClick() == ClickType.LEFT && currentItem != null) {
                player.playSound(player.getLocation(), Sound.valueOf(VMPlugin.getInstance().getConfig().getString("sounds.menu_click")), 0.5f, 1);
                itemList.get(slot).toggleMode();
                updateShopInventories();
                return true;
            }
        }
        return false;
    }
    /** Add everything from inventory to shop */
    public void quickAdd(Inventory inventory, int slot) {
        ShopItem shopItem = itemList.get(slot);
        for (ItemStack inventoryStack : inventory.getContents()) {
            if (shopItem.asItemStack(ShopItem.LoreType.ITEM).isSimilar(inventoryStack)) {
                storageMenu.addItem(inventoryStack);
                inventory.remove(inventoryStack);
            }
        }
        updateShopInventories();
    }

    /** Create new buy shop inventory */
    protected Inventory newBuyShopInventory() {
        return BuyShopMenu.create(this);
    }
    /** Create new edit shop inventory */
    protected abstract Inventory newEditShopInventory();
    /** Create new EditForSale inventory*/
    protected abstract Inventory newShopfrontMenu(Boolean isEditor, ShopItem.LoreType loreType);

    /** Create new storage inventory */
    protected Inventory newStorageInventory() {
        Inventory inventory = Bukkit.createInventory(null, storageSize, new Color.Builder().path("menus.edit_storage.title").build());
        ArrayList<ItemStack> storage = (ArrayList<ItemStack>) this.config.getList("storage");
        inventory.setContents(stacksFromArray(storage));

        MenuItem back = new MenuItem.Builder(Material.valueOf(mainConfig.getString("items.back.material")))
                .nameFromPath("items.back.name")
                .build();

        inventory.setItem(storageSize - 1, back);
        return inventory;
    }
    /** Create new edit villager inventory */
    protected Inventory newEditVillagerInventory() {
        return ProfessionMenu.create();
    }
    /** Create new sell shop inventory */
    protected Inventory newSellShopInventory() {
        return SellShopMenu.create(this);
    }

    protected ItemStack[] stacksFromArray(ArrayList<ItemStack> arrayList) {
        ItemStack[] stacks = new ItemStack[arrayList.size()];
        for (int i = 0; i < arrayList.size(); i++) {
            stacks[i] = arrayList.get(i);
        }
        return stacks;
    }

    /** Sends stats message to Player */
    public void sendStats(Player player) {
        for (String line: shopStats.getStats()) {
            player.sendMessage(line);
        }
    }

    /** Save method */
    public void save() {
        config.set("ownerUUID", ownerUUID);
        config.set("ownerName", ownerName);
        config.set("expire", expireDate.getTime());
        config.set("times_rented", timesRented);

        ItemStack[] storage = storageMenu.getContents();
        storage[storage.length - 1] = null;
        config.set("storage", storage);

        List<ItemStack> itemStackList = new ArrayList<>();
        List<Double> priceList = new ArrayList<>();
        List<String> modeList = new ArrayList<>();
        List<Integer> maxList = new ArrayList<>();

        for (Integer slot : itemList.keySet()) {
            if (itemList.get(slot) == null) {
                itemStackList.add(null);
                priceList.add(0.0);
                modeList.add("SELL");
                maxList.add(0);
            } else {
                itemStackList.add(itemList.get(slot).asItemStack(ShopItem.LoreType.ITEM));
                priceList.add(itemList.get(slot).getPrice());
                modeList.add(itemList.get(slot).getMode().toString());
                maxList.add(itemList.get(slot).getBuyLimit());
            }
        }
        config.set("prices", priceList);
        config.set("for_sale", itemStackList);
        config.set("modes", modeList);
        config.set("max_buy", maxList);

        config.set("stats.items_sold", shopStats.getItemsSold());
        config.set("stats.items_bought", shopStats.getItemsBought());
        config.set("stats.money_earned", shopStats.getMoneyEarned());
        config.set("stats.money_spent", shopStats.getMoneySpent());
        try {
            config.save(file);
        } catch (IOException i) {}
    }

    /** Remove ItemStack from stock method */
    public void removeFromStock(ItemStack itemStack) {
        storageMenu.removeItem(itemStack);
        updateShopInventories();
    }

    /** Update ForSaleInventory Inventory and ForSaleInventory inventory method*/
    public void updateShopInventories() {
        this.shopfrontMenu.setContents(newShopfrontMenu(false, ShopItem.LoreType.MENU).getContents());
        this.shopfrontDetailedMenu.setContents(newShopfrontMenu(false, ShopItem.LoreType.ITEM).getContents());
        this.editShopfrontMenu.setContents(newShopfrontMenu(true, ShopItem.LoreType.MENU).getContents());
    }

    /** Reload all inventories */
    public void reload() {
        this.mainConfig = VMPlugin.getInstance().getConfig();
        this.buyShopMenu.setContents(newBuyShopInventory().getContents());
        this.editShopMenu.setContents(newEditShopInventory().getContents());
        this.shopfrontMenu.setContents(newShopfrontMenu(false, ShopItem.LoreType.MENU).getContents());
        this.shopfrontDetailedMenu.setContents(newShopfrontMenu(false, ShopItem.LoreType.ITEM).getContents());
        this.editShopfrontMenu.setContents(newShopfrontMenu(true, ShopItem.LoreType.MENU).getContents());
        this.editVillagerMenu.setContents(newEditVillagerInventory().getContents());
        this.sellShopMenu.setContents(newSellShopInventory().getContents());
    }

    /** Returns amount of ItemStack in storage */
    public int getItemAmount(ItemStack itemStack) {
        int amount = 0;
        for (ItemStack storageStack : storageMenu.getContents()) {
            if (storageStack == null) { continue; }
            if (amount == storageSize - 1) continue;

            if (storageStack.isSimilar(itemStack)) {
                amount = amount + storageStack.getAmount();
            }
        }
        return amount;
    }

    /** Returns amount of ItemStack in specified Inventory */
    protected int getAmountInventory(ItemStack itemStack, Inventory inventory) {
        int amount = 0;
        for (ItemStack storageStack : inventory.getContents()) {
            if (storageStack == null) { continue; }

            if (storageStack.isSimilar(itemStack)) {
                amount = amount + storageStack.getAmount();
            }
        }
        return amount;
    }

    /** Returns true if item is blacklisted, false if not */
    private boolean isBlackListed(Material material) {
        List<String> blackList = mainConfig.getStringList("item_blacklist");
        return blackList.contains(material.toString());
    }

    /** Adds shopItem to player's inventory and drops overflowing items */
    protected void giveShopItem(Player player, ShopItem shopItem) {
        ItemStack itemStack = shopItem.asItemStack(ShopItem.LoreType.ITEM);
        HashMap<Integer, ItemStack> itemsLeft = player.getInventory().addItem(itemStack);
        for (int i : itemsLeft.keySet()) {
            player.getLocation().getWorld().dropItemNaturally(player.getLocation(), itemsLeft.get(i));
        }
    }


    /** Getters */
    public String getOwnerUUID() {
        return ownerUUID;
    }
    public int getStorageSize() {
        return storageSize;
    }
    public int getShopfrontSize() {
        return shopfrontSize;
    }
    public Timestamp getExpireDate() {
        return expireDate;
    }
    public String getDuration() {
        return duration;
    }
    public int getCost() {
        return cost;
    }
    public abstract VillagerType getType();
    public HashMap<Integer, ShopItem> getItemList() {
        return itemList;
    }
    public String getEntityUUID() {
        return entityUUID;
    }
    public int getTimesRented() {
        return timesRented;
    }
}
