package me.bestem0r.villagermarket.events.dynamic;

import me.bestem0r.villagermarket.shops.VillagerShop;
import me.bestem0r.villagermarket.utilities.ColorBuilder;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import java.util.UUID;

public class SetSkin implements Listener {

    private final VillagerShop shop;
    private final UUID uuid;

    public SetSkin(VillagerShop shop, UUID uuid) {
        this.shop = shop;
        this.uuid = uuid;
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent event) {
        if (uuid.equals(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);

            shop.setCitizensSkin(event.getMessage());
            event.getPlayer().sendMessage(new ColorBuilder(Bukkit.getPluginManager().getPlugin("VillagerMarket"))
                    .path("messages.skin_set").addPrefix().build());

            HandlerList.unregisterAll(this);
        }
    }
}
