package net.bestemor.villagermarket.command.subcommand;

import net.bestemor.core.command.ISubCommand;
import net.bestemor.core.config.ConfigManager;
import net.bestemor.villagermarket.VMPlugin;
import net.bestemor.villagermarket.shop.PlayerShop;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class StatsCommand implements ISubCommand {

    private final VMPlugin plugin;

    public StatsCommand(VMPlugin plugin) {
        this.plugin = plugin;
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

        player.sendMessage(ConfigManager.getMessage("messages.get_stats"));

        plugin.getPlayerListener().addClickListener(player.getUniqueId(), shop -> {
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
        });
    }


    @Override
    public String getDescription() {
        return "Show shop statistics";
    }

    @Override
    public String getUsage() {
        return "";
    }

    @Override
    public boolean requirePermission() {
        return true;
    }

}
