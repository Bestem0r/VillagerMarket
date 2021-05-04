package me.bestem0r.villagermarket.events;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import me.bestem0r.villagermarket.UpdateChecker;
import me.bestem0r.villagermarket.VMPlugin;
import me.bestem0r.villagermarket.inventories.Shopfront;
import me.bestem0r.villagermarket.shops.AdminShop;
import me.bestem0r.villagermarket.shops.PlayerShop;
import me.bestem0r.villagermarket.shops.ShopMenu;
import me.bestem0r.villagermarket.shops.VillagerShop;
import me.bestem0r.villagermarket.utilities.ColorBuilder;
import me.bestem0r.villagermarket.utilities.Methods;
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
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public class PlayerEvents implements Listener {

    private final VMPlugin plugin;

    public PlayerEvents(VMPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler (ignoreCancelled = true, priority = EventPriority.NORMAL)
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
                    if (plugin.getConfig().getBoolean("per_adminshop_permissions") && !player.hasPermission("villagermarket.adminshop." + villagerShop.getEntityUUID())) {
                        player.sendMessage(new ColorBuilder(plugin).path("messages.no_permission_adminshop").addPrefix().build());
                        return;
                    }
                    villagerShop.getShopfrontHolder().open(player, Shopfront.Type.CUSTOMER);
                }
            } else {
                PlayerShop playerShop = (PlayerShop) villagerShop;
                if (villagerShop.getOwnerUUID().equals("null")) {
                    villagerShop.openInventory(player, ShopMenu.BUY_SHOP);
                } else if (villagerShop.getOwnerUUID().equals(player.getUniqueId().toString()) || playerShop.isTrusted(player)) {
                    villagerShop.openInventory(player, ShopMenu.EDIT_SHOP);
                } else {
                    villagerShop.getShopfrontHolder().open(player, Shopfront.Type.CUSTOMER);
                }
            }
            player.playSound(player.getLocation(), Sound.valueOf(plugin.getConfig().getString("sounds.open_shop")), 0.5f, 1);
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

        if (dataContainer.has(new NamespacedKey(plugin, "vm-item"), PersistentDataType.STRING)) {
            event.setCancelled(true);
            //Check if player does not have access to the region

            if (Bukkit.getPluginManager().isPluginEnabled("WorldGuard") && plugin.getConfig().getBoolean("world_guard")) {
                RegionManager rm = WorldGuard.getInstance().getPlatform().getRegionContainer().get(BukkitAdapter.adapt(player.getWorld()));
                ApplicableRegionSet set = rm.getApplicableRegions(BukkitAdapter.asBlockVector(player.getLocation()));
                UUID uuid = player.getUniqueId();
                for (ProtectedRegion region : set) {
                    if (!region.getOwners().getUniqueIds().contains(uuid) && !region.getMembers().getUniqueIds().contains(uuid)) {
                        player.sendMessage(new ColorBuilder(plugin).path("messages.region_no_access").addPrefix().build());
                        return;
                    }
                }
            }

            String data = dataContainer.get(new NamespacedKey(plugin, "vm-item"), PersistentDataType.STRING);
            int shopSize = Integer.parseInt(data.split("-")[0]);
            int storageSize = Integer.parseInt(data.split("-")[1]);

            UUID villagerUUID = Methods.spawnShop(plugin, player.getLocation(), "player");
            if (Bukkit.getEntity(villagerUUID) != null) {
                Methods.newShopConfig(plugin, villagerUUID, storageSize, shopSize, -1, VillagerShop.VillagerType.PLAYER, "infinite");
                PlayerShop playerShop = (PlayerShop) Methods.shopFromUUID(villagerUUID);
                playerShop.setOwner(player);
            } else {
                Bukkit.getLogger().severe(ChatColor.RED + "Unable to spawn Villager! Does WorldGuard deny mobs pawn?");
            }

            player.playSound(event.getClickedBlock().getLocation().subtract(0.5, 0, 0.5), Sound.valueOf(plugin.getConfig().getString("sounds.create_shop")), 1, 1);
            itemStack.setAmount(itemStack.getAmount() - 1);
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {

        Player player = event.getPlayer();
        if (player.hasPermission("villagermarket.admin")) {
            new UpdateChecker(plugin, 82965).getVersion(version -> {
                String currentVersion = plugin.getDescription().getVersion();
                if (!currentVersion.equalsIgnoreCase(version)) {
                    String foundVersion = ChatColor.translateAlternateColorCodes('&', "&bA new version of VillagerMarket was found!");
                    String downloadVersion = ChatColor.translateAlternateColorCodes('&', "&bGet it here for the latest features and bug fixes: &ehttps://www.spigotmc.org/resources/villager-market.82965/");

                    player.sendMessage(new ColorBuilder(plugin).path("plugin_prefix").build() + " " + foundVersion);
                    player.sendMessage(new ColorBuilder(plugin).path("plugin_prefix").build() + " " + downloadVersion);
                }
            });
        }

        if (!VMPlugin.abandonOffline.containsKey(event.getPlayer())) return;

        List<ItemStack> storage = VMPlugin.abandonOffline.get(event.getPlayer());

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
