package net.bestemor.villagermarket.command.subcommand;

import net.bestemor.core.command.ISubCommand;
import net.bestemor.core.config.ConfigManager;
import net.bestemor.villagermarket.VMPlugin;
import net.bestemor.villagermarket.shop.PlayerShop;
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

public class StatsCommand implements ISubCommand {

    private final VMPlugin plugin;
    private final StatsEvent listener = new StatsEvent();

    public StatsCommand(VMPlugin plugin) {
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this.listener, plugin);
    }

    @Override
    public List<String> getCompletion(String[] args) {
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

        player.sendMessage(ConfigManager.getMessage("messages.get_stats"));

        plugin.getPlayerEvents().addCancelledPlayer(player.getUniqueId());
        listener.players.add(player.getUniqueId());
    }


    @Override
    public String getDescription() {
        return "Show shop statistics";
    }

    @Override
    public String getUsage() {
        return null;
    }

    @Override
    public boolean requirePermission() {
        return true;
    }

    private class StatsEvent implements Listener {

        private final List<UUID> players = new ArrayList<>();

        @EventHandler(priority = EventPriority.LOW)
        public void onInteract(PlayerInteractEntityEvent event) {

            if (!players.contains(event.getPlayer().getUniqueId())) {
                return;
            }
            players.remove(event.getPlayer().getUniqueId());
            plugin.getPlayerEvents().removeCancelledPlayer(event.getPlayer().getUniqueId());

            Player player = event.getPlayer();
            event.setCancelled(true);

            VillagerShop shop = plugin.getShopManager().getShop(event.getRightClicked().getUniqueId());

            if (shop != null) {
                boolean show = player.hasPermission("villagermarket.spy");
                if (shop instanceof PlayerShop) {
                    PlayerShop playerShop = (PlayerShop) shop;
                    show = show || (playerShop.hasOwner() && playerShop.getOwnerUUID().equals(player.getUniqueId()));
                }
                if (!show) {
                    player.sendMessage(ConfigManager.getMessage("messages.not_owner"));
                    return;
                }
                shop.sendStats(player);
            } else {
                player.sendMessage(ConfigManager.getMessage("messages.no_villager_shop"));
            }
        }
    }

}
