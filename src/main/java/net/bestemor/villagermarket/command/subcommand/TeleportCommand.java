package net.bestemor.villagermarket.command.subcommand;

import net.bestemor.core.command.ISubCommand;
import net.bestemor.core.config.ConfigManager;
import net.bestemor.villagermarket.VMPlugin;
import net.bestemor.villagermarket.shop.VillagerShop;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class TeleportCommand implements ISubCommand {

    private final VMPlugin plugin;

    public TeleportCommand(VMPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public List<String> getCompletion(String[] args) {
        if (args.length == 2) {
            List<String> completions = new ArrayList<>();
            
            // add UUIDs for tab completion
            for (VillagerShop shop : plugin.getShopManager().getShops()) {
                // first 8 characters only
                String shortUuid = shop.getEntityUUID().toString().substring(0, 8);
                completions.add(shortUuid);
            }
            
            // filter completions based on what user typed
            String input = args[1].toLowerCase();
            if (!input.isEmpty()) {
                completions.removeIf(completion -> !completion.toLowerCase().startsWith(input));
            }
            
            return completions;
        }
        return new ArrayList<>();
    }

    @Override
    public void run(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ConfigManager.getMessage("messages.player_only"));
            return;
        }

        if (args.length < 2) {
            player.sendMessage(ConfigManager.getMessage("messages.teleport_usage"));
            return;
        }

        String identifier = args[1];
        VillagerShop targetShop = null;

        // find shop by UUID
        for (VillagerShop shop : plugin.getShopManager().getShops()) {
            String uuid = shop.getEntityUUID().toString();
            if (uuid.toLowerCase().startsWith(identifier.toLowerCase()) || uuid.equalsIgnoreCase(identifier)) {
                targetShop = shop;
                break;
            }
        }

        if (targetShop == null) {
            player.sendMessage(ConfigManager.getMessage("messages.villager_shop_not_found"));
            return;
        }

        // use saved location from EntityInfo
        Location shopLocation = targetShop.getEntityInfo().getLocation();
        if (shopLocation != null && shopLocation.getWorld() != null) {
            player.teleport(shopLocation);
            String shopName = targetShop.getShopName() != null ? targetShop.getShopName() : "Villager Shop";
            player.sendMessage(ConfigManager.getMessage("messages.teleported_to_villager").replace("%shop%", shopName));
        } else {
            player.sendMessage(ConfigManager.getMessage("messages.teleport_location_invalid"));
        }
    }

    @Override
    public String getDescription() {
        return "Teleport to shop";
    }

    @Override
    public String getUsage() {
        return "<id>";
    }

}