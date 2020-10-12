package bestem0r.villagermarket.utilities;

import bestem0r.villagermarket.VMPlugin;
import bestem0r.villagermarket.shops.VillagerShop;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Villager;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.UUID;

public abstract class Methods {

    /** Returns Villager Shop based on EntityUUID */
    public static VillagerShop shopFromUUID(UUID uuid) {
        for (VillagerShop villagerShop : VMPlugin.shops) {
            if (villagerShop.getEntityUUID().equals(uuid.toString())) return villagerShop;
        }
        return null;
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
        ItemStack[] itemStackSelection = Bukkit.createInventory(null, shopfrontSize * 9).getContents();
        ItemStack[] itemStackStorage = Bukkit.createInventory(null, storageSize * 9).getContents();

        config.set("storage", itemStackStorage);
        config.set("for_sale", itemStackSelection);

        ArrayList<Double> priceList = new ArrayList<>(Arrays.asList(new Double[shopfrontSize * 9]));
        ArrayList<String> modeList = new ArrayList<>(Arrays.asList(new String[shopfrontSize * 9]));
        Collections.fill(priceList, 0.0);
        Collections.fill(modeList, "SELL");

        config.set("prices", priceList);
        config.set("modes", modeList);

        try {
            config.save(file);
            VMPlugin.getInstance().addVillager(entityUUID, file, villagerType);
        } catch (IOException i) {
            Bukkit.getLogger().severe("Failed to save config!");
            i.printStackTrace();
        }
    }

    /** Spawns new Villager Entity and sets its attributes to default values */
    public static void spawnShop(Location location, String type, int storageSize, int shopSize, int cost, String duration) {
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
            Bukkit.getLogger().info(ChatColor.RED + "Unable to spawn Villager! Does WorldGuard deny mobspawn?");
        }
    }
}
