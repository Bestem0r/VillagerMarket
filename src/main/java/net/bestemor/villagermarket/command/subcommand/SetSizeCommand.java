package net.bestemor.villagermarket.command.subcommand;

import net.bestemor.core.command.ISubCommand;
import net.bestemor.core.config.ConfigManager;
import net.bestemor.villagermarket.VMPlugin;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SetSizeCommand implements ISubCommand {

    private final VMPlugin plugin;
    private final List<String> sizes = Arrays.asList("infinite", "1", "2", "3", "4", "5", "6");

    public SetSizeCommand(VMPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public List<String> getCompletion(String[] args) {
        switch (args.length) {
            case 2:
                return Arrays.asList("storage", "shopfront");
            case 3:
                return sizes;
        }
        return new ArrayList<>();
    }

    @Override
    public void run(CommandSender sender, String[] args) {
        if (sender instanceof Player) {
            Player player = (Player) sender;

            if (args.length != 3) {
                player.sendMessage(ChatColor.RED + "Incorrect usage: Please specify storage or shopfront and a size.");
                return;
            }
            if (!args[1].equals("storage") && !args[1].equals("shopfront")) {
                player.sendMessage(ChatColor.RED + "Incorrect usage: Please specify storage or shopfront.");
                return;
            }
            if (!sizes.contains(args[2])) {
                player.sendMessage(ChatColor.RED + "Incorrect usage: Please specify a correct size.");
                return;
            }

            SetAction action = new SetAction(args[1], args[2].equals("infinite") ? 0 : Integer.parseInt(args[2]));
            player.sendMessage(ConfigManager.getMessage("messages.set_size"));

            plugin.getPlayerEvents().addClickListener(player.getUniqueId(), shop -> {

                shop.closeAllMenus();
                if (action.action.equals("storage")) {
                    shop.setStorageSize(action.size);
                } else {
                    shop.setShopfrontSize(action.size);
                }
                shop.save();
                plugin.getShopManager().loadShop(shop.getFile());
                player.sendMessage(ConfigManager.getMessage("messages.size_set").replace("%size%", action.size == 0 ? "infinite" : String.valueOf(action.size)));

            });
        }
    }

    @Override
    public String getDescription() {
        return "Set shop size or storage size";
    }

    @Override
    public String getUsage() {
        return "<shop|storage> <size>";
    }

    private static class SetAction {
        private final String action;
        private final int size;

        public SetAction(String action, int size) {
            this.action = action;
            this.size = size;
        }
    }
}
