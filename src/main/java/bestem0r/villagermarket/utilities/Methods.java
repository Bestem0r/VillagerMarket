package bestem0r.villagermarket.utilities;

import bestem0r.villagermarket.VMPlugin;
import bestem0r.villagermarket.shops.VillagerShop;
import org.bukkit.*;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Villager;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.util.*;

public abstract class Methods {

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

    /** Returns list of professions */
    public static List<Villager.Profession> getProfessions() {
        return professions;
    }

    /** Saves/Resets Villager Config with default values */
    public static void newShopConfig(UUID entityUUID, int storageSize, int shopfrontSize, int cost, VillagerShop.VillagerType villagerType, String duration) {
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
            VMPlugin.getInstance().addVillager(entityUUID, file, villagerType);
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

    /** Returns ItemStack[] from ItemStack ArrayList */
    public static ItemStack[] stacksFromArray(ArrayList<ItemStack> arrayList) {
        ItemStack[] stacks = new ItemStack[arrayList.size()];
        for (int i = 0; i < arrayList.size(); i++) {
            stacks[i] = arrayList.get(i);
        }
        return stacks;
    }

    /** Returns true if item is blacklisted, false if not */
    public static boolean isBlackListed(Material material) {
        FileConfiguration mainConfig = VMPlugin.getInstance().getConfig();
        List<String> blackList = mainConfig.getStringList("item_blacklist");
        return blackList.contains(material.toString());
    }

    /** Spawns new Villager Entity and sets its attributes to default values */
    public static UUID spawnShop(Location location, String type, int storageSize, int shopSize, int cost, String duration) {
        World world = location.getWorld();
        Villager villager = (Villager) world.spawnEntity(location, EntityType.VILLAGER);
        villager.setAI(false);
        villager.setInvulnerable(true);
        villager.setCollidable(false);
        villager.setSilent(true);
        villager.setProfession(Villager.Profession.valueOf(VMPlugin.getInstance().getConfig().getString("villager.default_profession")));
        String namePath = (type.equalsIgnoreCase("player") ? "name_available" : "name_admin");
        villager.setCustomName(new Color.Builder().path("villager." + namePath).build());

        //Check if Villager is spawned correctly
        if (Bukkit.getEntity(villager.getUniqueId()) != null) {
            newShopConfig(villager.getUniqueId(), storageSize, shopSize, cost, VillagerShop.VillagerType.valueOf(type.toUpperCase()), duration);
        } else {
            Bukkit.getLogger().info(ChatColor.RED + "Unable to spawn Villager! Does WorldGuard deny mobs pawn?");
        }
        return villager.getUniqueId();
    }
}
