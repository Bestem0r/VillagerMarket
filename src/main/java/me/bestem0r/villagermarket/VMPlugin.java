package me.bestem0r.villagermarket;

import me.bestem0r.villagermarket.commands.CommandModule;
import me.bestem0r.villagermarket.commands.subcommands.*;
import me.bestem0r.villagermarket.events.ChunkLoad;
import me.bestem0r.villagermarket.events.EntityEvents;
import me.bestem0r.villagermarket.events.PlayerEvents;
import me.bestem0r.villagermarket.shops.AdminShop;
import me.bestem0r.villagermarket.shops.PlayerShop;
import me.bestem0r.villagermarket.shops.VillagerShop;
import me.bestem0r.villagermarket.utilities.ColorBuilder;
import me.bestem0r.villagermarket.utilities.Methods;
import me.bestem0r.villagermarket.utilities.MetricsLite;
import me.bestem0r.villagermarket.utilities.Placeholders;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class VMPlugin extends JavaPlugin {

    private Economy econ = null;

    public static final List<String> log = new ArrayList<>();
    public static final List<VillagerShop> shops = new ArrayList<>();

    private boolean redstoneEnabled;
    private boolean regenVillagers;

    private boolean citizensEnabled;

    public static final HashMap<UUID, List<ItemStack>> abandonOffline = new HashMap<>();

    @Override
    public void onEnable() {
        setupEconomy();

        MetricsLite metricsLite = new MetricsLite(this, 8922);

        getConfig().options().copyDefaults();
        saveDefaultConfig();

        reload();

        loadConfigs();
        registerEvents();
        beginSaveThread();
        beginRedstoneThread();
        beginExpireThread();

        CommandModule module = new CommandModule.Builder(this)
                .addSubCommand("create", new CreateCommand(this))
                .addSubCommand("item", new ItemCommand(this))
                .addSubCommand("move", new MoveCommand(this))
                .addSubCommand("reload", new ReloadCommand(this))
                .addSubCommand("remove", new RemoveCommand(this))
                .addSubCommand("search", new SearchCommand(this))
                .addSubCommand("stats", new StatsCommand(this))
                .addSubCommand("trusted", new TrustedCommand(this))
                .addSubCommand("getid", new GetIDCommand(this))
                .build();
        module.register("vm");

        Bukkit.getScheduler().runTaskLater(this, () -> {
            if (Bukkit.getPluginManager().getPlugin("VillagerBank") != null) {
                Bukkit.getLogger().info("[VillagerMarket] Nice to see you Villager Bank!");
                Bukkit.getLogger().info("[VillagerBank] You too Villager Market!");
            }
        }, 31);

        new UpdateChecker(this, 82965).getVersion(version -> {
            String currentVersion = this.getDescription().getVersion();
            if (!currentVersion.equalsIgnoreCase(version)) {
                String foundVersion = ChatColor.translateAlternateColorCodes('&', "&bA new version of VillagerMarket was found!");
                String latestVersion = ChatColor.translateAlternateColorCodes('&',"&bLatest version: &a" + version + "&b.");
                String yourVersion = ChatColor.translateAlternateColorCodes('&', "&bYour version &c" + currentVersion + "&b.");
                String downloadVersion = ChatColor.translateAlternateColorCodes('&', "&bGet it here for the latest features and bug fixes: &ehttps://www.spigotmc.org/resources/villager-market.82965/");

                getLogger().warning(foundVersion);
                getLogger().warning(latestVersion);
                getLogger().warning(yourVersion);
                getLogger().warning(downloadVersion);
            }
        });

        if (regenVillagers) {
            for (World world : Bukkit.getWorlds()) {
                for (Chunk chunk : world.getLoadedChunks()) {
                    checkChunk(chunk);
                }
            }
        }

        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null){
            new Placeholders(this).register();
        }

        File abandonFile = new File(getDataFolder() + "/abandon_offline.yml");
        YamlConfiguration abandonConfig = YamlConfiguration.loadConfiguration(abandonFile);
        ConfigurationSection section = abandonConfig.getConfigurationSection("abandon_offline");
        if (section != null) {
            for (String uuid : section.getKeys(false)) {
                abandonOffline.put(UUID.fromString(uuid), (List<ItemStack>) abandonConfig.getList("abandon_offline." + uuid));
            }
        }

        super.onEnable();
    }

    @Override
    public void onDisable() {
        for (VillagerShop villagerShop : shops) {
            villagerShop.save();
        }
        Bukkit.getScheduler().cancelTasks(this);
        if (getConfig().getBoolean("auto_log")) saveLog();


        File abandonFile = new File(getDataFolder() + "/abandon_offline.yml");
        YamlConfiguration abandonConfig = YamlConfiguration.loadConfiguration(abandonFile);
        abandonConfig.set("abandon_offline", null);
        for (UUID uuid : abandonOffline.keySet()) {
            Bukkit.getLogger().info(uuid.toString());
            abandonConfig.set("abandon_offline." + uuid.toString(), abandonOffline.get(uuid));
        }
        try {
            abandonConfig.save(abandonFile);
        } catch (IOException e) {
            e.printStackTrace();
        }

        super.onDisable();
    }

    public void reload() {
        reloadConfig();
        this.regenVillagers =getConfig().getBoolean("villager_regen");
        this.redstoneEnabled = getConfig().getBoolean("enable_redstone_output");
        this.citizensEnabled = Bukkit.getPluginManager().getPlugin("Citizens") != null;
    }

    /** Setup Vault integration */
    private void setupEconomy() {
        RegisteredServiceProvider<Economy> economyProvider = getServer().getServicesManager().getRegistration(net.milkbowl.vault.economy.Economy.class);
        if (economyProvider != null) {
            econ = economyProvider.getProvider();
        } else {
            Bukkit.getLogger().info("Could not find Economy Provider!");
        }

    }

    /** Adds new Villager to villagers HashMap */
    public void addVillager(UUID entityUUID, File file, VillagerShop.VillagerType type) {
        VillagerShop villagerShop = Methods.shopFromUUID(entityUUID);
        if (villagerShop != null) {
            shops.remove(villagerShop);
        }
        switch (type) {
            case ADMIN:
                shops.add(new AdminShop(this, file));
                break;
            case PLAYER:
                shops.add(new PlayerShop(this, file));
        }
    }

    public void checkChunk(Chunk chunk) {
        if (regenVillagers) {
            for (VillagerShop villagerShop : VMPlugin.shops) {
                EntityInfo entityInfo = villagerShop.getEntityInfo();
                if (!entityInfo.hasStoredData()) {
                    continue;
                }
                if (entityInfo.isInChunk(chunk)) {
                    Bukkit.getLogger().info("Checking chunk: " + chunk.getX() + "_" + chunk.getZ());
                    if (entityInfo.exists()) {
                        Bukkit.getLogger().info("Appending: " + entityInfo.getEntityUUID());
                        entityInfo.appendToExisting();
                    } else {
                        Bukkit.getLogger().info("Regenerating: " + entityInfo.getEntityUUID());
                        entityInfo.reCreate();
                        Bukkit.getLogger().info("New entityUUID: " + entityInfo.getEntityUUID());
                    }
                }
            }
        }
    }

    /** Registers event listeners */
    private void registerEvents() {
        EntityEvents entityEvents = new EntityEvents(this);
        PlayerEvents playerEvents = new PlayerEvents(this);

        PluginManager pluginManager = getServer().getPluginManager();
        pluginManager.registerEvents(entityEvents, this);
        pluginManager.registerEvents(playerEvents, this);
        pluginManager.registerEvents(new ChunkLoad(this), this);
    }

    /** Thread runs save() method for all Villager Shops */
    private void beginSaveThread() {
        long interval = 20 * 60 * getConfig().getLong("auto_save_interval");
        Bukkit.getScheduler().runTaskTimerAsynchronously(this, () -> {
            for (VillagerShop villagerShop : VMPlugin.shops) {
                villagerShop.save();
            }
        }, 20L, interval);
    }

    /** Thread updates redstone output for all Villagers */
    private void beginRedstoneThread() {
        long interval = 20 * getConfig().getLong("redstone_update_interval");

        Bukkit.getScheduler().scheduleSyncRepeatingTask(this, () -> {
            for (VillagerShop villagerShop : VMPlugin.shops) {
                if (villagerShop instanceof PlayerShop) {
                    PlayerShop playerShop = (PlayerShop) villagerShop;
                    playerShop.updateRedstone(false);
                }
            }
        }, 20L, interval);
    }

    /** Thread check if rent time has expired and runs abandon() method */
    private void beginExpireThread() {
        long interval = 20 * getConfig().getLong("expire_check_interval");

        Bukkit.getScheduler().scheduleSyncRepeatingTask(this, () -> {
            for (VillagerShop villagerShop : VMPlugin.shops) {
                if (!(villagerShop instanceof PlayerShop)) { continue; }
                PlayerShop playerShop = (PlayerShop) villagerShop;

                if (playerShop.hasExpired() && !villagerShop.getOwnerUUID().equals("null")) {
                    Player player = Bukkit.getPlayer(UUID.fromString(villagerShop.getOwnerUUID()));
                    if (player != null) {
                        player.sendMessage(new ColorBuilder(this).path("messages.expired").addPrefix().build());
                        player.playSound(player.getLocation(), Sound.valueOf(getConfig().getString("sounds.expired")), 1, 1);
                    }
                    playerShop.abandon();
                }
            }
        }, 20L, interval);
    }

    /** Saves log to /log/ folder and clears log */
    public void saveLog() {
        String fileName = new Date().toString().replace(":","-");
        File file = new File(Bukkit.getServer().getPluginManager().getPlugin("VillagerMarket").getDataFolder() + "/logs/" + fileName + ".yml");
        FileConfiguration logConfig = YamlConfiguration.loadConfiguration(file);
        logConfig.set("log", log);
        try {
            logConfig.save(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
        log.clear();
    }

    /** Loads Villager Shops from /shops/ folder */
    private void loadConfigs() {
        Long before = new Date().getTime();
        File shopsFile = new File(Bukkit.getServer().getPluginManager().getPlugin("VillagerMarket").getDataFolder() + "/Shops/");
        if (shopsFile.exists() && shopsFile.isDirectory()) {
            for (File file : shopsFile.listFiles()) {
                String fileName = file.getName();
                String entityUUID = fileName.substring(0, fileName.length() - 4);
                FileConfiguration config = YamlConfiguration.loadConfiguration(file);
                String type = config.getString("type");
                type = (type == null ? "player" : type);

                try {
                    addVillager(UUID.fromString(entityUUID), file, VillagerShop.VillagerType.valueOf(type.toUpperCase()));
                } catch (Exception e) {
                    Bukkit.getLogger().severe("[VillagerMarket] " + file.toString() + " seems to be corrupt!");
                    e.printStackTrace();
                }
            }
        }

        Long after = new Date().getTime();
        Bukkit.getLogger().info("[VillagerMarket] Loaded " + shops.size() + " shops in " + (after - before) + " ms!");
    }

    public Economy getEconomy() {
        return econ;
    }
    public boolean isRedstoneEnabled() {
        return redstoneEnabled;
    }
    public boolean isCitizensEnabled() {
        return citizensEnabled;
    }
}
