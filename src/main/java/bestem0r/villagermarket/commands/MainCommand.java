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
            if ((args.length == 4 || args.length == 5) && args[0].equalsIgnoreCase("create")) {
                if (!player.hasPermission("villagermarket.create")) {
                    player.sendMessage(ChatColor.RED + "You do not have permission for this command!");
                    return true;
                }
                if (!args[1].equalsIgnoreCase("player") && !args[1].equalsIgnoreCase("admin")) {
                    player.sendMessage(ChatColor.RED + "Incorrect usage: Type must be Player or Admin!");
                    player.sendMessage(ChatColor.RED + "Usage: /vm create <type> <shopsize> [storagesize] [price] [hours]");
                    return true;
                }
                if (args[1].equalsIgnoreCase("player") && args.length != 5) {
                    player.sendMessage(ChatColor.RED + "You need to specify a price for the villager!");
                    player.sendMessage(ChatColor.RED + "Usage: /vm create <type> <shopsize> [storagesize] [price] [hours]");
                    return true;
                }
                if ((!canConvert(args[2]) && !canConvert(args[3])) || ((args[1].equalsIgnoreCase("player") && !canConvert(args[2]) && !canConvert(args[3]) && !canConvert(args[4])))) {
                    player.sendMessage(ChatColor.RED + "Incorrect usage: Must be a number!");
                    player.sendMessage(ChatColor.RED + "Usage: /vm create <type> <shopsize> [storagesize] [price] [hours]");
                    return true;
                }

                String type = args[1];
                int shopfrontSize = Integer.parseInt(args[2]);
                int storageSize = Integer.parseInt(args[3]);
                int cost = (args.length == 4 ? Integer.parseInt(args[3]) : 0);

                if (storageSize < 1 || storageSize > 6) {
                    player.sendMessage(ChatColor.RED + "Incorrect usage: Storage size must be between 1 and 6!");
                    player.sendMessage(ChatColor.RED + "Usage: /vm create <type> <shopsize> [storagesize] [price] [hours]");
                    return true;
                }
                if (shopfrontSize < 1 || shopfrontSize > 6) {
                    player.sendMessage(ChatColor.RED + "Incorrect usage: Shop size must be between 1 and 6!");
                    player.sendMessage(ChatColor.RED + "Usage: /vm create <type> <shopsize> [storagesize] [price] [hours]");
                    return true;
                }
                if (cost < 0) {
                    player.sendMessage(ChatColor.RED + "Incorrect usage: Cost can't be less than 0!");
                    player.sendMessage(ChatColor.RED + "Usage: /vm create <type> <shopsize> [storagesize] [price] [hours]");
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
                    Config.newShopConfig(entityUUID, storageSize, shopfrontSize, cost, type, );
                    VMPlugin.getDataManager().getVillagerEntities().add(villager);
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
            } else if (args.length == 1 && args[0].equalsIgnoreCase("help")) {
                for (String text : help) {
                    player.sendMessage(ChatColor.translateAlternateColorCodes('&', text));
                }
            }
            else {
                player.sendMessage(ChatColor.RED + "Incorrect usage! Use /vm help!");
                return true;
            }
        }
        return true;
    }
    private String[] help = {
            "&a&lVillager Market's commands:",
            "",
            "&7Create shop: &a/vm create <type> <shopsize> [storagesize] [price] [hours]",
            "&7Remove shop: &a/vm remove",
            "Reload configs: &a/vm reload"
    };
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
