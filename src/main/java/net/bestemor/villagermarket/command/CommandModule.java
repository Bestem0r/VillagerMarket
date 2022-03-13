package net.bestemor.villagermarket.command;

import net.bestemor.villagermarket.ConfigManager;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class CommandModule implements CommandExecutor, TabCompleter {

    private Map<String, ISubCommand> subCommands;
    private JavaPlugin plugin;
    private String permissionPrefix;

    private CommandModule() {}

    public static class Builder {

        private final JavaPlugin plugin;
        private final Map<String, ISubCommand> subCommands = new HashMap<>();
        private String permissionPrefix = "command";

        public Builder(JavaPlugin plugin) {
            this.plugin = plugin;
        }

        public Builder addSubCommand(String label, ISubCommand ISubCommand) {
            subCommands.put(label, ISubCommand);
            return this;
        }

        public Builder permissionPrefix(String prefix) {
            this.permissionPrefix = prefix;
            return this;
        }

        public CommandModule build() {
            CommandModule commandModule = new CommandModule();
            commandModule.subCommands = subCommands;
            commandModule.plugin = plugin;
            commandModule.permissionPrefix = permissionPrefix;
            return commandModule;
        }
    }

    private static class HelpCommandI implements ISubCommand {
        private CommandModule commandModule;

        private HelpCommandI(CommandModule commandModule) {
            this.commandModule = commandModule;
        }

        @Override
        public List<String> getCompletion(int index, String[] args) {
            return new ArrayList<>(); }

        @Override
        public void run(CommandSender sender, String[] args) {
            List<String> help = new ArrayList<>();
            help.add("§bVillagerMarket Commands:");
            commandModule.subCommands.forEach((k, v) -> {
                help.add("> " + "§e" + ChatColor.translateAlternateColorCodes('&', v.getDescription()));
            });
            help.forEach(sender::sendMessage);
        }
        @Override
        public String getDescription() {
            return "/vm help: &6List all of Villager Market's commands";
        }

        @Override
        public boolean requirePermission() {
            return false;
        }
    }

    public void register(String command) {
        HelpCommandI helpCommand = new HelpCommandI(this);
        subCommands.put("help", helpCommand);

        plugin.getCommand(command).setExecutor(this);
        plugin.getCommand(command).setTabCompleter(this);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, String s, String[] args) {
        if (args.length == 0 || !subCommands.containsKey(args[0])) {
            sender.sendMessage(ConfigManager.getMessage("messages.invalid_command_usage"));
            return true;
        }

        if (subCommands.get(args[0]).requirePermission() && sender instanceof Player) {
            Player player = (Player) sender;
            if (!player.hasPermission(permissionPrefix + "." + args[0])) {
                player.sendMessage(ConfigManager.getMessage("messages.no_permission_command"));
                return true;
            }
        }
        subCommands.get(args[0]).run(sender, args);
        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender commandSender, @NotNull Command command, String s, String[] args) {

        if (args.length == 0 || args[0].length() == 0) {
            return new ArrayList<>(subCommands.keySet());
        }

        //Tab completion for registered sub-commands
        if (args.length == 1) {
            return subCommands.keySet().stream()
                    .filter(c -> c.startsWith(args[0]))
                    .collect(Collectors.toList());
        }
        if (subCommands.containsKey(args[0])) {
            return subCommands.get(args[0]).getCompletion(args.length - 2, args);
        }
        return null;
    }
}
