package bestem0r.villagermarket.utilities;

import bestem0r.villagermarket.VMPlugin;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public abstract class Color {

    public static class Builder {

        private String path;
        private HashMap<String, String> replaceList = new HashMap<>();
        private boolean addPredix = false;

        private final FileConfiguration mainConfig = VMPlugin.getInstance().getConfig();

        public Builder path(String path) {
            this.path = path;
            return this;
        }
        public Builder replace(String replace, String value) {
            replaceList.put(replace, value);
            return this;
        }
        public Builder addPrefix() {
            this.addPredix = true;
            return this;
        }

        public String build() {
            String text = mainConfig.getString(path);
            if (text == null) return path;
            for (String replace : replaceList.keySet()) {
                text = text.replaceAll(replace, replaceList.get(replace));
            }
            if (addPredix) {
                String prefix = ChatColor.translateAlternateColorCodes('&', mainConfig.getString("prefix"));
                return prefix + " " + ChatColor.translateAlternateColorCodes('&', text);
            } else {
                return ChatColor.translateAlternateColorCodes('&', text);
            }
        }

        public ArrayList<String> buildLore() {
            List<String> loreList = mainConfig.getStringList(path);
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
}
