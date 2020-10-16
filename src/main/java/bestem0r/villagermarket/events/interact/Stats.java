package bestem0r.villagermarket.events.interact;

import bestem0r.villagermarket.VMPlugin;
import bestem0r.villagermarket.shops.VillagerShop;
import bestem0r.villagermarket.utilities.Color;
import bestem0r.villagermarket.utilities.Methods;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;

public class Stats implements Listener {

    private final Player player;

    public Stats(Player player) {
        this.player = player;
    }

    @EventHandler (priority = EventPriority.LOWEST)
    public void onInteract(PlayerInteractEntityEvent event) {
        if (event.getPlayer() != player) return;
        event.setCancelled(true);
        VillagerShop villagerShop = Methods.shopFromUUID(event.getRightClicked().getUniqueId());

        if (villagerShop != null) {
            if (!player.hasPermission("villagermarket.spy") && !villagerShop.getOwnerUUID().equals(player.getUniqueId().toString())) {
                player.sendMessage(new Color.Builder().path("messages.not_owner").addPrefix().build());
                return;
            }
            villagerShop.sendStats(player);
        } else {
            player.sendMessage(new Color.Builder().path("messages.no_villager_shop").addPrefix().build());
        }
        HandlerList.unregisterAll(this);
    }
}

