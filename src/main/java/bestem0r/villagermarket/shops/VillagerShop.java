package bestem0r.villagermarket.shops;

import bestem0r.villagermarket.VMPlugin;
import bestem0r.villagermarket.events.ItemDrop;
import bestem0r.villagermarket.events.chat.AddAmount;
import bestem0r.villagermarket.items.MenuItem;
import bestem0r.villagermarket.items.ShopItem;
import bestem0r.villagermarket.menus.BuyShopMenu;
import bestem0r.villagermarket.menus.EditShopMenu;
import bestem0r.villagermarket.menus.ProfessionMenu;
import bestem0r.villagermarket.menus.SellShopMenu;
import bestem0r.villagermarket.utilities.Color;
import bestem0r.villagermarket.utilities.Config;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.*;

public abstract class VillagerShop {

    public enum ShopMenu {
        BUY_SHOP,
        EDIT_SHOP,
        EDIT_SHOPFRONT,
        STORAGE,
        EDIT_VILLAGER,
        SELL_SHOP,
        SHOPFRONT,
        SHOPFRONT_DETAILED
    }

    protected String ownerUUID;
    protected String ownerName;
    protected String entityUUID;

    protected String duration;
    protected int seconds;
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

        this.seconds = secondsFromString(duration);

        this.cost = config.getInt("cost");

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
        ItemStack currentItem = event.getCurrentItem();
        ItemStack cursorItem = event.getCursor();
        if (slot < shopfrontSize && currentItem == null && cursorItem.getType() != Material.AIR) {
            //Add item
            if (slot == -1) return false;
            List<Material> blackList = VMPlugin.getInstance().getMaterialBlackList();

            if (blackList.contains(cursorItem.getType())) {
                player.sendMessage(new Color.Builder().path("messages.blacklisted").addPrefix().build());
            } else {
                player.sendMessage(new Color.Builder().path("messages.type_amount").addPrefix().build());
                player.sendMessage(new Color.Builder().path("messages.type_cancel").addPrefix().build());

                ShopItem.Builder builder = new ShopItem.Builder(cursorItem)
                        .entityUUID(entityUUID)
                        .villagerType(getType())
                        .slot(slot);

                Bukkit.getServer().getPluginManager().registerEvents(new ItemDrop(player), VMPlugin.getInstance());
                Bukkit.getServer().getPluginManager().registerEvents(new AddAmount(player, builder), VMPlugin.getInstance());
            }
            event.getView().close();
            return true;
        } else if (slot < shopfrontSize) {
            //Quick add
            if (event.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY) {
                quickAdd(player.getInventory(), event.getRawSlot());
            }
            //Edit
            if (event.getClick() == ClickType.MIDDLE) {
                player.sendMessage(new Color.Builder().path("messages.type_amount").addPrefix().build());
                player.sendMessage(new Color.Builder().path("messages.type_cancel").addPrefix().build());

                ShopItem.Builder builder = new ShopItem.Builder(itemList.get(slot).asItemStack(ShopItem.LoreType.ITEM))
                        .entityUUID(entityUUID)
                        .villagerType(getType())
                        .slot(slot);
                Bukkit.getServer().getPluginManager().registerEvents(new AddAmount(player, builder), VMPlugin.getInstance());
                event.getView().close();
            }
            //Back
            if (slot == shopfrontSize - 1) {
                player.playSound(player.getLocation(), Sound.valueOf(VMPlugin.getInstance().getConfig().getString("sounds.menu_click")), 0.5f, 1);
                player.openInventory(getInventory(ShopMenu.EDIT_SHOP));
                return true;
            }
            //Delete item
            if (event.getClick() == ClickType.RIGHT && currentItem != null) {
                player.playSound(player.getLocation(), Sound.valueOf(VMPlugin.getInstance().getConfig().getString("sounds.remove_item")), 0.5f, 1);
                itemList.remove(slot);
                updateShopInventories();
                event.setCancelled(true);
            }
            //Change mode
            if (event.getClick() == ClickType.LEFT && currentItem != null) {
                player.playSound(player.getLocation(), Sound.valueOf(VMPlugin.getInstance().getConfig().getString("sounds.menu_click")), 0.5f, 1);
                getItemList().get(slot).toggleMode();
                updateShopInventories();
                event.setCancelled(true);
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

    /** Buy shop (This is only for Player Shops )*/
    public void buyShop(Player player, Entity villager) {
        Economy economy = VMPlugin.getEconomy();
        if (economy.getBalance(player) < cost) {
            player.sendMessage(new Color.Builder().path("messages.not_enough_money").addPrefix().build());
            return;
        }

        economy.withdrawPlayer(player, cost);

        villager.setCustomName(new Color.Builder().path("villager.name_taken").replace("%player%", player.getName()).build());
        this.ownerUUID = (player.getUniqueId().toString());
        this.ownerName = (player.getName());

        Date date = new Date();
        this.expireDate = (seconds == 0 ? new Timestamp(0) : new Timestamp(date.getTime() + (seconds * 1000L)));
        this.editShopMenu.setContents(EditShopMenu.create(this).getContents());

        player.playSound(player.getLocation(), Sound.valueOf(mainConfig.getString("sounds.buy_shop")), 1, 1);
        player.openInventory(editShopMenu);
    }
    
    /** Sell shop (This is only for Player Shops) */
    public void abandon() {

        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(UUID.fromString(ownerUUID));
        Economy economy = VMPlugin.getEconomy();

        Entity villager = Bukkit.getEntity(UUID.fromString(entityUUID));
        if (villager != null) villager.setCustomName(new Color.Builder().path("villager.name_available").build());

        Config.newShopConfig(entityUUID, storageSize / 9, shopfrontSize / 9, getCost(), "player", duration);

        economy.depositPlayer(offlinePlayer, ((double) getCost() * (mainConfig.getDouble("refund_percent") / 100)));

        ArrayList<ItemStack> storage = new ArrayList<>(Arrays.asList(getInventory(ShopMenu.STORAGE).getContents()));

        if (offlinePlayer.isOnline()) {
            Player player = (Player) offlinePlayer;
            for (ItemStack storageStack : storage) {
                if (storageStack != null) {
                    if (storage.indexOf(storageStack) == storageSize - 1) continue;
                    HashMap<Integer, ItemStack> exceed = player.getInventory().addItem(storageStack);
                    for (Integer i : exceed.keySet()) {
                        player.getLocation().getWorld().dropItemNaturally(player.getLocation(), exceed.get(i));
                    }
                }
            }
        } else {
            VMPlugin.getDataManager().getAbandonOffline().put(offlinePlayer, storage);
        }
    }

    /** Increase rent time */
    public void increaseTime(Player player) {
        Timestamp newExpire = new Timestamp(expireDate.getTime() + (seconds * 1000L));
        Date date = new Date();
        date.setTime(date.getTime() + ((mainConfig.getInt("max_rent") * 86400) * 1000L));
        if (newExpire.after(date)) {
            player.sendMessage(new Color.Builder().path("messages.max_rent_time").addPrefix().build());
            return;
        }
        Economy economy = VMPlugin.getEconomy();
        if (economy.getBalance(player) < cost) {
            player.sendMessage(new Color.Builder().path("messages.not_enough_money").addPrefix().build());
            return;
        }
        economy.withdrawPlayer(player, cost);
        this.expireDate = newExpire;
        this.editShopMenu.setContents(EditShopMenu.create(this).getContents());

        player.playSound(player.getLocation(), Sound.valueOf(mainConfig.getString("sounds.increase_time")), 1, 1);
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


    /** Save method */
    public void save() {
        config.set("ownerUUID", ownerUUID);
        config.set("ownerName", ownerName);
        config.set("expire", expireDate.getTime());

        ItemStack[] storage = storageMenu.getContents();
        storage[storage.length - 1] = null;
        config.set("storage", storage);

        List<ItemStack> itemStackList = new ArrayList<>();
        List<Double> priceList = new ArrayList<>();
        List<String> modeList = new ArrayList<>();

        for (Integer slot : itemList.keySet()) {
            if (itemList.get(slot) == null) {
                itemStackList.add(null);
                priceList.add(0.0);
                modeList.add("SELL");
            } else {
                itemStackList.add(itemList.get(slot).asItemStack(ShopItem.LoreType.ITEM));
                priceList.add(itemList.get(slot).getPrice());
                modeList.add(itemList.get(slot).getMode().toString());
            }
        }
        config.set("prices", priceList);
        config.set("for_sale", itemStackList);
        config.set("modes", modeList);
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

}
