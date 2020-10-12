package bestem0r.villagermarket.shops;

import bestem0r.villagermarket.VMPlugin;
import bestem0r.villagermarket.events.chat.ChangeName;
import bestem0r.villagermarket.items.ShopItem;
import bestem0r.villagermarket.menus.EditShopMenu;
import bestem0r.villagermarket.menus.ShopfrontMenu;
import bestem0r.villagermarket.utilities.Color;
import bestem0r.villagermarket.utilities.Methods;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.sql.Timestamp;
import java.util.*;

public class PlayerShop extends VillagerShop {

    public PlayerShop(File file) {
        super(file);

        super.ownerUUID = config.getString("ownerUUID");
        super.ownerName = config.getString("ownerName");

        super.shopfrontMenu = newShopfrontMenu(false, ShopItem.LoreType.MENU);
        super.shopfrontDetailedMenu = newShopfrontMenu(false, ShopItem.LoreType.ITEM);
        super.editShopfrontMenu = newShopfrontMenu(true, ShopItem.LoreType.MENU);
    }

    @Override
    void buildItemList() {
        List<Double> priceList = config.getDoubleList("prices");
        List<String> modeList = config.getStringList("modes");
        List<ItemStack> itemList = (List<ItemStack>) this.config.getList("for_sale");

        for (int i = 0; i < itemList.size(); i ++) {
            double price = (priceList.size() > i ? priceList.get(i) : 0.0);
            ShopItem.Mode mode = (modeList.size() > i ? ShopItem.Mode.valueOf(modeList.get(i)) : ShopItem.Mode.SELL);
            ShopItem shopItem = null;
            if (itemList.get(i) != null) {
                shopItem = new ShopItem.Builder(itemList.get(i))
                        .price(price)
                        .villagerType(VillagerType.PLAYER)
                        .amount(itemList.get(i).getAmount())
                        .mode(mode)
                        .build();
            }
            this.itemList.put(i, shopItem);
        }
    }

    @Override
    protected Boolean buyItem(int slot, Player player) {
        Economy economy = VMPlugin.getEconomy();
        ShopItem shopItem = itemList.get(slot);

        double tax = mainConfig.getInt("tax");
        double price = shopItem.getPrice();
        int amount = shopItem.getAmount();
        int inStock = getItemAmount(shopItem.asItemStack(ShopItem.LoreType.ITEM));
        String currency = VMPlugin.getCurrency();

        double taxAmount = tax / 100 * price;

        if ((inStock < amount)) {
            player.sendMessage(new Color.Builder().path("messages.not_enough_stock").addPrefix().build());
            return false;
        }
        if (economy.getBalance(player) < price) {
            player.sendMessage(new Color.Builder().path("messages.not_enough_money").addPrefix().build());
            return false;
        }
        OfflinePlayer owner = Bukkit.getOfflinePlayer(UUID.fromString(ownerUUID));
        if (player.getUniqueId().equals(owner.getUniqueId())) {
            player.sendMessage(new Color.Builder().path("messages.cannot_buy_from_yourself").addPrefix().build());
            return false;
        }

        economy.depositPlayer(owner, price - taxAmount);
        economy.withdrawPlayer(player, price);
        shopStats.addSold(amount);
        shopStats.addEarned(price);

        if (owner.isOnline()) {
            Player ownerOnline = owner.getPlayer();
            ownerOnline.sendMessage(new Color.Builder()
                    .path("messages.sold_item_as_owner")
                    .replace("%player%", player.getName())
                    .replace("%amount%", String.valueOf(amount))
                    .replace("%item%", shopItem.getType().name().toLowerCase())
                    .replace("%price%", (price) + currency)
                    .addPrefix()
                    .build());
            ownerOnline.sendMessage(new Color.Builder()
                    .path("messages.tax")
                    .replace("%tax%", (taxAmount) + currency)
                    .addPrefix()
                    .build()
            );
        }
        giveShopItem(player, shopItem);
        removeFromStock(shopItem.asItemStack(ShopItem.LoreType.ITEM));
        player.playSound(player.getLocation(), Sound.valueOf(mainConfig.getString("sounds.buy_item")), 1, 1);
        VMPlugin.log.add(new Date().toString() + ": " + player.getName() + " bought " + amount + "x " + shopItem.getType() + " from " + ownerName + " (" + price + currency + ")");
        return true;
    }

