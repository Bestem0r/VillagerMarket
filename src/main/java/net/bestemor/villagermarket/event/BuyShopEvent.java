package net.bestemor.villagermarket.event;

import net.bestemor.villagermarket.shop.PlayerShop;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.jetbrains.annotations.NotNull;

public class BuyShopEvent extends PlayerEvent implements Cancellable {

    private final HandlerList HANDLERS_LIST = new HandlerList();

    private final PlayerShop shop;
    private boolean cancelled;

    public BuyShopEvent(@NotNull Player who, PlayerShop shop) {
        super(who);
        this.shop = shop;
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

    public PlayerShop getShop() {
        return shop;
    }
}
