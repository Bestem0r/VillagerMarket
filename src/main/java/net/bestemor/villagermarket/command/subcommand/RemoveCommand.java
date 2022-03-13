package net.bestemor.villagermarket.command.subcommand;

import net.bestemor.villagermarket.ConfigManager;
import net.bestemor.villagermarket.VMPlugin;
import net.bestemor.villagermarket.command.ISubCommand;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class RemoveCommand implements ISubCommand {

    private final VMPlugin plugin;
    private final RemoveEvent listener;

    public RemoveCommand(VMPlugin plugin) {
        this.plugin = plugin;
        this.listener = new RemoveEvent();
        Bukkit.getPluginManager().registerEvents(listener, plugin);
    }

    @Override
    public List<String> getCompletion(int index, String[] args) {
        return new ArrayList<>();
    }

    @Override
    public void run(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            return;
        }
        Player player = (Player) sender;

        if (listener.players.contains(player.getUniqueId())) {
            return;
        }

        player.sendMessage(ConfigManager.getMessage("messages.remove_villager"));
        plugin.getPlayerEvents().addCancelledPlayer(player.getUniqueId());
        listener.players.add(player.getUniqueId());
    }

    @Override
    public String getDescription() {
        return "Remove trusted: &6/vm trusted remove <player>";
    }

    @Override
    public boolean requirePermission() {
        return true;
    }

    private class RemoveEvent implements Listener {

        private final List<UUID> players = new ArrayList<>();

        @EventHandler(priority = EventPriority.LOW)
        public void onInteract(PlayerInteractEntityEvent event) {
            if (!players.contains(event.getPlayer().getUniqueId())) { return; }
            event.setCancelled(true);

            Player player = event.getPlayer();
            if (plugin.getShopManager().isShop(event.getRightClicked())) {
                player.sendMessage(ConfigManager.getMessage("messages.villager_removed"));
                player.playSound(player.getLocation(), ConfigManager.getSound("sounds.remove_villager"), 0.5f, 1);
                plugin.getShopManager().removeShop(event.getRightClicked().getUniqueId());
            } else {
                player.sendMessage(ConfigManager.getMessage("messages.no_villager_shop"));
            }
            plugin.getPlayerEvents().removeCancelledPlayer(player.getUniqueId());
            players.remove(player.getUniqueId());
        }
    }
}
