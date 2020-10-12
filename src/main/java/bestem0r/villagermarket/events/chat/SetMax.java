package bestem0r.villagermarket.events.chat;

import bestem0r.villagermarket.items.ShopItem;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

public class SetMax implements Listener {

    private final Player player;
    private final ShopItem shopItem;

    public SetMax(Player player, ShopItem shopItem) {
        this.player = player;
        this.shopItem = shopItem;
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent event) {
        if (event.getPlayer() != player) return;

        shopItem.
    }
}
