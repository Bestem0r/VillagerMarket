package net.bestemor.villagermarket.listener;

import net.bestemor.villagermarket.ConfigManager;
import net.bestemor.villagermarket.VMPlugin;
import net.bestemor.villagermarket.shop.EntityInfo;
import net.bestemor.villagermarket.shop.VillagerShop;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.world.ChunkLoadEvent;

import java.util.ArrayList;
import java.util.List;

public class ChunkLoadListener implements Listener {

    private final VMPlugin plugin;
    private final boolean regenVillagers;

    public ChunkLoadListener(VMPlugin plugin) {
        this.plugin = plugin;

        this.regenVillagers = ConfigManager.getBoolean("villager_regen");
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        if (regenVillagers) {
            checkChunk(event.getPlayer().getLocation().getChunk());
        }
    }

    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {
        if (regenVillagers && Bukkit.getOnlinePlayers().size() > 0) {
            checkChunk(event.getChunk());
        }
    }

    private void checkChunk(Chunk chunk) {
        List<EntityInfo> recreateList = new ArrayList<>();

        Bukkit.getLogger().info("Checking chunk: " + chunk.getX() + ", " + chunk.getZ());
        Bukkit.getLogger().info("Entities:" + chunk.getEntities().length);
        for (VillagerShop villagerShop : plugin.getShopManager().getShops()) {
            EntityInfo entityInfo = villagerShop.getEntityInfo();
            if (!entityInfo.hasStoredData()) {
                continue;
            }
            if (entityInfo.isInChunk(chunk)) {
                //Bukkit.getLogger().info("Checking chunk: " + chunk.getX() + "_" + chunk.getZ());
                if (entityInfo.exists()) {
                    //Bukkit.getLogger().info("Appending: " + entityInfo.getEntityUUID());
                    entityInfo.appendToExisting();
                } else {
                    recreateList.add(entityInfo);
                }
            }
        }
        recreateList.forEach(EntityInfo::recreate);
    }
}