    @Override
    protected Boolean sellItem(int slot, Player player) {
        ShopItem shopItem = itemList.get(slot);
        Economy economy = VMPlugin.getEconomy();
        OfflinePlayer owner = Bukkit.getOfflinePlayer(UUID.fromString(ownerUUID));
        String currency = VMPlugin.getCurrency();

        double tax = mainConfig.getInt("tax");
        double moneyLeft = economy.getBalance(owner);
        double price = shopItem.getPrice();
        double taxAmount = tax / 100 * price;
        int amount = shopItem.getAmount();
        int amountInInventory = getAmountInventory(shopItem.asItemStack(ShopItem.LoreType.ITEM), player.getInventory());


        if (moneyLeft < price) {
            player.sendMessage(new Color.Builder().path("messages.owner_not_enough_money").addPrefix().build());
            return false;
        }
        if (amountInInventory < amount) {
            player.sendMessage(new Color.Builder().path("messages.not_enough_in_inventory").addPrefix().build());
            return false;
        }
        player.getInventory().removeItem(shopItem.asItemStack(ShopItem.LoreType.ITEM));
        economy.depositPlayer(player, price - taxAmount);
        getInventory(ShopMenu.STORAGE).addItem(shopItem.asItemStack(ShopItem.LoreType.ITEM));
        shopStats.addBought(amount);
        shopStats.addSpent(price);

        player.playSound(player.getLocation(), Sound.valueOf(mainConfig.getString("sounds.sell_item")), 0.5f, 1);
        player.sendMessage(new Color.Builder()
                        .path("messages.tax")
                        .replace("%tax%", taxAmount + currency)
                        .addPrefix()
                        .build());

        economy.withdrawPlayer(owner, price);
        if (owner.isOnline()) {
            Player ownerOnline = owner.getPlayer();
            ownerOnline.sendMessage(new Color.Builder()
                    .path("messages.bought_item_as_owner")
                    .replace("%player%", player.getName())
                    .replace("%amount%", String.valueOf(amount))
                    .replace("%item%", shopItem.getType().name().replaceAll("_", " ").toLowerCase())
                    .replace("%price%", price + currency)
                    .addPrefix()
                    .build());
        }
        VMPlugin.log.add(new Date().toString() + ": " + player.getName() + " sold " + amount + "x " + shopItem.getType() + " to " + ownerName + " (" + price + currency + ")");
        return true;
    }

    @Override
    public Boolean editShopInteract(Player player, InventoryClickEvent event) {
        player.playSound(player.getLocation(), Sound.valueOf(mainConfig.getString("sounds.menu_click")), 0.5f, 1);

        int slot = event.getRawSlot();
        Inventory inventory;
        switch (slot) {
            //Edit for sale
            case 0:
                updateShopInventories();
                inventory = getInventory(ShopMenu.EDIT_SHOPFRONT);
                break;
            //Preview shop
            case 1:
                inventory = getInventory(ShopMenu.SHOPFRONT);
                break;
            //Storage
            case 2:
                inventory = getInventory(ShopMenu.STORAGE);
                break;
            //Edit villager
            case 3:
                inventory = getInventory(ShopMenu.EDIT_VILLAGER);
                break;
            //Change name
            case 4:
                inventory = null;
                Bukkit.getServer().getPluginManager().registerEvents(new ChangeName(player, entityUUID), VMPlugin.getInstance());
                player.sendMessage(new Color.Builder().path("messages.change_name").addPrefix().build());
                player.sendMessage(new Color.Builder().path("messages.type_cancel").addPrefix().build());
                break;
            //Sell shop
            case 5:
                inventory = getInventory(ShopMenu.SELL_SHOP);
                break;
             //Increase time
            case 7:
                if (!super.duration.equalsIgnoreCase("infinite")) {
                    increaseTime(player);
                }
                return true;
            //Back
            case 8:
                inventory = null;
                break;
            default:
                return false;
        }
        event.getView().close();
        if (inventory != null) player.openInventory(inventory);
        return true;
    }

