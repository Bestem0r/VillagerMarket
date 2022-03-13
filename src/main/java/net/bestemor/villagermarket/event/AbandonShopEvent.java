package net.bestemor.villagermarket.event;

import net.bestemor.villagermarket.shop.PlayerShop;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class AbandonShopEvent extends Event  {

    private final HandlerList HANDLERS_LIST = new HandlerList();

    private final PlayerShop shop;

    public AbandonShopEvent(PlayerShop shop) {
        this.shop = shop;
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
