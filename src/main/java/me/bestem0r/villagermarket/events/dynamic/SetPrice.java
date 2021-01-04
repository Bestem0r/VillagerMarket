package me.bestem0r.villagermarket.events.dynamic;

import me.bestem0r.villagermarket.inventories.Shopfront;
import me.bestem0r.villagermarket.items.ShopItem;
import me.bestem0r.villagermarket.shops.PlayerShop;
import me.bestem0r.villagermarket.shops.VillagerShop;
import me.bestem0r.villagermarket.utilities.ColorBuilder;
import me.bestem0r.villagermarket.utilities.Methods;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.math.BigDecimal;
import java.util.UUID;

public class SetPrice implements Listener {

    private final JavaPlugin plugin;
    private final Player player;
    private final ShopItem.Builder builder;

    public SetPrice(JavaPlugin plugin, Player player, ShopItem.Builder builder) {
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
            player.sendMessage(hasComma(message) ? new ColorBuilder(plugin).path("messages.use_dot").addPrefix().build() : new ColorBuilder(plugin).path("messages.not_number").addPrefix().build());
            return;
        } else if (Double.parseDouble(message) < 0) {
            player.sendMessage(new ColorBuilder(plugin).path("messages.negative_price").addPrefix().build());
            return;
        }
        builder.price(new BigDecimal(message));

        String entityUUID = builder.getEntityUUID();
        VillagerShop villagerShop = Methods.shopFromUUID(UUID.fromString(entityUUID));

        ShopItem shopItem = builder.build();
        shopItem.refreshLore(villagerShop);
        villagerShop.getItemList().put(shopItem.getSlot(), shopItem);

        player.sendMessage(new ColorBuilder(plugin).path("messages.add_successful").addPrefix().build());
        HandlerList.unregisterAll(this);

        if (villagerShop instanceof PlayerShop) {
            Bukkit.getScheduler().runTask(plugin, () -> {
                villagerShop.getShopfrontHolder().open(player, Shopfront.Type.EDITOR);
                player.playSound(player.getLocation(), Sound.valueOf(plugin.getConfig().getString("sounds.add_item")), 0.5f, 1);
            });
        } else {
            player.sendMessage(new ColorBuilder(plugin).path("messages.type_limit_admin").addPrefix().build());
            player.sendMessage(new ColorBuilder(plugin).path("messages.type_cancel").addPrefix().replace("%cancel%", cancel).build());
            Bukkit.getPluginManager().registerEvents(new SetLimit(plugin, player, villagerShop, shopItem), plugin);
        }
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
            if ((c < '0' || c > '9') && c != '.') {
                return false;
            }
        }
        return true;
    }
    private Boolean hasComma(String string) {
        for (int i = 0; i < string.length(); i++) {
            char c = string.charAt(i);
            if (c == ',') {
                return true;
            }
        }
        return false;
    }
}
