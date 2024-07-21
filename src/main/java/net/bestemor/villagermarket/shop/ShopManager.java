package net.bestemor.villagermarket.shop;

import de.tr7zw.nbtapi.NBTItem;
import net.bestemor.core.config.ConfigManager;
import net.bestemor.core.config.VersionUtils;
import net.bestemor.villagermarket.VMPlugin;
import net.bestemor.villagermarket.menu.Shopfront;
import net.bestemor.villagermarket.utils.VMUtils;
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
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

public class ShopManager {

    private final VMPlugin plugin;
    private final Map<UUID, VillagerShop> shops = new HashMap<>();
    private final List<String> blackList;

    private Instant nextAutoDiscount = Instant.now();

    private final HashMap<UUID, List<ItemStack>> expiredStorages = new HashMap<>();

    public ShopManager(VMPlugin plugin) {
        this.plugin = plugin;

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

        if (VersionUtils.getMCVersion() >= 14) {
            villager.setInvulnerable(!plugin.getConfig().getBoolean("villager.killable"));
            villager.setCollidable(false);
            villager.setRemoveWhenFarAway(false);
            villager.setSilent(true);
            villager.setAI(plugin.getConfig().getBoolean("villager.ai"));
        } else {
            villager.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, Integer.MAX_VALUE, 128, false, false));
        }
        villager.setMetadata("villagershop", new FixedMetadataValue(plugin, true));

        String defaultProfession = ConfigManager.getString("villager.default_profession");
        defaultProfession = VersionUtils.getMCVersion() < 14 && defaultProfession.equals("NONE") ? "FARMER" : defaultProfession;
        villager.setProfession(Villager.Profession.valueOf(defaultProfession));
        String namePath = (type.equalsIgnoreCase("player") ? "name_available" : "name_admin");
        villager.setCustomName(ConfigManager.getString("villager." + namePath));
        villager.setCustomNameVisible(plugin.getConfig().getBoolean("villager.name_always_display"));

        return villager;
    }

    public void openShop(Player p, VillagerShop shop, boolean enableEdit) {
        if (shop instanceof AdminShop) {
            if (p.hasPermission("villagermarket.adminshops") && enableEdit) {
                shop.openInventory(p, ShopMenu.EDIT_SHOP);
            } else {
                if (ConfigManager.getBoolean("per_adminshop_permissions") && !p.hasPermission("villagermarket.adminshop." + shop.getEntityUUID())) {
                    p.sendMessage(ConfigManager.getMessage("messages.no_permission_adminshop"));
                    return;
                }
                if (shop.isRequirePermission() && !p.hasPermission("villagermarket.adminshop." + shop.getEntityUUID())) {
                    p.sendMessage(ConfigManager.getMessage("messages.no_permission_adminshop"));
                    return;
                }
                shop.getShopfrontHolder().open(p, Shopfront.Type.CUSTOMER);
            }
        } else {
            PlayerShop playerShop = (PlayerShop) shop;
            boolean canEdit = playerShop.getOwnerUUID().equals(p.getUniqueId()) || playerShop.isTrusted(p) || (p.isSneaking() && p.hasPermission("villagermarket.spy"));
            if (!playerShop.hasOwner()) {
                if (shop.isRequirePermission() && !p.hasPermission("villagermarket.playershop." + shop.getEntityUUID())) {
                    p.sendMessage(ConfigManager.getMessage("messages.no_permission_playershop"));
                    return;
                }
                resetShopEntity(shop.getEntityUUID());
                shop.openInventory(p, ShopMenu.BUY_SHOP);
            } else if (canEdit && enableEdit) {
                shop.updateMenu(ShopMenu.EDIT_SHOP);
                shop.openInventory(p, ShopMenu.EDIT_SHOP);
            } else {
                shop.getShopfrontHolder().open(p, Shopfront.Type.CUSTOMER);
            }
        }

        p.playSound(p.getLocation(), ConfigManager.getSound("sounds.open_shop"), 0.5f, 1);
    }

    public void resetShopEntity(UUID uuid) {
        Entity entity = VMUtils.getEntity(uuid);
        if (entity != null) {
            if (plugin.isCitizensEnabled() && CitizensAPI.getNPCRegistry().isNPC(entity)) {
                CitizensAPI.getNPCRegistry().getNPC(entity).setName(ConfigManager.getString("villager.name_available"));
            } else if (entity instanceof Villager) {
                entity.setCustomName(ConfigManager.getString("villager.name_available"));
            }
            VillagerShop shop = shops.get(uuid);
            shop.setProfession(VersionUtils.getMCVersion() < 14 ? Villager.Profession.FARMER : Villager.Profession.NONE);
        }
    }

    public void closeAllShopfronts() {
        for (VillagerShop shop : shops.values()) {
            shop.getShopfrontHolder().closeAll();
        }
    }

    public void cloneShop(VillagerShop shop, Location location) {
        shop.save();
        String type = shop instanceof PlayerShop ? "player" : "admin";
        Entity entity = spawnShop(location, type);
        File file = createShopConfig(entity.getUniqueId(), shop.getStorageSize() / 9, shop.getShopSize() / 9, shop.getCost(), type.toUpperCase(), shop.getDuration());
        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        for (String key : shop.getConfig().getKeys(false)) {
            ConfigurationSection section = shop.getConfig().getConfigurationSection(key);
            if (section != null) {
                config.set(key, section);
            } else {
                config.set(key, shop.getConfig().get(key));
            }
        }
        try {
            config.save(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
        loadShop(file);
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

        Entity entity = VMUtils.getEntity(uuid);
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
        closeAllShopfronts();

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            saveAll();
            load();
        });
    }

    public boolean isShop(Entity entity) {
        return shops.containsKey(entity.getUniqueId());
    }
    public HashMap<UUID, List<ItemStack>> getExpiredStorages() {
        return expiredStorages;
    }

    /** Thread runs save() method for all Villager Shops */
    private void beginSaveThread() {
        long interval = 20 * 60L * Math.max(1, ConfigManager.getInt("auto_save_interval"));
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            if (!plugin.isEnabled()) {
                return;
            }
            List<VillagerShop> shopsCopy = new ArrayList<>(shops.values());
            shopsCopy.forEach(VillagerShop::save);
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
        long interval = 20L * ConfigManager.getInt("expire_check_interval");

        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, this::checkExpirations, 20L, interval);
    }

    private void checkExpirations() {
        boolean addDiscount = Instant.now().isAfter(nextAutoDiscount);
        int discountsToAdd = 0;
        if (addDiscount) {
            discountsToAdd = ConfigManager.getInt("auto_discount.shop_amount");
        }
        List<VillagerShop> shopsCopy = new ArrayList<>(shops.values());
        Collections.shuffle(shopsCopy);
        for (VillagerShop villagerShop : shopsCopy) {
            villagerShop.checkDiscounts();
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
                if (discountsToAdd <= 0 || !ConfigManager.getBoolean("auto_discount.enable")) {
                    continue;
                }
                String uuid = villagerShop.getEntityUUID().toString();
                if (!plugin.getConfig().getStringList("auto_discount.admin_shops").contains(uuid)) {
                    continue;
                }
                for (int i = 0; i < ConfigManager.getInt("auto_discount.item_amount"); i++) {
                    int min = ConfigManager.getInt("auto_discount.discount.min");
                    int max = ConfigManager.getInt("auto_discount.discount.max");
                    int discount = ThreadLocalRandom.current().nextInt(min, max + 1);
                    Instant end = VMUtils.getTimeFromNow(ConfigManager.getString("auto_discount.duration"));
                    villagerShop.addRandomDiscount(discount, end);
                }
                discountsToAdd --;
            }
            if (addDiscount) {
                nextAutoDiscount = VMUtils.getTimeFromNow(ConfigManager.getString("auto_discount.interval"));
            }
        }
    }

    /** Thread updates redstone output for all Villagers */
    private void beginRedstoneThread() {
        long interval = 20L * ConfigManager.getInt("redstone_update_interval");

        Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, () -> {
            if (!ConfigManager.getBoolean("enable_redstone_output")) {
                return;
            }
            for (VillagerShop villagerShop : shops.values()) {
                if (villagerShop instanceof PlayerShop) {
                    PlayerShop playerShop = (PlayerShop) villagerShop;
                    playerShop.updateRedstone(false);
                }
            }
        }, 20L, interval);
    }

    /** Saves/Resets Villager Config with default values */
    public File createShopConfig(UUID entityUUID, int storageSize, int shopfrontSize, int cost, String type, String duration) {
        File file = new File(plugin.getDataFolder() + "/Shops/", entityUUID + ".yml");
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
        return file;
    }

    /** Returns new Villager Shop Item */
    public ItemStack getShopItem(VMPlugin plugin, int shopSize, int storageSize, int amount) {

        int maxAmount = (Math.min(amount, 64));
        String infinite = ConfigManager.getString("quantity.infinite");
        String storageString = (storageSize == 0 ? infinite : String.valueOf(storageSize));
        String shopString = (shopSize == 0 ? infinite : String.valueOf(shopSize));

        ItemStack shopItem = ConfigManager.getItem("shop_item").replace("%shop_size%", shopString).replace("%storage_size%", storageString).build();

        shopItem.setAmount(maxAmount);
        if (VersionUtils.getMCVersion() >= 14) {
            ItemMeta shopMeta = shopItem.getItemMeta();
            shopMeta.getPersistentDataContainer().set(new NamespacedKey(plugin, "vm-item"), PersistentDataType.STRING, shopSize + "-" + storageSize);
            shopItem.setItemMeta(shopMeta);
            return shopItem;
        } else {
            NBTItem nbtItem = new NBTItem(shopItem);
            nbtItem.setString("vm-item", shopSize + "-" + storageSize);
            return nbtItem.getItem();
        }
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
