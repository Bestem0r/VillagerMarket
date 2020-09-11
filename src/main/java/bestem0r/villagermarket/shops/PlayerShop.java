package bestem0r.villagermarket.shops;

import bestem0r.villagermarket.inventories.EditShopInventory;
import bestem0r.villagermarket.inventories.ForSaleInventory;
import bestem0r.villagermarket.items.ItemForSale;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.util.List;

public class PlayerShop extends VillagerShop {

    public PlayerShop(File file) {
        super(file);

        super.ownerUUID = config.getString("ownerUUID");
        super.ownerName = config.getString("ownerName");

    }

    @Override
    void buildItemList() {
        List<Double> priceList = config.getDoubleList("prices");
        List<ItemStack> itemList = (List<ItemStack>) this.config.getList("for_sale");

        for (int i = 0; i < itemList.size(); i ++) {
            double price = (priceList.size() > i ? priceList.get(i) : 0.0);
            ItemForSale itemForSale = null;
            if (itemList.get(i) != null) {
                itemForSale = new ItemForSale.Builder(itemList.get(i))
                        .price(price)
                        .villagerType(VillagerType.PLAYER)
                        .build();
            }
            this.itemList.put(i, itemForSale);
        }
    }


    /** Inventory methods */
    @Override
    protected Inventory newEditShopInventory() {
        return EditShopInventory.create(VillagerType.PLAYER);
    }

    /** Create new inventory for items for sale editor, or shop front */
    @Override
    protected Inventory newForSaleInventory(Boolean isEditor) {
        return new ForSaleInventory.Builder(this)
                .isEditor(isEditor)
                .size(super.generalSize)
                .itemList(itemList)
                .build();
    }

    @Override
    public VillagerType getType() {
        return VillagerType.PLAYER;
    }

}
