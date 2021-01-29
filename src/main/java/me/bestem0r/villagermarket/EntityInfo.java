package me.bestem0r.villagermarket;

import me.bestem0r.villagermarket.shops.VillagerShop;
import me.bestem0r.villagermarket.utilities.Methods;
import org.bukkit.*;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Villager;

import java.io.File;
import java.util.Locale;
import java.util.UUID;

public class EntityInfo {

    private final VMPlugin plugin;
    private final FileConfiguration config;

    private UUID entityUUID;
    private String name = "Villager Shop";
    private Villager.Profession profession = Villager.Profession.NONE;
    private int chunkX = 0;
    private int chunkZ = 0;
    private Location location = null;

    public EntityInfo(VMPlugin plugin, FileConfiguration config, UUID entityUUID) {
        this.plugin = plugin;
        this.config = config;
        this.entityUUID = entityUUID;

        if (config.getString("villager.name") != null) {
            this.name = config.getString("villager.name");
            this.profession = Villager.Profession.valueOf(config.getString("villager.profession"));
            double x = config.getDouble("villager.location.x");
            double y = config.getDouble("villager.location.y");
            double z = config.getDouble("villager.location.z");
            World world = Bukkit.getWorld(config.getString("villager.location.world"));
            if (world != null) {
                this.location = new Location(world, x, y, z);
                chunkX = location.getChunk().getX();
                chunkZ = location.getChunk().getZ();
            }
        }
    }
    public void save() {
        Villager villager = (Villager) Bukkit.getEntity(entityUUID);
        if (villager != null) {
            config.set("villager.profession", villager.getProfession().name());
            config.set("villager.name", villager.getName());
            config.set("villager.location.x", villager.getLocation().getX());
            config.set("villager.location.y", villager.getLocation().getY());
            config.set("villager.location.z", villager.getLocation().getZ());
            config.set("villager.location.world", villager.getLocation().getWorld().getName());
        }
    }
    public boolean hasStoredData() {
        return (location != null);
    }
    public boolean isInChunk(Chunk chunk) {
        return (chunk.getX() == chunkX && chunk.getZ() == chunkZ);
    }

    public UUID getEntityUUID() {
        return entityUUID;
    }

    public boolean exists() {
        return (Bukkit.getEntity(entityUUID) != null);
    }
    public void appendToExisting() {
        Villager villager = (Villager) Bukkit.getEntity(entityUUID);
        if (villager != null) {
            villager.setCustomName(name);
            villager.setProfession(profession);
        }
    }
    public void reCreate() {
        VillagerShop villagerShop = Methods.shopFromUUID(entityUUID);
        this.entityUUID = Methods.spawnShop(plugin, location, "none");
        if (villagerShop != null) {
            villagerShop.changeUUID(entityUUID);
        }

        Villager villager = (Villager) Bukkit.getEntity(entityUUID);

        if (villager != null) {
            appendToExisting();
        } else {
            Bukkit.getLogger().severe(ChatColor.RED + "Unable to (re)spawn Villager! Does WorldGuard deny mobs pawn?");
        }
    }
}
