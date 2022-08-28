package net.bestemor.villagermarket.command.subcommand;

import net.bestemor.core.command.ISubCommand;
import net.bestemor.core.config.ConfigManager;
import net.bestemor.villagermarket.VMPlugin;
import net.bestemor.villagermarket.shop.VillagerShop;
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

public class CloneCommand implements ISubCommand {

    private final VMPlugin plugin;
    private final CloneListener listener;

    public CloneCommand(VMPlugin plugin) {
        this.plugin = plugin;
        this.listener = new CloneListener();
        Bukkit.getPluginManager().registerEvents(this.listener, plugin);
    }

    @Override
    public List<String> getCompletion(String[] strings) {
        return new ArrayList<>();
    }

    @Override
    public void run(CommandSender sender, String[] strings) {
        if (sender instanceof Player) {
            Player player = (Player) sender;

            if (listener.players.contains(player.getUniqueId())) {
                return;
            }

            plugin.getPlayerEvents().addCancelledPlayer(player.getUniqueId());
            listener.players.add(player.getUniqueId());
            player.sendMessage(ConfigManager.getMessage("messages.clone_shop"));
        }
    }

    @Override
    public String getDescription() {
        return "Clone Villager Shop";
    }

    @Override
    public String getUsage() {
        return "";
    }

    @Override
    public boolean requirePermission() {
        return true;
    }

    private class CloneListener implements Listener {
        private final List<UUID> players = new ArrayList<>();

        @EventHandler(priority = EventPriority.LOW)
        public void onInteract(PlayerInteractEntityEvent event) {

            if (!players.contains(event.getPlayer().getUniqueId())) { return; }
            event.setCancelled(true);

            VillagerShop shop = plugin.getShopManager().getShop(event.getRightClicked().getUniqueId());
            Player player = event.getPlayer();
            if (shop != null) {
                plugin.getShopManager().cloneShop(shop, player.getLocation());
                player.sendMessage(ConfigManager.getMessage("messages.shop_cloned"));
            } else {
                player.sendMessage(ConfigManager.getMessage("messages.no_villager_shop"));
            }
            plugin.getPlayerEvents().removeCancelledPlayer(player.getUniqueId());
            players.remove(player.getUniqueId());
        }

    }
}
