package bestem0r.villagermarket.shops;

import bestem0r.villagermarket.VMPlugin;
import bestem0r.villagermarket.events.chat.ChangeName;
import bestem0r.villagermarket.events.chat.SetCommand;
import bestem0r.villagermarket.items.ShopItem;
import bestem0r.villagermarket.menus.EditShopMenu;
import bestem0r.villagermarket.menus.ShopfrontMenu;
import bestem0r.villagermarket.utilities.Color;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.io.File;
import java.math.BigDecimal;
import java.util.Date;

public class AdminShop extends VillagerShop {

    public AdminShop(File file) {
        super(file);

        super.ownerUUID = "admin_shop";
        super.ownerName = "admin_shop";

        NamespacedKey key = new NamespacedKey(VMPlugin.getInstance(), "villagermarket-command");
        for (int slot : itemList.keySet()) {
            ShopItem shopItem = itemList.get(slot);
            if (shopItem == null) { continue; }
            ItemMeta itemMeta = shopItem.getItemMeta();
            if (itemMeta.getPersistentDataContainer().has(key, PersistentDataType.STRING)) {
                shopItem.setCommand(itemMeta.getPersistentDataContainer().get(key, PersistentDataType.STRING));
            }
        }
        updateShopInventories();
    }

    /** Buys item/command from the admin shop */
    @Override
    protected void buyItem(int slot, Player player) {
        ShopItem shopItem = itemList.get(slot);
        Economy economy = VMPlugin.getEconomy();

        BigDecimal price = shopItem.getPrice();

        if (economy.getBalance(player) < price.doubleValue()) {
            player.sendMessage(new Color.Builder().path("messages.not_enough_money").addPrefix().build());
            return;
        }
        if (shopItem.getPlayerLimit(player) >= shopItem.getLimit() && shopItem.getLimit() != 0) {
            player.sendMessage(new Color.Builder().path("messages.reached_buy_limit").addPrefix().build());
            return;
        }
        economy.withdrawPlayer(player, price.doubleValue());
        giveShopItem(player, shopItem);
        shopItem.increasePlayerLimit(player);

        player.playSound(player.getLocation(), Sound.valueOf(mainConfig.getString("sounds.buy_item")), 1, 1);
        player.sendMessage(new Color.Builder().path("messages.money_left").addPrefix().replace("%amount%", String.valueOf(economy.getBalance(player))).build());
        String currency = config.getString("currency");
        String valueCurrency = (config.getBoolean("currency_before") ? currency + price : price + currency);
        VMPlugin.log.add(new Date().toString() + ": " + player.getName() + " bought " + shopItem.getAmount() + "x " + shopItem.getType() + " from Admin Shop " + "(" + valueCurrency + ")");

    }

    /** Sells item to the admin shop */
    @Override
    protected void sellItem(int slot, Player player) {
        ShopItem shopItem = itemList.get(slot);
        Economy economy = VMPlugin.getEconomy();

        int amount = shopItem.getAmount();
        int amountInInventory = getAmountInventory(shopItem.asItemStack(ShopItem.LoreType.ITEM), player.getInventory());
        BigDecimal price = shopItem.getPrice();

        if (amountInInventory < amount) {
            player.sendMessage(new Color.Builder().path("messages.not_enough_in_inventory").addPrefix().build());
            return;
        }
        if (shopItem.getPlayerLimit(player) >= shopItem.getLimit() && shopItem.getLimit() != 0) {
            player.sendMessage(new Color.Builder().path("messages.reached_sell_limit").addPrefix().build());
            return;
        }
        economy.depositPlayer(player, price.doubleValue());
        player.getInventory().removeItem(shopItem.asItemStack(ShopItem.LoreType.ITEM));
        shopItem.increasePlayerLimit(player);

        player.playSound(player.getLocation(), Sound.valueOf(mainConfig.getString("sounds.sell_item")), 0.5f, 1);

        String currency = config.getString("currency");
        String valueCurrency = (config.getBoolean("currency_before") ? currency + price : price + currency);
        VMPlugin.log.add(new Date().toString() + ": " + player.getName() + " sold " + amount + "x " + shopItem.getType() + " to Admin Shop " + "(" + valueCurrency + ")");
    }

    @Override
    protected void shiftFunction(InventoryClickEvent event, ShopItem shopItem) {
        Player player = (Player) event.getWhoClicked();
        String cancel = mainConfig.getString("cancel");
        player.sendMessage(new Color.Builder().path("messages.type_command").addPrefix().build());
        player.sendMessage(new Color.Builder().path("messages.type_cancel").replace("%cancel%", cancel).addPrefix().build());

        Bukkit.getPluginManager().registerEvents(new SetCommand(player, this, shopItem), VMPlugin.getInstance());
        event.getView().close();
    }

    @Override
    public int getAvailable(ShopItem shopItem) {
        return 0;
    }

    /** Runs when a Player wants to buy a command */
    public void buyCommand(Player player, ShopItem shopItem) {
        Economy economy = VMPlugin.getEconomy();

        BigDecimal price = shopItem.getPrice();
        if (economy.getBalance(player) < price.doubleValue()) {
            player.sendMessage(new Color.Builder().path("messages.not_enough_money").addPrefix().build());
            return;
        }
        if (shopItem.getPlayerLimit(player) >= shopItem.getLimit() && shopItem.getLimit() != 0) {
            player.sendMessage(new Color.Builder().path("messages.reached_command_limit").addPrefix().build());
            return;
        }
        economy.withdrawPlayer(player, price.doubleValue());
        shopItem.runCommand(player);
        shopItem.increasePlayerLimit(player);
    }

    /** Called when a player with villagermarket.admin clicks in the edit shop menu */
    @Override
    public void editShopInteract(Player player, InventoryClickEvent event) {
        player.playSound(player.getLocation(), Sound.valueOf(mainConfig.getString("sounds.menu_click")), 0.5f, 1);
        String cancel = mainConfig.getString("cancel");

        int slot = event.getRawSlot();
        event.getView().close();
        switch (slot) {
            //Edit for sale
            case 0:
                updateShopInventories();
                openInventory(player, ShopMenu.EDIT_SHOPFRONT);
                break;
            //Preview shop
            case 1:
                openInventory(player, ShopMenu. SHOPFRONT);
                break;
            //Edit villager
            case 2:
                openInventory(player, ShopMenu.EDIT_VILLAGER);
                break;
            //Change name
            case 3:
                Bukkit.getServer().getPluginManager().registerEvents(new ChangeName(player, entityUUID), VMPlugin.getInstance());
                player.sendMessage(new Color.Builder().path("messages.change_name").addPrefix().build());
                player.sendMessage(new Color.Builder().path("messages.type_cancel").replace("%cancel%", cancel).addPrefix().build());
                break;
        }
    }

    @Override
    protected Inventory newEditShopInventory() {
        return EditShopMenu.create(this);
    }

    @Override
    protected Inventory newShopfrontMenu(Boolean isEditor, ShopItem.LoreType loreType) {
        return new ShopfrontMenu.Builder(this)
                .isEditor(isEditor)
                .size(super.shopSize)
                .loreType(loreType)
                .itemList(itemList)
                .build();
    }
}
