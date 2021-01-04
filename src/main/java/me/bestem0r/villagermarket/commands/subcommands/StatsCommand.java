package me.bestem0r.villagermarket.commands.subcommands;

import me.bestem0r.villagermarket.commands.CommandModule;
import me.bestem0r.villagermarket.commands.SubCommand;
import me.bestem0r.villagermarket.shops.VillagerShop;
import me.bestem0r.villagermarket.utilities.ColorBuilder;
import me.bestem0r.villagermarket.utilities.Methods;
import org.bukkit.Bukkit;
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

public class StatsCommand implements SubCommand {

    private final JavaPlugin plugin;

    public StatsCommand(JavaPlugin plugin) {
        this.plugin = plugin;
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

        player.sendMessage(new ColorBuilder(plugin).path("messages.get_stats").addPrefix().build());
        Bukkit.getPluginManager().registerEvents(new StatsEvent(player), plugin);
    }

    @Override
    public void setModule(CommandModule module) {

    }

    @Override
    public String getDescription() {
        return "how Shop statistics: &6/vm stats";
    }

    private class StatsEvent implements Listener {

        private final Player player;

        public StatsEvent(Player player) {
            this.player = player;
        }

        @EventHandler(priority = EventPriority.LOWEST)
        public void onInteract(PlayerInteractEntityEvent event) {
            if (event.getPlayer() != player) return;
            event.setCancelled(true);
            VillagerShop villagerShop = Methods.shopFromUUID(event.getRightClicked().getUniqueId());

            HandlerList.unregisterAll(this);
            if (villagerShop != null) {
                if (!player.hasPermission("villagermarket.spy") && !villagerShop.getOwnerUUID().equals(player.getUniqueId().toString())) {
                    player.sendMessage(new ColorBuilder(plugin).path("messages.not_owner").addPrefix().build());
                    return;
                }
                villagerShop.sendStats(player);
            } else {
                player.sendMessage(new ColorBuilder(plugin).path("messages.no_villager_shop").addPrefix().build());
            }
        }
    }

}
