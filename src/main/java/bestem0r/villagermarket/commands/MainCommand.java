package bestem0r.villagermarket.commands;

import bestem0r.villagermarket.utilities.Color;
import bestem0r.villagermarket.utilities.Config;
import bestem0r.villagermarket.VMPlugin;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.*;

import java.util.UUID;

public class MainCommand implements org.bukkit.command.CommandExecutor {

    Villager villager;

    @Override
    public boolean onCommand(CommandSender sender, Command command, String s, String[] args) {
        if (sender instanceof Player) {


            Player player = (Player) sender;
            if ((args.length == 3 || args.length == 4) && args[0].equalsIgnoreCase("create")) {
                if (!player.hasPermission("villagermarket.create")) {
                    player.sendMessage(ChatColor.RED + "You do not have permission for this command!");
                    return true;
                }
                if (!args[1].equalsIgnoreCase("player") && !args[1].equalsIgnoreCase("admin")) {
                    player.sendMessage(ChatColor.RED + "Incorrect usage: Type must be Player or Admin!");
                    return true;
                }
                if (args[1].equalsIgnoreCase("player") && args.length != 4) {
                    player.sendMessage(ChatColor.RED + "You need to specify a price for the villager!");
                    return true;
                }
                if (!canConvert(args[2]) || (args[1].equalsIgnoreCase("player") && !canConvert(args[3]))) {
                    player.sendMessage(ChatColor.RED + "Incorrect usage: Arguments must be a number!");
                    return true;
                }

                String type = args[1];
                int size = Integer.parseInt(args[2]);
                int cost = (args.length == 4 ? Integer.parseInt(args[3]) : 0);

                if (size < 1 || size > 3) {
                    player.sendMessage(ChatColor.RED + "Incorrect usage: Size must be between 1 and 3!");
                    return true;
                }
                if (cost < 0) {
                    player.sendMessage(ChatColor.RED + "Incorrect usage: Cost can't be less than 0!");
                    return true;
                }

                World world = player.getWorld();
                villager = (Villager) world.spawnEntity(player.getLocation(), EntityType.VILLAGER);

                villager.setAI(false);
                villager.setInvulnerable(true);
                villager.setCollidable(false);
                villager.setSilent(true);
                villager.setProfession(Villager.Profession.valueOf(VMPlugin.getInstance().getConfig().getString("villager.default_profession")));
                String namePath = (type.equalsIgnoreCase("player") ? "name_available" : "name_admin");
                villager.setCustomName(new Color.Builder().path("villager." + namePath).build());

                String entityUUID = villager.getUniqueId().toString();
                if (Bukkit.getEntity(UUID.fromString(entityUUID)) != null) {
                    Config.newShopConfig(entityUUID, villager, size, cost, type);
                } else {
                    player.sendMessage(VMPlugin.getPrefix() + ChatColor.RED + "Unable to spawn Villager! Does WorldGuard deny mobspawn?");
                }
            } else if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
                if (!player.hasPermission("villagermarket.reload")) {
                    player.sendMessage(ChatColor.RED + "You do not have permission for this command!");
                    return true;
                }
                VMPlugin.getInstance().reloadConfig();
                VMPlugin.loadDefaultValues();
                player.sendMessage(new Color.Builder().path("messages.reloaded").addPrefix().build());
                for (String id : VMPlugin.getDataManager().getVillagers().keySet()) {
                    VMPlugin.getDataManager().getVillagers().get(id).reload();
                }
            } else if (args.length == 1 && args[0].equalsIgnoreCase("remove")) {
                if (!player.hasPermission("villagermarket.remove")) {
                    player.sendMessage(ChatColor.RED + "You do not have permission for this command!");
                    return true;
                }
                player.sendMessage(new Color.Builder().path("messages.remove_villager").addPrefix().build());
                VMPlugin.getDataManager().getRemoveVillager().add(player);
            }
            else {
                player.sendMessage(ChatColor.RED + "Incorrect usage: /vm <create/reload/remove>");
                return true;
            }
        }
        return true;
    }
    private Boolean canConvert(String string) {
        for (int i = 0; i < string.length(); i++) {
            char c = string.charAt(i);
            if (c < '0' || c > '9') {
                return false;
            }
        }
        return true;
    }

}
