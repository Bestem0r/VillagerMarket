package me.bestem0r.villagermarket.utilities;

import me.bestem0r.villagermarket.VMPlugin;
import me.bestem0r.villagermarket.shops.VillagerShop;
import org.bukkit.*;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Villager;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class Methods {

    private Methods() {}

    private static final List<Villager.Profession> professions = Arrays.asList(
            Villager.Profession.ARMORER,
            Villager.Profession.BUTCHER,
            Villager.Profession.CARTOGRAPHER,
            Villager.Profession.CLERIC,
            Villager.Profession.FARMER,
            Villager.Profession.FISHERMAN,
            Villager.Profession.LEATHERWORKER,
            Villager.Profession.LIBRARIAN
    );

    /** Returns Villager Shop based on EntityUUID */
    public static VillagerShop shopFromUUID(UUID uuid) {
        for (VillagerShop villagerShop : VMPlugin.shops) {
            if (villagerShop.getEntityUUID().equals(uuid.toString())) return villagerShop;
        }
        return null;
    }

    /** Creates ItemStack from path in config.yml */
    public static ItemStack stackFromPath(JavaPlugin plugin, String path) {
        FileConfiguration config = plugin.getConfig();
        ItemStack item = new ItemStack(Material.valueOf(config.getString(path + ".material")));

        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(new ColorBuilder(plugin).path(path + ".name").build());
        meta.setLore(new ColorBuilder(plugin).path(path + ".lore").buildLore());

        item.setItemMeta(meta);
        return item;
    }

    /** Returns list of professions */
    public static List<Villager.Profession> getProfessions() {
        return professions;
    }

    /** Saves/Resets Villager Config with default values */
    public static void newShopConfig(VMPlugin plugin, UUID entityUUID, int storageSize, int shopfrontSize, int cost, VillagerShop.VillagerType villagerType, String duration) {
        File file = new File(Bukkit.getServer().getPluginManager().getPlugin("VillagerMarket").getDataFolder() + "/Shops/", entityUUID + ".yml");
        FileConfiguration config = YamlConfiguration.loadConfiguration(file);

        config.set("ownerUUID", "null");
        config.set("ownerName", "null");
        config.set("storageSize", storageSize);
        config.set("shopfrontSize", shopfrontSize);
        config.set("type", villagerType.toString().toLowerCase());
        config.set("duration", duration);
        config.set("expire", 0);
        config.set("times_rented", 1);

        config.set("stats.items_bought", 0);
        config.set("stats.items_sold", 0);
        config.set("stats.money_earned", 0);
        config.set("stats.money_spent", 0);

        config.set("cost", cost);
        config.set("items_for_sale", null);
        config.set("storage", null);

        try {
            config.save(file);
            plugin.addVillager(entityUUID, file, villagerType);
        } catch (IOException i) {
            i.printStackTrace();
        }
    }

    /** Convert duration string to seconds */
    public static int secondsFromString(String string) {
        if (string.equalsIgnoreCase("infinite")) return 0;

        String unit = string.substring(string.length() - 1);
        int size = Integer.parseInt(string.substring(0, string.length() - 1));
        switch (unit) {
            case "s":
                return size;
            case "m":
                return size * 60;
            case "h":
                return size * 3600;
            case "d":
                return size * 86400;
            default:
                Bukkit.getLogger().severe("Could not convert unit: " + unit);
                return 0;
        }
    }

    /** Returns true if item is blacklisted, false if not */
    public static boolean isBlackListed(JavaPlugin plugin, Material material) {
        List<String> blackList = plugin.getConfig().getStringList("item_blacklist");
        return blackList.contains(material.toString());
    }

    /** Properly checks if the two ItemStacks are equal  */
    public static boolean compareItems(ItemStack item1, ItemStack item2) {
        if (item1 == null || item2 == null) { return false; }
        ItemStack item1clone = item1.clone();
        ItemStack item2clone = item2.clone();

        item1clone.setAmount(1);
        item2clone.setAmount(1);

        return  item1clone.toString().equals(item2clone.toString());
    }

    /** Spawns new Villager Entity and sets its attributes to default values */
    public static UUID spawnShop(VMPlugin plugin, Location location, String type) {
        World world = location.getWorld();
        Villager villager = (Villager) world.spawnEntity(location, EntityType.VILLAGER);

        villager.setInvulnerable(true);
        villager.setCollidable(false);
        villager.setSilent(true);
        villager.setAI(plugin.getConfig().getBoolean("villager.ai"));
        villager.setProfession(Villager.Profession.valueOf(plugin.getConfig().getString("villager.default_profession")));
        String namePath = (type.equalsIgnoreCase("player") ? "name_available" : "name_admin");
        villager.setCustomName(new ColorBuilder(plugin).path("villager." + namePath).build());

        //Check if Villager is spawned correctly
        return villager.getUniqueId();
    }
}
