package bestem0r.villagermarket.shops;

import bestem0r.villagermarket.VMPlugin;
import bestem0r.villagermarket.events.InventoryClick;
import bestem0r.villagermarket.events.ItemDrop;
import bestem0r.villagermarket.events.chat.SetAmount;
import bestem0r.villagermarket.items.MenuItem;
import bestem0r.villagermarket.items.ShopItem;
import bestem0r.villagermarket.menus.BuyShopMenu;
import bestem0r.villagermarket.menus.ProfessionMenu;
import bestem0r.villagermarket.menus.SellShopMenu;
import bestem0r.villagermarket.utilities.Color;
import bestem0r.villagermarket.utilities.Methods;
import bestem0r.villagermarket.utilities.ShopStats;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import scala.math.BigInt;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public abstract class VillagerShop {

    protected String ownerUUID;
    protected String ownerName;
    protected String entityUUID;

    protected String duration;
    protected int seconds;
    protected int timesRented;
    protected Timestamp expireDate;

    protected int cost;
    protected BigDecimal collectedMoney = BigDecimal.valueOf(0);
    protected final List<String> trusted;

    protected HashMap<Integer, ShopItem> itemList = new HashMap<>();

    protected int shopSize;
    protected int storageSize;

    protected File file;
    protected FileConfiguration config;
    protected FileConfiguration mainConfig;

    protected Inventory buyShopMenu;
    protected Inventory editShopMenu;
    protected Inventory storageMenu;
    protected Inventory shopfrontMenu;
    protected Inventory shopfrontDetailedMenu;
    protected Inventory editShopfrontMenu;
    protected Inventory editVillagerMenu;
    protected Inventory sellShopMenu;

    protected ShopStats shopStats;

    public enum VillagerType {
        ADMIN,
        PLAYER
    }

    /** Constructor */
    public VillagerShop(File file) {
        this.mainConfig = VMPlugin.getInstance().getConfig();
        this.file = file;
        this.config = YamlConfiguration.loadConfiguration(file);

        this.ownerUUID = config.getString("ownerUUID");
        this.ownerName = config.getString("ownerName");
        this.entityUUID = file.getName().substring(0, file.getName().indexOf('.'));

        this.shopSize = config.getInt("shopfrontSize") * 9;
        this.storageSize = config.getInt("storageSize") * 9;
        this.expireDate = new Timestamp(config.getLong("expire"));

        int size = config.getInt("size");
        if (size != 0) {
            this.shopSize = size * 9;
            this.storageSize = size * 18;
        }
        this.duration = config.getString("duration");
        this.duration = (duration == null ? "infinite" : duration);
        this.timesRented = config.getInt("times_rented");
        this.timesRented = (timesRented == 0 ? 1 : timesRented);
        this.seconds = Methods.secondsFromString(duration);
        this.trusted = config.getStringList("trusted");

        this.cost = config.getInt("cost");
        this.shopStats = new ShopStats(config);

        this.storageMenu = newStorageInventory();
        buildItemList();
        this.buyShopMenu = newBuyShopInventory();
        this.editShopMenu = newEditShopInventory();
        this.shopfrontMenu = newShopfrontMenu(false, ShopItem.LoreType.MENU);
        this.shopfrontDetailedMenu = newShopfrontMenu(false, ShopItem.LoreType.ITEM);
        this.editShopfrontMenu = newShopfrontMenu(true, ShopItem.LoreType.MENU);
        this.editVillagerMenu = newEditVillagerInventory();
        this.sellShopMenu = newSellShopInventory();

    }

    /** Populates ItemList of shop items */
    private void buildItemList() {
        if (config.getList("for_sale") != null) {
            buildItemList_old();
            config.set("for_sale", null);
            config.set("prices", null);
            config.set("modes", null);
            config.set("max_buy", null);
            save();
        }
        VillagerType type = (this instanceof AdminShop ? VillagerType.ADMIN : VillagerType.PLAYER);
        ConfigurationSection section = config.getConfigurationSection("items_for_sale");
        if (section == null) { return; }

        for (String slot : section.getKeys(false)) {
            ItemStack itemStack = config.getItemStack("items_for_sale." + slot + ".item");
            if (itemStack == null) { continue; }
            ShopItem shopItem = new ShopItem.Builder(itemStack)
                    .price(config.getDouble("items_for_sale." + slot + ".price"))
                    .villagerType(type)
                    .mode(ShopItem.Mode.valueOf(config.getString("items_for_sale." + slot + ".mode")))
                    .buyLimit(config.getInt("items_for_sale." + slot + ".buy_limit"))
                    .amount(itemStack.getAmount())
                    .build();
            ConfigurationSection limits = config.getConfigurationSection("items_for_sale." + slot + ".limits");
            if (limits != null) {
                for (String uuid : limits.getKeys(false)) {
                    shopItem.addPlayerLimit(UUID.fromString(uuid), config.getInt("items_for_sale." + slot + ".limits." + uuid));
                }
            }
            itemList.put(Integer.parseInt(slot), shopItem);
        }
    }

    @Deprecated
    private void buildItemList_old() {
        List<Double> priceList = config.getDoubleList("prices");
        List<String> modeList = config.getStringList("modes");
        List<Integer> maxList = config.getIntegerList("max_buy");
        List<ItemStack> itemList = (List<ItemStack>) this.config.getList("for_sale");
        if (itemList == null) { return; }
        VillagerType type = (this instanceof AdminShop ? VillagerType.ADMIN : VillagerType.PLAYER);

        for (int i = 0; i < itemList.size(); i ++) {
            double price = (priceList.size() > i ? priceList.get(i) : 0.0);
            int max = (maxList.size() > i ? maxList.get(i) : 0);
            ShopItem.Mode mode = (modeList.size() > i ? ShopItem.Mode.valueOf(modeList.get(i)) : ShopItem.Mode.SELL);
            ShopItem shopItem = null;
            if (itemList.get(i) != null) {
                shopItem = new ShopItem.Builder(itemList.get(i))
                        .price(price)
                        .villagerType(type)
                        .amount(itemList.get(i).getAmount())
                        .mode(mode)
                        .buyLimit(max)
                        .amount(itemList.get(i).getAmount())
                        .build();
            }
            this.itemList.put(i, shopItem);
        }
    }

    /** Abstract Methods */
    protected abstract void buyItem(int slot, Player player);
    protected abstract void sellItem(int slot, Player player);
    protected abstract void shiftFunction(InventoryClickEvent event, ShopItem shopItem);
    public abstract int getAvailable(ShopItem shopItem);

    /** Inventory methods */
    public void openInventory(Player player, ShopMenu shopMenu) {
        switch (shopMenu) {
            case STORAGE:
                player.openInventory(storageMenu);
                break;
            case BUY_SHOP:
                player.openInventory(buyShopMenu);
                break;
            case SHOPFRONT:
                player.openInventory(shopfrontMenu);
                break;
            case SHOPFRONT_DETAILED:
                player.openInventory(shopfrontDetailedMenu);
                break;
            case EDIT_SHOP:
                player.openInventory(editShopMenu);
                break;
            case SELL_SHOP:
                player.openInventory(sellShopMenu);
                break;
            case EDIT_SHOPFRONT:
                player.openInventory(editShopfrontMenu);
                break;
            case EDIT_VILLAGER:
                player.openInventory(editVillagerMenu);
                break;
        }
        Bukkit.getPluginManager().registerEvents(new InventoryClick(player, this, shopMenu), VMPlugin.getInstance());
    }

    /** Runs when customer interacts with shopfront menu */
    public void customerInteract(InventoryClickEvent event) {
        int slot = event.getRawSlot();
        Player player = (Player) event.getWhoClicked();
        ShopItem shopItem = itemList.get(slot);
        if (slot == shopSize - 1) {
            player.playSound(player.getLocation(), Sound.valueOf(mainConfig.getString("sounds.menu_click")), 0.5f, 1);
            if (event.getClick() == ClickType.RIGHT) {
                event.getView().close();
                if (player.getUniqueId().toString().equals(ownerUUID) || (player.hasPermission("villagermarket.admin") && this instanceof AdminShop)) {
                    openInventory(player, ShopMenu.EDIT_SHOP);
                }
            } else {
                openInventory(player, ShopMenu.SHOPFRONT_DETAILED);
            }
        }
        if (shopItem == null) { return; }
        switch (shopItem.getMode()) {
            case BUY:
                sellItem(slot, player);
                break;
            case SELL:
                buyItem(slot, player);
                break;
        }
        if (shopItem.getMode() == ShopItem.Mode.COMMAND && this instanceof AdminShop) {
            AdminShop adminShop = (AdminShop) this;
            adminShop.buyCommand(player, shopItem);
        }

        updateShopInventories();
    }

    /** Runs when owner interacts with edit shop menu */
    public abstract void editShopInteract(Player player, InventoryClickEvent event);

    /** Runs when owner interacts with edit shopfront menu */
    public void itemsInteract(Player player, InventoryClickEvent event) {
        int slot = event.getRawSlot();
        String cancel = mainConfig.getString("cancel");
        ItemStack currentItem = event.getCurrentItem();
        ItemStack cursorItem = event.getCursor();
        ShopItem shopItem = itemList.get(slot);
        if (slot < shopSize && currentItem == null && cursorItem.getType() != Material.AIR) {
            //Add item
            if (slot == -1) return;
            if (Methods.isBlackListed(cursorItem.getType())) {
                player.sendMessage(new Color.Builder().path("messages.blacklisted").addPrefix().build());
            } else {
                player.sendMessage(new Color.Builder().path("messages.type_amount").addPrefix().build());
                player.sendMessage(new Color.Builder().path("messages.type_cancel").replace("%cancel%", cancel).addPrefix().build());

                ShopItem.Builder builder = new ShopItem.Builder(cursorItem)
                        .entityUUID(entityUUID)
                        .villagerType(this instanceof PlayerShop ? VillagerType.PLAYER : VillagerType.ADMIN)
                        .slot(slot);

                Bukkit.getServer().getPluginManager().registerEvents(new ItemDrop(player), VMPlugin.getInstance());
                Bukkit.getServer().getPluginManager().registerEvents(new SetAmount(player, builder), VMPlugin.getInstance());
            }
            event.getView().close();
        } else if (slot < shopSize) {
            event.setCancelled(true);
            //Back
            if (slot == shopSize - 1) {
                player.playSound(player.getLocation(), Sound.valueOf(VMPlugin.getInstance().getConfig().getString("sounds.menu_click")), 0.5f, 1);
                openInventory(player, ShopMenu.EDIT_SHOP);
                return;
            }
            //Quick add or change buy limit
            if (event.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY) {
                shiftFunction(event, shopItem);
                return;
            }
            //Edit
            if (event.getClick() == ClickType.MIDDLE && shopItem.getMode() != ShopItem.Mode.COMMAND) {
                player.sendMessage(new Color.Builder().path("messages.type_amount").addPrefix().build());
                player.sendMessage(new Color.Builder().path("messages.type_cancel").replace("%cancel%", cancel).addPrefix().build());

                ShopItem.Builder builder = new ShopItem.Builder(shopItem.asItemStack(ShopItem.LoreType.ITEM))
                        .entityUUID(entityUUID)
                        .villagerType(this instanceof PlayerShop ? VillagerType.PLAYER : VillagerType.ADMIN)
                        .slot(slot);
                Bukkit.getServer().getPluginManager().registerEvents(new SetAmount(player, builder), VMPlugin.getInstance());
                event.getView().close();
                return;
            }
            //Delete item
            if (event.getClick() == ClickType.RIGHT && currentItem != null) {
                player.playSound(player.getLocation(), Sound.valueOf(VMPlugin.getInstance().getConfig().getString("sounds.remove_item")), 0.5f, 1);
                itemList.remove(slot);
                updateShopInventories();
            }
            //Change mode
            if (event.getClick() == ClickType.LEFT && currentItem != null && shopItem.getMode() != ShopItem.Mode.COMMAND) {
                player.playSound(player.getLocation(), Sound.valueOf(VMPlugin.getInstance().getConfig().getString("sounds.menu_click")), 0.5f, 1);
                shopItem.toggleMode();
                updateShopInventories();
            }
        }
    }

    /** Create new buy shop inventory */
    protected Inventory newBuyShopInventory() {
        return BuyShopMenu.create(this);
    }
    /** Create new edit shop inventory */
    protected abstract Inventory newEditShopInventory();
    /** Create new EditForSale inventory*/
    protected abstract Inventory newShopfrontMenu(Boolean isEditor, ShopItem.LoreType loreType);
    /** Runs when player interacts with change profession menu */
    public void editVillagerInteract(InventoryClickEvent event) {
        Villager villagerObject = (Villager) Bukkit.getEntity(UUID.fromString(entityUUID));
        Player player = (Player) event.getWhoClicked();

        if (event.getRawSlot() > 8) return;
        event.getView().close();
        event.setCancelled(true);

        if (event.getRawSlot() == 8) {
            player.playSound(player.getLocation(), Sound.valueOf(mainConfig.getString("sounds.back")), 0.5f, 1);
            event.getView().close();
            openInventory(player, ShopMenu.EDIT_SHOP);
            return;
        }

        assert villagerObject != null;
        villagerObject.setProfession(Methods.getProfessions().get(event.getRawSlot()));
        player.playSound(player.getLocation(), Sound.valueOf(mainConfig.getString("sounds.change_profession")), 0.5f, 1);
    }

    /** Create new storage inventory */
    protected Inventory newStorageInventory() {
        Inventory inventory = Bukkit.createInventory(null, storageSize, new Color.Builder().path("menus.edit_storage.title").build());

        ArrayList<ItemStack> storage = (ArrayList<ItemStack>) this.config.getList("storage");
        if (storage != null) {
            inventory.setContents(Methods.stacksFromArray(storage));
        }

        MenuItem back = new MenuItem.Builder(Material.valueOf(mainConfig.getString("items.back.material")))
                .nameFromPath("items.back.name")
                .build();
        inventory.setItem(storageSize - 1, back);

        return inventory;
    }
    /** Create new edit villager inventory */
    protected Inventory newEditVillagerInventory() {
        return ProfessionMenu.create();
    }
    /** Create new sell shop inventory */
    protected Inventory newSellShopInventory() {
        return SellShopMenu.create(this);
    }

    /** Sends stats message to Player */
    public void sendStats(Player player) {
        for (String line: shopStats.getStats()) {
            player.sendMessage(line);
        }
    }

    /** Save method */
    public void save() {
        config.set("ownerUUID", ownerUUID);
        config.set("ownerName", ownerName);
        config.set("shopfrontSize", shopSize / 9);
        config.set("storageSize", storageSize / 9);
        config.set("cost", cost);
        config.set("expire", expireDate.getTime());
        config.set("times_rented", timesRented);
        config.set("collected_money", collectedMoney);
        config.set("stats.items_sold", shopStats.getItemsSold());
        config.set("stats.items_bought", shopStats.getItemsBought());
        config.set("stats.money_earned", shopStats.getMoneyEarned());
        config.set("stats.money_spent", shopStats.getMoneySpent());

        ItemStack[] storage = storageMenu.getContents();
        storage[storage.length - 1] = null;
        config.set("storage", storage);

        config.set("items_for_sale", null);
        for (Integer slot : itemList.keySet()) {
            ShopItem shopItem = itemList.get(slot);
            if (shopItem == null) { continue; }
            config.set("items_for_sale." + slot + ".item", shopItem.asItemStack(ShopItem.LoreType.ITEM));
            config.set("items_for_sale." + slot + ".price", shopItem.getPrice());
            config.set("items_for_sale." + slot + ".mode", shopItem.getMode().toString());
            config.set("items_for_sale." + slot + ".buy_limit", shopItem.getLimit());

            HashMap<UUID, Integer> playerLimits = shopItem.getPlayerLimit();
            for (UUID uuid : playerLimits.keySet()) {
                config.set("items_for_sale." + slot + ".limits." + uuid.toString(), playerLimits.get(uuid));
            }
        }
        try {
            config.save(file);
        } catch (IOException i) {}
    }

    /** Remove ItemStack from stock method */
    public void removeFromStock(ItemStack itemStack) {
        storageMenu.removeItem(itemStack);
        updateShopInventories();
    }

    /** Update ForSaleInventory Inventory and ForSaleInventory inventory method*/
    public void updateShopInventories() {
        this.shopfrontMenu.setContents(newShopfrontMenu(false, ShopItem.LoreType.MENU).getContents());
        this.shopfrontDetailedMenu.setContents(newShopfrontMenu(false, ShopItem.LoreType.ITEM).getContents());
        this.editShopfrontMenu.setContents(newShopfrontMenu(true, ShopItem.LoreType.MENU).getContents());
    }

    /** Reload all inventories */
    public void reload() {
        this.mainConfig = VMPlugin.getInstance().getConfig();
        this.buyShopMenu.setContents(newBuyShopInventory().getContents());
        this.editShopMenu.setContents(newEditShopInventory().getContents());
        this.editVillagerMenu.setContents(newEditVillagerInventory().getContents());
        this.sellShopMenu.setContents(newSellShopInventory().getContents());
        updateShopInventories();
    }

    /** Returns amount of ItemStack in storage */
    public int getItemAmount(ItemStack itemStack) {
        return getAmountInventory(itemStack, storageMenu);
    }

    /** Returns amount of ItemStack in specified Inventory */
    protected int getAmountInventory(ItemStack itemStack, Inventory inventory) {
        int amount = 0;
        for (ItemStack storageStack : inventory.getStorageContents()) {
            if (storageStack == null) { continue; }

            if (storageStack.isSimilar(itemStack)) {
                amount = amount + storageStack.getAmount();
            }
        }
        return amount;
    }

    /** Adds shopItem to player's inventory and drops overflowing items */
    protected void giveShopItem(Player player, ShopItem shopItem) {
        ItemStack itemStack = shopItem.asItemStack(ShopItem.LoreType.ITEM);
        HashMap<Integer, ItemStack> itemsLeft = player.getInventory().addItem(itemStack);
        for (int i : itemsLeft.keySet()) {
            player.getLocation().getWorld().dropItemNaturally(player.getLocation(), itemsLeft.get(i));
        }
    }


    /** Getters */
    public String getOwnerUUID() {
        return ownerUUID;
    }
    public int getStorageSize() {
        return storageSize;
    }
    public int getShopSize() {
        return shopSize;
    }
    public Timestamp getExpireDate() {
        return expireDate;
    }
    public String getDuration() {
        return duration;
    }
    public int getCost() {
        return cost;
    }
    public HashMap<Integer, ShopItem> getItemList() {
        return itemList;
    }
    public String getEntityUUID() {
        return entityUUID;
    }
    public int getTimesRented() {
        return timesRented;
    }
    public BigDecimal getCollectedMoney() {
        return collectedMoney;
    }
}
