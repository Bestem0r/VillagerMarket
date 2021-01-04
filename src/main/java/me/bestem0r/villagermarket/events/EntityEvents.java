package me.bestem0r.villagermarket.events;

import me.bestem0r.villagermarket.VMPlugin;
import me.bestem0r.villagermarket.shops.ShopMenu;
import me.bestem0r.villagermarket.shops.VillagerShop;
import me.bestem0r.villagermarket.utilities.Methods;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LightningStrike;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.*;

import java.io.File;
import java.util.Objects;

public class EntityEvents implements Listener {
    
    private final VMPlugin plugin;
    
    public EntityEvents(VMPlugin plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        Entity entity = event.getEntity();
        File file = new File(Objects.requireNonNull(Bukkit.getServer().getPluginManager().getPlugin("VillagerMarket")).getDataFolder() + "/Shops/", event.getEntity().getUniqueId().toString() + ".yml");
        if (file.exists()) {
            file.delete();
            VMPlugin.shops.remove(Methods.shopFromUUID(entity.getUniqueId()));
        }
    }

    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        if (Methods.shopFromUUID(event.getEntity().getUniqueId()) != null) {
            event.setCancelled(true);
        }
    }
    @EventHandler
    public void onHitEntity(EntityDamageByEntityEvent event) {
        if (Methods.shopFromUUID(event.getEntity().getUniqueId()) != null) {
            if (event.getDamager() instanceof Player) {
                Player player = (Player) event.getDamager();
                if (!player.isSneaking()) return;
                if (!player.hasPermission("villagermarket.spy")) return;
                VillagerShop villagerShop = Methods.shopFromUUID(event.getEntity().getUniqueId());
                villagerShop.openInventory(player, ShopMenu.EDIT_SHOP);
            }
        }
    }

    @EventHandler
    public void onLightningStrike(EntitySpawnEvent event) {
        Entity spawnedEntity = event.getEntity();
        if (spawnedEntity instanceof LightningStrike) {
            for (Entity entity : spawnedEntity.getNearbyEntities(2, 2, 2)) {
                if (Methods.shopFromUUID(event.getEntity().getUniqueId()) != null) {
                    event.setCancelled(true);
                }
            }
        }
    }

    @EventHandler
    public void onCareerChange(VillagerCareerChangeEvent event) {
        if (Methods.shopFromUUID(event.getEntity().getUniqueId()) != null) {
            event.setCancelled(true);
        }
    }

}
