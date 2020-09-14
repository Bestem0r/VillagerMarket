package bestem0r.villagermarket.shops;

import bestem0r.villagermarket.VMPlugin;
import bestem0r.villagermarket.events.PlayerChat;
import bestem0r.villagermarket.menus.BuyShopMenu;
import bestem0r.villagermarket.menus.ProfessionMenu;
import bestem0r.villagermarket.menus.SellShopMenu;
import bestem0r.villagermarket.items.ShopfrontItem;
import bestem0r.villagermarket.items.MenuItem;
import bestem0r.villagermarket.utilities.ColorBuilder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

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
    protected OfflinePlayer owner;

    protected int size;
    protected int cost;

    protected HashMap<Integer, ShopfrontItem> itemList = new HashMap<>();

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

        this.size = config.getInt("size");

        this.cost = config.getInt("cost");

        buildItemList();

        this.ownerUUID = "";
        this.ownerName = "";

        this.shopfrontSize = size * 9;
        this.storageSize = size * 18;

        this.buyShopMenu = newBuyShopInventory();
        this.editShopMenu = newEditShopInventory();
        this.storageMenu = newStorageInventory();
        this.editVillagerMenu = newEditVillagerInventory();
        this.sellShopMenu = newSellShopInventory();
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
        ShopfrontItem shopfrontItem = itemList.get(slot);
        if (shopfrontItem == null) return false;
        switch (shopfrontItem.getMode()) {
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
            List<Material> blackList = VMPlugin.getInstance().getMaterialBlackList();

            if (blackList.contains(cursorItem.getType())) {
                player.sendMessage(VMPlugin.getPrefix() + ColorBuilder.color("messages.blacklisted"));
            } else {
                PlayerChat.startChatSession(player, entityUUID, cursorItem, slot);
            }
            event.getView().close();
            return true;
        } else if (slot < shopfrontSize) {
            //Back
            if (slot == 8) {
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

    /** Create new buy shop inventory */
    protected Inventory newBuyShopInventory() {
        return BuyShopMenu.create(this);
    }
    /** Create new edit shop inventory */
    protected abstract Inventory newEditShopInventory();
    /** Create new EditForSale inventory*/
    protected abstract Inventory newShopfrontMenu(Boolean isEditor, ShopfrontItem.LoreType loreType);

    /** Create new storage inventory */
    protected Inventory newStorageInventory() {
        Inventory inventory = Bukkit.createInventory(null, storageSize, ColorBuilder.color("menus.edit_storage.title"));
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

        ItemStack[] storage = storageMenu.getContents();
        storage[storage.length - 1] = null;
        config.set("storage", storage);

        List<ItemStack> forSaleList = new ArrayList<>();
        List<Double> priceList = new ArrayList<>();
        for (Integer slot : itemList.keySet()) {
            if (itemList.get(slot) == null) {
                forSaleList.add(null);
                priceList.add(0.0);
            } else {
                forSaleList.add(itemList.get(slot).asItemStack(ShopfrontItem.LoreType.ITEM));
                priceList.add(itemList.get(slot).getPrice());
            }
        }
        config.set("prices", priceList);
        config.set("for_sale", forSaleList);
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
        this.shopfrontMenu.setContents(newShopfrontMenu(false, ShopfrontItem.LoreType.MENU).getContents());
        this.editShopfrontMenu.setContents(newShopfrontMenu(true, ShopfrontItem.LoreType.MENU).getContents());
    }

    /** Reload all inventories */
    public void reload() {
        this.buyShopMenu.setContents(newBuyShopInventory().getContents());
        this.editShopMenu.setContents(newEditShopInventory().getContents());
        this.shopfrontMenu.setContents(newShopfrontMenu(false, ShopfrontItem.LoreType.MENU).getContents());
        this.editShopfrontMenu.setContents(newShopfrontMenu(true, ShopfrontItem.LoreType.MENU).getContents());
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

    public int getSize() {
        return size;
    }

    public int getCost() {
        return cost;
    }

    public abstract VillagerType getType();

    public HashMap<Integer, ShopfrontItem> getItemList() {
        return itemList;
    }

    /** Setters */
    public void setOwnerUUID(String ownerUUID) {
        this.ownerUUID = ownerUUID;
    }

    public void setOwnerName(String ownerName) {
        this.ownerName = ownerName;
    }
}
