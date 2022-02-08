package me.bestem0r.villagermarket.utils;

import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemStack;

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
}
