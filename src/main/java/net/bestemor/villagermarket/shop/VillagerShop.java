package net.bestemor.villagermarket.shop;

import net.bestemor.core.config.ConfigManager;
import net.bestemor.core.menu.Menu;
import net.bestemor.villagermarket.VMPlugin;
import net.bestemor.villagermarket.menu.*;
import net.bestemor.villagermarket.utils.VMUtils;
import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.trait.LookClose;
import net.citizensnpcs.trait.SkinTrait;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.*;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

public abstract class VillagerShop {

    protected UUID entityUUID;

    private final EntityInfo entityInfo;

    protected String duration;
    protected final int seconds;
    protected int timesRented;
    protected Instant expireDate;

    protected final int cost;
    protected BigDecimal collectedMoney = BigDecimal.valueOf(0);

    protected int shopSize;
    protected int storageSize;

    protected File file;
    protected final FileConfiguration config;

    protected final EnumMap<ShopMenu, Menu> menus = new EnumMap<>(ShopMenu.class);

    protected String shopName;

    protected final ShopfrontHolder shopfrontHolder;
    protected ShopStats shopStats;

    protected final VMPlugin plugin;
    private boolean requirePermission = false;

    public enum VillagerType {
        ADMIN,
        PLAYER
    }

    public VillagerShop(VMPlugin plugin, File file) {
        this.plugin = plugin;
        this.file = file;
        this.config = YamlConfiguration.loadConfiguration(file);
        this.entityUUID = UUID.fromString(file.getName().substring(0, file.getName().indexOf('.')));
        this.entityInfo = new EntityInfo(plugin, config, this);

        this.shopSize = config.getInt("shopfrontSize") * 9;
        this.storageSize = config.getInt("storageSize") * 9;
        this.expireDate = Instant.ofEpochMilli(config.getLong("expire"));

        this.shopName = config.getString("shop_name");
        this.shopName = (shopName == null ? entityInfo.getName() : shopName);
        this.duration = config.getString("duration");
        this.duration = (duration == null ? "infinite" : duration);
        this.timesRented = config.getInt("times_rented");
        this.timesRented = (timesRented == 0 ? 1 : timesRented);
        this.seconds = VMUtils.secondsFromString(duration);

        this.cost = config.getInt("cost");
        this.shopStats = new ShopStats(config);

        this.menus.put(ShopMenu.EDIT_SHOP, new EditShopMenu(plugin, this));
        this.menus.put(ShopMenu.EDIT_VILLAGER, new EditVillagerMenu(plugin, this));
        this.menus.put(ShopMenu.SELL_SHOP, new SellShopMenu(plugin, this));

        this.shopfrontHolder = new ShopfrontHolder(plugin, this);
    }

    public void setUUID(UUID uuid) {
        plugin.getShopManager().changeID(entityUUID, uuid);
        entityUUID = uuid;
        file.renameTo(new File(plugin.getDataFolder() + "/Shops/" + uuid + ".yml"));
        this.file = (new File(plugin.getDataFolder() + "/Shops/" + uuid + ".yml"));
    }

    public abstract void buyItem(ShopItem item, int amount, Player player);

    public abstract void sellItem(ShopItem item, int amount, Player player);

    public abstract String getModeCycle(String mode, boolean isItemTrade);

    public abstract int getAvailable(ShopItem shopItem);

    public void updateMenu(ShopMenu shopMenu) {
        menus.get(shopMenu).update();
    }

    public void openInventory(HumanEntity player, ShopMenu shopMenu) {
        this.openInventory((Player) player, shopMenu);
    }

    public void openInventory(Player player, ShopMenu shopMenu) {
        if (shopMenu == ShopMenu.EDIT_SHOPFRONT) {
            shopfrontHolder.open(player, Shopfront.Type.EDITOR);
        } else if (shopMenu == ShopMenu.CUSTOMER) {
            shopfrontHolder.open(player, Shopfront.Type.CUSTOMER);
        } else {
            menus.get(shopMenu).open(player);
        }
    }

