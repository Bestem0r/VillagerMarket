package me.bestem0r.villagermarket.utilities;

import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class ColorBuilder {

    private String path;
    private final HashMap<String, String> replaceList = new HashMap<>();
    private boolean addPredix = false;

    private final FileConfiguration config;

    public ColorBuilder(JavaPlugin plugin) {
        this.config = plugin.getConfig();
    }

    public ColorBuilder path(String path) {
        this.path = path;
        return this;
    }
    public ColorBuilder replace(String replace, String value) {
        replaceList.put(replace, value);
        return this;
    }
    public ColorBuilder replaceWithCurrency(String replace, String value) {
        String currency = config.getString("currency");
        String valueCurrency = (config.getBoolean("currency_before") ? currency + value : value + currency);
        replaceList.put(replace, valueCurrency);
        return this;
    }
    public ColorBuilder addPrefix() {
        this.addPredix = true;
        return this;
    }

    public String build() {
        String text = config.getString(path);
        if (text == null) return path;
        for (String replace : replaceList.keySet()) {
            text = text.replace(replace, replaceList.get(replace));
        }
        if (addPredix) {
            String prefix = ChatColor.translateAlternateColorCodes('&', config.getString("plugin_prefix"));
            return prefix + " " + ChatColor.translateAlternateColorCodes('&', text);
        } else {
            return ChatColor.translateAlternateColorCodes('&', text);
        }
    }

    public ArrayList<String> buildLore() {
        List<String> loreList = config.getStringList(path);
        ArrayList<String> returnLore = new ArrayList<>();
        for (String lore : loreList) {
            for (String replace : replaceList.keySet()) {
                lore = lore.replace(replace, replaceList.get(replace));
            }
            returnLore.add(ChatColor.translateAlternateColorCodes('&', lore));
        }
        return returnLore;
    }
}
