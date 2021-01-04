package me.bestem0r.villagermarket.commands.subcommands;

import me.bestem0r.villagermarket.VMPlugin;
import me.bestem0r.villagermarket.commands.CommandModule;
import me.bestem0r.villagermarket.commands.SubCommand;
import me.bestem0r.villagermarket.shops.VillagerShop;
import me.bestem0r.villagermarket.utilities.ColorBuilder;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.List;

public class ReloadCommand implements SubCommand {

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
        module.commandOutput(sender, new ColorBuilder(plugin).path("messages.reloaded").addPrefix().build());
        plugin.reload();
        plugin.saveLog();
        plugin.shops.forEach((VillagerShop::reload));
    }

    @Override
    public void setModule(CommandModule module) {
        this.module = module;
    }

    @Override
    public String getDescription() {
        return "Reload plugin: &6/vm reload";
    }
}
