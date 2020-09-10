package bestem0r.villagermarket.shops;

import bestem0r.villagermarket.inventories.EditShopInventory;
import bestem0r.villagermarket.inventories.ForSaleInventory;
import bestem0r.villagermarket.utilities.ColorBuilder;
import com.mojang.datafixers.util.Pair;
import org.bukkit.*;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.File;
import java.util.*;

public class PlayerShop extends VillagerShop {

    public PlayerShop(File file) {
        super(file);

        super.ownerUUID = config.getString("ownerUUID");
        super.ownerName = config.getString("ownerName");
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
                .villagerType(VillagerType.PLAYER)
                .itemsForSale(super.itemsForSale)
                .build();
    }

}
