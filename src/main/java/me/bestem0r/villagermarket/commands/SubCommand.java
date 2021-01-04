package me.bestem0r.villagermarket.commands;

import org.bukkit.command.CommandSender;

import java.util.List;

public interface SubCommand {

    List<String> getCompletion(int index, String[] args);

    void run(CommandSender sender, String[] args);

    void setModule(CommandModule module);

    String getDescription();
}
