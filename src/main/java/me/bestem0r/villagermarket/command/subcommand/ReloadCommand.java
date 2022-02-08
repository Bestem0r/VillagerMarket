package me.bestem0r.villagermarket.command.subcommand;

import me.bestem0r.villagermarket.ConfigManager;
import me.bestem0r.villagermarket.VMPlugin;
import me.bestem0r.villagermarket.command.CommandModule;
import me.bestem0r.villagermarket.command.ISubCommand;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.List;

public class ReloadCommand implements ISubCommand {

    private final VMPlugin plugin;
    private CommandModule module;

    public ReloadCommand(VMPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public List<String> getCompletion(int index, String[] args) {
        return new ArrayList<>();
    }

    @Override
    public void run(CommandSender sender, String[] args) {
        sender.sendMessage(ConfigManager.getMessage("messages.reloaded"));

        Bukkit.getScheduler().runTaskAsynchronously(plugin, plugin::saveLog);

        plugin.reloadConfiguration();
        plugin.getShopManager().reloadAll();

    }

    @Override
    public String getDescription() {
        return "Reload plugin: &6/vm reload";
    }

    @Override
    public boolean requirePermission() {
        return true;
    }
}
