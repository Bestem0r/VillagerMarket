package me.bestem0r.villagermarket.command;

import org.bukkit.command.CommandSender;

import java.util.List;

public interface ISubCommand {

    List<String> getCompletion(int index, String[] args);

    void run(CommandSender sender, String[] args);

    String getDescription();

    boolean requirePermission();
}
