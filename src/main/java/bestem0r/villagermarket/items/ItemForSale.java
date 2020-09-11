package bestem0r.villagermarket.items;

import bestem0r.villagermarket.shops.VillagerShop;
import bestem0r.villagermarket.utilities.ColorBuilder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

public class ItemForSale extends ItemStack {

    private enum LoreType {
        ITEM,
        MENU
    }

    private final ItemMeta itemMeta;
    private VillagerShop.VillagerType villagerType;
    boolean isEditor = false;

    private double price;
    private int slot;
    private final List<String> itemLore;
    private List<String> menuLore;

    private LoreType loreType;

    private ItemForSale(ItemStack itemStack) {
        super(itemStack);

        this.itemMeta = super.getItemMeta();
        this.itemLore = super.getItemMeta().getLore();
    }

    public static class Builder {

        private final ItemStack itemStack;
        private VillagerShop.VillagerType villagerType;
        private String entityUUID;

        private double price;
        private int slot;

        private LoreType loreType = LoreType.MENU;

        public Builder(ItemStack itemStack) {
            this.itemStack = itemStack;
        }

        public Builder villagerType(VillagerShop.VillagerType villagerType) {
            this.villagerType = villagerType;
            return this;
        }
        public Builder entityUUID(String entityUUID) {
            this.entityUUID = entityUUID;
            return this;
        }

        public Builder price(double price) {
            this.price = price;
            return this;
        }
        public Builder slot(int slot) {
            this.slot = slot;
            return this;
        }
        public Builder loreType(LoreType loreType) {
            this.loreType = loreType;
            return this;
        }
        public ItemForSale build() {
            ItemForSale itemForSale = new ItemForSale(itemStack);
            itemForSale.villagerType = villagerType;

            itemForSale.price = price;
            itemForSale.slot = slot;

            itemForSale.loreType = loreType;

            return itemForSale;
        }

        public String getEntityUUID() {
            return entityUUID;
        }
    }

    /** Getters */
    public double getPrice() {
        return price;
    }
    public int getSlot() {
        return slot;
    }


    public void toggleEditor(boolean editor) {
        isEditor = editor;
    }
    public void toggleLore() {
        switch (loreType) {
            case ITEM:
                itemMeta.setLore(menuLore);
                super.setItemMeta(itemMeta);
                loreType = LoreType.MENU;
                break;
            case MENU:
                itemMeta.setLore(itemLore);
                super.setItemMeta(itemMeta);
                loreType = LoreType.ITEM;
                break;
        }
    }

    public void updateStorage(int storageAmount) {
        String itemAmount_s = String.valueOf(super.getAmount());
        String price_s = String.valueOf(price);
        String storageAmount_s = String.valueOf(storageAmount);
        switch (villagerType) {
            case ADMIN:
                if (isEditor) {
                    menuLore = ColorBuilder.loreReplaceTwo("menus.edit_for_sale.item_lore_admin", "%amount%", itemAmount_s, "%price%", price_s);
                } else {
                    menuLore = ColorBuilder.loreReplaceTwo("menus.buy_items.item_lore_admin", "%amount%", itemAmount_s, "%price%", price_s);
                }
                break;
            case PLAYER:
                if (isEditor) {
                    menuLore = ColorBuilder.loreReplaceThree("menus.edit_for_sale.item_lore", "%amount%", itemAmount_s, "%price%", price_s, "%stock%", storageAmount_s);
                } else {
                    menuLore = ColorBuilder.loreReplaceThree("menus.buy_items.item_lore", "%amount%", itemAmount_s, "%price%", price_s, "%stock%", storageAmount_s);
                }
                break;
        }
    }

    public ItemStack asItemStack() {
        ItemStack itemStack = new ItemStack(super.clone());
        ItemMeta itemMeta = itemStack.getItemMeta();
        itemMeta.setLore(itemLore);
        return itemStack;
    }
}
