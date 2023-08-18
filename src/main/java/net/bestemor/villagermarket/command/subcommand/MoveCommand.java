package net.bestemor.villagermarket.command.subcommand;

import net.bestemor.core.command.ISubCommand;
import net.bestemor.core.config.ConfigManager;
import net.bestemor.villagermarket.VMPlugin;
import net.bestemor.villagermarket.shop.VillagerShop;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.*;

public class MoveCommand implements ISubCommand {

    private final VMPlugin plugin;
    private final EventListener listener;

    public MoveCommand(VMPlugin plugin) {
        this.plugin = plugin;
        this.listener = new EventListener();

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
            if (listener.selectShop.contains(player.getUniqueId())) {
                return;
            }

            player.sendMessage(ConfigManager.getMessage("messages.move_villager"));

            plugin.getPlayerEvents().addCancelledPlayer(player.getUniqueId());
            listener.selectShop.add(player.getUniqueId());
        }
    }

    @Override
    public String getDescription() {
        return "Move shop: &6/vm move";
    }

    @Override
    public String getUsage() {
        return "";
    }

    @Override
    public boolean requirePermission() {
        return true;
    }

    private class EventListener implements Listener {

        private final List<UUID> selectShop = new ArrayList<>();
        private final Map<UUID, Entity> moveShop = new HashMap<>();

        @EventHandler(priority = EventPriority.LOW)
        public void onInteract(PlayerInteractEntityEvent event) {

            if (!selectShop.contains(event.getPlayer().getUniqueId())) { return; }
            event.setCancelled(true);

            VillagerShop shop = plugin.getShopManager().getShop(event.getRightClicked().getUniqueId());
            Player player = event.getPlayer();
            if (shop != null) {
                player.sendMessage(ConfigManager.getMessage("messages.move_villager_to"));
                moveShop.put(player.getUniqueId(), event.getRightClicked());
            } else {
                player.sendMessage(ConfigManager.getMessage("messages.no_villager_shop"));
            }
            plugin.getPlayerEvents().removeCancelledPlayer(player.getUniqueId());
            selectShop.remove(player.getUniqueId());
        }

        @EventHandler (priority = EventPriority.LOWEST)
        public void onInteract(PlayerInteractEvent event) {

            if (!moveShop.containsKey(event.getPlayer().getUniqueId()) || event.getClickedBlock() == null) { return; }
            event.setCancelled(true);

            Entity entity = moveShop.get(event.getPlayer().getUniqueId());

            if (entity != null) {
                entity.teleport(event.getClickedBlock().getLocation().add(0.5, 1, 0.5));
            }
            moveShop.remove(event.getPlayer().getUniqueId());
        }
    }
}
