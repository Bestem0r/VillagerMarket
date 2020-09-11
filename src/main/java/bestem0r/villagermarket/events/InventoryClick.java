package bestem0r.villagermarket.events;

import bestem0r.villagermarket.*;
import bestem0r.villagermarket.shops.AdminShop;
import bestem0r.villagermarket.shops.VillagerShop;
import bestem0r.villagermarket.utilities.ColorBuilder;
import bestem0r.villagermarket.utilities.Config;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.*;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class InventoryClick implements Listener {

    DataManager dataManager;
    private ArrayList<String> menus = new ArrayList<>();

    public InventoryClick(DataManager dataManager) {
        this.dataManager = dataManager;
        setUpMenus();
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        String title = ChatColor.stripColor(event.getView().getTitle());

        if (!menus.contains(title) || !dataManager.getClickMap().containsKey(player.getUniqueId().toString())) return;

        int titleIndex = menus.indexOf(title);

        String entityUUID = dataManager.getClickMap().get(player.getUniqueId().toString());
        VillagerShop villagerShop = dataManager.getVillagers().get(entityUUID);
        Entity villager = Bukkit.getEntity(UUID.fromString(entityUUID));

        boolean isAdminShop = (villagerShop instanceof AdminShop);

        ItemStack currentItem = event.getCurrentItem();
        ItemStack cursorItem = event.getCursor();
        
        Economy economy = VMPlugin.getEconomy();
        int cost = villagerShop.getCost();
        switch (titleIndex) {
            //Buy available
            case 0:
                event.setCancelled(true);
                if (event.getRawSlot() != 4) break;

                if (economy.getBalance(player) < cost) {
                    player.sendMessage(VMPlugin.getPrefix() + ColorBuilder.color("messages.not_enough_money"));
                    event.getView().close();
                    return;
                }
                economy.withdrawPlayer(player, cost);

                villager.setCustomName(ColorBuilder.colorReplace("villager.name_taken", "%player%", player.getName()));
                villagerShop.setOwnerUUID(player.getUniqueId().toString());
                villagerShop.setOwnerName(player.getName());
                event.getView().close();
                player.playSound(player.getLocation(), Sound.valueOf(VMPlugin.getInstance().getConfig().getString("sounds.buy_shop")), 1, 1);
                player.openInventory(villagerShop.getInventory(VillagerShop.ShopInventory.EDIT_SHOP));
                break;
            //Edit shop
            case 1:
                player.playSound(player.getLocation(), Sound.valueOf(VMPlugin.getInstance().getConfig().getString("sounds.menu_click")), 0.5f, 1);
                Inventory inventory;
                if (event.getRawSlot() > event.getView().getTopInventory().getSize() - 1) return;
                switch (event.getRawSlot()) {
                    //Edit for sale
                    case 0:
                        villagerShop.updateShopInventories();
                        inventory = villagerShop.getInventory(VillagerShop.ShopInventory.EDIT_FOR_SALE);
                        break;
                    //Edit villager
                    case 1:
                        inventory = villagerShop.getInventory(VillagerShop.ShopInventory.EDIT_VILLAGER);
                        break;
                    //Edit storage
                    case 2:
                        inventory = villagerShop.getInventory(VillagerShop.ShopInventory.STORAGE);
                        break;
                    //Sell shop
                    case 3:
                        inventory = villagerShop.getInventory(VillagerShop.ShopInventory.SELL_SHOP);
                        break;
                    //Back
                    default:
                        inventory = null;
                }
                event.setCancelled(true);
                event.getView().close();
                if (inventory != null) player.openInventory(inventory);
                break;
            //Edit for sale
            case 2:
                if (((event.getRawSlot() < villagerShop.getSize() * 9) || event.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY) && (currentItem == null) && (cursorItem.getType() != Material.AIR)) {
                    List<Material> blackList = VMPlugin.getInstance().getMaterialBlackList();
                    if (blackList.contains(cursorItem.getType())) {
                        player.sendMessage(VMPlugin.getPrefix() + ColorBuilder.color("messages.blacklisted"));
                    } else {
                        PlayerChat.startChatSession(player, entityUUID, cursorItem, event.getRawSlot());
                    }
                    event.getView().close();
                    event.setCancelled(true);
                } else if (event.getRawSlot() < villagerShop.getSize() * 9) {
                    if (event.getRawSlot() == 8) {
                        player.playSound(player.getLocation(), Sound.valueOf(VMPlugin.getInstance().getConfig().getString("sounds.menu_click")), 0.5f, 1);
                        event.getView().close();
                        player.openInventory(villagerShop.getInventory(VillagerShop.ShopInventory.EDIT_SHOP));
                        event.setCancelled(true);
                        break;
                    }
                    //Delete item
                    if (event.getClick() == ClickType.RIGHT && currentItem != null) {
                        player.playSound(player.getLocation(), Sound.valueOf(VMPlugin.getInstance().getConfig().getString("sounds.remove_item")), 0.5f, 1);
                        villagerShop.getItemsForSale().remove(event.getRawSlot());
                        villagerShop.updateShopInventories();
                    }
                    event.setCancelled(true);
                }
                break;
            //Buy items
            case 3:
                if (!(event.getRawSlot() < villagerShop.getSize() * 9)) { return; }
                event.setCancelled(true);
                if (currentItem == null) { return; }

                int amount = currentItem.getAmount();
                double price = villagerShop.getItemsForSale().get(event.getRawSlot()).getPrice();
                int inStock = (isAdminShop ? 999 : villagerShop.getItemAmount(villagerShop.getItemsForSale().get(event.getRawSlot()).asItemStack()));
                if ((inStock < amount)) {
                    player.sendMessage(VMPlugin.getPrefix() + ColorBuilder.color("messages.not_enough_stock"));
                    event.getView().close();
                    break;
                }
                if (economy.getBalance(player) < price) {
                    player.sendMessage(VMPlugin.getPrefix() + ColorBuilder.color("messages.not_enough_money"));
                    event.getView().close();
                    break;
                }
                if (!isAdminShop) {
                    Player owner = Bukkit.getPlayer(UUID.fromString(villagerShop.getOwnerUUID()));
                    if (owner != null) {
                        economy.depositPlayer(owner, price);
                        owner.sendMessage(ColorBuilder.replaceBought(player.getName(), currentItem.getType().name(), String.valueOf(price), String.valueOf(amount)));
                    } else {
                        double pendingMoney = price;
                        if (Config.getPendingConfig().get(villagerShop.getOwnerUUID()) != null) {
                            pendingMoney += Config.getPendingConfig().getDouble(villagerShop.getOwnerUUID());
                        }
                        Config.getPendingConfig().set(villagerShop.getOwnerUUID(), pendingMoney);
                        Config.savePending();
                    }
                }
                economy.withdrawPlayer(player, price);

                ItemStack boughtItemStack = currentItem.clone();
                ItemMeta boughtMeta = boughtItemStack.getItemMeta();
                boughtMeta.setLore(new ArrayList<>());
                boughtItemStack.setItemMeta(boughtMeta);

                player.getInventory().addItem(boughtItemStack);
                player.playSound(player.getLocation(), Sound.valueOf(VMPlugin.getInstance().getConfig().getString("sounds.buy_item")), 1, 1);

                villagerShop.updateShopInventories();
                if (!isAdminShop) villagerShop.removeFromStock(villagerShop.getItemsForSale().get(event.getRawSlot()));
                break;
            //Edit villager
            case 4:
                Villager villagerObject = (Villager) villager;
                event.getView().close();
                event.setCancelled(true);
                switch (event.getRawSlot()) {
                    case 0:
                        villagerObject.setProfession(Villager.Profession.ARMORER);
                        break;
                    case 1:
                        villagerObject.setProfession(Villager.Profession.BUTCHER);
                        break;
                    case 2:
                        villagerObject.setProfession(Villager.Profession.CARTOGRAPHER);
                        break;
                    case 3:
                        villagerObject.setProfession(Villager.Profession.CLERIC);
                        break;
                    case 4:
                        villagerObject.setProfession(Villager.Profession.FARMER);
                        break;
                    case 5:
                        villagerObject.setProfession(Villager.Profession.FISHERMAN);
                        break;
                    case 6:
                        villagerObject.setProfession(Villager.Profession.LEATHERWORKER);
                        break;
                    case 7:
                        villagerObject.setProfession(Villager.Profession.LIBRARIAN);
                        break;
                    case 8:
                        player.openInventory(villagerShop.getInventory(VillagerShop.ShopInventory.EDIT_SHOP));
                        player.playSound(player.getLocation(), Sound.valueOf(VMPlugin.getInstance().getConfig().getString("sounds.back")), 0.5f, 1);
                        break;
                }
                break;
            //Storage
            case 5:
                if (event.getRawSlot() == villagerShop.getSize() * 18 - 1) {
                    player.playSound(player.getLocation(), Sound.valueOf(VMPlugin.getInstance().getConfig().getString("sounds.back")), 0.5f, 1);
                    event.getView().close();
                    player.openInventory(villagerShop.getInventory(VillagerShop.ShopInventory.EDIT_SHOP));
                    event.setCancelled(true);
                }
                break;
            //Sell shop
            case 6:
                event.setCancelled(true);
                if (event.getRawSlot() == 3) {
                    event.getView().close();
                    villager.setCustomName(ColorBuilder.color("villager.name_available"));
                    dataManager.getVillagerEntities().remove(villager);
                    dataManager.removeVillager(entityUUID);
                    Config.newShopConfig(entityUUID, (Villager) villager, villagerShop.getSize(), villagerShop.getCost(), "player");

                    economy.depositPlayer(player, ((double) villagerShop.getCost() * (VMPlugin.getInstance().getConfig().getDouble("refund_percent") / 100)));

                    ArrayList<ItemStack> storage = new ArrayList<>(Arrays.asList(villagerShop.getInventory(VillagerShop.ShopInventory.STORAGE).getContents()));
                    for (ItemStack storageStack : storage) {
                        if (storageStack != null) {
                            if (storage.indexOf(storageStack) == villagerShop.getSize() * 18 -1) continue;
                            player.getInventory().addItem(storageStack);
                        }
                    }
                    player.sendMessage(VMPlugin.getPrefix() + ColorBuilder.color("messages.sold_shop"));
                    player.sendMessage(ColorBuilder.color("messages.sold_shop_2"));
                    player.playSound(player.getLocation(), Sound.valueOf(VMPlugin.getInstance().getConfig().getString("sounds.sell_shop")), 0.5f, 1);
                    player.playSound(player.getLocation(), Sound.valueOf(VMPlugin.getInstance().getConfig().getString("sounds.menu_click")), 0.5f, 1);
                } else if (event.getRawSlot() == 5) {
                    event.getView().close();
                    player.openInventory(villagerShop.getInventory(VillagerShop.ShopInventory.EDIT_SHOP));
                    player.playSound(player.getLocation(), Sound.valueOf(VMPlugin.getInstance().getConfig().getString("sounds.back")), 0.5f, 1);
                }
                break;
        }
    }
    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        Player player = (Player) event.getWhoClicked();
        if (!dataManager.getClickMap().containsKey(player.getUniqueId().toString())) { return; }

        String title = ChatColor.stripColor(event.getView().getTitle());
        if (title.equals(menus.get(0)) || title.equals(menus.get(2))) {
            event.setCancelled(true);
        }
    }

    public void setUpMenus() {
        menus.add(0, ChatColor.stripColor(ColorBuilder.color("menus.buy_shop.title")));
        menus.add(1, ChatColor.stripColor(ColorBuilder.color("menus.edit_shop.title")));
        menus.add(2, ChatColor.stripColor(ColorBuilder.color("menus.edit_for_sale.title")));
        menus.add(3, ChatColor.stripColor(ColorBuilder.color("menus.buy_items.title")));
        menus.add(4, ChatColor.stripColor(ColorBuilder.color("menus.edit_villager.title")));
        menus.add(5, ChatColor.stripColor(ColorBuilder.color("menus.edit_storage.title")));
        menus.add(6, ChatColor.stripColor(ColorBuilder.color("menus.sell_shop.title")));
    }
}
