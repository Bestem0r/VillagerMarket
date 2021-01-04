package me.bestem0r.villagermarket.events.dynamic;

import me.bestem0r.villagermarket.items.ShopItem;
import me.bestem0r.villagermarket.shops.VillagerShop;
import me.bestem0r.villagermarket.utilities.ColorBuilder;
import me.bestem0r.villagermarket.utilities.Methods;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.plugin.java.JavaPlugin;

public class SetAmount implements Listener {

    private final JavaPlugin plugin;
    private final Player player;
    private final ShopItem.Builder builder;

    public SetAmount(JavaPlugin plugin, Player player, ShopItem.Builder builder) {
        this.plugin = plugin;
        this.player = player;
        this.builder = builder;
    }

    @EventHandler (priority = EventPriority.LOWEST)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        if (event.getPlayer() != player) return;
        String message = event.getMessage();
        String cancel = plugin.getConfig().getString("cancel");

        event.setCancelled(true);
        if (message.equalsIgnoreCase(cancel)) {
            player.sendMessage(new ColorBuilder(plugin).path("messages.cancelled").addPrefix().build());
            HandlerList.unregisterAll(this);
            return;
        }
        if (!canConvert(message)) {
            player.sendMessage(new ColorBuilder(plugin).path("messages.not_number").addPrefix().build());
            return;
        } else if (Integer.parseInt(message) > 64 || Integer.parseInt(message) < 1) {
            player.sendMessage(new ColorBuilder(plugin).path("messages.not_valid_range").addPrefix().build());
            return;
        }
        builder.amount(Integer.parseInt(event.getMessage()));

        player.sendMessage(new ColorBuilder(plugin).path("messages.amount_successful").addPrefix().build());
        player.sendMessage(new ColorBuilder(plugin).path("messages.type_price").addPrefix().build());
        player.sendMessage(new ColorBuilder(plugin).path("messages.type_cancel").replace("%cancel%", cancel).addPrefix().build());

        Bukkit.getServer().getPluginManager().registerEvents(new SetPrice(plugin, player, builder), plugin);
        HandlerList.unregisterAll(this);
    }

    @EventHandler (priority = EventPriority.LOWEST)
    public void onEntityInteract(PlayerInteractEntityEvent event) {
        if (event.getPlayer() != player) { return; }
        VillagerShop villagerShop = Methods.shopFromUUID(event.getRightClicked().getUniqueId());
        if (villagerShop != null) {
            player.sendMessage(new ColorBuilder(plugin).path("messages.finish_process").addPrefix().build());
            event.setCancelled(true);
        }
    }


    private Boolean canConvert(String string) {
        for (int i = 0; i < string.length(); i++) {
            char c = string.charAt(i);
            if (c < '0' || c > '9') {
                return false;
            }
        }
        return true;
    }
}
