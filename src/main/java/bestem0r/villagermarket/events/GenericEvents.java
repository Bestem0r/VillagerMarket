package bestem0r.villagermarket.events;

import bestem0r.villagermarket.DataManager;
import bestem0r.villagermarket.VMPlugin;
import bestem0r.villagermarket.shops.VillagerShop;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.WorldLoadEvent;

import java.io.File;
import java.util.Objects;

public class GenericEvents implements Listener {

    DataManager dataManager;
    
    public GenericEvents(VMPlugin plugin, DataManager dataManager) {
        this.dataManager = dataManager;
    }

    @EventHandler
    public void onChunk(ChunkLoadEvent event) {
        for (Entity entity : event.getChunk().getEntities()) {
            if (dataManager.getVillagerEntities().contains(entity)) { continue; }
            if (dataManager.getVillagers().containsKey(entity.getUniqueId().toString())) {
                dataManager.getVillagerEntities().add(entity);
            }
        }
    }

    @EventHandler
    public void onWorldLoad(WorldLoadEvent event) {
        World world = event.getWorld();
        for (Entity entity : world.getEntities()) {
            if (dataManager.getVillagerEntities().contains(entity)) { continue; }
            if (dataManager.getVillagers().containsKey(entity.getUniqueId().toString())) {
                dataManager.getVillagerEntities().add(entity);
            }
        }
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        Entity entity = event.getEntity();
        String entityUUID = entity.getUniqueId().toString();
        File file = new File(Objects.requireNonNull(Bukkit.getServer().getPluginManager().getPlugin("VillagerMarket")).getDataFolder() + "/Shops/", entityUUID + ".yml");
        if (file.exists()) {
            file.delete();
            dataManager.getVillagers().remove(entityUUID);
        }
    }

    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        String entityUUID = event.getEntity().getUniqueId().toString();
        if (dataManager.getVillagers().containsKey(entityUUID)) {
            event.setCancelled(true);
        }
    }
    @EventHandler
    public void onHitEntity(EntityDamageByEntityEvent event) {
        String entityUUID = event.getEntity().getUniqueId().toString();
        if (dataManager.getVillagers().containsKey(entityUUID)) {
            if (event.getDamager() instanceof Player) {
                Player player = (Player) event.getDamager();
                if (!player.isSneaking()) return;
                if (!player.hasPermission("villagermarket.spy")) return;
                VillagerShop villagerShop = dataManager.getVillagers().get(entityUUID);
                player.openInventory(villagerShop.getInventory(VillagerShop.ShopMenu.STORAGE));
            }
        }
    }
}
