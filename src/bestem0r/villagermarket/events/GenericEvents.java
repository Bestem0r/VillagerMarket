package bestem0r.villagermarket.events;

import bestem0r.villagermarket.*;
import bestem0r.villagermarket.utilities.ColorBuilder;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.*;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.*;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

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
}
