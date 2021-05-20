package me.bestem0r.villagermarket.shops;

import me.bestem0r.villagermarket.VMPlugin;
import me.bestem0r.villagermarket.events.InventoryClick;
import me.bestem0r.villagermarket.events.dynamic.ChangeName;
import me.bestem0r.villagermarket.events.dynamic.SetLimit;
import me.bestem0r.villagermarket.inventories.EditShop;
import me.bestem0r.villagermarket.inventories.Shopfront;
import me.bestem0r.villagermarket.inventories.StorageBuilder;
import me.bestem0r.villagermarket.items.ShopItem;
import me.bestem0r.villagermarket.utilities.ColorBuilder;
import me.bestem0r.villagermarket.utilities.Methods;
import me.bestem0r.villagermarket.utilities.ShopStats;
import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.io.File;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.util.*;

public class PlayerShop extends VillagerShop {

    public PlayerShop(VMPlugin plugin, File file) {
        super(plugin, file);
        super.collectedMoney = BigDecimal.valueOf(config.getDouble("collected_money"));
        super.editShopInv.setContents(EditShop.create(plugin, this).getContents());

        super.ownerUUID = config.getString("ownerUUID");
        super.ownerName = config.getString("ownerName");

        updateRedstone(false);
    }

    public void updateRedstone(boolean forceOff) {
        if (!plugin.isRedstoneEnabled()) { return; }
        if (cost == -1) { return; }
        Entity entity = Bukkit.getEntity(entityUUID);
        if (entity == null) { return; }

        Location location = entity.getLocation();
        Material standingOn = entity.getLocation().clone().subtract(0, 1, 0).getBlock().getType();
        boolean isOnPiston = (standingOn == Material.PISTON_HEAD || standingOn == Material.MOVING_PISTON);

        Block replace = location.subtract(0, (isOnPiston ? 3 : 2), 0).getBlock();

        replace.setType(ownerUUID.equals("null") || forceOff ? Material.AIR : Material.REDSTONE_BLOCK);
    }

