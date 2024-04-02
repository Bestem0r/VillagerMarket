package net.bestemor.villagermarket.event;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.jetbrains.annotations.NotNull;

public class PlaceShopEggEvent extends PlayerEvent implements Cancellable {

    private static final HandlerList HANDLERS_LIST = new HandlerList();

    private boolean cancelled = false;
    private final Location location;

    private final int shopSize;
    private final int storageSize;

    public PlaceShopEggEvent(@NotNull Player who, Location location, int shopSize, int storageSize) {
        super(who);
        this.location = location;
        this.shopSize = shopSize;
        this.storageSize = storageSize;
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
        return this.HANDLERS_LIST;
    }
    public static HandlerList getHandlerList() {
        return HANDLERS_LIST;
    }
    public Location getLocation() {
        return location;
    }

    public int getShopSize() {
        return shopSize;
    }

    public int getStorageSize() {
        return storageSize;
    }
}
