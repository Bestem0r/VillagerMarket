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


    public static void newShopConfig(String entityUUID, Villager villager, int storageSize, int shopfrontSize, int cost, String type) {
        File file = new File(Bukkit.getServer().getPluginManager().getPlugin("VillagerMarket").getDataFolder() + "/Shops/", entityUUID + ".yml");
        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        config.set("ownerUUID", "null");
        config.set("ownerName", "null");
        config.set("storageSize", storageSize);
        config.set("shopfrontSize", shopfrontSize);
        config.set("type", type);

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
            VMPlugin.getDataManager().addVillager(entityUUID, file, VillagerShop.VillagerType.valueOf(type.toUpperCase()));
            VMPlugin.getDataManager().getVillagerEntities().add(villager);
        } catch (IOException i) {
            Bukkit.getLogger().severe("Failed to save config!");
        }

    }
}
