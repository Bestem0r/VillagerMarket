package bestem0r.villagermarket;

import bestem0r.villagermarket.commands.VMCompleter;
import bestem0r.villagermarket.commands.VMExecutor;
import bestem0r.villagermarket.events.EntityEvents;
import bestem0r.villagermarket.events.PlayerEvents;
import bestem0r.villagermarket.shops.AdminShop;
import bestem0r.villagermarket.shops.PlayerShop;
import bestem0r.villagermarket.shops.VillagerShop;
import bestem0r.villagermarket.utilities.Color;
import bestem0r.villagermarket.utilities.Methods;
import bestem0r.villagermarket.utilities.MetricsLite;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
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

    public static final HashMap<OfflinePlayer, List<ItemStack>> abandonOffline = new HashMap<>();

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
        beginSaveThread();
        beginExpireThread();

        getCommand("vm").setTabCompleter(new VMCompleter());
        getCommand("vm").setExecutor(new VMExecutor());

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
                for (Player player : Bukkit.getOnlinePlayers()) {
                    if (player.hasPermission("villagermarket.admin")) {
                        player.sendMessage(new Color.Builder().path("plugin_prefix").build() + " " + foundVersion);
                        player.sendMessage(new Color.Builder().path("plugin_prefix").build() + " " + downloadVersion);
                    }
                }
            }
        });

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
                shops.add(new AdminShop(file));
                break;
            case PLAYER:
                shops.add(new PlayerShop(file));
        }
    }
    /** Registers event listeners */
    private void registerEvents() {
        EntityEvents entityEvents = new EntityEvents(this);
        PlayerEvents playerEvents = new PlayerEvents();

        PluginManager pluginManager = getServer().getPluginManager();
        pluginManager.registerEvents(entityEvents, this);
        pluginManager.registerEvents(playerEvents, this);
    }

    /** Thread runs save() method for all Villager Shops */
    private void beginSaveThread() {
        long interval = 20 * 60 * VMPlugin.getInstance().getConfig().getLong("auto_save_interval");
        Bukkit.getServer().getScheduler().scheduleAsyncRepeatingTask(VMPlugin.getInstance(), () -> {
            for (VillagerShop villagerShop : VMPlugin.shops) {
                villagerShop.save();
            }
        }, 20L, interval);
    }

    /** Thread check if rent time has expired and runs abandon() method */
    private void beginExpireThread() {
        long interval = 20 * VMPlugin.getInstance().getConfig().getLong("expire_check_interval");
        FileConfiguration config = VMPlugin.getInstance().getConfig();

        Bukkit.getServer().getScheduler().scheduleSyncRepeatingTask(VMPlugin.getInstance(), () -> {
            for (VillagerShop villagerShop : VMPlugin.shops) {
                if (!(villagerShop instanceof PlayerShop)) { continue; }
                PlayerShop playerShop = (PlayerShop) villagerShop;

                if (playerShop.hasExpired() && !villagerShop.getOwnerUUID().equals("null")) {
                    Player player = Bukkit.getPlayer(UUID.fromString(villagerShop.getOwnerUUID()));
                    if (player != null) {
                        player.sendMessage(new Color.Builder().path("messages.expired").addPrefix().build());
                        player.playSound(player.getLocation(), Sound.valueOf(config.getString("sounds.expired")), 1, 1);
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

    /** Getters */
    public static Economy getEconomy() {
        return econ;
    }
    public static VMPlugin getInstance() {
        return instance;
    }
}
