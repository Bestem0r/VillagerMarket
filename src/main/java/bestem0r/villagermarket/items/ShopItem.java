package bestem0r.villagermarket.items;

import bestem0r.villagermarket.VMPlugin;
import bestem0r.villagermarket.shops.VillagerShop;
import bestem0r.villagermarket.utilities.Color;
import net.milkbowl.vault.economy.Economy;
import org.apache.commons.lang.WordUtils;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;
import java.util.UUID;

public class ShopItem extends ItemStack {

    public enum LoreType {
        ITEM,
        MENU
    }
    public enum Mode {
        BUY,
        SELL
    }

    private VillagerShop.VillagerType villagerType;
    boolean isEditor = false;

    private double price;
    private int slot;
    private List<String> menuLore;

    private Mode mode;

    private String menuName;

    private ShopItem(ItemStack itemStack) {
        super(itemStack);
    }

    public static class Builder {

        private final ItemStack itemStack;
        private VillagerShop.VillagerType villagerType;
        private String entityUUID;

        private double price;
        private int slot;
        private int amount = 1;

        private Mode mode = Mode.SELL;

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
        public Builder amount(int amount) {
            this.amount = amount;
            return this;
        }
        public Builder mode(Mode mode) {
            this.mode = mode;
            return this;
        }

        public ShopItem build() {
            ShopItem shopItem = new ShopItem(itemStack);
            shopItem.villagerType = villagerType;

            shopItem.price = price;
            shopItem.slot = slot;
            shopItem.setAmount(amount);

            shopItem.mode = mode;

            return shopItem;
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
    public Mode getMode() {
        return mode;
    }

    public void toggleEditor(boolean editor) {
        isEditor = editor;
    }

    public void toggleMode() {
        switch (mode) {
            case BUY:
                mode = Mode.SELL;
                break;
            case SELL:
                mode = Mode.BUY;
                break;
        }
    }

    public void refreshLore(VillagerShop villagerShop) {
        Economy economy = VMPlugin.getEconomy();
        double moneyLeft = 0;

        if (villagerType == VillagerShop.VillagerType.PLAYER) {
            OfflinePlayer owner = Bukkit.getOfflinePlayer(UUID.fromString(villagerShop.getOwnerUUID()));
            moneyLeft = economy.getBalance(owner);
        }

        int storageAmount = villagerShop.getItemAmount(asItemStack(LoreType.ITEM));

        Mode itemMode = mode;
        if (!isEditor) itemMode = (mode == Mode.BUY ? Mode.SELL : Mode.BUY);

        String inventoryPath = (isEditor ? ".edit_shopfront." : ".shopfront.");
        String typePath = (villagerType == VillagerShop.VillagerType.ADMIN ? "admin." : "player.");
        String modePath = itemMode.toString().toLowerCase();

        String lorePath = "menus" + inventoryPath + typePath + modePath + "_lore";
        menuLore = new Color.Builder()
                .path(lorePath)
                .replace("%amount%", String.valueOf(super.getAmount()))
                .replace("%price%", String.valueOf(price))
                .replace("%stock%", String.valueOf(storageAmount))
                .replace("%money%", String.valueOf(moneyLeft))
                .buildLore();

        String namePath = "menus" + inventoryPath + "item_name";
        String name = (super.getItemMeta().hasDisplayName() ? super.getItemMeta().getDisplayName() : WordUtils.capitalizeFully(super.getType().name().replaceAll("_", " ")));
        String mode = new Color.Builder().path("menus" + inventoryPath + "modes." + itemMode.toString().toLowerCase()).build();
        menuName = new Color.Builder()
                .path(namePath)
                .replace("%item_name%", name)
                .replace("%mode%", mode)
                .build();
    }

    public ItemStack asItemStack(LoreType loreType) {
        ItemStack itemStack = new ItemStack(this);

        if (loreType == LoreType.MENU) {
            ItemMeta itemMeta = itemStack.getItemMeta();
            itemMeta.setLore(menuLore);
            itemMeta.setDisplayName(menuName);
            itemStack.setItemMeta(itemMeta);
            return itemStack;
        }

        return itemStack;
    }
}
