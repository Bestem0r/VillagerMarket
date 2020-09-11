package bestem0r.villagermarket.shops;

import bestem0r.villagermarket.inventories.EditShopInventory;
import bestem0r.villagermarket.inventories.ForSaleInventory;
import bestem0r.villagermarket.items.ItemForSale;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.util.List;

public class AdminShop extends VillagerShop {

    public AdminShop(File file) {
        super(file);

        super.ownerUUID = "admin_shop";
        super.ownerName = "admin_shop";
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
                        .villagerType(VillagerType.ADMIN)
                        .build();
            }
            this.itemList.put(i, itemForSale);
        }
    }

    @Override
    protected Inventory newEditShopInventory() {
        return EditShopInventory.create(VillagerType.ADMIN);
    }

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
        return VillagerType.ADMIN;
    }


}
