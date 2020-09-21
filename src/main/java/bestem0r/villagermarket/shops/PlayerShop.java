package bestem0r.villagermarket.shops;

import bestem0r.villagermarket.VMPlugin;
import bestem0r.villagermarket.events.chat.ChangeName;
import bestem0r.villagermarket.items.ShopItem;
import bestem0r.villagermarket.menus.EditShopMenu;
import bestem0r.villagermarket.menus.ShopfrontMenu;
import bestem0r.villagermarket.utilities.Color;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

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
        ShopItem shopItem = itemList.get(slot);
        Economy economy = VMPlugin.getEconomy();

        int amount = shopItem.getAmount();
        int inStock = getItemAmount(shopItem.asItemStack(ShopItem.LoreType.ITEM));
        double price = shopItem.getPrice();

        if ((inStock < amount)) {
            player.sendMessage(new Color.Builder().path("messages.not_enough_stock").addPrefix().build());
            return false;
        }
        if (economy.getBalance(player) < price) {
            player.sendMessage(new Color.Builder().path("messages.not_enough_money").addPrefix().build());
            return false;
        }
        OfflinePlayer owner = Bukkit.getOfflinePlayer(UUID.fromString(ownerUUID));

        economy.depositPlayer(owner, price);
        if (owner.isOnline()) {
            Player ownerOnline = owner.getPlayer();
            economy.depositPlayer(owner, price);
            ownerOnline.sendMessage(new Color.Builder()
                    .path("messages.sold_item_as_owner")
                    .replace("%player%", player.getName())
                    .replace("%amount%", String.valueOf(amount))
                    .replace("%item%", shopItem.getType().name().toLowerCase())
                    .replace("%price%", String.valueOf(price))
                    .addPrefix()
                    .build());
        }
        economy.withdrawPlayer(player, price);

        ItemStack boughtStack = shopItem.asItemStack(ShopItem.LoreType.ITEM);

        HashMap<Integer, ItemStack> itemsLeft = player.getInventory().addItem(boughtStack);
        for (int i : itemsLeft.keySet()) {
            player.getLocation().getWorld().dropItemNaturally(player.getLocation(), itemsLeft.get(i));
        }
        player.playSound(player.getLocation(), Sound.valueOf(mainConfig.getString("sounds.buy_item")), 1, 1);

        removeFromStock(boughtStack);
        return true;
    }

    @Override
    protected Boolean sellItem(int slot, Player player) {
        ShopItem shopItem = itemList.get(slot);
        Economy economy = VMPlugin.getEconomy();
        OfflinePlayer owner = Bukkit.getOfflinePlayer(UUID.fromString(ownerUUID));

        int amount = shopItem.getAmount();
        double moneyLeft = economy.getBalance(owner);
        double price = shopItem.getPrice();
        int amountInInventory = getAmountInventory(shopItem.asItemStack(ShopItem.LoreType.ITEM), player.getInventory());

        if (moneyLeft < price) {
            player.sendMessage(VMPlugin.getPrefix() + new Color.Builder().path("messages.owner_not_enough_money").build());
            return false;
        }
        if (amountInInventory < amount) {
            player.sendMessage(VMPlugin.getPrefix() + new Color.Builder().path("messages.not_enough_in_inventory").build());
            return false;
        }
        player.getInventory().removeItem(shopItem.asItemStack(ShopItem.LoreType.ITEM));
        economy.depositPlayer(player, price);
        getInventory(ShopMenu.STORAGE).addItem(shopItem.asItemStack(ShopItem.LoreType.ITEM));

        player.playSound(player.getLocation(), Sound.valueOf(mainConfig.getString("sounds.sell_item")), 0.5f, 1);

        economy.withdrawPlayer(owner, price);
        if (owner.isOnline()) {
            Player ownerOnline = owner.getPlayer();
            ownerOnline.sendMessage(new Color.Builder()
                    .path("messages.bought_item_as_owner")
                    .replace("%player%", player.getName())
                    .replace("%amount%", String.valueOf(amount))
                    .replace("%item%", shopItem.getType().name().replaceAll("_", " ").toLowerCase())
                    .replace("%price%", String.valueOf(price))
                    .addPrefix()
                    .build());
        }

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
                player.sendMessage(VMPlugin.getPrefix() + new Color.Builder().path("messages.change_name").build());
                player.sendMessage(VMPlugin.getPrefix() + new Color.Builder().path("messages.type_cancel").build());
                break;
            //Sell shop
            case 5:
                inventory = getInventory(ShopMenu.SELL_SHOP);
                break;
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


    /** Inventory methods */
    @Override
    protected Inventory newEditShopInventory() {
        return EditShopMenu.create(VillagerType.PLAYER);
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
