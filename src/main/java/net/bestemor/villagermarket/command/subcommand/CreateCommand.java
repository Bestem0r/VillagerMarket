package net.bestemor.villagermarket.command.subcommand;

import net.bestemor.core.command.ISubCommand;
import net.bestemor.core.config.ConfigManager;
import net.bestemor.villagermarket.VMPlugin;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class CreateCommand implements ISubCommand {

    private final VMPlugin plugin;
    private final List<String> sizes = Arrays.asList("infinite", "1", "2", "3", "4", "5", "6");

    public CreateCommand(VMPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public List<String> getCompletion(String[] args) {
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
                return sizes;
            case 4:
                return args[1].equals("player") ? sizes : new ArrayList<>();
            case 6:
                if (args[1].equalsIgnoreCase("player")) {
                    list.add("infinite");
                    list.add("1d");
                    list.add("24h");
                    list.add("1m");
                    list.add("60s");
                    return list;
                }
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
        Entity entity = plugin.getShopManager().spawnShop(player.getLocation(), type);
        player.sendMessage(ConfigManager.getMessage("messages.id").replace("%id%", entity.getUniqueId().toString()));
        if (Bukkit.getEntity(entity.getUniqueId()) != null) {
            plugin.getShopManager().createShopConfig(entity.getUniqueId(), storageSize, shopSize, cost, type.toUpperCase(Locale.ROOT), duration);
        } else {
            Bukkit.getLogger().severe(ChatColor.RED + "Unable to spawn Villager! Does WorldGuard deny mobs pawn?");
        }
        player.playSound(player.getLocation(), ConfigManager.getSound("sounds.create_shop"), 1, 1);
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
    public String getDescription() {
        return "Create new shop";
    }

    @Override
    public String getUsage() {
        return "<type> <shopsize> [storagesize] [price] [time]";
    }

    @Override
    public boolean requirePermission() {
        return true;
    }
}
