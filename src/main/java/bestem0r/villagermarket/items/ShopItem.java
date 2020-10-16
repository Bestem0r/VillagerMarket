package bestem0r.villagermarket.items;

import bestem0r.villagermarket.VMPlugin;
import bestem0r.villagermarket.shops.VillagerShop;
import bestem0r.villagermarket.utilities.Color;
import net.milkbowl.vault.economy.Economy;
import org.apache.commons.lang.WordUtils;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.FileConfiguration;
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
    private int buyLimit = 0;
    private List<String> menuLore;

    private Mode mode;

    private String menuName;

    private ShopItem(ItemStack itemStack) {
        super(itemStack);
    }

    /** Builder for new ShopItem */
    public static class Builder {

        private final ItemStack itemStack;
        private VillagerShop.VillagerType villagerType;
        private String entityUUID;

        private double price;
        private int slot;
        private int amount = 1;
        private int buyLimit = 0;

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
        public Builder buyLimit(int amount) {
            this.buyLimit = amount;
            return this;
        }

        public ShopItem build() {
            ShopItem shopItem = new ShopItem(itemStack);
            shopItem.villagerType = villagerType;

            shopItem.price = price;
            shopItem.slot = slot;
            shopItem.setAmount(amount);
            shopItem.buyLimit = buyLimit;

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
    public int getBuyLimit() {
        return buyLimit;
    }

    /** Setters */
    public void setBuyLimit(int buyLimit) {
        this.buyLimit = buyLimit;
    }

    /** Toggles between sell/buy mode */
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

    /** Refreshes Menu lore */
    public void refreshLore(VillagerShop villagerShop) {
        FileConfiguration config = VMPlugin.getInstance().getConfig();

        int storageAmount = villagerShop.getItemAmount(asItemStack(LoreType.ITEM));
        int available = (Math.max(buyLimit - storageAmount, 0));

        Mode itemMode = mode;
        if (!isEditor) itemMode = (mode == Mode.BUY ? Mode.SELL : Mode.BUY);

        String inventoryPath = (isEditor ? ".edit_shopfront." : ".shopfront.");
        String typePath = (villagerType == VillagerShop.VillagerType.ADMIN ? "admin." : "player.");
        String modePath = itemMode.toString().toLowerCase();

        String lorePath = "menus" + inventoryPath + typePath + modePath + "_lore";
        menuLore = new Color.Builder()
                .path(lorePath)
                .replace("%amount%", String.valueOf(super.getAmount()))
                .replaceWithCurrency("%price%", String.valueOf(price))
                .replace("%stock%", String.valueOf(storageAmount))
                .replace("%available%", String.valueOf(available))
                .replace("%limit%", (buyLimit == 0 ? config.getString("time.unlimited") : String.valueOf(buyLimit)))
                .buildLore();

        String namePath = "menus" + inventoryPath + "item_name";
        String name = (super.getItemMeta().hasDisplayName() ? super.getItemMeta().getDisplayName() : WordUtils.capitalizeFully(getType().name().replaceAll("_", " ")));
        String mode = new Color.Builder().path("menus" + inventoryPath + "modes." + itemMode.toString().toLowerCase()).build();
        menuName = new Color.Builder()
                .path(namePath)
                .replace("%item_name%", name)
                .replace("%mode%", mode)
                .build();
    }

    /** Returns an ItemStack either as a menu item or as the original item */
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
