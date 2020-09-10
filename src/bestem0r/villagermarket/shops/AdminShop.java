package bestem0r.villagermarket.shops;

import bestem0r.villagermarket.inventories.EditShopInventory;
import bestem0r.villagermarket.inventories.ForSaleInventory;
import org.bukkit.inventory.Inventory;

import java.io.File;

public class AdminShop extends VillagerShop {

    public AdminShop(File file) {
        super(file);

        super.ownerUUID = "admin_shop";
        super.ownerName = "admin_shop";
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
                .villagerType(VillagerType.ADMIN)
                .itemsForSale(super.itemsForSale)
                .build();
    }
}
