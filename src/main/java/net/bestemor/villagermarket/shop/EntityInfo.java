package net.bestemor.villagermarket.shop;

import net.bestemor.core.config.VersionUtils;
import net.bestemor.villagermarket.VMPlugin;
import net.bestemor.villagermarket.utils.VMUtils;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Villager;

import java.util.UUID;

public class EntityInfo {

    private final VMPlugin plugin;
    private final FileConfiguration config;
    private final VillagerShop shop;

    private String name = "Villager Shop";
    private Villager.Profession profession = VersionUtils.getMCVersion() > 13 ? Villager.Profession.NONE : Villager.Profession.FARMER;
    private int chunkX = 0;
    private int chunkZ = 0;
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

            String professionString = config.getString("entity.profession");
            if (isProfession(professionString)) {
                this.profession = Villager.Profession.valueOf(professionString);
            } else {
                this.profession = VersionUtils.getMCVersion() < 14 ? Villager.Profession.FARMER : Villager.Profession.NONE;
            }
            World world = Bukkit.getWorld(config.getString("entity.location.world"));
            if (world != null) {
                this.location = new Location(world, x, y, z);
                chunkX = location.getChunk().getX();
                chunkZ = location.getChunk().getZ();
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

    public boolean hasStoredData() {
        return (location != null);
    }
    public boolean isInChunk(Chunk chunk) {
        return (chunk.getX() == chunkX && chunk.getZ() == chunkZ);
    }

    public UUID getEntityUUID() {
        return shop.getEntityUUID();
    }

    public boolean exists() {
        return (VMUtils.getEntity(shop.getEntityUUID()) != null);
    }

    public void appendToExisting() {
        Entity entity = VMUtils.getEntity(shop.getEntityUUID());
        if (entity instanceof Villager) {
            Villager villager = (Villager) entity;
            villager.setCustomName(name);
            villager.setProfession(profession);
        }
    }
    public void recreate() {
        Entity entity = plugin.getShopManager().spawnShop(location, "none");

        Villager villager = (Villager) Bukkit.getEntity(entity.getUniqueId());

        if (villager != null) {
            shop.setUUID(entity.getUniqueId());
            appendToExisting();
        } else {
            entity.remove();
        }
    }

    public Location getLocation() {
        return location;
    }
}
