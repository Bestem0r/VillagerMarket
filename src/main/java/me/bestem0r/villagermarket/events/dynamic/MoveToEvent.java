package me.bestem0r.villagermarket.events.dynamic;

import me.bestem0r.villagermarket.shops.PlayerShop;
import me.bestem0r.villagermarket.shops.VillagerShop;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.UUID;

public class MoveToEvent implements Listener {

    private final Player player;
    private final VillagerShop villagerShop;

    public MoveToEvent(Player player, VillagerShop villagerShop) {
        this.player = player;
        this.villagerShop = villagerShop;
    }

    @EventHandler (priority = EventPriority.LOWEST)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getPlayer() != player) return;
        if (event.getClickedBlock() == null) return;
        event.setCancelled(true);

        Entity entity = Bukkit.getEntity(villagerShop.getEntityUUID());
        if (villagerShop instanceof PlayerShop) {
            ((PlayerShop) villagerShop).updateRedstone(true);
        }
        if (entity != null) {
            entity.teleport(event.getClickedBlock().getLocation().add(0.5, 1, 0.5));
        }
        if (villagerShop instanceof PlayerShop) {
            ((PlayerShop) villagerShop).updateRedstone(false);
        }
        HandlerList.unregisterAll(this);
    }
}
