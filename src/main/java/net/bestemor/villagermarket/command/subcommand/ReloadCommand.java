package net.bestemor.villagermarket.command.subcommand;

import net.bestemor.villagermarket.ConfigManager;
import net.bestemor.villagermarket.VMPlugin;
import net.bestemor.villagermarket.command.CommandModule;
import net.bestemor.villagermarket.command.ISubCommand;
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
