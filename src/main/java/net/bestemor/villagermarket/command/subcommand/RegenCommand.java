package net.bestemor.villagermarket.command.subcommand;

import net.bestemor.core.command.ISubCommand;
import net.bestemor.villagermarket.VMPlugin;
import net.bestemor.villagermarket.shop.EntityInfo;
import net.bestemor.villagermarket.shop.VillagerShop;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.command.CommandSender;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class RegenCommand implements ISubCommand {

    private final VMPlugin plugin;

    private int chunksToCheck = 0;
    private final Map<String, Integer> shopsToCheck = new HashMap<>();

    public RegenCommand(VMPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public List<String> getCompletion(String[] args) {
        return new ArrayList<>();
    }

    @Override
    public void run(CommandSender sender, String[] args) {

        if (chunksToCheck > 0) {
            sender.sendMessage("§cShop regeneration is currently ongoing...");
            return;
        }

        List<Chunk> chunks = new ArrayList<>();
        for (World world : Bukkit.getWorlds()) {
            chunks.addAll(Arrays.stream(world.getLoadedChunks()).collect(Collectors.toList()));
        }
        sender.sendMessage("§eChecking " + chunks.size() + " chunks...");
        VMPlugin.log.add(new Date() + ": Checking " + chunks.size() + " chunks...");
        this.chunksToCheck = chunks.size();

        List<EntityInfo> regenList = new ArrayList<>();
        for (Chunk c : chunks) {

            checkChunk(c, (regen) -> {
                regenList.addAll(regen);
                chunksToCheck--;

                if (chunksToCheck == 0) {

                    VMPlugin.log.add(new Date() + ": Regenerating " + regenList.size() + " shops...");
                    sender.sendMessage("§eRegenerating " + regenList.size() + " shops...");
                    regenList.forEach(EntityInfo::recreate);
                    VMPlugin.log.add(new Date() + ": Regen done!");
                    sender.sendMessage("§aRegen done! Complete log saved in log file");
                }
            });
        }
    }

    @Override
    public String getDescription() {
        return "Regenerate lost shops";
    }

    @Override
    public String getUsage() {
        return "";
    }

    @Override
    public boolean requirePermission() {
        return true;
    }

    private void checkChunk(Chunk chunk, Consumer<List<EntityInfo>> recreate) {
        List<EntityInfo> recreateList = new ArrayList<>();

        int delay = 0;
        final String chunkID = chunk.getWorld().getName() + "_" + chunk.getX() + "_" + chunk.getZ();
        this.shopsToCheck.put(chunkID, plugin.getShopManager().getShops().size());

        for (VillagerShop villagerShop : plugin.getShopManager().getShops()) {
            EntityInfo entityInfo = villagerShop.getEntityInfo();

            if (!entityInfo.hasStoredData() || !entityInfo.isInChunk(chunk)) {
                this.shopsToCheck.put(chunkID, this.shopsToCheck.get(chunkID) - 1);
                if (shopsToCheck.get(chunkID) == 0) {
                    recreate.accept(recreateList);
                    shopsToCheck.remove(chunkID);
                }
                continue;
            }
            //Bukkit.getLogger().info("Checking chunk: " + chunk.getX() + "_" + chunk.getZ());
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
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
                this.shopsToCheck.put(chunkID, this.shopsToCheck.get(chunkID) - 1);

                if (shopsToCheck.get(chunkID) == 0) {
                    recreate.accept(recreateList);
                    shopsToCheck.remove(chunkID);
                }
            }, delay);

            delay ++;
        }
    }
}
