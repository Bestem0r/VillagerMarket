package net.bestemor.villagermarket.event.interact;

import net.bestemor.villagermarket.shop.ShopItem;
import net.bestemor.villagermarket.shop.VillagerShop;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.jetbrains.annotations.NotNull;

public abstract class InteractWithShopEvent extends PlayerEvent implements Cancellable {
    private static final HandlerList HANDLERS_LIST = new HandlerList();

    private final ShopItem shopItem;
    private final VillagerShop shop;
    private boolean cancelled;

    public InteractWithShopEvent(@NotNull Player who,VillagerShop shop, ShopItem shopItem) {
        super(who);
        this.shop = shop;
        this.shopItem = shopItem;
    }

    public VillagerShop getShop() {
        return shop;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean b) {
        this.cancelled = b;
    }

    @NotNull
    @Override
    public HandlerList getHandlers() {
        return HANDLERS_LIST;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS_LIST;
    }

    public ShopItem getShopItem() {
        return shopItem;
    }
}
