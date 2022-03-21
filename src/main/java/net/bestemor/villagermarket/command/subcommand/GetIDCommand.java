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

public class GetIDCommand implements ISubCommand {

    private final VMPlugin plugin;
    private final GetIDEvent listener;

    public GetIDCommand(VMPlugin plugin ) {
        this.plugin = plugin;

        this.listener = new GetIDEvent();
        Bukkit.getPluginManager().registerEvents(listener, plugin);
    }

    @Override
    public List<String> getCompletion(String[] args) {
        return new ArrayList<>();
    }

    @Override
    public void run(CommandSender sender, String[] args) {
        if (sender instanceof Player) {
            Player player = (Player) sender;

            if (listener.players.contains(player.getUniqueId())) {
                return;
            }

            plugin.getPlayerEvents().addCancelledPlayer(player.getUniqueId());
            listener.players.add(player.getUniqueId());
            player.sendMessage(ConfigManager.getMessage("messages.get_id"));
        }
    }

    @Override
    public String getDescription() {
        return "Get shop UUID";
    }

    @Override
    public String getUsage() {
        return "";
    }

    @Override
    public boolean requirePermission() {
        return true;
    }

    private class GetIDEvent implements Listener {

        private final List<UUID> players = new ArrayList<>();

        @EventHandler(priority = EventPriority.LOW)
        public void onInteract(PlayerInteractEntityEvent event) {

            if (!players.contains(event.getPlayer().getUniqueId())) { return; }
            event.setCancelled(true);

            VillagerShop shop = plugin.getShopManager().getShop(event.getRightClicked().getUniqueId());
            Player player = event.getPlayer();
            if (shop != null) {
                player.sendMessage(ConfigManager.getMessage("messages.id").replace("%id%", shop.getEntityUUID().toString()));
            } else {
                player.sendMessage(ConfigManager.getMessage("messages.no_villager_shop"));
            }
            plugin.getPlayerEvents().removeCancelledPlayer(player.getUniqueId());
            players.remove(player.getUniqueId());
        }

    }

}
