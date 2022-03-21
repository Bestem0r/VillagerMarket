package net.bestemor.villagermarket.shop;

import net.bestemor.core.config.ConfigManager;
import net.bestemor.villagermarket.VMPlugin;
import net.bestemor.villagermarket.event.AbandonShopEvent;
import net.bestemor.villagermarket.menu.BuyShopMenu;
import net.bestemor.villagermarket.menu.StorageHolder;
import net.citizensnpcs.api.CitizensAPI;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

public class PlayerShop extends VillagerShop {

    private final StorageHolder storageHolder;
    private final List<String> trustedPlayers;

    protected UUID ownerUUID;
    protected String ownerName;

    public PlayerShop(VMPlugin plugin, File file) {
        super(plugin, file);
        super.collectedMoney = BigDecimal.valueOf(config.getDouble("collected_money"));

        final List<ItemStack> items = new ArrayList<>();
        List<?> storage = getConfig().getList("storage");
        if (storage != null) {
            items.addAll((List<ItemStack>) storage);
        }
        this.storageHolder = new StorageHolder(plugin, this);
        this.storageHolder.loadItems(items);

        this.menus.put(ShopMenu.BUY_SHOP, new BuyShopMenu(plugin, this));

        String uuidString = config.getString("ownerUUID");
        String nameString = config.getString("ownerName");
        this.ownerUUID = uuidString == null || uuidString.equals("admin_shop") || uuidString.equals("null") ? null : UUID.fromString(uuidString);
        this.ownerName = nameString == null || nameString.equals("admin_shop") || nameString.equals("null") ? null : nameString;

        this.trustedPlayers = config.getStringList("trusted");

        updateRedstone(false);
        shopfrontHolder.load();
    }

    public void updateRedstone(boolean forceOff) {
        if (!plugin.getShopManager().isRedstoneEnabled() || cost == -1) { return; }
        Entity entity = Bukkit.getEntity(entityUUID);

        if (entity != null) {
            Location location = entity.getLocation();
            Material standingOn = entity.getLocation().clone().subtract(0, 1, 0).getBlock().getType();
            boolean isOnPiston = (standingOn == Material.PISTON_HEAD || standingOn == Material.MOVING_PISTON);

            Block replace = location.subtract(0, (isOnPiston ? 3 : 2), 0).getBlock();
            replace.setType(ownerUUID == null || forceOff ? Material.AIR : Material.REDSTONE_BLOCK);
        }
    }

    public void openStorage(Player player) {
        storageHolder.open(player);
    }
    public StorageHolder getStorageHolder() {
        return storageHolder;
    }

    /** Buy item from the shop as the customer */
    @Override
    protected void buyItem(int slot, Player player) {
        Economy economy = plugin.getEconomy();
        ShopItem shopItem = shopfrontHolder.getItemList().get(slot);

        BigDecimal price = shopItem.getPrice();
        int amount = shopItem.getAmount();
        OfflinePlayer owner = Bukkit.getOfflinePlayer(ownerUUID);
        if (!shopItem.verifyPurchase(player, owner, storageHolder)) {
            return;
        }

        if (shopItem.isItemTrade()) {
            removeItems(player.getInventory(), shopItem.getItemTrade());
            storageHolder.addItem(shopItem.getItemTrade());

            if (owner.isOnline() && owner.getPlayer() != null) {
                Player ownerOnline = owner.getPlayer();
                ownerOnline.sendMessage(ConfigManager.getCurrencyBuilder("messages.sold_item_as_owner")
                        .replace("%price%", shopItem.getItemTrade().getAmount() + "x " + " " + shopItem.getItemTradeName())
                        .replace("%player%", player.getName())
                        .replace("%amount%", String.valueOf(amount))
                        .replace("%item%", shopItem.getItemName()).addPrefix().build());
            }
        } else {
            BigDecimal tax = BigDecimal.valueOf(ConfigManager.getDouble("tax"));
            BigDecimal taxAmount = tax.divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP).multiply(price);

            depositOwner(price.subtract(taxAmount));
            economy.withdrawPlayer(player, shopItem.getPrice().doubleValue());
            shopStats.addEarned(price.doubleValue());

            if (owner.isOnline() && owner.getPlayer() != null) {
                Player ownerOnline = owner.getPlayer();
                ConfigManager.CurrencyBuilder builder = ConfigManager.getCurrencyBuilder("messages.sold_item_as_owner")
                        .replace("%player%", player.getName())
                        .replace("%amount%", String.valueOf(amount))
                        .replace("%item%", shopItem.getItemName()).addPrefix();

                if (price.equals(BigDecimal.ZERO)) {
                    builder.replace("%price%", ConfigManager.getString("quantity.free"));
                } else {
                    builder.replaceCurrency("%price%", price);
                }
                ownerOnline.sendMessage(builder.build());

                if (taxAmount.doubleValue() > 0) {
                    BigDecimal t = new BigDecimal(String.valueOf(taxAmount));
                    ownerOnline.sendMessage(ConfigManager.getCurrencyBuilder("messages.tax").replaceCurrency("%tax%", t).addPrefix().build());
                }
            }

            BigDecimal left = BigDecimal.valueOf(economy.getBalance(player));
            player.sendMessage(ConfigManager.getCurrencyBuilder("messages.money_left").replaceCurrency("%amount%", left).addPrefix().build());
        }

