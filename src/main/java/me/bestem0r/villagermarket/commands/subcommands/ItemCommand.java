package me.bestem0r.villagermarket.commands.subcommands;

import me.bestem0r.villagermarket.VMPlugin;
import me.bestem0r.villagermarket.commands.CommandModule;
import me.bestem0r.villagermarket.commands.SubCommand;
import me.bestem0r.villagermarket.utilities.ColorBuilder;
import me.bestem0r.villagermarket.utilities.Methods;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

public class ItemCommand implements SubCommand {

    private CommandModule commandModule;
    private final VMPlugin plugin;

    public ItemCommand(VMPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public List<String> getCompletion(int index, String[] args) {
        List<String> list = new ArrayList<>();

        switch (args.length) {
            case 2:
                list.add("give");
                return list;
            case 3:
                return null;
            case 4: case 5:
                list.add("infinite");
                list.add("1");
                list.add("2");
                list.add("3");
                list.add("4");
                list.add("5");
                list.add("6");
                return list;
            default:
                return list;
        }
    }

    @Override
    public void run(CommandSender sender, String[] args) {
        if (args.length != 5 && args.length != 6) {
            commandModule.commandOutput(sender, ChatColor.RED + "Incorrect number of arguments!");
            commandModule.commandOutput(sender, ChatColor.RED + "/vm item give <player> <shopsize> <storagesize> [amount]");
            return;
        }
        Player target = Bukkit.getPlayer(args[2]);
        if (target == null) {
            commandModule.commandOutput(sender, ChatColor.RED + "Could not find player: " + args[2]);
            return;
        }
        if ((!args[3].equals("infinite") && !args[4].equals("infinite")) && (!canConvert(args[3]) || !canConvert(args[4]))) {
            commandModule.commandOutput(sender, ChatColor.RED + "Invalid size: " + args[3] + " or " + args[4]);
            return;
        }
        int amount = 1;
        int shopSize = (args[3].equals("infinite") ? 0 : Integer.parseInt(args[3]));
        int storageSize = (args[4].equals("infinite") ? 0 : Integer.parseInt(args[4]));

        if (storageSize < 0 || storageSize > 6 || shopSize < 0 || shopSize > 6) {
            commandModule.commandOutput(sender, ChatColor.RED + "Invalid shop/storage size!");
            return;
        }
        if (args.length == 6) {
            if (!canConvert(args[5])) {
                commandModule.commandOutput(sender, ChatColor.RED + "Invalid amount: " + args[5]);
                return;
            }
            amount = Integer.parseInt(args[5]);
        }
        target.getInventory().addItem(Methods.villagerShopItem(plugin, shopSize, storageSize, amount));
        target.playSound(target.getLocation(), Sound.ENTITY_ITEM_PICKUP, 1, 1);
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
        this.commandModule = module;
    }

    @Override
    public String getDescription() {
        return "Give item: &6/vm item give <player> <shopsize> <storagesize> [amount]";
    }
}
