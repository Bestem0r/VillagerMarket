package net.bestemor.villagermarket.listener;

import net.bestemor.core.config.ConfigManager;
import net.bestemor.core.config.VersionUtils;
import net.bestemor.villagermarket.VMPlugin;
import net.bestemor.villagermarket.shop.VillagerShop;
import net.bestemor.villagermarket.utils.VMUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

public class ChatListener implements Listener {

    private final VMPlugin plugin;
    private final Map<UUID, Consumer<String>> stringListeners = new HashMap<>();
    private final Map<UUID, Consumer<BigDecimal>> decimalListeners = new HashMap<>();

    private final String cancelInput;

    public ChatListener(VMPlugin plugin) {
        this.plugin = plugin;
        this.cancelInput = ConfigManager.getString("cancel");
    }

    @EventHandler (priority = EventPriority.LOWEST)
    public void onChat(AsyncPlayerChatEvent event) {

        UUID uuid = event.getPlayer().getUniqueId();
        String message = ChatColor.stripColor(event.getMessage()).replace("§", "").replace("&", "");
        //Bukkit.getLogger().info("|" + message + "|");
        Player player = event.getPlayer();

        if (stringListeners.containsKey(uuid) || decimalListeners.containsKey(uuid)) {
            event.setCancelled(true);

            if (message.equalsIgnoreCase(cancelInput)) {
                player.sendMessage(ConfigManager.getMessage("messages.cancelled"));
                decimalListeners.remove(uuid);
                stringListeners.remove(uuid);

            } else if (stringListeners.containsKey(uuid)) {
                Consumer<String> consumer = stringListeners.get(uuid);
                stringListeners.remove(uuid);
                Bukkit.getScheduler().runTask(plugin, () -> consumer.accept(event.getMessage()));

            } else if (decimalListeners.containsKey(uuid)) {
                if (!isNumeric(message)) {
                    player.sendMessage(VMUtils.hasComma(message) ? ConfigManager.getMessage("messages.use_dot") : ConfigManager.getMessage("messages.not_number"));
                } else if (Double.parseDouble(message) < 0) {
                    player.sendMessage(ConfigManager.getMessage("messages.negative_price"));
                } else {
                    Consumer<BigDecimal> consumer = decimalListeners.get(uuid);
                    decimalListeners.remove(uuid);
                    Bukkit.getScheduler().runTask(plugin, () -> consumer.accept(new BigDecimal(message)));
                }
            }

        }
    }

    @EventHandler
    public void onDrop(PlayerDropItemEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        if (stringListeners.containsKey(uuid) || decimalListeners.containsKey(uuid)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onLeave(PlayerQuitEvent event) {
        stringListeners.remove(event.getPlayer().getUniqueId());
    }

    @EventHandler (ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onEntityInteract(PlayerInteractEntityEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();

        if (VersionUtils.getMCVersion() >= 9 && event.getHand() == EquipmentSlot.OFF_HAND) { return; }

        if (!stringListeners.containsKey(uuid) && !decimalListeners.containsKey(uuid)) { return; }

        VillagerShop shop = plugin.getShopManager().getShop(event.getRightClicked().getUniqueId());
        if (shop != null) {
            event.getPlayer().sendMessage(ConfigManager.getMessage("messages.finish_process"));
            event.setCancelled(true);
        }
    }

    public void addStringListener(Player player, Consumer<String> result) {
        player.sendMessage(ConfigManager.getMessage("messages.type_cancel").replace("%cancel%", cancelInput));
        stringListeners.put(player.getUniqueId(), result);
    }

    public void addDecimalListener(Player player, Consumer<BigDecimal> result) {
        player.sendMessage(ConfigManager.getMessage("messages.type_cancel").replace("%cancel%", cancelInput));
        decimalListeners.put(player.getUniqueId(), result);
    }

    private boolean isNumeric(String strNum) {
        if (strNum == null) {
            return false;
        }
        try {
            double d = Double.parseDouble(strNum);
        } catch (NumberFormatException nfe) {
            return false;
        }
        return true;
    }
}