    /** Buy item from the shop as the customer */
    @Override
    protected void buyItem(int slot, Player player) {
        Economy economy = plugin.getEconomy();
        ShopItem shopItem = itemList.get(slot);

        BigDecimal tax = BigDecimal.valueOf(mainConfig.getDouble("tax"));
        BigDecimal price = shopItem.getPrice();
        int amount = shopItem.getAmount();
        int inStock = getAmountInStorage(shopItem.asItemStack(ShopItem.LoreType.ITEM));

        BigDecimal taxAmount = tax.divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        taxAmount = taxAmount.multiply(price);

        OfflinePlayer owner = Bukkit.getOfflinePlayer(UUID.fromString(ownerUUID));
        if (player.getUniqueId().equals(owner.getUniqueId())) {
            player.sendMessage(new ColorBuilder(plugin).path("messages.cannot_buy_from_yourself").addPrefix().build());
            return;
        }
        if ((inStock < amount)) {
            player.sendMessage(new ColorBuilder(plugin).path("messages.not_enough_stock").addPrefix().build());
            return;
        }
        if (economy.getBalance(player) < price.doubleValue()) {
            player.sendMessage(new ColorBuilder(plugin).path("messages.not_enough_money").addPrefix().build());
            return;
        }

        depositOwner(price.subtract(taxAmount));
        economy.withdrawPlayer(player, shopItem.getPrice().doubleValue());
        shopStats.addSold(amount);
        shopStats.addEarned(price.doubleValue());

        if (owner.isOnline()) {
            Player ownerOnline = owner.getPlayer();
            ownerOnline.sendMessage(new ColorBuilder(plugin)
                    .path("messages.sold_item_as_owner")
                    .replace("%player%", player.getName())
                    .replace("%amount%", String.valueOf(amount))
                    .replace("%item%", shopItem.getType().name().toLowerCase())
                    .replaceWithCurrency("%price%", price.stripTrailingZeros().toPlainString())
                    .addPrefix()
                    .build());
            if (taxAmount.doubleValue() > 0) {
                ownerOnline.sendMessage(new ColorBuilder(plugin)
                        .path("messages.tax")
                        .replaceWithCurrency("%tax%", taxAmount.stripTrailingZeros().toPlainString())
                        .addPrefix()
                        .build()
                );
            }
        }
        giveShopItem(player, shopItem);
        removeFromStock(shopItem.asItemStack(ShopItem.LoreType.ITEM));

        player.playSound(player.getLocation(), Sound.valueOf(mainConfig.getString("sounds.buy_item")), 1, 1);
        double moneyLeft = economy.getBalance(player);
        player.sendMessage(new ColorBuilder(plugin).path("messages.money_left").addPrefix()
                .replaceWithCurrency("%amount%", BigDecimal.valueOf(moneyLeft).setScale(2, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString()).build());

        String currency = config.getString("currency");
        String valueCurrency = (config.getBoolean("currency_before") ? currency + price : price + currency);
        VMPlugin.log.add(new Date() + ": " + player.getName() + " bought " + amount + "x " + shopItem.getType() + " from " + ownerName + " (" + valueCurrency + ")");
    }

    /** Sell item to the shop as the customer */
    @Override
    protected void sellItem(int slot, Player player) {
        ShopItem shopItem = itemList.get(slot);
        Economy economy = plugin.getEconomy();
        OfflinePlayer owner = Bukkit.getOfflinePlayer(UUID.fromString(ownerUUID));

        BigDecimal tax = BigDecimal.valueOf(mainConfig.getDouble("tax"));
        BigDecimal price = shopItem.getPrice();
        BigDecimal moneyLeft = BigDecimal.valueOf(economy.getBalance(owner));
        BigDecimal taxAmount = tax.divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        taxAmount = taxAmount.multiply(price);

        int amount = shopItem.getAmount();
        int amountInInventory = getAmountInventory(shopItem.asItemStack(ShopItem.LoreType.ITEM), player.getInventory());

        if (player.getUniqueId().equals(UUID.fromString(ownerUUID))) {
            player.sendMessage(new ColorBuilder(plugin).path("messages.cannot_sell_to_yourself").addPrefix().build());
            return;
        }
        if (amount > getAvailable(shopItem)) {
            player.sendMessage(new ColorBuilder(plugin).path("messages.reached_buy_limit").addPrefix().build());
            return;
        }
        if (moneyLeft.compareTo(price) < 0) {
            player.sendMessage(new ColorBuilder(plugin).path("messages.owner_not_enough_money").addPrefix().build());
            return;
        }
        if (amountInInventory < amount) {
            player.sendMessage(new ColorBuilder(plugin).path("messages.not_enough_in_inventory").addPrefix().build());
            return;
        }
        removeItems(player.getInventory(), shopItem.asItemStack(ShopItem.LoreType.ITEM));
        economy.depositPlayer(player, price.subtract(taxAmount).doubleValue());
        moneyLeft = BigDecimal.valueOf(economy.getBalance(owner));
        addToStorage(shopItem.asItemStack(ShopItem.LoreType.ITEM));
        shopStats.addBought(amount);
        shopStats.addSpent(price.doubleValue());

        player.playSound(player.getLocation(), Sound.valueOf(mainConfig.getString("sounds.sell_item")), 0.5f, 1);
        player.sendMessage(new ColorBuilder(plugin)
                .path("messages.money_currently")
                .replaceWithCurrency("%amount%", moneyLeft.setScale(2, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString()).build());

        if (taxAmount.doubleValue() > 0) {
            player.sendMessage(new ColorBuilder(plugin)
                    .path("messages.tax")
                    .replaceWithCurrency("%tax%", taxAmount.stripTrailingZeros().toPlainString())
                    .addPrefix()
                    .build());
        }

        economy.withdrawPlayer(owner, price.doubleValue());
        if (owner.isOnline()) {
            Player ownerOnline = owner.getPlayer();
            ownerOnline.sendMessage(new ColorBuilder(plugin)
                    .path("messages.bought_item_as_owner")
                    .replace("%player%", player.getName())
                    .replace("%amount%", String.valueOf(amount))
                    .replace("%item%", shopItem.getType().name().replaceAll("_", " ").toLowerCase())
                    .replaceWithCurrency("%price%", price.stripTrailingZeros().toPlainString())
                    .addPrefix()
                    .build());
        }
        String currency = config.getString("currency");
        String valueCurrency = (config.getBoolean("currency_before") ? currency + price : price + currency);
        VMPlugin.log.add(new Date().toString() + ": " + player.getName() + " sold " + amount + "x " + shopItem.getType() + " to " + ownerName + " (" + valueCurrency + ")");
    }

    private void addToStorage(ItemStack itemStack) {
        for (Inventory storage : storageInventories) {
            int available = getAvailableInInventory(itemStack, storage);
            if (available >= itemStack.getAmount()) {
                storage.addItem(itemStack);
                break;
            }
        }
    }

    /** Edit shop front shift functions */
    @Override
    protected void shiftFunction(InventoryClickEvent event, ShopItem shopItem) {
        Player player = (Player) event.getWhoClicked();
        switch (shopItem.getMode()) {
            case SELL:
                quickAdd(player.getInventory(), shopItem);
                break;
            case BUY:
                String cancel = mainConfig.getString("cancel");
                player.sendMessage(new ColorBuilder(plugin).path("messages.type_limit_player").addPrefix().build());
                player.sendMessage(new ColorBuilder(plugin).path("messages.type_cancel").replace("%cancel%", cancel).addPrefix().build());
                Bukkit.getPluginManager().registerEvents(new SetLimit(plugin, player, this, shopItem), plugin);
                Bukkit.getScheduler().runTaskLater(plugin, () -> { event.getView().close(); }, 1L);
        }
    }

    /** Add everything from inventory to shop */
    public void quickAdd(Inventory inventory, ShopItem shopItem) {
        for (ItemStack inventoryStack : inventory.getContents()) {
            if (inventoryStack == null) { continue; }
            if (Methods.compareItems(shopItem.asItemStack(ShopItem.LoreType.ITEM), inventoryStack)) {
                addToStorage(inventoryStack);
                inventory.remove(inventoryStack);
            }
        }
        shopfrontHolder.update();
    }

    @Override
    public void editShopInteract(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        player.playSound(player.getLocation(), Sound.valueOf(mainConfig.getString("sounds.menu_click")), 0.5f, 1);
        String cancel = mainConfig.getString("cancel");

        int slot = event.getRawSlot();
        switch (slot) {
            //Edit for sale
            case 9:
                shopfrontHolder.open(player, Shopfront.Type.EDITOR);
                break;
            //Preview shop
            case 10:
                shopfrontHolder.open(player, Shopfront.Type.CUSTOMER);
                break;
            //Storage
            case 11:
                openInventory(player, ShopMenu.STORAGE);
                break;
            //Edit villager
            case 12:
                openInventory(player, ShopMenu.EDIT_VILLAGER);
                break;
            //Change name
            case 13:
                if (player.hasPermission("villagermarket.change_name")) {
                    event.getView().close();
                    Bukkit.getServer().getPluginManager().registerEvents(new ChangeName(plugin, player, entityUUID.toString()), plugin);
                    player.sendMessage(new ColorBuilder(plugin).path("messages.change_name").addPrefix().build());
                    player.sendMessage(new ColorBuilder(plugin).path("messages.type_cancel").replace("%cancel%", cancel).addPrefix().build());
                } else {
                    player.sendMessage(new ColorBuilder(plugin).path("messages.no_permission_change_name").addPrefix().build());
                }
                break;
            //Sell shop
            case 14:
                openInventory(player, ShopMenu.SELL_SHOP);
                break;
            //Collect money
            case 15:
                if (mainConfig.getBoolean("require_collect")) {
                    collectMoney(player);
                }
                break;
             //Increase time
            case 16:
                if (!super.duration.equalsIgnoreCase("infinite")) {
                    increaseTime(player);
                }
                break;
            case 26:
                event.getView().close();
        }
    }

    public void storageInteract(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();

        if (event.getRawSlot() == event.getView().getTopInventory().getSize() - 1) {
            openInventory(player, ShopMenu.EDIT_SHOP);
            player.playSound(player.getLocation(), Sound.valueOf(mainConfig.getString("sounds.menu_click")), 1, 1);
            event.setCancelled(true);
        }
        ItemStack current = event.getCurrentItem();
        if (current == null) { return; }
        NamespacedKey uiKey = new NamespacedKey(plugin, "vm-gui-item");

        event.setCancelled(current.getItemMeta().getPersistentDataContainer().has(uiKey, PersistentDataType.STRING));

        if (current.getItemMeta().getPersistentDataContainer().has(uiKey, PersistentDataType.INTEGER)) {
            Integer page = current.getItemMeta().getPersistentDataContainer().get(uiKey, PersistentDataType.INTEGER);
            player.playSound(player.getLocation(), Sound.valueOf(mainConfig.getString("sounds.menu_click")), 1, 1);
            player.openInventory(storageInventories.get(page));
            Bukkit.getPluginManager().registerEvents(new InventoryClick(player, this, ShopMenu.STORAGE), plugin);
            event.setCancelled(true);
        }
    }

    /** Collects money */
    private void collectMoney(Player player) {
        Economy economy = plugin.getEconomy();
        economy.depositPlayer(player, collectedMoney.doubleValue());
        player.sendMessage(new ColorBuilder(plugin).path("messages.collected_money").addPrefix()
                .replaceWithCurrency("%amount%", collectedMoney.stripTrailingZeros().toPlainString()).build());
        player.playSound(player.getLocation(), Sound.valueOf(mainConfig.getString("sounds.collect_money")), 1, 1);
        super.collectedMoney = BigDecimal.valueOf(0);
        super.editShopInv.setContents(EditShop.create(plugin, this).getContents());
    }

    /** Deposits money to the Shop/Owner */
    public void depositOwner(BigDecimal amount) {
        if (mainConfig.getBoolean("require_collect")) {
            super.collectedMoney = collectedMoney.add(amount);
            super.editShopInv.setContents(EditShop.create(plugin, this).getContents());
        } else {
            Economy economy = plugin.getEconomy();
            economy.depositPlayer(Bukkit.getOfflinePlayer(UUID.fromString(ownerUUID)), amount.doubleValue());
        }
    }

    /** Returns how many more of an certain ShopItem the owner wants to buy */
    @Override
    public int getAvailable(ShopItem shopItem) {
        Economy economy = plugin.getEconomy();
        OfflinePlayer owner = Bukkit.getOfflinePlayer(UUID.fromString(ownerUUID));

        int inStorage = 0;
        int available = 0;
        for (Inventory storage : storageInventories) {
            inStorage += getAmountInventory(shopItem.asItemStack(ShopItem.LoreType.ITEM), storage);
            available += getAvailableInInventory(shopItem.asItemStack(ShopItem.LoreType.ITEM), storage);
        }
        if (shopItem.getLimit() != 0) {
            available = shopItem.getLimit() - inStorage;
        }

        available = (Math.min(available, (int) Math.ceil(economy.getBalance(owner) / shopItem.getPrice().doubleValue()) * shopItem.getAmount()));
        available = (Math.max(available, 0));
        return available;
    }

    private int getAvailableInInventory(ItemStack itemStack, Inventory inventory) {
        int inStorage = getAmountInventory(itemStack, inventory);
        int availableSlots = 0;
        for (ItemStack storageItem : inventory.getContents()) {
            if (storageItem == null) {
                availableSlots ++;
                continue;
            }
            if (Methods.compareItems(storageItem, itemStack)) { availableSlots ++; }
        }

        return availableSlots * itemStack.getType().getMaxStackSize() - inStorage;
    }

    /** Buy shop */
    public void buyShop(Player player) {
        Economy economy = plugin.getEconomy();
        if (plugin.getConfig().getBoolean("buy_shop_permission") && !player.hasPermission("villagermarket.buy_shop")) {
            player.sendMessage(new ColorBuilder(plugin).path("messages.no_permission_buy_shop").addPrefix().build());
            return;
        }
        if (economy.getBalance(player) < cost) {
            player.sendMessage(new ColorBuilder(plugin).path("messages.not_enough_money").addPrefix().build());
            return;
        }

        economy.withdrawPlayer(player, cost);
        setOwner(player);

        Date date = new Date();
        this.expireDate = (seconds == 0 ? new Timestamp(0) : new Timestamp(date.getTime() + (seconds * 1000L)));
        this.editShopInv.setContents(EditShop.create(plugin, this).getContents());

        player.playSound(player.getLocation(), Sound.valueOf(mainConfig.getString("sounds.buy_shop")), 1, 1);
        openInventory(player, ShopMenu.EDIT_SHOP);

        String currency = mainConfig.getString("currency");
        String valueCurrency = (mainConfig.getBoolean("currency_before") ? currency + cost : cost + currency);
        VMPlugin.log.add(new Date().toString() + ": " + player.getName() + " bought shop for " + valueCurrency);
        updateRedstone(false);
    }


    /** Sell shop */
    public void abandon() {
        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(UUID.fromString(ownerUUID));
        Economy economy = plugin.getEconomy();

        Entity entity = Bukkit.getEntity(entityUUID);
        if (entity != null) {
            if (plugin.isCitizensEnabled() && CitizensAPI.getNPCRegistry().isNPC(entity)) {
                CitizensAPI.getNPCRegistry().getNPC(entity).setName(new ColorBuilder(plugin).path("villager.name_available").build());
            } else if (entity instanceof Villager){
                Villager villager = (Villager) entity;
                entity.setCustomName(new ColorBuilder(plugin).path("villager.name_available").build());
                villager.setProfession(Villager.Profession.NONE);
            }
        }

        if (cost != -1) { economy.depositPlayer(offlinePlayer, ((double) getCost() * (mainConfig.getDouble("refund_percent") / 100)) * timesRented); }
        economy.depositPlayer(offlinePlayer, collectedMoney.doubleValue());

        List<ItemStack> storage = filteredStorage();
        if (offlinePlayer.isOnline()) {
            Player player = (Player) offlinePlayer;
            Inventory inventory = Bukkit.createInventory(null, 54, new ColorBuilder(plugin).path("menus.expired_shop.title").build());

            inventory.setContents(storage.toArray(new ItemStack[0]));
            player.openInventory(inventory);
        } else {
            VMPlugin.abandonOffline.put(offlinePlayer.getUniqueId(), storage);
        }

        config.set("storage", null);
        super.trusted.clear();
        super.collectedMoney = BigDecimal.valueOf(0);
        super.ownerUUID = "null";
        super.ownerName = "null";
        super.shopStats = new ShopStats(plugin);
        super.itemList = new HashMap<>();
        super.timesRented = 0;
        super.storageInventories = new StorageBuilder(plugin, storageSize, new ArrayList<>()).create();
        super.editShopInv.setContents(EditShop.create(plugin, this).getContents());

        VMPlugin.log.add(new Date().toString() + ": " + offlinePlayer.getName() + " abandoned shop! ");
        updateRedstone(false);
    }

    /** Sells/Removes the Player Shop */
    public void sell(Player player) {
        abandon();

        player.playSound(player.getLocation(), Sound.valueOf(mainConfig.getString("sounds.sell_shop")), 0.5f, 1);
        player.playSound(player.getLocation(), Sound.valueOf(mainConfig.getString("sounds.menu_click")), 0.5f, 1);
        if (cost != -1) {
            player.sendMessage(new ColorBuilder(plugin).path("messages.sold_shop").addPrefix().build());
        } else {
            Entity villager = Bukkit.getEntity(entityUUID);
            villager.getWorld().dropItemNaturally(villager.getLocation(), Methods.villagerShopItem(plugin, shopSize / 9, storageSize / 9, 1));
            if (plugin.isCitizensEnabled() && CitizensAPI.getNPCRegistry().isNPC(villager)) {
                NPC npc = CitizensAPI.getNPCRegistry().getNPC(villager);
                npc.destroy();
            } else {
                villager.remove();
            }
            file.delete();
            VMPlugin.shops.remove(this);
        }
    }

    /** Increase rent time */
    public void increaseTime(Player player) {
        Timestamp newExpire = new Timestamp(expireDate.getTime() + (seconds * 1000L));
        Date date = new Date();
        date.setTime(date.getTime() + ((mainConfig.getInt("max_rent") * 86400L) * 1000L));
        if (newExpire.after(date)) {
            player.sendMessage(new ColorBuilder(plugin).path("messages.max_rent_time").addPrefix().build());
            return;
        }
        Economy economy = plugin.getEconomy();
        if (economy.getBalance(player) < cost) {
            player.sendMessage(new ColorBuilder(plugin).path("messages.not_enough_money").addPrefix().build());
            return;
        }
        economy.withdrawPlayer(player, cost);
        this.expireDate = newExpire;
        this.editShopInv.setContents(EditShop.create(plugin, this).getContents());
        this.timesRented ++;

        player.playSound(player.getLocation(), Sound.valueOf(mainConfig.getString("sounds.increase_time")), 1, 1);
    }

    /** Returns true if rent has expired, false if not */
    public boolean hasExpired() {
        if (expireDate.getTime() == 0L) return false;
        return expireDate.before(new Date());
    }

    /** Sets owner and changes name */
    public void setOwner(Player player) {
        Entity villager = Bukkit.getEntity(entityUUID);
        String name = new ColorBuilder(plugin).path("villager.name_taken").replace("%player%", player.getName()).build();
        if (plugin.isCitizensEnabled() && CitizensAPI.getNPCRegistry().isNPC(villager)) {
            CitizensAPI.getNPCRegistry().getNPC(villager).setName(name);
        } else {
            villager.setCustomName(name);
        }
        this.ownerUUID = (player.getUniqueId().toString());
        this.ownerName = (player.getName());
    }

    /** Adds trusted to the trusted list */
    public void addTrusted(Player player) {
        super.trusted.add(player.getUniqueId().toString());
    }
    /** Removes trusted from the trusted list */
    public void removeTrusted(Player player) {
        super.trusted.remove(player.getUniqueId().toString());
    }
    /** Returns true if player is trusted, false if not */
    public boolean isTrusted(Player player) {
        return trusted.contains(player.getUniqueId().toString());
    }

}