    /** Buy shop (This is only for Player Shops )*/
    public void buyShop(Player player, Entity villager) {
        Economy economy = VMPlugin.getEconomy();
        if (economy.getBalance(player) < cost) {
            player.sendMessage(new Color.Builder().path("messages.not_enough_money").addPrefix().build());
            return;
        }

        economy.withdrawPlayer(player, cost);

        villager.setCustomName(new Color.Builder().path("villager.name_taken").replace("%player%", player.getName()).build());
        this.ownerUUID = (player.getUniqueId().toString());
        this.ownerName = (player.getName());

        Date date = new Date();
        this.expireDate = (seconds == 0 ? new Timestamp(0) : new Timestamp(date.getTime() + (seconds * 1000L)));
        this.editShopMenu.setContents(EditShopMenu.create(this).getContents());

        player.playSound(player.getLocation(), Sound.valueOf(mainConfig.getString("sounds.buy_shop")), 1, 1);
        player.openInventory(editShopMenu);
    }

    /** Sell shop (This is only for Player Shops) */
    public void abandon() {
        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(UUID.fromString(ownerUUID));
        Economy economy = VMPlugin.getEconomy();

        Entity villager = Bukkit.getEntity(UUID.fromString(entityUUID));
        if (villager != null) villager.setCustomName(new Color.Builder().path("villager.name_available").build());

        Methods.newShopConfig(UUID.fromString(entityUUID), storageSize / 9, shopfrontSize / 9, getCost(), VillagerType.PLAYER, duration);
        economy.depositPlayer(offlinePlayer, ((double) getCost() * (mainConfig.getDouble("refund_percent") / 100)) * timesRented);

        ArrayList<ItemStack> storage = new ArrayList<>(Arrays.asList(getInventory(ShopMenu.STORAGE).getContents()));

        if (offlinePlayer.isOnline()) {
            Player player = (Player) offlinePlayer;
            for (ItemStack storageStack : storage) {
                if (storageStack != null) {
                    if (storage.indexOf(storageStack) == storageSize - 1) continue;
                    HashMap<Integer, ItemStack> exceed = player.getInventory().addItem(storageStack);
                    for (Integer i : exceed.keySet()) {
                        player.getLocation().getWorld().dropItemNaturally(player.getLocation(), exceed.get(i));
                    }
                }
            }
        } else {
            VMPlugin.abandonOffline.put(offlinePlayer, storage);
        }
    }

    /** Increase rent time */
    public void increaseTime(Player player) {
        Timestamp newExpire = new Timestamp(expireDate.getTime() + (seconds * 1000L));
        Date date = new Date();
        date.setTime(date.getTime() + ((mainConfig.getInt("max_rent") * 86400) * 1000L));
        if (newExpire.after(date)) {
            player.sendMessage(new Color.Builder().path("messages.max_rent_time").addPrefix().build());
            return;
        }
        Economy economy = VMPlugin.getEconomy();
        if (economy.getBalance(player) < cost) {
            player.sendMessage(new Color.Builder().path("messages.not_enough_money").addPrefix().build());
            return;
        }
        economy.withdrawPlayer(player, cost);
        this.expireDate = newExpire;
        this.editShopMenu.setContents(EditShopMenu.create(this).getContents());
        this.timesRented ++;

        player.playSound(player.getLocation(), Sound.valueOf(mainConfig.getString("sounds.increase_time")), 1, 1);
    }

    /** Returns true if rent has expired, false if not */
    public boolean hasExpired() {
        if (expireDate.getTime() == 0L) return false;
        return expireDate.before(new Date());
    }

    /** Inventory methods */
    @Override
    protected Inventory newEditShopInventory() {
        return EditShopMenu.create(this);
    }

    /** Create new inventory for items for sale editor, or shop front */
    @Override
    protected Inventory newShopfrontMenu(Boolean isEditor, ShopItem.LoreType loreType) {
        return new ShopfrontMenu.Builder(this)
                .isEditor(isEditor)
                .size(super.shopfrontSize)
                .loreType(loreType)
                .itemList(itemList)
                .build();
    }

    @Override
    public VillagerType getType() {
        return VillagerType.PLAYER;
    }

}
