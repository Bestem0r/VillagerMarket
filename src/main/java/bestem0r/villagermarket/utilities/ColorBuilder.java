package bestem0r.villagermarket.utilities;

import bestem0r.villagermarket.VMPlugin;
import org.bukkit.ChatColor;

import java.util.ArrayList;

public class ColorBuilder {

    public static String color(String path) {
        if (VMPlugin.getInstance().getConfig().getString(path) == null) {
            return path;
        }
        return ChatColor.translateAlternateColorCodes('&', VMPlugin.getInstance().getConfig().getString(path));
    }
    public static ArrayList<String> lore(String lorePath) {
        ArrayList<String> lore = (ArrayList<String>) VMPlugin.getInstance().getConfig().getList(lorePath);
        for (int i = 0; i < lore.size(); i++) {
            lore.set(i, ChatColor.translateAlternateColorCodes('&', lore.get(i)));
        }
        return lore;
    }
    public static ArrayList<String> loreReplace(String lorePath, String replace, String value) {

        ArrayList<String> lore = (ArrayList<String>) VMPlugin.getInstance().getConfig().getList(lorePath);
        ArrayList<String> loreReturn = new ArrayList<>();
        for (int i = 0; i < lore.size(); i++) {
            String raw = lore.get(i).replace(replace, value);
            loreReturn.add(ChatColor.translateAlternateColorCodes('&', raw));
        }
        return loreReturn;
    }public static ArrayList<String> loreReplaceTwo(String lorePath, String replace, String value, String replace_2, String value_2) {

        ArrayList<String> lore = (ArrayList<String>) VMPlugin.getInstance().getConfig().getList(lorePath);
        ArrayList<String> loreReturn = new ArrayList<>();
        for (int i = 0; i < lore.size(); i++) {
            String raw = lore.get(i).replace(replace, value).replace(replace_2, value_2);
            loreReturn.add(ChatColor.translateAlternateColorCodes('&', raw));
        }
        return loreReturn;
    }
    public static ArrayList<String> loreReplaceThree(String lorePath, String replace, String value, String replace_2, String value_2, String replace_3, String value_3) {

        ArrayList<String> lore = (ArrayList<String>) VMPlugin.getInstance().getConfig().getList(lorePath);
        ArrayList<String> loreReturn = new ArrayList<>();
        for (int i = 0; i < lore.size(); i++) {
            String raw = lore.get(i).replace(replace, value).replace(replace_2, value_2).replace(replace_3, value_3);
            loreReturn.add(ChatColor.translateAlternateColorCodes('&', raw));
        }
        return loreReturn;
    }
    public static String colorReplace(String path, String replace, String value) {
        String raw = VMPlugin.getInstance().getConfig().getString(path);
        return ChatColor.translateAlternateColorCodes('&', raw.replace(replace, value));
    }
    public static String replaceBought(String playerName, String materialName, String price, String amount) {
        String raw = VMPlugin.getInstance().getConfig().getString("messages.someone_bought");
        materialName = materialName.toLowerCase().replace("_", " ");
        assert raw != null;
        String prefix = VMPlugin.getPrefix();
        String colored = ChatColor.translateAlternateColorCodes('&', raw);
        return prefix + colored.replace("%player%", playerName).replace("%material%", materialName).replace("%price%", price).replace("%amount%", amount);
    }
}
