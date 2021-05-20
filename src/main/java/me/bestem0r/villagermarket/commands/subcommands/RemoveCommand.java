package me.bestem0r.villagermarket.commands.subcommands;

import me.bestem0r.villagermarket.VMPlugin;
import me.bestem0r.villagermarket.commands.CommandModule;
import me.bestem0r.villagermarket.commands.SubCommand;
import me.bestem0r.villagermarket.shops.VillagerShop;
import me.bestem0r.villagermarket.utilities.ColorBuilder;
import me.bestem0r.villagermarket.utilities.Methods;
import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.api.npc.NPCRegistry;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class RemoveCommand implements SubCommand {

    private final JavaPlugin plugin;

    public RemoveCommand(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public List<String> getCompletion(int index, String[] args) {
        return new ArrayList<>();
    }

    @Override
    public void run(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            return;
        }
        Player player = (Player) sender;
        player.sendMessage(new ColorBuilder(plugin).path("messages.remove_villager").addPrefix().build());
        Bukkit.getPluginManager().registerEvents(new RemoveEvent(player), plugin);
    }

    @Override
    public void setModule(CommandModule module) {

    }

    @Override
    public String getDescription() {
        return "Remove trusted: &6/vm trusted remove <player>";
    }

    private class RemoveEvent implements Listener {

        private final Player player;

        public RemoveEvent(Player player) {
            this.player = player;
        }

        @EventHandler(priority = EventPriority.LOWEST)
        public void onInteract(PlayerInteractEntityEvent event) {
            if (event.getPlayer() != player) return;
            event.setCancelled(true);
            VillagerShop villagerShop = Methods.shopFromUUID(event.getRightClicked().getUniqueId());
            if (villagerShop != null) {
                player.sendMessage(new ColorBuilder(plugin).path("messages.villager_removed").addPrefix().build());
                player.playSound(player.getLocation(), Sound.valueOf(plugin.getConfig().getString("sounds.remove_villager")), 0.5f, 1);

                File file = new File(Objects.requireNonNull(Bukkit.getServer().getPluginManager().getPlugin("VillagerMarket")).getDataFolder() + "/Shops/", event.getRightClicked().getUniqueId().toString() + ".yml");
                file.delete();

                VMPlugin.shops.remove(villagerShop);
                if (Bukkit.getPluginManager().isPluginEnabled("Citizens") && CitizensAPI.getNPCRegistry().isNPC(event.getRightClicked())) {
                    NPC npc = CitizensAPI.getNPCRegistry().getNPC(event.getRightClicked());
                    npc.destroy();
                } else {
                    event.getRightClicked().remove();
                }
            } else {
                player.sendMessage(new ColorBuilder(plugin).path("messages.no_villager_shop").addPrefix().build());
            }
            HandlerList.unregisterAll(this);
        }
    }
}
