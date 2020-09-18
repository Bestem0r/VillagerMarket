package bestem0r.villagermarket.utilities;

import bestem0r.villagermarket.VMPlugin;
import bestem0r.villagermarket.shops.VillagerShop;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Villager;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

public class Config {

    //private static File pendingFile;
    //private static FileConfiguration pendingConfig;


    public static void newShopConfig(String entityUUID, Villager villager, int size, int cost, String type) {
        File file = new File(Bukkit.getServer().getPluginManager().getPlugin("VillagerMarket").getDataFolder() + "/Shops/", entityUUID + ".yml");
        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        config.set("ownerUUID", "null");
        config.set("ownerName", "null");
        config.set("size", size);
        config.set("type", type);

        config.set("cost", cost);
        ItemStack[] itemStackSelection = Bukkit.createInventory(null, size * 9).getContents();
        ItemStack[] itemStackStorage = Bukkit.createInventory(null, size * 18).getContents();

        config.set("storage", itemStackStorage);
        config.set("for_sale", itemStackSelection);

        ArrayList<Double> priceList = new ArrayList<>(Arrays.asList(new Double[size * 9]));
        ArrayList<String> modeList = new ArrayList<>(Arrays.asList(new String[size * 9]));
        Collections.fill(priceList, 0.0);
        Collections.fill(modeList, "SELL");

        config.set("prices", priceList);
        config.set("modes", modeList);

        try {
            config.save(file);
            VMPlugin.getDataManager().addVillager(entityUUID, file, VillagerShop.VillagerType.valueOf(type.toUpperCase()));
            VMPlugin.getDataManager().getVillagerEntities().add(villager);
        } catch (IOException i) {
            Bukkit.getLogger().severe("Failed to save config!");
        }

    }

    /*public static void savePending() {
        try {
            pendingConfig.save(pendingFile);
        } catch (IOException i) {}
    }
    public static void setupPendingConfig() {
        File file = new File(Bukkit.getServer().getPluginManager().getPlugin("VillagerMarket").getDataFolder(), "pending_transactions.yml");
        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        pendingFile = file;
        pendingConfig = config;
        if (!file.exists()) {
            savePending();
        }
    }
    public static FileConfiguration getPendingConfig() {
        return pendingConfig;
    }*/
}
