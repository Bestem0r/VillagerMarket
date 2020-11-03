package bestem0r.villagermarket.events;

import bestem0r.villagermarket.VMPlugin;
import bestem0r.villagermarket.shops.AdminShop;
import bestem0r.villagermarket.shops.PlayerShop;
import bestem0r.villagermarket.shops.ShopMenu;
import bestem0r.villagermarket.shops.VillagerShop;
import bestem0r.villagermarket.utilities.Color;
import bestem0r.villagermarket.utilities.Methods;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;

public class PlayerEvents implements Listener {

    @EventHandler (ignoreCancelled = true, priority = EventPriority.HIGH)
    public void playerRightClick(PlayerInteractEntityEvent event) {
        Player player = event.getPlayer();
        VillagerShop villagerShop = Methods.shopFromUUID(event.getRightClicked().getUniqueId());
        if (villagerShop != null) {
            event.setCancelled(true);
            if(event.getHand() == EquipmentSlot.OFF_HAND) { return; }

            if (villagerShop instanceof AdminShop) {
                if (player.hasPermission("villagermarket.admin")) {
                    villagerShop.openInventory(player, ShopMenu.EDIT_SHOP);
                } else {
                    villagerShop.openInventory(player, ShopMenu.SHOPFRONT);
                }
            } else {
                PlayerShop playerShop = (PlayerShop) villagerShop;
                if (villagerShop.getOwnerUUID().equals("null")) {
                    villagerShop.openInventory(player, ShopMenu.BUY_SHOP);
                } else if (villagerShop.getOwnerUUID().equals(player.getUniqueId().toString()) || playerShop.isTrusted(player)) {
                    villagerShop.openInventory(player, ShopMenu.EDIT_SHOP);
                } else {
                    villagerShop.openInventory(player, ShopMenu.SHOPFRONT);
                }
            }
            player.playSound(player.getLocation(), Sound.valueOf(VMPlugin.getInstance().getConfig().getString("sounds.open_shop")), 0.5f, 1);
        }
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        for (VillagerShop villagerShop : VMPlugin.shops) {
            Entity entity = Bukkit.getEntity(UUID.fromString(villagerShop.getEntityUUID()));
            if (entity == null) continue;
            if (entity.getNearbyEntities(5, 5, 5).contains(player)) {
                entity.teleport(entity.getLocation().setDirection(player.getLocation().subtract(entity.getLocation()).toVector()));
            }
        }
    }

    @EventHandler
    public void onItemClick(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack itemStack = player.getInventory().getItemInMainHand();
        if (itemStack.getItemMeta() == null) return;

        ItemMeta itemMeta = itemStack.getItemMeta();
        PersistentDataContainer dataContainer = itemMeta.getPersistentDataContainer();
        if (event.getAction() != Action.LEFT_CLICK_BLOCK && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        if (dataContainer.has(new NamespacedKey(VMPlugin.getInstance(), "vm-item"), PersistentDataType.STRING)) {
            event.setCancelled(true);
            //Check if player does not have access to the region
            RegionManager rm = WorldGuard.getInstance().getPlatform().getRegionContainer().get(BukkitAdapter.adapt(player.getWorld()));
            ApplicableRegionSet set = rm.getApplicableRegions(BukkitAdapter.asBlockVector(player.getLocation()));
            UUID uuid = player.getUniqueId();
            for (ProtectedRegion region : set) {
                if (!region.getOwners().getUniqueIds().contains(uuid) && !region.getMembers().getUniqueIds().contains(uuid)) {
                    player.sendMessage(new Color.Builder().path("messages.region_no_access").addPrefix().build());
                    return;
                }
            }

            String data = dataContainer.get(new NamespacedKey(VMPlugin.getInstance(), "vm-item"), PersistentDataType.STRING);
            int shopSize = Integer.parseInt(data.split("-")[0]);
            int storageSize = Integer.parseInt(data.split("-")[1]);

            UUID villagerUUID = Methods.spawnShop(player.getLocation(), "player", storageSize, shopSize, -1, "infinite");
            PlayerShop playerShop = (PlayerShop) Methods.shopFromUUID(villagerUUID);
            playerShop.setOwner(player);

            player.playSound(event.getClickedBlock().getLocation().subtract(0.5, 0, 0.5), Sound.valueOf(VMPlugin.getInstance().getConfig().getString("sounds.create_shop")), 1, 1);
            itemStack.setAmount(itemStack.getAmount() - 1);
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        if (!VMPlugin.abandonOffline.containsKey(event.getPlayer())) return;

        Player player = event.getPlayer();
        ArrayList<ItemStack> storage = VMPlugin.abandonOffline.get(event.getPlayer());

        for (ItemStack storageStack : storage) {
            if (storageStack != null) {
                if (storage.indexOf(storageStack) == storage.size() - 1) continue;
                HashMap<Integer, ItemStack> exceed = player.getInventory().addItem(storageStack);
                for (Integer i : exceed.keySet()) {
                    player.getLocation().getWorld().dropItemNaturally(player.getLocation(), exceed.get(i));
                }
            }
        }
        VMPlugin.abandonOffline.remove(event.getPlayer());
    }
}
