package net.bestemor.villagermarket.command.subcommand;

import net.bestemor.core.command.ISubCommand;
import net.bestemor.core.config.ConfigManager;
import net.bestemor.villagermarket.VMPlugin;
import net.bestemor.villagermarket.menu.StorageHolder;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ExpiredStorageCommand implements ISubCommand {

    private final VMPlugin plugin;

    public ExpiredStorageCommand(VMPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public List<String> getCompletion(String[] args) {
        return new ArrayList<>();
    }

    @Override
    public void run(CommandSender sender, String[] args) {
        if (sender instanceof Player player) {

            UUID storageUUID = player.getUniqueId();
            if (args.length > 1) {
                if (!player.hasPermission("villagermarket.admin")) {
                    player.sendMessage(ConfigManager.getMessage("messages.no_permission_command"));
                    return;
                }

                OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
                if (!target.hasPlayedBefore()) {
                    player.sendMessage(ConfigManager.getMessage("messages.player_not_found"));
                    return;
                }
                storageUUID = target.getUniqueId();
            }

            player.closeInventory();
            final UUID finalStorageUUID = storageUUID;

            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (plugin.getShopManager().getExpiredStorages().containsKey(finalStorageUUID)) {
                    final StorageHolder holder = new StorageHolder(plugin, 0);
                    holder.setAddingAllowed(false);
                    holder.loadItems(plugin.getShopManager().getExpiredStorages().get(finalStorageUUID));

                    holder.setClickEvent(() -> Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        List<ItemStack> items = holder.getItems();
                        if (items.isEmpty()) {
                            plugin.getShopManager().getExpiredStorages().remove(finalStorageUUID);
                        } else {
                            plugin.getShopManager().getExpiredStorages().put(finalStorageUUID, items);
                        }
                    }, 1L));

                    holder.open(player);

                } else {
                    player.sendMessage(ConfigManager.getMessage("messages.no_expired_storage"));
                }
            }, 1L);
        }
    }

    @Override
    public String getDescription() {
        return "Open expired shop storage";
    }

    @Override
    public String getUsage() {
        return "";
    }

    @Override
    public boolean requirePermission() {
        return false;
    }
}
