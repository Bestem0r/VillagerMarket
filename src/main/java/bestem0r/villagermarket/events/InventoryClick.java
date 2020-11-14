package bestem0r.villagermarket.events;

import bestem0r.villagermarket.VMPlugin;
import bestem0r.villagermarket.shops.AdminShop;
import bestem0r.villagermarket.shops.PlayerShop;
import bestem0r.villagermarket.shops.ShopMenu;
import bestem0r.villagermarket.shops.VillagerShop;
import bestem0r.villagermarket.utilities.Color;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.*;

public class InventoryClick implements Listener {

    private final Player player;
    private final VillagerShop villagerShop;
    private final ShopMenu shopMenu;
    private final FileConfiguration mainConfig;

    public InventoryClick(Player player, VillagerShop villagerShop, ShopMenu shopMenu) {
        this.player = player;
        this.villagerShop = villagerShop;
        this.shopMenu = shopMenu;
        this.mainConfig = VMPlugin.getInstance().getConfig();
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if ((Player) event.getWhoClicked() != this.player) { return; }

        if (event.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY && shopMenu != ShopMenu.STORAGE) event.setCancelled(true);

        switch (shopMenu) {
            //Buy available
            case BUY_SHOP:
                if (event.getRawSlot() > 8) return;
                if (!(villagerShop instanceof PlayerShop)) return;
                event.setCancelled(true);
                if (event.getRawSlot() == 4) {
                    event.getView().close();
                    PlayerShop playerShop = (PlayerShop) villagerShop;
                    playerShop.buyShop(player);
                }
                break;
            //Edit shop
            case EDIT_SHOP:
                if (event.getRawSlot() > 8) return;
                event.setCancelled(true);
                villagerShop.editShopInteract(player, event);
                break;
            //Edit for sale
            case EDIT_SHOPFRONT:
                villagerShop.itemsInteract(player, event);
                break;
            //Buy/sell items
            case SHOPFRONT:
                if (!(event.getRawSlot() < villagerShop.getShopSize())) return;
                event.setCancelled(true);
                if (event.getCurrentItem() == null) return;
                villagerShop.customerInteract(event);
                break;
            //Edit villager
            case EDIT_VILLAGER:
                villagerShop.editVillagerInteract(event);
                break;
            //Storage
            case STORAGE:
                if (event.getRawSlot() == villagerShop.getStorageSize() - 1) {
                    player.playSound(player.getLocation(), Sound.valueOf(mainConfig.getString("sounds.back")), 0.5f, 1);
                    event.getView().close();
                    villagerShop.openInventory(player, ShopMenu.EDIT_SHOP);
                    event.setCancelled(true);
                }
                break;
            //Sell shop
            case SELL_SHOP:
                if (event.getRawSlot() > 8) return;
                if (!(villagerShop instanceof PlayerShop)) return;
                event.setCancelled(true);
                if (event.getRawSlot() == 3) {
                    PlayerShop playerShop = (PlayerShop) villagerShop;
                    playerShop.sell(player);
                    event.getView().close();
                }
                if (event.getRawSlot() == 5) {
                    villagerShop.openInventory(player, ShopMenu.EDIT_SHOP);
                    player.playSound(player.getLocation(), Sound.valueOf(mainConfig.getString("sounds.back")), 0.5f, 1);
                    event.getView().close();
                }
                break;
            //Details
            case SHOPFRONT_DETAILED:
                if (!(event.getRawSlot() < villagerShop.getShopSize())) return;
                if (event.getCurrentItem() == null) return;
                event.setCancelled(true);
                if (event.getRawSlot() == villagerShop.getShopSize() - 1) {
                    player.playSound(player.getLocation(), Sound.valueOf(mainConfig.getString("sounds.menu_click")), 0.5f, 1);
                    if (event.getClick() == ClickType.RIGHT) {
                        event.getView().close();
                        if (villagerShop.getOwnerUUID().equals(player.getUniqueId().toString()) || (villagerShop instanceof AdminShop && player.hasPermission("villagermarket.admin"))) {
                            villagerShop.openInventory(player, ShopMenu.EDIT_SHOP);
                        }
                    } else {
                        villagerShop.openInventory(player, ShopMenu.SHOPFRONT);
                    }
                } else {
                    player.sendMessage(new Color.Builder().path("messages.must_be_menulore").addPrefix().build());
                }
        }
    }
    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if ((Player) event.getWhoClicked() != this.player) { return; }
        if (shopMenu == ShopMenu.STORAGE) { return; }
        event.setCancelled(true);
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if ((Player) event.getPlayer() != this.player) { return; }
        if (shopMenu == ShopMenu.STORAGE) {
            villagerShop.updateShopInventories();
        }
        HandlerList.unregisterAll(this);
    }
}
