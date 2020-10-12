package bestem0r.villagermarket.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class VMCompleter implements TabCompleter {
    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args){
        if(sender instanceof Player) {
            List<String> list = new ArrayList<>();

            if ((args.length == 0 || args.length == 1)) {
                if (args[0].isEmpty()) {
                    list.add("help");
                    list.add("move");
                    list.add("reload");
                    list.add("create");
                    list.add("remove");
                    list.add("search");
                    list.add("stats");
                    return list;
                }
                if (args[0].charAt(0) == 'c') {
                    list.add("create");
                } else if (args[0].charAt(0) == 'h') {
                    list.add("help");
                } else if (args[0].charAt(0) == 'm') {
                    list.add("move");
                } else if (args[0].charAt(0) == 'r' && args[0].length() < 3) {
                    list.add("reload");
                    list.add("remove");
                } else if (args[0].charAt(0) == 'r' && args[0].charAt(2) == 'l') {
                    list.add("reload");
                } else if (args[0].charAt(0) == 'r' && args[0].charAt(2) == 'm') {
                    list.add("remove");
                } else if (args[0].charAt(0) == 's' && args[0].length() < 2) {
                    list.add("search");
                    list.add("stats");
                } else if (args[0].charAt(0) == 's' && args[0].charAt(1) == 'e') {
                    list.add("search");
                } else if (args[0].charAt(0) == 's' && args[0].charAt(1) == 't') {
                    list.add("stats");
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
            if ((args.length == 3) && args[0].equalsIgnoreCase("create") || (args.length == 4) && args[1].equalsIgnoreCase("player")){
                list.add("1");
                list.add("2");
                list.add("3");
                list.add("4");
                list.add("5");
                list.add("6");
                return list;
            }
            if (args.length == 6 && args[1].equalsIgnoreCase("player") && args[5].isEmpty()) {
                list.add("infinite");
                list.add("1d");
                list.add("24h");
                list.add("1m");
                list.add("60s");
                return list;
            }

        }
        return new ArrayList<>();
    }
}
