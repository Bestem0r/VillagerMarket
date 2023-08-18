package net.bestemor.villagermarket.command.subcommand;

import net.bestemor.core.command.ISubCommand;
import net.bestemor.core.config.ConfigManager;
import net.bestemor.villagermarket.VMPlugin;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class CloneCommand implements ISubCommand {

    private final VMPlugin plugin;

    public CloneCommand(VMPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public List<String> getCompletion(String[] strings) {
        return new ArrayList<>();
    }

    @Override
    public void run(CommandSender sender, String[] strings) {
        if (sender instanceof Player) {
            Player player = (Player) sender;

            player.sendMessage(ConfigManager.getMessage("messages.clone_shop"));

            plugin.getPlayerEvents().addClickListener(player.getUniqueId(), shop -> {
                plugin.getShopManager().cloneShop(shop, player.getLocation());
                player.sendMessage(ConfigManager.getMessage("messages.shop_cloned"));
            });
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
}
