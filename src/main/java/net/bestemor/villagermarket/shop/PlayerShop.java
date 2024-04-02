package net.bestemor.villagermarket.shop;

import net.bestemor.core.config.ConfigManager;
import net.bestemor.core.config.CurrencyBuilder;
import net.bestemor.core.config.VersionUtils;
import net.bestemor.villagermarket.VMPlugin;
import net.bestemor.villagermarket.event.AbandonShopEvent;
import net.bestemor.villagermarket.event.interact.BuyShopItemsEvent;
import net.bestemor.villagermarket.event.interact.SellShopItemsEvent;
import net.bestemor.villagermarket.event.interact.TradeShopItemsEvent;
import net.bestemor.villagermarket.menu.BuyShopMenu;
import net.bestemor.villagermarket.menu.StorageHolder;
import net.bestemor.villagermarket.utils.VMUtils;
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
import java.util.stream.Collectors;

public class PlayerShop extends VillagerShop {

    private final StorageHolder storageHolder;
    private final List<String> trustedPlayers;

    protected UUID ownerUUID;
    protected String ownerName;
    private boolean disableNotifications = false;

    public PlayerShop(VMPlugin plugin, File file) {
        super(plugin, file);
        super.collectedMoney = BigDecimal.valueOf(config.getDouble("collected_money"));

        String uuidString = config.getString("ownerUUID");
        String nameString = config.getString("ownerName");
        this.ownerUUID = uuidString == null || uuidString.equals("admin_shop") || uuidString.equals("null") ? null : UUID.fromString(uuidString);
        this.ownerName = nameString == null || nameString.equals("admin_shop") || nameString.equals("null") ? null : nameString;
        this.disableNotifications = config.getBoolean("disable_notifications");

        if (ownerUUID != null) {
            OfflinePlayer owner = Bukkit.getOfflinePlayer(ownerUUID);
            this.ownerName = owner.getName();
        }

        final List<ItemStack> items = new ArrayList<>();
        List<?> storage = getConfig().getList("storage");
        if (storage != null) {
            items.addAll((List<ItemStack>) storage);
        }
        this.storageHolder = new StorageHolder(plugin, this);
        this.storageHolder.loadItems(items);

        this.menus.put(ShopMenu.BUY_SHOP, new BuyShopMenu(plugin, this));

        this.trustedPlayers = config.getStringList("trusted");

        updateRedstone(false);

        Bukkit.getScheduler().runTask(plugin, () -> {
            Entity entity = VMUtils.getEntity(entityUUID);
            setShopName(entity == null ? null : entity.getCustomName());
            shopfrontHolder.load();
            isLoaded = true;
        });
    }

