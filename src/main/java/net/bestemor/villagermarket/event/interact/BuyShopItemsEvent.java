package net.bestemor.villagermarket.event.interact;

import net.bestemor.villagermarket.shop.ShopItem;
import net.bestemor.villagermarket.shop.VillagerShop;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class BuyShopItemsEvent extends InteractWithShopEvent {
    private static final HandlerList HANDLERS_LIST = new HandlerList();


    public BuyShopItemsEvent(@NotNull Player who, VillagerShop shop, ShopItem shopItem) {
        super(who,shop,shopItem);
    }

    /** Rest of file is required boilerplate for custom events **/
    @NotNull
    @Override
    public HandlerList getHandlers() {
        return HANDLERS_LIST;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS_LIST;
    }

}
