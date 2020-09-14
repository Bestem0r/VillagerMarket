package bestem0r.villagermarket.utilities;

import bestem0r.villagermarket.VMPlugin;
import org.bukkit.ChatColor;

public class ColorBuilder {

    public static String color(String path) {
        if (VMPlugin.getInstance().getConfig().getString(path) == null) {
            return path;
        }
        return ChatColor.translateAlternateColorCodes('&', VMPlugin.getInstance().getConfig().getString(path));
    }
}
