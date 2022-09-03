package net.bestemor.villagermarket.utils;

import org.apache.commons.io.FileUtils;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

public class VMUtils {

    private VMUtils() {}

    /** Convert duration string to seconds */
    public static int secondsFromString(String string) {
        if (string.equalsIgnoreCase("infinite")) { return 0; }

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

    /** Properly checks if the two ItemStacks are equal  */
    public static boolean compareItems(ItemStack item1, ItemStack item2) {
        if (item1 == null || item2 == null) { return false; }
        ItemStack item1clone = item1.clone();
        ItemStack item2clone = item2.clone();

        item1clone.setAmount(1);
        item2clone.setAmount(1);

        return item1clone.toString().equals(item2clone.toString());
    }

    public static boolean hasComma(String s) {
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == ',') {
                return true;
            }
        }
        return false;
    }

    public static boolean isInteger(String s) {
        if (s.length() == 0) {
            return false;
        }
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c < '0' || c > '9') {
                return false;
            }
        }
        return true;
    }

    public static Entity getEntity(UUID uuid) {
        for (World world : Bukkit.getWorlds()) {
            for (Chunk chunk : world.getLoadedChunks()) {
                for (Entity entity : chunk.getEntities()) {
                    if (entity.getUniqueId().equals(uuid))
                        return entity;
                }
            }
        }
        return null;
    }

    public static void updateConfig(JavaPlugin plugin, String origin, String target) {

        // Do not update configs which are not yet created
        if (!new File(plugin.getDataFolder() + "/" + target + ".yml").exists()) {
            return;
        }

        File targetFile = new File(plugin.getDataFolder() + "/" + target + ".yml");
        FileConfiguration targetConfig = YamlConfiguration.loadConfiguration(targetFile);

        // Create temporary file to load as FileConfiguration
        InputStream inputStream = plugin.getResource(origin + ".yml");
        File originFile = new File(plugin.getDataFolder(), origin + "_tmp.yml");
        try {
            FileUtils.copyInputStreamToFile(inputStream, originFile);
        } catch (IOException | NullPointerException e) {
            e.printStackTrace();
        }
        FileConfiguration originConfig = YamlConfiguration.loadConfiguration(originFile);
        // Delete temporary file after loaded
        originFile.delete();

        boolean changes = false;
        // Check if any keys are missing in the target config
        for (String key : originConfig.getKeys(true)) {
            if (!targetConfig.contains(key)) {
                targetConfig.set(key, originConfig.get(key));
                changes = true;
            }
        }

        if (changes) {
            try {
                targetConfig.save(targetFile);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
