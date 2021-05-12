package me.bestem0r.villagermarket.shops;

import me.bestem0r.villagermarket.EntityInfo;
import me.bestem0r.villagermarket.VMPlugin;
import me.bestem0r.villagermarket.events.InventoryClick;
import me.bestem0r.villagermarket.events.ItemDrop;
import me.bestem0r.villagermarket.events.dynamic.SetAmount;
import me.bestem0r.villagermarket.inventories.*;
import me.bestem0r.villagermarket.items.ShopItem;
import me.bestem0r.villagermarket.utilities.ColorBuilder;
import me.bestem0r.villagermarket.utilities.Methods;
import me.bestem0r.villagermarket.utilities.ShopStats;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
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
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public abstract class VillagerShop {

    protected String ownerUUID;
    protected String ownerName;
    protected String entityUUID;
    private final EntityInfo entityInfo;

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

    protected Inventory buyShopInv;
    protected Inventory editShopInv;
    protected List<Inventory> storageInventories;

    protected final ShopfrontHolder shopfrontHolder;

    protected Inventory editVillagerInv;
    protected Inventory sellShopInv;

    protected ShopStats shopStats;

    protected final VMPlugin plugin;

    public enum VillagerType {
        ADMIN,
        PLAYER
    }

    public VillagerShop(VMPlugin plugin, File file) {
        this.plugin = plugin;
        this.mainConfig = plugin.getConfig();
        this.file = file;
        this.config = YamlConfiguration.loadConfiguration(file);
        this.entityUUID = file.getName().substring(0, file.getName().indexOf('.'));
        this.entityInfo = new EntityInfo(plugin, config, UUID.fromString(entityUUID));

        this.ownerUUID = config.getString("ownerUUID");
        this.ownerName = config.getString("ownerName");

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
        this.shopStats = new ShopStats(plugin, config);

        List<ItemStack> storage = (List<ItemStack>) this.config.getList("storage");
        storage = (storage == null ? new ArrayList<>() : storage);

        this.storageInventories = new StorageBuilder(plugin, storageSize, storage).create();
        buildItemList();
        this.buyShopInv = BuyShop.create(plugin, this);
        this.editShopInv = EditShop.create(plugin, this);

        this.shopfrontHolder = new ShopfrontHolder(plugin, this);
        shopfrontHolder.load();

        this.editVillagerInv = EditVillager.create(plugin);
        this.sellShopInv = SellShop.create(plugin, this);

    }

    /** Populates ItemList of shop items */
    private void buildItemList() {
        if (config.getList("for_sale") != null) {
            Bukkit.getLogger().warning("[VillagerMarket] Old shop file detected! Converting: " + entityUUID);
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
            ShopItem shopItem = new ShopItem.Builder(plugin, itemStack)
                    .price(new BigDecimal(config.getString("items_for_sale." + slot + ".price")))
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
            if (itemList.get(i) != null) {
                ShopItem shopItem = new ShopItem.Builder(plugin, itemList.get(i))
                        .price(BigDecimal.valueOf(price))
                        .villagerType(type)
                        .amount(itemList.get(i).getAmount())
                        .mode(mode)
                        .buyLimit(max)
                        .amount(itemList.get(i).getAmount())
                        .build();
                this.itemList.put(i, shopItem);
            }
        }
    }

    public void changeUUID(UUID uuid) {
        entityUUID = uuid.toString();
        file.renameTo(new File(plugin.getDataFolder() + "/Shops/" + uuid + ".yml"));
        this.file = (new File(plugin.getDataFolder() + "/Shops/" + uuid + ".yml"));
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
                if (storageSize == 0) {
                    storageInventories = new StorageBuilder(plugin, 0, filteredStorage()).create();
                }
                player.openInventory(storageInventories.get(0));
                break;
            case BUY_SHOP:
                player.openInventory(buyShopInv);
                break;
            case EDIT_SHOP:
                player.openInventory(editShopInv);
                break;
            case SELL_SHOP:
                player.openInventory(sellShopInv);
                break;
            case EDIT_VILLAGER:
                player.openInventory(editVillagerInv);
                break;
        }
        Bukkit.getPluginManager().registerEvents(new InventoryClick(player, this, shopMenu), plugin);
    }

    /** Runs when customer interacts with shopfront menu */
    public void customerInteract(InventoryClickEvent event, int slot) {
        Player player = (Player) event.getWhoClicked();
        ShopItem shopItem = itemList.get(slot);

        if (shopItem == null) { return; }
        event.setCancelled(true);
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
        shopfrontHolder.update();
    }

    protected List<ItemStack> filteredStorage() {
        List<ItemStack> storage = new ArrayList<>();
        NamespacedKey guiKey = new NamespacedKey(plugin, "vm-gui-item");
        for (Inventory storageMenu : storageInventories) {
            for (ItemStack item : storageMenu.getContents()) {
                if (item == null) {
                    storage.add(null);
                } else {
                    ItemMeta meta = item.getItemMeta();
                    PersistentDataContainer container = meta.getPersistentDataContainer();
                    if (container.has(guiKey, PersistentDataType.STRING) || container.has(guiKey, PersistentDataType.INTEGER)) {
                        continue;
                    }
                    storage.add(item);
                }
            }
        }
        while (storage.size() != 0 && storage.get(storage.size() - 1) == null) {
            storage.remove(storage.size() - 1);
        }
        return storage;
    }
    protected void removeItems(Inventory inventory, ItemStack item) {
        int count = item.getAmount();
        for (int i = 0; i < inventory.getContents().length; i ++) {
            ItemStack stored = inventory.getItem(i);
            if (Methods.compareItems(stored, item)) {
                if (stored.getAmount() > count) {
                    stored.setAmount(stored.getAmount() - count);
                    break;
                } else {
                    count -= stored.getAmount();
                    inventory.setItem(i, null);
                }
            }
        }
    }

    /** Runs when owner interacts with edit shop menu */
    public abstract void editShopInteract(InventoryClickEvent event);

    /** Runs when owner interacts with edit shopfront menu */
    public void editorInteract(InventoryClickEvent event, int slot) {
        Player player = (Player) event.getWhoClicked();
        String cancel = mainConfig.getString("cancel");
        ItemStack currentItem = event.getCurrentItem();
        ItemStack cursorItem = event.getCursor();
        ShopItem shopItem = itemList.get(slot);

        if (currentItem == null && cursorItem.getType() != Material.AIR) {
            //Add item
            if (Methods.isBlackListed(plugin, cursorItem.getType())) {
                player.sendMessage(new ColorBuilder(plugin).path("messages.blacklisted").addPrefix().build());
            } else {
                player.sendMessage(new ColorBuilder(plugin).path("messages.type_amount").addPrefix().build());
                player.sendMessage(new ColorBuilder(plugin).path("messages.type_cancel").replace("%cancel%", cancel).addPrefix().build());

                ShopItem.Builder builder = new ShopItem.Builder(plugin, cursorItem)
                        .entityUUID(entityUUID)
                        .villagerType(this instanceof PlayerShop ? VillagerType.PLAYER : VillagerType.ADMIN)
                        .slot(slot);

                Bukkit.getServer().getPluginManager().registerEvents(new ItemDrop(player), plugin);
                Bukkit.getServer().getPluginManager().registerEvents(new SetAmount(plugin, player, builder), plugin);
            }
            event.getView().close();
        } else {
            if (shopItem == null) { return; }

            //Quick add or change buy limit
            if (event.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY) {
                shiftFunction(event, shopItem);
                return;
            }
            //Edit
            if (event.getClick() == ClickType.MIDDLE && shopItem.getMode() != ShopItem.Mode.COMMAND) {
                player.sendMessage(new ColorBuilder(plugin).path("messages.type_amount").addPrefix().build());
                player.sendMessage(new ColorBuilder(plugin).path("messages.type_cancel").replace("%cancel%", cancel).addPrefix().build());

                ShopItem.Builder builder = new ShopItem.Builder(plugin, shopItem.asItemStack(ShopItem.LoreType.ITEM))
                        .entityUUID(entityUUID)
                        .villagerType(this instanceof PlayerShop ? VillagerType.PLAYER : VillagerType.ADMIN)
                        .slot(slot);
                Bukkit.getServer().getPluginManager().registerEvents(new SetAmount(plugin, player, builder), plugin);
                event.getView().close();
                return;
            }
            //Delete item
            if (event.getClick() == ClickType.RIGHT) {
                player.playSound(player.getLocation(), Sound.valueOf(plugin.getConfig().getString("sounds.remove_item")), 1, 1);
                itemList.remove(slot);
                shopfrontHolder.update();
            }
            //Change mode
            if (event.getClick() == ClickType.LEFT && shopItem.getMode() != ShopItem.Mode.COMMAND) {
                player.playSound(player.getLocation(), Sound.valueOf(plugin.getConfig().getString("sounds.menu_click")), 1, 1);
                shopItem.toggleMode();
                shopfrontHolder.update();
            }
        }
    }

    /** Runs when player interacts with change profession menu */
    public void editVillagerInteract(InventoryClickEvent event) {
        Villager villagerObject = (Villager) Bukkit.getEntity(UUID.fromString(entityUUID));
        Player player = (Player) event.getWhoClicked();

        if (event.getRawSlot() > 17) return;
        event.setCancelled(true);

        if (event.getRawSlot() == 17) {
            player.playSound(player.getLocation(), Sound.valueOf(mainConfig.getString("sounds.back")), 0.5f, 1);
            openInventory(player, ShopMenu.EDIT_SHOP);
            return;
        }

        assert villagerObject != null;
        event.getView().close();
        villagerObject.setProfession(Villager.Profession.values()[event.getRawSlot()]);
        player.playSound(player.getLocation(), Sound.valueOf(mainConfig.getString("sounds.change_profession")), 0.5f, 1);
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
        entityInfo.save();

        config.set("storage", filteredStorage());

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
        } catch (IOException i) {
            i.printStackTrace();
        }
    }

    /** Remove ItemStack from stock method */
    public void removeFromStock(ItemStack itemStack) {
        for (Inventory storageMenu : storageInventories) {
            if (storageMenu.containsAtLeast(itemStack, itemStack.getAmount())) {
                storageMenu.removeItem(itemStack);
                break;
            }
        }
       shopfrontHolder.update();
    }

    /** Reload all inventories */
    public void reload() {
        this.mainConfig = plugin.getConfig();
        this.buyShopInv.setContents(BuyShop.create(plugin, this).getContents());
        this.editShopInv.setContents(EditShop.create(plugin, this).getContents());
        this.editVillagerInv.setContents(EditVillager.create(plugin).getContents());
        this.sellShopInv.setContents(SellShop.create(plugin, this).getContents());
        shopfrontHolder.reload();
    }

    /** Returns amount of ItemStack in storage */
    public int getAmountInStorage(ItemStack itemStack) {
        int amount = 0;
        for (Inventory storageMenu : storageInventories) {
            amount += getAmountInventory(itemStack, storageMenu);
        }
        return amount;
    }

    /** Returns amount of ItemStack in specified Inventory */
    protected int getAmountInventory(ItemStack itemStack, Inventory inventory) {
        int amount = 0;
        for (ItemStack storageStack : inventory.getContents()) {
            if (storageStack == null) { continue; }

            if (Methods.compareItems(storageStack, itemStack)) {
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

    public void setCitizensSkin(String skin) {
        Citize
    }

    public boolean hasOwner() {
        return !(this instanceof AdminShop) && !ownerUUID.equals("null");
    }

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
    public ShopfrontHolder getShopfrontHolder() {
        return shopfrontHolder;
    }
    public EntityInfo getEntityInfo() {
        return entityInfo;
    }
    public String getOwnerName() {
        return ownerName;
    }
}
