package net.bestemor.villagermarket.shop;

import net.bestemor.villagermarket.ConfigManager;
import net.bestemor.villagermarket.VMPlugin;
import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

public class ShopManager {

    private final VMPlugin plugin;
    private final Map<UUID, VillagerShop> shops = new HashMap<>();
    private final List<String> blackList;

    private boolean redstoneEnabled;

    private final HashMap<UUID, List<ItemStack>> expiredStorages = new HashMap<>();

    public ShopManager(VMPlugin plugin) {
        this.plugin = plugin;

        this.redstoneEnabled = ConfigManager.getBoolean("enable_redstone_output");
        this.blackList = ConfigManager.getStringList("item_blacklist");

        beginSaveThread();
        beginRedstoneThread();
        beginExpireThread();
    }

    public void load() {

        this.shops.clear();
        File abandonFile = new File(plugin.getDataFolder() + "/abandon_offline.yml");
        YamlConfiguration abandonConfig = YamlConfiguration.loadConfiguration(abandonFile);
        ConfigurationSection section = abandonConfig.getConfigurationSection("abandon_offline");
        if (section != null) {
            for (String uuid : section.getKeys(false)) {
                expiredStorages.put(UUID.fromString(uuid), (List<ItemStack>) abandonConfig.getList("abandon_offline." + uuid));
            }
        }

        long before = Instant.now().toEpochMilli();
        File shopsFile = new File(Bukkit.getServer().getPluginManager().getPlugin("VillagerMarket").getDataFolder() + "/Shops/");
        if (shopsFile.exists() && shopsFile.isDirectory()) {
            for (File file : shopsFile.listFiles()) {
                loadShop(file);
            }
        }
        long after = Instant.now().toEpochMilli();
        Bukkit.getLogger().info("[VillagerMarket] Loaded " + shops.size() + " shops in " + (after - before) + " ms!");
    }

    public VillagerShop getShop(UUID uuid) {
        return shops.get(uuid);
    }

    public void changeID(UUID oldID, UUID newID) {
        shops.put(newID, shops.get(oldID));
        shops.remove(oldID);
    }

    /** Spawns new Villager Entity and sets its attributes to default values */
    public Entity spawnShop(Location location, String type) {
        Villager villager = (Villager) location.getWorld().spawnEntity(location, EntityType.VILLAGER);

        villager.setInvulnerable(!plugin.getConfig().getBoolean("villager.killable"));
        //Bukkit.getLogger().info("Killable: " + villager.isInvulnerable());
        villager.setCollidable(false);
        villager.setSilent(true);
        villager.setAI(plugin.getConfig().getBoolean("villager.ai"));
        villager.setProfession(Villager.Profession.valueOf(ConfigManager.getString("villager.default_profession")));
        String namePath = (type.equalsIgnoreCase("player") ? "name_available" : "name_admin");
        villager.setCustomName(ConfigManager.getString("villager." + namePath));
        villager.setCustomNameVisible(plugin.getConfig().getBoolean("villager.name_always_display"));

        return villager;
    }

    public void closeAllShopfronts() {
        for (VillagerShop shop : shops.values()) {
            shop.getShopfrontHolder().closeAll();
        }
    }

    public void saveAll() {
        File abandonFile = new File(plugin.getDataFolder() + "/abandon_offline.yml");
        YamlConfiguration abandonConfig = YamlConfiguration.loadConfiguration(abandonFile);

        abandonConfig.set("abandon_offline", null);
        for (UUID uuid : expiredStorages.keySet()) {
            abandonConfig.set("abandon_offline." + uuid.toString(), expiredStorages.get(uuid));
        }

        try {
            abandonConfig.save(abandonFile);
        } catch (IOException e) {
            e.printStackTrace();
        }

        shops.values().forEach(VillagerShop::save);
    }

    public void loadShop(File file) {
        String fileName = file.getName();
        UUID uuid = UUID.fromString(fileName.substring(0, fileName.length() - 4));
        FileConfiguration config = YamlConfiguration.loadConfiguration(file);

        String typeS = config.getString("type");
        typeS = (typeS == null ? "player" : typeS);

        try {
            VillagerShop.VillagerType type = VillagerShop.VillagerType.valueOf(typeS.toUpperCase(Locale.ROOT));
            switch (type) {
                case ADMIN:
                    shops.put(uuid, new AdminShop(plugin, file));
                    break;
                case PLAYER:
                    shops.put(uuid, new PlayerShop(plugin, file));
            }
        } catch (Exception e) {
            Bukkit.getLogger().severe("[VillagerMarket] " + file + " seems to be corrupt!");
            e.printStackTrace();
        }
    }

    public void removeShop(UUID uuid) {
        File file = new File(Objects.requireNonNull(Bukkit.getServer().getPluginManager().getPlugin("VillagerMarket")).getDataFolder() + "/Shops/", uuid.toString() + ".yml");
        file.delete();

        Entity entity = Bukkit.getEntity(uuid);
        if (plugin.isCitizensEnabled() && CitizensAPI.getNPCRegistry().isNPC(entity)) {
            NPC npc = CitizensAPI.getNPCRegistry().getNPC(entity);
            npc.destroy();
        } else if (entity != null) {
            entity.remove();
        }
        shops.remove(uuid);
    }

