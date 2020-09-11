package bestem0r.villagermarket.shops;

import bestem0r.villagermarket.VMPlugin;
import bestem0r.villagermarket.inventories.BuyShopInventory;
import bestem0r.villagermarket.inventories.ProfessionInventory;
import bestem0r.villagermarket.inventories.SellShopInventory;
import bestem0r.villagermarket.items.ItemForSale;
import bestem0r.villagermarket.items.MenuItem;
import bestem0r.villagermarket.utilities.ColorBuilder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public abstract class VillagerShop {

    public enum ShopInventory {
        BUY_SHOP,
        EDIT_SHOP,
        EDIT_FOR_SALE,
        STORAGE,
        EDIT_VILLAGER,
        SELL_SHOP,
        BUY_ITEMS
    }

    protected String ownerUUID;
    protected String ownerName;

    protected int size;
    protected int cost;

    protected HashMap<Integer, ItemForSale> itemList = new HashMap<>();

    protected int generalSize;
    protected int storageSize;

    protected File file;
    protected FileConfiguration config;

    protected FileConfiguration mainConfig;

    protected Inventory buyShopInventory;
    protected Inventory editShopInventory;
    protected Inventory storageInventory;
    protected Inventory forSaleInventory;
    protected Inventory editForSaleInventory;
    protected Inventory editVillagerInventory;
    protected Inventory sellShopInventory;

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

        this.size = config.getInt("size");

        this.cost = config.getInt("cost");

        buildItemList();

        this.ownerUUID = "";
        this.ownerName = "";

        this.generalSize = size * 9;
        this.storageSize = size * 18;

        this.buyShopInventory = newBuyShopInventory();
        this.editShopInventory = newEditShopInventory();
        this.storageInventory = newStorageInventory();
        this.forSaleInventory = newForSaleInventory(false);
        this.editForSaleInventory = newForSaleInventory(true);
        this.editVillagerInventory = newEditVillagerInventory();
        this.sellShopInventory = newSellShopInventory();
    }

    abstract void buildItemList();

    /** Inventory methods */

    public Inventory getInventory(ShopInventory shopInventory) {
        switch (shopInventory) {
            case STORAGE:
                return storageInventory;
            case BUY_SHOP:
                return buyShopInventory;
            case BUY_ITEMS:
                return forSaleInventory;
            case EDIT_SHOP:
                return editShopInventory;
            case SELL_SHOP:
                return sellShopInventory;
            case EDIT_FOR_SALE:
                return editForSaleInventory;
            case EDIT_VILLAGER:
                return editVillagerInventory;
        }
        return forSaleInventory;
    }

    /** Create new buy shop inventory */
    protected Inventory newBuyShopInventory() {
        return BuyShopInventory.create(this);
    }
    /** Create new edit shop inventory */
    protected abstract Inventory newEditShopInventory();

    protected abstract Inventory newForSaleInventory(Boolean isEditor);

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
        return ProfessionInventory.create();
    }
    /** Create new sell shop inventory */
    protected Inventory newSellShopInventory() {
        return SellShopInventory.create(this);
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

        ItemStack[] storage = storageInventory.getContents();
        storage[storage.length - 1] = null;
        config.set("storage", storage);

        List<ItemStack> forSaleList = new ArrayList<>();
        List<Double> priceList = new ArrayList<>();
        for (Integer slot : itemList.keySet()) {
            if (itemList.get(slot) == null) {
                forSaleList.add(null);
                priceList.add(0.0);
            } else {
                forSaleList.add(itemList.get(slot).asItemStack());
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
        storageInventory.removeItem(itemStack);
        updateShopInventories();
    }

    /** Update ForSaleInventory Inventory and ForSaleInventory inventory method*/
    public void updateShopInventories() {
        this.forSaleInventory.setContents(newForSaleInventory(false).getContents());
        this.editForSaleInventory.setContents(newForSaleInventory(true).getContents());
    }

    /** Reload all inventories */
    public void reload() {
        this.buyShopInventory.setContents(newBuyShopInventory().getContents());
        this.editShopInventory.setContents(newEditShopInventory().getContents());
        this.forSaleInventory.setContents(newForSaleInventory(false).getContents());
        this.editForSaleInventory.setContents(newForSaleInventory(true).getContents());
        this.editVillagerInventory.setContents(newEditVillagerInventory().getContents());
        this.sellShopInventory.setContents(newSellShopInventory().getContents());
    }

    public int getItemAmount(ItemStack itemStack) {
        int i = 0;
        Bukkit.getLogger().info("Type: " + itemStack.getType().name());

        for (ItemStack storageStack : storageInventory.getContents()) {
            if (storageStack == null) { continue; }
            if (storageStack.getType() == Material.AIR) { continue; }
            if (i == storageSize - 1) continue;
            Bukkit.getLogger().info(storageStack.getType().name());
            if (storageStack.isSimilar(itemStack)) {
                i = i + storageStack.getAmount();
            }
        }
        return i;
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

    public HashMap<Integer, ItemForSale> getItemList() {
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
