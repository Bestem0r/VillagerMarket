package bestem0r.villagermarket;

import bestem0r.villagermarket.commands.VMCompleter;
import bestem0r.villagermarket.commands.VMExecutor;
import bestem0r.villagermarket.events.EntityEvents;
import bestem0r.villagermarket.events.InventoryClick;
import bestem0r.villagermarket.events.PlayerEvents;
import bestem0r.villagermarket.shops.AdminShop;
import bestem0r.villagermarket.shops.PlayerShop;
import bestem0r.villagermarket.shops.VillagerShop;
import bestem0r.villagermarket.utilities.Threads;
import bestem0r.villagermarket.utilities.Methods;
import bestem0r.villagermarket.utilities.MetricsLite;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
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

    private static Economy econ = null;
    private static VMPlugin instance;

    public static final List<String> log = new ArrayList<>();
    public static final List<VillagerShop> shops = new ArrayList<>();

    public static final HashMap<Player, VillagerShop> clickMap = new HashMap<>();
    public static final HashMap<OfflinePlayer, ArrayList<ItemStack>> abandonOffline = new HashMap<>();

    @Override
    public void onEnable() {
        setupEconomy();

        MetricsLite metricsLite = new MetricsLite(this, 8922);
        instance = this;

        getConfig().options().copyDefaults();
        saveDefaultConfig();
        reloadConfig();

        loadConfigs();
        registerEvents();
        beginThreads();

        getCommand("vm").setTabCompleter(new VMCompleter());
        getCommand("vm").setExecutor(new VMExecutor());

        super.onEnable();
    }

    @Override
    public void onDisable() {
        for (VillagerShop villagerShop : shops) {
            villagerShop.save();
        }
        Bukkit.getScheduler().cancelTasks(this);
        if (getConfig().getBoolean("auto_log")) saveLog();

        super.onDisable();
    }

    /** Setup Vault integration */
    private boolean setupEconomy() {
        RegisteredServiceProvider<Economy> economyProvider = getServer().getServicesManager().getRegistration(net.milkbowl.vault.economy.Economy.class);
        if (economyProvider != null) {
            econ = economyProvider.getProvider();
        } else {
            Bukkit.getLogger().info("Could not find Economy Provider!");
        }

        return (econ != null);
    }

    /** Adds new Villager to villagers HashMap */
    public void addVillager(UUID entityUUID, File file, VillagerShop.VillagerType type) {
        VillagerShop villagerShop = Methods.shopFromUUID(entityUUID);
        if (villagerShop != null) {
            shops.remove(villagerShop);
        }
        switch (type) {
            case ADMIN:
                shops.add(new AdminShop(file));
                break;
            case PLAYER:
                shops.add(new PlayerShop(file));
        }
    }
    /** Registers event listeners */
    private void registerEvents() {
        EntityEvents entityEvents = new EntityEvents(this);
        InventoryClick inventoryClick = new InventoryClick(this);
        PlayerEvents playerEvents = new PlayerEvents();

        PluginManager pluginManager = getServer().getPluginManager();
        pluginManager.registerEvents(entityEvents, this);
        pluginManager.registerEvents(inventoryClick, this);
        pluginManager.registerEvents(playerEvents, this);
    }

    /** Starts async/sync Bukkit threads from AsyncThreads class */
    private void beginThreads() {
        Threads thread = new Threads();
        thread.beginSaveThread();
        thread.beginExpireThread();
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
        File shopsFile = new File(Bukkit.getServer().getPluginManager().getPlugin("VillagerMarket").getDataFolder() + "/Shops/");
        if (shopsFile.exists()) {
            for (File file : shopsFile.listFiles()) {
                String fileName = file.getName();
                String entityUUID = fileName.substring(0, fileName.length() - 4);
                FileConfiguration config = YamlConfiguration.loadConfiguration(file);
                String type = config.getString("type");
                type = (type == null ? "player" : type);

                addVillager(UUID.fromString(entityUUID), file, VillagerShop.VillagerType.valueOf(type.toUpperCase()));
            }
        }
    }

    /** Getters */
    public static Economy getEconomy() {
        return econ;
    }
    public static VMPlugin getInstance() {
        return instance;
    }
}
