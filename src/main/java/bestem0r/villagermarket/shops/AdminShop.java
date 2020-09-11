package bestem0r.villagermarket.shops;

import bestem0r.villagermarket.inventories.EditShopInventory;
import bestem0r.villagermarket.inventories.ForSaleInventory;
import bestem0r.villagermarket.items.ItemForSale;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.io.File;

public class AdminShop extends VillagerShop {

    public AdminShop(File file) {
        super(file);

        super.ownerUUID = "admin_shop";
        super.ownerName = "admin_shop";
    }

    @Override
    protected ItemForSale newItemForSale(ItemStack itemStack, double price) {
        return new ItemForSale.Builder(itemStack)
                .price(price)
                .villagerType(VillagerType.ADMIN)
                .build();
    }

    @Override
    protected Inventory newEditShopInventory() {
        return EditShopInventory.create(VillagerType.ADMIN);
    }

    @Override
    protected Inventory newForSaleInventory(Boolean isEditor) {
        return new ForSaleInventory.Builder()
                .isEditor(isEditor)
                .size(super.generalSize)
                .villagerType(VillagerType.ADMIN)
                .itemsForSale(super.itemsForSale)
                .build();
    }

    @Override
    public VillagerType getType() {
        return VillagerType.ADMIN;
    }


}
