package net.bestemor.villagermarket.listener;

import net.bestemor.core.config.VersionUtils;
import net.bestemor.villagermarket.VMPlugin;
import net.bestemor.villagermarket.shop.ShopMenu;
import net.bestemor.villagermarket.shop.VillagerShop;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.VillagerCareerChangeEvent;
import org.bukkit.event.weather.LightningStrikeEvent;

public class EntityListener implements Listener {
    
    private final VMPlugin plugin;
    
    public EntityListener(VMPlugin plugin) {
        this.plugin = plugin;
        if (VersionUtils.getMCVersion() > 13) {
            Bukkit.getPluginManager().registerEvents(new Listener() {
                @EventHandler
                public void onCareerChange(VillagerCareerChangeEvent event) {
                    if (plugin.getShopManager().getShop(event.getEntity().getUniqueId()) != null) {
                        event.setCancelled(true);
                    }
                }
            }, plugin);
        }
    }
    
    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        Entity entity = event.getEntity();
        if (plugin.getShopManager().getShop(event.getEntity().getUniqueId()) != null) {
            plugin.getShopManager().removeShop(entity.getUniqueId());
        }
    }

    @EventHandler (priority = EventPriority.HIGH)
    public void onEntityDamage(EntityDamageEvent event) {
        if (plugin.getShopManager().getShop(event.getEntity().getUniqueId()) != null) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onHitEntity(EntityDamageByEntityEvent event) {
        if (plugin.getShopManager().getShop(event.getEntity().getUniqueId()) != null) {
            if (event.getDamager() instanceof Player) {
                Player player = (Player) event.getDamager();
                if (!player.isSneaking()) return;
                if (!player.hasPermission("villagermarket.spy")) return;
                VillagerShop villagerShop = plugin.getShopManager().getShop(event.getEntity().getUniqueId());
                villagerShop.updateMenu(ShopMenu.EDIT_SHOP);
                villagerShop.openInventory(player, ShopMenu.EDIT_SHOP);
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onLightningStrike(LightningStrikeEvent event) {
        for (Entity entity : event.getLightning().getNearbyEntities(4, 4, 4)) {
            if (plugin.getShopManager().getShop(entity.getUniqueId()) != null) {
                event.setCancelled(true);
            }
        }
    }
}
