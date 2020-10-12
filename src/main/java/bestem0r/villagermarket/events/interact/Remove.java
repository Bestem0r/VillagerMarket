package bestem0r.villagermarket.events.interact;

import bestem0r.villagermarket.VMPlugin;
import bestem0r.villagermarket.shops.VillagerShop;
import bestem0r.villagermarket.utilities.Color;
import bestem0r.villagermarket.utilities.Methods;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;

import java.io.File;
import java.util.Objects;

public class Remove implements Listener {

    private final Player player;

    public Remove(Player player) {
        this.player = player;
    }

    @EventHandler
    public void onInteract(PlayerInteractEntityEvent event) {
        if (event.getPlayer() != player) return;
        VillagerShop villagerShop = Methods.shopFromUUID(event.getRightClicked().getUniqueId());
        if (villagerShop != null) {
            player.sendMessage(new Color.Builder().path("messages.villager_removed").addPrefix().build());
            player.playSound(player.getLocation(), Sound.valueOf(VMPlugin.getInstance().getConfig().getString("sounds.remove_villager")), 0.5f, 1);

            File file = new File(Objects.requireNonNull(Bukkit.getServer().getPluginManager().getPlugin("VillagerMarket")).getDataFolder() + "/Shops/", event.getRightClicked().getUniqueId().toString() + ".yml");
            file.delete();

            VMPlugin.shops.remove(villagerShop);
            event.getRightClicked().remove();
        } else {
            player.sendMessage(new Color.Builder().path("messages.no_villager_shop").addPrefix().build());
        }
        HandlerList.unregisterAll(this);
        event.setCancelled(true);
    }
}