    protected void removeItems(Inventory inventory, ItemStack item, int amount) {
        int count = amount;

        for (int i = 0; i < inventory.getContents().length; i++) {
            ItemStack stored = inventory.getItem(i);
            if (VMUtils.compareItems(stored, item)) {
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

    public void setProfession(Villager.Profession profession) {
        Entity entity = VMUtils.getEntity(entityUUID);
        if (plugin.isCitizensEnabled() && CitizensAPI.getNPCRegistry().isNPC(entity)) {
            Entity spawned = plugin.getShopManager().spawnShop(entity.getLocation(), "player");
            spawned.setCustomName(CitizensAPI.getNPCRegistry().getNPC(entity).getName());
            try {
                CitizensAPI.getNPCRegistry().getNPC(entity).destroy();
            } catch (Exception ignore) {
            }
            setUUID(spawned.getUniqueId());
        }

        Villager villagerObject = (Villager) VMUtils.getEntity(entityUUID);
        if (villagerObject != null) {
            villagerObject.setProfession(profession);
        }
    }

    /**
     * Save method
     */
    public void save() {
        config.set("expire", expireDate == null ? 0 : expireDate.toEpochMilli());
        config.set("times_rented", timesRented);
        config.set("collected_money", collectedMoney);
        config.set("require_permission", requirePermission);
        config.set("shop_name", shopName);

        shopStats.save();
        entityInfo.save();

        config.set("items_for_sale", null);
        for (Integer slot : shopfrontHolder.getItemList().keySet()) {
            ShopItem shopItem = shopfrontHolder.getItemList().get(slot);
            if (shopItem == null || slot == null) {
                continue;
            }
            config.set("items_for_sale." + slot + ".item", shopItem.getRawItem());
            config.set("items_for_sale." + slot + ".amount", shopItem.getAmount());
            config.set("items_for_sale." + slot + ".trade_amount", shopItem.getItemTradeAmount());
            config.set("items_for_sale." + slot + ".price", shopItem.isItemTrade() ? shopItem.getItemTrade() : shopItem.getSellPrice(false));
            config.set("items_for_sale." + slot + ".buy_price", shopItem.getBuyPrice(false));
            config.set("items_for_sale." + slot + ".mode", shopItem.getMode().toString());
            config.set("items_for_sale." + slot + ".buy_limit", shopItem.getLimit());
            config.set("items_for_sale." + slot + ".command", shopItem.getCommands());
            config.set("items_for_sale." + slot + ".server_trades", shopItem.getServerTrades());
            config.set("items_for_sale." + slot + ".limit_mode", shopItem.getLimitMode().toString());
            config.set("items_for_sale." + slot + ".cooldown", shopItem.getCooldown());
            config.set("items_for_sale." + slot + ".discount.amount", shopItem.getDiscount());
            config.set("items_for_sale." + slot + ".allow_custom_amount", shopItem.isAllowCustomAmount());
            config.set("items_for_sale." + slot + ".discount.end", shopItem.getDiscountEnd() == null ? 0 : shopItem.getDiscountEnd().getEpochSecond());
            if (shopItem.getNextReset() != null && shopItem.getNextReset().getEpochSecond() != 0 && shopItem.getCooldown() != null) {
                config.set("items_for_sale." + slot + ".next_reset", shopItem.getNextReset().getEpochSecond());
            } else {
                config.set("items_for_sale." + slot + ".next_reset", 0);
            }

            Map<UUID, Integer> playerLimits = shopItem.getPlayerLimits();
            for (UUID uuid : playerLimits.keySet()) {
                if (uuid == null) {
                    continue;
                }
                config.set("items_for_sale." + slot + ".limits." + uuid, playerLimits.get(uuid));
            }
        }
        try {
            config.save(file);
        } catch (IOException i) {
            i.printStackTrace();
        }
    }

    public void closeAllMenus() {
        for (ShopMenu shopMenu : menus.keySet()) {
            menus.get(shopMenu).close();
        }
        this.shopfrontHolder.closeAll();
    }


    /**
     * Adds shopItem to player's inventory and drops overflowing items
     */
    protected void giveShopItem(Player player, ShopItem shopItem, int amount) {
        ItemStack itemStack = shopItem.getRawItem();
        int stacks = (int) Math.floor((double) amount / itemStack.getMaxStackSize());
        int rest = amount - (stacks * itemStack.getMaxStackSize());

        List<ItemStack> itemsLeft = new ArrayList<>();
        for (int stack = 0; stack < stacks; stack++) {
            ItemStack item = itemStack.clone();
            item.setAmount(item.getMaxStackSize());
            itemsLeft.addAll(player.getInventory().addItem(item).values());

        }
        if (rest > 0) {
            ItemStack item = itemStack.clone();
            item.setAmount(rest);
            itemsLeft.addAll(player.getInventory().addItem(item).values());
        }
        for (ItemStack item : itemsLeft) {
            player.getLocation().getWorld().dropItemNaturally(player.getLocation(), item);
        }
    }

    public void setCitizensSkin(String skin) {
        Entity entity = VMUtils.getEntity(entityUUID);
        if (entity != null) {
            String name = entity.getCustomName();
            if (CitizensAPI.getNPCRegistry().isNPC(entity)) {
                name = CitizensAPI.getNPCRegistry().getNPC(entity).getName();
            }
            if (name == null || name.isBlank()) {
                name = entity.getName();
            }

            NPC npc = CitizensAPI.getNPCRegistry().createNPC(EntityType.PLAYER, name);
            npc.spawn(entity.getLocation());
            npc.getOrAddTrait(LookClose.class).lookClose(true);
            npc.getOrAddTrait(SkinTrait.class).setSkinName(skin);

            if (CitizensAPI.getNPCRegistry().isNPC(entity) && CitizensAPI.getNPCRegistry().getNPC(entity).isSpawned()) {
                CitizensAPI.getNPCRegistry().getNPC(entity).destroy();
            } else {
                entity.remove();
            }
            setUUID(npc.getEntity().getUniqueId());
        }

    }

    public void sendStats(Player player) {
        for (String line : shopStats.getStats()) {
            player.sendMessage(line);
        }
    }

    public int getStorageSize() {
        return storageSize;
    }

    public int getShopSize() {
        return shopSize;
    }

    public Instant getExpireDate() {
        return expireDate;
    }

    public String getDuration() {
        return duration;
    }

    public int getCost() {
        return cost;
    }

    public UUID getEntityUUID() {
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

    public FileConfiguration getConfig() {
        return config;
    }

    public int getSeconds() {
        return seconds;
    }

    public File getFile() {
        return file;
    }

    public void setStorageSize(int storageSize) {
        this.storageSize = storageSize;
        this.config.set("storageSize", storageSize);
        try {
            this.config.save(this.file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String getShopName() {
        return shopName;
    }

    public void setShopName(String customName) {
        Entity entity = Bukkit.getEntity(entityUUID);
        if (plugin.isCitizensEnabled() && CitizensAPI.getNPCRegistry().isNPC(entity)) {
            CitizensAPI.getNPCRegistry().getNPC(entity).setName(customName);
        } else if (entity != null) {
            entity.setCustomName(customName);
        }
        shopName = customName;
        shopfrontHolder.closeAll();
        shopfrontHolder.load();
    }

    public void checkDiscounts() {
        for (ShopItem shopItem : shopfrontHolder.getItemList().values()) {
            if (shopItem.getDiscount() > 0 && Instant.now().isAfter(shopItem.getDiscountEnd())) {
                shopItem.setDiscount(0, null);
            }
        }
    }

    public void addRandomDiscount(int discount, Instant end) {
        List<ShopItem> items = new ArrayList<>(shopfrontHolder.getItemList().values());
        Collections.shuffle(items);
        for (ShopItem shopItem : items) {
            if (shopItem.getDiscount() == 0) {
                if (ConfigManager.getBoolean("auto_discount.announce")) {
                    Bukkit.broadcastMessage(ConfigManager.getMessage("messages.discount_announcement")
                            .replace("%shop_name%", getShopName())
                            .replace("%item%", shopItem.getItemName())
                            .replace("%discount%", String.valueOf(discount))
                    );
                }
                shopItem.setDiscount(discount, end);
                break;
            }
        }
    }

    public void setShopfrontSize(int shopSize) {
        this.shopSize = shopSize;
        this.config.set("shopfrontSize", shopSize);
        try {
            this.config.save(this.file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void setRequirePermission(boolean b) {
        this.requirePermission = b;
    }

    public boolean isRequirePermission() {
        return requirePermission;
    }
}
