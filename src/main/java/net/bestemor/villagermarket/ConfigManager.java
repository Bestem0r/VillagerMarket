package net.bestemor.villagermarket;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ConfigManager {

    private ConfigManager() {}

    private static FileConfiguration config;

    private static String prefixPath = "prefix";
    private static String currencyPath = "currency";
    private static String isBeforePath = "currency_before";

    private final static Map<String, Object> cache = new HashMap<>();
    private final static Map<String, List<String>> listCache = new HashMap<>();

    public static void setConfig(FileConfiguration config) {
        ConfigManager.config = config;
    }

    public static void clearCache() {
        cache.clear();
        listCache.clear();
    }

    public static void setPrefixPath(String path) {
        prefixPath = path;
    }
    public static void setCurrencyPath(String currencyPath) {
        ConfigManager.currencyPath = currencyPath;
    }
    public static void setIsBeforePath(String isBeforePath) {
        ConfigManager.isBeforePath = isBeforePath;
    }

    public static String getString(String path) {
        String s = get(path, String.class);
        return s == null ? path : ChatColor.translateAlternateColorCodes('&', s);
    }

    public static String getMessage(String path) {
        return prefixPath == null ? "" : (getString(prefixPath) + " ") + getString(path);
    }

    public static Sound getSound(String path) {
        return Sound.valueOf(getString(path));
    }

    public static boolean getBoolean(String path) {
        return get(path, Boolean.class);
    }

    public static double getDouble(String path) {
        return config.getDouble(path);
    }

    public static int getInt(String path) {
        return get(path, Integer.class);
    }

    public static long getLong(String path) {
        return get(path, Integer.class);
    }

    public static CurrencyBuilder getCurrencyBuilder(String path) {
        return new CurrencyBuilder(getString(path));
    }

    public static List<String> getStringList(String path) {
        checkConfig();
        if (listCache.containsKey(path)) {
            return listCache.get(path);
        }
        listCache.put(path, config.getStringList(path));
        return getStringList(path);
    }

    public static ListBuilder getListBuilder(String path) {
        checkConfig();
        return new ListBuilder(path);
    }

    public static ItemBuilder getItem(String path) {
        checkConfig();
        return new ItemBuilder(path);
    }

    public static String getTimeLeft(Instant time) {
        if (time == null || time.getEpochSecond() == 0) {
            return getUnit("never", false);
        }
        Instant difference = time.minusSeconds(Instant.now().getEpochSecond());
        StringBuilder builder = new StringBuilder();
        int days = (int) Math.floor(difference.getEpochSecond() / 86400d);
        int hours = (int) Math.floor((difference.getEpochSecond() - (days * 86400d)) / 3600d);
        int minutes = (int) Math.ceil((difference.getEpochSecond() - (days * 86400d) - (hours * 3600d)) / 60);
        if (days > 0) {
            builder.append(days).append(" ").append(getUnit("d", days > 1)).append(" ");
        }
        if (hours > 0) {
            builder.append(hours).append(" ").append(getUnit("h", hours > 1)).append(" ");
        }
        if (minutes > 0) {
            builder.append(minutes).append(" ").append(getUnit("m", minutes > 1));
        }
        if (hours < 1 && minutes < 1 && days < 1)  {
            builder.append(getString("time.less_than_a_minute"));
        }
        return builder.toString();
    }

    public static String getUnit(String u, boolean plural) {
        if (u.equals("infinite")) {
            return getString("time.indefinitely");
        }
        switch (u) {
            case "s":
                return getString("time.second" + (plural ? "s" : ""));
            case "m":
                return getString("time.minute" + (plural ? "s" : ""));
            case "h":
                 return getString("time.hour" + (plural ? "s" : ""));
            case "d":
                return getString("time.day" + (plural ? "s" : ""));
            case "never":
                return getString("time.never");
            default:
                return "invalid unit";
        }
    }

    private static <T> T get(String path, Class<T> clazz) {
        checkConfig();
        if (cache.containsKey(path) && clazz.isInstance(cache.get(path))) {
            return clazz.cast(cache.get(path));
        }
        T t = config.getObject(path, clazz);
        cache.put(path, t);
        return t;
    }

    private static void checkConfig() {
        if (config == null) {
            throw new IllegalStateException("No FileConfiguration is loaded");
        }
    }

    public static class CurrencyBuilder {

        private String s;
        boolean addPrefix = false;
        private final Map<String, String> replacements = new HashMap<>();
        private Map<String, BigDecimal> currencyReplacements = new HashMap<>();

        public CurrencyBuilder(String s) {
            this.s = s;
        }

        public String build() {
            boolean isBefore = getBoolean(isBeforePath);
            String currency = getString(currencyPath);

            for (String sOld : replacements.keySet()) {
                s = s.replace(sOld, replacements.get(sOld));
            }
            for (String sOld : currencyReplacements.keySet()) {
                String amount = new BigDecimal(currencyReplacements.get(sOld).toString()).setScale(2, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString();
                s = s.replace(sOld, isBefore ? (currency + amount) : (amount + currency));
            }
            if (addPrefix) {
                s = getString(prefixPath) + " " + s;
            }
            return s;
        }

        public CurrencyBuilder replace(String sOld, String sNew) {
            replacements.put(sOld, sNew);
            return this;
        }

        public CurrencyBuilder replaceCurrency(String replace, BigDecimal b) {
            currencyReplacements.put(replace, b);
            return this;
        }

        public CurrencyBuilder addPrefix() {
            addPrefix = true;
            return this;
        }
    }

    public static class ItemBuilder {

        private final String path;

        private final Map<String, BigDecimal> currencyReplacements = new HashMap<>();
        private final Map<String, String> replacements = new HashMap<>();

        private ItemBuilder(String path) {
            this.path = path;
        }

        public ItemBuilder replace(String sOld, String sNew) {
            replacements.put(sOld, sNew);
            return this;
        }

        public ItemBuilder replaceCurrency(String sOld, BigDecimal b) {
            currencyReplacements.put(sOld, b);
            return this;
        }

        public ItemStack build() {
            ItemStack item = new ItemStack(Material.valueOf(getString(path + ".material")));

            String name = getString(path + ".name");
            ListBuilder b = new ListBuilder(path + ".lore");
            b.currencyReplacements = currencyReplacements;
            b.replacements = replacements;

            List<String> lore = b.build();

            ItemMeta meta = item.getItemMeta();

            if (!currencyReplacements.isEmpty()) {
                CurrencyBuilder c = new CurrencyBuilder(name);
                c.currencyReplacements = currencyReplacements;
                name = c.build();
            }
            for (String sOld : replacements.keySet()) {
                name = name.replace(sOld, replacements.get(sOld));
            }

            if (meta != null) {
                meta.setDisplayName(name);
                meta.setLore(lore);
            }

            item.setItemMeta(meta);
            return item;
        }
    }

    public static class ListBuilder {

        private final String path;
        private Map<String, String> replacements = new HashMap<>();
        private Map<String, BigDecimal> currencyReplacements = new HashMap<>();

        private ListBuilder(String path) {
            this.path = path;
        }

        public ListBuilder replace(String sOld, String sNew) {
            replacements.put(sOld, sNew);
            return this;
        }

        public ListBuilder replaceCurrency(String s, BigDecimal amount) {
            currencyReplacements.put(s, amount);
            return this;
        }

        public List<String> build() {

            List<String> original = getStringList(path);
            List<String> result = new ArrayList<>();

            for (String line : original) {
                for (String sOld : replacements.keySet()) {
                    line = line.replace(sOld, replacements.get(sOld));
                }
                if (!currencyReplacements.isEmpty()) {
                    CurrencyBuilder b = new CurrencyBuilder(line);
                    b.currencyReplacements = currencyReplacements;
                    line = b.build();
                }
                result.add(ChatColor.translateAlternateColorCodes('&', line));
            }
            return result;
        }
    }
}
