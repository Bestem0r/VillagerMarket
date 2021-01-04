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

public class SetCommand implements Listener {

    private final JavaPlugin plugin;
    private final Player player;
    private final VillagerShop villagerShop;
    private final ShopItem shopItem;

    public SetCommand(JavaPlugin plugin, Player player, VillagerShop villagerShop, ShopItem shopItem) {
        this.plugin = plugin;
        this.player = player;
        this.villagerShop = villagerShop;
        this.shopItem = shopItem;
    }


    @EventHandler
    public void onChat(AsyncPlayerChatEvent event) {
        if (event.getPlayer() != this.player) { return; }

        String cancel = plugin.getConfig().getString("cancel");
        event.setCancelled(true);
        if (event.getMessage().equalsIgnoreCase(cancel)) {
            player.sendMessage(new ColorBuilder(plugin).path("messages.cancelled").addPrefix().build());
            HandlerList.unregisterAll(this);
            return;
        }

        shopItem.setCommand(event.getMessage());

        Bukkit.getScheduler().runTask(plugin, () -> {
            villagerShop.getShopfrontHolder().open(player, Shopfront.Type.EDITOR);
        });
        HandlerList.unregisterAll(this);
    }
}
