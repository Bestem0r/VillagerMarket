package me.bestem0r.villagermarket.commands.subcommands;

import me.bestem0r.villagermarket.commands.CommandModule;
import me.bestem0r.villagermarket.commands.SubCommand;
import me.bestem0r.villagermarket.shops.PlayerShop;
import me.bestem0r.villagermarket.shops.VillagerShop;
import me.bestem0r.villagermarket.utilities.ColorBuilder;
import me.bestem0r.villagermarket.utilities.Methods;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

public class TrustedCommand implements SubCommand {

    private final JavaPlugin plugin;

    public enum Action {
        ADD,
        REMOVE
    }

    public TrustedCommand(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public List<String> getCompletion(int index, String[] args) {
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
        switch (args[1]) {
            case "add":
                player.sendMessage(new ColorBuilder(plugin).path("messages.add_trusted").addPrefix().build());
                Bukkit.getPluginManager().registerEvents(new TrustedEvent(player, target, Action.ADD), plugin);
                break;
            case "remove":
                player.sendMessage(new ColorBuilder(plugin).path("messages.remove_trusted").addPrefix().build());
                Bukkit.getPluginManager().registerEvents(new TrustedEvent(player, target, Action.REMOVE), plugin);
        }
    }

    @Override
    public void setModule(CommandModule module) {

    }

    @Override
    public String getDescription() {
        return "Remove/remove trusted: &6/vm trusted <add/remove> <player>";
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

        @EventHandler(priority = EventPriority.LOWEST)
        public void onEntityInteract(PlayerInteractEntityEvent event) {
            if (event.getPlayer() != sender) return;
            event.setCancelled(true);

            HandlerList.unregisterAll(this);

            VillagerShop villagerShop = Methods.shopFromUUID(event.getRightClicked().getUniqueId());
            if (villagerShop != null) {
                if (villagerShop instanceof PlayerShop) {
                    PlayerShop playerShop = (PlayerShop) villagerShop;
                    if (!playerShop.getOwnerUUID().equals(sender.getUniqueId().toString())) {
                        sender.sendMessage(new ColorBuilder(plugin).path("messages.not_owner").addPrefix().build());
                        return;
                    }
                    switch (action) {
                        case ADD:
                            playerShop.addTrusted(target);
                            sender.sendMessage(new ColorBuilder(plugin).path("messages.trusted_added").addPrefix().build());
                            break;
                        case REMOVE:
                            playerShop.removeTrusted(target);
                            sender.sendMessage(new ColorBuilder(plugin).path("messages.trusted_removed").addPrefix().build());
                            break;
                    }
                } else {
                    sender.sendMessage(new ColorBuilder(plugin).path("messages.not_playershop").addPrefix().build());
                }
            } else {
                sender.sendMessage(new ColorBuilder(plugin).path("messages.no_villager_shop").addPrefix().build());
            }
        }
    }
}
