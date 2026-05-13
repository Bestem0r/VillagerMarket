package net.bestemor.villagermarket.command.subcommand;

import net.bestemor.core.command.ISubCommand;
import net.bestemor.core.config.ConfigManager;
import net.bestemor.villagermarket.VMPlugin;
import net.bestemor.villagermarket.shop.VillagerShop;
import net.bestemor.villagermarket.utils.VMUtils;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class SpawnCommand implements ISubCommand {

    private final VMPlugin plugin;

    public SpawnCommand(VMPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public List<String> getCompletion(String[] args) {
        if (args.length == 2) {
            List<String> completions = new ArrayList<>();
            completions.add("all");
            
            // add UUIDs for specific shops
            for (VillagerShop shop : plugin.getShopManager().getShops()) {
                Entity entity = VMUtils.getEntity(shop.getEntityUUID());
                if (entity == null) { // Only suggest shops that need respawning
                    String shortUuid = shop.getEntityUUID().toString().substring(0, 8);
                    completions.add(shortUuid);
                }
            }

            String input = args[1].toLowerCase();
            if (!input.isEmpty()) {
                completions.removeIf(completion -> !completion.toLowerCase().startsWith(input));
            }
            
            return completions;
        }
        return new ArrayList<>();
    }

    @Override
    public void run(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ConfigManager.getMessage("messages.player_only"));
            return;
        }

        if (args.length < 2) {
            player.sendMessage(ConfigManager.getMessage("messages.spawn_usage"));
            return;
        }

        String identifier = args[1];
        
        if (identifier.equalsIgnoreCase("all")) {
            spawnAllMissingVillagers(player);
        } else {
            spawnSpecificVillager(player, identifier);
        }
    }

    private void spawnAllMissingVillagers(Player player) {
        // create copy of the shops to avoid ConcurrentModificationException
        List<VillagerShop> shopsCopy = new ArrayList<>(plugin.getShopManager().getShops());
        int spawned = 0;
        int failed = 0;
        int alreadyExists = 0;
        
        for (VillagerShop shop : shopsCopy) {
            Entity entity = VMUtils.getEntity(shop.getEntityUUID());
            if (entity == null) { // Villager is missing
                Location location = shop.getEntityInfo().getLocation();
                
                if (location != null && location.getWorld() != null) {
                    try {
                        Entity newEntity = plugin.getShopManager().spawnShop(location, "player");
                        
                        if (newEntity != null) {
                            shop.setUUID(newEntity.getUniqueId());
                            
                            // set correct shop name instead "Available shop!"
                            String shopName = shop.getShopName();
                            if (shopName != null && !shopName.isEmpty()) {
                                shop.setShopName(shopName);
                            }
                            
                            spawned++;
                        } else {
                            failed++;
                        }
                    } catch (Exception e) {
                        failed++;
                    }
                } else {
                    failed++;
                }
            } else {
                alreadyExists++;
            }
        }
        
        if (spawned > 0) {
            String plural = spawned == 1 ? "" : "s";
            player.sendMessage(ConfigManager.getMessage("messages.spawn_villagers_spawned")
                .replace("%count%", String.valueOf(spawned))
                .replace("%plural%", plural));
        }
        if (failed > 0) {
            String plural = failed == 1 ? "" : "s";
            player.sendMessage(ConfigManager.getMessage("messages.spawn_villagers_failed")
                .replace("%count%", String.valueOf(failed))
                .replace("%plural%", plural));
        }
        if (spawned == 0 && failed == 0) {
            player.sendMessage(ConfigManager.getMessage("messages.spawn_all_exist"));
        }
    }

    private void spawnSpecificVillager(Player player, String identifier) {
        VillagerShop targetShop = null;
        
        player.sendMessage(ConfigManager.getMessage("messages.spawn_searching").replace("%id%", identifier));
        
        // find shop by UUID
        for (VillagerShop shop : plugin.getShopManager().getShops()) {
            String uuid = shop.getEntityUUID().toString();
            if (uuid.toLowerCase().startsWith(identifier.toLowerCase()) || uuid.equalsIgnoreCase(identifier)) {
                targetShop = shop;
                player.sendMessage(ConfigManager.getMessage("messages.spawn_found_shop").replace("%id%", uuid.substring(0, 8)));
                break;
            }
        }
        
        if (targetShop == null) {
            player.sendMessage(ConfigManager.getMessage("messages.spawn_not_found"));
            return;
        }
        
        Entity entity = VMUtils.getEntity(targetShop.getEntityUUID());
        if (entity != null) {
            player.sendMessage(ConfigManager.getMessage("messages.spawn_already_exists"));
            return;
        }
        
        Location location = targetShop.getEntityInfo().getLocation();
        if (location == null || location.getWorld() == null) {
            player.sendMessage(ConfigManager.getMessage("messages.spawn_invalid_location"));
            return;
        }
        
        String locationStr = location.getBlockX() + ", " + location.getBlockY() + ", " + location.getBlockZ();
        player.sendMessage(ConfigManager.getMessage("messages.spawn_attempting").replace("%location%", locationStr));
        
        try {
            Entity newEntity = plugin.getShopManager().spawnShop(location, "player");
            
            if (newEntity != null) {
                // update the shop with the new entity UUID
                targetShop.setUUID(newEntity.getUniqueId());
                
                // set correct shop name instead "Available shop!"
                String shopName = targetShop.getShopName();
                if (shopName != null && !shopName.isEmpty()) {
                    targetShop.setShopName(shopName);
                }
                
                String displayName = shopName != null ? shopName : "Villager Shop";
                player.sendMessage(ConfigManager.getMessage("messages.spawn_success").replace("%shop%", displayName));
            } else {
                player.sendMessage(ConfigManager.getMessage("messages.spawn_failed_null"));
            }
        } catch (Exception e) {
            player.sendMessage(ConfigManager.getMessage("messages.spawn_failed_error").replace("%error%", e.getMessage()));
            plugin.getLogger().warning(e.toString());
        }
    }

    @Override
    public String getDescription() {
        return "Spawn missing villagers";
    }

    @Override
    public String getUsage() {
        return "<all|id>";
    }

}