package bestem0r.villagermarket.commands;

import bestem0r.villagermarket.VMPlugin;
import bestem0r.villagermarket.events.interact.Move;
import bestem0r.villagermarket.events.interact.Remove;
import bestem0r.villagermarket.events.interact.Stats;
import bestem0r.villagermarket.events.interact.Trusted;
import bestem0r.villagermarket.items.MenuItem;
import bestem0r.villagermarket.shops.VillagerShop;
import bestem0r.villagermarket.utilities.Color;
import bestem0r.villagermarket.utilities.Methods;
import org.bukkit.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class VMExecutor implements org.bukkit.command.CommandExecutor {

    private final String[] help = {
            "&a&lVillager Market's commands:",
            "> Create shop: &6/vm create <type> <shopsize> [storagesize] [price] [hours]",
            "> Remove shop: &6/vm remove",
            "> Move shop: &6/vm move",
            "> Give item: &6/vm item give <player> <shopsize> <storagesize> [amount]",
            "> Add trusted: &6/vm trusted add <player>",
            "> Remove trusted: &6/vm trusted remove <player>",
            "> Search for nearby shops: &6/vm search <radius>",
            "> Show Shop statistics: &6/vm stats",
            "> Reload configs: &6/vm reload",
    };

    @Override
    public boolean onCommand(CommandSender sender, Command command, String s, String[] args) {
        if (sender instanceof Player) {

            if (args.length == 0) {
                return false;
            }
            Player player = (Player) sender;
            //Create
            if ((args.length == 3 || args.length == 5 || args.length == 6) && args[0].equalsIgnoreCase("create")) {
                if (!player.hasPermission("villagermarket.create")) {
                    player.sendMessage(ChatColor.RED + "You do not have permission for this command!");
                    return true;
                }
                if (!args[1].equalsIgnoreCase("player") && !args[1].equalsIgnoreCase("admin")) {
                    player.sendMessage(ChatColor.RED + "Incorrect usage: Type must be Player or Admin!");
                    player.sendMessage(ChatColor.RED + "Usage: /vm create <type> <shopsize> [storagesize] [price] [time]");
                    return true;
                }
                if ((!canConvert(args[2]) && !canConvert(args[3])) || ((args[1].equalsIgnoreCase("player") && !canConvert(args[2]) && !canConvert(args[3]) && !canConvert(args[4])))) {
                    player.sendMessage(ChatColor.RED + "Incorrect usage: Must be a number!");
                    player.sendMessage(ChatColor.RED + "Usage: /vm create <type> <shopsize> [storagesize] [price] [time]");
                    return true;
                }

                String type = args[1];
                int shopSize = Integer.parseInt(args[2]);
                int storageSize = (args.length == 6 || args.length == 5 ? Integer.parseInt(args[3]) : 1);
                int cost = (args.length == 6 || args.length == 5 ? Integer.parseInt(args[4]) : 0);
                String duration = (args.length == 6 ? args[5] : "infinite");

                if (storageSize < 1 || storageSize > 6) {
                    player.sendMessage(ChatColor.RED + "Incorrect usage: Storage size must be between 1 and 6!");
                    player.sendMessage(ChatColor.RED + "Usage: /vm create <type> <shopsize> [storagesize] [price] [time");
                    return true;
                }
                if (shopSize < 1 || shopSize > 6) {
                    player.sendMessage(ChatColor.RED + "Incorrect usage: Shop size must be between 1 and 6!");
                    player.sendMessage(ChatColor.RED + "Usage: /vm create <type> <shopsize> [storagesize] [price] [time]");
                    return true;
                }
                if (cost < 0) {
                    player.sendMessage(ChatColor.RED + "Incorrect usage: Cost can't be less than 0!");
                    player.sendMessage(ChatColor.RED + "Usage: /vm create <type> <shopsize> [storagesize] [price] [time]");
                    return true;
                }
                Methods.spawnShop(player.getLocation(), type, storageSize, shopSize, cost, duration);
                player.playSound(player.getLocation(), Sound.valueOf(VMPlugin.getInstance().getConfig().getString("sounds.create_shop")), 1, 1);
            //Item
            } else if ((args.length == 5) || (args.length == 6) && args[0].equalsIgnoreCase("item")) {
                if (!args[1].equalsIgnoreCase("give")) return false;
                if (!player.hasPermission("villagermarket.item.give")) {
                    player.sendMessage(ChatColor.RED + "You do not have permissions for this command!");
                    return true;
                }
                if (Bukkit.getPlayer(args[2]) == null) {
                    player.sendMessage(ChatColor.RED + "Could not find player: " + args[2]);
                    return true;
                }
                if (!canConvert(args[3]) || !canConvert(args[4])) {
                    player.sendMessage(ChatColor.RED + "Invalid size: " + args[3] + " or " + args[4]);
                    return false;
                }
                int amount = 1;
                int shopSize = Integer.parseInt(args[3]);
                int storageSize = Integer.parseInt(args[4]);
                Player target = Bukkit.getPlayer(args[2]);

                if (storageSize < 1 || storageSize > 6 || shopSize < 1 || shopSize > 6) {
                    player.sendMessage(ChatColor.RED + "Invalid shop/storage size!");
                    return false;
                }
                if (args.length == 6) {
                    if (!canConvert(args[5])) {
                        player.sendMessage(ChatColor.RED + "Invalid amount: " + args[5]);
                        return true;
                    }
                    amount = Integer.parseInt(args[5]);
                }
                target.getInventory().addItem(villagerShopItem(shopSize, storageSize, amount));
                target.playSound(target.getLocation(), Sound.ENTITY_ITEM_PICKUP, 1, 1);
            //Reload
            } else if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
                if (!player.hasPermission("villagermarket.reload")) {
                    player.sendMessage(ChatColor.RED + "You do not have permission for this command!");
                    return true;
                }
                VMPlugin.getInstance().reloadConfig();
                VMPlugin.getInstance().saveLog();
                player.sendMessage(new Color.Builder().path("messages.reloaded").addPrefix().build());
                for (VillagerShop villagerShop : VMPlugin.shops) {
                    villagerShop.reload();
                }
            //Remove
            } else if (args.length == 1 && args[0].equalsIgnoreCase("remove")) {
                if (!player.hasPermission("villagermarket.remove")) {
                    player.sendMessage(ChatColor.RED + "You do not have permission for this command!");
                    return true;
                }
                player.sendMessage(new Color.Builder().path("messages.remove_villager").addPrefix().build());
                Bukkit.getPluginManager().registerEvents(new Remove(player), VMPlugin.getInstance());
            //Help
            } else if (args.length == 1 && args[0].equalsIgnoreCase("help")) {
                for (String text : help) {
                    player.sendMessage(ChatColor.translateAlternateColorCodes('&', text));
                }
            //Move
            } else if (args.length == 1 && args[0].equalsIgnoreCase("move")) {
                if (!player.hasPermission("villagermarket.move")) {
                    player.sendMessage(ChatColor.RED + "You do not have permission for this command!");
                    return true;
                }
                player.sendMessage(new Color.Builder().path("messages.move_villager").addPrefix().build());
                Bukkit.getPluginManager().registerEvents(new Move(player), VMPlugin.getInstance());
            //Search
            } else if (args[0].equalsIgnoreCase("search")) {
                if (!player.hasPermission("villagermarket.search")) {
                    player.sendMessage("You do not have permissions for this command!");
                    return true;
                }
                if (args.length != 2) {
                    player.sendMessage(ChatColor.RED + "Incorrect usage! Specify radius!");
                    player.sendMessage(ChatColor.RED + "Usage: /vm search <radius>");
                    return true;
                }
                if (!canConvert(args[1])) {
                    player.sendMessage(ChatColor.RED + "Incorrect usage! Radius must be a number!");
                    player.sendMessage(ChatColor.RED + "Usage: /vm search <radius>");
                    return true;
                }
                double radius = Double.parseDouble(args[1]);
                if (radius > 10000) {
                    player.sendMessage(ChatColor.RED + "Radius can't be more than 10 000 blocks!");
                    return true;
                }
                int result = 0;
                List<String> shopInfo = new ArrayList<>();
                for (Entity entity : player.getNearbyEntities(radius, radius, radius)) {
                    if (Methods.shopFromUUID(entity.getUniqueId()) != null) {
                        Location location = entity.getLocation();
                        shopInfo.add(new Color.Builder().path("messages.search_shop_info")
                                .replace("%name%", entity.getCustomName())
                                .replace("%location%", location.getBlockX() + " " + location.getBlockY() + " " + location.getBlockZ())
                                .build());
                        result++;
                    }
                }
                player.sendMessage(new Color.Builder().path("messages.search_result").replace("%amount%", String.valueOf(result)).addPrefix().build());
                shopInfo.forEach(player::sendMessage);
            }
            //Trusted
            else if (args[0].equalsIgnoreCase("trusted") && args.length == 3) {
                if (!args[1].equals("add") && !args[1].equals("remove")) {
                    player.sendMessage(ChatColor.RED + "Unknown subcommand: " + args[1]);
                    player.sendMessage(ChatColor.RED + "Usage: /vm trusted add/remove <player>");
                    return false;
                }
                Player target = Bukkit.getPlayer(args[2]);
                if (target == null) {
                    player.sendMessage(ChatColor.RED + "Could not find player: " + args[2]);
                    return true;
                }
                switch (args[1]) {
                    case "add":
                        player.sendMessage(new Color.Builder().path("messages.add_trusted").addPrefix().build());
                        Bukkit.getPluginManager().registerEvents(new Trusted(player, target, Trusted.Action.ADD), VMPlugin.getInstance());
                        break;
                    case "remove":
                        player.sendMessage(new Color.Builder().path("messages.remove_trusted").addPrefix().build());
                        Bukkit.getPluginManager().registerEvents(new Trusted(player, target, Trusted.Action.REMOVE), VMPlugin.getInstance());
                }
            }
            //Stats
            else if (args[0].equalsIgnoreCase("stats")) {
                player.sendMessage(new Color.Builder().path("messages.get_stats").addPrefix().build());
                Bukkit.getPluginManager().registerEvents(new Stats(player), VMPlugin.getInstance());
            }
            else {
                player.sendMessage(ChatColor.RED + "Incorrect usage! Use /vm help!");
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

    /** Returns new Villager Shop Item */
    private ItemStack villagerShopItem(int shopSize, int storageSize, int amount) {
        FileConfiguration mainConfig = VMPlugin.getInstance().getConfig();

        amount = (Math.min(amount, 64));
        String storageString = String.valueOf(storageSize);
        String shopString = String.valueOf(shopSize);

        ArrayList<String> lore = new Color.Builder()
                .path("shop_item.lore")
                .replace("%shop_size%", shopString)
                .replace("%storage_size%", storageString)
                .buildLore();

        return new MenuItem.Builder(Material.valueOf(mainConfig.getString("shop_item.material")))
                .amount(amount)
                .nameFromPath("shop_item.name")
                .lore(lore)
                .persistentData(new NamespacedKey(VMPlugin.getInstance(), "vm-item"), shopString + "-" + storageString)
                .build();
    }
}
