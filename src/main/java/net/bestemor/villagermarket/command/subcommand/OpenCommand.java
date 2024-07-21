package net.bestemor.villagermarket.command.subcommand;

import net.bestemor.core.command.ISubCommand;
import net.bestemor.villagermarket.VMPlugin;
import net.bestemor.villagermarket.shop.VillagerShop;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class OpenCommand implements ISubCommand {

    private final VMPlugin plugin;

    public OpenCommand(VMPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public List<String> getCompletion(String[] args) {
        if (args.length == 2) {
            return plugin.getShopManager().getShops().stream()
                    .map(VillagerShop::getEntityUUID)
                    .map(Object::toString)
                    .collect(Collectors.toList());
        }
        return null;
    }

    @Override
    public void run(CommandSender sender, String[] args) {
        if (args.length != 3) {
            sender.sendMessage("§cIncorrect number of arguments.");
            sender.sendMessage("§c/vm open <shop> <player>");
            return;
        }

        UUID shopUUID;
        try {
            shopUUID = UUID.fromString(args[1]);
        } catch (IllegalArgumentException e) {
            sender.sendMessage("§cInvalid UUID.");
            return;
        }
        VillagerShop shop = plugin.getShopManager().getShop(shopUUID);
        if (shop == null) {
            sender.sendMessage("§cShop not found.");
            return;
        }

        Player target = plugin.getServer().getPlayer(args[2]);
        if (target == null) {
            sender.sendMessage("§cPlayer not found.");
            return;
        }

        plugin.getShopManager().openShop(target, shop, false);
    }

    @Override
    public String getDescription() {
        return "Opens specified shop for player.";
    }

    @Override
    public String getUsage() {
        return "<shop> <player>";
    }
}
