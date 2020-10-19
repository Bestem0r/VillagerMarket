package bestem0r.villagermarket.events;

import bestem0r.villagermarket.VMPlugin;
import bestem0r.villagermarket.shops.AdminShop;
import bestem0r.villagermarket.shops.PlayerShop;
import bestem0r.villagermarket.shops.ShopMenu;
import bestem0r.villagermarket.shops.VillagerShop;
import bestem0r.villagermarket.utilities.Color;
import bestem0r.villagermarket.utilities.Methods;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;

public class PlayerEvents implements Listener {

    @EventHandler (ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void playerRightClick(PlayerInteractEntityEvent event) {
        Player player = event.getPlayer();
        VillagerShop villagerShop = Methods.shopFromUUID(event.getRightClicked().getUniqueId());
        if (villagerShop != null) {
            event.setCancelled(true);
            if(event.getHand() == EquipmentSlot.OFF_HAND) { return; }

            Inventory inventory;

            if (villagerShop instanceof AdminShop) {
                if (player.hasPermission("villagermarket.admin")) {
                    inventory = villagerShop.getInventory(ShopMenu.EDIT_SHOP);
                } else {
                    inventory = villagerShop.getInventory(ShopMenu.SHOPFRONT);
                }
            } else {
                if (villagerShop.getOwnerUUID().equals("null")) {
                    inventory = villagerShop.getInventory(ShopMenu.BUY_SHOP);
                } else if (villagerShop.getOwnerUUID().equals(player.getUniqueId().toString())) {
                    inventory = villagerShop.getInventory(ShopMenu.EDIT_SHOP);
                } else {
                    inventory = villagerShop.getInventory(ShopMenu.SHOPFRONT);
                }
            }
            VMPlugin.clickMap.put(player, villagerShop);

            player.openInventory(inventory);
            player.playSound(player.getLocation(), Sound.valueOf(VMPlugin.getInstance().getConfig().getString("sounds.open_shop")), 0.5f, 1);
        }
    }

    @EventHandler
    public void onCloseInventory(InventoryCloseEvent event) {
        Player player = (Player) event.getPlayer();

        if (!VMPlugin.clickMap.containsKey(player)) { return; }
        VillagerShop villagerShop = VMPlugin.clickMap.get(player);

        String title = ChatColor.stripColor(event.getView().getTitle());
        if (title.equalsIgnoreCase(ChatColor.stripColor(new Color.Builder().path("menus.edit_storage.title").build()))) {
            villagerShop.updateShopInventories();
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
        PersistentDataContainer container = itemMeta.getPersistentDataContainer();
        if (event.getAction() != Action.LEFT_CLICK_BLOCK && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        if (container.has(new NamespacedKey(VMPlugin.getInstance(), "vm-item"), PersistentDataType.STRING)) {
            String data = container.get(new NamespacedKey(VMPlugin.getInstance(), "vm-item"), PersistentDataType.STRING);
            int shopSize = Integer.parseInt(data.split("-")[0]);
            int storageSize = Integer.parseInt(data.split("-")[1]);

            UUID villagerUUID = Methods.spawnShop(player.getLocation(), "player", storageSize, shopSize, -1, "infinite");
            PlayerShop playerShop = (PlayerShop) Methods.shopFromUUID(villagerUUID);
            playerShop.setOwner(player);

            player.playSound(event.getClickedBlock().getLocation().subtract(0.5, 0, 0.5), Sound.valueOf(VMPlugin.getInstance().getConfig().getString("sounds.create_shop")), 1, 1);
            itemStack.setAmount(itemStack.getAmount() - 1);
            event.setCancelled(true);
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