        shopStats.addSold(amount);

        giveShopItem(player, shopItem);
        storageHolder.removeItem(shopItem.getRawItem());

        player.playSound(player.getLocation(), ConfigManager.getSound("sounds.buy_item"), 1, 1);
        VMPlugin.log.add(new Date() + ": " + player.getName() + " bought " + amount + "x " + shopItem.getType() + " from " + ownerName + " (" + price + ")");
    }

    /** Sell item to the shop as the customer */
    @Override
    protected void sellItem(int slot, Player player) {
        ShopItem shopItem = shopfrontHolder.getItemList().get(slot);
        Economy economy = plugin.getEconomy();
        OfflinePlayer owner = Bukkit.getOfflinePlayer(ownerUUID);

        BigDecimal tax = BigDecimal.valueOf(ConfigManager.getDouble("tax"));
        BigDecimal price = shopItem.getPrice();
        BigDecimal taxAmount = tax.divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP).multiply(price);

        int amount = shopItem.getAmount();
        if (!shopItem.verifyPurchase(player, Bukkit.getOfflinePlayer(ownerUUID), storageHolder)) {
            return;
        }

        removeItems(player.getInventory(), shopItem.getRawItem());
        economy.depositPlayer(player, price.subtract(taxAmount).doubleValue());

        BigDecimal moneyLeft = BigDecimal.valueOf(economy.getBalance(player));
        storageHolder.addItem(shopItem.getRawItem());
        shopStats.addBought(amount);
        shopStats.addSpent(price.doubleValue());

        player.playSound(player.getLocation(), ConfigManager.getSound("sounds.sell_item"), 0.5f, 1);
        player.sendMessage(ConfigManager.getCurrencyBuilder("messages.money_currently").replaceCurrency("%amount%", moneyLeft).build());
        
        if (taxAmount.doubleValue() > 0) {
            player.sendMessage(ConfigManager.getCurrencyBuilder("messages.tax").replaceCurrency("%tax%", taxAmount).addPrefix().build());
        }

        economy.withdrawPlayer(owner, price.doubleValue());
        if (owner.isOnline() && owner.getPlayer() != null) {
            Player ownerOnline = owner.getPlayer();
            ConfigManager.CurrencyBuilder builder =ConfigManager.getCurrencyBuilder("messages.bought_item_as_owner")
                    .replace("%player%", player.getName())
                    .replace("%amount%", String.valueOf(amount))
                    .replace("%item%", shopItem.getType().name().replaceAll("_", " ").toLowerCase()).addPrefix();

            if (price.equals(BigDecimal.ZERO)) {
                builder.replace("%price%", ConfigManager.getString("quantity.free"));
            } else {
                builder.replaceCurrency("%price%", price);
            }
            ownerOnline.sendMessage(builder.build());
        }
        VMPlugin.log.add(new Date() + ": " + player.getName() + " sold " + amount + "x " + shopItem.getType() + " to " + ownerName + " (" + price + ")");
    }

    @Override
    public String getModeCycle(String mode, boolean isItemTrade) {
        return ConfigManager.getString("menus.edit_item.mode_cycle.player_shop." + (!isItemTrade ? mode : "item_trade"));
    }

    /** Collects money */
    public void collectMoney(Player player) {
        Economy economy = plugin.getEconomy();
        economy.depositPlayer(player, collectedMoney.doubleValue());
        player.sendMessage(ConfigManager.getCurrencyBuilder("messages.collected_money").replaceCurrency("%amount%", collectedMoney).addPrefix().build());
        player.playSound(player.getLocation(), ConfigManager.getSound("sounds.collect_money"), 1, 1);
        super.collectedMoney = BigDecimal.valueOf(0);
        super.updateMenu(ShopMenu.EDIT_SHOP);
    }

    /** Deposits money to the Shop/Owner */
    public void depositOwner(BigDecimal amount) {
        if (ConfigManager.getBoolean("require_collect")) {
            super.collectedMoney = collectedMoney.add(amount);
            super.updateMenu(ShopMenu.EDIT_SHOP);
        } else {
            Economy economy = plugin.getEconomy();
            economy.depositPlayer(Bukkit.getOfflinePlayer(ownerUUID), amount.doubleValue());
        }
    }

    /** Returns how many more of an certain ShopItem the owner wants to buy */
    @Override
    public int getAvailable(ShopItem shopItem) {
        Economy economy = plugin.getEconomy();
        OfflinePlayer owner = ownerUUID == null ? null : Bukkit.getOfflinePlayer(ownerUUID);

        ItemStack i = shopItem.isItemTrade() ? shopItem.getItemTrade() : shopItem.getRawItem();
        int available = storageHolder.getAvailableSpace(i);

        if (shopItem.getLimit() != 0) {
            int inStorage = storageHolder.getAmount(i);
            available = Math.min(shopItem.getLimit() - inStorage, available);
        }

        if (owner != null) {
            available = Math.max(0, Math.min(available, (int) Math.floor(economy.getBalance(owner) / shopItem.getPrice().doubleValue()) * shopItem.getAmount()));
        }

        if (shopItem.isItemTrade()) {
            int inStorage = storageHolder.getAmount(shopItem.getRawItem());
            available = Math.min(inStorage, available);
            available = available < shopItem.getItemTrade().getAmount() ? 0 : available;
        } else {
            available = available < shopItem.getAmount() ? 0 : available;
        }
        return available;
    }

    public void abandon() {
        Bukkit.getPluginManager().callEvent(new AbandonShopEvent(this));

        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(ownerUUID);
        Economy economy = plugin.getEconomy();

        if (cost != -1) { economy.depositPlayer(offlinePlayer, ((double) getCost() * (ConfigManager.getDouble("refund_percent") / 100)) * timesRented); }
        economy.depositPlayer(offlinePlayer, collectedMoney.doubleValue());

        List<ItemStack> storage = storageHolder.getItems();
        if (!storage.isEmpty()) {
            if (plugin.getShopManager().getExpiredStorages().containsKey(ownerUUID)) {
                plugin.getShopManager().getExpiredStorages().get(ownerUUID).addAll(storage);
            } else {
                plugin.getShopManager().getExpiredStorages().put(ownerUUID, storage);
            }
        }

        Entity entity = Bukkit.getEntity(entityUUID);
        if (entity != null) {
            if (plugin.isCitizensEnabled() && CitizensAPI.getNPCRegistry().isNPC(entity)) {
                CitizensAPI.getNPCRegistry().getNPC(entity).setName(ConfigManager.getString("villager.name_available"));
            } else if (entity instanceof Villager) {
                entity.setCustomName(ConfigManager.getString("villager.name_available"));
            }
            setProfession(Villager.Profession.NONE);
        }


        if (offlinePlayer.isOnline()) {
            Player player = (Player) offlinePlayer;
            player.playSound(player.getLocation(), ConfigManager.getSound("sounds.expired"), 1, 1);
            player.sendMessage(ConfigManager.getMessage("messages.expired"));
        }

        storageHolder.clear();
        trustedPlayers.clear();
        shopfrontHolder.closeAll();
        shopfrontHolder.clear();
        super.collectedMoney = BigDecimal.ZERO;
        this.ownerUUID = null;
        this.ownerName = null;
        super.shopStats = new ShopStats(config);
        super.timesRented = 0;
        super.updateMenu(ShopMenu.EDIT_SHOP);

        VMPlugin.log.add(new Date() + ": " + offlinePlayer.getName() + " abandoned shop: " + entityUUID.toString());
        updateRedstone(false);
    }

    /** Increase rent time */
    public void increaseTime() {
        super.expireDate = expireDate.plusSeconds(seconds);
        this.timesRented ++;
    }

    public boolean hasOwner() {
        return ownerUUID != null;
    }
    public String getOwnerName() {
        return ownerName;
    }
    public UUID getOwnerUUID() {
        return ownerUUID;
    }

    /** Returns true if rent has expired, false if not */
    public boolean hasExpired() {
        return seconds != 0 && expireDate != null && expireDate.isBefore(Instant.now());
    }

    @Override
    public void save() {
        config.set("storage", storageHolder.getItems());
        config.set("trusted", trustedPlayers);

        config.set("ownerUUID", ownerUUID == null ? null : ownerUUID.toString());
        config.set("ownerName", ownerName);

        super.save();
    }

    /** Sets owner and changes name */
    public void setOwner(Player player) {
        Entity villager = Bukkit.getEntity(entityUUID);
        String name = ConfigManager.getString("villager.name_taken").replace("%player%", player.getName());
        this.expireDate = (seconds == 0 ? null : Instant.now().plusSeconds(seconds));

        if (plugin.isCitizensEnabled() && CitizensAPI.getNPCRegistry().isNPC(villager)) {
            CitizensAPI.getNPCRegistry().getNPC(villager).setName(name);
        } else {
            villager.setCustomName(name);
        }
        this.ownerUUID = player.getUniqueId();
        this.ownerName = player.getName();
    }

    /** Adds trusted to the trusted list */
    public void addTrusted(Player player) {
        trustedPlayers.add(player.getUniqueId().toString());
    }
    /** Removes trusted from the trusted list */
    public void removeTrusted(Player player) {
        trustedPlayers.remove(player.getUniqueId().toString());
    }
    /** Returns true if player is trusted, false if not */
    public boolean isTrusted(Player player) {
        return trustedPlayers.contains(player.getUniqueId().toString());
    }
}
