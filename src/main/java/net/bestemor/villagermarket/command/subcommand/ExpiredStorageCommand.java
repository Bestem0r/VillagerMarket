package net.bestemor.villagermarket.command.subcommand;

import net.bestemor.core.command.ISubCommand;
import net.bestemor.core.config.ConfigManager;
import net.bestemor.villagermarket.VMPlugin;
import net.bestemor.villagermarket.menu.StorageHolder;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

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
        if (sender instanceof Player) {
            Player player = (Player) sender;

            if (plugin.getShopManager().getExpiredStorages().containsKey(player.getUniqueId())) {
                final StorageHolder holder = new StorageHolder(plugin, 0);
                holder.loadItems(plugin.getShopManager().getExpiredStorages().get(player.getUniqueId()));

                holder.setCloseEvent(() -> {
                    List<ItemStack> items = holder.getItems();
                    if (items.isEmpty()) {
                        plugin.getShopManager().getExpiredStorages().remove(player.getUniqueId());
                    } else {
                        plugin.getShopManager().getExpiredStorages().put(player.getUniqueId(), items);
                    }
                });

                holder.open(player);

            } else {
                player.sendMessage(ConfigManager.getMessage("messages.no_expired_storage"));
            }
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
