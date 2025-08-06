package net.bestemor.villagermarket.shop;

import net.bestemor.villagermarket.VMPlugin;
import net.bestemor.villagermarket.utils.VMUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Villager;

public class EntityInfo {

    private final VMPlugin plugin;
    private final FileConfiguration config;
    private final VillagerShop shop;

    private String name = "Villager Shop";
    private Location location = null;

    public EntityInfo(VMPlugin plugin, FileConfiguration config, VillagerShop shop) {
        this.plugin = plugin;
        this.config = config;
        this.shop = shop;

        if (config.getString("entity.name") != null) {
            this.name = config.getString("entity.name");

            double x = config.getDouble("entity.location.x");
            double y = config.getDouble("entity.location.y");
            double z = config.getDouble("entity.location.z");

            World world = Bukkit.getWorld(config.getString("entity.location.world"));
            if (world != null) {
                this.location = new Location(world, x, y, z);
            }
        }
    }
    public void save() {
        if (plugin.isEnabled()) {
            Bukkit.getScheduler().runTask(plugin, this::saveSync);
        } else {
            saveSync();
        }
    }

    private boolean isProfession(String s) {
        if (s == null) {
            return false;
        }
        for (Villager.Profession profession : Villager.Profession.values()) {
            if (s.equals(profession.name())) {
                return true;
            }
        }
        return false;
    }

    private void saveSync() {
        Entity entity = VMUtils.getEntity(shop.getEntityUUID());
        if (entity != null) {
            if (plugin.isEnabled()) {
                Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> saveInfo(entity));
            } else {
                saveInfo(entity);
            }

        }
    }

    private void saveInfo(Entity entity) {
        if (entity instanceof Villager) {
            config.set("entity.profession", ((Villager) entity).getProfession().name());
        }
        config.set("entity.name", entity.getName());
        config.set("entity.location.x", entity.getLocation().getX());
        config.set("entity.location.y", entity.getLocation().getY());
        config.set("entity.location.z", entity.getLocation().getZ());
        config.set("entity.location.world", entity.getLocation().getWorld().getName());
    }

    public String getName() {
        return name;
    }

    public Location getLocation() {
        return location;
    }
}
