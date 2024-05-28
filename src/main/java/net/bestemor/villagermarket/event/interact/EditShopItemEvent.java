package net.bestemor.villagermarket.event.interact;

import net.bestemor.villagermarket.event.interact.enums.EditType;
import net.bestemor.villagermarket.shop.ShopItem;
import net.bestemor.villagermarket.shop.VillagerShop;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

import java.math.BigDecimal;

public class EditShopItemEvent extends InteractWithShopEvent {
    private static final HandlerList HANDLERS_LIST = new HandlerList();
    private BigDecimal newValue;
    private EditType type;
    public EditShopItemEvent(@NotNull Player who, VillagerShop shop, ShopItem shopItem,BigDecimal newValue,EditType type) {
        super(who,shop,shopItem);
        this.newValue = newValue;
        this.type = type;
    }

    public BigDecimal getNewValue() {
        return newValue;
    }

    public void setNewValue(BigDecimal newValue) {
        this.newValue = newValue;
    }

    public EditType getType() {
        return type;
    }

    public void setType(EditType type) {
        this.type = type;
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

