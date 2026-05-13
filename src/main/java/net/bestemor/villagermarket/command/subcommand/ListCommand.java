package net.bestemor.villagermarket.command.subcommand;

import net.bestemor.core.command.ISubCommand;
import net.bestemor.core.config.ConfigManager;
import net.bestemor.villagermarket.VMPlugin;
import net.bestemor.villagermarket.shop.VillagerShop;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class ListCommand implements ISubCommand {

    private final VMPlugin plugin;

    public ListCommand(VMPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public List<String> getCompletion(String[] args) {
        return new ArrayList<>();
    }

    @Override
    public void run(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ConfigManager.getMessage("messages.player_only"));
            return;
        }

        player.sendMessage(ConfigManager.getMessage("messages.list_header"));
        
        int count = 0;
        for (VillagerShop shop : plugin.getShopManager().getShops()) {
            String shopName = shop.getShopName();
            if (shopName == null || shopName.isEmpty()) {
                shopName = "Unnamed Shop";
            }
            
            String uuid = shop.getEntityUUID().toString().substring(0, 8);
            Location location = shop.getEntityInfo().getLocation();

            TextComponent shopComponent = new TextComponent("§7- §b" + shopName + " §7(ID: §e" + uuid + "§7)");
            shopComponent.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/vm teleport " + uuid));
            shopComponent.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text("§aClick to teleport to this shop!")));
            
            player.spigot().sendMessage(shopComponent);
            
            if (location != null && location.getWorld() != null) {
                String locationStr = location.getBlockX() + ", " + 
                                   location.getBlockY() + ", " + 
                                   location.getBlockZ();
                String world = location.getWorld().getName();
                
                // create location text with clickable coordinates
                TextComponent locationPrefix = new TextComponent("  §7Location: ");
                TextComponent clickableCoords = new TextComponent("§f" + locationStr);
                clickableCoords.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/vm teleport " + uuid));
                clickableCoords.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text("§aClick to teleport!")));
                TextComponent worldSuffix = new TextComponent(" §7in §f" + world);
                
                // combine components
                locationPrefix.addExtra(clickableCoords);
                locationPrefix.addExtra(worldSuffix);
                
                player.spigot().sendMessage(locationPrefix);
            } else {
                player.sendMessage(ConfigManager.getMessage("messages.list_location_unknown"));
            }
            count++;
        }
        
        if (count == 0) {
            player.sendMessage(ConfigManager.getMessage("messages.list_no_shops"));
        } else {
            player.sendMessage(ConfigManager.getMessage("messages.list_found_shops").replace("%count%", String.valueOf(count)));
            player.sendMessage(ConfigManager.getMessage("messages.list_click_help"));
        }
    }

    @Override
    public String getDescription() {
        return "List all villager shops";
    }

    @Override
    public String getUsage() {
        return "";
    }

}