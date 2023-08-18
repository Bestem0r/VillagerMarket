package net.bestemor.villagermarket.command;

import net.bestemor.core.config.ConfigManager;
import net.bestemor.villagermarket.VMPlugin;
import net.bestemor.villagermarket.shop.ShopMenu;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class ShopCommand implements CommandExecutor {

    private final VMPlugin plugin;

    public ShopCommand(VMPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String s, @NotNull String[] args) {

        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players can use this command");
            return false;
        }

        Player player = (Player) sender;
        if (!player.hasPermission("villagermarket.command.shop")) {
            player.sendMessage(ConfigManager.getMessage("messages.no_permission_command"));
            return true;
        }

        String id = ConfigManager.getString("default_admin_shop");
        if (id.isEmpty()) {
            player.sendMessage("§cNo default admin shop set");
            return true;
        }

        plugin.getShopManager().getShop(UUID.fromString(id)).openInventory(player, ShopMenu.CUSTOMER);

        return true;
    }
}
