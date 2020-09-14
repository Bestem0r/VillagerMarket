package bestem0r.villagermarket.events;

import bestem0r.villagermarket.DataManager;
import bestem0r.villagermarket.VMPlugin;
import bestem0r.villagermarket.shops.VillagerShop;
import bestem0r.villagermarket.utilities.Color;
import bestem0r.villagermarket.utilities.ColorBuilder;
import bestem0r.villagermarket.utilities.Config;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.UUID;

public class InventoryClick implements Listener {

    DataManager dataManager;
    VMPlugin plugin;
    private final ArrayList<String> menus = new ArrayList<>();
    private final FileConfiguration mainConfig;

    public InventoryClick(DataManager dataManager, VMPlugin plugin) {
        this.dataManager = dataManager;
        this.plugin = plugin;
        this.mainConfig = plugin.getConfig();
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

                villager.setCustomName(new Color.Builder().path("villager.name_taken").replace("%player%", player.getName()).build());
                villagerShop.setOwnerUUID(player.getUniqueId().toString());
                villagerShop.setOwnerName(player.getName());
                event.getView().close();
                player.playSound(player.getLocation(), Sound.valueOf(mainConfig.getString("sounds.buy_shop")), 1, 1);
                player.openInventory(villagerShop.getInventory(VillagerShop.ShopMenu.EDIT_SHOP));
                break;
            //Edit shop
            case 1:
                if (event.getRawSlot() > event.getView().getTopInventory().getSize() - 1) return;
                event.setCancelled(true);
                villagerShop.editShopInteract(player, event);
                break;
            //Edit for sale
            case 2:
                villagerShop.itemsInteract(player, event);
                break;
            //Buy/sell items
            case 3:
                if (!(event.getRawSlot() < villagerShop.getSize() * 9)) return;
                event.setCancelled(true);
                if (event.getCurrentItem() == null) return;

                if (event.getRawSlot() == villagerShop.getSize() * 9 - 1) {
                    player.playSound(player.getLocation(), Sound.valueOf(mainConfig.getString("sounds.menu_click")), 0.5f, 1);
                    player.openInventory(villagerShop.getInventory(VillagerShop.ShopMenu.SHOPFRONT_DETAILED));
                    break;
                }

                if (!villagerShop.customerInteract(event.getSlot(), player)) {
                    event.getView().close();
                }
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
                        player.openInventory(villagerShop.getInventory(VillagerShop.ShopMenu.EDIT_SHOP));
                        player.playSound(player.getLocation(), Sound.valueOf(mainConfig.getString("sounds.back")), 0.5f, 1);
                        break;
                }
                break;
            //Storage
            case 5:
                if (event.getRawSlot() == villagerShop.getSize() * 18 - 1) {
                    player.playSound(player.getLocation(), Sound.valueOf(mainConfig.getString("sounds.back")), 0.5f, 1);
                    event.getView().close();
                    player.openInventory(villagerShop.getInventory(VillagerShop.ShopMenu.EDIT_SHOP));
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

                    economy.depositPlayer(player, ((double) villagerShop.getCost() * (mainConfig.getDouble("refund_percent") / 100)));

                    ArrayList<ItemStack> storage = new ArrayList<>(Arrays.asList(villagerShop.getInventory(VillagerShop.ShopMenu.STORAGE).getContents()));
                    for (ItemStack storageStack : storage) {
                        if (storageStack != null) {
                            if (storage.indexOf(storageStack) == villagerShop.getSize() * 18 - 1) continue;
                            player.getInventory().addItem(storageStack);
                        }
                    }
                    player.sendMessage(VMPlugin.getPrefix() + ColorBuilder.color("messages.sold_shop"));
                    player.sendMessage(ColorBuilder.color("messages.sold_shop_2"));
                    player.playSound(player.getLocation(), Sound.valueOf(mainConfig.getString("sounds.sell_shop")), 0.5f, 1);
                    player.playSound(player.getLocation(), Sound.valueOf(mainConfig.getString("sounds.menu_click")), 0.5f, 1);
                } else if (event.getRawSlot() == 5) {
                    event.getView().close();
                    player.openInventory(villagerShop.getInventory(VillagerShop.ShopMenu.EDIT_SHOP));
                    player.playSound(player.getLocation(), Sound.valueOf(mainConfig.getString("sounds.back")), 0.5f, 1);
                }
                break;
            //Details
            case 7:
                if (!(event.getRawSlot() < villagerShop.getSize() * 9)) return;
                if (event.getCurrentItem() == null) return;
                event.setCancelled(true);
                if (event.getRawSlot() == villagerShop.getSize() * 9 - 1) {
                    player.playSound(player.getLocation(), Sound.valueOf(mainConfig.getString("sounds.menu_click")), 0.5f, 1);
                    player.openInventory(villagerShop.getInventory(VillagerShop.ShopMenu.SHOPFRONT));
                } else {
                    player.sendMessage(new Color.Builder().path("messages.must_be_menulore").addPrefix().build());
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
        menus.add(0, ChatColor.stripColor(new Color.Builder().path("menus.buy_shop.title").build()));
        menus.add(1, ChatColor.stripColor(new Color.Builder().path("menus.edit_shop.title").build()));
        menus.add(2, ChatColor.stripColor(new Color.Builder().path("menus.edit_shopfront.title").build()));
        menus.add(3, ChatColor.stripColor(new Color.Builder().path("menus.shopfront.title").build()));
        menus.add(4, ChatColor.stripColor(new Color.Builder().path("menus.edit_villager.title").build()));
        menus.add(5, ChatColor.stripColor(new Color.Builder().path("menus.edit_storage.title").build()));
        menus.add(6, ChatColor.stripColor(new Color.Builder().path("menus.sell_shop.title").build()));
        menus.add(7, ChatColor.stripColor(new Color.Builder().path("menus.shopfront.title").build()) + " (details)");
    }
}
