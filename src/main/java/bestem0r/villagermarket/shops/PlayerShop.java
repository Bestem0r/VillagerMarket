package bestem0r.villagermarket.shops;

import bestem0r.villagermarket.inventories.EditShopInventory;
import bestem0r.villagermarket.inventories.ForSaleInventory;
import bestem0r.villagermarket.items.ItemForSale;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.io.File;

public class PlayerShop extends VillagerShop {

    public PlayerShop(File file) {
        super(file);

        super.ownerUUID = config.getString("ownerUUID");
        super.ownerName = config.getString("ownerName");
    }

    @Override
    protected ItemForSale newItemForSale(ItemStack itemStack, double price) {
        return new ItemForSale.Builder(itemStack)
                .price(price)
                .villagerType(VillagerType.PLAYER)
                .build();
    }

    /** Inventory methods */
    @Override
    protected Inventory newEditShopInventory() {
        return EditShopInventory.create(VillagerType.PLAYER);
    }

    /** Create new inventory for items for sale editor, or shop front */
    @Override
    protected Inventory newForSaleInventory(Boolean isEditor) {
        return new ForSaleInventory.Builder()
                .isEditor(isEditor)
                .size(super.generalSize)
                .villagerType(VillagerType.PLAYER)
                .itemsForSale(super.itemsForSale)
                .build();
    }

    @Override
    public VillagerType getType() {
        return VillagerType.PLAYER;
    }

}
