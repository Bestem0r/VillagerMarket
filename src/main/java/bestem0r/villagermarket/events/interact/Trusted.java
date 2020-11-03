package bestem0r.villagermarket.events.interact;

import bestem0r.villagermarket.shops.PlayerShop;
import bestem0r.villagermarket.shops.VillagerShop;
import bestem0r.villagermarket.utilities.Color;
import bestem0r.villagermarket.utilities.Methods;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;

public class Trusted implements Listener {

    public enum Action {
        ADD,
        REMOVE
    }

    private final Player sender;
    private final Player target;
    private final Action action;

    public Trusted(Player sender, Player target, Action action) {
        this.sender = sender;
        this.target = target;
        this.action = action;
    }

    @EventHandler (priority = EventPriority.LOWEST)
    public void onEntityInteract(PlayerInteractEntityEvent event) {
        if (event.getPlayer() != sender) return;
        event.setCancelled(true);

        VillagerShop villagerShop = Methods.shopFromUUID(event.getRightClicked().getUniqueId());
        if (villagerShop != null) {
            if (villagerShop instanceof PlayerShop) {
                PlayerShop playerShop = (PlayerShop) villagerShop;
                if (!playerShop.getOwnerUUID().equals(sender.getUniqueId().toString())) {
                    sender.sendMessage(new Color.Builder().path("messages.not_owner").addPrefix().build());
                    return;
                }
                switch (action) {
                    case ADD:
                        playerShop.addTrusted(target);
                        sender.sendMessage(new Color.Builder().path("messages.trusted_added").addPrefix().build());
                        break;
                    case REMOVE:
                        playerShop.removeTrusted(target);
                        sender.sendMessage(new Color.Builder().path("messages.trusted_removed").addPrefix().build());
                        break;
                }
            } else {
                sender.sendMessage(new Color.Builder().path("messages.not_playershop").addPrefix().build());
            }
        } else {
            sender.sendMessage(new Color.Builder().path("messages.no_villager_shop").addPrefix().build());
        }
        HandlerList.unregisterAll(this);
    }
}
