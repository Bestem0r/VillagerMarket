package me.bestem0r.villagermarket.commands;

import me.bestem0r.villagermarket.utilities.ColorBuilder;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;
import java.util.stream.Collectors;

public class CommandModule implements CommandExecutor, TabCompleter {

    private Map<String, SubCommand> subCommands;
    private JavaPlugin plugin;

    private CommandModule() {}

    public static class Builder {

        private final JavaPlugin plugin;
        private final Map<String, SubCommand> subCommands = new HashMap<>();

        public Builder(JavaPlugin plugin) {
            this.plugin = plugin;
        }

        public Builder addSubCommand(String label, SubCommand subCommand) {
            subCommands.put(label, subCommand);
            return this;
        }
        public CommandModule build() {
            CommandModule commandModule = new CommandModule();
            subCommands.forEach((k, v) -> v.setModule(commandModule));
            commandModule.subCommands = subCommands;
            commandModule.plugin = plugin;
            return commandModule;
        }
    }

    private static class HelpCommand implements SubCommand {
        private CommandModule commandModule;

        @Override
        public List<String> getCompletion(int index, String[] args) {
            return new ArrayList<>(); }

        @Override
        public void run(CommandSender sender, String[] args) {
            List<String> help = new ArrayList<>();
            help.add("§bVillagerMarket Commands:");
            commandModule.subCommands.forEach((k, v) -> {
                help.add("> " + "§e" + v.getDescription());
            });
            help.forEach(s -> commandModule.commandOutput(sender, s));
        }
        @Override
        public void setModule(CommandModule module) {
            this.commandModule = module;
        }

        @Override
        public String getDescription() {
            return "List all of SpawnerCollectors' commands";
        }
    }

    public void register(String command) {
        HelpCommand helpCommand = new HelpCommand();
        helpCommand.setModule(this);
        subCommands.put("help", helpCommand);

        plugin.getCommand(command).setExecutor(this);
        plugin.getCommand(command).setTabCompleter(this);
    }

    public void commandOutput(CommandSender sender, String message) {
        if (sender instanceof Player) {
            Player player = (Player) sender;
            player.sendMessage(message);
        } else {
            Bukkit.getLogger().info(message);
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String s, String[] args) {
        if (args.length == 0 || !subCommands.containsKey(args[0])) {
            commandOutput(sender, new ColorBuilder(plugin).path("messages.invalid_command_usage").addPrefix().build());
            return true;
        }

        if (sender instanceof Player) {
            Player player = (Player) sender;
            if (!player.hasPermission("villagermarket.command." + args[0])) {
                player.sendMessage(new ColorBuilder(plugin).path("messages.no_permission_command").addPrefix().build());
                return true;
            }
        }
        subCommands.get(args[0]).run(sender, args);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender commandSender, Command command, String s, String[] args) {

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
