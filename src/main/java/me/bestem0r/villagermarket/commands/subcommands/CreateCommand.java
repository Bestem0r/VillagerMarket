package me.bestem0r.villagermarket.commands.subcommands;

import me.bestem0r.villagermarket.VMPlugin;
import me.bestem0r.villagermarket.commands.CommandModule;
import me.bestem0r.villagermarket.commands.SubCommand;
import me.bestem0r.villagermarket.utilities.Methods;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class CreateCommand implements SubCommand {

    private final VMPlugin plugin;

    public CreateCommand(VMPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public List<String> getCompletion(int index, String[] args) {
        List<String> list = new ArrayList<>();
        switch (args.length) {
            case 2:
                if (args[1] == null || args[1].equalsIgnoreCase("")) {
                    list.add("player");
                    list.add("admin");
                } else if (args[1].charAt(0) == 'p') {
                    list.add("player");
                } else {
                    list.add("admin");
                }
                return list;
            case 3:
                list.add("infinite");
                list.add("1");
                list.add("2");
                list.add("3");
                list.add("4");
                list.add("5");
                list.add("6");
                return list;
            case 4:
                if (args[1].equals("player")) {
                    list.add("infinite");
                    list.add("1");
                    list.add("2");
                    list.add("3");
                    list.add("4");
                    list.add("5");
                    list.add("6");
                    return list;
                }
                return new ArrayList<>();
            case 6:
                if (args[1].equalsIgnoreCase("player")) {
                    list.add("infinite");
                    list.add("1d");
                    list.add("24h");
                    list.add("1m");
                    list.add("60s");
                    return list;
                }
                return new ArrayList<>();
            default:
                return new ArrayList<>();
        }
    }

    @Override
    public void run(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            return;
        }
        Player player = (Player) sender;
        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "Incorrect usage: Please specify shop type!");
            player.sendMessage(ChatColor.RED + "Use /vm help for command description");
            return;
        }
        if (!args[1].equals("player") && !args[1].equals("admin")) {
            player.sendMessage(ChatColor.RED + "Incorrect usage: Type must be Player or Admin!");
            player.sendMessage(ChatColor.RED + "/vm create <type> <shopsize> [storagesize] [price] [time]");
            return;
        }
        switch (args[1]) {
            case "player":
                if (args.length != 5 && args.length != 6) {
                    player.sendMessage(ChatColor.RED + "Incorrect number of arguments!");
                    player.sendMessage(ChatColor.RED + "/vm create player <shopsize> <storagesize> <price> [time]");
                    return;
                }
                if ((!canConvert(args[2]) && !args[2].equals("infinite")) || (!canConvert(args[3]) && !args[3].equals("infinite")) || !canConvert(args[4])) {
                    player.sendMessage(ChatColor.RED + "Incorrect usage!");
                    player.sendMessage(ChatColor.RED + "/vm create player <shopsize> <storagesize> <price> [time]");
                    return;
                }
                break;
            case "admin":
                if (args.length != 3) {
                    player.sendMessage(ChatColor.RED + "Incorrect number of arguments!");
                    player.sendMessage(ChatColor.RED + "/vm create admin <shopsize>");
                    return;
                }
                if (!canConvert(args[2]) && !args[2].equals("infinite")) {
                    player.sendMessage(ChatColor.RED + "Incorrect usage: Shopsize must be a number or infinite!");
                    player.sendMessage(ChatColor.RED + "/vm create admin <shopsize>");
                    return;
                }
        }

        String type = args[1];
        int shopSize = (args[2].equals("infinite") ? 0 : Integer.parseInt(args[2]));
        int storageSize = (args[1].equals("player") ? (args[3].equals("infinite") ? 0 : Integer.parseInt(args[3])) : 1);
        int cost = (args[1].equals("player") ? Integer.parseInt(args[4]) : 0);
        String duration = (args.length == 6 ? args[5] : "infinite");

        if (storageSize < 0 || storageSize > 6) {
            player.sendMessage(ChatColor.RED + "Incorrect usage: Storage size must be between 1 and 6, or infinite!");
            player.sendMessage(ChatColor.RED + "/vm create player <shopsize> [storagesize] [price] [time");
            return;
        }
        if (shopSize < 0 || shopSize > 6) {
            player.sendMessage(ChatColor.RED + "Incorrect usage: Shop size must be between 1 and 6, or infinite!");
            player.sendMessage(ChatColor.RED + "/vm create <type> <shopsize> [storagesize] [price] [time]");
            return;
        }
        if (cost < 0) {
            player.sendMessage(ChatColor.RED + "Incorrect usage: Cost can't be less than 0!");
            player.sendMessage(ChatColor.RED + "/vm create player <shopsize> [storagesize] [price] [time]");
            return;
        }
        Methods.spawnShop(plugin, player.getLocation(), type, storageSize, shopSize, cost, duration);
        player.playSound(player.getLocation(), Sound.valueOf(plugin.getConfig().getString("sounds.create_shop")), 1, 1);
    }

    private boolean canConvert(String string) {
        for (int i = 0; i < string.length(); i++) {
            char c = string.charAt(i);
            if (c < '0' || c > '9') {
                return false;
            }
        }
        return true;
    }

    @Override
    public void setModule(CommandModule module) {

    }

    @Override
    public String getDescription() {
        return "Create shop: &6/vm create <type> <shopsize> [storagesize] [price] [time]";
    }
}
