package me.bestem0r.villagermarket.events;

import me.bestem0r.villagermarket.shops.PlayerShop;
import me.bestem0r.villagermarket.shops.ShopMenu;
import me.bestem0r.villagermarket.shops.VillagerShop;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;

public class InventoryClick implements Listener {

    private final Player player;
    private final VillagerShop villagerShop;
    private final ShopMenu shopMenu;

    public InventoryClick(Player player, VillagerShop villagerShop, ShopMenu shopMenu) {
        this.player = player;
        this.villagerShop = villagerShop;
        this.shopMenu = shopMenu;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if ((Player) event.getWhoClicked() != this.player) { return; }

        event.setCancelled(event.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY && shopMenu != ShopMenu.STORAGE);

        switch (shopMenu) {
            //Buy available
            case BUY_SHOP:
                if (event.getRawSlot() > 8) return;
                if (!(villagerShop instanceof PlayerShop)) return;
                event.setCancelled(true);
                if (event.getRawSlot() == 4) {
                    PlayerShop playerShop = (PlayerShop) villagerShop;
                    playerShop.buyShop(player);
                }
                break;
            //Edit shop
            case EDIT_SHOP:
                if (event.getRawSlot() >= event.getView().getTopInventory().getSize()) return;
                event.setCancelled(true);
                villagerShop.editShopInteract(event);
                break;
            //Edit villager
            case EDIT_VILLAGER:
                villagerShop.editVillagerInteract(event);
                break;
            //Storage
            case STORAGE:
                if (villagerShop instanceof PlayerShop) {
                    PlayerShop playerShop = (PlayerShop) villagerShop;
                    playerShop.storageInteract(event);
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
                }
                if (event.getRawSlot() == 5) {
                    villagerShop.openInventory(player, ShopMenu.EDIT_SHOP);
                }
                break;
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
        HandlerList.unregisterAll(this);
    }
}
