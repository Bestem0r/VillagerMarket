package bestem0r.villagermarket.events;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerDropItemEvent;

public class ItemDrop implements Listener {

    private Player player;

    public ItemDrop(Player player) {
        this.player = player;
    }

    @EventHandler
    public void onItemDrop(PlayerDropItemEvent event) {
        if (event.getPlayer() == player) {
            event.setCancelled(true);
            HandlerList.unregisterAll(this);
        }
    }
}
