package net.bestemor.villagermarket.listener;

import net.bestemor.villagermarket.VMPlugin;
import net.bestemor.villagermarket.shop.ShopMenu;
import net.bestemor.villagermarket.shop.VillagerShop;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LightningStrike;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.*;

public class EntityListener implements Listener {
    
    private final VMPlugin plugin;
    
    public EntityListener(VMPlugin plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        Entity entity = event.getEntity();
        if (plugin.getShopManager().getShop(event.getEntity().getUniqueId()) != null) {
            plugin.getShopManager().removeShop(entity.getUniqueId());
        }
    }

    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        if (event.getEntity().isInvulnerable() && plugin.getShopManager().getShop(event.getEntity().getUniqueId()) != null) {
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
    public void onLightningStrike(EntitySpawnEvent event) {
        Entity spawnedEntity = event.getEntity();
        if (spawnedEntity instanceof LightningStrike) {
            for (Entity entity : spawnedEntity.getNearbyEntities(2, 2, 2)) {
                if (plugin.getShopManager().getShop(entity.getUniqueId()) != null) {
                    event.setCancelled(true);
                }
            }
        }
    }

    @EventHandler
    public void onCareerChange(VillagerCareerChangeEvent event) {
        if (plugin.getShopManager().getShop(event.getEntity().getUniqueId()) != null) {
            event.setCancelled(true);
        }
    }

}
