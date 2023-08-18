package net.bestemor.villagermarket.command.subcommand;

import net.bestemor.core.command.ISubCommand;
import net.bestemor.core.config.ConfigManager;
import net.bestemor.villagermarket.VMPlugin;
import net.bestemor.villagermarket.shop.AdminShop;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class ToggleRequirePermissionCommand implements ISubCommand {

    private final VMPlugin plugin;

    public ToggleRequirePermissionCommand(VMPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public List<String> getCompletion(String[] strings) {
        return new ArrayList<>();
    }

    @Override
    public void run(CommandSender sender, String[] strings) {
        if (!(sender instanceof Player)) {
            return;
        }

        Player player = (Player) sender;
        player.sendMessage(ConfigManager.getMessage("messages.toggle_require_permission"));

        plugin.getPlayerEvents().addClickListener(player.getUniqueId(), shop -> {
            shop.setRequirePermission(!shop.isRequirePermission());

            if (shop.isRequirePermission()) {
                String perm = shop instanceof AdminShop ? "villagermarket.adminshop." : "villagermarket.playershop.";
                player.sendMessage(ConfigManager.getMessage("messages.require_permission_enabled")
                        .replace("%permission%", perm + shop.getEntityUUID()));
            } else {
                player.sendMessage(ConfigManager.getMessage("messages.require_permission_disabled"));
            }
        });
    }

    @Override
    public String getDescription() {
        return "Toggle require permission";
    }

    @Override
    public String getUsage() {
        return "";
    }
}
