package bestem0r.villagermarket.events.interact;

import bestem0r.villagermarket.VMPlugin;
import bestem0r.villagermarket.utilities.Color;
import bestem0r.villagermarket.utilities.Methods;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;

public class Move implements Listener {

    private final Player player;

    public Move(Player player) {
        this.player = player;
    }

    @EventHandler
    public void onInteract(PlayerInteractEntityEvent event) {
        if (event.getPlayer() != player) return;
        if (Methods.shopFromUUID(event.getRightClicked().getUniqueId()) != null) {
            player.sendMessage(new Color.Builder().path("messages.move_villager_to").addPrefix().build());
            Bukkit.getPluginManager().registerEvents(new MoveTo(player, event.getRightClicked()), VMPlugin.getInstance());
        } else {
            player.sendMessage(new Color.Builder().path("messages.no_villager_shop").addPrefix().build());
        }
        HandlerList.unregisterAll(this);
        event.setCancelled(true);
    }
}
