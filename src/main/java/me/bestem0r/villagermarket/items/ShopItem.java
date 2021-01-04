package me.bestem0r.villagermarket.items;

import me.bestem0r.villagermarket.shops.VillagerShop;
import me.bestem0r.villagermarket.utilities.ColorBuilder;
import org.apache.commons.lang.WordUtils;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public class ShopItem extends ItemStack {

    public enum LoreType {
        ITEM,
        MENU
    }
    public enum Mode {
        BUY,
        SELL,
        COMMAND
    }
    private final JavaPlugin plugin;

    private VillagerShop.VillagerType villagerType;
    boolean isEditor = false;

    private BigDecimal price;
    private int slot;
    private int limit = 0;
    private List<String> menuLore;
    private final HashMap<UUID, Integer> playerLimit = new HashMap<>();

    private Mode mode;

    private String menuName;
    private String command;

    private ShopItem(JavaPlugin plugin, ItemStack itemStack) {
        super(itemStack);
        this.plugin = plugin;
    }

    /** Builder for new ShopItem */
    public static class Builder {

        private final ItemStack itemStack;
        private final JavaPlugin plugin;
        private VillagerShop.VillagerType villagerType;
        private String entityUUID;

        private BigDecimal price;
        private int slot;
        private int amount = 1;
        private int buyLimit = 0;

        private Mode mode = Mode.SELL;

        public Builder(JavaPlugin plugin, ItemStack itemStack) {
            this.plugin = plugin;
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
        public Builder price(BigDecimal price) {
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
            ShopItem shopItem = new ShopItem(plugin, itemStack);
            shopItem.villagerType = villagerType;

            shopItem.price = price;
            shopItem.slot = slot;
            shopItem.setAmount(amount);
            shopItem.limit = buyLimit;

            shopItem.mode = mode;

            return shopItem;
        }
        public String getEntityUUID() {
            return entityUUID;
        }
    }

    /** Getters */
    public BigDecimal getPrice() {
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
    public int getLimit() {
        return limit;
    }
    public HashMap<UUID, Integer> getPlayerLimit() {
        return playerLimit;
    }

    /** Setters */
    public void setLimit(int limit) {
        this.limit = limit;
    }
    public void setCommand(String command) {
        this.command = command;
        this.mode = Mode.COMMAND;
        NamespacedKey key = new NamespacedKey(plugin, "villagermarket-command");
        ItemMeta itemMeta = getItemMeta();
        itemMeta.getPersistentDataContainer().set(key, PersistentDataType.STRING, command);
        setItemMeta(itemMeta);

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
    /** Returns how many times the player has bought/sold this item */
    public int getPlayerLimit(Player player) {
        return playerLimit.getOrDefault(player.getUniqueId(), 0);
    }
    /** Increases the amount bought/sold for specified player by one */
    public void increasePlayerLimit(Player player) {
        if (playerLimit.containsKey(player.getUniqueId())) {
            playerLimit.replace(player.getUniqueId(), playerLimit.get(player.getUniqueId()) + 1);
        } else {
            playerLimit.put(player.getUniqueId(), 1);
        }
    }

    /** Runs command as console */
    public void runCommand(Player player) {
        ConsoleCommandSender sender = Bukkit.getConsoleSender();
        Bukkit.dispatchCommand(sender, command.replaceAll("%player%", player.getName()));
    }

    /** Loads player limits from config section */
    public void addPlayerLimit(UUID uuid, int amount) {
        playerLimit.put(uuid, amount);
    }

    /** Refreshes Menu lore */
    public void refreshLore(VillagerShop villagerShop) {
        FileConfiguration config = plugin.getConfig();

        int storageAmount = villagerShop.getAmountInStorage(asItemStack(LoreType.ITEM));
        int available = villagerShop.getAvailable(this);

        Mode itemMode = mode;
        if (!isEditor && mode != Mode.COMMAND) { itemMode = (mode == Mode.BUY ? Mode.SELL : Mode.BUY); }

        String inventoryPath = (isEditor ? ".edit_shopfront." : ".shopfront.");
        String typePath = (villagerType == VillagerShop.VillagerType.ADMIN ? "admin." : "player.");
        String modePath = itemMode.toString().toLowerCase();

        String lorePath = "menus" + inventoryPath + typePath + modePath + "_lore";
        menuLore = new ColorBuilder(plugin)
                .path(lorePath)
                .replace("%amount%", String.valueOf(super.getAmount()))
                .replaceWithCurrency("%price%", price.stripTrailingZeros().toPlainString())
                .replace("%stock%", String.valueOf(storageAmount))
                .replace("%available%", String.valueOf(available))
                .replace("%limit%", (limit == 0 ? config.getString("quantity.unlimited") : String.valueOf(limit)))
                .buildLore();

        String namePath = "menus" + inventoryPath + "item_name";
        String name = (super.getItemMeta().hasDisplayName() ? super.getItemMeta().getDisplayName() : WordUtils.capitalizeFully(getType().name().replaceAll("_", " ")));
        String mode = new ColorBuilder(plugin).path("menus" + inventoryPath + "modes." + itemMode.toString().toLowerCase()).build();
        menuName = new ColorBuilder(plugin)
                .path(namePath)
                .replace("%item_name%", name)
                .replace("%mode%", mode)
                .build();
    }

    /** Returns an ItemStack either as a menu item or as the original item */
    public ItemStack asItemStack(LoreType loreType) {
        ItemStack itemStack = new ItemStack(this);

        ItemMeta itemMeta = itemStack.getItemMeta();
        if (mode != Mode.COMMAND && loreType == LoreType.MENU) {
            itemMeta.setLore(menuLore);
            itemMeta.setDisplayName(menuName);
            itemStack.setItemMeta(itemMeta);
            return itemStack;
        }
        if (mode == Mode.COMMAND && loreType == LoreType.MENU) {
            List<String> currentLore = itemMeta.getLore();
            if (menuLore == null) {
            }
            if (currentLore != null) {
                currentLore.addAll(menuLore);
                itemMeta.setLore(currentLore);
            } else {
                itemMeta.setLore(menuLore);
            }
        }
        itemStack.setItemMeta(itemMeta);
        return itemStack;
    }
}
