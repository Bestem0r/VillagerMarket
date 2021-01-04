package me.bestem0r.villagermarket.commands.subcommands;

import me.bestem0r.villagermarket.commands.CommandModule;
import me.bestem0r.villagermarket.commands.SubCommand;
import me.bestem0r.villagermarket.utilities.ColorBuilder;
import me.bestem0r.villagermarket.utilities.Methods;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

public class SearchCommand implements SubCommand {

    private final JavaPlugin plugin;

    public SearchCommand(JavaPlugin plugin) {
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

        if (args.length != 2) {
            player.sendMessage(ChatColor.RED + "Incorrect usage: Specify radius!");
            player.sendMessage(ChatColor.RED + "/vm search <radius>");
            return;
        }
        if (!canConvert(args[1])) {
            player.sendMessage(ChatColor.RED + "Incorrect usage: Radius must be a number!");
            player.sendMessage(ChatColor.RED + "/vm search <radius>");
            return;
        }
        double radius = Double.parseDouble(args[1]);
        if (radius > 10000) {
            player.sendMessage(ChatColor.RED + "Radius can't be more than 10 000 blocks!");
            return;
        }
        int result = 0;
        List<TextComponent> shopInfo = new ArrayList<>();
        for (Entity entity : player.getNearbyEntities(radius, radius, radius)) {
            if (Methods.shopFromUUID(entity.getUniqueId()) != null) {
                Location location = entity.getLocation();
                TextComponent component = new TextComponent(new ColorBuilder(plugin).path("messages.search_shop_info")
                        .replace("%name%", entity.getCustomName())
                        .replace("%location%", location.getBlockX() + " " + location.getBlockY() + " " + location.getBlockZ())
                        .build());
                component.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tp " + location.getX() + " " + location.getY() + " " + location.getZ()));
                component.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text("Click to teleport!")));
                shopInfo.add(component);
                result++;
            }
        }
        player.sendMessage(new ColorBuilder(plugin).path("messages.search_result").replace("%amount%", String.valueOf(result)).addPrefix().build());
        shopInfo.forEach(m -> player.spigot().sendMessage(m));
    }

    private boolean canConvert(String string) {
        for (int i = 0; i < string.length(); i++) {
            char c = string.charAt(i);
            if (c < '0' || c > '9') {
                return false;
            }
        }
        return true;
    }

    @Override
    public void setModule(CommandModule module) {

    }

    @Override
    public String getDescription() {
        return "Search for nearby shops: &6/vm search <radius>";
    }
}
