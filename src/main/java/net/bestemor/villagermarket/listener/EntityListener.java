package net.bestemor.villagermarket.listener;

import de.tr7zw.nbtapi.NBTItem;
import net.bestemor.core.config.VersionUtils;
import net.bestemor.villagermarket.VMPlugin;
import net.bestemor.villagermarket.shop.ShopMenu;
import net.bestemor.villagermarket.shop.VillagerShop;
import net.bestemor.villagermarket.utils.VMUtils;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockDispenseEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.VillagerCareerChangeEvent;
import org.bukkit.event.weather.LightningStrikeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

public class EntityListener implements Listener {
    
    private final VMPlugin plugin;
    
    public EntityListener(VMPlugin plugin) {
        this.plugin = plugin;
        if (VersionUtils.getMCVersion() > 13) {
            Bukkit.getPluginManager().registerEvents(new Listener() {
                @EventHandler
                public void onCareerChange(VillagerCareerChangeEvent event) {
                    if (plugin.getShopManager().getShop(event.getEntity().getUniqueId()) != null) {
                        event.setCancelled(true);
                    }
                }
            }, plugin);
        }
    }
    
    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        Entity entity = event.getEntity();
        if (plugin.getShopManager().getShop(event.getEntity().getUniqueId()) != null) {
            plugin.getShopManager().removeShop(entity.getUniqueId());
        }
    }

    @EventHandler (priority = EventPriority.HIGH)
    public void onEntityDamage(EntityDamageEvent event) {
        if (plugin.getShopManager().getShop(event.getEntity().getUniqueId()) != null) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onHitEntity(EntityDamageByEntityEvent event) {
        if (plugin.getShopManager().getShop(event.getEntity().getUniqueId()) != null) {
            if (event.getDamager() instanceof Player player) {
                if (!player.isSneaking()) return;
                if (!player.hasPermission("villagermarket.spy")) return;
                VillagerShop villagerShop = plugin.getShopManager().getShop(event.getEntity().getUniqueId());
                villagerShop.updateMenu(ShopMenu.EDIT_SHOP);
                villagerShop.openInventory(player, ShopMenu.EDIT_SHOP);
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onLightningStrike(LightningStrikeEvent event) {
        for (Entity entity : event.getLightning().getNearbyEntities(4, 4, 4)) {
            if (plugin.getShopManager().getShop(entity.getUniqueId()) != null) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onDispense(BlockDispenseEvent event) {
        ItemStack itemStack = event.getItem();
        ItemMeta itemMeta = itemStack.getItemMeta();

        boolean isVMItem;

        String data = null;
        if (VersionUtils.getMCVersion() >= 14) {
            PersistentDataContainer dataContainer = itemMeta.getPersistentDataContainer();
            isVMItem = dataContainer.has(new NamespacedKey(plugin, "vm-item"), PersistentDataType.STRING);
            if (isVMItem) {
                data = dataContainer.get(new NamespacedKey(plugin, "vm-item"), PersistentDataType.STRING);
            }
        } else {
            NBTItem nbtItem = new NBTItem(itemStack);
            data = nbtItem.getString("vm-item");
            isVMItem = data != null && data.split("-").length > 0;
        }

        if (isVMItem && data != null && data.split("-").length > 0) {
            String[] split = data.split("-");
            isVMItem = VMUtils.isInteger(split[0]) && VMUtils.isInteger(split[1]);
        }
        if (isVMItem) {
            event.setCancelled(true);
        }
    }
}
