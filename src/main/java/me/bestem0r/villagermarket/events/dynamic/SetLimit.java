package me.bestem0r.villagermarket.events.dynamic;

import me.bestem0r.villagermarket.inventories.Shopfront;
import me.bestem0r.villagermarket.items.ShopItem;
import me.bestem0r.villagermarket.shops.VillagerShop;
import me.bestem0r.villagermarket.utilities.ColorBuilder;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.plugin.java.JavaPlugin;

public class SetLimit implements Listener {

    private final JavaPlugin plugin;
    private final Player player;
    private final VillagerShop villagerShop;
    private final ShopItem shopItem;

    public SetLimit(JavaPlugin plugin, Player player, VillagerShop villagerShop, ShopItem shopItem) {
        this.plugin = plugin;
        this.player = player;
        this.villagerShop = villagerShop;
        this.shopItem = shopItem;
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent event) {
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
        }
        shopItem.setLimit(Integer.parseInt(message));
        Bukkit.getScheduler().runTask(plugin, () ->
                villagerShop.getShopfrontHolder().open(player, Shopfront.Type.EDITOR));
        HandlerList.unregisterAll(this);
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
