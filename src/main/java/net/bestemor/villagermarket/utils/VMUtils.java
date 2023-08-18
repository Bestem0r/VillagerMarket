package net.bestemor.villagermarket.utils;

import net.bestemor.core.config.ConfigManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Entity;
import org.bukkit.inventory.ItemStack;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
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

    /** Properly checks if two ItemStacks are equal.
     * @param item1 An ItemStack.
     * @param item2 An ItemStack.
     * @return Whether the two provided ItemStacks are equal or not. */
    public static boolean compareItems(ItemStack item1, ItemStack item2) {
        if (item1 == null || item2 == null) { return false; }
        if (item1.getType() != item2.getType()) { return false; }
        if (item1.hasItemMeta() != item2.hasItemMeta()) { return false; }
        ItemStack item1clone = item1.clone();
        ItemStack item2clone = item2.clone();

        item1clone.setAmount(1);
        item2clone.setAmount(1);

        return (item1clone.toString() + item1clone.getDurability()).equals((item2clone.toString() + item2clone.getDurability()));
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
        return Bukkit.getEntity(uuid);
    }

    public static Instant getTimeFromNow(String time) {
        Instant i = Instant.now().truncatedTo(ChronoUnit.MINUTES);
        if (time == null) {
            return Instant.ofEpochSecond(0);
        }
        String s = time.substring(0, time.length() - 1);
        if (!VMUtils.isInteger(s)) {
            return Instant.ofEpochSecond(0);
        }

        int amount = Integer.parseInt(s);
        switch (time.substring(time.length() - 1)) {
            case "m":
                i = i.plus(amount, ChronoUnit.MINUTES);
                break;
            case "h":
                i = i.plus(amount, ChronoUnit.HOURS).truncatedTo(ChronoUnit.MINUTES);
                break;
            case "d":
                i = i.plus(amount, ChronoUnit.DAYS).truncatedTo(ChronoUnit.HOURS);
                break;
            default:
        }
        return i;
    }

    public static ChatColor getCodeBeforePlaceholder(List<String> l, String placeholder) {
        for (String s : l) {
            if (s.contains(placeholder)) {
                return getCodeBeforePlaceholder(s, placeholder);
            }
        }
        return ChatColor.WHITE;
    }

    public static ChatColor getCodeBeforePlaceholder(String s, String placeholder) {
        int index = s.indexOf(placeholder);
        if (index == -1) {
            return ChatColor.WHITE;
        }
        if (index == 0) {
            return ChatColor.WHITE;
        }
        String code = s.substring(index - 2, index);
        if (code.startsWith("&") || code.startsWith("§")) {
            return ChatColor.getByChar(code.charAt(1));
        }
        return ChatColor.WHITE;
    }

    public static String formatBuySellPrice(BigDecimal buy, BigDecimal sell) {
        String price;
        String currency = ConfigManager.getString("currency");
        String buyPrice = buy.stripTrailingZeros().setScale(2, RoundingMode.HALF_UP).toPlainString();
        String sellPrice = sell.stripTrailingZeros().setScale(2, RoundingMode.HALF_UP).toPlainString();
        if (ConfigManager.getBoolean("currency_before")) {
            price = currency + buyPrice + " / " + currency + sellPrice;
        } else {
            price = buyPrice + currency + " / " + sellPrice + currency;
        }
        return price;
    }
}
