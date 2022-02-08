package me.bestem0r.villagermarket.command.subcommand;

import me.bestem0r.villagermarket.VMPlugin;
import me.bestem0r.villagermarket.command.ISubCommand;
import me.bestem0r.villagermarket.shop.EntityInfo;
import me.bestem0r.villagermarket.shop.VillagerShop;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

public class RegenCommand implements ISubCommand {

    private final VMPlugin plugin;

    public RegenCommand(VMPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public List<String> getCompletion(int index, String[] args) {
        return new ArrayList<>();
    }

    @Override
    public void run(CommandSender sender, String[] args) {

        List<Chunk> chunks = new ArrayList<>();
        for (World world : Bukkit.getWorlds()) {
            chunks.addAll(Arrays.stream(world.getLoadedChunks()).collect(Collectors.toList()));
        }
        sender.sendMessage("§eChecking " + chunks.size() + " chunks...");
        VMPlugin.log.add(new Date() + ": Checking " + chunks.size() + " chunks...");

        List<EntityInfo> regenList = new ArrayList<>();
        for (Chunk chunk : chunks) {
            regenList.addAll(checkChunk(sender, chunk));
        }



        VMPlugin.log.add(new Date() + ": Regenerating " + regenList.size() + " shops...");
        sender.sendMessage("§eRegenerating " + regenList.size() + " shops...");
        regenList.forEach(EntityInfo::recreate);
        VMPlugin.log.add(new Date() + ": Regen done!");
        sender.sendMessage("§aRegen done! Complete log saved in log file");
    }

    @Override
    public String getDescription() {
        return "Regenerate lost shops: &6/vm regen";
    }

    @Override
    public boolean requirePermission() {
        return true;
    }

    private List<EntityInfo> checkChunk(CommandSender sender, Chunk chunk) {
        List<EntityInfo> recreateList = new ArrayList<>();

        //Bukkit.getLogger().info("Checking chunk: " + chunk.getX() + ", " + chunk.getZ());
        //Bukkit.getLogger().info("Entities:" + chunk.getEntities().length);
        for (VillagerShop villagerShop : plugin.getShopManager().getShops()) {
            EntityInfo entityInfo = villagerShop.getEntityInfo();
            if (!entityInfo.hasStoredData()) {
                continue;
            }
            if (entityInfo.isInChunk(chunk)) {
                //Bukkit.getLogger().info("Checking chunk: " + chunk.getX() + "_" + chunk.getZ());
                if (entityInfo.exists()) {
                    //Bukkit.getLogger().info("Appending: " + entityInfo.getEntityUUID());
                    VMPlugin.log.add(new Date() + ": Appending existing information to " + entityInfo.getEntityUUID().toString());
                    VMPlugin.log.add("- Location: " + entityInfo.getLocation().toString());
                    entityInfo.appendToExisting();
                } else {
                    VMPlugin.log.add(new Date() + ": Loading " + entityInfo.getEntityUUID().toString() + " for regeneration");
                    VMPlugin.log.add("- Location: " + entityInfo.getLocation().toString());
                    recreateList.add(entityInfo);
                }
            }
        }
        return recreateList;
    }
}
