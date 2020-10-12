package bestem0r.villagermarket.events;

import bestem0r.villagermarket.VMPlugin;
import bestem0r.villagermarket.shops.PlayerShop;
import bestem0r.villagermarket.shops.ShopMenu;
import bestem0r.villagermarket.shops.VillagerShop;
import bestem0r.villagermarket.utilities.Color;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;

import java.util.ArrayList;
import java.util.UUID;

public class InventoryClick implements Listener {

    VMPlugin plugin;
    private final ArrayList<String> menus = new ArrayList<>();
    private final FileConfiguration mainConfig;

    public InventoryClick(VMPlugin plugin) {
        this.plugin = plugin;
        this.mainConfig = plugin.getConfig();
        setUpMenus();
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        String title = ChatColor.stripColor(event.getView().getTitle());

        if (!menus.contains(title) || !VMPlugin.clickMap.containsKey(player.getUniqueId().toString())) return;

        int titleIndex = menus.indexOf(title);

        VillagerShop villagerShop = VMPlugin.clickMap.get(player.getUniqueId().toString());
        Entity villager = Bukkit.getEntity(UUID.fromString(villagerShop.getEntityUUID()));

        if (event.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY && titleIndex != 5) event.setCancelled(true);

        switch (titleIndex) {
            //Buy available
            case 0:
                if (event.getRawSlot() > 8) return;
                if (!(villagerShop instanceof Player)) return;
                event.setCancelled(true);
                if (event.getRawSlot() == 4) {
                    event.getView().close();
                    PlayerShop playerShop = (PlayerShop) villagerShop;
                    playerShop.buyShop(player, villager);
                }
                break;
            //Edit shop
            case 1:
                if (event.getRawSlot() > 8) return;
                event.setCancelled(true);
                villagerShop.editShopInteract(player, event);
                break;
            //Edit for sale
            case 2:
                villagerShop.itemsInteract(player, event);
                break;
            //Buy/sell items
            case 3:
                if (!(event.getRawSlot() < villagerShop.getShopfrontSize())) return;
                event.setCancelled(true);
                if (event.getCurrentItem() == null) return;

                if (event.getRawSlot() == villagerShop.getShopfrontSize() - 1) {
                    player.playSound(player.getLocation(), Sound.valueOf(mainConfig.getString("sounds.menu_click")), 0.5f, 1);
                    player.openInventory(villagerShop.getInventory(ShopMenu.SHOPFRONT_DETAILED));
                    break;
                }

                if (!villagerShop.customerInteract(event.getSlot(), player)) {
                    event.getView().close();
                }
                break;
            //Edit villager
            case 4:
                Villager villagerObject = (Villager) villager;
                if (event.getRawSlot() > 8) return;
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
                        player.playSound(player.getLocation(), Sound.valueOf(mainConfig.getString("sounds.back")), 0.5f, 1);
                        event.getView().close();
                        player.openInventory(villagerShop.getInventory(ShopMenu.EDIT_SHOP));
                        return;
                }
                player.playSound(player.getLocation(), Sound.valueOf(mainConfig.getString("sounds.change_profession")), 0.5f, 1);
                break;
            //Storage
            case 5:
                if (event.getRawSlot() == villagerShop.getStorageSize() - 1) {
                    player.playSound(player.getLocation(), Sound.valueOf(mainConfig.getString("sounds.back")), 0.5f, 1);
                    event.getView().close();
                    player.openInventory(villagerShop.getInventory(ShopMenu.EDIT_SHOP));
                    event.setCancelled(true);
                }
                break;
            //Sell shop
            case 6:
                if (event.getRawSlot() > 8) return;
                if (!(villagerShop instanceof PlayerShop)) return;
                event.setCancelled(true);
                if (event.getRawSlot() == 3) {
                    player.sendMessage(new Color.Builder().path("messages.sold_shop").addPrefix().build());
                    player.playSound(player.getLocation(), Sound.valueOf(mainConfig.getString("sounds.sell_shop")), 0.5f, 1);
                    player.playSound(player.getLocation(), Sound.valueOf(mainConfig.getString("sounds.menu_click")), 0.5f, 1);

                    PlayerShop playerShop = (PlayerShop) villagerShop;
                    playerShop.abandon();
                    event.getView().close();
                }
                if (event.getRawSlot() == 5) {
                    player.openInventory(villagerShop.getInventory(ShopMenu.EDIT_SHOP));
                    player.playSound(player.getLocation(), Sound.valueOf(mainConfig.getString("sounds.back")), 0.5f, 1);
                    event.getView().close();
                }
                break;
            //Details
            case 7:
                if (!(event.getRawSlot() < villagerShop.getShopfrontSize())) return;
                if (event.getCurrentItem() == null) return;
                event.setCancelled(true);
                if (event.getRawSlot() == villagerShop.getShopfrontSize() - 1) {
                    player.playSound(player.getLocation(), Sound.valueOf(mainConfig.getString("sounds.menu_click")), 0.5f, 1);
                    player.openInventory(villagerShop.getInventory(ShopMenu.SHOPFRONT));
                } else {
                    player.sendMessage(new Color.Builder().path("messages.must_be_menulore").addPrefix().build());
                }
                break;
        }
    }
    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        Player player = (Player) event.getWhoClicked();
        if (VMPlugin.clickMap.containsKey(player.getUniqueId().toString())) { return; }

        String title = ChatColor.stripColor(event.getView().getTitle());
        if (menus.contains(title)) {
            if (title.equals(menus.get(5))) return;
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
        String detailSuffix = new Color.Builder().path("menus.shopfront.detail_suffix").build();
        menus.add(7, ChatColor.stripColor(new Color.Builder().path("menus.shopfront.title").build()) + " " + ChatColor.stripColor(detailSuffix));
    }
}