    public void updateRedstone(boolean forceOff) {
        if (!plugin.getShopManager().isRedstoneEnabled() || cost == -1) { return; }
        Entity entity = VMUtils.getEntity(entityUUID);

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

        BigDecimal price = shopItem.getSellPrice();
        int amount = shopItem.getAmount();
        OfflinePlayer owner = Bukkit.getOfflinePlayer(ownerUUID);
        if (!shopItem.verifyPurchase(player, ItemMode.SELL, owner, storageHolder)) {
            return;
        }

        if (shopItem.isItemTrade()) {
            TradeShopItemsEvent tradeShopItemsEvent = new TradeShopItemsEvent(player,this, shopItem);
            Bukkit.getPluginManager().callEvent(tradeShopItemsEvent);
            if (tradeShopItemsEvent.isCancelled()) {
                return;
            }
            removeItems(player.getInventory(), shopItem.getItemTrade(), shopItem.getItemTradeAmount());
            storageHolder.addItem(shopItem.getItemTrade(), shopItem.getItemTradeAmount());

            if (owner.isOnline() && owner.getPlayer() != null && !disableNotifications) {
                Player ownerOnline = owner.getPlayer();
                ownerOnline.sendMessage(ConfigManager.getCurrencyBuilder("messages.sold_item_as_owner")
                        .replace("%price%", shopItem.getItemTradeAmount() + "x " + " " + shopItem.getItemTradeName())
                        .replace("%player%", player.getName())
                        .replace("%amount%", String.valueOf(amount))
                        .replace("%item%", shopItem.getItemName()).addPrefix().build());
            }
        } else {
            BuyShopItemsEvent buyShopItemsEvent = new BuyShopItemsEvent(player, this,shopItem);
            Bukkit.getPluginManager().callEvent(buyShopItemsEvent);
            if (buyShopItemsEvent.isCancelled()) {
                return;
            }
            BigDecimal tax = BigDecimal.valueOf(ConfigManager.getDouble("tax"));
            BigDecimal taxAmount = tax.divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP).multiply(price);

            depositOwner(price.subtract(taxAmount));
            economy.withdrawPlayer(player, shopItem.getSellPrice().doubleValue());
            shopStats.addEarned(price.doubleValue());

            if (owner.isOnline() && owner.getPlayer() != null) {
                Player ownerOnline = owner.getPlayer();
                CurrencyBuilder builder = ConfigManager.getCurrencyBuilder("messages.sold_item_as_owner")
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
        storageHolder.removeItem(shopItem.getRawItem(), shopItem.getAmount());

        CurrencyBuilder message = ConfigManager.getCurrencyBuilder("messages.bought_item_as_customer")
                .replace("%amount%", String.valueOf(shopItem.getAmount()))
                .replace("%item%", shopItem.getItemName())
                .replace("%shop%", getShopName())
                .addPrefix();

        if (shopItem.isItemTrade()) {
            message.replace("%price%", shopItem.getItemTradeAmount() + "x " + shopItem.getItemTradeName());
        } else {
            message.replaceCurrency("%price%", price);
        }
        player.sendMessage(message.build());

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
        BigDecimal price = shopItem.getBuyPrice();
        BigDecimal taxAmount = tax.divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP).multiply(price);

        int amount = shopItem.getAmount();
        if (!shopItem.verifyPurchase(player, ItemMode.BUY, Bukkit.getOfflinePlayer(ownerUUID), storageHolder)) {
            return;
        }
        SellShopItemsEvent sellShopItemsEvent = new SellShopItemsEvent(player,this, shopItem);
        Bukkit.getPluginManager().callEvent(sellShopItemsEvent);
        if (sellShopItemsEvent.isCancelled()) {
            return;
        }

        removeItems(player.getInventory(), shopItem.getRawItem(), shopItem.getAmount());
        economy.depositPlayer(player, price.subtract(taxAmount).doubleValue());

        BigDecimal moneyLeft = BigDecimal.valueOf(economy.getBalance(player));
        storageHolder.addItem(shopItem.getRawItem(), shopItem.getAmount());
        shopStats.addBought(amount);
        shopStats.addSpent(price.doubleValue());

        player.playSound(player.getLocation(), ConfigManager.getSound("sounds.sell_item"), 0.5f, 1);
        player.sendMessage(ConfigManager.getCurrencyBuilder("messages.money_currently").replaceCurrency("%amount%", moneyLeft).build());
        
        if (taxAmount.doubleValue() > 0) {
            player.sendMessage(ConfigManager.getCurrencyBuilder("messages.tax").replaceCurrency("%tax%", taxAmount).addPrefix().build());
        }

        economy.withdrawPlayer(owner, price.doubleValue());
        if (owner.isOnline() && owner.getPlayer() != null && !disableNotifications) {
            Player ownerOnline = owner.getPlayer();
            CurrencyBuilder builder = ConfigManager.getCurrencyBuilder("messages.bought_item_as_owner")
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

        player.sendMessage(ConfigManager.getCurrencyBuilder("messages.sold_item_as_customer")
                .replace("%amount%", String.valueOf(shopItem.getAmount()))
                .replaceCurrency("%price%", price)
                .replace("%item%", shopItem.getItemName())
                .replace("%shop%", getShopName()).build());

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

        } else if (ConfigManager.getBoolean("distribute_income")) {

            List<OfflinePlayer> trusted = trustedPlayers.stream()
                    .map(UUID::fromString)
                    .map(Bukkit::getOfflinePlayer)
                    .collect(Collectors.toList());

            trusted.add(Bukkit.getOfflinePlayer(ownerUUID));
            BigDecimal split = amount.divide(BigDecimal.valueOf(trusted.size()), 2, RoundingMode.HALF_UP);
            for (OfflinePlayer p : trusted) {
                Economy economy = plugin.getEconomy();
                economy.depositPlayer(p, split.doubleValue());
            }

        } else {
            Economy economy = plugin.getEconomy();
            economy.depositPlayer(Bukkit.getOfflinePlayer(ownerUUID), amount.doubleValue());
        }
    }

    /** Returns how many more of a certain ShopItem the owner can buy */
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

        if (owner != null && !shopItem.isItemTrade()) {
            available = Math.max(0, Math.min(available, (int) Math.floor(economy.getBalance(owner) / shopItem.getBuyPrice().doubleValue()) * shopItem.getAmount()));
        }

        if (shopItem.isItemTrade()) {
            int inStorage = storageHolder.getAmount(shopItem.getRawItem());
            available = available < shopItem.getItemTradeAmount() ? 0 : available;
            available = Math.min(inStorage, available);
        } else {
            available = available < shopItem.getAmount() ? 0 : available;
        }
        return available;
    }

    public void abandon() {
        Bukkit.getPluginManager().callEvent(new AbandonShopEvent(this));

        closeAllMenus();

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

        Entity entity = VMUtils.getEntity(entityUUID);
        if (entity != null) {
            if (plugin.isCitizensEnabled() && CitizensAPI.getNPCRegistry().isNPC(entity)) {
                CitizensAPI.getNPCRegistry().getNPC(entity).setName(ConfigManager.getString("villager.name_available"));
            } else if (entity instanceof Villager) {
                entity.setCustomName(ConfigManager.getString("villager.name_available"));
            }
            setProfession(VersionUtils.getMCVersion() < 14 ? Villager.Profession.FARMER : Villager.Profession.NONE);
        }


        if (offlinePlayer.isOnline()) {
            Player player = (Player) offlinePlayer;
            player.playSound(player.getLocation(), ConfigManager.getSound("sounds.expired"), 1, 1);
            player.sendMessage(ConfigManager.getMessage("messages.expired"));
        }

        VMPlugin.log.add(new Date() + ": " + offlinePlayer.getName() + " abandoned shop: " + entityUUID.toString());

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
    public void closeAllMenus() {
        storageHolder.closeAll();
        super.closeAllMenus();
    }

    @Override
    public void save() {
        config.set("storage", storageHolder.getItems());
        config.set("trusted", trustedPlayers);
        config.set("disable_notifications", disableNotifications);

        config.set("ownerUUID", ownerUUID == null ? null : ownerUUID.toString());
        config.set("ownerName", ownerName);

        super.save();
    }

    /** Sets owner and changes name */
    public void setOwner(Player player) {
        Entity villager = VMUtils.getEntity(entityUUID);
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

    public boolean isDisableNotifications() {
        return disableNotifications;
    }

    public void setDisableNotifications(boolean disableNotifications) {
        this.disableNotifications = disableNotifications;
    }
}
