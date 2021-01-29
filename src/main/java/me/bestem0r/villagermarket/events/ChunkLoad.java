package me.bestem0r.villagermarket.events;

import me.bestem0r.villagermarket.EntityInfo;
import me.bestem0r.villagermarket.VMPlugin;
import me.bestem0r.villagermarket.shops.VillagerShop;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;

public class ChunkLoad implements Listener {

    private final VMPlugin plugin;

    public ChunkLoad(VMPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {
        plugin.checkChunk(event.getChunk());
    }
}
