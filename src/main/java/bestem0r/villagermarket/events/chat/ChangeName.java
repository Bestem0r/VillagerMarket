package bestem0r.villagermarket.events.chat;

import bestem0r.villagermarket.VMPlugin;
import bestem0r.villagermarket.shops.PlayerShop;
import bestem0r.villagermarket.shops.VillagerShop;
import bestem0r.villagermarket.utilities.Color;
import bestem0r.villagermarket.utilities.Methods;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import java.util.UUID;

public class ChangeName implements Listener {

    private final Player player;
    private final Entity villager;
    private final VillagerShop villagerShop;

    public ChangeName(Player player, String entityUUID) {
        this.player = player;
        this.villager = Bukkit.getEntity(UUID.fromString(entityUUID));
        this.villagerShop = Methods.shopFromUUID(UUID.fromString(entityUUID));
    }

    @EventHandler (priority = EventPriority.LOWEST)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        if (event.getPlayer() == this.player) {
            String cancel = VMPlugin.getInstance().getConfig().getString("cancel");
            if (event.getMessage().equalsIgnoreCase(cancel)) {
                player.sendMessage(new Color.Builder().path("messages.cancelled").addPrefix().build());
            } else {
                String name = ChatColor.translateAlternateColorCodes('&', event.getMessage());
                if (villagerShop instanceof PlayerShop) {
                    villager.setCustomName(new Color.Builder()
                            .path("villager.custom_name")
                            .replace("%player%", player.getName())
                            .replace("%custom_name%", name)
                            .build());
                } else {
                    villager.setCustomName(name);
                }
                player.sendMessage(new Color.Builder()
                        .path("messages.change_name_set")
                        .replace("%name%", name)
                        .addPrefix()
                        .build());
            }

            event.setCancelled(true);
            HandlerList.unregisterAll(this);
        }
    }
}
