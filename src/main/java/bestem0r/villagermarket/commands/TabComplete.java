package bestem0r.villagermarket.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class TabComplete implements TabCompleter {
    @Override
    public List<String> onTabComplete (CommandSender sender, Command cmd, String label, String[] args){
        if(sender instanceof Player) {
            List<String> list = new ArrayList<>();

            if ((args.length == 0 || args.length == 1)) {
                if (args[0].isEmpty()) {
                    list.add("reload");
                    list.add("create");
                    list.add("remove");
                    return list;
                }
                if (args[0].charAt(0) == 'c') {
                    list.add("create");
                } else if (args[0].charAt(0) == 'r' && args[0].length() < 3) {
                    list.add("reload");
                    list.add("remove");
                }else if (args[0].charAt(0) == 'r' && args[0].charAt(2) == 'l') {
                    list.add("reload");
                }else if (args[0].charAt(0) == 'r' && args[0].charAt(2) == 'm') {
                    list.add("remove");
                }
                return list;
            }

            if ((args.length == 2) && args[0].equalsIgnoreCase("create")) {
                if (args[1] == null || args[1].equalsIgnoreCase("")) {
                    list.add("player");
                    list.add("admin");
                } else if (args[1].charAt(0) == 'p') {
                    list.add("player");
                } else {
                    list.add("admin");
                }
                return list;
            }
            if ((args.length == 2 || args.length == 3) && args[0].equalsIgnoreCase("create")) {
                list.add("1");
                list.add("2");
                list.add("3");
                return list;
            }

        }
        return new ArrayList<>();
    }
}
