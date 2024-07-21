package net.bestemor.villagermarket.command.subcommand;

import net.bestemor.core.command.ISubCommand;
import net.bestemor.core.config.ConfigManager;
import net.bestemor.villagermarket.VMPlugin;
import net.bestemor.villagermarket.shop.PlayerShop;
import net.bestemor.villagermarket.shop.VillagerShop;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;

import java.util.ArrayList;
import java.util.List;

public class TrustedCommand implements ISubCommand {

    private final VMPlugin plugin;

    public enum Action {
        ADD,
        REMOVE
    }

    public TrustedCommand(VMPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public List<String> getCompletion(String[] args) {
        List<String> list = new ArrayList<>();
        switch (args.length) {
            case 2:
                list.add("add");
                list.add("remove");
                return list;
            case 3:
                return null;
            default:
                return list;
        }
    }

    @Override
    public void run(CommandSender sender, String[] args) {

        if (!(sender instanceof Player)) {
            return;
        }
        Player player = (Player) sender;

        if (args.length < 3) {
            player.sendMessage(ChatColor.RED + "Incorrect number of arguments!");
            player.sendMessage(ChatColor.RED + "/vm trusted <add/remove> <player>");
            return;
        }

        if (!args[1].equals("add") && !args[1].equals("remove")) {
            player.sendMessage(ChatColor.RED + "Unknown subcommand: " + args[1]);
            player.sendMessage(ChatColor.RED + "/vm trusted <add/remove> <player>");
            return;
        }
        Player target = Bukkit.getPlayer(args[2]);
        if (target == null) {
            player.sendMessage(ChatColor.RED + "Could not find player: " + args[2]);
            return;
        }

        plugin.getPlayerListener().addCancelledPlayer(player.getUniqueId());
        switch (args[1]) {
            case "add":
                player.sendMessage(ConfigManager.getMessage("messages.add_trusted"));
                Bukkit.getPluginManager().registerEvents(new TrustedEvent(player, target, Action.ADD), plugin);
                break;
            case "remove":
                player.sendMessage(ConfigManager.getMessage("messages.remove_trusted"));
                Bukkit.getPluginManager().registerEvents(new TrustedEvent(player, target, Action.REMOVE), plugin);
        }
    }

    @Override
    public String getDescription() {
        return "Remove/remove trusted";
    }

    @Override
    public String getUsage() {
        return "<add/remove> <player>";
    }

    @Override
    public boolean requirePermission() {
        return true;
    }

    private class TrustedEvent implements Listener {

        private final Player sender;
        private final Player target;
        private final Action action;

        public TrustedEvent(Player sender, Player target, Action action) {
            this.sender = sender;
            this.target = target;
            this.action = action;
        }

        @EventHandler(priority = EventPriority.LOW)
        public void onEntityInteract(PlayerInteractEntityEvent event) {
            if (event.getPlayer() != sender) return;
            event.setCancelled(true);

            HandlerList.unregisterAll(this);
            plugin.getPlayerListener().removeCancelledPlayer(sender.getUniqueId());

            VillagerShop shop = plugin.getShopManager().getShop(event.getRightClicked().getUniqueId());
            if (shop != null) {
                if (shop instanceof PlayerShop) {
                    PlayerShop playerShop = (PlayerShop) shop;
                    if (!playerShop.getOwnerUUID().equals(sender.getUniqueId())) {
                        sender.sendMessage(ConfigManager.getMessage("messages.not_owner"));
                        return;
                    }
                    switch (action) {
                        case ADD:
                            playerShop.addTrusted(target);
                            sender.sendMessage(ConfigManager.getMessage("messages.trusted_added"));
                            break;
                        case REMOVE:
                            playerShop.removeTrusted(target);
                            sender.sendMessage(ConfigManager.getMessage("messages.trusted_removed"));
                            break;
                    }
                } else {
                    sender.sendMessage(ConfigManager.getMessage("messages.not_playershop"));
                }
            } else {
                sender.sendMessage(ConfigManager.getMessage("messages.no_villager_shop"));
            }
        }
    }
}