    public List<VillagerShop> getOwnedShops(Player player) {
        return shops.values().stream()
                .filter(PlayerShop.class::isInstance)
                .map(PlayerShop.class::cast)
                .filter(PlayerShop::hasOwner)
                .filter(s -> s.getOwnerUUID().equals(player.getUniqueId()))
                .collect(Collectors.toList());
    }

    public void reloadAll() {
        plugin.getMenuListener().closeAll();
        closeAllShopfronts();

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            saveAll();
            load();
        });
    }

    public boolean isShop(Entity entity) {
        return shops.containsKey(entity.getUniqueId());
    }
    public boolean isRedstoneEnabled() {
        return redstoneEnabled;
    }
    public HashMap<UUID, List<ItemStack>> getExpiredStorages() {
        return expiredStorages;
    }

    /** Thread runs save() method for all Villager Shops */
    private void beginSaveThread() {
        long interval = 20 * 60 * ConfigManager.getLong("auto_save_interval");
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            shops.values().forEach(VillagerShop::save);
        }, interval, interval);
    }

    public Collection<VillagerShop> getShops() {
        return shops.values();
    }

    /** Returns true if item is blacklisted, false if not */
    public boolean isBlackListed(Material material) {
        return blackList.contains(material.toString());
    }

    /** Thread check if rent time has expired and runs abandon() method */
    private void beginExpireThread() {
        long interval = 20 * ConfigManager.getLong("expire_check_interval");

        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            for (VillagerShop villagerShop : shops.values()) {
                if (villagerShop instanceof PlayerShop) {
                    PlayerShop playerShop = (PlayerShop) villagerShop;

                    if (playerShop.hasExpired() && playerShop.hasOwner()) {
                        Bukkit.getScheduler().runTask(plugin, playerShop::abandon);
                    }
                } else {
                    for (ShopItem item : villagerShop.getShopfrontHolder().getItemList().values()) {
                        if (item.getCooldown() != null && item.getNextReset() != null && Instant.now().isAfter(item.getNextReset())) {
                            item.clearLimits();
                        }
                    }
                }
            }
        }, 20L, interval);
    }

    public List<Entity> getEntities() {
        return shops.values().stream()
                .map(s -> Bukkit.getEntity(s.entityUUID))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }


    /** Thread updates redstone output for all Villagers */
    private void beginRedstoneThread() {
        long interval = 20 * ConfigManager.getLong("redstone_update_interval");

        Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, () -> {
            for (VillagerShop villagerShop : shops.values()) {
                if (villagerShop instanceof PlayerShop) {
                    PlayerShop playerShop = (PlayerShop) villagerShop;
                    playerShop.updateRedstone(false);
                }
            }
        }, 20L, interval);
    }

    /** Saves/Resets Villager Config with default values */
    public void createShopConfig(VMPlugin plugin, UUID entityUUID, int storageSize, int shopfrontSize, int cost, String type, String duration) {
        File file = new File(Bukkit.getServer().getPluginManager().getPlugin("VillagerMarket").getDataFolder() + "/Shops/", entityUUID + ".yml");
        FileConfiguration config = YamlConfiguration.loadConfiguration(file);


        config.set("storageSize", storageSize);
        config.set("shopfrontSize", shopfrontSize);
        config.set("type", type);
        config.set("duration", duration);
        config.set("cost", cost);

        try {
            config.save(file);
            plugin.getShopManager().loadShop(file);
        } catch (IOException i) {
            i.printStackTrace();
        }
    }

    /** Returns new Villager Shop Item */
    public ItemStack getShopItem(VMPlugin plugin, int shopSize, int storageSize, int amount) {

        int maxAmount = (Math.min(amount, 64));
        String infinite = ConfigManager.getString("quantity.infinite");
        String storageString = (storageSize == 0 ? infinite : String.valueOf(storageSize));
        String shopString = (shopSize == 0 ? infinite : String.valueOf(shopSize));

        ItemStack shopItem = ConfigManager.getItem("shop_item").replace("%shop_size%", shopString).replace("%storage_size%", storageString).build();

        ItemMeta shopMeta = shopItem.getItemMeta();
        shopMeta.getPersistentDataContainer().set(new NamespacedKey(plugin, "vm-item"), PersistentDataType.STRING, shopSize + "-" + storageSize);
        shopItem.setAmount(maxAmount);
        shopItem.setItemMeta(shopMeta);
        return shopItem;
    }

    public int getMaxShops(Player player) {
        if (player.hasPermission("villagermarket.unlimited_shops")) {
            return -1;
        }
        return player.getEffectivePermissions().stream()
                .filter((s) -> s.getPermission().startsWith("villagermarket.max_shops."))
                .filter((s) -> s.getPermission().length() > 25)
                .map((s) -> s.getPermission().substring(25))
                .mapToInt(Integer::parseInt)
                .max()
                .orElse(-1);
    }
}
