package bestem0r.villagermarket.events.interact;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;

public class MoveTo implements Listener {

    private final Player player;
    private final Entity entity;

    public MoveTo(Player player, Entity entity) {
        this.player = player;
        this.entity = entity;
    }

    @EventHandler (priority = EventPriority.LOWEST)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getPlayer() != player) return;
        if (event.getClickedBlock() == null) return;
        event.setCancelled(true);

        entity.teleport(event.getClickedBlock().getLocation().add(0.5, 1, 0.5));
        HandlerList.unregisterAll(this);
    }
}
